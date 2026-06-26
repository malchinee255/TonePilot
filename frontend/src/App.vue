<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">TP</div>
        <div>
          <h1>TonePilot</h1>
          <p>Lightroom Agent</p>
        </div>
      </div>

      <el-menu :default-active="activeView" class="nav-menu" @select="activeView = $event">
        <el-menu-item index="workflow">
          <el-icon><MagicStick /></el-icon>
          <span>调色链路</span>
        </el-menu-item>
        <el-menu-item index="knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="admin">
          <el-icon><Operation /></el-icon>
          <span>管理端</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <main class="workspace">
      <section class="topbar">
        <div>
          <h2>{{ pageTitle }}</h2>
          <p>{{ pageSubtitle }}</p>
        </div>
        <el-tag effect="plain" round>非生成式调色决策</el-tag>
      </section>

      <section v-if="activeView === 'workflow'" class="grid workflow-grid">
        <div class="panel">
          <div class="panel-title">
            <h3>照片</h3>
            <el-button :icon="Upload" type="primary" @click="uploadPhoto">上传</el-button>
          </div>
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="onPhotoChange"
            :file-list="photoFiles"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
          </el-upload>
          <div v-if="photo" class="entity-line">
            <span>#{{ photo.id }}</span>
            <strong>{{ photo.fileName }}</strong>
          </div>
          <el-form label-position="top" class="compact-form">
            <el-form-item label="模型供应商">
              <el-select v-model="modelProvider">
                <el-option label="本地规则" value="rule" />
                <el-option label="OpenAI" value="openai" />
                <el-option label="阿里 Qwen2" value="qwen2" />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="button-row">
            <el-button :icon="View" @click="analyzePhoto" :disabled="!photo">分析</el-button>
            <el-input v-model="targetStyle" placeholder="目标风格" />
            <el-button :icon="MagicStick" type="success" @click="generateAdjustment" :disabled="!photo">生成参数</el-button>
          </div>
          <div class="button-row">
            <el-button :icon="CircleCheck" @click="evaluateAdjustment" :disabled="!adjustment?.id">评测</el-button>
            <el-input v-model="presetName" placeholder="XMP 名称" />
            <el-button :icon="Download" @click="exportXmp" :disabled="!adjustment?.id">导出 XMP</el-button>
          </div>
          <div class="button-row single-action">
            <el-button :icon="MagicStick" @click="startTuningSession()" :disabled="!photo">开启微调会话</el-button>
            <el-tag v-if="tuningSession" type="success" effect="plain">会话 #{{ shortId(tuningSession.id) }}</el-tag>
          </div>
          <el-alert
            v-if="lightroomStatus"
            class="connector-alert"
            :title="lightroomStatus.message"
            :type="lightroomStatus.available ? 'success' : 'info'"
            show-icon
            :closable="false"
          />
        </div>

        <div class="panel preview-panel">
          <div class="panel-title">
            <h3>实时对比</h3>
            <el-tag v-if="tuningSession?.saved" type="success">已保存</el-tag>
          </div>
          <div v-if="tuningSession?.preview" class="compare-grid">
            <div class="image-box">
              <span>原图</span>
              <img :src="fileUrl(tuningSession.preview.originalUrl)" alt="原图" />
            </div>
            <div class="image-box">
              <span>调色预览</span>
              <img :src="fileUrl(tuningSession.preview.previewUrl)" alt="调色预览" />
            </div>
          </div>
          <el-empty v-else description="生成参数或开启微调后显示预览" />
        </div>

        <div class="panel tuning-panel">
          <div class="panel-title">
            <h3>多轮微调</h3>
            <el-button :icon="CircleCheck" type="primary" plain @click="saveTuningResult" :disabled="!tuningSession">保存结果</el-button>
          </div>
          <div class="chat-list">
            <div
              v-for="(message, index) in tuningSession?.messages || []"
              :key="index"
              class="chat-message"
              :class="message.role"
            >
              <span>{{ message.role === 'user' ? '用户' : 'Agent' }}</span>
              <p>{{ message.content }}</p>
            </div>
          </div>
          <div class="chat-input">
            <el-input
              v-model="tuningPrompt"
              type="textarea"
              :rows="3"
              placeholder="例如：再亮一点，肤色更通透，绿色不要那么脏"
              :disabled="!tuningSession"
            />
            <el-button :icon="MagicStick" type="success" @click="sendTuningMessage" :disabled="!tuningSession || !tuningPrompt.trim()">发送微调</el-button>
          </div>
          <el-table :data="tuningSession?.latestDeltas || []" height="220" empty-text="暂无参数变化">
            <el-table-column prop="label" label="参数" min-width="110" />
            <el-table-column prop="beforeValue" label="原值" width="76" />
            <el-table-column prop="afterValue" label="新值" width="76" />
            <el-table-column prop="delta" label="变化" width="76" />
            <el-table-column prop="reason" label="原因" min-width="180" />
          </el-table>
        </div>

        <div class="panel output-panel">
          <div class="panel-title">
            <h3>结果</h3>
            <el-tag v-if="evaluation" :type="evaluation.passed ? 'success' : 'warning'">{{ evaluation.score }}</el-tag>
          </div>
          <el-tabs v-model="resultTab">
            <el-tab-pane label="分析" name="analysis">
              <pre>{{ pretty(analysis) }}</pre>
            </el-tab-pane>
            <el-tab-pane label="参数" name="adjustment">
              <pre>{{ pretty(adjustment) }}</pre>
            </el-tab-pane>
            <el-tab-pane label="会话" name="session">
              <pre>{{ pretty(tuningSession) }}</pre>
            </el-tab-pane>
            <el-tab-pane label="评测" name="evaluation">
              <pre>{{ pretty(evaluation) }}</pre>
            </el-tab-pane>
            <el-tab-pane label="XMP" name="xmp">
              <a v-if="xmpExport" :href="xmpExport.xmpUrl" target="_blank">{{ xmpExport.xmpUrl }}</a>
            </el-tab-pane>
          </el-tabs>
        </div>
      </section>

      <section v-if="activeView === 'knowledge'" class="grid two">
        <div class="panel">
          <div class="panel-title">
            <h3>新增知识</h3>
            <el-button :icon="Plus" type="primary" @click="createKnowledge">保存</el-button>
          </div>
          <el-form label-position="top">
            <el-form-item label="标题"><el-input v-model="knowledgeForm.title" /></el-form-item>
            <el-form-item label="场景"><el-input v-model="knowledgeForm.scene" /></el-form-item>
            <el-form-item label="目标风格"><el-input v-model="knowledgeForm.targetStyle" /></el-form-item>
            <el-form-item label="问题"><el-input v-model="knowledgeForm.problems" /></el-form-item>
            <el-form-item label="策略"><el-input v-model="knowledgeForm.strategy" type="textarea" :rows="4" /></el-form-item>
          </el-form>
        </div>
        <div class="panel">
          <div class="panel-title">
            <h3>条目</h3>
            <el-button :icon="Refresh" @click="loadKnowledge">刷新</el-button>
          </div>
          <el-table :data="knowledgeList" height="520">
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="scene" label="场景" width="120" />
            <el-table-column prop="targetStyle" label="风格" width="140" />
            <el-table-column width="84">
              <template #default="{ row }">
                <el-button :icon="Delete" circle text @click="deleteKnowledge(row.id)" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section v-if="activeView === 'admin'" class="grid two">
        <div class="panel">
          <div class="panel-title">
            <h3>风格</h3>
            <el-button :icon="Plus" type="primary" @click="createStyle">创建</el-button>
          </div>
          <el-form label-position="top">
            <el-form-item label="名称"><el-input v-model="styleForm.styleName" /></el-form-item>
            <el-form-item label="编码"><el-input v-model="styleForm.styleCode" /></el-form-item>
            <el-form-item label="描述"><el-input v-model="styleForm.description" type="textarea" :rows="4" /></el-form-item>
          </el-form>
          <el-table :data="styles" height="280" @row-click="selectedStyle = $event">
            <el-table-column prop="styleName" label="名称" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </div>

        <div class="panel">
          <div class="panel-title">
            <h3>知识审核</h3>
            <el-button :icon="Refresh" @click="loadAdminKnowledge">刷新</el-button>
          </div>
          <div class="selected-style" v-if="selectedStyle">
            <span>#{{ selectedStyle.id }}</span>
            <strong>{{ selectedStyle.styleName }}</strong>
          </div>
          <el-table :data="adminKnowledge" height="520">
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column width="132">
              <template #default="{ row }">
                <el-button :icon="CircleCheck" circle text @click="approveKnowledge(row.id)" />
                <el-button :icon="Close" circle text @click="rejectKnowledge(row.id)" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type UploadFile } from 'element-plus'
import {
  CircleCheck,
  Close,
  Collection,
  Delete,
  Download,
  MagicStick,
  Operation,
  Plus,
  Refresh,
  Upload,
  UploadFilled,
  View
} from '@element-plus/icons-vue'
import { api, unwrap } from './api'

const activeView = ref('workflow')
const resultTab = ref('analysis')
const photoFiles = ref<UploadFile[]>([])
const selectedPhotoFile = ref<File | null>(null)
const photo = ref<any>(null)
const analysis = ref<any>(null)
const adjustment = ref<any>(null)
const evaluation = ref<any>(null)
const xmpExport = ref<any>(null)
const tuningSession = ref<any>(null)
const tuningPrompt = ref('')
const lightroomStatus = ref<any>(null)
const targetStyle = ref('夜景电影感')
const presetName = ref('TonePilot Preset')
const modelProvider = ref('rule')

const knowledgeList = ref<any[]>([])
const styles = ref<any[]>([])
const adminKnowledge = ref<any[]>([])
const selectedStyle = ref<any>(null)

const knowledgeForm = reactive({
  title: '夜景人像调色策略',
  scene: '夜景人像',
  targetStyle: '夜景电影感',
  problems: '灯光高光偏亮,人物偏暗,背景绿色偏脏',
  strategy: '降低高光，保留灯光细节\n提升阴影，恢复人物暗部\n降低绿色和黄色饱和度\n提高橙色明度，改善肤色'
})

const styleForm = reactive({
  styleName: '日系清透人像',
  styleCode: 'japanese_clear_portrait',
  description: '整体明亮、低对比、肤色干净、绿色柔和。'
})

const pageTitle = computed(() => {
  if (activeView.value === 'knowledge') return '调色知识库'
  if (activeView.value === 'admin') return '风格管理'
  return '调色链路'
})

const pageSubtitle = computed(() => {
  if (activeView.value === 'knowledge') return '策略、场景、参数范围'
  if (activeView.value === 'admin') return '风格、样本、审核状态'
  return '照片分析、参数生成、评测、XMP'
})

function onPhotoChange(file: UploadFile) {
  selectedPhotoFile.value = file.raw ?? null
  photoFiles.value = [file]
}

async function uploadPhoto() {
  if (!selectedPhotoFile.value) return ElMessage.warning('请选择照片')
  const form = new FormData()
  form.append('file', selectedPhotoFile.value)
  photo.value = await unwrap(api.post('/api/photos/upload', form))
  analysis.value = null
  adjustment.value = null
  evaluation.value = null
  xmpExport.value = null
  tuningSession.value = null
  ElMessage.success('已上传')
}

async function analyzePhoto() {
  analysis.value = await unwrap(api.post(`/api/photos/${photo.value.id}/analyze`, null, {
    params: { provider: modelProvider.value }
  }))
  resultTab.value = 'analysis'
}

async function generateAdjustment() {
  adjustment.value = await unwrap(api.post('/api/agent/generate-adjustment', {
    photoId: photo.value.id,
    targetStyle: targetStyle.value,
    provider: modelProvider.value
  }))
  await openTuningSession(adjustment.value.id)
  resultTab.value = 'adjustment'
}

async function evaluateAdjustment() {
  evaluation.value = await unwrap(api.post('/api/evaluation/check', {
    photoId: photo.value.id,
    adjustmentId: adjustment.value.id
  }))
  resultTab.value = 'evaluation'
}

async function exportXmp() {
  xmpExport.value = await unwrap(api.post('/api/xmp/export', {
    photoId: photo.value.id,
    adjustmentId: adjustment.value.id,
    presetName: presetName.value
  }))
  resultTab.value = 'xmp'
}

async function startTuningSession() {
  await openTuningSession(adjustment.value?.id)
}

async function openTuningSession(adjustmentId?: number) {
  if (!photo.value) return ElMessage.warning('请先上传照片')
  tuningSession.value = await unwrap(api.post('/api/tuning/sessions', {
    photoId: photo.value.id,
    adjustmentId
  }))
  adjustment.value = tuningSession.value.currentAdjustment
  resultTab.value = 'session'
}

async function sendTuningMessage() {
  if (!tuningSession.value || !tuningPrompt.value.trim()) return
  tuningSession.value = await unwrap(api.post(`/api/tuning/sessions/${tuningSession.value.id}/messages`, {
    message: tuningPrompt.value,
    provider: modelProvider.value
  }))
  adjustment.value = tuningSession.value.currentAdjustment
  tuningPrompt.value = ''
  resultTab.value = 'session'
}

async function saveTuningResult() {
  if (!tuningSession.value) return
  tuningSession.value = await unwrap(api.post(`/api/tuning/sessions/${tuningSession.value.id}/save`, {
    name: presetName.value || `${targetStyle.value} 微调版`
  }))
  adjustment.value = tuningSession.value.currentAdjustment
  resultTab.value = 'adjustment'
  ElMessage.success('已保存微调结果')
}

async function loadLightroomStatus() {
  lightroomStatus.value = await unwrap(api.get('/api/tuning/lightroom/status'))
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
      shadows: '+15 ~ +40',
      greenSaturation: '-15 ~ -35',
      orangeLuminance: '+5 ~ +20'
    }
  }))
  ElMessage.success('已保存')
  await loadKnowledge()
}

async function loadKnowledge() {
  knowledgeList.value = await unwrap(api.get('/api/knowledge'))
}

async function deleteKnowledge(id: number) {
  await unwrap(api.delete(`/api/knowledge/${id}`))
  await loadKnowledge()
}

async function createStyle() {
  await unwrap(api.post('/api/admin/styles', {
    ...styleForm,
    suitableScenes: ['白天人像', '自然光人像', '校园写真'],
    avoidScenes: ['夜景', '严重过曝照片'],
    status: 'enabled'
  }))
  ElMessage.success('已创建')
  await loadStyles()
}

async function loadStyles() {
  styles.value = await unwrap(api.get('/api/admin/styles'))
}

async function loadAdminKnowledge() {
  adminKnowledge.value = await unwrap(api.get('/api/admin/knowledge'))
}

async function approveKnowledge(id: number) {
  await unwrap(api.post(`/api/admin/knowledge/${id}/approve`))
  await loadAdminKnowledge()
}

async function rejectKnowledge(id: number) {
  await unwrap(api.post(`/api/admin/knowledge/${id}/reject`))
  await loadAdminKnowledge()
}

function pretty(value: unknown) {
  return value ? JSON.stringify(value, null, 2) : ''
}

function splitValues(value: string) {
  return value.split(/[\n,，]/).map(item => item.trim()).filter(Boolean)
}

function fileUrl(url: string) {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  return url
}

function shortId(id: string) {
  return id ? id.slice(0, 8) : ''
}

onMounted(async () => {
  await Promise.all([loadKnowledge(), loadStyles(), loadAdminKnowledge(), loadLightroomStatus()])
})
</script>
