<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>MaxSession 在线用户管理</h2>
          <p class="page-summary">查询当前 Maximo 在线用户，支持多选并踢出用户。</p>
        </div>
      </div>

      <el-form :inline="true" @submit.native.prevent>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button type="danger" icon="el-icon-delete" size="mini" :disabled="selectedUsers.length === 0" :loading="kicking" @click="handleKick">踢出用户 ({{ selectedUsers.length }})</el-button>
        </el-form-item>
        <el-form-item v-if="lastQueryTime">
          <span style="color:#909399;font-size:12px">上次查询: {{ lastQueryTime }} | 在线用户: {{ onlineUsers.length }} 人</span>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <el-table
          ref="userTable"
          v-loading="loading"
          :data="onlineUsers"
          stripe
          style="width: 100%"
          class="result-table"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column prop="userId" label="用户 ID" min-width="180" />
        </el-table>
        <el-empty v-if="!loading && onlineUsers.length === 0 && queried" description="暂无在线用户" />
      </div>
    </el-card>

    <!-- API 调用日志 -->
    <el-card class="log-card">
      <div class="page-header-row">
        <div>
          <h3>接口调用日志</h3>
        </div>
        <div>
          <el-button size="mini" icon="el-icon-delete" @click="clearLog">清空日志</el-button>
        </div>
      </div>
      <div ref="monacoContainer" class="monaco-container" />
    </el-card>
  </section>
</template>

<script>
import request from '@/utils/request'

const SCRIPT_API = 'api/script/SKS_MXSESSION_REMOVE'

export default {
  name: 'MaxSession',
  data() {
    return {
      loading: false,
      kicking: false,
      queried: false,
      lastQueryTime: '',
      onlineUsers: [],
      selectedUsers: [],
      logs: [],
      editor: null
    }
  },
  computed: {
    logContent() {
      return this.logs.join('\n')
    }
  },
  watch: {
    logContent() {
      this.$nextTick(() => {
        this.updateEditorContent()
      })
    }
  },
  mounted() {
    this.initEditor()
  },
  beforeDestroy() {
    if (this.editor) {
      this.editor.dispose()
      this.editor = null
    }
  },
  methods: {
    appendLog(type, msg) {
      const now = new Date()
      const time = now.toLocaleTimeString('zh-CN', { hour12: false })
      this.logs.push(`[${time}] [${type}] ${msg}`)
    },
    appendLogResult(data) {
      const formatted = JSON.stringify(data, null, 2)
      this.logs.push(formatted)
    },
    clearLog() {
      this.logs = []
    },
    async initEditor() {
      try {
        const monaco = await import(/* webpackChunkName: "monaco" */ 'monaco-editor')
        this.$nextTick(() => {
          if (!this.$refs.monacoContainer) return
          this.editor = monaco.editor.create(this.$refs.monacoContainer, {
            value: '',
            language: 'json',
            readOnly: true,
            theme: 'vs',
            automaticLayout: true,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            fontSize: 13,
            wordWrap: 'on',
            lineNumbers: 'off'
          })
        })
      } catch (err) {
        console.error('Monaco Editor 加载失败:', err)
      }
    },
    updateEditorContent() {
      if (this.editor) {
        this.editor.setValue(this.logContent)
      }
    },
    handleSelectionChange(selection) {
      this.selectedUsers = selection.map(row => row.userId)
    },
    async handleQuery() {
      this.loading = true
      this.queried = true
      this.appendLog('INFO', '开始查询在线用户...')

      try {
        const res = await request({
          url: SCRIPT_API,
          method: 'post',
          params: {
            sessionManager: 'full',
              action: 'query',
            _langcode: 'ZH'
          }
        })
        console.log('MaxSession 查询结果', res)

        this.appendLog('SUCCESS', '查询成功')
        this.appendLogResult(res.data || res)

        if (res.data && res.data.onlineUsers) {
          this.onlineUsers = res.data.onlineUsers.map(userId => ({ userId }))
          this.lastQueryTime = new Date().toLocaleTimeString('zh-CN', { hour12: false })
        } else if (res.data && res.data.status === 'success') {
          this.onlineUsers = (res.data.onlineUsers || []).map(userId => ({ userId }))
          this.lastQueryTime = new Date().toLocaleTimeString('zh-CN', { hour12: false })
        } else {
          this.onlineUsers = []
          this.appendLog('WARN', '接口返回格式异常: ' + JSON.stringify(res.data))
        }
      } catch (err) {
        this.msgError('查询在线用户失败: ' + (err.message || String(err)))
        this.appendLog('ERROR', '查询失败: ' + (err.message || String(err)))
        this.onlineUsers = []
      } finally {
        this.loading = false
      }
    },
    async handleKick() {
      if (this.selectedUsers.length === 0) {
        this.msgInfo('请先选择要踢出的用户')
        return
      }

      this.kicking = true
      this.appendLog('INFO', '开始踢出用户...')

      for (const userId of this.selectedUsers) {
        this.appendLog('INFO', `正在踢出用户: ${userId}`)
        try {
          const res = await request({
            url: SCRIPT_API,
            method: 'post',
            params: {
              sessionManager: 'full',
              action: 'disconnect',
              _langcode: 'ZH',
              userId: userId
            }
          })
          this.appendLog('SUCCESS', `踢出用户 ${userId} 成功`)
          this.appendLogResult(res.data || res)
        } catch (err) {
          this.appendLog('ERROR', `踢出用户 ${userId} 失败: ${err.message || String(err)}`)
        }
      }

      this.appendLog('INFO', '踢出操作完成')
      this.msgSuccess('踢出操作完成')
      this.kicking = false

      // 踢出完成后自动刷新列表
      await this.handleQuery()
    }
  }
}
</script>

<style lang="scss" scoped>
.query-page {
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
.result-panel {
  margin-top: 8px;
}
.result-table {
  margin-top: 8px;
}
.log-card {
  margin-top: 16px;
}
.monaco-container {
  height: 300px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
