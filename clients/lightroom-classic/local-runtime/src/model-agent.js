const { providerRuntimeConfig } = require('./runtime-config')

async function createModelTune({ payload, selected, analysis, config, reactTrace, knowledgeMatches, fallbackTune, fetchImpl }) {
  const provider = String(payload?.provider || config?.provider || 'qwen2').toLowerCase()
  const providerConfig = providerRuntimeConfig(config || {}, provider)

  if (!providerConfig.apiKey) {
    return {
      success: false,
      sessionId: payload?.sessionId || `model-${Date.now()}`,
      message: `未配置 ${providerConfig.provider} API Key，请先在模型设置中保存 API Key。`,
      assistantMessage: `未配置 ${providerConfig.provider} API Key，请先在模型设置中保存 API Key。`,
      agentThought: {
        summary: '模型配置不完整，无法调用大模型。',
        decision: 'ask_user',
        nextAction: '请用户补充模型 API Key'
      },
      reactTrace: [{
        thought: '需要调用大模型生成调色方案',
        action: 'call_chat_model',
        observation: `缺少 ${providerConfig.provider} API Key`
      }],
      deltas: [],
      developSettings: {},
      runtimeProvider: providerConfig.provider
    }
  }

  const clientFetch = fetchImpl || fetch
  try {
    const response = await clientFetch(`${providerConfig.baseUrl}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${providerConfig.apiKey}`
      },
      body: JSON.stringify({
        model: providerConfig.model,
        response_format: { type: 'json_object' },
        messages: buildMessages(payload, selected, analysis, reactTrace, knowledgeMatches)
      })
    })
    const body = await response.json()
    if (!response.ok) {
      throw new Error(body?.error?.message || `HTTP ${response.status}`)
    }
    const content = body?.choices?.[0]?.message?.content || '{}'
    return normalizeModelResult(JSON.parse(content), providerConfig.provider, payload?.sessionId)
  } catch (error) {
    return {
      success: false,
      sessionId: payload?.sessionId || `model-${Date.now()}`,
      message: `模型调用失败：${error.message}`,
      assistantMessage: `模型调用失败：${error.message}`,
      agentThought: {
        summary: '模型调用失败，本轮没有生成可应用参数。',
        decision: 'ask_user',
        nextAction: '请检查网络、Base URL、模型名和 API Key 后重试'
      },
      reactTrace: [{
        thought: '调用模型获取 ReAct 决策',
        action: 'call_chat_model',
        observation: error.message
      }],
      deltas: [],
      developSettings: {},
      runtimeProvider: providerConfig.provider
    }
  }
}

function buildMessages(payload, selected, analysis, reactTrace, knowledgeMatches) {
  return [
    {
      role: 'system',
      content: [
        '你是 TonePilot Lightroom 调色 Agent，只输出 JSON。',
        '你需要使用 ReAct 思想组织本轮判断：先给出可见的思考摘要，再决定是否调用工具。',
        '不要输出隐藏推理链，不要逐字展示私密思考；只输出给用户可读的判断摘要、行动和观察。',
        '只生成 Lightroom Develop Settings 中本轮需要修改的参数，用户没有明确要求的参数必须保持不变。',
        '尤其不要默认修改 Temperature 和 Tint，只有用户明确说更暖、更冷、偏绿、偏洋红等白平衡需求时才可以修改。',
        'decision 只能是 respond、ask_user、apply_global_adjustments、plan_local_adjustments。',
        '不要生成图片，不要输出 Markdown。'
      ].join('\n')
    },
    {
      role: 'user',
      content: JSON.stringify({
        expectedSchema: {
          assistantMessage: '中文说明',
          agentThought: {
            summary: '主 Agent 对用户意图、照片和知识库的判断摘要',
            decision: 'respond | ask_user | apply_global_adjustments | plan_local_adjustments',
            nextAction: '下一步动作'
          },
          reactTrace: [
            { thought: '可见判断摘要', action: '工具或动作名称', observation: '观察结果摘要' }
          ],
          deltas: [{ group: 'basic', name: 'Exposure2012', label: '曝光', beforeValue: 0, afterValue: 0.2, delta: 0.2, reason: '原因' }],
          developSettings: { Exposure2012: 0.2 }
        },
        userMessage: payload?.message || '',
        photoMetadata: selected?.photo || {},
        currentAdjustment: selected?.currentAdjustment || null,
        analysis: analysis || {},
        reactTrace: reactTrace || payload?.reactTrace || [],
        knowledgeMatches: knowledgeMatches || payload?.knowledgeMatches || []
      })
    }
  ]
}

function normalizeModelResult(value, provider, sessionId) {
  const developSettings = value?.developSettings && typeof value.developSettings === 'object'
    ? value.developSettings
    : {}
  return {
    success: true,
    sessionId: value?.sessionId || sessionId || `model-${Date.now()}`,
    assistantMessage: value?.assistantMessage || '已根据你的描述生成 Lightroom 调色参数。',
    agentThought: normalizeAgentThought(value?.agentThought),
    reactTrace: normalizeReactTrace(value?.reactTrace),
    deltas: Array.isArray(value?.deltas) ? value.deltas : [],
    developSettings,
    runtimeProvider: provider
  }
}

function normalizeAgentThought(value) {
  return {
    summary: String(value?.summary || ''),
    decision: String(value?.decision || 'respond'),
    nextAction: String(value?.nextAction || '')
  }
}

function normalizeReactTrace(value) {
  if (!Array.isArray(value)) {
    return []
  }
  return value.map((step) => ({
    thought: String(step?.thought || ''),
    action: String(step?.action || ''),
    observation: String(step?.observation || '')
  })).filter((step) => step.thought || step.action || step.observation)
}

module.exports = {
  createModelTune,
  buildMessages
}
