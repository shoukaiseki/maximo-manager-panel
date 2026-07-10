<template>
  <section class="querydb-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>数据库搜索</h2>
          <p class="page-summary">在所有 ALN/UPPER 类型字段中搜索关键词，支持模糊搜索和精确搜索。</p>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="100px" @submit.native.prevent>
        <el-form-item label="搜索类型">
          <el-select v-model="formData.searchType" placeholder="请选择搜索类型" style="width: 150px;">
            <el-option label="模糊搜索" value="LIKE" />
            <el-option label="精确搜索" value="EQUALS" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索关键词">
          <el-input 
            v-model="formData.keyword" 
            placeholder="输入搜索关键词" 
            clearable 
            style="width: 350px;" 
            @keyup.enter.native="handleSearch"
            :disabled="searching"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" :loading="searching" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div v-if="searching" class="progress-section">
        <div class="progress-info">
          <span>正在搜索中...</span>
          <span style="margin-left: 20px;">已扫描表/字段: {{ scannedCount }}</span>
          <span style="margin-left: 20px;">匹配结果: {{ results.length }}</span>
        </div>
        <el-progress :percentage="progressPercent" :stroke-width="12" status="success" />
        <div class="last-update">
          <span>最后更新: {{ lastUpdateInfo }}</span>
        </div>
      </div>

      <div class="result-panel">
        <div v-if="results.length > 0" class="result-summary">
          <el-tag type="success" size="small">共找到 {{ results.length }} 条匹配结果</el-tag>
          <el-tag type="info" size="small" style="margin-left: 10px;" v-if="totalTime">总耗时: {{ formatTime(totalTime) }}</el-tag>
        </div>
        
        <el-table :data="results" border style="width: 100%;" v-if="results.length > 0">
          <el-table-column prop="ENTITYNAME" label="表名" min-width="180" />
          <el-table-column prop="COLUMNNAME" label="字段名" min-width="150" />
          <el-table-column prop="MAXTYPE" label="类型" width="100" />
          <el-table-column prop="COUNT" label="匹配条数" width="120">
            <template slot-scope="scope">
              <el-tag type="warning" size="small">{{ scope.row.COUNT }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="QUERY_SQL" label="查询语句" min-width="300" show-overflow-tooltip />
          <el-table-column prop="TIME_MS" label="耗时(ms)" width="120">
            <template slot-scope="scope">
              <span :style="{ color: scope.row.TIME_MS > 1000 ? '#f56c6c' : '#67c23a' }">{{ scope.row.TIME_MS }}</span>
            </template>
          </el-table-column>
        </el-table>
        
        <el-empty v-if="!searching && results.length === 0 && hasSearched" description="未找到匹配结果" />
        <el-empty v-if="!searching && results.length === 0 && !hasSearched" description="请输入关键词后点击查询" />
      </div>
    </el-card>
  </section>
</template>

<script>
import solonapi from '@/api/solonapi'

export default {
  name: 'QueryDb',
  data() {
    return {
      formData: {
        searchType: 'EQUALS',
        keyword: ''
      },
      searching: false,
      hasSearched: false,
      results: [],
      totalTime: 0,
      scannedCount: 0,
      totalFields: 0,
      lastUpdateInfo: '',
      eventSource: null,
      authHeaders: {}
    }
  },
  computed: {
    progressPercent() {
      if (this.totalFields === 0) return 0
      return Math.min(Math.round((this.scannedCount / this.totalFields) * 100), 100)
    }
  },
  beforeDestroy() {
    this.closeEventSource()
  },
  methods: {
    async handleSearch() {
      if (!this.formData.keyword.trim()) {
        this.$message.warning('请输入搜索关键词')
        return
      }

      this.closeEventSource()
      this.searching = true
      this.hasSearched = true
      this.results = []
      this.totalTime = 0
      this.scannedCount = 0
      this.lastUpdateInfo = ''

      this.authHeaders = {}
      const maximoEnvSettings = localStorage.getItem('maximo-env-settings')
      if (maximoEnvSettings) {
        try {
          const settings = JSON.parse(maximoEnvSettings)
          if (settings.useApiKey && settings.apiKey) {
            this.authHeaders['apiKey'] = settings.apiKey
          }
        } catch (e) {
          console.error('解析配置失败', e)
        }
      }

      if (!this.authHeaders.apiKey) {
        this.$message.error('请先配置 API Key！')
        this.searching = false
        return
      }

      const baseUrl = process.env.VUE_APP_BASE_API || ''
      const exactMatch = this.formData.searchType === 'EQUALS'
      const url = `${baseUrl}/solonapi/querydb/search?keyword=${encodeURIComponent(this.formData.keyword.trim())}&exactMatch=${exactMatch}&apiKey=${encodeURIComponent(this.authHeaders.apiKey)}`

      console.log('Starting search:', url)

      this.eventSource = new EventSource(url)

      this.eventSource.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.type === 'progress') {
            this.scannedCount = data.current
            this.totalFields = data.total
            const now = new Date()
            this.lastUpdateInfo = `${now.toLocaleTimeString()} - 已扫描 ${data.current}/${data.total}`
          } else if (data.type === 'result') {
            this.results.push(data.data)
            const now = new Date()
            this.lastUpdateInfo = `${now.toLocaleTimeString()} - ${data.data.ENTITYNAME}.${data.data.COLUMNNAME}: ${data.data.COUNT} 条`
          } else if (data.type === 'completed') {
            this.totalTime = data.totalTime
            this.searching = false
            this.lastUpdateInfo = `搜索完成，共耗时 ${this.formatTime(data.totalTime)}`
            this.closeEventSource()
          } else if (data.type === 'error') {
            this.$message.error('搜索失败: ' + data.message)
            this.searching = false
            this.closeEventSource()
          }
        } catch (e) {
          console.error('解析SSE消息失败:', e)
        }
      }

      this.eventSource.onerror = (error) => {
        console.error('SSE error:', error)
        this.searching = false
        this.$message.error('搜索连接异常')
        this.closeEventSource()
      }
    },
    closeEventSource() {
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
      }
    },
    resetForm() {
      this.closeEventSource()
      this.formData = {
        searchType: 'LIKE',
        keyword: ''
      }
      this.searching = false
      this.hasSearched = false
      this.results = []
      this.totalTime = 0
      this.scannedCount = 0
      this.totalFields = 0
      this.lastUpdateInfo = ''
    },
    formatTime(ms) {
      if (ms < 1000) {
        return ms + 'ms'
      } else if (ms < 60000) {
        return (ms / 1000).toFixed(2) + 's'
      } else {
        const minutes = Math.floor(ms / 60000)
        const seconds = ((ms % 60000) / 1000).toFixed(2)
        return minutes + 'm ' + seconds + 's'
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.querydb-page {
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
  font-size: 13px;
}

.progress-section {
  margin-top: 20px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.progress-info {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
}

.last-update {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}

.result-panel {
  margin-top: 20px;
}

.result-summary {
  margin-bottom: 12px;
}

.el-table__body tr:hover>td {
  background-color: #ecf5ff !important;
}
</style>
