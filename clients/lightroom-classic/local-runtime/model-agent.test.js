const assert = require('assert')
const test = require('node:test')

const { createModelTune } = require('./src/model-agent')

test('没有配置 API Key 时模型 Agent 自动回退本地规则', async () => {
  const result = await createModelTune({
    payload: { provider: 'qwen2', message: '再亮一点' },
    selected: { photo: { fileName: 'a.raw' }, currentAdjustment: { basic: { exposure: 0 } } },
    analysis: { intent: '提亮画面' },
    config: { provider: 'qwen2', qwen2: { apiKey: '', baseUrl: 'https://qwen.test/v1', model: 'qwen' } },
    fallbackTune: () => ({ success: true, data: { assistantMessage: 'fallback', developSettings: { Exposure2012: 0.1 }, deltas: [] } })
  })

  assert.strictEqual(result.assistantMessage, 'fallback')
  assert.strictEqual(result.runtimeProvider, 'rule')
})

test('Qwen2 使用 OpenAI 兼容接口并返回模型给出的 developSettings', async () => {
  const calls = []
  const result = await createModelTune({
    payload: { provider: 'qwen2', message: '只降低高光' },
    selected: { photo: { fileName: 'a.raw' }, currentAdjustment: { basic: { highlights: 0 } } },
    analysis: { intent: '降低高光' },
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
  assert.deepStrictEqual(result.developSettings, { Highlights2012: -25 })
  assert.strictEqual(result.runtimeProvider, 'qwen2')
})
