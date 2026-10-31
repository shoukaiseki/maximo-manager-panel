logs\1.log 导入成功了,但显示失败,

导入成功后返回内容增加一个sks:ref属性,设置目前的_id值,_id应该是mbo所属主键,改成sks:uniqueID,使用mbo.getUniqueIDValue()获取,增加:uniqueIDName字段,使用 mbo.getUniqueIDName() 获取

导入返回的内容增加 

```
sks:successTotal            成功导入的对象总数,当前所属对象成功总和
sks:failTotal               失败导入的对象总数,当前所属对象失败总和
sks:successSumTotal         成功导入的对象总数的总和,递归successSumTotal总和
sks:failSumTotal            失败导入的对象总数的总和,递归failSumTotal总和
sks:objectName              导入的对象名称
sks:uuid                    导入的对象uuid
sks:currentCacheUseError      当前缓存使用错误信息,失败时才设置,参考 E:/gitwork/wushiling/jsproject/masscript/sharptree/autoscript/SKS.AUTOSCRIPT.LIBRARY.js中的使用方式
```

成功显示优化下,多标签页显示信息,默认列表显示各对象的导入结果,树结构标签页展示各对象的导入结果


导出时候每个对象增加一个 sks:uuid属性,随机生成一个uuid,编译导入失败时排查


public\javaapisource\jscustom\SksMigrateParamConfig.ts 中增加一个 _errorAssert 属性