<template>
  <ml-maintain :config="config" />
</template>

<script>
import MlMaintain from './MlMaintain'
import { queryMaxAttributeCfg, getMaxAttributeCfgDetail, exportMaxAttributeCfg, importMaxAttributeCfg } from '@/api/mlmaint'

export default {
  name: 'MaxAttributeCfgMlMaintain',
  components: { MlMaintain },
  data() {
    return {
      config: {
        title: 'MAXATTRIBUTECFG 多语言维护',
        objectName: 'MAXATTRIBUTECFG',
        listKey: 'maxattributecfgList',
        // 两个多语言字段: TITLE(80) / REMARKS(4000)
        mlFields: [
          { field: 'TITLE', label: '标题', maxlength: 80 },
          { field: 'REMARKS', label: '备注', maxlength: 4000 }
        ],
        primaryKey: 'MAXATTRIBUTEID',
        idsParam: 'maxattributecfgIds',
        keyFields: ['OBJECTNAME', 'ATTRIBUTENAME'],
        savedQueryApp: 'MLMAINT_MAXATTRIBUTECFG',
        // 导出按"仅多语言"输出 maxObjects 格式, 并限定到属性级
        exportMl: true,
        exportAttributes: true,
        // 导入支持 maxObjects 格式(表结构+多语言 / 仅多语言)
        importMaxObjects: true,
        searchFields: [
          { param: 'objectname', label: '对象名', width: 180, col: 'OBJECTNAME' },
          { param: 'attributename', label: '属性名', width: 180, col: 'ATTRIBUTENAME' },
          { param: 'value', label: '标题/备注', width: 200 }
        ],
        columns: [
          { prop: 'OBJECTNAME', label: '对象名', width: 180 },
          { prop: 'ATTRIBUTENAME', label: '属性名', width: 180 },
          { prop: 'MAXTYPE', label: '类型', width: 100, advanced: true },
          { prop: 'LENGTH', label: '长度', width: 80, advanced: true },
          { prop: 'REQUIRED', label: '必填', width: 70, advanced: true },
          { prop: 'PERSISTENT', label: '持久化', width: 80, advanced: true },
          { prop: 'DOMAINID', label: '域', width: 120, advanced: true },
          { prop: 'ALIAS', label: '别名', width: 140, advanced: true },
          { prop: 'MAXATTRIBUTEID', label: '主键', width: 110, advanced: true }
        ],
        api: {
          query: queryMaxAttributeCfg,
          detail: getMaxAttributeCfgDetail,
          exportData: exportMaxAttributeCfg,
          importData: importMaxAttributeCfg
        }
      }
    }
  }
}
</script>
