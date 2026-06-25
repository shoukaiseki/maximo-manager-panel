import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
import Layout from '/src/layout'

/**
 * Note: 路由配置项
 *
 * hidden: true                   // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true               // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect           // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'             // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * meta : {
    noCache: true                // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'               // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'             // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false            // 如果设置为false，则不会在breadcrumb面包屑中显示
  }
 */


// 公共路由
export const constantRoutes = [
    {
        path: '/redirect',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '/redirect/:path(.*)',
                component: (resolve) => require(['@/views/redirect'], resolve)
            }
        ]
    },
    {
        path: '/login',
        component: (resolve) => require(['@/views/login'], resolve),
        hidden: true
    },
    {
        path: '/404',
        component: (resolve) => require(['@/views/error/404'], resolve),
        hidden: true
    },
    {
        path: '/401',
        component: (resolve) => require(['@/views/error/401'], resolve),
        hidden: true
    },
    {
        path: '',
        component: Layout,
        redirect: 'index',
        children: [
            {
                path: 'index',
                component: (resolve) => require(['/src/views/index'], resolve),
                name: '首页',
                meta: { title: '首页', icon: 'dashboard', noCache: false, affix: true }
            },
        ]
    },
     {
        path: '/sks-components',
        component: Layout,
        name: 'SksComponents',
        meta: { title: '组件示例', icon: '' ,noCache: false},
        hidden: false,
        children: [
            {
                path: 'index',
                component: (resolve) => require(['/src/views/components/index'], resolve),
                name: 'sksComponentIconsIndex',
                meta: { title: '组件首页', icon: '' ,noCache: false}
            },
            {
                path: 'icons',
                component: (resolve) => require(['/src/views/components/icons/index'], resolve),
                name: 'WangbaoComponentsIndex',
                meta: { title: '图标', icon: '' ,noCache: false}
            },
            {
                path: 'echarts/bar',
                component: (resolve) => require(['/src/views/components/echarts/bar/index'], resolve),
                name: 'WangbaoComponentsEchartsBar',
                meta: { title: '柱状图', icon: '' ,noCache: false},
            },
        ]
    },
    {
        path: '/maxsys',
        name: "maxsys",
        component: Layout,
        hidden: false,
        redirect: "noRedirect",
        alwaysShow: true,
        meta: {
            "title": "maximo系统工具",
            "icon": "tool",
            "noCache": false
        },
        children: [
            {
                path: 'messages',
                component: (resolve) => require(['/src/views/maximo/MessagesQuery'], resolve),
                name: 'MessagesQuery',
                meta: { title: '消息查询', icon: 'message', noCache: false }
            },
            {
                path: 'maxscript',
                component: (resolve) => require(['/src/views/maximo/maxscript/AutoScriptQuery'], resolve),
                name: 'AutoScriptQuery',
                meta: { title: '脚本查询', icon: 'code', noCache: false }
            },
            {
                path: 'maxmenu',
                component: (resolve) => require(['/src/views/maximo/maxmenu/MaxMenuTree'], resolve),
                name: 'MaxMenuTree',
                meta: { title: '菜单管理', icon: 'tree', noCache: false }
            },
            {
                path: 'maxsession',
                component: (resolve) => require(['/src/views/maximo/maxsession/MaxSession'], resolve),
                name: 'MaxSession',
                meta: { title: '在线用户', icon: 'user', noCache: false }
            }
        ]
    },
    {
        path: '/maslog',
        name: "maslog",
        component: Layout,
        hidden: false,
        redirect: "noRedirect",
        alwaysShow: true,
        meta: {
            "title": "maximo日志",
            "icon": "log",
            "noCache": false
        },
        children: [
            {
                path: 'masLogViewer',
                component: (resolve) => require(['/src/views/maximo/maslog/MasLogViewer'], resolve),
                name: 'MasLogViewer',
                meta: { title: '日志查询', icon: 'log', noCache: false }
            },
            {
                path: 'masLogMarker',
                component: (resolve) => require(['/src/views/maximo/maslog/MasLogMarker'], resolve),
                name: 'MasLogMarker',
                meta: { title: '日志标记', icon: 'mark', noCache: false }
            }
        ]
    },
    {
        path: '/maxobject',
        name: "maxobject",
        component: Layout,
        hidden: false,
        redirect: "noRedirect",
        alwaysShow: true,
        meta: {
            "title": "MaxObject",
            "icon": "table",
            "noCache": false
        },
        children: [
            {
                path: 'index',
                component: (resolve) => require(['/src/views/maximo/maxobject/MaxObjectList'], resolve),
                name: 'MaxObjectList',
                meta: { title: '对象列表', icon: 'list', noCache: false }
            }
        ]
    },
    {
        path: '/maxobject-detail',
        component: Layout,
        hidden: true,
        children: [
            {
                path: 'index/:objectname',
                component: (resolve) => require(['/src/views/maximo/maxobject/MaxObjectDetail'], resolve),
                name: 'MaxObjectDetail',
                meta: { title: '对象详情', activeMenu: '/maxobject', noCache: false }
            }
        ]
    },
]

export default new Router({
    mode: 'history', // 去掉url中的#
    scrollBehavior: () => ({ y: 0 }),
    routes: constantRoutes
})