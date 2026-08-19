<template>
  <section class="audit-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>应用XML审计记录</h2>
          <p class="page-summary">查询 A_MAXPRESENTATION 审计记录，支持 APP 过滤，按交易ID倒序。</p>
        </div>
      </div>

      <AuditPanel :app="routeApp" />
    </el-card>
  </section>
</template>

<script>
import AuditPanel from './AuditPanel.vue'

export default {
  name: 'MaxAppXmlAudit',
  components: {
    AuditPanel
  },
  data() {
    return {
      routeApp: ''
    }
  },
  created() {
    const app = this.$route.query && this.$route.query.app
    if (app) {
      this.routeApp = String(app)
    }
  }
}
</script>

<style lang="scss" scoped>
.audit-page {
  padding: 16px;
}
.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  h2 {
    margin: 0 0 6px 0;
  }
}
.page-summary {
  color: #606266;
  margin: 0;
}
</style>