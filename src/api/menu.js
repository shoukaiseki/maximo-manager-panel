export const menuListJson1= `{
        "msg": "操作成功",
        "code": 200,
        "data": [

            {
                "name": "Tool",
                "path": "/tool",
                "hidden": false,
                "redirect": "noRedirect",
                "component": "Layout",
                "alwaysShow": true,
                "meta": {
                    "title": "系统工具",
                    "icon": "tool",
                    "noCache": false
                },
                "children": [
                    {
                        "name": "Build",
                        "path": "build",
                        "hidden": false,
                        "component": "tool/build/index",
                        "meta": {
                            "title": "表单构建",
                            "icon": "build",
                            "noCache": false
                        }
                    },
                    {
                        "name": "Gen",
                        "path": "gen",
                        "hidden": false,
                        "component": "tool/gen/index",
                        "meta": {
                            "title": "maximo管理面板",
                            "icon": "code",
                            "noCache": false
                        }
                    },
                    {
                        "name": "GenApp",
                        "path": "autumnApp",
                        "hidden": false,
                        "component": "tool/autumnApp/index",
                        "meta": {
                            "title": "应用配置",
                            "icon": "code",
                            "noCache": false
                        }
                    }
                ]
            }

        ]
    }`

export const menuListJson= `{
        "msg": "操作成功",
        "code": 200,
        "data": [
        ]
}`

// 获取路由
export const getRouters = () => {
    console.log("getRouters")
    return new Promise((resolve, reject) => {
        let res = JSON.parse(menuListJson);
        console.log("res",res)
        // throw new Error("")
        resolve( res )
    })
}
