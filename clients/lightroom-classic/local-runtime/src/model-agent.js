const { providerRuntimeConfig } = require('./runtime-config')

async function createModelTune({ payload, selected, analysis, config, fallbackTune, fetchImpl }) {
  const provider = String(payload?.provider || config?.provider || 'rule').toLowerCase()
  const providerConfig = providerRuntimeConfig(config || {}, provider)
  const fallback = () => normalizeFallback(fallbackTune(), 'rule')

  if (providerConfig.provider === 'rule' || !providerConfig.apiKey) {
    return fallback()
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
        messages: buildMessages(payload, selected, analysis)
      })
    })
    const body = await response.json()
    if (!response.ok) {
      throw new Error(body?.error?.message || `HTTP ${response.status}`)
    }
    const content = body?.choices?.[0]?.message?.content || '{}'
    return normalizeModelResult(JSON.parse(content), providerConfig.provider)
  } catch (error) {
    const data = fallback()
    data.assistantMessage = `${data.assistantMessage || '已使用本地规则完成。'}（模型调用失败，已回退本地规则：${error.message}）`
    data.runtimeProvider = 'rule'
    return data
  }
}

function buildMessages(payload, selected, analysis) {
  return [
    {
      role: 'system',
      content: [
        '你是 TonePilot Lightroom 调色 Agent，只输出 JSON。',
        '只生成 Lightroom Develop Settings 中本轮需要修改的参数，用户没有明确要求的参数必须保持不变。',
        '尤其不要默认修改 Temperature 和 Tint，只有用户明确说更暖、更冷、偏绿、偏洋红等白平衡需求时才可以修改。',
        '不要生成图片，不要输出 Markdown。'
      ].join('\n')
    },
    {
      role: 'user',
      content: JSON.stringify({
        expectedSchema: {
          assistantMessage: '中文说明',
          deltas: [{ group: 'basic', name: 'Exposure2012', label: '曝光', beforeValue: 0, afterValue: 0.2, delta: 0.2, reason: '原因' }],
          developSettings: { Exposure2012: 0.2 }
        },
        userMessage: payload?.message || '',
        photoMetadata: selected?.photo || {},
        currentAdjustment: selected?.currentAdjustment || null,
        analysis: analysis || {}
      })
    }
  ]
}

function normalizeModelResult(value, provider) {
  const developSettings = value?.developSettings && typeof value.developSettings === 'object'
    ? value.developSettings
    : {}
  return {
    sessionId: value?.sessionId || `model-${Date.now()}`,
    assistantMessage: value?.assistantMessage || '已根据你的描述生成 Lightroom 调色参数。',
    deltas: Array.isArray(value?.deltas) ? value.deltas : [],
    developSettings,
    runtimeProvider: provider
  }
}

function normalizeFallback(result, provider) {
  const data = result?.data || result || {}
  return {
    sessionId: data.sessionId || `fallback-${Date.now()}`,
    assistantMessage: data.assistantMessage || '已使用本地规则生成调色参数。',
    deltas: Array.isArray(data.deltas) ? data.deltas : [],
    developSettings: data.developSettings || {},
    runtimeProvider: provider
  }
}

module.exports = {
  createModelTune,
  buildMessages
}
