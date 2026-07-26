<template>
  <div class="api-caller-container">
    <div class="api-caller-header">
      <el-select v-model="selectedProjectId" placeholder="选择项目" class="project-select" size="small" @change="loadProject">
        <el-option label="默认" value="" />
        <el-option v-for="p in projects" :key="p.id" :label="p.name + (p.type === 'global' ? ' (全局)' : '')" :value="p.id" />
      </el-select>
      <el-button type="text" icon="el-icon-folder-opened" @click="showProjectList = true">项目列表</el-button>
      <el-select v-model="selectedEnvId" placeholder="环境" class="env-select" size="small">
        <el-option label="默认" value="" />
        <el-option v-for="e in environments" :key="e.id" :label="e.name" :value="e.id" />
      </el-select>
      <el-button type="text" icon="el-icon-setting" @click="openEnvDialog">环境</el-button>
      <el-select v-model="requestMethod" class="method-select" size="small">
        <el-option label="GET" value="GET" />
        <el-option label="POST" value="POST" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
      </el-select>
      <el-input v-model="urlPath" placeholder="/api/os/MXAPIMESSAGE" class="url-input" />
      <el-button type="primary" @click="sendRequest" :loading="loading" class="send-btn">发送</el-button>
      <el-button @click="clearResult" class="clear-btn">清空</el-button>
      <el-button type="success" icon="el-icon-document" @click="saveCurrentRequest" :disabled="!selectedProjectId" class="save-btn">保存</el-button>
    </div>

    <div class="api-caller-body">
      <div class="request-sidebar" :style="{ width: sidebarWidth + 'px' }">
        <div class="sidebar-header">
          <span>接口列表</span>
          <div class="sidebar-header-right">
            <el-button type="text" size="small" icon="el-icon-folder-add" @click="openAddFolderDialog()" title="添加目录" class="sidebar-action-btn" />
            <el-button type="text" size="small" icon="el-icon-plus" @click="openAddRequestDialog()" title="添加接口" class="sidebar-action-btn" />
            <el-button type="text" :icon="allExpanded ? 'el-icon-caret-bottom' : 'el-icon-caret-right'" @click="toggleExpandAll" class="expand-all-btn" :title="allExpanded ? '全部折叠' : '全部展开'" />
            <el-input v-model="requestFilter" placeholder="搜索" size="small" class="sidebar-filter" />
          </div>
        </div>
        <div class="sidebar-tree">
          <FolderNode v-for="folder in rootFolders" :key="folder.id"
            :folder="folder"
            :all-folders="folders"
            :expanded-folders="expandedFolders"
            :current-request-id="currentRequestId"
            :request-filter="requestFilter"
            :project-requests="projectRequests"
            @toggle="toggleFolder"
            @select-request="loadRequest"
            @add-folder="openAddFolderDialog"
            @add-request="openAddRequestDialog"
            @delete-folder="deleteFolderConfirm"
            @delete-request="deleteRequestConfirm" />
          <div v-for="req in getRequestsWithoutFolder" :key="req.id" 
               class="request-item" :class="{ active: currentRequestId === req.id }"
               @click="loadRequest(req)">
            <span class="method-badge" :class="req.method.toLowerCase()">{{ req.method }}</span>
            {{ req.name }}
            <el-button type="text" icon="el-icon-delete" size="mini" class="sidebar-item-delete" @click.stop="deleteRequestConfirm(req)" />
          </div>
          <div v-if="folders.length === 0 && projectRequests.length === 0" class="sidebar-empty">
            <el-empty description="暂无接口，点击上方按钮添加" :image-size="60" />
          </div>
        </div>
      </div>

      <div
        class="sidebar-divider"
        @mousedown="startSidebarResize"
        :class="{ dragging: sidebarDragging }"
      ></div>

      <div class="main-content">
        <div class="tabs-wrapper">
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
              <el-button type="text" icon="el-icon-plus" @click="addApiKeyHeader">+ apiKey</el-button>
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
              <textarea v-model="bodyJson" class="json-textarea" placeholder="输入JSON格式的请求体..."></textarea>
            </div>

            <div v-if="bodyType === 'none'" class="body-none">
              <el-empty description="无请求体" />
            </div>
          </el-tab-pane>
        </el-tabs>

        <div class="result-divider" @mousedown="startResultResize" :class="{ dragging: resultDragging }">
          <i class="el-icon-d-arrow-thick"></i>
        </div>

        <div class="result-section" :style="resultSectionStyle">
          <div class="result-header">
            <span class="result-label">响应结果</span>
            <span v-if="responseTime" class="response-time">{{ responseTime }}ms</span>
            <span v-if="responseStatus" :class="['status-code', responseStatus >= 200 && responseStatus < 300 ? 'success' : 'error']">
              {{ responseStatus }}
            </span>
            <div class="result-header-right" v-if="responseText">
              <el-button type="text" size="mini" :icon="resultAllExpanded ? 'el-icon-caret-bottom' : 'el-icon-caret-right'" @click="toggleResultExpandAll" title="全部展开/折叠" />
              <el-radio-group v-model="resultViewerMode" size="mini">
                <el-radio-button label="tree">树形</el-radio-button>
                <el-radio-button label="source">源码</el-radio-button>
                <el-radio-button label="table">表格</el-radio-button>
              </el-radio-group>
              <el-button type="text" icon="el-icon-copy-document" @click="copyResult">复制</el-button>
            </div>
          </div>
          <div v-if="responseText && parsedResponse !== null" class="result-body">
            <vue-json-pretty v-if="resultViewerMode === 'tree'" :data="parsedResponse" :deep="resultAllExpanded ? 999 : 3" class="result-json-pretty" />
            <json-viewer v-else-if="resultViewerMode === 'source'" :value="parsedResponse" :expand-depth="resultAllExpanded ? 999 : 3" boxed class="result-json-viewer" />
            <JsonTableGrid v-else-if="resultViewerMode === 'table'" :data="parsedResponse" class="result-json-table" />
          </div>
          <div v-else-if="responseText && parsedResponse === null" class="result-body">
            <pre class="result-pre">{{ responseText }}</pre>
          </div>
          <div v-else class="result-empty">
            <el-empty description="点击发送按钮执行请求" />
          </div>
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
        <el-table-column prop="name" label="项目名称">
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
            <el-tag v-if="scope.row.id === defaultProjectId" size="mini" type="warning" style="margin-left:6px">默认</el-tag>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="260">
          <template slot-scope="scope">
            <el-button type="text" @click="selectProject(scope.row.id)">选择</el-button>
            <el-button type="text" @click="editProject(scope.row)">编辑</el-button>
            <el-button type="text" @click="copyProject(scope.row)">复制</el-button>
            <el-button type="text" @click="exportProject(scope.row)">导出</el-button>
            <el-button type="text" @click="setDefaultProject(scope.row.id)" :disabled="scope.row.id === defaultProjectId">设为默认</el-button>
            <el-button type="text" @click="deleteProjectConfirm(scope.row)">删除</el-button>
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

    <el-dialog title="环境管理" :visible.sync="showEnvDialog" width="600px">
      <div class="env-header">
        <el-button type="primary" icon="el-icon-plus" size="small" @click="addEnv">新建环境</el-button>
      </div>
      <el-table :data="environments" border size="small">
        <el-table-column prop="name" label="环境名称">
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
            <el-tag v-if="scope.row.id === defaultEnvId" size="mini" type="warning" style="margin-left:6px">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变量数">
          <template slot-scope="scope">
            {{ (scope.row.variables || []).length }} 个
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template slot-scope="scope">
            <el-button type="text" @click="editEnv(scope.row)">编辑</el-button>
            <el-button type="text" @click="setDefaultEnv(scope.row.id)" :disabled="scope.row.id === defaultEnvId">设为默认</el-button>
            <el-button type="text" style="color:#f56c6c" @click="deleteEnv(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog :title="envEditId ? '编辑环境' : '新建环境'" :visible.sync="showEnvEditDialog" width="500px">
      <el-form>
        <el-form-item label="环境名称">
          <el-input v-model="envEditName" placeholder="如：开发、测试、生产" />
        </el-form-item>
        <el-form-item label="变量列表">
          <div class="env-vars-header">
            <span>使用 <code>{<!-- -->{key}}</code> 语法在 URL/Header/参数中引用变量</span>
            <el-button type="text" icon="el-icon-plus" @click="addEnvVar">添加变量</el-button>
          </div>
          <div v-for="(v, idx) in envEditVars" :key="idx" class="env-var-row">
            <el-input v-model="v.key" placeholder="变量名" size="small" class="env-var-key" />
            <el-select v-model="v.valueType" size="small" class="env-var-type" @change="onEnvVarTypeChange(v)">
              <el-option label="自定义" value="default" />
              <el-option label="系统预设" value="system" />
            </el-select>
            <template v-if="v.valueType === 'system'">
              <el-select v-model="v.value" placeholder="选择预设" size="small" class="env-var-value">
                <el-option label="全局 API Key (apiKey)" value="apiKey" />
                <el-option label="Maximo 接口路径 (masUrl)" value="masUrl" />
              </el-select>
            </template>
            <template v-else>
              <el-input v-model="v.value" placeholder="变量值" size="small" class="env-var-value" />
            </template>
            <el-button type="text" icon="el-icon-delete" style="color:#f56c6c" @click="envEditVars.splice(idx, 1)" />
          </div>
          <div class="env-preset-hint">
            预设变量（自动生效）：<code>{<!-- -->{apiKey}}</code> <code>{<!-- -->{maxauth}}</code> <code>{<!-- -->{baseUrl}}</code>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showEnvEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEnv">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog title="添加目录" :visible.sync="showFolderDialog" width="400px">
      <el-form>
        <el-form-item label="目录名称">
          <el-input v-model="editFolderName" placeholder="输入目录名称" />
        </el-form-item>
        <el-form-item label="上级目录">
          <el-select v-model="editFolderParentId" placeholder="无（根目录）" clearable style="width:100%">
            <el-option label="无（根目录）" value="" />
            <el-option v-for="f in folders" :key="f.id" :label="f.name" :value="f.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showFolderDialog = false">取消</el-button>
        <el-button type="primary" @click="saveFolder">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="添加接口" :visible.sync="showRequestDialog" width="400px">
      <el-form>
        <el-form-item label="接口名称">
          <el-input v-model="editRequestName" placeholder="输入接口名称" />
        </el-form-item>
        <el-form-item label="请求方式">
          <el-select v-model="editRequestMethod" style="width:100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属目录">
          <el-select v-model="editRequestFolderId" placeholder="无（根目录）" clearable style="width:100%">
            <el-option label="无（根目录）" value="" />
            <el-option v-for="f in folders" :key="f.id" :label="f.name" :value="f.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showRequestDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAddRequest">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'
import FolderNode from './FolderNode.vue'
import JsonTableGrid from '/src/components/JsonTableGrid.vue'

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
      environments: [],
      selectedEnvId: '',
      showEnvDialog: false,
      showEnvEditDialog: false,
      envEditId: '',
      envEditName: '',
      envEditVars: [],
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
      responseTime: null,
      resultDragging: false,
      resultViewerMode: 'tree',
      resultAllExpanded: false,
      resultSectionHeight: 350,
      resultStartY: 0,
      resultStartHeight: 0,
      sidebarWidth: 260,
      sidebarDragging: false,
      sidebarStartX: 0,
      sidebarStartWidth: 0,
      showFolderDialog: false,
      editFolderId: '',
      editFolderName: '',
      editFolderParentId: '',
      showRequestDialog: false,
      editRequestId: '',
      editRequestName: '',
      editRequestMethod: 'GET',
      editRequestFolderId: '',
      currentRequestName: '',
      currentRequestFolderId: '',
      defaultProjectId: localStorage.getItem('apicaller-default-project') || '',
      defaultEnvId: localStorage.getItem('apicaller-default-env') || ''
    }
  },
  components: {
    FolderNode,
    JsonTableGrid
  },
  computed: {
    parsedResponse() {
      try {
        return JSON.parse(this.responseText)
      } catch (e) {
        return null
      }
    },
    resultSectionStyle() {
      return { height: this.resultSectionHeight + 'px' }
    },
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
    rootFolders() {
      return this.folders.filter(f => !f.parentId)
    },
    allExpanded() {
      return this.folders.length > 0 && this.expandedFolders.length === this.folders.length
    },
    getRequestsWithoutFolder() {
      return this.projectRequests.filter(r => !r.folderId && 
        r.name.toLowerCase().includes(this.requestFilter.toLowerCase()))
    },
    resolvedEnvVariables() {
      const vars = {}
      // 预设变量
      try {
        const saved = localStorage.getItem('maximo-env-settings')
        if (saved) {
          const settings = JSON.parse(saved)
          if (settings.apiKey) vars['apiKey'] = settings.apiKey
          if (settings.maxauth) vars['maxauth'] = settings.maxauth
          if (settings.baseUrl) vars['baseUrl'] = settings.baseUrl
        }
      } catch (e) {}
      if (!vars['baseUrl']) vars['baseUrl'] = 'http://localhost:9080'
      // 选中环境的用户自定义变量（覆盖预设）
      if (this.selectedEnvId) {
        const env = this.environments.find(e => e.id === this.selectedEnvId)
        if (env && env.variables) {
          env.variables.forEach(v => {
            if (!v.key) return
            if (v.valueType === 'system') {
              // 系统预设变量
              if (v.value === 'apiKey') {
                const saved = localStorage.getItem('maximo-env-settings')
                if (saved) {
                  try {
                    const settings = JSON.parse(saved)
                    if (settings.apiKey) vars[v.key] = settings.apiKey
                  } catch (e) {}
                }
              } else if (v.value === 'masUrl') {
                vars[v.key] = '/maximo'
              }
            } else {
              vars[v.key] = v.value
            }
          })
        }
      }
      return vars
    }
  },
  mounted() {
    this.loadProjects()
    this.loadEnvironments()
    document.addEventListener('mousemove', this.doResultResize)
    document.addEventListener('mouseup', this.stopResultResize)
    document.addEventListener('mousemove', this.doSidebarResize)
    document.addEventListener('mouseup', this.stopSidebarResize)
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.doResultResize)
    document.removeEventListener('mouseup', this.stopResultResize)
    document.removeEventListener('mousemove', this.doSidebarResize)
    document.removeEventListener('mouseup', this.stopSidebarResize)
  },
  methods: {
    startResultResize(e) {
      this.resultDragging = true
      this.resultStartY = e.clientY
      this.resultStartHeight = this.resultSectionHeight
      document.body.style.cursor = 'row-resize'
      document.body.style.userSelect = 'none'
    },
    doResultResize(e) {
      if (!this.resultDragging) return
      const delta = this.resultStartY - e.clientY
      const mainContent = this.$el && this.$el.querySelector('.main-content')
      if (mainContent) {
        const maxHeight = mainContent.clientHeight - 100
        let newHeight = this.resultStartHeight + delta
        newHeight = Math.max(150, Math.min(newHeight, maxHeight))
        this.resultSectionHeight = newHeight
      }
    },
    stopResultResize() {
      if (this.resultDragging) {
        this.resultDragging = false
        document.body.style.cursor = ''
        document.body.style.userSelect = ''
      }
    },
    toggleResultExpandAll() {
      this.resultAllExpanded = !this.resultAllExpanded
    },
    startSidebarResize(e) {
      this.sidebarDragging = true
      this.sidebarStartX = e.clientX
      this.sidebarStartWidth = this.sidebarWidth
      document.body.style.cursor = 'col-resize'
      document.body.style.userSelect = 'none'
    },
    doSidebarResize(e) {
      if (!this.sidebarDragging) return
      const delta = e.clientX - this.sidebarStartX
      let newWidth = this.sidebarStartWidth + delta
      newWidth = Math.max(160, Math.min(newWidth, 600))
      this.sidebarWidth = newWidth
    },
    stopSidebarResize() {
      if (this.sidebarDragging) {
        this.sidebarDragging = false
        document.body.style.cursor = ''
        document.body.style.userSelect = ''
      }
    },
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
          // 自动选择默认项目
          if (this.defaultProjectId && !this.selectedProjectId) {
            const exists = this.projects.some(p => p.id === this.defaultProjectId)
            if (exists) {
              this.selectedProjectId = this.defaultProjectId
              this.loadProject(this.defaultProjectId)
            }
          }
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
    toggleExpandAll() {
      if (this.allExpanded) {
        this.expandedFolders = []
      } else {
        this.expandedFolders = this.folders.map(f => f.id)
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
      this.currentRequestName = request.name || ''
      this.currentRequestFolderId = request.folderId || ''
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
    addApiKeyHeader() {
      this.headers.push({ key: 'apiKey', value: '{{apiKey}}', enabled: true })
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
      let url = this.resolveTemplate(this.urlPath)
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
            config.headers[this.resolveTemplate(h.key)] = this.resolveTemplate(h.value)
          }
        })

        const enabledParams = this.params.filter(p => p.enabled && p.key)
        if (enabledParams.length > 0) {
          const params = {}
          enabledParams.forEach(p => {
            params[this.resolveTemplate(p.key)] = this.resolveTemplate(p.value)
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
            config.data = this.bodyJson ? JSON.parse(this.resolveTemplate(this.bodyJson)) : {}
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
    },
    resolveTemplate(text) {
      if (!text || !this.selectedEnvId) return text
      let result = text
      const vars = this.resolvedEnvVariables
      for (const key of Object.keys(vars)) {
        const regex = new RegExp('\\{\\{' + key + '\\}\\}', 'g')
        result = result.replace(regex, vars[key])
      }
      return result
    },
    async loadEnvironments() {
      try {
        const response = await axios.get('/solonapi/apiproject/env/list', { headers: this.getAuthHeaders() })
        if (response.data.code === 200) {
          this.environments = response.data.data || []
          // 自动选择默认环境
          if (this.defaultEnvId && !this.selectedEnvId) {
            const exists = this.environments.some(e => e.id === this.defaultEnvId)
            if (exists) {
              this.selectedEnvId = this.defaultEnvId
            }
          }
        }
      } catch (e) {
        console.error('加载环境列表失败', e)
      }
    },
    openEnvDialog() {
      this.loadEnvironments()
      this.showEnvDialog = true
    },
    addEnv() {
      this.envEditId = ''
      this.envEditName = ''
      this.envEditVars = []
      this.showEnvDialog = false
      this.showEnvEditDialog = true
    },
    onEnvVarTypeChange(v) {
      if (v.valueType === 'system') {
        v.value = 'apiKey'
      } else {
        v.value = ''
      }
    },
    editEnv(env) {
      this.envEditId = env.id
      this.envEditName = env.name
      this.envEditVars = (env.variables || []).map(v => ({
        key: v.key || '',
        value: v.value || '',
        valueType: v.valueType || 'default'
      }))
      this.showEnvDialog = false
      this.showEnvEditDialog = true
    },
    async deleteEnv(env) {
      this.$confirm('确定删除环境 "' + env.name + '" 吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
        .then(async () => {
          try {
            const response = await axios.post('/solonapi/apiproject/env/delete', null, {
              params: { id: env.id },
              headers: this.getAuthHeaders()
            })
            if (response.data.code === 200) {
              this.$message.success('删除成功')
              this.loadEnvironments()
              if (this.selectedEnvId === env.id) this.selectedEnvId = ''
            } else {
              this.$message.error(response.data.message)
            }
          } catch (e) {
            this.$message.error('删除失败')
          }
        })
    },
    setDefaultProject(projectId) {
      this.defaultProjectId = projectId
      localStorage.setItem('apicaller-default-project', projectId)
      this.$message.success('已设为默认项目')
    },
    setDefaultEnv(envId) {
      this.defaultEnvId = envId
      localStorage.setItem('apicaller-default-env', envId)
      this.$message.success('已设为默认环境')
    },
    addEnvVar() {
      this.envEditVars.push({ key: '', value: '', valueType: 'default' })
    },
    async saveEnv() {
      if (!this.envEditName.trim()) {
        this.$message.warning('请输入环境名称')
        return
      }
      try {
        const headers = this.getAuthHeaders()
        headers['Content-Type'] = 'application/json'
        const body = JSON.stringify({
          id: this.envEditId,
          name: this.envEditName,
          variables: this.envEditVars.filter(v => v.key.trim()).map(v => ({
            key: v.key,
            value: v.valueType === 'system' ? v.value : v.value,
            valueType: v.valueType || 'default'
          }))
        })
        const response = await axios.post('/solonapi/apiproject/env/save', body, { headers })
        if (response.data.code === 200) {
          this.$message.success('保存成功')
          this.showEnvEditDialog = false
          this.loadEnvironments()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (e) {
        this.$message.error('保存失败')
      }
    },
    openAddFolderDialog(parentId) {
      this.editFolderId = ''
      this.editFolderName = ''
      this.editFolderParentId = parentId || ''
      this.showFolderDialog = true
    },
    async saveFolder() {
      if (!this.editFolderName.trim()) {
        this.$message.warning('请输入目录名称')
        return
      }
      try {
        const response = await axios.post('/solonapi/apiproject/folder/save', null, {
          params: {
            projectId: this.selectedProjectId,
            id: this.editFolderId,
            name: this.editFolderName,
            parentId: this.editFolderParentId || ''
          },
          headers: this.getAuthHeaders()
        })
        if (response.data.code === 200) {
          this.$message.success('目录创建成功')
          this.showFolderDialog = false
          this.loadProject(this.selectedProjectId)
        } else {
          this.$message.error(response.data.message)
        }
      } catch (e) {
        this.$message.error('保存目录失败')
      }
    },
    deleteFolderConfirm(folderId) {
      this.$confirm('确定删除该目录及其所有子目录和接口吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await axios.post('/solonapi/apiproject/folder/delete', null, {
            params: { projectId: this.selectedProjectId, folderId },
            headers: this.getAuthHeaders()
          })
          if (response.data.code === 200) {
            this.$message.success('删除成功')
            this.loadProject(this.selectedProjectId)
          } else {
            this.$message.error(response.data.message)
          }
        } catch (e) {
          this.$message.error('删除失败')
        }
      })
    },
    openAddRequestDialog(folderId) {
      this.editRequestId = ''
      this.editRequestName = ''
      this.editRequestMethod = 'GET'
      this.editRequestFolderId = folderId || ''
      this.showRequestDialog = true
    },
    async confirmAddRequest() {
      if (!this.editRequestName.trim()) {
        this.$message.warning('请输入接口名称')
        return
      }
      try {
        const requestData = {
          id: '',
          name: this.editRequestName,
          method: this.editRequestMethod,
          url: '',
          folderId: this.editRequestFolderId || null,
          params: [],
          headers: [],
          body: {}
        }
        const headers = this.getAuthHeaders()
        headers['Content-Type'] = 'application/json'
        const response = await axios.post('/solonapi/apiproject/request/save?projectId=' + this.selectedProjectId, JSON.stringify(requestData), { headers })
        if (response.data.code === 200) {
          this.$message.success('接口创建成功')
          this.showRequestDialog = false
          const newReqId = response.data.data.id
          await this.loadProject(this.selectedProjectId)
          // 自动选中新创建的接口
          const newReq = this.projectRequests.find(r => r.id === newReqId)
          if (newReq) {
            this.loadRequest(newReq)
          }
        } else {
          this.$message.error(response.data.message)
        }
      } catch (e) {
        this.$message.error('创建接口失败')
      }
    },
    async saveCurrentRequest() {
      if (!this.selectedProjectId) {
        this.$message.warning('请先选择项目')
        return
      }

      const requestData = {
        id: this.currentRequestId || '',
        name: this.currentRequestName || '',
        method: this.requestMethod,
        url: this.urlPath,
        folderId: this.currentRequestFolderId || null,
        params: this.params.filter(p => p.key).map(p => ({
          key: p.key,
          value: p.value,
          enabled: p.enabled !== false
        })),
        headers: this.headers.filter(h => h.key).map(h => ({
          key: h.key,
          value: h.value,
          enabled: h.enabled !== false
        })),
        body: {}
      }

      if (this.bodyType !== 'none') {
        if (this.bodyType === 'json') {
          requestData.body = { type: 'json', content: this.bodyJson }
        } else if (this.bodyType === 'form-data') {
          requestData.body = {
            type: 'form-data',
            formData: this.bodyParams.filter(p => p.key).map(p => ({ key: p.key, value: p.value }))
          }
        } else if (this.bodyType === 'urlencoded') {
          requestData.body = {
            type: 'urlencoded',
            urlEncoded: this.bodyParams.filter(p => p.key).map(p => ({ key: p.key, value: p.value }))
          }
        }
      }

      try {
        const headers = this.getAuthHeaders()
        headers['Content-Type'] = 'application/json'
        const response = await axios.post('/solonapi/apiproject/request/save?projectId=' + this.selectedProjectId, JSON.stringify(requestData), { headers })
        if (response.data.code === 200) {
          this.$message.success('保存成功')
          this.currentRequestId = response.data.data.id
          this.loadProject(this.selectedProjectId)
        } else {
          this.$message.error(response.data.message)
        }
      } catch (e) {
        this.$message.error('保存失败')
      }
    },
    deleteRequestConfirm(request) {
      this.$confirm('确定删除接口 "' + request.name + '" 吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await axios.post('/solonapi/apiproject/request/delete', null, {
            params: { requestId: request.id },
            headers: this.getAuthHeaders()
          })
          if (response.data.code === 200) {
            this.$message.success('删除成功')
            if (this.currentRequestId === request.id) {
              this.currentRequestId = ''
            }
            this.loadProject(this.selectedProjectId)
          } else {
            this.$message.error(response.data.message)
          }
        } catch (e) {
          this.$message.error('删除失败')
        }
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
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-divider {
  width: 5px;
  background: #e8eaed;
  cursor: col-resize;
  border-radius: 3px;
  flex-shrink: 0;
  transition: background 0.15s;
}

.sidebar-divider:hover,
.sidebar-divider.dragging {
  background: #409eff;
}

.tabs-wrapper {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
}

.sidebar-header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.expand-all-btn {
  padding: 0 4px;
  font-size: 14px;
}

.sidebar-action-btn {
  padding: 0 4px;
  font-size: 14px;
  color: #909399;
}

.sidebar-action-btn:hover {
  color: #409eff;
}

.save-btn {
  margin-left: 4px;
}

.sidebar-item-delete {
  margin-left: auto;
  padding: 0 2px;
  font-size: 12px;
  color: #c0c4cc;
  visibility: hidden;
}

.request-item:hover .sidebar-item-delete {
  visibility: visible;
  color: #f56c6c;
}

.sidebar-filter {
  width: 120px;
}

.sidebar-empty {
  padding: 20px 10px;
  display: flex;
  justify-content: center;
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
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-top: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.result-divider {
  height: 6px;
  background: #e8eaed;
  cursor: row-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 10px;
  margin-bottom: 0;
  border-radius: 3px;
  flex-shrink: 0;
  transition: background 0.15s;
}

.result-divider:hover,
.result-divider.dragging {
  background: #409eff;
}

.result-divider i {
  font-size: 10px;
  color: #909399;
  line-height: 1;
}

.result-divider:hover i,
.result-divider.dragging i {
  color: #fff;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
  flex-shrink: 0;
}

.result-header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
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
  padding: 0;
}

.result-body :deep(.vue-json-pretty) {
  padding: 12px 16px;
  font-size: 13px;
}

.result-body :deep(.json-viewer) {
  padding: 12px 16px;
  font-size: 13px;
}

.result-body :deep(.jv-container) {
  border: none !important;
}

.result-json-table {
  height: 100%;
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

.env-select {
  width: 100px;
}

.env-header {
  margin-bottom: 10px;
}

.env-vars-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.env-var-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.env-var-key {
  width: 130px;
}

.env-var-type {
  width: 110px;
}

.env-var-value {
  flex: 1;
}

.env-preset-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.env-preset-hint code {
  background: #f5f7fa;
  padding: 1px 4px;
  border-radius: 2px;
  font-size: 12px;
  color: #409eff;
}
</style>
