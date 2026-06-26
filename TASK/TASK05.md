# 1
帮我增加个页面,maxsession,查询按钮可以查询在线的用户

获取在线的用户
```
curl --request POST \
  --url 'api/script/SKS_MXSESSION_REMOVE?sessionManager=full&_langcode=ZH' \
  --header 'Content-Type: application/json' 
```
返回信息如下
```
{
	"status": "success",
	"action": "get",
	"totalLiveSessions": 0,
	"onlineUsers": [
		"MAXZHCN",
		"MAXADMIN"
	]
}
```


可以多选用户,点击踢出用户
```
curl --request POST \
  --url 'api/script/SKS_MXSESSION_REMOVE?sessionManager=full&_langcode=ZH&userId=MAXADMIN  ' \
  --header 'Accept: */*' 
```

踢出用户循环调用选择的用户,

页面下面加个monaco-editor, 显示调用接口返回的日志



# 2
增加一个页面,搜索字段列表

字段列表参考 src\views\maximo\maxobject\MaxObjectDetailColumn.vue 
增加分页,搜索功能,搜索根据OBJECTNAME,字段名,字段描述进行搜索

根据对象名称,字段名搜索时候,如果搜索时候开始字符为 "=",则进行精确查找 
查找时候支持通配符,如 %ITEM%

列表最左侧增加按钮,复制字段json到剪切板

##  复制字段json到剪切板功能
搞两个按钮,精简json和完整json
参考导出对象接口 E:\gitwork\wushiling\jsproject\masscript\cn\shoukaiseki\tools\SKS_EXPORT_DBCONFIG.js

# 3

你这写法不对啊,你还是根据接口获取包含对象的json吧

```
curl --request POST \
  --url 'api/script/SKS_EXPORT_DBCONFIG?_langcode=zh&ignoreDefVal=true' \
  --header 'Accept: */*' \

  --data '{
	"objectNames": ["ACCOUNTDEFAULTS"]
	
}'
```

完整的去掉 ignoreDefVal=true参数,
objectNames 传入行中的的对象名

返回的格式如下
```
{
	"maxObjects": [
		{
			"level": "ORG",
			"service": "FINANCIAL",
			"langCode": "EN",
			"description": "帐户缺省表",
			"mainObject": false,
			"className": "psdi.app.financial.AccountDefaultsSet",
			"attributes": [
				{
					"searchType": "WILDCARD",
					"defaultValue": null,
					"userDefined": false,
					"length": 20,
					"description": "组类型",
					"scale": 0,
					"mustBe": true,
					"positive": false,
					"title": "组类型",
					"type": "UPPER",
					"required": true,
					"primaryColumn": 2,
					"domain": null,
					"attribute": "DFLTGROUP"
				},
				{
					"searchType": "WILDCARD",
					"defaultValue": null,
					"userDefined": false,
					"length": 110,
					"description": "先前的缺省 GL",
					"scale": 0,
					"mustBe": true,
					"positive": false,
					"title": "原来的缺省 GL ",
					"type": "GL",
					"required": false,
					"domain": null,
					"attribute": "OLDGLDEFAULT"
				}
			],
			"object": "ACCOUNTDEFAULTS"
		}
	]
}
```
查找attributes中attribute与行中 attributeName一致的json字符串展示

