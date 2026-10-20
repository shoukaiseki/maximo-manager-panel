<template>
  <section class="pubfile-page">
    <el-card>
      <div class="page-header-row">
        <div>
          <h2>公开文件管理</h2>
          <p class="page-summary">
            上传图片/文件后由后端生成可公开访问的 URL（用于主页图片链接）。列表查询、下载/预览无需认证，上传需 X-API-Key 认证。
          </p>
        </div>
        <div class="page-actions">
          <el-upload
            ref="upload"
            class="inline-upload"
            action=""
            :multiple="true"
            :show-file-list="false"
            :http-request="httpUpload">
            <el-button type="primary" icon="el-icon-upload2" size="mini" :loading="uploading">
              {{ uploading ? '上传中...' : '上传文件' }}
            </el-button>
          </el-upload>
          <el-button icon="el-icon-refresh" size="mini" :loading="loading" @click="fetchList">刷新</el-button>
        </div>
      </div>

      <el-form :inline="true" @submit.native.prevent>
        <el-form-item label="文件名">
          <el-input v-model="keyword" placeholder="原始文件名 / 存储文件名 模糊过滤" clearable style="width:260px" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="kind" placeholder="全部" clearable style="width:130px">
            <el-option label="图片" value="image" />
            <el-option label="其它" value="other" />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <span class="pubfile-count">
          共 <b>{{ filteredList.length }}</b> 个文件<template v-if="keyword || kind">（总 {{ list.length }} 个）</template>
        </span>
      </div>

      <el-table :data="pagedList" border style="width:100%" v-loading="loading">
        <el-table-column label="预览" width="90" align="center">
          <template slot-scope="scope">
            <el-image
              v-if="isImage(scope.row)"
              :src="previewUrlOf(scope.row)"
              :preview-src-list="[previewUrlOf(scope.row)]"
              fit="contain"
              class="thumb">
              <div slot="error" class="thumb-fallback">图</div>
            </el-image>
            <span v-else class="thumb-fallback">{{ extOf(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="原始文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="storedName" label="存储文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="大小" width="100" align="right">
          <template slot-scope="scope">{{ formatSize(scope.row.size) }}</template>
        </el-table-column>
        <el-table-column prop="contentType" label="类型" width="150" show-overflow-tooltip />
        <el-table-column label="上传时间" width="160">
          <template slot-scope="scope">{{ formatTime(scope.row.uploadTime) }}</template>
        </el-table-column>
        <el-table-column label="公开链接（图片链接直接用）" min-width="260">
          <template slot-scope="scope">
            <el-link type="primary" :underline="false" :href="urlOf(scope.row)" target="_blank" title="点击预览">
              {{ urlOf(scope.row) }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleCopy(scope.row, $event)">复制链接</el-button>
            <el-button type="text" size="mini" @click="handleOpen(scope.row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="filteredList.length > 0"
        :total="filteredList.length"
        :page.sync="pageNum"
        :limit.sync="pageSize" />
      <el-empty v-if="!loading && filteredList.length === 0" description="暂无文件" />
    </el-card>
  </section>
</template>

<script>
import { listPublicFiles, uploadPublicFile, buildPublicFileUrl } from '@/api/pubfile'
import handleClipboard from '@/utils/clipboard'

const IMAGE_EXT = ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp', 'svg', 'ico']

export default {
  name: 'PublicFile',
  data() {
    return {
      loading: false,
      uploading: false,
      list: [],
      keyword: '',
      kind: '',
      pageNum: 1,
      pageSize: 10
    }
  },
  computed: {
    filteredList() {
      const kw = (this.keyword || '').trim().toLowerCase()
      return (this.list || []).filter(row => {
        if (this.kind === 'image' && !this.isImage(row)) return false
        if (this.kind === 'other' && this.isImage(row)) return false
        if (!kw) return true
        return String(row.originalName || '').toLowerCase().indexOf(kw) >= 0 ||
          String(row.storedName || '').toLowerCase().indexOf(kw) >= 0
      })
    },
    pagedList() {
      const start = (this.pageNum - 1) * this.pageSize
      return this.filteredList.slice(start, start + this.pageSize)
    }
  },
  watch: {
    keyword() { this.pageNum = 1 },
    kind() { this.pageNum = 1 }
  },
  created() {
    this.fetchList()
  },
  methods: {
    // solonapi 响应拦截器返回的已是响应体 { code, message, data }，此处兼容拿到 axios 原始响应的情况
    pickBody(res) {
      const body = (res && res.headers && res.config) ? res.data : res
      if (!body) {
        throw new Error('接口无返回内容')
      }
      if (typeof body.code === 'number' && body.code !== 200) {
        throw new Error(body.message || ('接口返回 code=' + body.code))
      }
      if (body.success === false) {
        throw new Error(body.message || '接口返回失败')
      }
      return body
    },
    async fetchList() {
      this.loading = true
      try {
        const body = this.pickBody(await listPublicFiles())
        this.list = Array.isArray(body) ? body : (Array.isArray(body.data) ? body.data : [])
        if ((this.pageNum - 1) * this.pageSize >= this.filteredList.length) {
          this.pageNum = 1
        }
      } catch (err) {
        console.error('[公开文件管理] 查询列表失败:', err)
        this.$message.error('查询列表失败: ' + (err.message || err))
        this.list = []
      } finally {
        this.loading = false
      }
    },
    // el-upload 自定义上传：走 axios 封装，认证头 X-API-Key 由 solonapi 拦截器自动附带
    httpUpload(option) {
      const formData = new FormData()
      formData.append('file', option.file)
      this.uploading = true
      return uploadPublicFile(formData).then(res => {
        this.pickBody(res)
        this.$message.success('上传成功: ' + option.file.name)
        this.fetchList()
        return res
      }).catch(err => {
        console.error('[公开文件管理] 上传失败:', option.file && option.file.name, err)
        this.$message.error('上传失败 ' + (option.file ? option.file.name : '') + ': ' + (err.message || err))
        throw err
      }).finally(() => {
        this.uploading = false
      })
    },
    extOf(row) {
      const ext = String((row && row.extension) || '').toLowerCase().replace('.', '')
      return (ext || 'FILE').toUpperCase()
    },
    isImage(row) {
      const ext = String((row && row.extension) || '').toLowerCase().replace('.', '')
      if (ext) {
        return IMAGE_EXT.indexOf(ext) >= 0
      }
      return String((row && row.contentType) || '').toLowerCase().indexOf('image/') === 0
    },
    urlOf(row) {
      return buildPublicFileUrl(row)
    },
    // 预览地址：显式带上 _action=preview（后端该值与 default 分支同一逻辑）
    previewUrlOf(row) {
      const url = this.urlOf(row)
      if (!url) return ''
      return url + (url.indexOf('?') >= 0 ? '&' : '?') + '_action=preview'
    },
    // 复制用的绝对地址：后端返回绝对 URL 时原样使用，相对路径则补上当前协议+域名+端口
    absoluteUrlOf(row) {
      const url = this.urlOf(row)
      if (!url) return ''
      return /^https?:\/\//i.test(url) ? url : window.location.origin + url
    },
    formatSize(size) {
      const n = Number(size)
      if (!n) return '-'
      if (n < 1024) return n + ' B'
      if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
      if (n < 1024 * 1024 * 1024) return (n / 1024 / 1024).toFixed(2) + ' MB'
      return (n / 1024 / 1024 / 1024).toFixed(2) + ' GB'
    },
    formatTime(time) {
      if (!time) return '-'
      const date = new Date(Number(time))
      if (isNaN(date.getTime())) return String(time)
      const pad = v => (v < 10 ? '0' + v : '' + v)
      return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) +
        ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds())
    },
    handleCopy(row, event) {
      const url = this.absoluteUrlOf(row)
      if (!url) {
        this.$message.warning('该文件没有可用的公开链接')
        return
      }
      handleClipboard(url, event)
    },
    handleOpen(row) {
      const url = this.urlOf(row)
      if (!url) {
        this.$message.warning('该文件没有可用的公开链接')
        return
      }
      // 下载带上 _action=download，后端返回 Content-Disposition: attachment
      window.open(url + (url.indexOf('?') >= 0 ? '&' : '?') + '_action=download', '_blank')
    }
  }
}
</script>

<style lang="scss" scoped>
.pubfile-page {
  .page-header-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 12px;

    h2 {
      margin: 0 0 6px;
      font-size: 18px;
    }

    .page-summary {
      margin: 0;
      color: #909399;
      font-size: 12px;
      line-height: 1.6;
      max-width: 900px;
    }

    .page-actions {
      white-space: nowrap;

      .inline-upload {
        display: inline-block;
        margin-right: 8px;
      }
    }
  }

  .toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 8px;
  }

  .pubfile-count {
    margin-left: auto;
    color: #606266;
    font-size: 12px;
    white-space: nowrap;

    b {
      color: #409eff;
    }
  }

  .thumb {
    width: 56px;
    height: 56px;
    border: 1px solid #ebeef5;
    border-radius: 4px;
  }

  .thumb-fallback {
    display: inline-block;
    padding: 4px 6px;
    color: #909399;
    font-size: 12px;
    border: 1px solid #ebeef5;
    border-radius: 4px;
  }
}
</style>
