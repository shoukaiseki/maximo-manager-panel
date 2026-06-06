<template>
  <section class="query-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>MaxObject 查询</h2>
          <p class="page-summary">支持 objectname 或 description 模糊搜索，查询结果点击跳转详情。</p>
        </div>
      </div>

      <el-form :model="formData" ref="queryForm" :inline="true" label-width="90px" @submit.native.prevent>
        <el-form-item label="搜索关键词" >
          <el-input v-model="formData.keyword" placeholder="输入对象名或描述..." clearable style="width: 300px;" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="cyan" icon="el-icon-search" size="mini" :loading="loading" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="result-panel">
        <el-table :data="objectList" stripe style="width: 100%" class="result-table" @row-click="handleRowClick">
          <el-table-column prop="objectName" label="对象名" width="200">
            <template slot-scope="scope">
              <el-link type="primary" :underline="false">{{ scope.row.objectName }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="英文描述" />
          <el-table-column prop="descriptionCn" label="中文描述" />
          <el-table-column label="操作" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click.stop="goDetail(scope.row.objectName)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <SksPagination
          v-if="total > 0"
          :total="total"
          :page.sync="pageNum"
          :limit.sync="pageSize"
          @pagination="handlePageChange"
        />
        <el-empty v-if="!loading && objectList.length === 0 && hasSearched" description="暂无查询结果" />
        <el-empty v-if="!loading && objectList.length === 0 && !hasSearched" description="请输入关键词后点击搜索" />
      </div>
    </el-card>
  </section>
</template>

<script>
import { getMaxObjectList } from '@/api/maxobject'

export default {
  name: 'MaxObjectList',
  data() {
    return {
      loading: false,
      hasSearched: false,
      objectList: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      formData: {
        keyword: ''
      }
    }
  },
  methods: {
    handleQuery() {
      this.hasSearched = true
      this.loading = true
      this.pageNum = 1
      getMaxObjectList(this.formData.keyword, this.pageNum, this.pageSize)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.objectList = res.data.rows || []
            this.total = res.data.total || 0
          } else {
            this.objectList = []
            this.total = 0
            this.$message.error(res.message || '查询失败')
          }
        })
        .catch(err => {
          this.$message.error('请求失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.loading = false
        })
    },
    handlePageChange({ page, limit }) {
      this.pageNum = page
      this.pageSize = limit
      this.loading = true
      getMaxObjectList(this.formData.keyword, page, limit)
        .then(res => {
          if (res.code === 200 && res.data) {
            this.objectList = res.data.rows || []
            this.total = res.data.total || 0
          } else {
            this.objectList = []
            this.total = 0
          }
        })
        .catch(err => {
          this.$message.error('请求失败: ' + (err.message || String(err)))
        })
        .finally(() => {
          this.loading = false
        })
    },
    resetForm() {
      this.formData.keyword = ''
      this.hasSearched = false
      this.objectList = []
      this.pageNum = 1
      this.pageSize = 20
      this.total = 0
    },
    handleRowClick(row) {
      this.goDetail(row.objectName)
    },
    goDetail(objectName) {
      this.$router.push({
        path: `/maxobject-detail/index/${objectName}`
      })
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
  margin-top: 20px;
}
.result-table {
  margin-top: 16px;
}
</style>