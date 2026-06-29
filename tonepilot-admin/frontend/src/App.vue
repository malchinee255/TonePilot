<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">TP</div>
        <div>
          <h1>TonePilot</h1>
          <p>调色风格管理端</p>
        </div>
      </div>

      <el-menu :default-active="activeView" class="nav-menu" @select="activeView = $event">
        <el-menu-item index="styles">
          <el-icon><Operation /></el-icon>
          <span>风格库</span>
        </el-menu-item>
        <el-menu-item index="knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="samples">
          <el-icon><Picture /></el-icon>
          <span>样片管理</span>
        </el-menu-item>
        <el-menu-item index="observability">
          <el-icon><DataLine /></el-icon>
          <span>观测评估</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="workspace">
      <section class="topbar">
        <div>
          <h2>{{ pageTitle }}</h2>
          <p>{{ pageSubtitle }}</p>
        </div>
        <el-tag effect="plain" round>用户端：Lightroom Classic 插件</el-tag>
      </section>

      <section v-if="activeView === 'styles'" class="grid two">
        <div class="panel">
          <div class="panel-title">
            <h3>创建风格</h3>
            <el-button :icon="Plus" type="primary" @click="createStyle">保存</el-button>
          </div>
          <el-form label-position="top">
            <el-form-item label="风格名称">
              <el-input v-model="styleForm.styleName" placeholder="例如：夜景电影感" />
            </el-form-item>
            <el-form-item label="风格编码">
              <el-input v-model="styleForm.styleCode" placeholder="例如：night_cinematic" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="styleForm.description" type="textarea" :rows="4" />
            </el-form-item>
            <el-form-item label="适用场景">
              <el-input v-model="styleForm.suitableScenes" placeholder="用逗号分隔" />
            </el-form-item>
            <el-form-item label="避免场景">
              <el-input v-model="styleForm.avoidScenes" placeholder="用逗号分隔" />
            </el-form-item>
          </el-form>
        </div>

        <div class="panel">
          <div class="panel-title">
            <h3>风格列表</h3>
            <el-button :icon="Refresh" @click="loadStyles">刷新</el-button>
          </div>
          <el-table :data="styles" height="560" @row-click="selectStyle">
            <el-table-column prop="styleName" label="名称" min-width="160" />
            <el-table-column prop="styleCode" label="编码" min-width="180" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column width="76">
              <template #default="{ row }">
                <el-button :icon="Delete" circle text @click.stop="deleteStyle(row.id)" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section v-if="activeView === 'knowledge'" class="grid two">
        <div class="panel">
          <div class="panel-title">
            <h3>新增调色知识</h3>
            <el-button :icon="Plus" type="primary" @click="createKnowledge">保存</el-button>
          </div>
          <el-form label-position="top">
            <el-form-item label="标题">
              <el-input v-model="knowledgeForm.title" />
            </el-form-item>
            <el-form-item label="场景">
              <el-input v-model="knowledgeForm.scene" />
            </el-form-item>
            <el-form-item label="目标风格">
              <el-input v-model="knowledgeForm.targetStyle" />
            </el-form-item>
            <el-form-item label="常见问题">
              <el-input v-model="knowledgeForm.problems" placeholder="用逗号或换行分隔" />
            </el-form-item>
            <el-form-item label="调色策略">
              <el-input v-model="knowledgeForm.strategy" type="textarea" :rows="5" />
            </el-form-item>
          </el-form>
        </div>

        <div class="panel">
          <div class="panel-title">
            <h3>知识审核</h3>
            <div class="title-actions">
              <el-select v-model="knowledgeStatus" class="status-select" @change="loadAdminKnowledge">
                <el-option label="全部" value="" />
                <el-option label="待审核" value="pending" />
                <el-option label="已启用" value="approved" />
                <el-option label="已拒绝" value="rejected" />
              </el-select>
              <el-button :icon="Refresh" @click="loadAdminKnowledge">刷新</el-button>
            </div>
          </div>
          <el-table :data="adminKnowledge" height="560">
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="scene" label="场景" width="130" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column width="138">
              <template #default="{ row }">
                <el-button :icon="CircleCheck" circle text @click="approveKnowledge(row.id)" />
                <el-button :icon="Close" circle text @click="rejectKnowledge(row.id)" />
                <el-button :icon="SwitchButton" circle text @click="disableKnowledge(row.id)" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section v-if="activeView === 'samples'" class="grid two">
        <div class="panel">
          <div class="panel-title">
            <h3>上传风格样片</h3>
            <el-button :icon="Upload" type="primary" @click="uploadSample">上传</el-button>
          </div>
          <el-alert
            class="inline-alert"
            type="info"
            :closable="false"
            title="样片只用于管理端沉淀风格知识；用户修图入口在 Lightroom 插件中。"
          />
          <el-form label-position="top">
            <el-form-item label="所属风格">
              <el-select v-model="sampleForm.styleId" placeholder="选择风格">
                <el-option
                  v-for="style in styles"
                  :key="style.id"
                  :label="style.styleName"
                  :value="style.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="样片说明">
              <el-input v-model="sampleForm.description" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="标签">
              <el-input v-model="sampleForm.tags" placeholder="夜景, 人像, 霓虹" />
            </el-form-item>
            <el-form-item label="成片">
              <el-upload :auto-upload="false" :limit="1" :on-change="handleSampleFileChange">
                <el-button :icon="Picture">选择文件</el-button>
              </el-upload>
            </el-form-item>
          </el-form>
        </div>

        <div class="panel">
          <div class="panel-title">
            <h3>样片列表</h3>
            <div class="title-actions">
              <el-button :icon="Refresh" @click="loadSamples">刷新</el-button>
            </div>
          </div>
          <el-table :data="samples" height="560">
            <el-table-column prop="description" label="说明" min-width="180" />
            <el-table-column prop="sampleType" label="类型" width="120" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column width="150">
              <template #default="{ row }">
                <el-button size="small" @click="analyzeSample(row.id)">分析</el-button>
                <el-button size="small" @click="generateKnowledge(row.id)">生成知识</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section v-if="activeView === 'observability'" class="grid two">
        <div class="panel">
          <div class="panel-title">
            <h3>LLM 调用</h3>
            <el-button :icon="Refresh" @click="loadObservability">刷新</el-button>
          </div>
          <el-table :data="llmCalls" height="560">
            <el-table-column prop="provider" label="模型厂商" width="120" />
            <el-table-column prop="model" label="模型" width="150" />
            <el-table-column prop="purpose" label="用途" min-width="160" />
            <el-table-column prop="success" label="成功" width="90" />
          </el-table>
        </div>

        <div class="panel">
          <div class="panel-title">
            <h3>审计事件</h3>
            <el-button :icon="DataAnalysis" @click="runBenchmark">运行评估</el-button>
          </div>
          <el-table :data="auditEvents" height="480">
            <el-table-column prop="eventType" label="事件" width="150" />
            <el-table-column prop="actor" label="来源" width="130" />
            <el-table-column prop="summary" label="摘要" min-width="220" />
          </el-table>
          <div v-if="benchmarkSummary" class="benchmark-box">
            {{ benchmarkSummary }}
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import {
  CircleCheck,
  Close,
  Collection,
  DataAnalysis,
  DataLine,
  Delete,
  Operation,
  Picture,
  Plus,
  Refresh,
  SwitchButton,
  Upload
} from '@element-plus/icons-vue'
import { api, unwrap } from './api'

const activeView = ref('styles')
const styles = ref<any[]>([])
const adminKnowledge = ref<any[]>([])
const samples = ref<any[]>([])
const llmCalls = ref<any[]>([])
const auditEvents = ref<any[]>([])
const knowledgeStatus = ref('')
const sampleFile = ref<File | undefined>()
const benchmarkSummary = ref('')

const styleForm = reactive({
  styleName: '夜景电影感',
  styleCode: 'night_cinematic',
  description: '压住高光、提亮暗部、增强对比和空气感，适合城市夜景与霓虹人像。',
  suitableScenes: '城市夜景, 霓虹, 夜景人像',
  avoidScenes: '高调日系, 儿童写真'
})

const knowledgeForm = reactive({
  title: '夜景电影感调色策略',
  scene: '城市夜景',
  targetStyle: '夜景电影感',
  problems: '灯光高光过亮, 暗部细节不足, 绿色偏脏',
  strategy: '降低高光保留灯牌细节\n轻微提升阴影恢复暗部\n增加对比和去朦胧\n降低绿色饱和度并增加暗角聚焦'
})

const sampleForm = reactive({
  styleId: undefined as number | undefined,
  description: '夜景样片，灯牌高光明显，暗部有可恢复细节。',
  tags: '夜景, 电影感, 霓虹'
})

const pageTitle = computed(() => {
  if (activeView.value === 'knowledge') return '调色知识库'
  if (activeView.value === 'samples') return '风格样片'
  if (activeView.value === 'observability') return '观测与评估'
  return '风格库'
})

const pageSubtitle = computed(() => {
  if (activeView.value === 'knowledge') return '维护 Agent 可检索的场景策略、参数经验和审核状态'
  if (activeView.value === 'samples') return '上传管理员样片，分析风格并生成可审核知识'
  if (activeView.value === 'observability') return '查看 LLM 调用、审计事件和自动化评估结果'
  return '维护用户端插件可引用的调色风格定义'
})

watch(activeView, async value => {
  if (value === 'samples') {
    await Promise.all([loadStyles(), loadSamples()])
  }
  if (value === 'observability') {
    await loadObservability()
  }
})

function selectStyle(row: any) {
  sampleForm.styleId = row.id
}

async function createStyle() {
  await unwrap(api.post('/api/admin/styles', {
    styleName: styleForm.styleName,
    styleCode: styleForm.styleCode,
    description: styleForm.description,
    suitableScenes: splitValues(styleForm.suitableScenes),
    avoidScenes: splitValues(styleForm.avoidScenes),
    status: 'enabled'
  }))
  ElMessage.success('风格已保存')
  await loadStyles()
}

async function loadStyles() {
  styles.value = await unwrap(api.get('/api/admin/styles'))
  if (!sampleForm.styleId && styles.value.length > 0) {
    sampleForm.styleId = styles.value[0].id
  }
}

async function deleteStyle(id: number) {
  await unwrap(api.delete(`/api/admin/styles/${id}`))
  ElMessage.success('风格已删除')
  await loadStyles()
}

async function createKnowledge() {
  await unwrap(api.post('/api/knowledge', {
    title: knowledgeForm.title,
    scene: knowledgeForm.scene,
    targetStyle: knowledgeForm.targetStyle,
    problems: splitValues(knowledgeForm.problems),
    strategy: splitValues(knowledgeForm.strategy),
    paramRanges: {
      highlights: '-30 ~ -60',
      shadows: '+10 ~ +35',
      dehaze: '+3 ~ +15',
      greenSaturation: '-10 ~ -30'
    }
  }))
  ElMessage.success('知识已进入审核池')
  await loadAdminKnowledge()
}

async function loadAdminKnowledge() {
  const params = knowledgeStatus.value ? { status: knowledgeStatus.value } : undefined
  adminKnowledge.value = await unwrap(api.get('/api/admin/knowledge', { params }))
}

async function approveKnowledge(id: number) {
  await unwrap(api.post(`/api/admin/knowledge/${id}/approve`))
  await loadAdminKnowledge()
}

async function rejectKnowledge(id: number) {
  await unwrap(api.post(`/api/admin/knowledge/${id}/reject`))
  await loadAdminKnowledge()
}

async function disableKnowledge(id: number) {
  await unwrap(api.post(`/api/admin/knowledge/${id}/disable`))
  await loadAdminKnowledge()
}

async function uploadSample() {
  if (!sampleForm.styleId) {
    ElMessage.warning('请先选择所属风格')
    return
  }
  if (!sampleFile.value) {
    ElMessage.warning('请选择一张成片样片')
    return
  }
  const formData = new FormData()
  formData.append('styleId', String(sampleForm.styleId))
  formData.append('sampleType', 'final_only')
  formData.append('sourceType', 'manual_upload')
  formData.append('description', sampleForm.description)
  splitValues(sampleForm.tags).forEach(tag => formData.append('tags', tag))
  formData.append('finalImage', sampleFile.value)
  await unwrap(api.post('/api/admin/style-samples/upload', formData))
  ElMessage.success('样片已上传')
  await loadSamples()
}

async function loadSamples() {
  const params = sampleForm.styleId ? { styleId: sampleForm.styleId } : undefined
  samples.value = await unwrap(api.get('/api/admin/style-samples', { params }))
}

function handleSampleFileChange(file: UploadFile) {
  sampleFile.value = file.raw
}

async function analyzeSample(id: number) {
  await unwrap(api.post(`/api/admin/style-samples/${id}/analyze`))
  ElMessage.success('样片分析已完成')
  await loadSamples()
}

async function generateKnowledge(id: number) {
  await unwrap(api.post(`/api/admin/style-samples/${id}/generate-knowledge`))
  ElMessage.success('已生成待审核知识')
  await loadAdminKnowledge()
}

async function loadObservability() {
  const [calls, events] = await Promise.all([
    unwrap<any[]>(api.get('/api/observability/llm-calls', { params: { limit: 50 } })),
    unwrap<any[]>(api.get('/api/observability/audit-events', { params: { limit: 50 } }))
  ])
  llmCalls.value = calls
  auditEvents.value = events
}

async function runBenchmark() {
  const report = await unwrap<any>(api.post('/api/evaluation/benchmark', {}))
  benchmarkSummary.value = `评估完成：${report.caseResults?.length ?? 0} 个用例，综合分 ${report.averageScore ?? '-'}`
}

function splitValues(value: string) {
  return value.split(/[\n,，]/).map(item => item.trim()).filter(Boolean)
}

onMounted(async () => {
  await Promise.all([loadStyles(), loadAdminKnowledge()])
})
</script>
