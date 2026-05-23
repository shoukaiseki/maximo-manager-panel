import { login, logout, getInfo } from '/src/api/login'
import { getToken, setToken, removeToken } from '/src/utils/auth'
import { aqoeous } from '/src/api/base/aqoeous'
import { dispose } from 'echarts/lib/echarts'

const user = {
    state: {
        token: getToken(),
        name: '',
        avatar: '',
        roles: [],
        permissions: [],
        userId:'',
        //全局用户列表,只为了显示userId对应的用户昵称
        userList:{},
        locationUuidList:{},
        locationUuidListLock: false,
    },

    mutations: {
        SET_TOKEN: (state, token) => {
            state.token = token
        },
        SET_NAME: (state, name) => {
            state.name = name
        },
        SET_AVATAR: (state, avatar) => {
            state.avatar = avatar
        },
        SET_ROLES: (state, roles) => {
            state.roles = roles
        },
        SET_PERMISSIONS: (state, permissions) => {
            state.permissions = permissions
        },
        //sks增加
        SET_USERID:(state, userId) => {
            state.userId = userId
        },
        //sks
        SET_USER_LIST:(state,user)=>{
            if(user&&user.userId!=undefined){
                state.userList[""+user.userId]=user
            }
            // console.log("state.userList",state.userList,user)
        },
        SET_LOCATION_UUID_LIST:(state,location)=>{
            if(location&&location.location!=undefined){
                state.locationUuidList[""+location.location]=location

            }

        }
    },

    actions: {
        // GetLocationByUUID({commit},uuid){
        //     const that=this
        //     return new Promise((resolve, reject) => {
        //         if(this.state.user.locationUuidListLock){
        //             setTimeout(()=>{
        //                 that.dispatch('baseGetLocationByUUID',uuid)
        //                 that.baseGetLocationByUUID({commit},uuid).then(res=>{
        //                      resolve(res)
        //                  }).catch(error=>{
        //                      reject(error)
        //                  })
        //             },1000)
        //             return
        //         }
        //         that.baseGetLocationByUUID({commit},uuid).then(res=>{
        //             resolve(res)
        //         }).catch(error=>{
        //             reject(error)
        //         })
        //     })
        // },
        // baseGetLocationByUUID({commit},uuid){
        GetLocationByUUID({commit},uuid){
            let locationUuidList = this.state.user.locationUuidList
            this.state.user.locationUuidListLock=true
            // console.log("this.state.user.locationUuidListTotal",this.state.user.locationUuidListLock)
            let row = locationUuidList[uuid]
            if(row){
                return new Promise((resolve, reject) => {
                    resolve(row)
                })
            }
            // console.log("GetSysUser.userList",userList)
            return new Promise((resolve, reject) => {
                const params={
                    exactQueryLocationList:[uuid],
                    pageNum:1,
                    pageSize:1,
                }
                const queryObjectList=[
                    {
                        resultName: "mainList",
                        qoName: 'locationQO',
                        mergeParent : true,
                        listFirstMergeParent : true,
                        listMergeParent : false,
                        qoJson:params,
                        includeFieldList:['locationId','location','locationLabel'],
                    }
                ]
                aqoeous(queryObjectList).then(response=>{
                    let  resData= response.data.mainList
                    if(resData){
                        commit('SET_LOCATION_UUID_LIST', resData)
                    }
                    this.state.user.locationUuidListLock=false
                    resolve(resData)
                }).catch(error => {
                    this.state.user.locationUuidListLock=false
                    reject(error)
                })
            })
        },
        GetSysUser({commit},userId){
            let userList = this.state.user.userList
            let row = userList[userId+""]
            if(row){
                return new Promise((resolve, reject) => {
                    resolve(row)
                })
            }
            // console.log("GetSysUser.userList",userList)
            return new Promise((resolve, reject) => {
                const params={
                    exactQueryUniqueIdList:[userId],
                    pageNum:1,
                    pageSize:1,
                }
                const queryObjectList=[
                    {
                        resultName: "mainList",
                        qoName: 'sysUserQO',
                        mergeParent : true,
                        listFirstMergeParent : true,
                        listMergeParent : false,
                        qoJson:params,
                    }
                ]
                aqoeous(queryObjectList).then(response=>{
                    let  resData= response.data.mainList
                    if(resData){
                        commit('SET_USER_LIST', resData)
                    }
                    resolve(resData)
                }).catch(error => {
                    reject(error)
                })
            })
        },
        // 登录
        Login({ commit }, userInfo) {
            return new Promise((resolve, reject) => {
                let token = 'shoukaiseki'
                setToken(token)
                commit('SET_TOKEN', token)
                resolve()
            })
        },

        // 获取用户信息
        GetInfo({ commit, state }) {
            return new Promise((resolve, reject) => {
                getInfo(state.token).then(res => {
                    const user = res.user
                    const avatar = user.avatar == "" ? require("@/assets/images/profile.jpg") : process.env.VUE_APP_BASE_API + user.avatar;
                    if (res.roles && res.roles.length > 0) { // 验证返回的roles是否是一个非空数组
                        commit('SET_ROLES', res.roles)
                        commit('SET_PERMISSIONS', res.permissions)
                    } else {
                        commit('SET_ROLES', ['ROLE_DEFAULT'])
                    }
                    commit('SET_NAME', user.userName)
                    commit('SET_AVATAR', avatar)
                    //sks增加
                    commit('SET_USERID',user.userId)
                    commit('SET_USER_LIST',{})
                    resolve(res)
                }).catch(error => {
                    reject(error)
                })
            })
        },
        // 退出系统
        LogOut({ commit, state }) {
            return new Promise((resolve, reject) => {
                logout(state.token).then(() => {
                    commit('SET_TOKEN', '')
                    commit('SET_ROLES', [])
                    commit('SET_PERMISSIONS', [])
                    //sks
                    commit('SET_USERID','')
                    //sks
                    commit('SET_USER_LIST',{})
                    removeToken()
                    resolve()
                }).catch(error => {
                    reject(error)
                })
            })
        },

        // 前端 登出
        FedLogOut({ commit }) {
            return new Promise(resolve => {
                commit('SET_TOKEN', '')
                removeToken()
                resolve()
            })
        }
    }
}

export default user
