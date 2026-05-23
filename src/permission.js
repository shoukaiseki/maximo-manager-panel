import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '/src/utils/auth'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/auth-redirect', '/bind', '/register','/h5show','/anonProductionOrder']

const regWhiteList= [/^\/h5show\/\d+$/,/\/anonProductionOrder/];

router.beforeEach((to, from, next) => {
    console.log("to.path=",to.path)
    NProgress.start()
    if (getToken()) {
        /* has token*/
        if (to.path === '/login') {
            next({ path: '/' })
            NProgress.done()
        } else {
            if (store.getters.roles.length === 0) {
                // 判断当前用户是否已拉取完user_info信息
                // 拉取user_info
                store.dispatch('GetInfo').then(res => {
                    const roles = res.roles
                    store.dispatch('GenerateRoutes', {roles}).then(accessRoutes => {
                        console.log("accessRoutes",accessRoutes)
                        // 测试 默认静态页面
                        // store.dispatch('permission/generateRoutes', { roles }).then(accessRoutes => {
                        // })
                        // 根据roles权限生成可访问的路由表
                        router.addRoutes(accessRoutes) // 动态添加可访问路由表
                        next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
                    })
                })
            } else {
                next()
                // 没有动态改变权限的需求可直接next() 删除下方权限判断 ↓
                // if (hasPermission(store.getters.roles, to.meta.roles)) {
                //   next()
                // } else {
                //   next({ path: '/401', replace: true, query: { noGoBack: true }})
                // }
                // 可删 ↑
            }
        }
    } else {
        var regSuccess=false;
        // console.log("正则匹配,path=",to.path)
        for (let i = 0; i < regWhiteList.length; i++) {
            if (to.path.match(regWhiteList[i])) {
                regSuccess=true;
                // console.log("匹配成功,path=",to.path)
                break;
            }

        }
        // 没有token
        if (whiteList.indexOf(to.path) !== -1||regSuccess) {
            // 在免登录白名单，直接进入
            //正则匹配白名单数组有一匹配成功,直接进入
            next()
        } else {
            next(`/login?redirect=${to.fullPath}`) // 否则全部重定向到登录页
            NProgress.done()
        }
    }
})

router.afterEach(() => {
    NProgress.done()
})
