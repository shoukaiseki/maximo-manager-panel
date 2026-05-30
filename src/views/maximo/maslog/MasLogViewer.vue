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
        
        <el-select
          v-if="!isStreaming"
          v-model="refreshInterval"
          size="small"
          style="width: 120px; margin-right: 12px;"
        >
          <el-option label="5秒" :value="5" />
          <el-option label="10秒" :value="10" />
          <el-option label="20秒" :value="20" />
          <el-option label="30秒" :value="30" />
          <el-option label="50秒" :value="50" />
          <el-option label="80秒" :value="80" />
          <el-option label="100秒" :value="100" />
          <el-option label="200秒" :value="200" />
          <el-option label="500秒" :value="500" />
          <el-option label="1000秒" :value="1000" />
        </el-select>
        
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
          <p>暂无日志数据，请点击"查看日志"按钮开始接收</p>
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
  name: 'MasLogViewer',
  components: {
    LogViewer
  },
  data() {
    return {
      connecting: false,
      isStreaming: false,
      logs: [],
      filterKeyword: '',
      autoRefresh: true,
      refreshInterval: 10,
      countdown: 0,
      timer: null,
      countdownTimer: null,
      abortController: null, // 用于取消 fetch 请求
      maxLogs: 600000, // 最大日志行数阈值（达到此值时触发清理）
      logsToRemove: 200000, // 每次清理时删除的日志行数
      manualStop: false, // 标记是否手动停止
      reconnectTimer: null, // 重连定时器
      autoScroll: true, // 自动滚动开关
      updateTimer: null, // 批量更新定时器
      pendingLogs: [], // 待处理的日志队列
      scrollDebounceTimer: null, // 滚动防抖定时器
      lastScrollTop: 0, // 记录上次滚动位置
      drawerViewerKey: 0 // log-viewer 的 key，用于强制刷新
    }
  },
  computed: {
    ...mapGetters(['selectedEnv']),
    filteredLogs() {
      let logs = this.logs
      
      // 应用过滤
      if (this.filterKeyword) {
        const keyword = this.filterKeyword.toLowerCase()
        logs = logs.filter(log => 
          log.toLowerCase().includes(keyword)
        )
      }
      
      return logs
    },
    fullLogText() {
      // 将所有日志合并为一个字符串，每行用换行符分隔，并应用高亮
      return this.filteredLogs.map(log => this.highlightLogLine(log)).join('\n')
    }
  },
  mounted() {
    // 组件挂载时不自动连接，等待用户点击
  },
  beforeDestroy() {
    this.stopLogStream(true)
    this.clearTimers()
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

      // 检查是否配置了 maximo_api_key
      const apiKey = localStorage.getItem('maximo_api_key')
      if (!apiKey) {
        this.msgError('请先配置 Maximo API Key！\n可点击右上角进行设置\n或在浏览器控制台执行：localStorage.setItem("maximo_api_key", "your-key")')
        return
      }

      this.manualStop = false
      this.connecting = true
      
      try {
        // 先初始化
        await this.initializeLogging()
        
        // 然后连接 SSE
        await this.connectToSSE()
      } catch (error) {
        this.connecting = false
        this.$message.error('启动日志流失败: ' + error.message)
        console.error('Start log stream error:', error)
        
        // 如果不是手动停止，2秒后重连
        if (!this.manualStop) {
          this.scheduleReconnect()
        }
      }
    },
    
    async initializeLogging() {
      return new Promise((resolve, reject) => {
        request({
          url: '/api/script/SHARPTREE.AUTOSCRIPT.LOGGING',
          method: 'get',
          headers: { 'Content-Type': 'application/json' },
          params: { initialize: true }
        }).then(response => {
          console.log('Initialization response:', response)
          this.$message.success('初始化成功')
          resolve()
        }).catch(error => {
          console.error('Initialization error:', error)
          this.$message.error('初始化失败')
          reject(error)
        })
      })
    },
    
    async connectToSSE() {
      if (this.isStreaming) {
        return
      }

      this.connecting = true
      
      try {
        // 获取 API 配置
        const env = this.selectedEnv
        const baseUrl = env?.apiUrl || process.env.VUE_APP_BASE_API || ''
        
        // 从缓存中获取 apiKey
        const apiKey = localStorage.getItem('maximo_api_key') || 'pllidkh661nmu6n4rk7qs4celrq9esl7nbeclgns'
        
        // 构建 SSE URL
        const timeout = this.refreshInterval  // 转换为毫秒
        const url = `${baseUrl}/maximo/api/script/SHARPTREE.AUTOSCRIPT.LOGGING?timeout=${timeout}`
        
        console.log('Connecting to SSE:', url)
        console.log('Timeout parameter:', timeout, 'seconds')
        
        // 创建 AbortController 用于取消请求
        this.abortController = new AbortController()
        
        // 使用 fetch API 连接 SSE
        const response = await fetch(url, {
          headers: {
            'Accept': 'text/event-stream',
            'apiKey': apiKey
          },
          signal: this.abortController.signal
        })
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        console.log('SSE connection opened')
        this.connecting = false
        this.isStreaming = true
        this.$message.success('日志流连接成功')
        
        // 如果开启了自动刷新，启动定时器
        if (this.autoRefresh) {
          this.startAutoRefresh()
        }
        
        // 获取可读流
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        
        // 持续读取数据
        while (true) {
          const { done, value } = await reader.read()
          
          if (done) {
            console.log('\nSSE connection closed')
            this.stopLogStream()
            this.$message.warning('日志流已断开')
            break
          }
          
          // 解码数据块
          const chunk = decoder.decode(value, { stream: true })
          buffer += chunk
          
          // 按行处理数据（支持 \n 和 \r\n）
          const lines = buffer.split(/\r?\n/)
          buffer = lines.pop() // 保留最后一个不完整的行
          
          for (const line of lines) {
            const trimmedLine = line.trim()
            
            // 跳过空行
            if (!trimmedLine) {
              continue
            }
            
            // 添加日志行
            this.addLog(trimmedLine)
          }
        }
      } catch (error) {
        if (error.name === 'AbortError') {
          console.log('SSE connection aborted by user')
        } else {
          console.error('SSE connection error:', error)
          this.$message.error('连接错误: ' + error.message)
        }
        this.stopLogStream()
      }
    },
    
    stopLogStream(manual = false) {
      // 中止 fetch 请求
      if (this.abortController) {
        this.abortController.abort()
        this.abortController = null
      }
      
      this.isStreaming = false
      this.connecting = false
      this.stopAutoRefresh()
      
      if (manual) {
        this.manualStop = true
        // 清除重连定时器
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer)
          this.reconnectTimer = null
        }
        this.$message.info('已停止接收日志')
      } else {
        // 非手动停止，安排重连
        this.scheduleReconnect()
      }
    },
    
    scheduleReconnect() {
      // 清除之前的重连定时器
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
      }
      
      // 2秒后重连
      this.reconnectTimer = setTimeout(() => {
        if (!this.manualStop) {
          console.log('Attempting to reconnect...')
          this.startLogStream()
        }
      }, 2000)
    },
    
    addLog(message) {
      // 跳过空消息
      if (!message || !message.trim()) {
        return
      }
      
      // 将日志加入待处理队列
      this.pendingLogs.push(message)
      
      // 使用 requestAnimationFrame 批量处理，避免频繁更新
      if (!this.updateTimer) {
        this.updateTimer = requestAnimationFrame(() => {
          this.flushPendingLogs()
          this.updateTimer = null
        })
      }
    },
    
    flushPendingLogs() {
      if (this.pendingLogs.length === 0) return
      
      // 批量添加日志
      const newLogs = this.pendingLogs.splice(0)
      this.logs.push(...newLogs)
      
      // 限制日志数量，避免内存溢出
      // 当日志数超过阈值时，删除最旧的 logsToRemove 行
      if (this.logs.length > this.maxLogs) {
        const keepCount = this.logs.length - this.logsToRemove
        this.logs = this.logs.slice(-keepCount)
        console.log(`日志数量超过 ${this.maxLogs}，已删除最旧的 ${this.logsToRemove} 行，当前剩余 ${this.logs.length} 行`)
      }
      
      // 根据自动滚动开关决定是否滚动
      if (this.autoScroll) {
        // 使用 $nextTick 确保 DOM 更新完成后再滚动
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }
    },
    
    handleFilterChange() {
      // 过滤条件变化时自动更新显示
      this.$nextTick(() => {
        if (this.autoScroll) {
          this.scrollToBottom()
        }
      })
    },
    
    handleAutoScrollChange(value) {
      // 开启自动滚动时，立即滚动到底部
      if (value) {
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }
    },
    
    handleScroll(e) {
      // 记录滚动位置
      this.lastScrollTop = e.target.scrollTop
    },
    
    clearLogs() {
      // 清空日志
      this.$confirm('确定要清空所有日志吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.logs = []
        this.pendingLogs = []
        // 强制刷新 log-viewer 组件
        this.drawerViewerKey++
        this.$message.success('日志已清空')
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    showHelp() {
      this.$alert(`
        <div style="text-align: left; line-height: 1.8;">
          <h3>📋 使用说明</h3>
          <ul style="padding-left: 20px;">
            <li><strong>超时时间</strong>：设置 SSE 连接的超时时间（秒）</li>
            <li><strong>查看日志</strong>：连接到 Maximo 日志流</li>
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
            <li><span style="color:#f00;font-weight:bold;">FATAL</span> - 鲜红：严重错误（脉冲动画）</li>
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
      if (value && this.isStreaming) {
        this.startAutoRefresh()
      } else {
        this.stopAutoRefresh()
      }
    },
    
    handleIntervalChange() {
      // 刷新间隔改变时，如果正在流式传输，重新连接以应用新的 timeout
      if (this.isStreaming) {
        this.stopLogStream(true)
        setTimeout(() => {
          this.startLogStream()
        }, 500)
      }
    },
    
    startAutoRefresh() {
      this.countdown = this.refreshInterval
      
      // 倒计时定时器
      this.countdownTimer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          this.countdown = this.refreshInterval
        }
      }, 1000)
      
      // 刷新定时器 - 定期重新连接以获取新日志
      this.timer = setInterval(() => {
        // SSE 会自动保持连接，这里只是重置倒计时
        // 如果需要定期重新连接，可以调用:
        // this.stopLogStream()
        // setTimeout(() => this.startLogStream(), 500)
      }, this.refreshInterval * 1000)
    },
    
    stopAutoRefresh() {
      this.clearTimers()
      this.countdown = 0
    },
    
    clearTimers() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
        this.countdownTimer = null
      }
    },
    
    scrollToBottom() {
      // 直接滚动到底部，不使用防抖
      const container = this.$refs.logContainer
      if (container) {
        // 使用 requestAnimationFrame 确保在下一帧执行滚动
        requestAnimationFrame(() => {
          container.scrollTop = container.scrollHeight
          this.lastScrollTop = container.scrollHeight
        })
      }
    },
    
    highlightLog(log) {
      if (!this.filterKeyword) {
        return this.escapeHtml(log)
      }
      
      const escaped = this.escapeHtml(log)
      const keyword = this.escapeHtml(this.filterKeyword)
      const regex = new RegExp(`(${keyword})`, 'gi')
      return escaped.replace(regex, '<span class="highlight">$1</span>')
    },
    
    formatLogLine(log) {
      // Java 日志高亮方案（极致性能优化版）
      let formatted = this.escapeHtml(log)
      
      // 1. 高亮日志级别（最先执行，避免被其他规则干扰）
      // 使用更高效的检测方式
      if (formatted.includes('DEBUG') || formatted.includes('INFO') || 
          formatted.includes('WARN') || formatted.includes('ERROR') || 
          formatted.includes('FATAL') || formatted.includes('TRACE')) {
        formatted = formatted.replace(/\b(DEBUG|INFO|WARN|WARNING|ERROR|FATAL|TRACE)\b/g, (match) => {
          const levelClass = this.getLogLevelClass(match)
          return `<span class="log-level ${levelClass}">${match}</span>`
        })
      }
      
      // 2. 高亮异常类型（在类名之前，避免冲突）
      if (formatted.includes('Exception') || formatted.includes('Error')) {
        formatted = formatted.replace(/\b([A-Z][a-zA-Z]*Exception|[A-Z][a-zA-Z]*Error)\b/g, '<span class="log-exception">$&</span>')
      }
      
      // 3. 高亮类名（仅当包含点号和大写字母时）
      if (formatted.includes('.') && /[A-Z]/.test(formatted)) {
        formatted = formatted.replace(/([a-z]+\.)+[A-Z][a-zA-Z0-9_]*/g, '<span class="log-class">$&</span>')
      }
      
      // 4. 高亮时间戳（仅当日志看起来像有时间戳）
      if (/\d{4}-\d{2}-\d{2}/.test(formatted)) {
        formatted = formatted.replace(/\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}(?:\.\d+)?/g, '<span class="log-timestamp">$&</span>')
      }
      
      // 5. 如果有过滤关键词，额外高亮
      if (this.filterKeyword) {
        const keyword = this.escapeHtml(this.filterKeyword)
        const filterRegex = new RegExp(`(${keyword})`, 'gi')
        formatted = formatted.replace(filterRegex, '<span class="highlight">$1</span>')
      }
      
      return formatted
    },
    
    getLogLevelClass(level) {
      const levelMap = {
        'DEBUG': 'debug',
        'TRACE': 'debug',
        'INFO': 'info',
        'WARN': 'warn',
        'WARNING': 'warn',
        'ERROR': 'error',
        'FATAL': 'fatal'
      }
      return levelMap[level.toUpperCase()] || 'info'
    },
    
    escapeHtml(text) {
      const div = document.createElement('div')
      div.textContent = text
      return div.innerHTML
    },
    
    highlightLogLine(line) {
      let highlighted = line
      
      // ANSI 转义码定义
      const ANSI = {
        reset: '\x1b[0m',
        bold: '\x1b[1m',
        underline: '\x1b[4m',
        // 前景色
        black: '\x1b[30m',
        red: '\x1b[31m',
        green: '\x1b[32m',
        yellow: '\x1b[33m',
        blue: '\x1b[34m',
        magenta: '\x1b[35m',
        cyan: '\x1b[36m',
        white: '\x1b[37m',
        // 亮色
        brightRed: '\x1b[91m',
        brightGreen: '\x1b[92m',
        brightYellow: '\x1b[93m',
        brightBlue: '\x1b[94m',
        brightMagenta: '\x1b[95m',
        brightCyan: '\x1b[96m',
        brightWhite: '\x1b[97m'
      }
      
      // 1. 日志级别高亮（带颜色）
      // FATAL - 鲜红色 + 粗体
      highlighted = highlighted.replace(
        /\b(FATAL)\b/g,
        `${ANSI.brightRed}${ANSI.bold}$1${ANSI.reset}`
      )
      // ERROR - 红色
      highlighted = highlighted.replace(
        /\b(ERROR)\b/g,
        `${ANSI.red}$1${ANSI.reset}`
      )
      // WARN/WARNING - 黄色
      highlighted = highlighted.replace(
        /\b(WARN|WARNING)\b/g,
        `${ANSI.yellow}$1${ANSI.reset}`
      )
      // INFO - 绿色
      highlighted = highlighted.replace(
        /\b(INFO)\b/g,
        `${ANSI.green}$1${ANSI.reset}`
      )
      // DEBUG/TRACE - 蓝色
      highlighted = highlighted.replace(
        /\b(DEBUG|TRACE)\b/g,
        `${ANSI.blue}$1${ANSI.reset}`
      )
      
      // 2. 类名高亮（青色）- 匹配 com.xxx.yyy.ClassName 格式
      highlighted = highlighted.replace(
        /\b([a-z]+(?:\.[a-z]+)+\.[A-Z][a-zA-Z0-9]*)\b/g,
        `${ANSI.cyan}$1${ANSI.reset}`
      )
      
      // 3. 异常类型高亮（粉红色粗体加下划线）
      highlighted = highlighted.replace(
        /\b((?:[A-Z][a-zA-Z0-9]*Exception|[A-Z][a-zA-Z0-9]*Error))\b/g,
        `${ANSI.brightMagenta}${ANSI.bold}${ANSI.underline}$1${ANSI.reset}`
      )
      
      // 4. 数字高亮（紫色）
      highlighted = highlighted.replace(
        /\b(\d+)\b/g,
        `${ANSI.magenta}$1${ANSI.reset}`
      )
      
      // 5. 过滤关键词高亮（黄色粗体）
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
