const assert = require('assert')
const fs = require('fs')
const os = require('os')
const path = require('path')
const test = require('node:test')

const {
  readRuntimeConfig,
  writeRuntimeConfig,
  publicRuntimeConfig,
  providerRuntimeConfig
} = require('./src/runtime-config')

test('本地运行时没有配置文件时默认完全离线使用本地规则', () => {
  const filePath = path.join(os.tmpdir(), `tonepilot-runtime-${Date.now()}.json`)

  const config = readRuntimeConfig(filePath)

  assert.strictEqual(config.provider, 'rule')
  assert.strictEqual(config.knowledge.enabled, false)
  assert.strictEqual(config.openai.model, 'gpt-4o-mini')
  assert.strictEqual(config.qwen2.model, 'qwen-plus')
})

test('本地运行时保存模型配置时只合并指定字段并隐藏密钥', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'tonepilot-runtime-'))
  const filePath = path.join(dir, 'runtime-config.json')

  const saved = writeRuntimeConfig(filePath, {
    provider: 'qwen2',
    qwen2: {
      apiKey: 'sk-qwen-secret',
      model: 'qwen-plus-latest'
    }
  })
  const publicConfig = publicRuntimeConfig(saved)

  assert.strictEqual(saved.provider, 'qwen2')
  assert.strictEqual(saved.qwen2.apiKey, 'sk-qwen-secret')
  assert.strictEqual(saved.qwen2.baseUrl, 'https://dashscope.aliyuncs.com/compatible-mode/v1')
  assert.strictEqual(publicConfig.qwen2.apiKeyConfigured, true)
  assert.strictEqual(publicConfig.qwen2.apiKey, undefined)
})

test('模型调用只能读取当前选中厂商的运行时配置', () => {
  const config = {
    provider: 'openai',
    openai: { apiKey: 'sk-openai', baseUrl: 'https://api.openai.test/v1', model: 'gpt-test' },
    qwen2: { apiKey: 'sk-qwen', baseUrl: 'https://qwen.test/v1', model: 'qwen-test' },
    knowledge: { enabled: false, syncUrl: '' }
  }

  const providerConfig = providerRuntimeConfig(config, 'qwen2')

  assert.deepStrictEqual(providerConfig, {
    provider: 'qwen2',
    apiKey: 'sk-qwen',
    baseUrl: 'https://qwen.test/v1',
    model: 'qwen-test'
  })
})
