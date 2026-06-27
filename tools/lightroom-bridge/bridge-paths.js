const os = require('os')
const path = require('path')

function createBridgePaths(options = {}) {
  const env = options.env || process.env
  const fsRoot = resolveRoot(env.TONEPILOT_LIGHTROOM_BRIDGE_ROOT || path.join(os.homedir(), '.tonepilot-lightroom-bridge'))
  const lightroomRoot = trimTrailingSeparator(env.TONEPILOT_LIGHTROOM_BRIDGE_LIGHTROOM_ROOT || fsRoot)

  return {
    fsRoot,
    lightroomRoot,
    fs: (...segments) => path.join(fsRoot, ...segments),
    lightroom: (...segments) => joinLightroomPath(lightroomRoot, segments)
  }
}

function resolveRoot(value) {
  const text = String(value || '')
  if (usesWindowsSeparator(text)) {
    return trimTrailingSeparator(text)
  }
  return path.resolve(text)
}

function joinLightroomPath(root, segments) {
  const normalized = segments
    .filter((segment) => segment !== undefined && segment !== null && String(segment).length > 0)
    .map((segment) => String(segment).replace(/^[/\\]+|[/\\]+$/g, ''))

  if (usesWindowsSeparator(root)) {
    return [trimTrailingSeparator(root), ...normalized].join('\\')
  }
  return path.join(root, ...normalized)
}

function trimTrailingSeparator(value) {
  const text = String(value || '')
  if (/^[a-zA-Z]:[\\/]?$/.test(text)) {
    return text.replace('/', '\\')
  }
  return text.replace(/[/\\]+$/g, '')
}

function usesWindowsSeparator(value) {
  return /^[a-zA-Z]:[\\/]/.test(value) || String(value).includes('\\')
}

module.exports = {
  createBridgePaths
}
