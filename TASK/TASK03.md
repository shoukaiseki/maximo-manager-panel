# 1
增加了以下接口 maximo/api/script/SKS_LOG_MARKER

传入参数marker=start的时候,同时获取startuuid参数,logger日志输出这个UUID,response返回成功,当再次接收到marker=end,同时获取enduuid参数,response返回成功, 当接收到marker=get,startuuid,enduuid参数的时候,按照_handleV8读取日志的方式,读取两个UUID之间的内容(包含UUID),并且response返回日志内容

接口返回信息参考 E:\gitwork\wushiling\jsproject\masscript\cn\shoukaiseki\tools\SKS_LOG_MARKER.js
 src\views\maximo\maslog\下面新增一个页面,复制一个 src\views\maximo\maslog\MasLogViewer.vue 的副本进行修改

 手动刷新/实时刷新 手动滚动/自动滚动 查看日志按钮删掉

 增加 "标记日志"按钮,显示开始uuid,结束uuid

 另外你有什么其它好的建议吗?


# 2
maxobject列表加上分页,列表使用 npm install sks-plugin-el-erp 组件

maxobject点击详情之后,详情不显示信息,可参考E:\gitwork\RuoYi-Vue\ruoyi-ui目录下的 src/router/index.js 和 src/router/index.js


# 3

E:\gitwork\maximo-manager-panel\src\views\maximo\maslog\MasLogMarker.vue  的 filterCustomLogs 方法增加过滤方式

项目目录src下创建一个用于配置自定义过滤的目录,创建一个过滤配置js文件,再创建一个logs目录,

logs目录创建3个文件
文件1
```
[WARNING ] SRVE8115W: WARNING: Cannot set status. Response already committed
```
文件2
```
Getting MAXPROP from maximo cache 
```
文件3
```
MAXPROP successfully fetched from maximo cache 
```

这三个文件使用endWith方式判断,行中包含的就过滤掉

还有像 c:/Users/jiang/Downloads/appdoctype.log 这个日志内容也过滤掉,帮我起个过滤方式

# 4
src\filter-config\filterConfig.js 放过滤的函数,vue页面只需要调用这边的函数,日志内容传入参数,返回过滤后的结果

帮我把 c:/Users/jiang/Downloads/appdoctype.log 文件也加到logs目录,重新起个名,也加个过滤方式


# 5
c:/Users/jiang/Downloads/ttt.log 这是页面显示的日志,没有过滤掉配置的信息啊

# 6
c:/Users/jiang/Downloads/ttt.log 这是最新页面显示的日志,没有过滤掉配置的信息啊