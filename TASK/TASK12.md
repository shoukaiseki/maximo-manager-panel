# 序列任务
新增一个功能,序列查询和诊断,
菜单放到 maximo系统工具下
前端在 src\views\maximo 目录下新增一个子目录存放
使用server下的服务
查询序列的语句
```
select * from MAXSEQUENCE

```

表中有以下字段,列表默认显示 MAXSEQUENCEID,TBNAME,NAME,SEQUENCENAME
点击显示所有字段的时候显示其它字段

```json
{
  "maxObjects": [
    {
      "object": "MAXSEQUENCE",
      "description": "记录唯一标识序列预留。",
      "level": "SYSTEM",
      "service": "SYSTEM",
      "className": "psdi.app.system.MaxSequenceSet",
      "langCode": "EN",
      "triggerRoot": "MAXSEQUENCE",
      "storagePartition": "MAXDATA",
      "unqiueColumn": "MAXSEQUENCEID",
      "mainObject": false,
      "internal": true,
      "attributes": [
        {
          "attribute": "MAXRESERVED",
          "description": "序列中上次预留的标识",
          "title": "最大预留值",
          "type": "BIGINT",
          "length": 19,
          "scale": 0,
          "required": true,
          "mustBe": true,
          "defaultValue": "1",
          "restricted": true,
          "userDefined": false,
          "searchType": "EXACT"
        },
        {
          "attribute": "MAXSEQUENCEID",
          "description": "唯一标识列",
          "title": "Maxsequence 标识",
          "type": "BIGINT",
          "length": 19,
          "scale": 0,
          "required": true,
          "mustBe": true,
          "restricted": true,
          "userDefined": false,
          "searchType": "EXACT"
        },
        {
          "attribute": "MAXVALUE",
          "description": "序列中的最大值",
          "title": "最大值",
          "type": "BIGINT",
          "length": 19,
          "scale": 0,
          "restricted": true,
          "userDefined": false,
          "searchType": "EXACT"
        },
        {
          "attribute": "NAME",
          "description": "用于预留序列的列名称",
          "title": "列",
          "type": "UPPER",
          "sameAsObject": "MAXOBJECT",
          "sameAsAttribute": "OBJECTNAME",
          "length": 30,
          "scale": 0,
          "primaryColumn": 2,
          "required": true,
          "mustBe": true,
          "userDefined": false,
          "searchType": "EXACT"
        },
        {
          "attribute": "RANGE",
          "description": "序列范围",
          "title": "序列范围",
          "type": "BIGINT",
          "length": 19,
          "scale": 0,
          "restricted": true,
          "userDefined": false,
          "searchType": "EXACT"
        },
        {
          "attribute": "SEQUENCENAME",
          "description": "序列的名称。",
          "title": "序号",
          "type": "UPPER",
          "sameAsObject": "MAXOBJECT",
          "sameAsAttribute": "OBJECTNAME",
          "length": 30,
          "scale": 0,
          "required": true,
          "mustBe": true,
          "userDefined": false,
          "searchType": "NONE"
        },
        {
          "attribute": "TBNAME",
          "description": "用于预留序列的表名称",
          "title": "表",
          "type": "UPPER",
          "sameAsObject": "MAXOBJECT",
          "sameAsAttribute": "OBJECTNAME",
          "length": 30,
          "scale": 0,
          "primaryColumn": 1,
          "required": true,
          "mustBe": true,
          "userDefined": false,
          "searchType": "EXACT"
        }
      ]
    }
  ]
}
```

设 全部诊断/诊断 按钮,全部诊断按照当前过滤条件诊断所有数据,诊断按钮需要勾选后才能点击

诊断前端逻辑,诊断情况下,主表使用tab标签页显示 主表,诊断两个标签,诊断标签中使用缓存搜索,可选择 诊断表显示在主表基础上增加显示 "记录最大值","序列当前值","状态"三列(属于虚拟字段,以V_开头),状态设定 NORMAL/ABNORMAL,使用tag显示中文,可进行缓存过滤筛选


表列最大值
```
--COMPANIESID,COMPANIES 是 MAXSEQUENCE表中的 NAME 和 TBNAME的值
select max(COMPANIESID) from COMPANIES;
```

序列当前值是 MAXSEQUENCE表中, SEQUENCENAME字段存的是数据库序列名


因为有些数据不是使用序列新增到表中的,所以会出现记录最大值与大于当前序列值


注意: 按照 开发文档.md 中的要求开发