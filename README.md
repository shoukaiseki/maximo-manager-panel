### 权限
```js
//是否拥有库管员权限
this.canInventoryAdmin=this.checkPermi(['inventory:InventoryBalance:admin'])||this.isAdmin;
```

### 获取当前用户
```
const userId = this.$store.state.user.userId;
```
## 注意
如果 npm install 安装失败  使用 webstorm 打开,点击提示的 npm install安装即可


执行的命令是
```
"C:\Program Files\nodejs\node.exe" "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" install --scripts-prepend-node-path=auto
```
## 开发
npm install xe-utils
npm install  --save  vxe-table@3.1.8


```bash
# 克隆项目
git clone https://gitee.com/y_project/RuoYi-Vue

# 进入项目目录
cd ruoyi-ui

# 安装依赖
npm install

# 建议不要直接使用 cnpm 安装依赖，会有各种诡异的 bug。可以通过如下操作解决 npm 下载速度慢的问题
npm install --registry=https://registry.npm.taobao.org

# 启动服务
npm run dev
```

浏览器访问 http://localhost:80

## 发布

```bash
# 构建测试环境
npm run build:stage

# 构建生产环境
npm run build:prod
```