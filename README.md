# Maximo Management Panel

基于 Vue 2 + Element UI 的 Maximo 管理面板，配套 Solon + Java 17 后端。用于 Maximo 对象结构浏览、菜单管理查看、自动化脚本诊断、实时日志监控等运维场景。

## 技术栈

**前端**
- Vue 2.6 + Vue Router + Vuex
- Element UI 2.14
- VXE Table 3.x
- Monaco Editor（脚本查看）
- sks-plugin-el-erp（私有组件库）

**后端** (`server/`)
- Solon 2.9.4 + Java 17 + Maven
- DB2 数据库（JDBC）
- HikariCP 连接池
- API Key 认证（X-API-Key）

## 功能模块

| 模块 | 页面 | 说明 |
|------|------|------|
| 对象管理 | MaxObjectList / MaxObjectDetail | Maximo 对象列表（分页搜索）、详情（属性/关系/索引/域值） |
| 菜单管理 | MaxMenuTree | Maximo 菜单树形结构，模块→分组→应用/操作，中英文描述 |
| 自动化脚本 | AutoScriptQuery | AutoScript 查询诊断，脚本内容查看与变量分析 |
| 日志查看 | MasLogViewer | 实时日志查看（SSE 流式推送），支持高亮过滤 |
| 日志标记 | MasLogMarker | 日志关键词标记与管理 |
| 消息查询 | MessagesQuery | Maximo 消息查询 |

## 快速开始

### 后端启动

```bash
cd server
mvn package -DskipTests
java -jar target/solon-server-1.0.0.jar
```

后端默认启动在 `http://localhost:8081`。

也可使用快捷脚本：
```bash
bin/run-solon-server.bat        # 生产模式
bin/run-solon-server-dev.bat    # 开发模式
```

### 前端开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端默认启动在 `http://localhost:28765`，自动代理 `/maximo` 和 `/solonapi` 到后端。

### 构建

```bash
# 测试环境
npm run build:stage

# 生产环境
npm run build:prod
```

构建产物输出到 `dist/` 目录。

## 项目结构

```
├── bin/                        # 构建/运行脚本
├── server/                     # Solon Java 后端
│   ├── src/main/java/com/sks/server/
│   │   ├── controller/         # API 控制器
│   │   │   ├── MaxObjectController.java    # 对象管理
│   │   │   ├── MaxMenuController.java      # 菜单管理
│   │   │   └── AutoScriptController.java   # 自动化脚本
│   │   ├── service/            # 业务逻辑
│   │   │   ├── MaxObjectService.java
│   │   │   ├── MaxMenuService.java
│   │   │   └── AutoScriptService.java
│   │   └── model/              # 数据模型（RestResult 等）
│   ├── src/main/resources/     # 配置文件
│   ├── backend.config.json     # 多环境配置（不入库）
│   └── pom.xml
├── src/                        # Vue 前端
│   ├── api/                    # API 接口封装
│   │   ├── maxobject.js        # 对象管理 API
│   │   ├── maxmenu.js          # 菜单管理 API
│   │   ├── autoscript.js       # 自动化脚本 API
│   │   └── solonapi.js         # 通用请求封装
│   ├── views/maximo/           # 业务页面
│   │   ├── maxobject/          # 对象管理（列表+详情）
│   │   ├── maxmenu/            # 菜单管理（树形表格）
│   │   ├── maxscript/          # 自动化脚本查询
│   │   └── maslog/             # 日志查看与标记
│   ├── router/                 # 路由配置
│   └── store/                  # Vuex 状态管理
├── AIDOC/                      # 功能文档
└── vue.config.js               # 前端代理配置
```

## 后端配置

后端配置文件位于 `server/backend.config.json`，支持配置多个后端环境、API Key、DB2 连接等。

**注意**: `backend.config.json` 已加入 `.gitignore`，提交时不会被包含。
可参考 `backend.config.example.json` 的格式创建自己的配置。

## API 接口

所有 API 请求需携带 `X-API-Key` 请求头进行认证。

### 对象管理

| 接口 | 说明 |
|------|------|
| `GET /maxobject/list?keyword=&pageNum=&pageSize=` | 对象列表（分页） |
| `GET /maxobject/{objectName}/detail` | 对象详情（基本信息+属性+关系+索引） |

### 菜单管理

| 接口 | 说明 |
|------|------|
| `GET /maxmenu/modules?sortBy=zh` | 所有模块列表（支持 zh/en/module 排序） |
| `GET /maxmenu/fullTree?sortBy=zh` | 完整菜单树（所有模块+菜单项） |
| `GET /maxmenu/moduleTree?module=ASSET` | 指定模块的菜单树 |
| `GET /maxmenu/appMenu?app=ASSET` | 应用菜单（APPMENU） |
| `GET /maxmenu/appTool?app=ASSET` | 应用工具栏（APPTOOL） |
| `GET /maxmenu/search?keyword=&menuType=&elementType=` | 搜索菜单 |

### 自动化脚本

| 接口 | 说明 |
|------|------|
| `GET /autoscript/list?keyword=&pageNum=&pageSize=` | 脚本列表（分页） |
| `GET /autoscript/{scriptId}/detail` | 脚本详情（含变量） |

### 日志

| 接口 | 说明 |
|------|------|
| `GET /maximo/log/{server}` | 读取 Maximo 日志（SSE 流式推送） |

## 许可证

MIT