<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>消息查询</h2>
          <p class="page-summary">支持 msgid / msggroup + msgkey 查询，环境认证信息由配置自动加载。</p>
        </div>
      </div>
     <el-form :model="formData" ref="queryForm" :inline="true" label-width="68px" @submit.native.prevent>
            <el-form-item label="消息 ID">
              <el-input v-model="formData.msgid" placeholder="BMXAA6378I"  @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item label="消息组">
              <el-input v-model="formData.msggroup" placeholder="system"  @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item label="消息键">
              <el-input v-model="formData.msgkey" placeholder="crontaskmanager25"  @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item label="消息内容">
              <el-input v-model="formData.value" placeholder="模糊搜索 value 内容"  @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item>
                <el-button type="cyan" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
            </el-form-item>
        </el-form>


      <!-- <el-form :model="formData" class="query-form" label-width="100px" label-position="top">
        <el-divider />

        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="消息 ID">
              <el-input v-model="formData.msgid" placeholder="BMXAA6378I" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="消息组">
              <el-input v-model="formData.msggroup" placeholder="system" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="消息键">
              <el-input v-model="formData.msgkey" placeholder="crontaskmanager25" />
            </el-form-item>
          </el-col>
          <el-col :span="12" class="button-column">
            <el-button type="primary" @click="handleQuery" :loading="loading">查询</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-col>
        </el-row>
      </el-form> -->

      <div class="result-panel">
        <!-- <el-alert v-if="error" :title="error" type="error" show-icon closable @close="error = ''" />
        <el-card v-if="!error && messages&&messages.length === 0" class="empty-card">暂无查询结果，请输入条件后点击查询。</el-card> -->
        <el-table loading="loading" :data="messages" stripe style="width: 100%" class="result-table">
          <el-table-column prop="msgid" label="msgid" />
          <el-table-column prop="msggroup" label="msggroup" />
          <el-table-column prop="msgkey" label="msgkey" />
          <el-table-column prop="value" label="value" />
          <el-table-column prop="displaymethod_description" label="displaymethod" />
        </el-table>
       <pagination v-show="tablePatam.total > 0"
            :total="tablePatam.total"
            :page.sync="tablePatam.pageNum"
            :limit.sync="tablePatam.pageSize"
            @pagination="getList"
        />
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
      tablePatam:{
        total: 0,
        pageNum: 1,
        pageSize: 10
      },
      loading: false,
      error: '',
      messages: [],
      formData: {
        msgid: '',
        // msgid: 'BMXAA6378I',
        msggroup: '',
        msgkey: '',
        value: ''
      }
    }
  },
  computed: {
    ...mapGetters([
      'selectedEnv'
    ])
  },
  methods: {
    buildWhere(params) {
      let clauses = []
      if (params.msgid) {
        clauses.push(`msgid="${params.msgid}"`)
      }
      if (params.msggroup) {
        clauses.push(`msggroup="${params.msggroup}"`)
      }
      if (params.msgkey) {
        clauses.push(`msgkey="${params.msgkey}"`)
      }
      if (params.value) {
        clauses.push(`value="%${params.value}%"`)
      }
      return clauses.join(' and ')
    },
    async handleQuery() {
      this.tablePatam.pageNum=1
      this.getList()
    },
    getList() {
      this.loading = true
      this.error = ''
      this.messages = []

      try {

        const env = this.selectedEnv
        const params = {
          msgid:  this.formData.msgid ,
          msggroup:this.formData.msggroup ,
          msgkey:  this.formData.msgkey ,
          value:   this.formData.value 
        }

        const whereClause = this.buildWhere(params)
        console.log("whereClause=", whereClause)
//         oslc.paging   是否启用分页,是否启用只判断字符串是否为 TRUE (不区分大小写),另外 oslc.pageSize 不为空时,默认启用分页功能
// oslc.pageSize 每页记录条数,如果参数为空,oslc.paging为TRUE,接口查询结果是默认显示20条
// pageno   分页页码,默认第一页

        const queryParams={
          lean: '1',
          'collectioncount': 1,
          'oslc.select': '*',
          'oslc.where': whereClause&&whereClause!==''?whereClause: undefined,
          'oslc.paging': 'true',
          'oslc.pageSize': this.tablePatam.pageSize,
          'pageno': this.tablePatam.pageNum
        }

        queryMessages(queryParams).then(res => {
          console.log("查询结果", res)
          this.messages = res.data.member || []
          this.tablePatam.total=res.data.responseInfo&&res.data.responseInfo.totalCount||this.messages.length
          this.loading=false
        }).catch(err => {
          this.msgError("查询消息失败：" + err.message)
          this.loading=false
        })
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
        msgkey: '',
        value: ''
      }
      this.searchMode = 'msgid'
      this.error = ''
      this.tablePatam.pageNum=1
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