const crypto = require('crypto')
const fs = require('fs')
const http = require('http')
const path = require('path')
const url = require('url')
const { createBridgePaths } = require('../bridge-paths')

const port = Number(process.env.TONEPILOT_LIGHTROOM_BRIDGE_PORT || 47917)
const host = process.env.TONEPILOT_LIGHTROOM_BRIDGE_HOST || '0.0.0.0'
const backendBaseUrl = (process.env.TONEPILOT_BACKEND_URL || 'http://127.0.0.1:8080').replace(/\/$/, '')
const bridgePaths = createBridgePaths()
const bridgeRoot = bridgePaths.fsRoot
const applyJobsDir = bridgePaths.fs('apply-jobs')
const processingDir = bridgePaths.fs('processing')
const resultsDir = bridgePaths.fs('results')
const applyResultsDir = bridgePaths.fs('apply-results')
const agentRequestsDir = bridgePaths.fs('agent-requests')
const agentResultsDir = bridgePaths.fs('agent-results')
const heartbeatPath = bridgePaths.fs('heartbeat.txt')
const selectedPhotoPath = bridgePaths.fs('selected-photo.json')
const sessionsPath = bridgePaths.fs('sessions.json')

ensureDirectory(applyJobsDir)
ensureDirectory(processingDir)
ensureDirectory(resultsDir)
ensureDirectory(applyResultsDir)
ensureDirectory(agentRequestsDir)
ensureDirectory(agentResultsDir)
setInterval(processAgentRequests, 500)

const server = http.createServer(async (request, response) => {
  const parsedUrl = url.parse(request.url, true)
  setCorsHeaders(response)

  if (request.method === 'OPTIONS') {
    response.writeHead(204)
    response.end()
    return
  }

  try {
    if (request.method === 'GET' && parsedUrl.pathname === '/status') {
      respondJson(response, statusPayload())
      return
    }

    if (request.method === 'GET' && parsedUrl.pathname === '/agent-console') {
      respondHtml(response, agentConsoleHtml())
      return
    }

    if (request.method === 'GET' && parsedUrl.pathname === '/api/lightroom/selected-photo') {
      respondJson(response, readSelectedPhotoPayload())
      return
    }

    if (request.method === 'POST' && parsedUrl.pathname === '/api/lightroom-agent/chat') {
      const body = await readRequestBody(request)
      const payload = JSON.parse(body || '{}')
      const result = await createAgentChat(payload)
      respondJson(response, result, result.success === false ? 400 : 200)
      return
    }

    if (request.method === 'GET' && parsedUrl.pathname && parsedUrl.pathname.startsWith('/files/')) {
      serveResultFile(parsedUrl.pathname.substring('/files/'.length), response)
      return
    }

    respondJson(response, { success: false, message: '未找到接口' }, 404)
  } catch (error) {
    respondJson(response, { success: false, message: error.message }, 500)
  }
})

server.listen(port, host, () => {
  console.log(`[TonePilot Bridge] 服务已启动: http://${host}:${port}`)
  console.log(`[TonePilot Bridge] Bridge 文件目录: ${bridgeRoot}`)
  console.log(`[TonePilot Bridge] Lightroom 任务目录: ${bridgePaths.lightroomRoot}`)
  console.log(`[TonePilot Bridge] 后端地址: ${backendBaseUrl}`)
})

function statusPayload() {
  const heartbeat = readHeartbeat()
  if (!heartbeat) {
    return {
      available: false,
      mode: 'lightroom-classic-bridge',
      message: 'Bridge 服务已启动，但 Lightroom 插件尚未写入心跳。请确认 Lightroom Classic 已启用 TonePilot 插件。',
      nextSteps: [
        '在 Lightroom Classic 插件管理器中添加 TonePilotLightroomBridge.lrplugin',
        '保持 Lightroom Classic 打开，并选中要修图的照片',
        `确认插件任务目录为 ${bridgePaths.lightroomRoot}`
      ]
    }
  }

  return {
    available: true,
    mode: 'lightroom-classic-bridge',
    message: `Lightroom Bridge 已连接，最近心跳 ${heartbeat.ageSeconds} 秒前。`,
    nextSteps: []
  }
}

async function createAgentChat(payload) {
  const selected = readSelectedPhotoPayload()
  if (!selected.available) {
    return {
      success: false,
      message: selected.message || 'Lightroom 当前没有选中照片，请先在 Lightroom 中选择照片。'
    }
  }
  if (!payload.message || !String(payload.message).trim()) {
    return {
      success: false,
      message: '请输入调色或分析指令。'
    }
  }

  const sessionContext = ensurePhotoSession(selected)
  const analysis = buildPhotoAnalysis(selected, payload.message)
  const backendResponse = await fetch(`${backendBaseUrl}/api/lightroom-agent/tune`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      sessionId: payload.sessionId || `agent-console-${Date.now()}`,
      localPhotoId: selected.photo?.fileName || '',
      message: enrichMessage(payload.message, analysis),
      provider: payload.provider || 'rule',
      currentAdjustment: selected.currentAdjustment || null,
      photoMetadata: selected.photo || null
    })
  })
  const backendData = await backendResponse.json()
  if (!backendResponse.ok || backendData.success === false) {
    return {
      success: false,
      message: backendData.message || `后端 Agent 调用失败，HTTP ${backendResponse.status}`
    }
  }

  const agentData = backendData.data
  const applyResult = await applyDevelopSettingsInLightroom(agentData.developSettings || {})
  const version = applyResult.success ? recordSessionVersion(sessionContext, {
    userIntent: payload.message,
    agentSummary: agentData.assistantMessage,
    analysis,
    deltas: agentData.deltas || [],
    developSettings: agentData.developSettings || {},
    previewUrl: applyResult.previewUrl || '',
    applySuccess: true,
    applyMessage: applyResult.message || ''
  }) : null
  return {
    success: applyResult.success,
    message: applyResult.message || agentData.assistantMessage || 'TonePilot Agent 已完成。',
    data: {
      sessionId: agentData.sessionId,
      photo: selected.photo,
      analysis,
      assistantMessage: agentData.assistantMessage,
      deltas: agentData.deltas || [],
      developSettings: agentData.developSettings || {},
      beforePreviewUrl: sessionContext.session.baselinePreviewUrl || selected.previewUrl || null,
      afterPreviewUrl: version?.previewUrl || applyResult.previewUrl || null,
      version,
      session: compactSession(sessionContext.session),
      apply: applyResult
    }
  }
}

async function applyDevelopSettingsInLightroom(developSettings) {
  const jobId = `agent-apply-${Date.now()}-${crypto.randomBytes(3).toString('hex')}`
  const resultFileName = `${jobId}.result`
  const previewFileName = `${jobId}.jpg`
  const resultPath = path.join(applyResultsDir, resultFileName)
  const jobPath = path.join(applyJobsDir, `${jobId}.lua`)
  const timeoutMs = Number(process.env.TONEPILOT_LIGHTROOM_BRIDGE_APPLY_TIMEOUT_MS || 60000)
  fs.writeFileSync(jobPath, toLuaTable({
    id: jobId,
    resultPath: bridgePaths.lightroom('apply-results', resultFileName),
    previewFileName,
    previewPath: bridgePaths.lightroom('results', previewFileName),
    developSettings
  }), 'utf8')
  return waitForResult(resultPath, timeoutMs)
}

function snapshotCurrentPreview() {
  const sourcePath = path.join(resultsDir, 'selected-preview.jpg')
  if (!fs.existsSync(sourcePath)) {
    return { previewUrl: null }
  }
  const fileName = `before-${Date.now()}-${crypto.randomBytes(3).toString('hex')}.jpg`
  const targetPath = path.join(resultsDir, fileName)
  fs.copyFileSync(sourcePath, targetPath)
  return {
    previewUrl: `/files/${fileName}?t=${Date.now()}`
  }
}

function readSessions() {
  if (!fs.existsSync(sessionsPath)) {
    return { sessions: {} }
  }
  try {
    const data = JSON.parse(fs.readFileSync(sessionsPath, 'utf8').replace(/^\uFEFF/, ''))
    return data && typeof data === 'object' ? { sessions: data.sessions || {} } : { sessions: {} }
  } catch (error) {
    return { sessions: {} }
  }
}

function writeSessions(data) {
  fs.writeFileSync(sessionsPath, JSON.stringify({ sessions: data.sessions || {} }, null, 2), 'utf8')
}

function stablePhotoKey(payload) {
  const photo = payload.photo || payload || {}
  const metadataPath = payload.metadataDebug?.path?.rawValue
    || payload.metadataDebug?.path?.formattedValue
    || payload.metadataDiagnostics?.path
    || photo.path
    || ''
  const parts = [
    metadataPath,
    photo.fileName || '',
    photo.copyName || '',
    photo.captureTime || '',
    photo.dimensions || '',
    photo.camera || '',
    photo.lens || ''
  ]
  return crypto.createHash('sha1').update(parts.join('|')).digest('hex').slice(0, 16)
}

function previewUrlFor(fileName) {
  return `/files/${fileName}?t=${Date.now()}`
}

function copySelectedPreview(fileName) {
  const sourcePath = path.join(resultsDir, 'selected-preview.jpg')
  if (!fs.existsSync(sourcePath)) {
    return ''
  }
  fs.copyFileSync(sourcePath, path.join(resultsDir, fileName))
  return previewUrlFor(fileName)
}

function ensurePhotoSession(selectedPayload) {
  const sessionsData = readSessions()
  const photoKey = stablePhotoKey(selectedPayload)
  const photo = selectedPayload.photo || {}
  let session = sessionsData.sessions[photoKey]
  if (!session) {
    session = {
      photoKey,
      fileName: photo.fileName || '',
      copyName: photo.copyName || '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      baselinePreviewUrl: '',
      baselineAdjustment: null,
      currentVersionId: '',
      versions: []
    }
    sessionsData.sessions[photoKey] = session
  }

  session.fileName = photo.fileName || session.fileName || ''
  session.copyName = photo.copyName || session.copyName || ''
  ensureSessionBaseline(selectedPayload, session, sessionsData)
  return { sessionsData, session }
}

function ensureSessionBaseline(selectedPayload, session, sessionsData) {
  if (session.baselinePreviewUrl) {
    return session
  }
  const fileName = `baseline-${session.photoKey}.jpg`
  const baselineUrl = copySelectedPreview(fileName)
  if (baselineUrl) {
    session.baselinePreviewUrl = baselineUrl
    session.baselineAdjustment = selectedPayload.currentAdjustment || null
    session.updatedAt = new Date().toISOString()
    writeSessions(sessionsData)
  }
  return session
}

function recordSessionVersion(sessionContext, value) {
  const session = sessionContext.session
  const versionId = `v-${Date.now()}-${crypto.randomBytes(3).toString('hex')}`
  const versionFileName = `version-${session.photoKey}-${versionId}.jpg`
  let versionPreviewUrl = value.previewUrl || ''
  if (!versionPreviewUrl) {
    versionPreviewUrl = copySelectedPreview(versionFileName)
  }
  const version = {
    versionId,
    photoKey: session.photoKey,
    fileName: session.fileName || '',
    createdAt: new Date().toISOString(),
    userIntent: String(value.userIntent || ''),
    agentSummary: value.agentSummary || '',
    analysis: value.analysis || {},
    deltas: value.deltas || [],
    developSettings: value.developSettings || {},
    previewUrl: versionPreviewUrl,
    applySuccess: value.applySuccess !== false,
    applyMessage: value.applyMessage || ''
  }
  session.versions = Array.isArray(session.versions) ? session.versions : []
  session.versions.push(version)
  if (session.versions.length > 30) {
    session.versions = session.versions.slice(session.versions.length - 30)
  }
  session.currentVersionId = version.versionId
  session.updatedAt = new Date().toISOString()
  writeSessions(sessionContext.sessionsData)
  return version
}

function compactSession(session) {
  const versions = Array.isArray(session?.versions) ? session.versions : []
  const normalizedVersions = versions.map((version) => ({
    ...version,
    photoKey: version.photoKey || session.photoKey || '',
    fileName: version.fileName || session.fileName || ''
  }))
  const currentVersion = normalizedVersions.find((version) => version.versionId === session.currentVersionId) || normalizedVersions[normalizedVersions.length - 1] || null
  return {
    photoKey: session?.photoKey || '',
    baselinePreviewUrl: session?.baselinePreviewUrl || '',
    currentVersionId: session?.currentVersionId || '',
    currentVersion,
    versions: normalizedVersions.slice(-8).reverse()
  }
}

function waitForResult(resultPath, timeoutMs) {
  const startedAt = Date.now()
  return new Promise((resolve) => {
    const timer = setInterval(() => {
      if (fs.existsSync(resultPath)) {
        clearInterval(timer)
        resolve(parseResultFile(resultPath))
        return
      }
      if (Date.now() - startedAt > timeoutMs) {
        clearInterval(timer)
        resolve({
          success: false,
          message: '等待 Lightroom 插件应用参数超时，请确认插件已启用且 Lightroom 中已选中照片。'
        })
      }
    }, 500)
  })
}

function parseResultFile(filePath) {
  const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/)
  const result = {}
  for (const line of lines) {
    const index = line.indexOf('=')
    if (index <= 0) continue
    result[line.slice(0, index)] = line.slice(index + 1)
  }
  return {
    success: result.success === 'true',
    message: result.message || '',
    previewUrl: result.previewUrl || ''
  }
}

function serveResultFile(fileName, response) {
  const safeName = path.basename(decodeURIComponent(fileName))
  const filePath = path.join(resultsDir, safeName)
  if (!fs.existsSync(filePath)) {
    respondJson(response, { success: false, message: '预览文件不存在' }, 404)
    return
  }
  response.writeHead(200, {
    'Content-Type': 'image/jpeg',
    'Cache-Control': 'no-store'
  })
  fs.createReadStream(filePath).pipe(response)
}

async function processAgentRequests() {
  let files = []
  try {
    files = fs.readdirSync(agentRequestsDir).filter((name) => name.endsWith('.json'))
  } catch (error) {
    console.error(`[TonePilot Bridge] 读取 Agent 请求目录失败: ${error.message}`)
    return
  }

  for (const fileName of files) {
    const requestPath = path.join(agentRequestsDir, fileName)
    const processingPath = path.join(processingDir, fileName)
    try {
      fs.renameSync(requestPath, processingPath)
    } catch (error) {
      continue
    }
    await processAgentRequest(processingPath)
  }
}

async function processAgentRequest(requestPath) {
  const jobId = path.basename(requestPath, '.json')
  const resultPath = path.join(agentResultsDir, `${jobId}.lua`)
  try {
    const payload = JSON.parse(fs.readFileSync(requestPath, 'utf8').replace(/^\uFEFF/, ''))
    const backendResponse = await fetch(`${backendBaseUrl}/api/lightroom-agent/tune`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: payload.sessionId || jobId,
        localPhotoId: payload.localPhotoId || '',
        message: payload.message || '',
        provider: payload.provider || 'rule',
        currentAdjustment: payload.currentAdjustment || null,
        photoMetadata: payload.photoMetadata || null
      })
    })
    const data = await backendResponse.json()
    if (!backendResponse.ok || data.success === false) {
      writeLuaResult(resultPath, {
        success: false,
        message: data.message || `后端 Agent 调用失败，HTTP ${backendResponse.status}`
      })
      return
    }
    writeLuaResult(resultPath, {
      success: true,
      message: data.data?.assistantMessage || 'TonePilot Agent 已生成调色参数。',
      response: data.data
    })
  } catch (error) {
    writeLuaResult(resultPath, {
      success: false,
      message: `调用 TonePilot 后端 Agent 失败：${error.message}`
    })
  } finally {
    fs.rmSync(requestPath, { force: true })
  }
}

function writeLuaResult(resultPath, value) {
  fs.writeFileSync(resultPath, toLuaTable(value), 'utf8')
}

function readHeartbeat() {
  if (!fs.existsSync(heartbeatPath)) {
    return null
  }
  const timestamp = Number(fs.readFileSync(heartbeatPath, 'utf8').trim())
  if (!Number.isFinite(timestamp)) {
    return null
  }
  const ageSeconds = Math.round(Date.now() / 1000 - timestamp)
  if (ageSeconds > 15) {
    return null
  }
  return { timestamp, ageSeconds }
}

function readSelectedPhotoPayload() {
  if (!fs.existsSync(selectedPhotoPath)) {
    return {
      available: false,
      message: 'Lightroom 插件尚未写入当前照片状态，请确认插件已启用并稍等 1 秒。'
    }
  }
  try {
    const payload = JSON.parse(fs.readFileSync(selectedPhotoPath, 'utf8').replace(/^\uFEFF/, ''))
    const ageSeconds = Math.round(Date.now() / 1000 - Number(payload.updatedAt || 0))
    if (!payload.available || ageSeconds > 15) {
      return {
        available: false,
        message: payload.message || 'Lightroom 当前没有选中照片。',
        ageSeconds
      }
    }
    const sessionContext = ensurePhotoSession(payload)
    return {
      ...payload,
      ageSeconds,
      session: compactSession(sessionContext.session),
      baselinePreviewUrl: sessionContext.session.baselinePreviewUrl || ''
    }
  } catch (error) {
    return {
      available: false,
      message: `读取 Lightroom 当前照片状态失败：${error.message}`
    }
  }
}

function buildPhotoAnalysis(selected, message) {
  const photo = selected.photo || {}
  const text = String(message || '').toLowerCase()
  const type = inferPhotoType(photo, text)
  const style = inferStyle(text, type)
  const intent = inferIntent(text)
  return {
    intent,
    photoType: type,
    recommendedStyle: style,
    basis: [
      photo.fileName ? `文件：${photo.fileName}` : '',
      photo.fileFormat ? `格式：${photo.fileFormat}` : '',
      photo.camera ? `相机：${photo.camera}` : '',
      photo.lens ? `镜头：${photo.lens}` : '',
      photo.iso ? `ISO：${photo.iso}` : '',
      photo.dimensions ? `尺寸：${photo.dimensions}` : ''
    ].filter(Boolean)
  }
}

function inferPhotoType(photo, text) {
  if (/夜景|夜色|灯光|赛博|city|night/.test(text)) return '夜景 / 城市氛围照片'
  if (/人像|肤色|写真|portrait/.test(text)) return '人像照片'
  if (/天空|海|山|风景|landscape/.test(text)) return '风光照片'
  if (/街拍|纪实|街头/.test(text)) return '街拍纪实照片'
  if (String(photo.fileFormat || '').toLowerCase().includes('raw')) return 'RAW 原片'
  return '普通摄影照片'
}

function inferStyle(text, type) {
  if (/电影|cinematic|赛博|夜景/.test(text)) return '夜景电影感：压高光、提暗部、增强对比和去朦胧，保留灯光层次'
  if (/日系|清透|干净/.test(text)) return '日系清透：提高整体明度、降低硬对比、控制绿色干扰'
  if (/胶片|film|复古/.test(text)) return '胶片复古：抬黑、降饱和、增加颗粒和暗角'
  if (type.includes('人像')) return '自然人像：保护肤色，提升橙色明度和暗部可读性'
  return '自然增强：先做曝光、对比、白平衡和局部色彩的温和修正'
}

function inferIntent(text) {
  const intents = []
  if (/亮|提亮|太暗/.test(text)) intents.push('提亮画面')
  if (/暗|压暗|太亮/.test(text)) intents.push('压暗亮度')
  if (/暖/.test(text)) intents.push('调整为暖调')
  if (/冷/.test(text)) intents.push('调整为冷调')
  if (/电影|cinematic|氛围/.test(text)) intents.push('建立电影感氛围')
  if (/肤色|人像/.test(text)) intents.push('优化肤色')
  if (/绿色|草地/.test(text)) intents.push('控制绿色')
  return intents.length > 0 ? intents.join('、') : '理解用户调色描述并生成可应用参数'
}

function enrichMessage(message, analysis) {
  return [
    String(message || '').trim(),
    '',
    '请先按摄影调色 Agent 的方式分析用户意图和照片类型，再生成 Lightroom 参数。',
    `照片类型判断：${analysis.photoType}`,
    `建议风格：${analysis.recommendedStyle}`,
    `用户意图：${analysis.intent}`
  ].join('\n')
}

function readRequestBody(request) {
  return new Promise((resolve, reject) => {
    let body = ''
    request.on('data', (chunk) => {
      body += chunk
      if (body.length > 1024 * 1024) {
        reject(new Error('请求体过大'))
      }
    })
    request.on('end', () => resolve(body))
    request.on('error', reject)
  })
}

function respondJson(response, payload, statusCode = 200) {
  response.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store'
  })
  response.end(JSON.stringify(payload))
}

function respondHtml(response, html, statusCode = 200) {
  response.writeHead(statusCode, {
    'Content-Type': 'text/html; charset=utf-8',
    'Cache-Control': 'no-store'
  })
  response.end(html)
}

function setCorsHeaders(response) {
  response.setHeader('Access-Control-Allow-Origin', '*')
  response.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS')
  response.setHeader('Access-Control-Allow-Headers', 'Content-Type')
}

function ensureDirectory(directory) {
  fs.mkdirSync(directory, { recursive: true })
}

function toLuaTable(value) {
  return `return ${luaValue(value)}\n`
}

function luaValue(value) {
  if (value === null || value === undefined) {
    return 'nil'
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) ? String(value) : '0'
  }
  if (typeof value === 'boolean') {
    return value ? 'true' : 'false'
  }
  if (typeof value === 'string') {
    return JSON.stringify(value)
  }
  if (Array.isArray(value)) {
    return `{${value.map(luaValue).join(',')}}`
  }
  const entries = Object.entries(value).map(([key, entryValue]) => {
    return `[${JSON.stringify(key)}]=${luaValue(entryValue)}`
  })
  return `{${entries.join(',')}}`
}

function agentConsoleHtml() {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>TonePilot Agent 控制台</title>
  <style>
    :root {
      color-scheme: dark;
      --bg: #1f1f1f;
      --panel: #2b2b2b;
      --panel-2: #343434;
      --line: #4a4a4a;
      --text: #d6d6d6;
      --muted: #9a9a9a;
      --accent: #b8c7d9;
      --ok: #7fb98f;
      --warn: #d7b56d;
      --bad: #d98383;
      font-family: "Segoe UI", "Microsoft YaHei", Arial, sans-serif;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      min-height: 100vh;
      background: var(--bg);
      color: var(--text);
      overflow: hidden;
    }
    .shell {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      height: 100vh;
    }
    aside {
      border-right: 1px solid var(--line);
      background: #252525;
      padding: 16px;
      overflow: auto;
    }
    main {
      display: grid;
      grid-template-rows: auto 1fr auto;
      min-width: 0;
      height: 100vh;
      border-left: 1px solid #191919;
    }
    .brand {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      padding-bottom: 12px;
      border-bottom: 1px solid var(--line);
    }
    h1, h2, h3, p { margin: 0; }
    h1 { font-size: 15px; font-weight: 650; }
    h2 { font-size: 13px; color: var(--muted); font-weight: 500; margin-top: 3px; }
    h3 { font-size: 12px; color: var(--accent); font-weight: 650; margin-bottom: 8px; }
    .badge {
      font-size: 11px;
      border: 1px solid var(--line);
      border-radius: 3px;
      padding: 3px 6px;
      color: var(--muted);
    }
    .section {
      padding: 14px 0;
      border-bottom: 1px solid var(--line);
    }
    .preview-card {
      margin-top: 14px;
      background: #181818;
      border: 1px solid var(--line);
      border-radius: 6px;
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      gap: 1px;
      overflow: hidden;
    }
    .preview-pane {
      position: relative;
      min-height: 0;
      aspect-ratio: 4 / 3;
      display: grid;
      place-items: center;
      background: #181818;
      overflow: hidden;
    }
    .preview-pane::before {
      content: attr(data-label);
      position: absolute;
      top: 8px;
      left: 8px;
      z-index: 1;
      padding: 3px 6px;
      border: 1px solid #4a4a4a;
      border-radius: 3px;
      background: rgba(31, 31, 31, 0.82);
      color: #d6d6d6;
      font-size: 11px;
      max-width: calc(100% - 16px);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .preview-pane img {
      width: 100%;
      height: 100%;
      object-fit: contain;
      display: none;
    }
    .preview-empty {
      color: var(--muted);
      font-size: 13px;
      text-align: center;
      padding: 18px;
      line-height: 1.6;
    }
    .analysis-box {
      display: grid;
      gap: 8px;
      font-size: 12px;
      color: var(--muted);
      line-height: 1.55;
    }
    .analysis-box strong {
      color: var(--text);
      font-weight: 600;
    }
    .version-list {
      display: grid;
      gap: 7px;
    }
    .version-item {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 8px;
      align-items: center;
      padding: 8px;
      border: 1px solid #3c3c3c;
      border-radius: 4px;
      background: #242424;
      color: var(--muted);
      font-size: 12px;
      line-height: 1.35;
    }
    .version-item strong {
      display: block;
      color: var(--text);
      font-weight: 600;
      margin-bottom: 2px;
      word-break: break-word;
    }
    .version-item button {
      min-width: 48px;
      height: 28px;
      padding: 0 8px;
      font-size: 12px;
    }
    .kv {
      display: grid;
      grid-template-columns: 64px 1fr;
      gap: 7px;
      font-size: 12px;
      line-height: 1.45;
      color: var(--muted);
    }
    .kv strong { color: var(--text); font-weight: 500; word-break: break-all; }
    .topbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      min-height: 58px;
      padding: 12px 16px;
      border-bottom: 1px solid var(--line);
      background: var(--panel);
    }
    .status { font-size: 12px; color: var(--muted); }
    .status.ok { color: var(--ok); }
    .status.warn { color: var(--warn); }
    .chat {
      padding: 16px;
      overflow: auto;
      background: #202020;
    }
    .message {
      width: min(760px, 100%);
      margin: 0 0 12px;
      padding: 11px 12px;
      border: 1px solid var(--line);
      border-radius: 6px;
      background: var(--panel);
      font-size: 13px;
      line-height: 1.55;
      white-space: pre-wrap;
    }
    .message.user {
      margin-left: auto;
      background: #30363d;
      border-color: #46515e;
    }
    .message.agent {
      background: #282828;
    }
    .message .role {
      display: block;
      margin-bottom: 5px;
      color: var(--accent);
      font-size: 11px;
      font-weight: 650;
    }
    .composer {
      display: grid;
      grid-template-columns: 1fr 124px;
      gap: 10px;
      padding: 12px 16px;
      border-top: 1px solid var(--line);
      background: var(--panel);
    }
    .quick-actions {
      grid-column: 1 / -1;
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
    .quick-action {
      min-height: 28px;
      padding: 0 9px;
      color: var(--muted);
      font-size: 12px;
      font-weight: 600;
    }
    textarea, select, button {
      color: var(--text);
      background: var(--panel-2);
      border: 1px solid var(--line);
      border-radius: 4px;
      font: inherit;
    }
    textarea {
      width: 100%;
      min-height: 74px;
      resize: vertical;
      padding: 10px;
      outline: none;
    }
    .actions {
      display: grid;
      grid-template-rows: 32px 1fr;
      gap: 8px;
    }
    button {
      cursor: pointer;
      font-weight: 650;
    }
    button.primary {
      background: #3a4652;
      border-color: #596879;
    }
    button:disabled {
      cursor: not-allowed;
      color: #777;
      background: #2c2c2c;
    }
    .deltas {
      margin-top: 10px;
      display: grid;
      gap: 6px;
    }
    .delta {
      display: grid;
      grid-template-columns: 92px 1fr;
      gap: 8px;
      color: var(--muted);
      font-size: 12px;
      padding: 6px 8px;
      background: #242424;
      border: 1px solid #3c3c3c;
      border-radius: 4px;
    }
    @media (max-width: 820px) {
      .shell { grid-template-columns: 1fr; }
      aside { display: none; }
      .composer { grid-template-columns: 1fr; }
      .actions { grid-template-columns: 1fr 1fr; grid-template-rows: none; }
    }
  </style>
</head>
<body>
  <div class="shell">
    <aside>
      <div class="brand">
        <div>
          <h1>TonePilot</h1>
          <h2>Lightroom Agent</h2>
        </div>
        <span class="badge">深灰控制台</span>
      </div>
      <section class="section">
        <h3>Lightroom 预览</h3>
        <div class="preview-card">
          <div class="preview-pane" data-label="修图前">
            <img id="previewImage" alt="Lightroom 修图前预览" />
            <div id="previewEmpty" class="preview-empty">等待 Lightroom 当前照片预览。<br />照片来源是 Lightroom 当前选中项，请直接在 Lightroom 中选择照片。</div>
          </div>
          <div class="preview-pane" data-label="修图后">
            <img id="afterPreviewImage" alt="Lightroom 修图后预览" />
            <div id="afterPreviewEmpty" class="preview-empty">发送调色请求后显示修图后预览。</div>
          </div>
        </div>
      </section>
      <section class="section">
        <h3>当前照片</h3>
        <div class="kv" id="photoMeta">
          <span>状态</span><strong>读取中...</strong>
        </div>
      </section>
      <section class="section">
        <h3>Agent 判断</h3>
        <div id="analysisSummary" class="analysis-box">
          <div>发送调色请求后，Agent 会根据你的意图和当前照片动态判断照片类型、修图方向和参数策略。</div>
        </div>
      </section>
      <section class="section">
        <h3>版本历史</h3>
        <div id="versionList" class="version-list">
          <div class="preview-empty">暂无修图版本。</div>
        </div>
      </section>
    </aside>
    <main>
      <header class="topbar">
        <div>
          <h1>Agent 对话修图</h1>
          <h2>基于 Lightroom 当前选中照片持续微调</h2>
        </div>
        <div id="status" class="status">连接中...</div>
      </header>
      <section id="chat" class="chat">
        <div class="message agent"><span class="role">Agent</span>选择 Lightroom 照片后，直接告诉我你想要的效果。我会先分析意图和照片类型，再应用调色参数；之后你可以继续说“再亮一点”“肤色更通透”“绿色别那么脏”。</div>
      </section>
      <footer class="composer">
        <div class="quick-actions">
          <button type="button" class="quick-action" data-prompt="自然优化，保持真实，只调整必要参数">自然优化</button>
          <button type="button" class="quick-action" data-prompt="夜景电影感，压高光，提暗部，增强氛围">夜景电影感</button>
          <button type="button" class="quick-action" data-prompt="再亮一点，但不要改变白平衡">再亮一点</button>
          <button type="button" class="quick-action" data-prompt="绿色别那么脏，降低绿色饱和度，保持画面自然">绿色干净</button>
          <button type="button" class="quick-action" data-prompt="胶片感，轻微颗粒和暗角，不改变白平衡">胶片感</button>
        </div>
        <textarea id="prompt" placeholder="例如：先分析这张照片，修成夜景电影感，再亮一点">先分析这张照片，修成夜景电影感，再亮一点</textarea>
        <div class="actions">
          <select id="provider">
            <option value="rule">本地规则</option>
            <option value="openai">OpenAI</option>
            <option value="qwen2">阿里 Qwen2</option>
          </select>
          <button id="send" class="primary">发送并修图</button>
        </div>
      </footer>
    </main>
  </div>
  <script>
    const chat = document.querySelector('#chat')
    const statusNode = document.querySelector('#status')
    const photoMeta = document.querySelector('#photoMeta')
    const previewImage = document.querySelector('#previewImage')
    const afterPreviewImage = document.querySelector('#afterPreviewImage')
    const beforePreviewPane = previewImage.closest('.preview-pane')
    const afterPreviewPane = afterPreviewImage.closest('.preview-pane')
    const previewEmpty = document.querySelector('#previewEmpty')
    const afterPreviewEmpty = document.querySelector('#afterPreviewEmpty')
    const analysisSummary = document.querySelector('#analysisSummary')
    const versionList = document.querySelector('#versionList')
    const quickAction = document.querySelectorAll('.quick-action')
    const prompt = document.querySelector('#prompt')
    const provider = document.querySelector('#provider')
    const send = document.querySelector('#send')
    let currentPhoto = null
    let sessionId = null
    let currentPreviewUrl = null
    let lastStablePreviewUrl = null
    let comparisonActive = false
    let comparisonPhotoName = ''
    let comparisonPhotoKey = ''

    function escapeHtml(value) {
      return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]))
    }

    function addMessage(role, html) {
      const node = document.createElement('div')
      node.className = 'message ' + role
      node.innerHTML = '<span class="role">' + (role === 'user' ? '你' : 'Agent') + '</span>' + html
      chat.appendChild(node)
      chat.scrollTop = chat.scrollHeight
    }

    function setComparisonLabels(photoName, versionLabel) {
      const name = photoName || '当前照片'
      beforePreviewPane.dataset.label = '修图前 · ' + name
      afterPreviewPane.dataset.label = '修图后 · ' + name + (versionLabel ? ' · ' + versionLabel : '')
    }

    function showComparison(beforeUrl, afterUrl, photoName, photoKey, versionLabel) {
      const activePhotoKey = currentPhoto?.session?.photoKey || ''
      if (activePhotoKey && photoKey && activePhotoKey !== photoKey) {
        return
      }
      comparisonActive = Boolean(beforeUrl || afterUrl)
      comparisonPhotoName = photoName || comparisonPhotoName
      comparisonPhotoKey = photoKey || comparisonPhotoKey
      setComparisonLabels(comparisonPhotoName, versionLabel)
      if (beforeUrl) {
        previewImage.src = beforeUrl
        previewImage.style.display = 'block'
        previewEmpty.style.display = 'none'
      }
      if (afterUrl) {
        afterPreviewImage.src = afterUrl
        afterPreviewImage.style.display = 'block'
        afterPreviewEmpty.style.display = 'none'
      } else {
        afterPreviewImage.style.display = 'none'
        afterPreviewImage.removeAttribute('src')
        afterPreviewEmpty.style.display = 'block'
      }
    }

    function renderSession(session, photoName) {
      if (!session) {
        versionList.innerHTML = '<div class="preview-empty">暂无修图版本。</div>'
        return
      }
      const activePhotoKey = currentPhoto?.session?.photoKey || session.photoKey || ''
      if (activePhotoKey && session.photoKey && activePhotoKey !== session.photoKey) {
        return
      }
      const currentVersion = session.currentVersion || null
      if (session.baselinePreviewUrl) {
        showComparison(session.baselinePreviewUrl, currentVersion?.previewUrl || '', photoName, session.photoKey, currentVersion?.versionId || '')
      }
      const versions = (Array.isArray(session.versions) ? session.versions : []).filter((version) => {
        return !version.photoKey || version.photoKey === session.photoKey
      })
      if (!versions.length) {
        versionList.innerHTML = '<div class="preview-empty">暂无修图版本。</div>'
        return
      }
      versionList.innerHTML = versions.map((version) => {
        const title = version.userIntent || version.agentSummary || 'Agent 修图'
        const deltaCount = Array.isArray(version.deltas) ? version.deltas.length : 0
        const time = version.createdAt ? new Date(version.createdAt).toLocaleString() : ''
        return '<div class="version-item"><div><strong>' + escapeHtml(title) + '</strong><span>' + escapeHtml(time) + ' · ' + deltaCount + ' 个参数</span></div><button type="button" data-version-id="' + escapeHtml(version.versionId) + '">查看</button></div>'
      }).join('')
      versionList.querySelectorAll('button[data-version-id]').forEach(button => {
        button.addEventListener('click', () => {
          const version = versions.find(item => item.versionId === button.dataset.versionId)
          if (version) {
            if (version.photoKey !== session.photoKey) return
            showComparison(session.baselinePreviewUrl, version.previewUrl, photoName, session.photoKey, version.versionId)
          }
        })
      })
    }

    function renderPhoto(payload) {
      currentPhoto = payload
      if (!payload.available) {
        statusNode.className = 'status warn'
        statusNode.textContent = payload.message || '未选中照片'
        photoMeta.innerHTML = '<span>状态</span><strong>' + escapeHtml(payload.message || '未选中照片') + '</strong>'
        versionList.innerHTML = '<div class="preview-empty">暂无修图版本。</div>'
        comparisonActive = false
        comparisonPhotoName = ''
        comparisonPhotoKey = ''
        setComparisonLabels('', '')
        previewImage.style.display = 'none'
        afterPreviewImage.style.display = 'none'
        previewImage.removeAttribute('src')
        afterPreviewImage.removeAttribute('src')
        currentPreviewUrl = null
        previewEmpty.style.display = 'block'
        afterPreviewEmpty.style.display = 'block'
        previewEmpty.textContent = '未读取到 Lightroom 当前照片。请在 Lightroom 中选择照片。'
        afterPreviewEmpty.textContent = '发送调色请求后显示修图后预览。'
        return
      }
      statusNode.className = 'status ok'
      statusNode.textContent = 'Lightroom 已选中：' + (payload.photo.fileName || '当前照片')
      const p = payload.photo || {}
      if (comparisonPhotoName && comparisonPhotoName !== (p.fileName || '')) {
          comparisonActive = false
          comparisonPhotoName = ''
          comparisonPhotoKey = ''
          setComparisonLabels(p.fileName || '', '')
          afterPreviewImage.style.display = 'none'
        afterPreviewImage.removeAttribute('src')
        afterPreviewEmpty.style.display = 'block'
        afterPreviewEmpty.textContent = '发送调色请求后显示修图后预览。'
      }
      renderSession(payload.session, p.fileName || '')
      if (payload.session?.baselinePreviewUrl) {
        previewImage.style.display = 'block'
        previewEmpty.style.display = 'none'
      } else if (payload.previewUrl) {
        if (!comparisonActive && payload.previewUrl !== currentPreviewUrl) {
          currentPreviewUrl = payload.previewUrl
          lastStablePreviewUrl = payload.previewUrl
          previewImage.src = payload.previewUrl
        }
        previewImage.style.display = 'block'
        previewEmpty.style.display = 'none'
      } else if (lastStablePreviewUrl) {
        previewImage.style.display = 'block'
        previewEmpty.style.display = 'none'
      } else {
        previewImage.style.display = 'none'
        currentPreviewUrl = null
        previewEmpty.style.display = 'block'
        previewEmpty.textContent = '正在等待 Lightroom 导出预览。'
      }
      photoMeta.innerHTML = [
        ['文件', p.fileName],
        ['格式', p.fileFormat],
        ['相机', p.camera],
        ['镜头', p.lens],
        ['ISO', p.iso],
        ['尺寸', p.dimensions]
      ].map(([k, v]) => '<span>' + k + '</span><strong>' + escapeHtml(v || '-') + '</strong>').join('')
    }

    async function pollPhoto() {
      try {
        const response = await fetch('/api/lightroom/selected-photo')
        renderPhoto(await response.json())
      } catch (error) {
        statusNode.className = 'status warn'
        statusNode.textContent = '无法读取 Lightroom 状态'
      }
    }

    function renderAgentResult(data) {
      const analysis = data.analysis || {}
      const basis = (analysis.basis || []).map(item => '<li>' + escapeHtml(item) + '</li>').join('')
      const deltas = (data.deltas || []).map(delta => '<div class="delta"><strong>' + escapeHtml(delta.label || delta.name) + '</strong><span>' + escapeHtml(delta.beforeValue) + ' -> ' + escapeHtml(delta.afterValue) + '（' + escapeHtml(delta.delta) + '）</span></div>').join('')
      if (data.session) {
        renderSession(data.session, data.photo?.fileName || currentPhoto?.photo?.fileName || '')
      }
      if (data.beforePreviewUrl || data.afterPreviewUrl) {
        showComparison(data.beforePreviewUrl, data.afterPreviewUrl, data.photo?.fileName || currentPhoto?.photo?.fileName || '', data.session?.photoKey || '', data.version?.versionId || '')
      }
      if (data.beforePreviewUrl || data.afterPreviewUrl) {
        comparisonActive = true
        comparisonPhotoName = data.photo?.fileName || currentPhoto?.photo?.fileName || ''
        comparisonPhotoKey = data.session?.photoKey || comparisonPhotoKey
        if (data.beforePreviewUrl) {
          previewImage.src = data.beforePreviewUrl
          previewImage.style.display = 'block'
          previewEmpty.style.display = 'none'
        }
        if (data.afterPreviewUrl) {
          afterPreviewImage.src = data.afterPreviewUrl
          afterPreviewImage.style.display = 'block'
          afterPreviewEmpty.style.display = 'none'
        } else {
          afterPreviewImage.style.display = 'none'
          afterPreviewEmpty.style.display = 'block'
          afterPreviewEmpty.textContent = 'Lightroom 已应用参数，正在等待修图后预览。'
        }
      }
      analysisSummary.innerHTML = [
        '<div><strong>意图</strong><br />' + escapeHtml(analysis.intent || '-') + '</div>',
        '<div><strong>照片类型</strong><br />' + escapeHtml(analysis.photoType || '-') + '</div>',
        '<div><strong>修图方向</strong><br />' + escapeHtml(analysis.recommendedStyle || '-') + '</div>'
      ].join('')
      addMessage('agent', [
        '<div>意图分析：' + escapeHtml(analysis.intent || '-') + '</div>',
        '<div>照片类型：' + escapeHtml(analysis.photoType || '-') + '</div>',
        '<div>建议风格：' + escapeHtml(analysis.recommendedStyle || '-') + '</div>',
        basis ? '<ul>' + basis + '</ul>' : '',
        '<div>' + escapeHtml(data.assistantMessage || '') + '</div>',
        deltas ? '<div class="deltas">' + deltas + '</div>' : ''
      ].join(''))
    }

    async function sendMessage() {
      const message = prompt.value.trim()
      if (!message) return
      addMessage('user', escapeHtml(message))
      send.disabled = true
      statusNode.className = 'status'
      statusNode.textContent = 'Agent 正在分析并修图...'
      try {
        const response = await fetch('/api/lightroom-agent/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ message, provider: provider.value, sessionId })
        })
        const result = await response.json()
        if (!result.success) {
          addMessage('agent', '没有完成修图：' + escapeHtml(result.message))
          return
        }
        sessionId = result.data.sessionId
        renderAgentResult(result.data)
        prompt.value = ''
        await pollPhoto()
      } catch (error) {
        addMessage('agent', '调用失败：' + escapeHtml(error.message))
      } finally {
        send.disabled = false
        statusNode.className = currentPhoto?.available ? 'status ok' : 'status warn'
      }
    }

    quickAction.forEach(button => {
      button.addEventListener('click', () => {
        prompt.value = button.dataset.prompt || ''
        sendMessage()
      })
    })
    send.addEventListener('click', sendMessage)
    prompt.addEventListener('keydown', event => {
      if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) sendMessage()
    })
    pollPhoto()
    setInterval(pollPhoto, 1500)
  </script>
</body>
</html>`
}
