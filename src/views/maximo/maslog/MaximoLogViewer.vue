<template>
  <section class="maslog-page">
    <el-card>
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
        
        <el-button 
          type="primary" 
          icon="el-icon-document" 
          size="mini"
          @click="startLogStream"
          :loading="connecting"
          :disabled="isStreaming"
        >
          {{ isStreaming ? '正在接收...' : '查看日志' }}
        </el-button>
        
        <el-button 
          v-if="isStreaming"
          type="danger" 
          icon="el-icon-video-pause" 
          size="mini"
          @click="stopLogStream"
        >
          停止接收
        </el-button>
        
        <el-button 
          v-if="logs.length > 0"
          type="warning" 
          icon="el-icon-delete" 
          size="mini"
          @click="clearLogs"
        >
          清空日志
        </el-button>
        
        <el-divider direction="vertical" />
        
        <el-switch
          v-model="autoRefresh"
          active-text="实时刷新"
          inactive-text="手动刷新"
          @change="handleAutoRefreshChange"
          :disabled="!isStreaming"
        />
        
        <el-switch
          v-model="autoScroll"
          active-text="自动滚动"
          inactive-text="手动滚动"
          style="margin-left: 12px;"
          @change="handleAutoScrollChange"
        />
        
        <div class="log-stats">
          <el-tag size="small" type="info">总行数: {{ logs.length }}</el-tag>
          <el-tag size="small" type="success" style="margin-left: 8px;">显示: {{ filteredLogs.length }}</el-tag>
          <el-tag size="small" type="warning" style="margin-left: 8px;" v-if="filterKeyword">过滤: "{{ filterKeyword }}"</el-tag>
          <el-tag size="small" :type="isStreaming ? 'success' : 'info'" style="margin-left: 8px;">
            {{ isStreaming ? '● 连接中' : '○ 已断开' }}
          </el-tag>
        </div>
      </div>
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
          <p>暂无日志数据，请点击"查看日志"按钮开始接收</p>
        </div>
      </div>
    </el-card>
  </section>
</template>

<script>
import LogViewer from '@femessage/log-viewer'

export default {
  name: 'MaximoLogViewer',
  components: {
    LogViewer
  },
  data() {
    return {
      authHeaders: {},
      connecting: false,
      isStreaming: false,
      logs: [],
      filterKeyword: '',
      autoRefresh: true,
      abortController: null,
      maxLogs: 600000,
      logsToRemove: 200000,
      manualStop: false,
      reconnectTimer: null,
      autoScroll: true,
      updateTimer: null,
      pendingLogs: [],
      scrollDebounceTimer: null,
      lastScrollTop: 0,
      drawerViewerKey: 0
    }
  },
  computed: {
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
  mounted() {
  },
  beforeDestroy() {
    this.stopLogStream(true)
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.updateTimer) {
      cancelAnimationFrame(this.updateTimer)
      this.updateTimer = null
    }
    if (this.scrollDebounceTimer) {
      clearTimeout(this.scrollDebounceTimer)
      this.scrollDebounceTimer = null
    }
  },
  methods: {
    async startLogStream() {
      if (this.isStreaming) {
        return
      }
      this.authHeaders={}
      const maximoEnvSettings = localStorage.getItem('maximo-env-settings')
      var hasAuth=false
      if (maximoEnvSettings) {
        try {
          const settings = JSON.parse(maximoEnvSettings)
          if (settings.useApiKey && settings.apiKey) {
            this.authHeaders['apikey'] = settings.apiKey
            if (settings.apiKey) {
              hasAuth = true
            }
          } else if (settings.maxauth) {
            this.authHeaders['maxauth'] = settings.maxauth
            if (settings.maxauth) {
              hasAuth = true
            }
          }
        } catch (e) {
          console.error('解析配置失败', e)
          hasAuth = false
        }
      }
      if(!hasAuth){
        this.$message.error('请先配置 Maximo API Key！')
        return
      }

      this.manualStop = false
      this.connecting = true
      
      try {
        await this.connectToLogStream()
      } catch (error) {
        this.connecting = false
        this.$message.error('启动日志流失败: ' + error.message)
        console.error('Start log stream error:', error)
        
        if (!this.manualStop) {
          this.scheduleReconnect()
        }
      }
    },
    
    async connectToLogStream() {
      if (this.isStreaming) {
        return
      }

      this.connecting = true
      
      try {
        const baseUrl = process.env.VUE_APP_BASE_API || ''
        const url = `${baseUrl}/maximo/oslc/service/logging?action=wsmethod:streamLog`
        
        console.log('Connecting to Maximo logging stream:', url)
        
        this.abortController = new AbortController()
        
        const response = await fetch(url, {
          headers: {
            ...this.authHeaders,
            'Accept': 'text/event-stream'
          },
          signal: this.abortController.signal
        })
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        console.log('Maximo logging connection opened')
        this.connecting = false
        this.isStreaming = true
        this.$message.success('日志流连接成功')

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        
        while (true) {
          const { done, value } = await reader.read()
          
          if (done) {
            console.log('\nMaximo logging connection closed')
            this.stopLogStream()
            this.$message.warning('日志流已断开')
            break
          }
          
          const chunk = decoder.decode(value, { stream: true })
          buffer += chunk
          
          const lines = buffer.split(/\r?\n/)
          buffer = lines.pop()
          
          for (const line of lines) {
            const trimmedLine = line.trim()
            
            if (!trimmedLine) {
              continue
            }
            
            this.addLog(trimmedLine)
          }
        }
      } catch (error) {
        if (error.name === 'AbortError') {
          console.log('Maximo logging connection aborted by user')
        } else {
          console.error('Maximo logging connection error:', error)
          this.$message.error('连接错误: ' + error.message)
        }
        this.stopLogStream()
      }
    },
    
    stopLogStream(manual = false) {
      if (this.abortController) {
        this.abortController.abort()
        this.abortController = null
      }
      
      this.isStreaming = false
      this.connecting = false

      if (manual) {
        this.manualStop = true
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer)
          this.reconnectTimer = null
        }
        this.$message.info('已停止接收日志')
      } else {
        this.scheduleReconnect()
      }
    },
    
    scheduleReconnect() {
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
      }
      
      this.reconnectTimer = setTimeout(() => {
        if (!this.manualStop) {
          console.log('Attempting to reconnect...')
          this.startLogStream()
        }
      }, 2000)
    },
    
    addLog(message) {
      if (!message || !message.trim()) {
        return
      }
      
      this.pendingLogs.push(message)
      
      if (!this.updateTimer) {
        this.updateTimer = requestAnimationFrame(() => {
          this.flushPendingLogs()
          this.updateTimer = null
        })
      }
    },
    
    flushPendingLogs() {
      if (this.pendingLogs.length === 0) return
      
      const newLogs = this.pendingLogs.splice(0)
      this.logs.push(...newLogs)
      
      if (this.logs.length > this.maxLogs) {
        const keepCount = this.logs.length - this.logsToRemove
        this.logs = this.logs.slice(-keepCount)
        console.log(`日志数量超过 ${this.maxLogs}，已删除最旧的 ${this.logsToRemove} 行，当前剩余 ${this.logs.length} 行`)
      }
      
      if (this.autoScroll) {
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }
    },
    
    handleFilterChange() {
      this.$nextTick(() => {
        if (this.autoScroll) {
          this.scrollToBottom()
        }
      })
    },
    
    handleAutoScrollChange(value) {
      if (value) {
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }
    },
    
    handleScroll(e) {
      this.lastScrollTop = e.target.scrollTop
    },
    
    clearLogs() {
      this.$confirm('确定要清空所有日志吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.logs = []
        this.pendingLogs = []
        this.drawerViewerKey++
        this.$message.success('日志已清空')
      }).catch(() => {
      })
    },
    
    showHelp() {
      this.$alert(`
        <div style="text-align: left; line-height: 1.8;">
          <h3>📋 使用说明</h3>
          <ul style="padding-left: 20px;">
            <li><strong>查看日志</strong>：连接到 Maximo 自带日志流接口</li>
            <li><strong>停止接收</strong>：手动断开连接</li>
            <li><strong>实时刷新</strong>：断开后自动重连（2秒间隔）</li>
            <li><strong>自动滚动</strong>：新日志到达时自动滚动到底部</li>
            <li><strong>过滤器</strong>：输入关键词实时过滤日志</li>
          </ul>
          
          <h3>🎨 日志高亮</h3>
          <ul style="padding-left: 20px;">
            <li><span style="color:#90a959;font-weight:bold;">INFO</span> - 绿色：正常信息</li>
            <li><span style="color:#d28445;font-weight:bold;">WARN</span> - 橙色：警告信息</li>
            <li><span style="color:#ac4142;font-weight:bold;">ERROR</span> - 红色：错误信息</li>
            <li><span style="color:#f00;font-weight:bold;">FATAL</span> - 鲜红：严重错误</li>
          </ul>
          
          <h3>⚙️ API Key 配置</h3>
          <p>在浏览器控制台执行：<br>
          <code style="background:#f5f5f5;padding:2px 6px;border-radius:3px;">localStorage.setItem('maximo_api_key', 'your-key')</code></p>
        </div>
      `, '帮助', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '知道了',
        customClass: 'help-dialog'
      })
    },
    
    handleAutoRefreshChange(value) {
      if (this.isStreaming) {
        this.stopLogStream(true)
        setTimeout(() => {
          this.startLogStream()
        }, 500)
      }
    },
    
    scrollToBottom() {
      const container = this.$refs.logContainer
      if (container) {
        requestAnimationFrame(() => {
          container.scrollTop = container.scrollHeight
          this.lastScrollTop = container.scrollHeight
        })
      }
    },
    
    highlightLogLine(line) {
      let highlighted = line
      
      const ANSI = {
        reset: '\x1b[0m',
        bold: '\x1b[1m',
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
      
      highlighted = highlighted.replace(
        /\b(FATAL)\b/g,
        `${ANSI.brightRed}${ANSI.bold}$1${ANSI.reset}`
      )
      highlighted = highlighted.replace(
        /\b(ERROR)\b/g,
        `${ANSI.red}$1${ANSI.reset}`
      )
      highlighted = highlighted.replace(
        /\b(WARN|WARNING)\b/g,
        `${ANSI.yellow}$1${ANSI.reset}`
      )
      highlighted = highlighted.replace(
        /\b(INFO)\b/g,
        `${ANSI.green}$1${ANSI.reset}`
      )
      highlighted = highlighted.replace(
        /\b(DEBUG|TRACE)\b/g,
        `${ANSI.blue}$1${ANSI.reset}`
      )
      
      highlighted = highlighted.replace(
        /\b([a-z]+(?:\.[a-z]+)+\.[A-Z][a-zA-Z0-9]*)\b/g,
        `${ANSI.cyan}$1${ANSI.reset}`
      )
      
      highlighted = highlighted.replace(
        /\b((?:[A-Z][a-zA-Z0-9]*Exception|[A-Z][a-zA-Z0-9]*Error))\b/g,
        `${ANSI.brightMagenta}${ANSI.bold}${ANSI.underline}$1${ANSI.reset}`
      )
      
      highlighted = highlighted.replace(
        /\b(\d+)\b/g,
        `${ANSI.magenta}$1${ANSI.reset}`
      )
      
      if (this.filterKeyword) {
        const keyword = this.filterKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        highlighted = highlighted.replace(
          new RegExp(`(${keyword})`, 'gi'),
          `${ANSI.brightYellow}${ANSI.bold}$1${ANSI.reset}`
        )
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

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.page-summary {
  color: #606266;
  margin: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  flex-wrap: wrap;
  gap: 8px;
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

.log-footer {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.log-stats {
  display: flex;
  align-items: center;
}
</style>