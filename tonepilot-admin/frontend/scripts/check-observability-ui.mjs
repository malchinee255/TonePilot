import { readFileSync } from 'node:fs'

const vue = readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')

const requiredSnippets = [
  'runtimeTree',
  'eventTypeLabel',
  '用户',
  '会话',
  'Trace',
  '事件',
  '<el-tree',
  '主 Agent 判断',
  '模型返回',
  'Lightroom 工具',
  'runtimeEventViewMode',
  'NOISE_EVENT_TYPES',
  'runtimeExecutions',
  'buildRuntimeExecutionSummaries',
  'visibleRuntimeEvents'
]

const missing = requiredSnippets.filter(snippet => !vue.includes(snippet))
if (missing.length > 0) {
  console.error(`观测界面缺少可读树形能力：${missing.join(', ')}`)
  process.exit(1)
}
