# 增加脚本查询诊断页面
创建maxscript目录,放到该目录下


```
-- 自动化脚本主表 与子表通过AUTOSCRIPT字段关联
select * from AUTOSCRIPT;

-- 启动点 与启动点变量通过AUTOSCRIPT和LAUNCHPOINTNAME字段关联,一个AUTOSCRIPT记录会有多个
select * from SCRIPTLAUNCHPOINT ;

-- 自动化脚本变量,一个AUTOSCRIPT记录会有多个
select * from AUTOSCRIPTVARS ;

-- 启动点变量 一个SCRIPTLAUNCHPOINT记录会有多个
select * from LAUNCHPOINTVARS ;


```
可查询字段 AUTOSCRIPT 脚本名称,DESCRIPTION 脚本描述, OBJECTNAME 对象名,ATTRIBUTENAME 字段名,LAUNCHPOINTNAME 启动点名称, source 脚本内容, 查询模式/诊断模式 切换开关,默认诊断模式

## 查询模式
如果 OBJECTNAME,ATTRIBUTENAME精确查找,LAUNCHPOINTNAME,AUTOSCRIPT ,DESCRIPTION,source 模糊查找,都匹配的才显示

## 诊断模式
OBJECTNAME 有值,则只要启动点OBJECTNAME有相同值的数据都显示,或者 脚本名为 <OBJECTNAME>.NEW 的也显示
OBJECTNAME,ATTRIBUTENAME 都有值,只要启动点OBJECTNAME有相同值的数据都显示,或者 脚本名为 <OBJECTNAME>.NEW 的也显示,或者source包含ATTRIBUTENAME(不区分大小写) 的也显示
诊断模式下,显示增加一个虚拟字段,标识那种匹配模式下匹配的(可以有多个)

诊断模式可以进行多次查询,多次查询相同的只显示一个,以AUTOSCRIPT为显示主表


## 页面设计

列表使用分页,使用 pagination 组件翻页,列表可以使用card,或者el-table,列表需要同时显示子表的表格信息 SCRIPTLAUNCHPOINT,AUTOSCRIPTVARS,LAUNCHPOINTVARS
```
       <pagination v-show="tablePatam.total > 0"
            :total="tablePatam.total"
            :page.sync="tablePatam.pageNum"
            :limit.sync="tablePatam.pageSize"
            @pagination="getList"
        />
```

列表上可以显示一部分字段,但是得一个功能或者按钮,能够查看所有字段信息(AUTOSCRIPT.SOURCE除外),AUTOSCRIPT.SOURCE通过一个按钮点击弹出窗口查看详细源代码内容,源码使用Monaco Editor 展示,通过AUTOSCRIPT.SCRIPTLANGUAGE内容展示js/python高亮

AUTOSCRIPT.SCRIPTLANGUAGE对应的脚本类型
```
           const extMap: Record<string, string> = {
              'javascript': 'js',
              'js': 'js',
              'python': 'py',
              'jython': 'py',
              'py': 'py',
              'nashorn': 'js',
              'ecmascript': 'js',
              'Nashorn': 'js',
              'MBR': 'py',
              'JavaScript': 'js',
              'JS': 'js',
              'ECMAScript': 'js'
            };
```


页面上要显示中文 scriptlaunchpoint.OBJECTEVENT 含义

```
- LAUNCHPOINTTYPE=OBJECT
  eventtype 由位标志决定:
  - eventtype=0 "初始化值": 位2/4/8/16/32/64/128/256/512中任意组合
  - eventtype=1 "验证应用程序": 位1024
  - eventtype=2 "允许创建对象": 位2048
  - eventtype=3 "允许删除对象": 位4096
  - eventtype=4 "保存": 位2/4/8/16/32/64/128/256/512中任意组合（非1024/2048/4096时）

  evcontext 由位标志决定:
  - evcontext=0 "保存前": 位2/4/8（无PostCommit和PostSave标志）
  - evcontext=1 "保存后": 位16/32/64（有PostSave，无PostCommit）
  - evcontext=2 "落实后": 位128/256/512（有PostCommit）

  位标志:
    - OBJECTEVENT&1==1			onMboInit				初始化值（Mbo初始化）
    - OBJECTEVENT&2==2			onMboAdd				新增（保存前）
    - OBJECTEVENT&4==4			onMboUpdate				修改（保存前）
    - OBJECTEVENT&8==8			onMboDelete				删除（保存前）
    - OBJECTEVENT&16==16		onMboAddPostSave		新增（保存后）
    - OBJECTEVENT&32==32		onMboUpdatePostSave		修改（保存后）
    - OBJECTEVENT&64==64		onMboDeletePostSave		删除（保存后）
    - OBJECTEVENT&128==128		onMboAddPostCommit		新增（落实后）
    - OBJECTEVENT&256==256		onMboUpdatePostCommit	修改（落实后）
    - OBJECTEVENT&512==512		onMboDeletePostCommit	删除（落实后）
    - OBJECTEVENT&1024==1024	onMboAppValidate		验证应用程序
    - OBJECTEVENT&2048==2048	onMboCanAdd				允许创建对象
    - OBJECTEVENT&4096==4096	onMboCanDelete			允许删除对象

- LAUNCHPOINTTYPE=ATTRIBUTE
  attributeevent 由位标志决定:
  - attributeevent=0 "初始化访问限制": 位8
  - attributeevent=1 "初始化值": 位2
  - attributeevent=2 "验证": 无位设置（默认值）
  - attributeevent=3 "检索列表": 位64
  - attributeevent=4 "运行操作": 位1

  位标志:
    - OBJECTEVENT==0			onMVAValidate	验证（不是位检查，而是全0）
    - OBJECTEVENT&1==1			onMVAAction		运行操作
    - OBJECTEVENT&2==2			onMVAInit		初始化值
    - OBJECTEVENT&8==8			onMVAInitAR		初始化访问限制
    - OBJECTEVENT&64==64		onMTDList		检索列表

```


参考maximo页面 E:\devwork\ideawork\maximo91_soloncode\xmltmp\dev\autoscript.xml
主列表需要显示 tab id="main" label="自动化脚本"  字段的所有信息,以及属性启动点,对象启动点对话框的所有信息,tab id="variables" label="变量" tab id="scrlaunchpoint" label="启动点" 的也都要显示


# 菜单管理页面
新增maxmenu目录,增加菜单管理页面,使用树结构展示 一级菜单,二级菜单,三级菜单等,菜单下面应用也也要显示,应用下面操作(menu.ELEMENTTYPE 字段的 'APP', 'HEADER', 'MODULE', 'OPTION', 'SEP')都得显示


逻辑参考 ai_repository_maximo\maximo菜单查询与Vue编辑器设计.md