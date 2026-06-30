const fs = require('fs')
const path = require('path')

const SUPPORTED_PROVIDERS = new Set(['openai', 'qwen2'])

const DEFAULT_CONFIG = {
  provider: 'qwen2',
  openai: {
    apiKey: '',
    baseUrl: 'https://api.openai.com/v1',
    model: 'gpt-4o-mini'
  },
  qwen2: {
    apiKey: '',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    model: 'qwen-plus'
  },
  knowledge: {
    enabled: false,
    syncUrl: ''
  }
}

function readRuntimeConfig(filePath) {
  if (!fs.existsSync(filePath)) {
    return cloneConfig(DEFAULT_CONFIG)
  }
  try {
    const raw = fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, '')
    return mergeRuntimeConfig(DEFAULT_CONFIG, JSON.parse(raw || '{}'))
  } catch (error) {
    return cloneConfig(DEFAULT_CONFIG)
  }
}

function writeRuntimeConfig(filePath, patch) {
  const current = readRuntimeConfig(filePath)
  const next = mergeRuntimeConfig(current, patch || {})
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, JSON.stringify(next, null, 2), 'utf8')
  return next
}

function mergeRuntimeConfig(base, patch) {
  const current = cloneConfig(base || DEFAULT_CONFIG)
  const provider = normalizeProvider(patch.provider || current.provider)
  return {
    provider,
    openai: mergeProvider(current.openai, patch.openai),
    qwen2: mergeProvider(current.qwen2, patch.qwen2),
    knowledge: {
      enabled: toBoolean(patch.knowledge?.enabled, current.knowledge?.enabled === true),
      syncUrl: stringOrDefault(patch.knowledge?.syncUrl, current.knowledge?.syncUrl || '')
    }
  }
}

function publicRuntimeConfig(config) {
  const normalized = mergeRuntimeConfig(DEFAULT_CONFIG, config || {})
  return {
    provider: normalized.provider,
    openai: publicProvider(normalized.openai),
    qwen2: publicProvider(normalized.qwen2),
    knowledge: normalized.knowledge
  }
}

function providerRuntimeConfig(config, providerValue) {
  const normalized = mergeRuntimeConfig(DEFAULT_CONFIG, config || {})
  const provider = normalizeProvider(providerValue || normalized.provider)
  const providerConfig = normalized[provider] || {}
  return {
    provider,
    apiKey: providerConfig.apiKey || '',
    baseUrl: (providerConfig.baseUrl || '').replace(/\/$/, ''),
    model: providerConfig.model || ''
  }
}

function mergeProvider(current, patch) {
  return {
    apiKey: patch && Object.prototype.hasOwnProperty.call(patch, 'apiKey')
      ? String(patch.apiKey || '').trim()
      : String(current?.apiKey || ''),
    baseUrl: stringOrDefault(patch?.baseUrl, current?.baseUrl || ''),
    model: stringOrDefault(patch?.model, current?.model || '')
  }
}

function publicProvider(provider) {
  return {
    baseUrl: provider.baseUrl || '',
    model: provider.model || '',
    apiKeyConfigured: Boolean(provider.apiKey)
  }
}

function normalizeProvider(value) {
  const provider = String(value || 'qwen2').toLowerCase()
  return SUPPORTED_PROVIDERS.has(provider) ? provider : 'qwen2'
}

function stringOrDefault(value, defaultValue) {
  if (value === undefined || value === null) {
    return defaultValue
  }
  return String(value).trim()
}

function toBoolean(value, defaultValue) {
  if (value === undefined || value === null) {
    return defaultValue
  }
  return value === true || value === 'true'
}

function cloneConfig(value) {
  return JSON.parse(JSON.stringify(value))
}

module.exports = {
  DEFAULT_CONFIG,
  readRuntimeConfig,
  writeRuntimeConfig,
  mergeRuntimeConfig,
  publicRuntimeConfig,
  providerRuntimeConfig
}
