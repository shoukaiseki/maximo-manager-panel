# 1
增加一个 MessagesQuery 页面, 用于查询消息
参考 E:\gitwork\maximo-manager-panel_bak\src\views\MessagesQuery.vue  

顶部增加下拉选择框,和git图标,放在右上角

参考 E:\gitwork\maximo-manager-panel_bak\src\App.vue


**注意** 当前布局不要改, E:\gitwork\maximo-manager-panel_bak的布局很丑,所以使用该项目的布局进行创建项目,然后修改


# 2
后端地址配置和MAXAUTH和apiKey 配置使用本项目一个json文件,支持配置多套方案

前端: 全局页面布局 顶部布局最右边选择后端环境(下拉选择框已经存在),可以切换不同环境

json文件已放置 server/backend.config.json

可参考 E:\gitwork\maximo-manager-panel_bak 项目

# 3
为 server/backend.config.json 创建一个 backend.config.example.json 文件, 用于展示配置文件的格式

git提交时候忽略 server/backend.config.json 文件