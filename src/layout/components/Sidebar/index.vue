<template>
    <div
        :class="{'has-logo':showLogo, 'has-menu-search': !isCollapse}"
        :style="{ backgroundColor: settings.sideTheme === 'theme-dark' ? variables.menuBg : variables.menuLightBg }"
    >
        <logo v-if="showLogo" :collapse="isCollapse" />
        <div v-if="!isCollapse" class="sidebar-menu-search" :class="settings.sideTheme">
            <el-input
                v-model="menuSearch"
                size="mini"
                clearable
                placeholder="搜索菜单"
                prefix-icon="el-icon-search"
            />
        </div>
        <el-scrollbar :class="settings.sideTheme" wrap-class="scrollbar-wrapper">
            <p v-if="isSearching && !filteredRoutes.length" class="sidebar-menu-empty">未找到匹配菜单</p>
            <el-menu
                :default-active="activeMenu"
                :default-openeds="searchOpeneds"
                :collapse="isCollapse"
                :background-color="settings.sideTheme === 'theme-dark' ? variables.menuBg : variables.menuLightBg"
                :text-color="settings.sideTheme === 'theme-dark' ? variables.menuText : 'rgba(0,0,0,.65)'"
                :unique-opened="true"
                :active-text-color="settings.theme"
                :collapse-transition="false"
                mode="vertical"
            >
                <sidebar-item
                    v-for="(route, index) in filteredRoutes"
                    :key="route.path  + index"
                    :item="route"
                    :base-path="route.path"
                />
            </el-menu>
        </el-scrollbar>
    </div>
</template>

<script>
import path from "path";
import { mapGetters, mapState } from "vuex";
import Logo from "./Logo";
import SidebarItem from "./SidebarItem";
import variables from "@/assets/styles/variables.scss";

export default {
    components: { SidebarItem, Logo },
    data() {
        return {
            menuSearch: ""
        };
    },
    computed: {
        ...mapState(["settings"]),
        ...mapGetters(["permission_routes", "sidebar"]),
        activeMenu() {
            const route = this.$route;
            const { meta, path } = route;
            // if set path, the sidebar will highlight the path you set
            if (meta.activeMenu) {
                return meta.activeMenu;
            }
            return path;
        },
        showLogo() {
            return this.$store.state.settings.sidebarLogo;
        },
        variables() {
            return variables;
        },
        isCollapse() {
            return !this.sidebar.opened;
        },
        isSearching() {
            return !!this.menuSearch.trim();
        },
        // 按菜单标题过滤(命中父级则保留整个分支, 命中子级则保留其祖先链)
        filteredRoutes() {
            const keyword = this.menuSearch.trim().toLowerCase();
            if (!keyword) {
                return this.permission_routes;
            }
            return this.filterMenu(this.permission_routes, keyword);
        },
        // 搜索时自动展开命中的父级菜单
        searchOpeneds() {
            if (!this.isSearching) {
                return [];
            }
            const opened = [];
            const walk = (routes, basePath) => {
                routes.forEach(route => {
                    const fullPath = path.resolve(basePath, route.path);
                    if (route.children && route.children.length) {
                        opened.push(fullPath);
                        walk(route.children, fullPath);
                    }
                });
            };
            walk(this.filteredRoutes, "/");
            return opened;
        }
    },
    methods: {
        filterMenu(routes, keyword) {
            const result = [];
            routes.forEach(route => {
                if (route.hidden) {
                    return;
                }
                const title = ((route.meta && route.meta.title) || "").toLowerCase();
                const children = route.children ? this.filterMenu(route.children, keyword) : [];
                if (title.indexOf(keyword) > -1) {
                    result.push(route);
                } else if (children.length) {
                    result.push({ ...route, children });
                }
            });
            return result;
        }
    }
};
</script>

<style lang="scss" scoped>
.sidebar-menu-search {
    padding: 8px 10px;
    line-height: normal;

    ::v-deep .el-input__inner {
        border-radius: 4px;
        background: rgba(255, 255, 255, 0.08);
        border-color: rgba(255, 255, 255, 0.14);
        color: #dfe6ec;

        &::placeholder {
            color: rgba(255, 255, 255, 0.4);
        }
    }

    ::v-deep .el-input__prefix,
    ::v-deep .el-input__suffix {
        color: rgba(255, 255, 255, 0.45);
    }

    &.theme-light {
        ::v-deep .el-input__inner {
            background: #f5f6f8;
            border-color: #e4e7ed;
            color: #303133;

            &::placeholder {
                color: #a8abb2;
            }
        }

        ::v-deep .el-input__prefix,
        ::v-deep .el-input__suffix {
            color: #a8abb2;
        }
    }
}

.sidebar-menu-empty {
    margin: 0;
    padding: 12px 16px;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.4);

    .theme-light & {
        color: #a8abb2;
    }
}
</style>
