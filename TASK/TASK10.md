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


# 3

增加库存管理主菜单,子菜单 工单库存余量分析页面

使用以下sql进行查询,可通过wonum,itemnum进行过滤

item.description 物料名称
```
select t1.siteid,t1.wonum,t1.LOCATION,t1.itemnum,item.DESCRIPTION,
t1.itemqty,t1.curbal,t1.RESERVEDQTY
from
(select siteid,wonum,itemnum,LOCATION,sum(ITEMQTY) itemqty,
    ( select sum(INVBALANCES.CURBAL) from INVBALANCES where INVBALANCES.ITEMNUM=WPMATERIAL.ITEMNUM and INVBALANCES.LOCATION=WPMATERIAL.LOCATION) as curbal,
    COALESCE(( select sum(INVRESERVE.RESERVEDQTY) from INVRESERVE where INVRESERVE.wonum != WPMATERIAL.wonum and INVRESERVE.siteid=WPMATERIAL.siteid and INVRESERVE.ITEMNUM=WPMATERIAL.ITEMNUM ),0) as RESERVEDQTY
    from WPMATERIAL
where exists(select 1 from WORKORDER where WORKORDER.SITEID=WPMATERIAL.SITEID and WORKORDER.WONUM=WPMATERIAL.WONUM and WORKORDER.status not in (select VALUE from SYNONYMDOMAIN where MAXVALUE in ('CAN','CLOSE','HISTEDIT') and DOMAINID='WOSTATUS'))
-- and wonum='PL202608220003'
group by siteid,wonum,itemnum,LOCATION) t1
left join item on(item.itemnum=t1.itemnum)

```

# 4

点击行后显示列表行信息,详情最下面显示以下三个标签页
```
--当前工单物料
select * from WPMATERIAL
where siteid='ISUZUSET' and LOCATION= 'VW01' and itemnum='7551837560'
  and wonum='PL202608220015'
;

--当前工单预留的数量
select *
from INVRESERVE
where siteid='ISUZUSET' and LOCATION= 'VW01' and itemnum='7551837560'
and wonum='PL202608220015'
;
--其它工单预留的数量
select *
from INVRESERVE
where siteid='ISUZUSET' and LOCATION= 'VW01' and itemnum='7551837560'
and wonum!='PL202608220015'
;

```

当前工单物料,默认只显示 WPITEMID,itemqty,ORDERUNIT,UNITCOST,LINECOST 字段, 其他字段可点击显示

当前工单预留的数量/其它工单预留的数量,默认只显示 REQUESTNUM,RESTYPE,reservedqty 字段, 其他字段可点击显示

字段标题显示中文描述