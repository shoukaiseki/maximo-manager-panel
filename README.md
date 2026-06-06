# Maximo Management Panel

基于 Vue 2 + Element UI 的 Maximo 管理面板，配套 Solon + Java 17 后端。

## 技术栈

**前端**
- Vue 2.6 + Vue Router + Vuex
- Element UI 2.14
- VXE Table 3.x
- sks-plugin-el-erp (私有组件库)

**后端** (`server/`)
- Solon 2.9.4
- Java 17 + Maven
- DB2 数据库
- HikariCP 连接池
- API Key 认证

## 快速开始

### 后端启动

```bash
cd server
mvn package -DskipTests
java -jar target/solon-server-1.0.0.jar
```

后端默认启动在 `http://localhost:8081`。

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
├── bin/                    # 构建/运行脚本
├── server/                 # Solon Java 后端
│   ├── src/main/java/
│   │   ├── controller/     # API 控制器
│   │   ├── service/        # 业务逻辑
│   │   └── model/          # 数据模型
│   └── pom.xml
├── src/                    # Vue 前端
│   ├── api/                # API 接口
│   ├── components/         # 公共组件
│   ├── views/              # 页面
│   ├── router/             # 路由
│   └── store/              # Vuex 状态
└── vue.config.js           # 前端代理配置
```

## 后端配置

后端配置文件位于 `server/backend.config.json`，支持配置多个后端环境、API Key 等。

**注意**: `backend.config.json` 已加入 `.gitignore`，提交时不会被包含。
可参考 `backend.config.example.json` 的格式创建自己的配置。

## API 接口

所有 API 请求需携带 `X-API-Key` 请求头进行认证。

| 接口 | 说明 |
|------|------|
| `GET /maxobject/list?keyword=&pageNum=&pageSize=` | 对象列表(分页) |
| `GET /maxobject/{objectName}/detail` | 对象详情(基本信息+属性+关系+索引) |
| `GET /maximo/log/{server}` | 读取 Maximo 日志(SSE) |

## 许可证

MIT