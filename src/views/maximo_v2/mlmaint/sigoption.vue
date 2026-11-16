<template>
  <ml-maintain :config="config" />
</template>

<script>
import MlMaintain from './MlMaintain'
import { querySigOption, getSigOptionDetail, exportSigOption, importSigOption } from '@/api/mlmaint'

export default {
  name: 'SigOptionMlMaintain',
  components: { MlMaintain },
  data() {
    return {
      config: {
        title: 'SIGOPTION 多语言维护',
        objectName: 'SIGOPTION',
        listKey: 'sigoptionList',
        mlField: 'DESCRIPTION',
        primaryKey: 'SIGOPTIONID',
        idsParam: 'sigoptionIds',
        keyFields: ['APP', 'OPTIONNAME'],
        searchFields: [
          { param: 'app', label: '应用', width: 180 },
          { param: 'optionname', label: '选项名', width: 180 },
          { param: 'value', label: '描述', width: 200 }
        ],
        columns: [
          { prop: 'APP', label: '应用', width: 160 },
          { prop: 'OPTIONNAME', label: '选项名', width: 160 },
          { prop: 'ALSOGRANTS', label: '同时授权', minWidth: 160, advanced: true },
          { prop: 'ALSOREVOKES', label: '同时撤销', minWidth: 160, advanced: true },
          { prop: 'PREREQUISITE', label: '前提条件', minWidth: 160, advanced: true },
          { prop: 'VISIBLE', label: '可见', width: 70, advanced: true },
          { prop: 'ESIGENABLED', label: '电子签名', width: 90, advanced: true },
          { prop: 'SIGOPTIONID', label: '主键', width: 110, advanced: true }
        ],
        api: {
          query: querySigOption,
          detail: getSigOptionDetail,
          exportData: exportSigOption,
          importData: importSigOption
        }
      }
    }
  }
}
</script>
