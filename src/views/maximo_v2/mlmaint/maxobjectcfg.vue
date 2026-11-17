<template>
  <ml-maintain :config="config" />
</template>

<script>
import MlMaintain from './MlMaintain'
import { queryMaxObjectCfg, getMaxObjectCfgDetail, exportMaxObjectCfg, importMaxObjectCfg } from '@/api/mlmaint'

export default {
  name: 'MaxObjectCfgMlMaintain',
  components: { MlMaintain },
  data() {
    return {
      config: {
        title: 'MAXOBJECTCFG 多语言维护',
        objectName: 'MAXOBJECTCFG',
        listKey: 'maxobjectcfgList',
        mlField: 'DESCRIPTION',
        primaryKey: 'MAXOBJECTID',
        idsParam: 'maxobjectcfgIds',
        keyFields: ['OBJECTNAME'],
        savedQueryApp: 'MLMAINT_MAXOBJECTCFG',
        // 导出按"仅多语言"输出 maxObjects 格式
        exportMl: true,
        // 导入支持 maxObjects 格式(表结构+多语言 / 仅多语言)
        importMaxObjects: true,
        searchFields: [
          { param: 'objectname', label: '对象名', width: 200, col: 'OBJECTNAME' },
          { param: 'value', label: '描述', width: 200 }
        ],
        columns: [
          { prop: 'OBJECTNAME', label: '对象名', width: 200 },
          { prop: 'CLASSNAME', label: '类名', minWidth: 220, advanced: true },
          { prop: 'SERVICENAME', label: '服务名', width: 120, advanced: true },
          { prop: 'SITEORGTYPE', label: '站点/组织', width: 100, advanced: true },
          { prop: 'PERSISTENT', label: '持久化', width: 80, advanced: true },
          { prop: 'MAINOBJECT', label: '主对象', width: 80, advanced: true },
          { prop: 'USERDEFINED', label: '用户定义', width: 90, advanced: true },
          { prop: 'MAXOBJECTID', label: '主键', width: 110, advanced: true }
        ],
        api: {
          query: queryMaxObjectCfg,
          detail: getMaxObjectCfgDetail,
          exportData: exportMaxObjectCfg,
          importData: importMaxObjectCfg
        }
      }
    }
  }
}
</script>
