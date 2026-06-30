const assert = require('assert')
const test = require('node:test')

const { createModelTune } = require('../src/model-agent')

test('没有配置 API Key 时模型 Agent 返回可解释错误，不再回退本地规则', async () => {
  const result = await createModelTune({
    payload: { provider: 'qwen2', message: '再亮一点' },
    selected: { photo: { fileName: 'a.raw' }, currentAdjustment: { basic: { exposure: 0 } } },
    analysis: { intent: '提亮画面' },
    config: { provider: 'qwen2', qwen2: { apiKey: '', baseUrl: 'https://qwen.test/v1', model: 'qwen' } },
    fallbackTune: () => ({ success: true, data: { assistantMessage: 'fallback', developSettings: { Exposure2012: 0.1 }, deltas: [] } })
  })

  assert.strictEqual(result.success, false)
  assert.match(result.message, /未配置 qwen2 API Key/)
  assert.strictEqual(result.runtimeProvider, 'qwen2')
})

test('Qwen2 使用 OpenAI 兼容接口并返回 ReAct 决策和 developSettings', async () => {
  const calls = []
  const result = await createModelTune({
    payload: { provider: 'qwen2', message: '只降低高光' },
    selected: { photo: { fileName: 'a.raw' }, currentAdjustment: { basic: { highlights: 0 } } },
    analysis: { intent: '降低高光' },
    reactTrace: [{
      thought: '需要先检查当前照片',
      action: 'inspect_lightroom_photo',
      observation: '当前选中 a.raw'
    }],
    knowledgeMatches: [{
      title: '夜景高光控制',
      score: 0.91,
      content: '夜景灯光需要优先保护高光。'
    }],
    config: {
      provider: 'qwen2',
      qwen2: { apiKey: 'sk-qwen', baseUrl: 'https://dashscope.test/compatible-mode/v1', model: 'qwen-plus' }
    },
    fallbackTune: () => ({ success: true, data: { assistantMessage: 'fallback', developSettings: {}, deltas: [] } }),
    fetchImpl: async (requestUrl, options) => {
      calls.push({ requestUrl, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({
          choices: [{
            message: {
              content: JSON.stringify({
                assistantMessage: '已只降低高光。',
                agentThought: {
                  summary: '用户明确要求降低高光',
                  decision: 'apply_global_adjustments',
                  nextAction: '调用 Lightroom 全局调色工具'
                },
                reactTrace: [{
                  thought: '只需要修改高光',
                  action: 'generate_develop_settings',
                  observation: '生成 Highlights2012=-25'
                }],
                deltas: [{ name: 'highlights', label: '高光', beforeValue: 0, afterValue: -25, delta: -25 }],
                developSettings: { Highlights2012: -25 }
              })
            }
          }]
        })
      }
    }
  })

  assert.strictEqual(calls[0].requestUrl, 'https://dashscope.test/compatible-mode/v1/chat/completions')
  assert.strictEqual(calls[0].options.headers.Authorization, 'Bearer sk-qwen')
  assert.match(calls[0].options.body, /qwen-plus/)
  assert.match(calls[0].options.body, /reactTrace/)
  assert.match(calls[0].options.body, /夜景高光控制/)
  assert.strictEqual(result.success, true)
  assert.strictEqual(result.agentThought.decision, 'apply_global_adjustments')
  assert.strictEqual(result.reactTrace[0].action, 'generate_develop_settings')
  assert.deepStrictEqual(result.developSettings, { Highlights2012: -25 })
  assert.strictEqual(result.runtimeProvider, 'qwen2')
})


test('model HTTP error keeps ask_user decision and visible ReAct observation', async () => {
  const result = await createModelTune({
    payload: { provider: 'openai', message: 'make it cool cinematic', sessionId: 's-http-error' },
    selected: { photo: { fileName: 'b.raw' }, currentAdjustment: { basic: { exposure: 0 } } },
    analysis: { intent: 'cool cinematic look' },
    config: {
      provider: 'openai',
      openai: { apiKey: 'sk-openai', baseUrl: 'https://openai.test/v1', model: 'gpt-test' }
    },
    fetchImpl: async () => ({
      ok: false,
      status: 429,
      json: async () => ({ error: { message: 'rate limit' } })
    })
  })

  assert.strictEqual(result.success, false)
  assert.strictEqual(result.sessionId, 's-http-error')
  assert.strictEqual(result.agentThought.decision, 'ask_user')
  assert.strictEqual(result.developSettings && Object.keys(result.developSettings).length, 0)
  assert.strictEqual(result.reactTrace[0].action, 'call_chat_model')
  assert.match(result.reactTrace[0].observation, /rate limit/)
  assert.strictEqual(result.runtimeProvider, 'openai')
})

test('model result filters empty ReAct steps and keeps plan_local_adjustments decision', async () => {
  const result = await createModelTune({
    payload: { provider: 'qwen2', message: 'plan a sky mask', sessionId: 's-mask-plan' },
    selected: { photo: { fileName: 'c.raw' }, currentAdjustment: { basic: {} } },
    analysis: { intent: 'local sky adjustment' },
    config: {
      provider: 'qwen2',
      qwen2: { apiKey: 'sk-qwen', baseUrl: 'https://qwen.test/v1', model: 'qwen-plus' }
    },
    fetchImpl: async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        choices: [{
          message: {
            content: JSON.stringify({
              assistantMessage: 'I will plan a local sky mask instead of applying global settings.',
              agentThought: {
                summary: 'The user asks for local sky editing.',
                decision: 'plan_local_adjustments',
                nextAction: 'Return mask region and parameter suggestions.'
              },
              reactTrace: [
                { thought: '', action: '', observation: '' },
                { thought: 'A local tool is required.', action: 'plan_mask_adjustment', observation: 'Generated a relative sky region.' }
              ],
              deltas: [],
              developSettings: {}
            })
          }
        }]
      })
    })
  })

  assert.strictEqual(result.success, true)
  assert.strictEqual(result.agentThought.decision, 'plan_local_adjustments')
  assert.strictEqual(result.reactTrace.length, 1)
  assert.strictEqual(result.reactTrace[0].action, 'plan_mask_adjustment')
  assert.deepStrictEqual(result.developSettings, {})
})
