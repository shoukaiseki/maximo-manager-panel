<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item.children,item) && (!onlyOneChild.children||onlyOneChild.noShowingChildren)&&!item.alwaysShow">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path)">
        <el-menu-item
          :index="resolvePath(onlyOneChild.path)"
          :class="['menu-lv-' + level, {'submenu-title-noDropdown':!isNest}]">
          <item :icon="onlyOneChild.meta.icon||(item.meta&&item.meta.icon)" :title="onlyOneChild.meta.title" />
        </el-menu-item>
      </app-link>
    </template>

    <el-submenu v-else ref="subMenu" :index="resolvePath(item.path)" :class="'menu-lv-' + level" popper-append-to-body>
      <template slot="title">
        <item v-if="item.meta" :icon="item.meta && item.meta.icon" :title="item.meta.title" />
      </template>
      <sidebar-item
        v-for="child in item.children"
        :key="child.path"
        :is-nest="true"
        :level="level + 1"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </el-submenu>
  </div>
</template>

<script>
import path from 'path'
import { isExternal } from '@/utils/validate'
import Item from './Item'
import AppLink from './Link'
import FixiOSBug from './FixiOSBug'

export default {
  name: 'SidebarItem',
  components: { Item, AppLink },
  mixins: [FixiOSBug],
  props: {
    // route object
    item: {
      type: Object,
      required: true
    },
    isNest: {
      type: Boolean,
      default: false
    },
    // 菜单层级: 1=一级, 2=二级, 3=三级(用于层级化缩进/字重)
    level: {
      type: Number,
      default: 1
    },
    basePath: {
      type: String,
      default: ''
    }
  },
  data() {
    this.onlyOneChild = null
    return {}
  },
  methods: {
    hasOneShowingChild(children = [], parent) {
      if (!children) {
        children = [];
      }
      const showingChildren = children.filter(item => {
        if (item.hidden) {
          return false
        } else {
          // Temp set(will be used if only has one showing child)
          this.onlyOneChild = item
          return true
        }
      })

      // When there is only one child router, the child router is displayed by default
      if (showingChildren.length === 1) {
        return true
      }

      // Show parent if there are no child router to display
      if (showingChildren.length === 0) {
        this.onlyOneChild = { ... parent, path: '', noShowingChildren: true }
        return true
      }

      return false
    },
    resolvePath(routePath) {
      if (isExternal(routePath)) {
        return routePath
      }
      if (isExternal(this.basePath)) {
        return this.basePath
      }
      return path.resolve(this.basePath, routePath)
    }
  }
}
</script>
<style lang="scss">
// 菜单层级区分: 一级加粗, 二级/三级逐级缩进并缩小字号
// 限定在 .sidebar-container 内, 折叠态(.el-menu--collapse)与折叠弹出层不受影响
.sidebar-container .el-menu:not(.el-menu--collapse) {
  .menu-lv-1.el-menu-item,
  .menu-lv-1 > .el-submenu__title {
    padding-left: 16px !important;
    font-weight: 600;
  }

  .menu-lv-2.el-menu-item,
  .menu-lv-2 > .el-submenu__title {
    padding-left: 36px !important;
    font-weight: 400;
  }

  .menu-lv-3.el-menu-item,
  .menu-lv-3 > .el-submenu__title {
    padding-left: 56px !important;
    font-weight: 400;
  }

  .menu-lv-2 span,
  .menu-lv-3 span {
    font-size: 13px;
  }
}
</style>

