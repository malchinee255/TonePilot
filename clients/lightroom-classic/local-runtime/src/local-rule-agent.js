const LIMITS = {
  Exposure2012: [-5, 5],
  Contrast2012: [-100, 100],
  Highlights2012: [-100, 100],
  Shadows2012: [-100, 100],
  Whites2012: [-100, 100],
  Blacks2012: [-100, 100],
  Texture: [-100, 100],
  Clarity2012: [-100, 100],
  Dehaze: [-100, 100],
  Vibrance: [-100, 100],
  Saturation: [-100, 100],
  Temperature: [2000, 50000],
  Tint: [-150, 150],
  ParametricShadows: [-100, 100],
  ParametricDarks: [-100, 100],
  ParametricLights: [-100, 100],
  ParametricHighlights: [-100, 100],
  Sharpness: [0, 150],
  LuminanceSmoothing: [0, 100],
  ColorNoiseReduction: [0, 100],
  GrainAmount: [0, 100],
  GrainSize: [0, 100],
  GrainFrequency: [0, 100],
  PostCropVignetteAmount: [-100, 100],
  PostCropVignetteMidpoint: [0, 100],
  BlueSaturation: [-100, 100],
  BlueLuminance: [-100, 100],
  GreenSaturation: [-100, 100],
  GreenLuminance: [-100, 100],
  OrangeSaturation: [-100, 100],
  OrangeLuminance: [-100, 100],
  SplitToningShadowHue: [0, 360],
  SplitToningShadowSaturation: [0, 100],
  SplitToningHighlightHue: [0, 360],
  SplitToningHighlightSaturation: [0, 100],
  PerspectiveVertical: [-100, 100],
  PerspectiveHorizontal: [-100, 100],
  PerspectiveRotate: [-10, 10],
  RedPrimaryHue: [-100, 100],
  RedPrimarySaturation: [-100, 100],
  GreenPrimaryHue: [-100, 100],
  GreenPrimarySaturation: [-100, 100],
  BluePrimaryHue: [-100, 100],
  BluePrimarySaturation: [-100, 100]
}

const CURRENT_KEYS = {
  Exposure2012: ['basic', 'exposure'],
  Contrast2012: ['basic', 'contrast'],
  Highlights2012: ['basic', 'highlights'],
  Shadows2012: ['basic', 'shadows'],
  Whites2012: ['basic', 'whites'],
  Blacks2012: ['basic', 'blacks'],
  Texture: ['basic', 'texture'],
  Clarity2012: ['basic', 'clarity'],
  Dehaze: ['basic', 'dehaze'],
  Vibrance: ['basic', 'vibrance'],
  Saturation: ['basic', 'saturation'],
  Temperature: ['basic', 'temperature'],
  Tint: ['basic', 'tint'],
  Sharpness: ['detail', 'sharpness'],
  LuminanceSmoothing: ['detail', 'luminanceSmoothing'],
  ColorNoiseReduction: ['detail', 'colorNoiseReduction'],
  GrainAmount: ['effects', 'grain'],
  GrainSize: ['effects', 'grainSize'],
  GrainFrequency: ['effects', 'grainFrequency'],
  PostCropVignetteAmount: ['effects', 'vignette'],
  BlueSaturation: ['hsl', 'blueSaturation'],
  BlueLuminance: ['hsl', 'blueLuminance'],
  GreenSaturation: ['hsl', 'greenSaturation'],
  GreenLuminance: ['hsl', 'greenLuminance'],
  OrangeSaturation: ['hsl', 'orangeSaturation'],
  OrangeLuminance: ['hsl', 'orangeLuminance']
}

const LABELS = {
  Exposure2012: '曝光',
  Contrast2012: '对比度',
  Highlights2012: '高光',
  Shadows2012: '阴影',
  Whites2012: '白色色阶',
  Blacks2012: '黑色色阶',
  Texture: '纹理',
  Clarity2012: '清晰度',
  Dehaze: '去朦胧',
  Vibrance: '自然饱和度',
  Saturation: '饱和度',
  Temperature: '色温',
  Tint: '色调',
  Sharpness: '锐化',
  LuminanceSmoothing: '明亮度降噪',
  ColorNoiseReduction: '颜色降噪',
  GrainAmount: '颗粒',
  GrainSize: '颗粒大小',
  GrainFrequency: '颗粒粗糙度',
  PostCropVignetteAmount: '暗角',
  BlueSaturation: '蓝色饱和度',
  BlueLuminance: '蓝色明亮度',
  GreenSaturation: '绿色饱和度',
  GreenLuminance: '绿色明亮度',
  OrangeSaturation: '橙色饱和度',
  OrangeLuminance: '橙色明亮度',
  EnableProfileCorrections: '镜头配置文件校正'
}

function createRuleTune({ sessionId, localPhotoId, message, currentAdjustment }) {
  const text = String(message || '')
  const current = currentAdjustment || {}
  const operations = []

  if (matches(text, ['亮', '提亮', '太暗', 'brighter'])) {
    operations.push(change('Exposure2012', 0.18), change('Shadows2012', 14))
  }
  if (matches(text, ['暗一点', '压暗', '太亮', '降低亮度'])) {
    operations.push(change('Exposure2012', -0.15), change('Highlights2012', -16))
  }
  if (matches(text, ['降低高光', '压高光', '高光'])) {
    operations.push(change('Highlights2012', -22))
  }
  if (matches(text, ['电影', 'cinematic', '夜景', '氛围'])) {
    operations.push(
      change('Highlights2012', -14),
      change('Shadows2012', 10),
      change('Contrast2012', 18),
      change('Dehaze', 5),
      change('PostCropVignetteAmount', -8)
    )
  }
  if (matches(text, ['柔和', '日系', '干净'])) {
    operations.push(change('Contrast2012', -8), change('Highlights2012', -12), change('Vibrance', 6))
  }
  if (matches(text, ['胶片', 'film', '复古'])) {
    operations.push(
      change('Blacks2012', 10),
      change('Saturation', -6),
      setValue('GrainAmount', 18),
      setValue('GrainSize', 24),
      setValue('GrainFrequency', 48),
      change('PostCropVignetteAmount', -10)
    )
  }
  if (matches(text, ['暖', '更暖', '偏暖'])) {
    operations.push(change('Temperature', 300))
  }
  if (matches(text, ['冷', '更冷', '偏冷'])) {
    operations.push(change('Temperature', -300))
  }
  if (matches(text, ['洋红', '粉'])) {
    operations.push(change('Tint', 6))
  }
  if (matches(text, ['绿色', '绿', '脏'])) {
    operations.push(change('GreenSaturation', -12), change('GreenLuminance', 6))
  }
  if (matches(text, ['蓝色', '蓝'])) {
    operations.push(change('BlueSaturation', -8), change('BlueLuminance', -6))
  }
  if (matches(text, ['肤色', '人像'])) {
    operations.push(change('OrangeLuminance', 8), change('OrangeSaturation', -4), change('Texture', -6))
  }
  if (matches(text, ['锐化', '清晰'])) {
    operations.push(change('Sharpness', 12), change('Texture', 4))
  }
  if (matches(text, ['降噪', '噪点'])) {
    operations.push(change('LuminanceSmoothing', 18), change('ColorNoiseReduction', 12))
  }
  if (matches(text, ['镜头校正', '镜头配置'])) {
    operations.push({ setting: 'EnableProfileCorrections', mode: 'set', value: true })
  }
  if (matches(text, ['垂直校正'])) {
    operations.push(change('PerspectiveVertical', 8))
  }

  const developSettings = buildDevelopSettings(current, operations)
  const deltas = buildDeltas(current, developSettings)

  return {
    success: true,
    data: {
      sessionId: sessionId || `local-rule-${Date.now()}`,
      localPhotoId: localPhotoId || '',
      assistantMessage: summarizeRuleTune(text, deltas),
      deltas,
      developSettings,
      runtimeProvider: 'rule'
    }
  }
}

function buildDevelopSettings(current, operations) {
  const result = {}
  for (const operation of operations) {
    if (!operation || result[operation.setting] !== undefined) continue
    const beforeValue = currentValue(current, operation.setting)
    const afterValue = operation.mode === 'set'
      ? operation.value
      : clamp(operation.setting, Number(beforeValue || 0) + operation.delta)
    if (beforeValue !== afterValue) {
      result[operation.setting] = afterValue
    }
  }
  return result
}

function buildDeltas(current, developSettings) {
  return Object.entries(developSettings).map(([setting, afterValue]) => {
    const beforeValue = currentValue(current, setting)
    return {
      group: groupFor(setting),
      name: setting,
      label: LABELS[setting] || setting,
      beforeValue,
      afterValue,
      delta: typeof afterValue === 'number' && typeof beforeValue === 'number'
        ? Number((afterValue - beforeValue).toFixed(2))
        : afterValue,
      reason: '根据用户本轮指令生成，只输出需要修改的 Lightroom 参数。'
    }
  })
}

function currentValue(current, setting) {
  const key = CURRENT_KEYS[setting]
  if (!key) {
    return typeof setting === 'string' && setting.startsWith('Enable') ? false : 0
  }
  const value = current?.[key[0]]?.[key[1]]
  return value === undefined || value === null || value === '' ? 0 : value
}

function groupFor(setting) {
  if (setting.includes('Saturation') || setting.includes('Luminance') || setting.includes('Hue')) return 'hsl'
  if (setting.includes('Grain') || setting.includes('Vignette')) return 'effects'
  if (setting.includes('Sharp') || setting.includes('Noise')) return 'detail'
  if (setting.includes('Perspective')) return 'transform'
  if (setting.startsWith('Enable')) return 'lensCorrections'
  return 'basic'
}

function summarizeRuleTune(message, deltas) {
  const names = deltas.map((delta) => delta.label).join('、')
  if (!names) {
    return '我先保留当前参数，没有检测到需要明确修改的全局调色项。你可以继续说明要更亮、更暖、电影感、胶片感或降低某类颜色。'
  }
  return `已根据「${message}」微调 ${deltas.length} 个参数：${names}。未被明确要求的参数保持不变。`
}

function matches(text, words) {
  return words.some((word) => String(text).toLowerCase().includes(String(word).toLowerCase()))
}

function change(setting, delta) {
  return { setting, mode: 'change', delta }
}

function setValue(setting, value) {
  return { setting, mode: 'set', value }
}

function clamp(setting, value) {
  const [min, max] = LIMITS[setting] || [-100, 100]
  return Math.max(min, Math.min(max, Number(value.toFixed ? value.toFixed(2) : value)))
}

module.exports = {
  createRuleTune,
  buildDevelopSettings
}
