<template>
  <section class="maslog-page">
    <el-card>
      <!-- 工具栏 -->
      <div class="toolbar">
        <i class="el-icon-question" @click="showHelp" title="查看帮助"></i>
        
        <el-input
          v-model="filterKeyword"
          placeholder="输入关键词过滤日志..."
          clearable
          prefix-icon="el-icon-search"
          style="width: 300px; margin-right: 12px;"
          @input="handleFilterChange"
        />

        <el-divider direction="vertical" />

        <!-- 标记开始 -->
        <el-button
          type="success"
          icon="el-icon-video-play"
          size="mini"
          @click="handleMarkerStart"
          :disabled="markerStartDisabled"
          :loading="startLoading"
        >
          标记开始
        </el-button>
        <el-tag
          v-if="startUuid"
          type="success"
          style="margin-left: 6px; cursor: pointer;"
          :closable="true"
          @close="clearStartUuid"
          @click="copyText(startUuid)"
          title="点击复制"
        >
          {{ startUuidShort }}
        </el-tag>

        <!-- 标记结束 -->
        <el-button
          type="warning"
          icon="el-icon-video-pause"
          size="mini"
          style="margin-left: 8px;"
          @click="handleMarkerEnd"
          :disabled="!startUuid || markerEndDisabled"
          :loading="endLoading"
        >
          标记结束
        </el-button>
        <el-tag
          v-if="endUuid"
          type="warning"
          style="margin-left: 6px; cursor: pointer;"
          :closable="true"
          @close="clearEndUuid"
          @click="copyText(endUuid)"
          title="点击复制"
        >
          {{ endUuidShort }}
        </el-tag>

        <el-divider direction="vertical" v-if="startUuid && endUuid" />

        <!-- 获取日志区间 -->
        <el-button
          v-if="startUuid && endUuid"
          type="primary"
          icon="el-icon-document"
          size="mini"
          @click="handleMarkerGet"
          :loading="getLoading"
        >
          获取标记区间
        </el-button>

        <el-button
          v-if="logs.length > 0"
          type="warning"
          icon="el-icon-delete"
          size="mini"
          style="margin-left: 8px;"
          @click="clearLogs"
        >
          清空日志
        </el-button>

        <el-button
          v-if="startUuid && endUuid"
          type="warning"
          icon="el-icon-refresh"
          size="mini"
          @click="clearAllMarkers"
        >
          清除标记
        </el-button>

        <el-button
          v-if="logs.length > 0"
          type="primary"
          icon="el-icon-document-copy"
          size="mini"
          @click="copyLogs"
        >
          复制日志
        </el-button>

        <div class="log-stats">
          <el-tag size="small" type="info">总行数: {{ logs.length }}</el-tag>
          <el-tag size="small" type="success" style="margin-left: 8px;">显示: {{ filteredLogs.length }}</el-tag>
          <el-tag size="small" type="warning" style="margin-left: 8px;" v-if="filterKeyword">过滤: "{{ filterKeyword }}"</el-tag>
        </div>
      </div>

      <!-- 标记状态栏 -->
      <div v-if="startUuid || endUuid" class="marker-status-bar">
        <template v-if="startUuid">
          <span class="marker-label marker-start-label">● 开始:</span>
          <code class="marker-uuid">{{ startUuid }}</code>
          <el-button type="text" size="mini" icon="el-icon-document" @click="copyText(startUuid)">复制</el-button>
        </template>
        <template v-if="startUuid && endUuid">
          <span style="color:#909399;margin:0 12px;">→</span>
        </template>
        <template v-if="endUuid">
          <span class="marker-label marker-end-label">● 结束:</span>
          <code class="marker-uuid">{{ endUuid }}</code>
          <el-button type="text" size="mini" icon="el-icon-document" @click="copyText(endUuid)">复制</el-button>
        </template>
      </div>

      <!-- 日志显示区域 -->
      <div class="log-container">
        <log-viewer
          v-if="logs.length > 0"
          :log="fullLogText"
          :key="drawerViewerKey"
          :has-number="true"
          style="height: calc(100vh - 280px); min-height: 600px; max-height: 900px;"
        />
        <div v-else class="empty-log">
          <i class="el-icon-document-delete" style="font-size: 48px; color: #dcdfe6;"></i>
          <p>暂无日志数据，请先标记开始/结束，然后获取标记区间</p>
        </div>
      </div>
    </el-card>
  </section>
</template>

<script>
import { mapGetters } from 'vuex'
import request from '@/utils/request'
import LogViewer from '@femessage/log-viewer'

export default {
  name: 'MasLogMarker',
  components: {
    LogViewer
  },
  data() {
    return {
      authHeaders: {},
      startUuid: '',
      endUuid: '',
      startLoading: false,
      endLoading: false,
      getLoading: false,
      markerStartDisabled: false,
      markerEndDisabled: false,
      logs: [],
      filterKeyword: '',
      drawerViewerKey: 0
    }
  },
  computed: {
    ...mapGetters(['selectedEnv']),
    startUuidShort() {
      return this.startUuid ? this.startUuid.substring(0, 8) + '...' : ''
    },
    endUuidShort() {
      return this.endUuid ? this.endUuid.substring(0, 8) + '...' : ''
    },
    filteredLogs() {
      let logs = this.logs
      if (this.filterKeyword) {
        const keyword = this.filterKeyword.toLowerCase()
        logs = logs.filter(log => 
          log.toLowerCase().includes(keyword)
        )
      }
      return logs
    },
    fullLogText() {
      return this.filteredLogs.map(log => this.highlightLogLine(log)).join('\n')
    }
  },
  beforeDestroy() {
    // 无需清理 SSE 相关资源
  },
  methods: {
    getAuthHeaders() {
      const headers = {}
      const maximoEnvSettings = localStorage.getItem('maximo-env-settings')
      if (maximoEnvSettings) {
        try {
          const settings = JSON.parse(maximoEnvSettings)
          if (settings.useApiKey && settings.apiKey) {
            headers['apikey'] = settings.apiKey
          } else if (settings.maxauth) {
            headers['maxauth'] = settings.maxauth
          }
        } catch (e) {
          console.error('解析配置失败', e)
        }
      }
      return headers
    },

    generateUuid() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0
        return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
      })
    },

    async handleMarkerStart() {
      const headers = this.getAuthHeaders()
      if (!headers.apikey && !headers.maxauth) {
        this.$message.error('请先配置 Maximo API Key！可点击右上角进行设置')
        return
      }

      const uuid = this.generateUuid()
      this.startLoading = true

      try {
        await request({
          url: '/api/script/SKS_LOG_MARKER',
          method: 'get',
          headers: {
            ...headers,
            'Content-Type': 'application/json'
          },
          params: { marker: 'start', startuuid: uuid }
        })
        this.startUuid = uuid
        this.markerStartDisabled = true
        this.$message.success('开始标记已写入日志')
      } catch (error) {
        this.$message.error('标记开始失败: ' + (error.message || '未知错误'))
        console.error('Marker start error:', error)
      } finally {
        this.startLoading = false
      }
    },

    async handleMarkerEnd() {
      const headers = this.getAuthHeaders()
      if (!headers.apikey && !headers.maxauth) {
        this.$message.error('请先配置 Maximo API Key！可点击右上角进行设置')
        return
      }

      const uuid = this.generateUuid()
      this.endLoading = true

      try {
        await request({
          url: '/api/script/SKS_LOG_MARKER',
          method: 'get',
          headers: {
            ...headers,
            'Content-Type': 'application/json'
          },
          params: { marker: 'end', enduuid: uuid }
        })
        this.endUuid = uuid
        this.markerEndDisabled = true
        this.$message.success('结束标记已写入日志')
        // 标记结束后自动获取区间日志
        this.$nextTick(() => {
          this.handleMarkerGet()
        })
      } catch (error) {
        this.$message.error('标记结束失败: ' + (error.message || '未知错误'))
        console.error('Marker end error:', error)
      } finally {
        this.endLoading = false
      }
    },

    async handleMarkerGet() {
      const headers = this.getAuthHeaders()
      if (!headers.apikey && !headers.maxauth) {
        this.$message.error('请先配置 Maximo API Key！可点击右上角进行设置')
        return
      }

      if (!this.startUuid || !this.endUuid) {
        this.$message.warning('请先完成开始和结束标记')
        return
      }

      this.getLoading = true

      try {
        const response = await request({
          url: '/api/script/SKS_LOG_MARKER',
          method: 'get',
          headers: {
            ...headers,
            'Content-Type': 'application/json'
          },
          params: {
            marker: 'get',
            startuuid: this.startUuid,
            enduuid: this.endUuid
          }
        })

        // 解析返回的日志内容
        const content = response?.data?.content || ''
        if (content) {
          this.logs = content.split('\n')
          this.$message.success(`获取成功，共 ${this.logs.length} 行日志`)
        } else {
          this.logs = []
          this.$message.info('标记区间内无日志内容')
        }

        // 强制刷新 log-viewer
        this.$nextTick(() => {
          this.drawerViewerKey++
        })
      } catch (error) {
        const errMsg = error?.response?.data?.message || error.message || '未知错误'
        this.$message.error('获取日志失败: ' + errMsg)
        console.error('Marker get error:', error)
      } finally {
        this.getLoading = false
      }
    },

    clearStartUuid() {
      this.startUuid = ''
      this.markerStartDisabled = false
    },

    clearEndUuid() {
      this.endUuid = ''
      this.markerEndDisabled = false
    },

    clearAllMarkers() {
      this.startUuid = ''
      this.endUuid = ''
      this.markerStartDisabled = false
      this.markerEndDisabled = false
      this.$message.info('已清除所有标记')
    },

    clearLogs() {
      this.$confirm('确定要清空所有日志吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.logs = []
        this.drawerViewerKey++
        this.$message.success('日志已清空')
      }).catch(() => {})
    },

    copyText(text) {
      if (!text) return
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      this.$message.success('已复制到剪贴板')
    },

    handleFilterChange() {
      // 过滤条件变化时仅更新 computed
    },

    copyLogs() {
      const text = this.logs.join('\n')
      if (!text) {
        this.$message.info('没有日志可复制')
        return
      }
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      this.$message.success(`已复制 ${this.logs.length} 行日志到剪贴板`)
    },

    showHelp() {
      this.$alert(`
        <div style="text-align: left; line-height: 1.8;">
          <h3>📋 日志标记工具说明</h3>
          <ul style="padding-left: 20px;">
            <li><strong>标记开始</strong>：在 Maximo 日志中写入一个开始标记（自动生成 UUID）</li>
            <li><strong>标记结束</strong>：在 Maximo 日志中写入一个结束标记（自动生成 UUID）</li>
            <li><strong>获取标记区间</strong>：读取两个标记之间的所有日志内容</li>
            <li><strong>复制 UUID</strong>：点击 UUID 标签即可复制</li>
          </ul>
          
          <h3>💡 使用流程</h3>
          <ol style="padding-left: 20px;">
            <li>点击「标记开始」→ 在目标系统执行操作</li>
            <li>点击「标记结束」→ 点击「获取标记区间」查看日志</li>
          </ol>
          
          <h3>🎨 日志高亮</h3>
          <ul style="padding-left: 20px;">
            <li><span style="color:#90a959;font-weight:bold;">INFO</span> - 绿色：正常信息</li>
            <li><span style="color:#d28445;font-weight:bold;">WARN</span> - 橙色：警告信息</li>
            <li><span style="color:#ac4142;font-weight:bold;">ERROR</span> - 红色：错误信息</li>
            <li><span style="color:#f00;font-weight:bold;">FATAL</span> - 鲜红：严重错误</li>
          </ul>
        </div>
      `, '帮助', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '知道了',
        customClass: 'help-dialog'
      })
    },

    highlightLogLine(line) {
      let highlighted = line

      const ANSI = {
        reset: '\x1b[0m',
        bold: '\x1b[1m',
        underline: '\x1b[4m',
        black: '\x1b[30m',
        red: '\x1b[31m',
        green: '\x1b[32m',
        yellow: '\x1b[33m',
        blue: '\x1b[34m',
        magenta: '\x1b[35m',
        cyan: '\x1b[36m',
        white: '\x1b[37m',
        brightRed: '\x1b[91m',
        brightGreen: '\x1b[92m',
        brightYellow: '\x1b[93m',
        brightBlue: '\x1b[94m',
        brightMagenta: '\x1b[95m',
        brightCyan: '\x1b[96m',
        brightWhite: '\x1b[97m'
      }

      // 1. 日志级别高亮
      highlighted = highlighted.replace(/\b(FATAL)\b/g, `${ANSI.brightRed}${ANSI.bold}$1${ANSI.reset}`)
      highlighted = highlighted.replace(/\b(ERROR)\b/g, `${ANSI.red}$1${ANSI.reset}`)
      highlighted = highlighted.replace(/\b(WARN|WARNING)\b/g, `${ANSI.yellow}$1${ANSI.reset}`)
      highlighted = highlighted.replace(/\b(INFO)\b/g, `${ANSI.green}$1${ANSI.reset}`)
      highlighted = highlighted.replace(/\b(DEBUG|TRACE)\b/g, `${ANSI.blue}$1${ANSI.reset}`)

      // 2. 类名高亮（青色）
      highlighted = highlighted.replace(/\b([a-z]+(?:\.[a-z]+)+\.[A-Z][a-zA-Z0-9]*)\b/g, `${ANSI.cyan}$1${ANSI.reset}`)

      // 3. 异常类型高亮（粉红色粗体下划线）
      highlighted = highlighted.replace(/\b((?:[A-Z][a-zA-Z0-9]*Exception|[A-Z][a-zA-Z0-9]*Error))\b/g, `${ANSI.brightMagenta}${ANSI.bold}${ANSI.underline}$1${ANSI.reset}`)

      // 4. 数字高亮（紫色）
      highlighted = highlighted.replace(/\b(\d+)\b/g, `${ANSI.magenta}$1${ANSI.reset}`)

      // 5. 过滤关键词高亮（黄色粗体）
      if (this.filterKeyword) {
        const keyword = this.filterKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        highlighted = highlighted.replace(new RegExp(`(${keyword})`, 'gi'), `${ANSI.brightYellow}${ANSI.bold}$1${ANSI.reset}`)
      }

      return highlighted
    }
  }
}
</script>

<style lang="scss" scoped>
.maslog-page {
  padding: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 0;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  flex-wrap: wrap;
  gap: 8px;
}

.marker-status-bar {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background-color: #f0f5ff;
  border: 1px solid #d6e4ff;
  border-radius: 4px;
  margin-top: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 13px;

  .marker-label {
    font-weight: bold;
    white-space: nowrap;
  }

  .marker-start-label {
    color: #67c23a;
  }

  .marker-end-label {
    color: #e6a23c;
  }

  .marker-uuid {
    background: #fff;
    padding: 2px 8px;
    border: 1px solid #dcdfe6;
    border-radius: 3px;
    font-family: 'Courier New', Courier, monospace;
    font-size: 12px;
    color: #303133;
    user-select: all;
  }
}

.log-stats {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.log-container {
  min-height: 600px;
  max-height: 900px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #1e1e1e;
  margin-top: 8px;
}

.empty-log {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 280px);
  min-height: 600px;
  max-height: 900px;
  color: #909399;

  p {
    margin-top: 16px;
    font-size: 14px;
  }
}
</style>