<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>消息查询</h2>
          <p class="page-summary">支持 msgid / msggroup + msgkey 查询，环境认证信息由配置自动加载。</p>
        </div>
      </div>

      <el-form :model="formData" class="query-form" label-width="100px" label-position="top">
        <el-divider />

        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="查询方式">
              <el-radio-group v-model="searchMode">
                <el-radio label="msgid">msgid</el-radio>
                <el-radio label="groupKey">msggroup + msgkey</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="消息 ID">
              <el-input v-model="formData.msgid" :disabled="searchMode !== 'msgid'" placeholder="BMXAA6378I" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="消息组">
              <el-input v-model="formData.msggroup" :disabled="searchMode !== 'groupKey'" placeholder="system" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="消息键">
              <el-input v-model="formData.msgkey" :disabled="searchMode !== 'groupKey'" placeholder="crontaskmanager25" />
            </el-form-item>
          </el-col>
          <el-col :span="12" class="button-column">
            <el-button type="primary" @click="handleQuery" :loading="loading">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-col>
        </el-row>
      </el-form>

      <div class="result-panel">
        <el-alert v-if="error" :title="error" type="error" show-icon closable @close="error = ''" />
        <el-card v-if="!error && messages.length === 0" class="empty-card">暂无查询结果，请输入条件后点击查询。</el-card>
        <el-table v-if="messages.length > 0" :data="messages" stripe style="width: 100%" class="result-table">
          <el-table-column prop="msgid" label="msgid" />
          <el-table-column prop="msggroup" label="msggroup" />
          <el-table-column prop="msgkey" label="msgkey" />
          <el-table-column prop="value" label="value" />
          <el-table-column prop="displaymethod_description" label="displaymethod" />
        </el-table>
      </div>
    </el-card>
  </section>
</template>

<script>
import { mapGetters } from 'vuex'
import { queryMessages } from '../../api/messages'

export default {
  name: 'MessagesQuery',
  data() {
    return {
      searchMode: 'msgid',
      loading: false,
      error: '',
      messages: [],
      formData: {
        msgid: 'BMXAA6378I',
        msggroup: '',
        msgkey: ''
      }
    }
  },
  computed: {
    ...mapGetters([
      'selectedEnv'
    ])
  },
  methods: {
    async handleQuery() {
      this.loading = true
      this.error = ''
      this.messages = []

      try {
        if (this.searchMode === 'msgid' && !this.formData.msgid) {
          throw new Error('请先输入 msgid。')
        }
        if (this.searchMode === 'groupKey' && (!this.formData.msggroup || !this.formData.msgkey)) {
          throw new Error('请填写 msggroup 和 msgkey。')
        }

        const env = this.selectedEnv
        const params = {
          baseUrl: env.baseUrl,
          maxauth: env.maxauth,
          apiKey: env.apiKey,
          useMaxauth: !env.useApiKey,
          useApiKey: env.useApiKey,
          msgid: this.searchMode === 'msgid' ? this.formData.msgid : undefined,
          msggroup: this.searchMode === 'groupKey' ? this.formData.msggroup : undefined,
          msgkey: this.searchMode === 'groupKey' ? this.formData.msgkey : undefined
        }

        this.messages = await queryMessages(params)
      } catch (err) {
        this.error = err instanceof Error ? err.message : String(err)
      } finally {
        this.loading = false
      }
    },
    resetForm() {
      this.formData = {
        msgid: '',
        msggroup: '',
        msgkey: ''
      }
      this.searchMode = 'msgid'
      this.error = ''
      this.messages = []
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
.button-column {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 30px;
}
.result-panel {
  margin-top: 20px;
}
.empty-card {
  margin-top: 16px;
  padding: 20px;
  color: #909399;
}
.result-table {
  margin-top: 16px;
}
</style>