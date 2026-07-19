# 1
src\views\maximo\maxmenu\MaxMenuTree.vue el-dialog title="菜单项详情" 使用tab页切换

当前为详细信息页

其他标签页根据sql查询,根据 MENUTYPE 进行分组 APPMENU, SEARCHMENU, APPTOOL

```
select distinct SIGOPTION.DESCRIPTION as DESCRIPTIONEN,l.DESCRIPTION DESCRIPTIONZH,MAXMENU.*
from MAXMENU
left join SIGOPTION on MAXMENU.KEYVALUE=SIGOPTION.OPTIONNAME and APP=MAXMENU.MODULEAPP
left join L_SIGOPTION as l  on l.OWNERID=SIGOPTION.SIGOPTIONID and l.LANGCODE='ZH'
where MODULEAPP='PR' and MENUTYPE !='MODULE'
order by MENUTYPE,POSITION,SUBPOSITION;

```
其它标签页显示列表,列表列显示 DESCRIPTIONEN,DESCRIPTIONZH,MENUTYPE,POSITION,SUBPOSITION,ELEMENTTYPE,KEYVALUE

可进行过滤,模糊查询

点击行显示详细的所有信息

再增加个签名标签页

```
select l.DESCRIPTION,SIGOPTION.*
from SIGOPTION
         left join L_SIGOPTION as l  on l.OWNERID=SIGOPTION.SIGOPTIONID and l.LANGCODE='ZH'
where app='PR'
```

两个sql中的 PR 是树结构行中的 MODULEAPP字段的值


# 2
增加一个数据库搜索页面 src\views\maximo\querydb\querydb.vue

功能如下:
定义 模糊搜索/精确搜索 下拉框,搜索的字符

点击查询之后,后端根据要搜索的信息,查询所表的字段

获取要搜索的表和字段名,ENTITYNAME是实体表名
```
select distinct ENTITYNAME,COLUMNNAME from MAXATTRIBUTE as ma where MAXTYPE in ('ALN','UPPER') and not exists(select 1 from MAXVIEW where ma.OBJECTNAME=VIEWNAME)
order by ENTITYNAME,COLUMNNAME 
```

返回行数据类型
```
ENTITYNAME,COLUMNNAME,MAXTYPE,符合的结果条数,查询时的语句,耗时
```
还需返回总耗时

返回使用sse流方式返回,前端显示进度条和最后一次接收到的行信息


# 3
显示的
```
{
  "msgGroup": "system",
  "msgKey": "NotSupported",
  "value": "不支持对{0}（数据类型 ={1}、值 ={2}、对象名称 ={3}、属性名称 ={4}）进行操作。 请向系统管理员报告该错误。",
  "displayMethod": "MSGBOX",
  "options": [
    "ok"
  ],
  "title": null,
  "buttonText": null,
  "explanation": "由于无法在指定的数据类型上执行操作，因此可能发生了定制编程错误。 例如，尝试从十进制字段检索日期。",
  "adminResponse": "请检查字段是否已正确配置。 操作必须与数据类型匹配。",
  "operatorResponse": null,
  "systemAction": null,
  "response": "请检查字段是否已正确配置。 操作必须与数据类型匹配。",
  "msgId": "BMXAA7816E",
  "ok": null,
  "yes": null,
  "no": null,
  "cancel": null,
  "close": true,
  "stop": true,
  "warning": null,
  "exclamation": null,
  "prefix": "Y",
  "msgIdPrefix": "BMXAA",
  "msgIdSuffix": "E"
}
```
接口返回的
```
{
    "messages": [
        {
            "msgGroup": "system",
            "msgKey": "NotSupported",
            "value": "不支持对{0}（数据类型 ={1}、值 ={2}、对象名称 ={3}、属性名称 ={4}）进行操作。 请向系统管理员报告该错误。",
            "displayMethod": "MSGBOX",
            "options": [
                "ok"
            ],
            "sks:options": 65,
            "explanation": "由于无法在指定的数据类型上执行操作，因此可能发生了定制编程错误。 例如，尝试从十进制字段检索日期。",
            "adminResponse": "请检查字段是否已正确配置。 操作必须与数据类型匹配。",
            "response": "请检查字段是否已正确配置。 操作必须与数据类型匹配。",
            "msgId": "BMXAA7816E",
            "close": true,
            "stop": true,
            "prefix": "Y",
            "msgIdPrefix": "BMXAA",
            "msgIdSuffix": "E"
        }
    ]
}
```