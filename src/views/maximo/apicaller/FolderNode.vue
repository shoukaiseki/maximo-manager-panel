<template>
  <div class="folder-item">
    <div class="folder-title" @click="$emit('toggle', folder.id)">
      <i class="el-icon-folder" :class="{ 'el-icon-folder-opened': expandedFolders.includes(folder.id) }" />
      {{ folder.name }}
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
        @select-request="(req) => $emit('select-request', req)" />
      <div
        v-for="req in folderRequests" :key="req.id"
        class="request-item"
        :class="{ active: currentRequestId === req.id }"
        @click="$emit('select-request', req)">
        <span class="method-badge" :class="req.method.toLowerCase()">{{ req.method }}</span>
        {{ req.name }}
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
</style>
