<template>
  <div class="api-caller-container">
    <div class="api-caller-header">
      <el-select v-model="selectedProjectId" placeholder="选择项目" class="project-select" size="small" @change="loadProject">
        <el-option label="无" value="" />
        <el-option v-for="p in projects" :key="p.id" :label="p.name + (p.type === 'global' ? ' (全局)' : '')" :value="p.id" />
      </el-select>
      <el-button type="text" icon="el-icon-folder-opened" @click="showProjectList = true">项目列表</el-button>
      <el-select v-model="requestMethod" class="method-select" size="small">
        <el-option label="GET" value="GET" />
        <el-option label="POST" value="POST" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
      </el-select>
      <el-input v-model="urlPath" placeholder="/api/os/MXAPIMESSAGE" class="url-input" />
      <el-button type="primary" @click="sendRequest" :loading="loading" class="send-btn">发送</el-button>
      <el-button @click="clearResult" class="clear-btn">清空</el-button>
    </div>

    <div class="api-caller-body">
      <div v-if="projectRequests.length > 0" class="request-sidebar">
        <div class="sidebar-header">
          <span>接口列表</span>
          <el-input v-model="requestFilter" placeholder="搜索" size="small" class="sidebar-filter" />
        </div>
        <div class="sidebar-tree">
          <div v-for="folder in folders" :key="folder.id" class="folder-item">
            <div class="folder-title" @click="toggleFolder(folder.id)">
              <i class="el-icon-folder" :class="{ 'el-icon-folder-opened': expandedFolders.includes(folder.id) }" />
              {{ folder.name }}
            </div>
            <div v-if="expandedFolders.includes(folder.id)" class="folder-content">
              <div v-for="req in getRequestsByFolder(folder.id)" :key="req.id" 
                   class="request-item" :class="{ active: currentRequestId === req.id }"
                   @click="loadRequest(req)">
                <span class="method-badge" :class="req.method.toLowerCase()">{{ req.method }}</span>
                {{ req.name }}
              </div>
            </div>
          </div>
          <div v-for="req in getRequestsWithoutFolder" :key="req.id" 
               class="request-item" :class="{ active: currentRequestId === req.id }"
               @click="loadRequest(req)">
            <span class="method-badge" :class="req.method.toLowerCase()">{{ req.method }}</span>
            {{ req.name }}
          </div>
        </div>
      </div>

      <div class="main-content">
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="参数" name="params">
            <div class="params-header">
              <el-button type="text" icon="el-icon-plus" @click="addParam">添加参数</el-button>
              <el-button type="text" icon="el-icon-delete" @click="clearParams">清空</el-button>
            </div>
            <el-table :data="params" border size="small" :show-header="params.length > 0">
              <el-table-column type="index" width="40" />
              <el-table-column prop="key" label="参数名" width="150">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.key" size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="value" label="参数值">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.value" size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="enabled" label="启用" width="80">
                <template slot-scope="scope">
                  <el-checkbox v-model="scope.row.enabled" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60">
                <template slot-scope="scope">
                  <el-button type="text" icon="el-icon-delete" @click="removeParam(scope.$index)" />
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="Headers" name="headers">
            <div class="params-header">
              <el-button type="text" icon="el-icon-plus" @click="addHeader">添加Header</el-button>
              <el-button type="text" icon="el-icon-delete" @click="clearHeaders">清空</el-button>
            </div>
            <el-table :data="headers" border size="small" :show-header="headers.length > 0">
              <el-table-column type="index" width="40" />
              <el-table-column prop="key" label="Header名" width="150">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.key" size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="value" label="Header值">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.value" size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="enabled" label="启用" width="80">
                <template slot-scope="scope">
                  <el-checkbox v-model="scope.row.enabled" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60">
                <template slot-scope="scope">
                  <el-button type="text" icon="el-icon-delete" @click="removeHeader(scope.$index)" />
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="Body" name="body">
            <el-select v-model="bodyType" class="body-type-select" size="small">
              <el-option label="None" value="none" />
              <el-option label="form-data" value="form-data" />
              <el-option label="x-www-form-urlencoded" value="urlencoded" />
              <el-option label="raw JSON" value="json" />
            </el-select>

            <div v-if="bodyType === 'form-data' || bodyType === 'urlencoded'" class="body-form">
              <div class="params-header">
                <el-button type="text" icon="el-icon-plus" @click="addBodyParam">添加参数</el-button>
                <el-button type="text" icon="el-icon-delete" @click="clearBodyParams">清空</el-button>
              </div>
              <el-table :data="bodyParams" border size="small" :show-header="bodyParams.length > 0">
                <el-table-column type="index" width="40" />
                <el-table-column prop="key" label="参数名" width="150">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.key" size="small" />
                  </template>
                </el-table-column>
                <el-table-column prop="value" label="参数值">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.value" size="small" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="60">
                  <template slot-scope="scope">
                    <el-button type="text" icon="el-icon-delete" @click="removeBodyParam(scope.$index)" />
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div v-if="bodyType === 'json'" class="body-json">
              <textarea v-model="bodyJson" class="json-textarea" placeholder="输入JSON格式的请求体..." />
            </div>

            <div v-if="bodyType === 'none'" class="body-none">
              <el-empty description="无请求体" />
            </div>
          </el-tab-pane>
        </el-tabs>

        <div class="result-section">
          <div class="result-header">
            <span class="result-label">响应结果</span>
            <span v-if="responseTime" class="response-time">{{ responseTime }}ms</span>
            <span v-if="responseStatus" :class="['status-code', responseStatus >= 200 && responseStatus < 300 ? 'success' : 'error']">
              {{ responseStatus }}
            </span>
            <el-button type="text" icon="el-icon-copy-document" @click="copyResult" v-if="responseText">复制</el-button>
          </div>
          <div v-if="responseText" class="result-body">
            <pre class="result-pre">{{ formattedResponse }}</pre>
          </div>
          <div v-else class="result-empty">
            <el-empty description="点击发送按钮执行请求" />
          </div>
        </div>
      </div>
    </div>

    <el-dialog title="项目列表" :visible.sync="showProjectList" width="800px" top="10vh">
      <div class="project-list-header">
        <el-input v-model="projectFilter" placeholder="搜索项目" size="small" class="project-filter-input" />
        <el-button type="primary" icon="el-icon-plus" @click="showCreateDialog = true">新建</el-button>
        <el-button type="primary" icon="el-icon-upload" @click="showImportDialog = true">导入</el-button>
      </div>
      <el-table :data="filteredProjects" border size="small">
        <el-table-column prop="name" label="项目名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="requestCount" label="接口数量">
          <template slot-scope="scope">
            <span>{{ scope.row.requestCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型">
          <template slot-scope="scope">
            <span :class="scope.row.type === 'global' ? 'global-tag' : 'user-tag'">
              {{ scope.row.type === 'global' ? '全局' : '个人' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template slot-scope="scope">
            <el-button type="text" @click="selectProject(scope.row.id)">选择</el-button>
            <el-button type="text" @click="editProject(scope.row)">编辑</el-button>
            <el-button type="text" @click="copyProject(scope.row)">复制</el-button>
            <el-button type="text" @click="exportProject(scope.row)">导出</el-button>
            <el-button v-if="scope.row.type !== 'global'" type="text" @click="deleteProjectConfirm(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog title="编辑项目" :visible.sync="showEditDialog" width="400px">
      <el-form>
        <el-form-item label="项目名称">
          <el-input v-model="editProjectName" placeholder="输入项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editProjectDesc" placeholder="输入项目描述" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="doEdit">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="新建项目" :visible.sync="showCreateDialog" width="400px">
      <el-form>
        <el-form-item label="项目名称">
          <el-input v-model="createProjectName" placeholder="输入项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createProjectDesc" placeholder="输入项目描述" />
        </el-form-item>
        <el-form-item label="创建到">
          <el-select v-model="createToGlobal" placeholder="请选择">
            <el-option label="个人项目" value="create_personal" />
            <el-option label="全局项目" value="create_global" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="doCreate">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="导入项目" :visible.sync="showImportDialog" width="600px">
      <div class="import-container">
        <el-form label-width="90px">
          <el-form-item label="项目名称">
            <el-input v-model="importProjectName" placeholder="输入项目名称" />
          </el-form-item>
        </el-form>
        <div class="import-option">
          <span>方式一：选择文件</span>
          <el-upload
            class="upload-demo"
            action=""
            :auto-upload="false"
            :on-change="handleFileChange"
            :show-file-list="false"
            accept=".json"
          >
            <el-button size="small" type="primary">选择APIPost JSON文件</el-button>
          </el-upload>
          <span v-if="importFileName" class="file-name">{{ importFileName }}</span>
        </div>
        <div class="import-option">
          <span>方式二：粘贴JSON</span>
          <el-textarea v-model="importJson" placeholder="粘贴APIPOST导出的JSON..." rows="8" class="import-textarea" />
        </div>
        <el-select v-model="importToGlobal" placeholder="请选择" style="width:200px">
          <el-option label="导入到个人项目" value="personal" />
          <el-option label="导入到全局项目" value="global" />
        </el-select>
      </div>
      <div slot="footer">
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="doImport">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="复制项目" :visible.sync="showCopyDialog" width="400px">
      <el-form>
        <el-form-item label="新项目名称">
          <el-input v-model="copyProjectName" placeholder="输入新项目名称" />
        </el-form-item>
        <el-form-item label="复制到">
          <el-select v-model="copyToGlobal" placeholder="请选择">
            <el-option label="个人项目" value="personal" />
            <el-option label="全局项目" value="global" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showCopyDialog = false">取消</el-button>
        <el-button type="primary" @click="doCopy">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'ApiCaller',
  data() {
    return {
      selectedProjectId: '',
      projects: [],
      projectRequests: [],
      folders: [],
      expandedFolders: [],
      requestFilter: '',
      currentRequestId: '',
      showProjectList: false,
      showImportDialog: false,
      showCopyDialog: false,
      showCreateDialog: false,
      showEditDialog: false,
      editProjectId: '',
      editProjectName: '',
      editProjectDesc: '',
      importJson: '',
      importFileName: '',
      importProjectName: '',
      copyProjectName: '',
      createProjectName: '',
      createProjectDesc: '',
      sourceProjectId: '',
      projectFilter: '',
      importToGlobal: 'personal',
      copyToGlobal: 'personal',
      createToGlobal: 'create_personal',
      requestMethod: 'GET',
      urlPath: '/api/os/MXAPIMESSAGE',
      activeTab: 'params',
      params: [],
      headers: [],
      bodyType: 'none',
      bodyParams: [],
      bodyJson: '',
      loading: false,
      responseText: '',
      responseStatus: null,
      responseTime: null
    }
  },
  computed: {
    formattedResponse() {
      try {
        const obj = JSON.parse(this.responseText)
        return JSON.stringify(obj, null, 2)
      } catch (e) {
        return this.responseText
      }
    },
    filteredProjects() {
      if (!this.projectFilter) return this.projects
      return this.projects.filter(p => 
        p.name.toLowerCase().includes(this.projectFilter.toLowerCase()) ||
        (p.description && p.description.toLowerCase().includes(this.projectFilter.toLowerCase()))
      )
    },
    getRequestsWithoutFolder() {
      return this.projectRequests.filter(r => !r.folderId && 
        r.name.toLowerCase().includes(this.requestFilter.toLowerCase()))
    }
  },
  mounted() {
    this.loadProjects()
  },
  methods: {
    getAuthHeaders() {
      const headers = {}
      const saved = localStorage.getItem('maximo-env-settings')
      if (saved) {
        try {
          const settings = JSON.parse(saved)
          if (settings.useApiKey && settings.apiKey) {
            headers['X-API-Key'] = settings.apiKey
          } else if (settings.maxauth) {
            headers['maxauth'] = settings.maxauth
          }
        } catch (e) {}
      }
      return headers
    },
    async loadProjects() {
      try {
        const response = await axios.get('/solonapi/apiproject/list', { 
          params: { user: 'default' },
          headers: this.getAuthHeaders()
        })
        if (response.data.code === 200) {
          this.projects = response.data.data
        }
      } catch (e) {
        console.error('加载项目列表失败', e)
      }
    },
    async loadProject(projectId) {
      if (!projectId) {
        this.projectRequests = []
        this.folders = []
        return
      }

      try {
        const response = await axios.get('/solonapi/apiproject/get', { 
          params: { user: 'default', projectId },
          headers: this.getAuthHeaders()
        })
        if (response.data.code === 200) {
          this.projectRequests = response.data.data.requests || []
          this.folders = response.data.data.folders || []
          this.expandedFolders = this.folders.map(f => f.id)
        }
      } catch (e) {
        console.error('加载项目失败', e)
      }
    },
    toggleFolder(folderId) {
      const index = this.expandedFolders.indexOf(folderId)
      if (index > -1) {
        this.expandedFolders.splice(index, 1)
      } else {
        this.expandedFolders.push(folderId)
      }
    },
    getRequestsByFolder(folderId) {
      return this.projectRequests.filter(r => r.folderId === folderId &&
        r.name.toLowerCase().includes(this.requestFilter.toLowerCase()))
    },
    loadRequest(request) {
      this.currentRequestId = request.id
      this.requestMethod = request.method || 'GET'
      this.urlPath = request.url || ''
      this.params = []
      this.headers = []
      this.bodyType = 'none'
      this.bodyParams = []
      this.bodyJson = ''

      if (request.params) {
        Object.keys(request.params).forEach(key => {
          this.params.push({ key, value: request.params[key], enabled: true })
        })
      }

      if (request.headers) {
        Object.keys(request.headers).forEach(key => {
          this.headers.push({ key, value: request.headers[key], enabled: true })
        })
      }

      if (request.body) {
        if (request.body.type === 'json' || request.body.json) {
          this.bodyType = 'json'
          this.bodyJson = typeof request.body.json === 'string' ? request.body.json : JSON.stringify(request.body.json, null, 2)
        } else if (request.body.formData) {
          this.bodyType = 'form-data'
          request.body.formData.forEach(item => {
            this.bodyParams.push({ key: item.key, value: item.value })
          })
        } else if (request.body.urlEncoded) {
          this.bodyType = 'urlencoded'
          request.body.urlEncoded.forEach(item => {
            this.bodyParams.push({ key: item.key, value: item.value })
          })
        }
      }
    },
    selectProject(projectId) {
      this.selectedProjectId = projectId
      this.showProjectList = false
      this.loadProject(projectId)
    },
    editProject(project) {
      this.editProjectId = project.id
      this.editProjectName = project.name || ''
      this.editProjectDesc = project.description || ''
      this.showEditDialog = true
    },
    async doEdit() {
      if (!this.editProjectName.trim()) {
        this.$message.warning('请输入项目名称')
        return
      }

      try {
        const response = await axios.post('/solonapi/apiproject/update', null, {
          params: { 
            user: 'default', 
            projectId: this.editProjectId, 
            name: this.editProjectName,
            description: this.editProjectDesc
          },
          headers: this.getAuthHeaders()
        })
        if (response.data.code === 200) {
          this.$message.success('更新成功')
          this.showEditDialog = false
          this.loadProjects()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (e) {
        this.$message.error('更新失败')
      }
    },
    copyProject(project) {
      this.sourceProjectId = project.id
      this.copyProjectName = project.name + '_副本'
      this.showCopyDialog = true
    },
    async doCopy() {
      if (!this.copyProjectName.trim()) {
        this.$message.warning('请输入项目名称')
        return
      }

      try {
        const response = await axios.post('/solonapi/apiproject/copy', null, {
          params: { user: 'default', sourceProjectId: this.sourceProjectId, newName: this.copyProjectName, toGlobal: this.copyToGlobal === 'global' },
          headers: this.getAuthHeaders()
        })
        if (response.data.code === 200) {
          this.$message.success('复制成功')
          this.showCopyDialog = false
          this.copyToGlobal = 'personal'
          this.loadProjects()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (e) {
        this.$message.error('复制失败')
      }
    },
    async exportProject(project) {
      try {
        const response = await axios.get('/solonapi/apiproject/export', { params: { user: 'default', projectId: project.id } })
        if (response.data.code === 200) {
          const blob = new Blob([response.data.data], { type: 'application/json' })
          const url = URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = project.name + '.json'
          a.click()
          URL.revokeObjectURL(url)
        } else {
          this.$message.error(response.data.message)
        }
      } catch (e) {
        this.$message.error('导出失败')
      }
    },
    deleteProjectConfirm(project) {
      this.$confirm('确定删除该项目吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await axios.post('/solonapi/apiproject/delete', null, {
            params: { user: 'default', projectId: project.id },
            headers: this.getAuthHeaders()
          })
          if (response.data.code === 200) {
            this.$message.success('删除成功')
            this.loadProjects()
            if (this.selectedProjectId === project.id) {
              this.selectedProjectId = ''
              this.projectRequests = []
            }
          } else {
            this.$message.error(response.data.message)
          }
        } catch (e) {
          this.$message.error('删除失败')
        }
      })
    },
    async doCreate() {
      if (!this.createProjectName.trim()) {
        this.$message.warning('请输入项目名称')
        return
      }

      try {
        const response = await axios.post('/solonapi/apiproject/create', null, {
          params: { 
            user: 'default', 
            name: this.createProjectName, 
            description: this.createProjectDesc,
            isGlobal: this.createToGlobal === 'create_global'
          },
          headers: this.getAuthHeaders()
        })
        if (response.data.code === 200) {
          this.$message.success('创建成功')
          this.showCreateDialog = false
          this.createProjectName = ''
          this.createProjectDesc = ''
          this.createToGlobal = 'create_personal'
          this.loadProjects()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (e) {
        this.$message.error('创建失败')
      }
    },
    handleFileChange(file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        this.importJson = e.target.result
        this.importFileName = file.name
        try {
          const parsed = JSON.parse(this.importJson)
          if (parsed.name) {
            this.importProjectName = parsed.name
          }
        } catch (err) {}
      }
      reader.readAsText(file.raw)
    },
    async doImport() {
      if (!this.importJson.trim()) {
        this.$message.warning('请输入JSON内容')
        return
      }

      let jsonToSend = this.importJson
      if (this.importProjectName.trim()) {
        try {
          const parsed = JSON.parse(this.importJson)
          parsed.name = this.importProjectName.trim()
          jsonToSend = JSON.stringify(parsed)
        } catch (e) {
          this.$message.warning('JSON格式错误，无法修改项目名称')
          return
        }
      }

      try {
        const headers = this.getAuthHeaders()
        headers['Content-Type'] = 'application/json'
        const response = await axios.post('/solonapi/apiproject/import?user=default&isGlobal=' + (this.importToGlobal === 'global'), jsonToSend, {
          headers: headers
        })
        if (response.data.code === 200) {
          this.$message.success('导入成功')
          this.showImportDialog = false
          this.importJson = ''
          this.importFileName = ''
          this.importProjectName = ''
          this.importToGlobal = 'personal'
          this.loadProjects()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (e) {
        this.$message.error('导入失败')
      }
    },
    addParam() {
      this.params.push({ key: '', value: '', enabled: true })
    },
    removeParam(index) {
      this.params.splice(index, 1)
    },
    clearParams() {
      this.params = []
    },
    addHeader() {
      this.headers.push({ key: '', value: '', enabled: true })
    },
    removeHeader(index) {
      this.headers.splice(index, 1)
    },
    clearHeaders() {
      this.headers = []
    },
    addBodyParam() {
      this.bodyParams.push({ key: '', value: '' })
    },
    removeBodyParam(index) {
      this.bodyParams.splice(index, 1)
    },
    clearBodyParams() {
      this.bodyParams = []
    },
    clearResult() {
      this.responseText = ''
      this.responseStatus = null
      this.responseTime = null
    },
    async sendRequest() {
      if (!this.urlPath.trim()) {
        this.$message.warning('请输入URL路径')
        return
      }

      this.loading = true
      this.responseText = ''
      this.responseStatus = null
      this.responseTime = null

      const startTime = Date.now()
      let url = this.urlPath
      if (!url.startsWith('/')) {
        url = '/' + url
      }
      if (!url.startsWith('/maximo')) {
        url = '/maximo' + url
      }

      try {
        const config = {
          method: this.requestMethod,
          url: url,
          headers: {},
          timeout: 60000
        }

        const saved = localStorage.getItem('maximo-env-settings')
        if (saved) {
          try {
            const settings = JSON.parse(saved)
            if (settings.useApiKey && settings.apiKey) {
              config.headers['X-API-Key'] = settings.apiKey
            } else if (settings.maxauth) {
              config.headers['maxauth'] = settings.maxauth
            }
          } catch (e) {
            console.error('解析配置失败', e)
          }
        }

        this.headers.forEach(h => {
          if (h.enabled && h.key) {
            config.headers[h.key] = h.value
          }
        })

        const enabledParams = this.params.filter(p => p.enabled && p.key)
        if (enabledParams.length > 0) {
          const params = {}
          enabledParams.forEach(p => {
            params[p.key] = p.value
          })
          if (this.requestMethod === 'GET') {
            config.params = params
          } else {
            if (!config.params) config.params = {}
            Object.assign(config.params, params)
          }
        }

        if (this.requestMethod !== 'GET') {
          if (this.bodyType === 'form-data') {
            const formData = new FormData()
            this.bodyParams.forEach(p => {
              if (p.key) {
                formData.append(p.key, p.value)
              }
            })
            config.data = formData
            config.headers['Content-Type'] = 'multipart/form-data'
          } else if (this.bodyType === 'urlencoded') {
            const data = {}
            this.bodyParams.forEach(p => {
              if (p.key) {
                data[p.key] = p.value
              }
            })
            config.data = data
            config.headers['Content-Type'] = 'application/x-www-form-urlencoded'
          } else if (this.bodyType === 'json') {
            config.data = this.bodyJson ? JSON.parse(this.bodyJson) : {}
            config.headers['Content-Type'] = 'application/json;charset=utf-8'
          }
        }

        const response = await axios(config)
        this.responseStatus = response.status
        this.responseText = JSON.stringify(response.data, null, 2)
        this.responseTime = Date.now() - startTime
      } catch (error) {
        if (error.response) {
          this.responseStatus = error.response.status
          this.responseText = JSON.stringify(error.response.data, null, 2)
        } else {
          this.responseText = error.message || '请求失败'
        }
        this.responseTime = Date.now() - startTime
      } finally {
        this.loading = false
      }
    },
    copyResult() {
      navigator.clipboard.writeText(this.responseText).then(() => {
        this.$message.success('复制成功')
      }).catch(() => {
        this.$message.error('复制失败')
      })
    }
  }
}
</script>

<style scoped>
.api-caller-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 10px;
}

.api-caller-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.project-select {
  width: 160px;
}

.method-select {
  width: 80px;
}

.url-input {
  flex: 1;
}

.send-btn {
  width: 80px;
}

.clear-btn {
  width: 80px;
}

.api-caller-body {
  flex: 1;
  display: flex;
  gap: 10px;
  overflow: hidden;
}

.request-sidebar {
  width: 260px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
}

.sidebar-filter {
  width: 120px;
}

.sidebar-tree {
  flex: 1;
  overflow: auto;
  padding: 5px;
}

.folder-item {
  margin-bottom: 2px;
}

.folder-title {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 8px;
  cursor: pointer;
  font-size: 13px;
}

.folder-title:hover {
  background: #f5f7fa;
}

.folder-content {
  padding-left: 20px;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  cursor: pointer;
  font-size: 12px;
  border-radius: 4px;
}

.request-item:hover {
  background: #ecf5ff;
}

.request-item.active {
  background: #409eff;
  color: #fff;
}

.method-badge {
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 2px;
  font-weight: bold;
}

.method-badge.get {
  background: #67c23a;
  color: #fff;
}

.method-badge.post {
  background: #409eff;
  color: #fff;
}

.method-badge.put {
  background: #e6a23c;
  color: #fff;
}

.method-badge.delete {
  background: #f56c6c;
  color: #fff;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.params-header {
  margin-bottom: 10px;
  display: flex;
  gap: 10px;
}

.body-type-select {
  margin-bottom: 10px;
  width: 200px;
}

.body-form {
  height: calc(100% - 40px);
}

.body-json {
  height: calc(100% - 40px);
}

.json-textarea {
  width: 100%;
  height: 100%;
  resize: none;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  font-family: monospace;
  font-size: 13px;
}

.body-none {
  padding: 40px;
}

.result-section {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  min-height: 200px;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 8px 15px;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
}

.result-label {
  font-weight: bold;
}

.response-time {
  color: #909399;
  font-size: 12px;
}

.status-code {
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 4px;
}

.status-code.success {
  color: #67c23a;
  background: #e8f5e9;
}

.status-code.error {
  color: #f56c6c;
  background: #fef0f0;
}

.result-body {
  flex: 1;
  overflow: auto;
  padding: 15px;
}

.result-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: monospace;
  font-size: 13px;
  color: #303133;
}

.result-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.project-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.project-filter-input {
  width: 200px;
}

.global-tag {
  color: #67c23a;
  background: #e8f5e9;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.user-tag {
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.import-textarea {
  width: 100%;
}

.import-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.import-option {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.import-option span {
  font-size: 14px;
  font-weight: 500;
}

.file-name {
  font-size: 13px;
  color: #67c23a;
}

.import-radio-group {
  margin-top: 10px;
}

.import-radio-group :deep(.el-radio) {
  margin-right: 20px;
}
</style>
