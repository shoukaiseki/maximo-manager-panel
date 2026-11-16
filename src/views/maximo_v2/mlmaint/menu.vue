<template>
  <ml-maintain :config="config" />
</template>

<script>
import MlMaintain from './MlMaintain'
import { queryMaxMenu, getMaxMenuDetail, exportMaxMenu, importMaxMenu } from '@/api/mlmaint'

export default {
  name: 'MaxMenuMlMaintain',
  components: { MlMaintain },
  data() {
    return {
      config: {
        title: 'MAXMENU 多语言维护',
        objectName: 'MAXMENU',
        listKey: 'maxmenuList',
        mlField: 'HEADERDESCRIPTION',
        primaryKey: 'MAXMENUID',
        idsParam: 'maxmenuIds',
        keyFields: ['MENUTYPE', 'MODULEAPP', 'ELEMENTTYPE', 'KEYVALUE'],
        searchFields: [
          {
            param: 'menutype',
            label: '菜单类型',
            width: 150,
            type: 'select',
            options: ['APPMENU', 'APPTOOL', 'MODULE', 'SEARCHMENU']
          },
          { param: 'moduleapp', label: '模块应用', width: 150 },
          { param: 'elementtype', label: '元素类型', width: 130 },
          { param: 'keyvalue', label: '键值', width: 170 },
          { param: 'value', label: '描述', width: 170 }
        ],
        columns: [
          { prop: 'MENUTYPE', label: '菜单类型', width: 110 },
          { prop: 'MODULEAPP', label: '模块应用', width: 140 },
          { prop: 'POSITION', label: '位置', width: 70 },
          { prop: 'SUBPOSITION', label: '子位置', width: 80 },
          { prop: 'ELEMENTTYPE', label: '元素类型', width: 110 },
          { prop: 'KEYVALUE', label: '键值', minWidth: 180 },
          { prop: 'VISIBLE', label: '可见', width: 70, advanced: true },
          { prop: 'MAXMENUID', label: '主键', width: 110, advanced: true }
        ],
        api: {
          query: queryMaxMenu,
          detail: getMaxMenuDetail,
          exportData: exportMaxMenu,
          importData: importMaxMenu
        }
      }
    }
  }
}
</script>
