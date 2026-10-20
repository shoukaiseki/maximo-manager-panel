<template>
  <div class="folder-item">
    <div class="folder-title" @click="$emit('toggle', folder.id)">
      <i v-if="hasChildren" :class="expandedFolders.includes(folder.id) ? 'el-icon-caret-bottom' : 'el-icon-caret-right'" class="caret-icon" />
      <i class="el-icon-folder" :class="{ 'el-icon-folder-opened': expandedFolders.includes(folder.id) }" />
      {{ folder.name }}
      <span class="folder-actions">
        <el-button type="text" size="mini" icon="el-icon-plus" class="folder-action-btn" title="添加子目录" @click.stop="$emit('add-folder', folder.id)" />
        <el-button type="text" size="mini" icon="el-icon-edit-outline" class="folder-action-btn" title="添加接口到此目录" @click.stop="$emit('add-request', folder.id)" />
        <el-button type="text" size="mini" icon="el-icon-delete" class="folder-action-btn folder-delete-btn" title="删除目录" @click.stop="$emit('delete-folder', folder.id)" />
      </span>
    </div>
    <div v-if="expandedFolders.includes(folder.id)" class="folder-content">
      <FolderNode
        v-for="child in childFolders" :key="child.id"
        :folder="child"
        :all-folders="allFolders"
        :expanded-folders="expandedFolders"
        :current-request-id="currentRequestId"
        :request-filter="requestFilter"
        :project-requests="projectRequests"
        @toggle="(id) => $emit('toggle', id)"
        @select-request="(req) => $emit('select-request', req)"
        @add-folder="(pid) => $emit('add-folder', pid)"
        @add-request="(pid) => $emit('add-request', pid)"
        @delete-folder="(pid) => $emit('delete-folder', pid)"
        @delete-request="(req) => $emit('delete-request', req)" />
      <div
        v-for="req in folderRequests" :key="req.id"
        class="request-item"
        :class="{ active: currentRequestId === req.id }"
        @click="$emit('select-request', req)">
        <span class="method-badge" :class="req.method.toLowerCase()">{{ req.method }}</span>
        {{ req.name }}
        <el-button type="text" icon="el-icon-delete" size="mini" class="request-delete-btn" @click.stop="$emit('delete-request', req)" />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FolderNode',
  props: {
    folder: { type: Object, required: true },
    allFolders: { type: Array, required: true },
    expandedFolders: { type: Array, required: true },
    currentRequestId: { type: String, default: '' },
    requestFilter: { type: String, default: '' },
    projectRequests: { type: Array, required: true }
  },
  computed: {
    childFolders() {
      return this.allFolders.filter(f => f.parentId === this.folder.id)
    },
    hasChildren() {
      return this.childFolders.length > 0 || this.projectRequests.some(r => r.folderId === this.folder.id)
    },
    folderRequests() {
      const nameFilter = this.requestFilter || ''
      return this.projectRequests.filter(r =>
        r.folderId === this.folder.id &&
        r.name.toLowerCase().includes(nameFilter.toLowerCase())
      )
    }
  }
}
</script>

<style scoped>
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

.caret-icon {
  font-size: 12px;
  color: #909399;
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

.folder-actions {
  display: none;
  margin-left: auto;
  gap: 2px;
}

.folder-title:hover .folder-actions {
  display: inline-flex;
}

.folder-action-btn {
  padding: 0 2px;
  font-size: 12px;
  color: #909399;
}

.folder-action-btn:hover {
  color: #409eff;
}

.folder-delete-btn:hover {
  color: #f56c6c !important;
}

.request-delete-btn {
  margin-left: auto;
  padding: 0 2px;
  font-size: 12px;
  color: #c0c4cc;
  visibility: hidden;
}

.request-item:hover .request-delete-btn {
  visibility: visible;
  color: #f56c6c;
}
</style>
