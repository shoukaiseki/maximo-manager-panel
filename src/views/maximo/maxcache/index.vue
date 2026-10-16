<template>
  <section class="maxcache-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>Maximo 缓存管理</h2>
          <p class="page-summary">
            列出当前 Maximo 服务器内存中的缓存（缓存名 / 总条数 / 缓存类名）。左侧列表可勾选缓存后执行「重载缓存」「删除缓存」；右侧展示选中缓存的详细信息。
          </p>
        </div>
        <div class="page-actions">
          <el-button type="cyan" icon="el-icon-refresh" size="mini" :loading="loading" @click="fetchList">刷新</el-button>
        </div>
      </div>

      <div ref="splitLayout" class="split-layout">
        <div class="split-left" :style="{ width: leftWidth + 'px' }">
          <div class="panel">
            <div class="toolbar">
              <el-input
                v-model="keyword"
                placeholder="缓存名称过滤"
                prefix-icon="el-icon-search"
                clearable
                size="mini"
                style="width:180px"
              />
              <el-button
                type="warning"
                icon="el-icon-refresh"
                size="mini"
                :disabled="!selectedNames.length"
                :loading="reloading"
                @click="handleReload"
              >重载缓存 ({{ selectedNames.length }})</el-button>
              <el-button
                type="danger"
                icon="el-icon-delete"
                size="mini"
                :disabled="!selectedNames.length"
                :loading="deleting"
                @click="handleDelete"
              >删除缓存 ({{ selectedNames.length }})</el-button>
              <span class="cache-count">
                共 <b>{{ list.length }}</b> 个缓存<template v-if="keyword">（筛选后 {{ filteredList.length }} 个）</template>
              </span>
            </div>

            <el-table
              ref="cacheTable"
              v-loading="loading"
              :data="filteredList"
              border
              stripe
              height="calc(100vh - 300px)"
              style="width:100%"
              @selection-change="onSelectionChange"
              @row-click="onRowClick"
            >
              <el-table-column type="selection" width="45" />
              <el-table-column prop="name" label="缓存名称" min-width="180">
                <template slot-scope="scope">
                  <el-link type="primary" :underline="false" @click.stop="showDetail(scope.row)">{{ scope.row.name }}</el-link>
                </template>
              </el-table-column>
              <el-table-column label="总条数" width="100" align="right">
                <template slot-scope="scope">
                  <el-tooltip content="该缓存内存中的数据总条目数（估算，未知时显示 -）" placement="top">
                    <span>{{ formatCount(scope.row.count) }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="缓存类名" min-width="220" show-overflow-tooltip>
                <template slot-scope="scope">
                  <span class="mono">{{ scope.row.className || '-' }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <div class="split-bar" :class="{ dragging: dragging }" title="拖动调整左右宽度" @mousedown="startDrag">
          <span class="split-bar-handle" />
        </div>

        <div class="split-right">
          <div class="panel detail-panel">
            <div class="detail-header">
              <h3>{{ detail ? detail.name : '缓存详情' }}</h3>
              <el-tag v-if="detail" :type="detail.mtEnabled ? 'success' : 'info'" size="mini">
                {{ detail.mtEnabled ? '租户级缓存' : '普通缓存' }}
              </el-tag>
            </div>

            <el-empty v-if="!detail" description="请从左侧选择缓存查看详情" />

            <template v-if="detail">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="缓存名称">{{ detail.name }}</el-descriptions-item>
                <el-descriptions-item label="总条数">{{ formatCount(detail.count) }}</el-descriptions-item>
                <el-descriptions-item label="缓存类名">
                  <span class="mono">{{ detail.className || '-' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="支持卸载">
                  {{ detail.mtEnabled ? '是（租户级缓存）' : '否（仅支持重载）' }}
                </el-descriptions-item>
              </el-descriptions>

              <div class="detail-actions">
                <el-button
                  type="warning"
                  icon="el-icon-refresh"
                  size="mini"
                  :loading="reloading"
                  @click="handleReloadRow(detail.name)"
                >重载该缓存</el-button>
                <el-button
                  type="danger"
                  icon="el-icon-delete"
                  size="mini"
                  :disabled="!detail.mtEnabled"
                  :loading="deleting"
                  @click="handleDeleteRow(detail.name)"
                >删除该缓存</el-button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </el-card>
  </section>
</template>

<script>
import { listCaches, reloadCaches, deleteCaches } from '@/api/maxcache'

export default {
  name: 'MaxCache',
  data() {
    return {
      loading: false,
      reloading: false,
      deleting: false,
      list: [],
      selected: [],
      detail: null,
      keyword: '',
      leftWidth: 480,
      dragging: false,
      dragStartX: 0,
      dragStartWidth: 0
    }
  },
  computed: {
    filteredList() {
      const kw = (this.keyword || '').trim().toUpperCase()
      if (!kw) return this.list
      return this.list.filter(item => (item.name || '').toUpperCase().indexOf(kw) > -1)
    },
    selectedNames() {
      return this.selected.map(row => row.name)
    }
  },
  created() {
    this.fetchList()
  },
  mounted() {
    this.initLeftWidth()
    window.addEventListener('resize', this.initLeftWidth)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.initLeftWidth)
    this.stopDrag()
  },
  methods: {
    // 初始左栏宽度：优先取本地保存值，否则按容器宽度 40%
    initLeftWidth() {
      if (this.dragging) return
      const saved = parseInt(window.localStorage.getItem('maxcache-left-width'), 10)
      if (!isNaN(saved)) {
        this.leftWidth = this.clampWidth(saved)
        return
      }
      const layout = this.$refs.splitLayout
      const total = layout ? layout.clientWidth : 0
      if (total > 0) {
        this.leftWidth = this.clampWidth(Math.round(total * 0.4))
      }
    },
    // 限制左栏宽度在 [220, 容器宽-280] 之间
    clampWidth(width) {
      const layout = this.$refs.splitLayout
      const total = layout ? layout.clientWidth : 0
      const min = 220
      const max = total > 0 ? Math.max(min, total - 280) : width
      if (width < min) return min
      if (width > max) return max
      return width
    },
    startDrag(e) {
      this.dragging = true
      this.dragStartX = e.clientX
      this.dragStartWidth = this.leftWidth
      document.addEventListener('mousemove', this.onDrag)
      document.addEventListener('mouseup', this.stopDrag)
      document.body.style.userSelect = 'none'
      document.body.style.cursor = 'col-resize'
    },
    onDrag(e) {
      if (!this.dragging) return
      this.leftWidth = this.clampWidth(this.dragStartWidth + (e.clientX - this.dragStartX))
    },
    stopDrag() {
      if (this.dragging) {
        this.dragging = false
        window.localStorage.setItem('maxcache-left-width', String(this.leftWidth))
      }
      document.removeEventListener('mousemove', this.onDrag)
      document.removeEventListener('mouseup', this.stopDrag)
      document.body.style.userSelect = ''
      document.body.style.cursor = ''
    },
    // 从响应体中解析出脚本返回的业务数据
    parseBody(res) {
      const body = res && res.data ? res.data : res
      if (!body) {
        throw new Error('接口无返回内容')
      }
      if (body.status && body.status !== 'success') {
        // 界面只展示简短 message；完整堆栈仅在浏览器开发者控制台查看
        console.error('[SKS_CACHE_MANAGE] 脚本执行失败:', body.message || '未知错误', body.errorStackTrace || '')
        throw new Error(body.message || '脚本执行失败')
      }
      return body.data || {}
    },
    formatCount(c) {
      return c === null || c === undefined || c < 0 ? '-' : c
    },
    async fetchList() {
      this.loading = true
      try {
        const data = this.parseBody(await listCaches())
        this.list = data.list || []
        this.selected = []
        if (this.$refs.cacheTable) {
          this.$refs.cacheTable.clearSelection()
        }
        this.detail = this.list.length ? this.list[0] : null
      } catch (err) {
        this.msgError('查询缓存列表失败: ' + (err.message || err))
        this.list = []
        this.detail = null
      } finally {
        this.loading = false
      }
    },
    // 右侧详情直接复用左侧列表行数据（list 接口已返回 总条数/缓存类名）
    showDetail(row) {
      if (!row || !row.name) return
      this.detail = row
    },
    onSelectionChange(selection) {
      this.selected = selection
    },
    onRowClick(row) {
      this.showDetail(row)
    },
    async handleReload() {
      const names = this.selectedNames
      if (!names.length) return
      try {
        await this.$confirm(`确定重载选中的 ${names.length} 个缓存吗？重载会同步通知所有服务器更新缓存。`, '重载缓存', {
          type: 'warning'
        })
      } catch (e) {
        return
      }
      await this.doReload(names)
    },
    async handleReloadRow(name) {
      try {
        await this.$confirm(`确定重载缓存 [${name}] 吗？`, '重载缓存', { type: 'warning' })
      } catch (e) {
        return
      }
      await this.doReload([name])
    },
    async doReload(names) {
      this.reloading = true
      try {
        const data = this.parseBody(await reloadCaches(names))
        const failed = (data.results || []).filter(r => r.status !== 'success')
        if (failed.length) {
          this.msgError(`重载完成：成功 ${data.successCount} 个，失败 ${data.errorCount} 个<br>` + failed.map(r => `${r.name}: ${r.message}`).join('<br>'), true)
        } else {
          this.msgSuccess(`重载成功：${data.successCount} 个缓存`)
        }
        await this.fetchList()
      } catch (err) {
        this.msgError('重载缓存失败: ' + (err.message || err))
      } finally {
        this.reloading = false
      }
    },
    async handleDelete() {
      const names = this.selectedNames
      if (!names.length) return
      try {
        await this.$confirm(
          `确定删除选中的 ${names.length} 个缓存吗？注意：MXServer 仅支持「租户级缓存」卸载，普通缓存会被跳过（仅可重载）。`,
          '删除缓存',
          { type: 'warning' }
        )
      } catch (e) {
        return
      }
      await this.doDelete(names)
    },
    async handleDeleteRow(name) {
      try {
        await this.$confirm(`确定删除缓存 [${name}] 吗？`, '删除缓存', { type: 'warning' })
      } catch (e) {
        return
      }
      await this.doDelete([name])
    },
    async doDelete(names) {
      this.deleting = true
      try {
        const data = this.parseBody(await deleteCaches(names))
        const errors = (data.results || []).filter(r => r.status === 'error')
        const skipped = (data.results || []).filter(r => r.status === 'skipped')
        if (errors.length) {
          this.msgError(`删除完成：成功 ${data.successCount} 个，失败 ${data.errorCount} 个<br>` + errors.map(r => `${r.name}: ${r.message}`).join('<br>'), true)
        } else if (skipped.length) {
          this.msgSuccess(`已卸载 ${data.successCount} 个缓存；跳过 ${data.skipCount} 个非租户级缓存`)
        } else {
          this.msgSuccess(`删除成功：${data.successCount} 个缓存`)
        }
        await this.fetchList()
      } catch (err) {
        this.msgError('删除缓存失败: ' + (err.message || err))
      } finally {
        this.deleting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.maxcache-page {
  padding: 16px;
}
.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.page-summary {
  color: #606266;
  margin: 0;
  max-width: 900px;
  line-height: 1.5;
}
.panel {
  min-height: 200px;
}
.split-layout {
  display: flex;
  align-items: stretch;
}
.split-left {
  flex: 0 0 auto;
  min-width: 0;
}
.split-right {
  flex: 1 1 auto;
  min-width: 0;
}
.split-bar {
  flex: 0 0 auto;
  width: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: col-resize;
  user-select: none;
}
.split-bar-handle {
  width: 2px;
  height: 100%;
  min-height: 200px;
  background: #e4e7ed;
  border-radius: 1px;
  transition: background 0.2s;
}
.split-bar:hover .split-bar-handle,
.split-bar.dragging .split-bar-handle {
  background: #409eff;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.cache-count {
  margin-left: auto;
  color: #606266;
  font-size: 12px;
  white-space: nowrap;
}
.cache-count b {
  color: #409eff;
}
.detail-panel {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.detail-header h3 {
  margin: 0;
  font-size: 15px;
}
.mono {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  word-break: break-all;
}
.detail-actions {
  margin-top: 16px;
}
</style>
