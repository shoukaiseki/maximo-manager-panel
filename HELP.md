# Maximo Management Panel 运行指南

## 环境要求

| 组件 | 版本要求 |
|------|---------|
| Node.js | >= 8.9 |
| Java | JDK 17 |
| Maven | 3.x |
| DB2 | 与 Maximo 相同的数据库 |

## 目录结构

```
maximo-manager-panel/
├── server/          # Java 后端 (Solon 框架)
├── src/             # Vue 前端
├── bin/             # 启动脚本
└── dist/            # 前端构建产物
```

---

## 一、后端启动

### 方式 1：使用批处理脚本（推荐）

双击运行 `bin/run-solon-server.bat`，会自动编译打包并启动后端服务。

脚本默认使用 `D:\usr\java\jdk-17.0.19x64` 的 JDK 和 `D:\usr\apache\apache-maven-3.9.7` 的 Maven。
如果路径不同，需修改脚本中的对应路径。

### 方式 2：手动启动

```bash
cd server
mvn package -DskipTests
java -jar target/solon-server-1.0.0.jar
```

后端默认运行在 `http://localhost:8081`。

---

## 二、前端启动

### 安装依赖

```bash
npm install
```

如果安装缓慢，可使用国内镜像：

```bash
npm install --registry=https://registry.npmmirror.com
```

### 环境配置

将 `.env.development.example` 复制为 `.env.development`：

```bash
copy .env.development.example .env.development
```

根据需要修改 `.env.development` 中的 `VUE_APP_DEFAULT_TARGET` 指向你的 Maximo 服务端地址。

### 开发模式运行

```bash
npm run dev
```

前端默认运行在 `http://localhost:28765`，浏览器会自动打开。

开发模式下，前端通过 Vue CLI 代理访问后端 API：
- `/solonapi/*` → 代理到 Solon 后端 `http://localhost:8081`
- `/maximo/*` → 代理到 Maximo 服务端

### 生产模式构建

```bash
# 测试环境
npm run build:stage

# 生产环境
npm run build:prod
```

构建产物在 `dist/` 目录，可直接部署到任意 Web 服务器（Nginx、IIS 等）。

---

## 三、后端配置

配置文件位于 `server/src/main/resources/` 目录，采用多 profile 机制。

### app.yml（主配置，必选）

```yaml
server:
  port: 8081

solon:
  apiKey: "your-api-key-here"
```

### app-{profile}.yml（环境配置，可选）

例如 `app-my.yml` 包含数据库等敏感配置，通过 profile 机制加载：

```yaml
db2:
  schema: "maximo"
  url: "jdbc:db2://localhost:50000/maximo"
  username: "db2inst1"
  password: "your-password"
  driverClassName: "com.ibm.db2.jcc.DB2Driver"
  maximumPoolSize: 10
  minimumIdle: 2
```

### 激活 profile

启动时通过启动参数指定环境名称（Solon 支持四种方式）：

```bash
# 方式一：启动参数（推荐）
java -jar target/solon-server-1.0.0.jar --env=my

# 方式二：系统属性
java -Dsolon.env=my -jar target/solon-server-1.0.0.jar

# 方式三：在 app.yml 中指定
solon.env: my

# 方式四：环境变量
export solon.env=my
java -jar target/solon-server-1.0.0.jar
```

Solon 会自动加载 `app.yml` + `app-{env}.yml`（例如 `app-my.yml`）。

> `app-*.yml` 已加入 `.gitignore`，不会提交到 git。
> 新人部署时，可复制 `app.yml` 为 `app-dev.yml` 并根据实际环境修改。

### 配置项说明

| 配置项 | 说明 |
|--------|------|
| `server.port` | 后端服务端口，默认 8081 |
| `solon.apiKey` | API 密钥，前端请求需携带 `X-API-Key` 请求头 |
| `db2.schema` | DB2 数据库模式名 |
| `db2.url` | DB2 数据库连接地址 |
| `db2.username` | 数据库用户名 |
| `db2.password` | 数据库密码 |
| `db2.maximumPoolSize` | 连接池最大连接数 |
| `db2.minimumIdle` | 连接池最小空闲连接数 |

> 修改配置后需重新编译打包（`mvn package -DskipTests`）才能生效。

---

## 四、前端代理配置

`vue.config.js` 中配置了开发模式的代理：

```javascript
// /solonapi/* → Solon 后端 (localhost:8081)
// /maximo/*   → Maximo 服务端
```

如需修改后端地址，可编辑 `.env.development` 中的 `VUE_APP_DEFAULT_TARGET`。

---

## 五、常见问题

### 5.1 后端启动报 "JDK 找不到"

修改 `bin/run-solon-server.bat` 中的 `JAVA_HOME` 路径为实际 JDK 17 安装路径。

### 5.2 API Key 不正确

启动后端后，控制台会输出 `API Key: Configured`。如果前端请求报 401：

1. 打开 `server/src/main/resources/app.yml`，检查 `solon.apiKey` 配置
2. 打开前端页面，点击**右上角设置按钮**（齿轮图标）→ 打开环境配置弹窗
3. 在弹窗中修改 **API Key**，然后保存
4. 保存后页面会自动刷新并生效

### 5.3 前端页面空白 / 接口报错

1. 确认 Solon 后端已启动（端口 8081）
2. 打开浏览器开发者工具（F12），查看 Network 标签
3. 检查请求是否被代理正确转发

### 5.4 npm install 失败

如果 npm install 安装失败，使用 WebStorm 打开项目，点击提示的 npm install 安装即可。

---

## 六、快速启动（总结）

```
1. 确保 JDK 17 和 Maven 已安装
2. 复制 `.env.development.example` 为 `.env.development`
3. 双击 `bin/run-solon-server.bat` → 后端启动
4. 新开终端执行 `npm install` → 安装前端依赖
5. 执行 `npm run dev` → 前端启动
6. 浏览器访问 `http://localhost:28765`
7. 右上角齿轮图标 → 配置 API Key 和后端地址
```