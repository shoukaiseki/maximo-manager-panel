<template>
  <section class="audit-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>自动化脚本审计记录</h2>
          <p class="page-summary">查询 A_AUTOSCRIPT 审计记录，支持脚本名称、脚本内容过滤，按交易ID倒序。</p>
        </div>
      </div>

      <AutoScriptAuditPanel :autoscript="routeAutoscript" />
    </el-card>
  </section>
</template>

<script>
import AutoScriptAuditPanel from './AutoScriptAuditPanel.vue'

export default {
  name: 'AutoScriptAudit',
  components: {
    AutoScriptAuditPanel
  },
  data() {
    return {
      routeAutoscript: ''
    }
  },
  created() {
    const autoscript = this.$route.query && this.$route.query.autoscript
    if (autoscript) {
      this.routeAutoscript = String(autoscript)
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
