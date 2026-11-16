<template>
  <ml-maintain :config="config" />
</template>

<script>
import MlMaintain from './MlMaintain'
import { queryMaxModules, getMaxModuleDetail, exportMaxModules, importMaxModules } from '@/api/mlmaint'

export default {
  name: 'MaxModulesMlMaintain',
  components: { MlMaintain },
  data() {
    return {
      config: {
        title: 'MAXMODULES 多语言维护',
        objectName: 'MAXMODULES',
        listKey: 'maxmodulesList',
        mlField: 'DESCRIPTION',
        primaryKey: 'MAXMODULESID',
        idsParam: 'maxmodulesIds',
        keyFields: ['MODULE'],
        searchFields: [
          { param: 'module', label: '模块', width: 180 },
          { param: 'value', label: '描述', width: 200 }
        ],
        columns: [
          { prop: 'MODULE', label: '模块', width: 160 },
          { prop: 'MAXMODULESID', label: '主键', width: 110, advanced: true }
        ],
        api: {
          query: queryMaxModules,
          detail: getMaxModuleDetail,
          exportData: exportMaxModules,
          importData: importMaxModules
        }
      }
    }
  }
}
</script>
