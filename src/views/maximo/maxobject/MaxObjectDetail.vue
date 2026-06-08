<template>
  <section class="detail-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/maxobject/index' }">MaxObject 查询</el-breadcrumb-item>
            <el-breadcrumb-item>{{ objectName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div>
          <el-button icon="el-icon-back" size="small" @click="goBack">返回列表</el-button>
        </div>
      </div>

      <el-divider content-position="left">基本信息</el-divider>
      <el-form v-if="Object.keys(mainInfo).length > 0" label-width="130px" class="main-info-form" @submit.native.prevent>
        <el-row :span="24">
          <el-col :span="8" v-for="field in mainInfoFieldDefs" :key="field.prop">
            <el-form-item :label="field.label" v-if="mainInfo[field.prop] !== undefined">
              <el-input
                :value="formatValue(mainInfo[field.prop])"
                readonly
                size="small"
                @keydown.native="handleKeyDown($event, field)"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-empty v-else-if="!loading" description="暂无基本信息" />

      <el-divider content-position="left">属性列表 ({{ attributes.length }})</el-divider>
      <!-- <SksTable :showRefreshButton="false" :mainTable="mainTable" :highlight-current-row="false" @refresh="handleAttrRefresh">
      </SksTable> -->
      <max-object-detail-column :data="attributes"></max-object-detail-column>
      <el-empty v-if="attributes.length === 0 && !loading" description="暂无属性数据" />

      <el-divider content-position="left">关系列表 ({{ relationships.length }})</el-divider>
      <max-object-detail-rel :data="relationships"></max-object-detail-rel>
      <el-empty v-if="relationships.length === 0 && !loading" description="暂无关系数据" />

      <el-divider content-position="left">索引列表 ({{ indexes.length }})</el-divider>
      <max-object-detail-idx :data="indexes"></max-object-detail-idx>
      <el-empty v-if="indexes.length === 0 && !loading" description="暂无索引数据" />

      <el-divider content-position="left">域信息 ({{ domains.length }})</el-divider>
      <max-object-detail-domain :data="domains"></max-object-detail-domain>
      <el-empty v-if="domains.length === 0 && !loading" description="暂无域数据" />
    </el-card>
  </section>
</template>

<script>
import { getMaxObjectDetail, getMaxObjectDomains } from '@/api/maxobject'
import MaxObjectDetailColumn from './MaxObjectDetailColumn.vue';
import MaxObjectDetailRel from './MaxObjectDetailRel.vue';
import MaxObjectDetailIdx from './MaxObjectDetailIdx.vue';
import MaxObjectDetailDomain from './MaxObjectDetailDomain.vue';

export default {
  name: 'MaxObjectDetail',
  components: {
    'max-object-detail-column': MaxObjectDetailColumn,
    'max-object-detail-rel': MaxObjectDetailRel,
    'max-object-detail-idx': MaxObjectDetailIdx,
    'max-object-detail-domain': MaxObjectDetailDomain
  },
  data() {
    return {
      loading: false,
      objectName: '',
      mainInfo: {},
      attributes: [],
      relationships: [],
      indexes: [],
      domains: [],
    }
  },
  computed: {
    mainInfoFieldDefs() {
      return [
        { prop: 'OBJECTNAME', label: '对象名' },
        { prop: 'DESCRIPTION', label: '英文描述' },
        { prop: 'L_DESCRIPTION', label: '中文描述' },
        { prop: 'CLASSNAME', label: 'Java 类' },
        { prop: 'ENTITYNAME', label: '实体名' },
        { prop: 'SERVICENAME', label: '服务名' },
        { prop: 'EXTENDSOBJECT', label: '扩展对象' },
        { prop: 'SITEORGTYPE', label: '站点/组织类型' },
        { prop: 'RESOURCETYPE', label: '资源类型' },
        { prop: 'TEXTDIRECTION', label: '文本方向' },
        { prop: 'PERSISTENT', label: '持久性' },
        { prop: 'EAUDITENABLED', label: '审计启用' },
        { prop: 'EAUDITFILTER', label: '审计过滤条件' },
        { prop: 'ESIGFILTER', label: '签名过滤条件' },
        { prop: 'ISVIEW', label: '是否视图' },
        { prop: 'IMPORTED', label: '已导入' },
        { prop: 'USERDEFINED', label: '用户定义' },
        { prop: 'MAINOBJECT', label: '主对象' },
        { prop: 'INTERNAL', label: '内部' },
        { prop: 'HASLD', label: '长文本' },
        { prop: 'LANGCODE', label: '语言代码' },
        { prop: 'L_LANGCODE', label: '本地化语言代码' },
        // { prop: 'L_OWNERID', label: '本地化所有者 ID' },
        // { prop: 'L_MAXOBJECTID', label: '本地化 ID' },
        // { prop: 'L_ROWSTAMP', label: '本地化行戳' },
        // { prop: 'MAXOBJECTID', label: '对象 ID' },
        // { prop: 'ROWSTAMP', label: '行戳' },
      ]
    }
  },
  created() {
    this.objectName = this.$route.params.objectname || ''
    if (this.objectName) {
      this.fetchDetail()
    }
  },
  methods: {
    fetchDetail() {
      this.loading = true
      getMaxObjectDetail(this.objectName)
        .then(res => {
          if (res.code === 200 && res.data) {
            const data = res.data
            // 主信息
            this.mainInfo = data.mainInfo || {}
            // 属性
            this.attributes = data.attributes || []
            // 关系
            this.relationships = data.relationships || []
            // 索引
            this.indexes = data.indexes || []
          } else {
            this.$message.error(res.message || '查询详情失败')
          }
        })
        .catch(err => {
          this.$message.error('请求失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.loading = false
        })

      // 查询域信息
      getMaxObjectDomains(this.objectName)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.domains = res.data
          }
        })
        .catch(() => {
          // 域信息非关键，失败不提示
        })
    },
    handleKeyDown(event, field) {
      if (event.altKey && event.key === 'F1') {
        event.preventDefault()
        this.$alert(
          `<div style="font-size:14px;line-height:2">
            <div><b>中文名：</b>${field.label}</div>
            <div><b>属性名：</b><code style="background:#f5f5f5;padding:2px 6px;border-radius:3px">${field.prop}</code></div>
          </div>`,
          '字段属性',
          { dangerouslyUseHTMLString: true, confirmButtonText: '确定' }
        )
      }
    },
    formatValue(val) {
      if (val === null || val === undefined) return ''
      return String(val)
    },
    goBack() {
      this.$router.push({ path: '/maxobject/index' })
    }
  },
  watch: {
    '$route.params.objectname'(val) {
      if (val && val !== this.objectName) {
        this.objectName = val
        this.fetchDetail()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-page {
  padding: 16px;
}
.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.main-info-form {
  margin-top: 8px;
}
.main-info-form .el-form-item {
  margin-bottom: 6px;
}

</style>