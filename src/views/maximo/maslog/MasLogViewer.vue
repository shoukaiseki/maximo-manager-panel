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
          size="small"
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
          size="small"
          @click="stopLogStream"
        >
          停止接收
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
        
        <el-tag v-if="isStreaming" type="info" size="small" style="margin-left: 12px;">
          已接收: {{ logs.length }} 条
        </el-tag>

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
        <div v-if="!logs || logs.length === 0" class="empty-log">
          <i class="el-icon-document-delete" style="font-size: 48px; color: #dcdfe6;"></i>
          <p>暂无日志数据，请点击"查看日志"按钮开始接收</p>
        </div>
        
        <div v-else class="log-content" ref="logContent">
          <div 
            v-for="(log, index) in filteredLogs" 
            :key="index"
            class="log-line"
            v-html="formatLogLine(log)"
          ></div>
        </div>
      </div>
    </el-card>
  </section>
</template>

<script>
import { mapGetters } from 'vuex'
import request from '@/utils/request'

export default {
  name: 'MasLogViewer',
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
      maxLogs: 300, // 最大保留日志条数（进一步减少以避免卡顿）
      manualStop: false, // 标记是否手动停止
      reconnectTimer: null, // 重连定时器
      autoScroll: true, // 自动滚动开关
      updateTimer: null, // 批量更新定时器
      pendingLogs: [], // 待处理的日志队列
      scrollDebounceTimer: null // 滚动防抖定时器
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
      
      // 限制显示的日志数量，避免 DOM 过多导致卡顿
      // 只保留最后 100 条用于显示（进一步减少）
      const maxDisplayLogs = 100
      if (logs.length > maxDisplayLogs) {
        return logs.slice(-maxDisplayLogs)
      }
      
      return logs
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
      if (this.logs.length > this.maxLogs) {
        this.logs = this.logs.slice(-this.maxLogs)
      }
      
      // 根据自动滚动开关决定是否滚动
      if (this.autoScroll) {
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
      // 使用防抖，避免频繁滚动
      if (this.scrollDebounceTimer) {
        clearTimeout(this.scrollDebounceTimer)
      }
      
      this.scrollDebounceTimer = setTimeout(() => {
        const container = this.$refs.logContent
        if (container) {
          // 强制滚动到底部
          container.scrollTop = container.scrollHeight
        }
      }, 50) // 50ms 防抖
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
      // Java 日志高亮方案（性能优化版）
      let formatted = this.escapeHtml(log)
      
      // 1. 高亮日志级别（最先执行，避免被其他规则干扰）
      if (/(DEBUG|INFO|WARN|WARNING|ERROR|FATAL|TRACE)/.test(formatted)) {
        formatted = formatted.replace(/\b(DEBUG|INFO|WARN|WARNING|ERROR|FATAL|TRACE)\b/g, (match) => {
          const levelClass = this.getLogLevelClass(match)
          return `<span class="log-level ${levelClass}">${match}</span>`
        })
      }
      
      // 2. 高亮异常类型（在类名之前，避免冲突）
      if (/[A-Z][a-zA-Z]*(Exception|Error)\b/.test(formatted)) {
        formatted = formatted.replace(/\b([A-Z][a-zA-Z]*Exception|[A-Z][a-zA-Z]*Error)\b/g, '<span class="log-exception">$&</span>')
      }
      
      // 3. 高亮类名
      if (/([a-z]+\.)+[A-Z]/.test(formatted)) {
        formatted = formatted.replace(/([a-z]+\.)+[A-Z][a-zA-Z0-9_]*/g, '<span class="log-class">$&</span>')
      }
      
      // 4. 高亮时间戳
      if (/\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}/.test(formatted)) {
        formatted = formatted.replace(/\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}(?:\.\d+)?/g, '<span class="log-timestamp">$&</span>')
      }
      
      // 5. 高亮关键词
      if (/\b(null|true|false|undefined)\b/.test(formatted)) {
        formatted = formatted.replace(/\b(null|true|false|undefined)\b/g, '<span class="log-keyword">$&</span>')
      }
      
      // 6. 高亮数字（最后执行，避免影响其他规则）
      if (/\b\d+\.?\d*\b/.test(formatted)) {
        formatted = formatted.replace(/\b\d+\.?\d*\b/g, '<span class="log-number">$&</span>')
      }
      
      // 7. 如果有过滤关键词，额外高亮
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
  min-height: 500px;
  max-height: 700px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #1e1e1e;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.empty-log {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #909399;
  
  p {
    margin-top: 16px;
    font-size: 14px;
  }
}

.log-content {
  padding: 12px;
  
  .log-line {
    padding: 3px 0;
    color: #d4d4d4;
    white-space: pre-wrap;
    word-break: break-all;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.6;
    border-left: 3px solid transparent;
    padding-left: 8px;
    transition: all 0.2s;
    
    &:hover {
      background-color: rgba(255, 255, 255, 0.05);
      border-left-color: rgba(255, 255, 255, 0.2);
    }
    
    // 日志级别颜色
    ::v-deep .log-level {
      font-weight: bold;
      padding: 1px 4px;
      border-radius: 2px;
      margin-right: 4px;
      
      &.debug {
        color: #6a9fb5;
        background-color: rgba(106, 159, 181, 0.1);
      }
      
      &.info {
        color: #90a959;
        background-color: rgba(144, 169, 89, 0.1);
      }
      
      &.warn {
        color: #d28445;
        background-color: rgba(210, 132, 69, 0.1);
      }
      
      &.error {
        color: #ac4142;
        background-color: rgba(172, 65, 66, 0.15);
      }
      
      &.fatal {
        color: #f00;
        background-color: rgba(255, 0, 0, 0.2);
        animation: pulse 1.5s ease-in-out infinite;
      }
    }
    
    // 时间戳
    ::v-deep .log-timestamp {
      color: #75715e;
      font-style: italic;
    }
    
    // 类名
    ::v-deep .log-class {
      color: #66d9ef;
      font-weight: 500;
    }
    
    // 异常类型
    ::v-deep .log-exception {
      color: #f92672;
      font-weight: bold;
      text-decoration: underline;
      text-decoration-color: rgba(249, 38, 114, 0.3);
    }
    
    // 数字
    ::v-deep .log-number {
      color: #ae81ff;
    }
    
    // 关键词
    ::v-deep .log-keyword {
      color: #f92672;
      font-weight: bold;
    }
    
    // 过滤高亮
    ::v-deep .highlight {
      background-color: #ffeb3b;
      color: #000;
      padding: 1px 3px;
      border-radius: 2px;
      font-weight: bold;
      box-shadow: 0 0 4px rgba(255, 235, 59, 0.5);
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
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
