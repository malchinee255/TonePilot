const assert = require('assert')
const path = require('path')
const test = require('node:test')

const { createBridgePaths } = require('./bridge-paths')

test('为 WSL Bridge 生成文件系统路径和 Lightroom Windows 路径', () => {
  const paths = createBridgePaths({
    env: {
      TONEPILOT_LIGHTROOM_BRIDGE_ROOT: '/mnt/c/Users/lvchanghong/.tonepilot-lightroom-bridge',
      TONEPILOT_LIGHTROOM_BRIDGE_LIGHTROOM_ROOT: 'C:\\Users\\lvchanghong\\.tonepilot-lightroom-bridge'
    }
  })

  const expectedFsRoot = path.resolve('/mnt/c/Users/lvchanghong/.tonepilot-lightroom-bridge')
  assert.strictEqual(paths.fsRoot, expectedFsRoot)
  assert.strictEqual(paths.lightroomRoot, 'C:\\Users\\lvchanghong\\.tonepilot-lightroom-bridge')
  assert.strictEqual(paths.fs('results', 'session-1.jpg'), path.join(expectedFsRoot, 'results', 'session-1.jpg'))
  assert.strictEqual(paths.lightroom('results', 'session-1.jpg'), 'C:\\Users\\lvchanghong\\.tonepilot-lightroom-bridge\\results\\session-1.jpg')
})

test('未配置 Lightroom 路径时默认复用 Bridge 文件系统路径', () => {
  const paths = createBridgePaths({
    env: {
      TONEPILOT_LIGHTROOM_BRIDGE_ROOT: 'C:\\Users\\lvchanghong\\.tonepilot-lightroom-bridge'
    }
  })

  assert.strictEqual(paths.lightroomRoot, paths.fsRoot)
  assert.strictEqual(paths.lightroom('jobs', 'abc.lua'), 'C:\\Users\\lvchanghong\\.tonepilot-lightroom-bridge\\jobs\\abc.lua')
})
