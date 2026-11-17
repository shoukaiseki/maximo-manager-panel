<template>
  <ml-maintain :config="config" />
</template>

<script>
import MlMaintain from './MlMaintain'
import { queryMaxApps, getMaxAppsDetail, exportMaxApps, importMaxApps } from '@/api/mlmaint'

export default {
  name: 'MaxAppsMlMaintain',
  components: { MlMaintain },
  data() {
    return {
      config: {
        title: 'MAXAPPS 多语言维护',
        objectName: 'MAXAPPS',
        listKey: 'maxappsList',
        mlField: 'DESCRIPTION',
        primaryKey: 'APP',
        idsParam: 'appIds',
        keyFields: ['APP'],
        savedQueryApp: 'MLMAINT_MAXAPPS',
        // 参考 maxobject/maxapps: 所有 / 有XML的应用 / MAFAPP(APPTYPE in ('WC'))
        tabs: [
          { label: '所有', name: 'all' },
          { label: '应用XML', name: 'xml' },
          { label: 'MAFAPP', name: 'mafapp' }
        ],
        searchFields: [
          { param: 'app', label: '应用', width: 180, col: 'APP' },
          { param: 'value', label: '描述', width: 220 }
        ],
        columns: [
          { prop: 'APP', label: '应用', width: 160 },
          { prop: 'APPTYPE', label: '类型', width: 90, advanced: true },
          { prop: 'MAINTBNAME', label: '维护表', width: 160, advanced: true },
          { prop: 'ORIGINALAPP', label: '原始应用', width: 140, advanced: true },
          { prop: 'LANGCODE', label: '基础语言', width: 90, advanced: true },
          { prop: 'ISMOBILE', label: '移动', width: 70, advanced: true },
          { prop: 'LOCKENABLED', label: '锁定', width: 80, advanced: true },
          { prop: 'MAXAPPSID', label: '主键', width: 100, advanced: true }
        ],
        api: {
          query: queryMaxApps,
          detail: getMaxAppsDetail,
          exportData: exportMaxApps,
          importData: importMaxApps
        }
      }
    }
  }
}
</script>
