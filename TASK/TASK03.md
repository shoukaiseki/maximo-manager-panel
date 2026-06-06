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
