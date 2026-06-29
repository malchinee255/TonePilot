const assert = require('assert')
const test = require('node:test')

const { createRuleTune } = require('../src/local-rule-agent')

test('本地规则只修改用户明确表达的参数方向，不默认修改白平衡', () => {
  const result = createRuleTune({
    sessionId: 's-1',
    localPhotoId: 'DSCF1719.RAF',
    message: '先分析这张照片，修成夜景电影感，再亮一点',
    currentAdjustment: {
      basic: {
        exposure: 0,
        shadows: 0,
        highlights: 0,
        contrast: 0,
        temperature: 4200,
        tint: 17
      }
    }
  })

  assert.strictEqual(result.success, true)
  assert.strictEqual(result.data.developSettings.Temperature, undefined)
  assert.strictEqual(result.data.developSettings.Tint, undefined)
  assert.ok(result.data.developSettings.Exposure2012 > 0)
  assert.ok(result.data.developSettings.Shadows2012 > 0)
  assert.ok(result.data.developSettings.Highlights2012 < 0)
})

test('本地规则在用户明确要求更暖时才修改色温', () => {
  const result = createRuleTune({
    sessionId: 's-2',
    localPhotoId: 'portrait.raw',
    message: '整体更暖一点，肤色自然',
    currentAdjustment: {
      basic: {
        temperature: 5000,
        tint: 0,
        orangeLuminance: 0
      }
    }
  })

  assert.strictEqual(result.data.developSettings.Temperature, 5300)
  assert.strictEqual(result.data.developSettings.Tint, undefined)
})

test('本地规则支持更多 Lightroom 全局参数族', () => {
  const result = createRuleTune({
    sessionId: 's-3',
    localPhotoId: 'landscape.raw',
    message: '胶片感，颗粒，暗角，蓝色更深，绿色别那么脏，轻微锐化，开启镜头校正',
    currentAdjustment: { basic: {}, hsl: {}, effects: {}, detail: {}, lensCorrections: {} }
  })

  const settings = result.data.developSettings

  assert.ok(settings.GrainAmount > 0)
  assert.ok(settings.PostCropVignetteAmount < 0)
  assert.ok(settings.BlueSaturation < 0)
  assert.ok(settings.GreenSaturation < 0)
  assert.ok(settings.Sharpness > 0)
  assert.strictEqual(settings.EnableProfileCorrections, true)
})
