# 1
src\views\maximo\MessagesQuery.vue  搜索 按照其他页面一样,不再区分查询模式,查询也调用SKS_EXPORT_MESSAGES脚本,脚本中增加分页查询功能,查询时返回中英文value值

导出的脚本 E:/gitwork/wushiling/jsproject/masscript/cn/shoukaiseki/tools/SKS_EXPORT_MESSAGES.js


# 2
增加一个表数据统计页面

src\views\maximo 下面新增一个目录

先获得要统计的所有表名
```
select
       OBJECTNAME,MAXOBJECT.DESCRIPTION,lzh.DESCRIPTION as LZH_DESCRIPTION
       from MAXOBJECT
left join L_MAXOBJECT as lzh on (lzh.OWNERID=MAXOBJECT.MAXOBJECTID and lzh.LANGCODE='ZH')
where 1=1
    and PERSISTENT=1
    and not exists(select 1 from MAXVIEW where MAXVIEW.VIEWNAME=MAXOBJECT.OBJECTNAME)
order by OBJECTNAME

```

再根据每个表名,查询表数据量,可选择排序方式,表名排序,数据行数排序(正序,倒序)