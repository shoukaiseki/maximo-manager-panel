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