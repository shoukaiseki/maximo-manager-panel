# 日志级别分组功能 实现计划

## Context

当前"日志级别配置"是单表单行编辑表格（数据存 `logger_level_config`），无分组概念、无导入、无跨组复用。用户需要将其重构为**多标签页分组系统**：一个始终保留的"默认日志记录器"组 + 用户自建组，支持行编辑、导入 JSON、跨组多选添加、从查询 tab 勾选添加，以及按当前组下发到 Maximo。

已与用户确认的关键点：
- 默认组读现有 `logger_level_config` 表（**不改表结构**），仍是第一个固定 tab；选中时"更新到 Maximo"禁用。
- 用户组用两张新表：`logger_level_group`（组）+ `logger_level_group_item`（条目，各组独立存 level/ignored）。
- "更新到 Maximo"仅下发当前选中组的未忽略条目。
- 去掉 JSON 模式，只保留表格行编辑。

## A. 后端（`server/src/main/java/com/sks/server`）

### A1. DDL — 追加到 [MysqlDbConfig.java](file:///e:/gitwork/maximo-manager-panel/server/src/main/java/com/sks/server/config/MysqlDbConfig.java) 的 `createTableSqls` 数组（`logger_level_config` 之后），风格对齐 `saved_query`：
- `logger_level_group`(id, name, description, created_at, updated_at, UNIQUE uk_name)
- `logger_level_group_item`(id, group_id, logger_name, level, ignored, description, sort_order, created_at, updated_at, UNIQUE uk_group_logger)
- 不加外键（与现有表一致）。

### A2. 新增 Model（`model/`，private 字段 + getter/setter，对齐 [LoggerLevelConfig.java](file:///e:/gitwork/maximo-manager-panel/server/src/main/java/com/sks/server/model/LoggerLevelConfig.java)）：
- `LoggerLevelGroup`(id, name, description)
- `LoggerLevelGroupItem`(id, groupId, loggerName, level, ignored, description, sortOrder)
- `LoggerGroupCreateReq`(name, description)
- `LoggerGroupItemSaveReq`(groupId, items)
- `LoggerGroupAddReq`(groupId, loggerNames)

### A3. [LoggerLevelService.java](file:///e:/gitwork/maximo-manager-panel/server/src/main/java/com/sks/server/service/LoggerLevelService.java) 新增 `importConfigs(List<LoggerLevelConfig>)`：`SELECT logger_name WHERE IN(...)` 取已存在 → 事务内 batch INSERT 差集 → 返回 `{added, skipped, total}`。复用 `VALID_LEVELS`。不改现有方法。

### A4. 新建 `service/LoggerGroupService.java`（`@Component`，`@Inject("mysql")`，JDBC，事务对齐 [SavedQueryService](file:///e:/gitwork/maximo-manager-panel/server/src/main/java/com/sks/server/service/SavedQueryService.java)）：
- `listGroups()` / `getGroup(id)` / `createGroup(name,desc)` / `updateGroup(id,name,desc)` / `deleteGroup(id)`（事务：先删 item 再删 group）
- `listItems(groupId)` / `saveItems(groupId, items)`（全量覆盖 upsert，对齐 `saveConfigs`）/ `deleteItem(id)`
- `addItems(groupId, loggerNames)`（增量：取已存在，仅 INSERT 差集，默认 level=INFO）

### A5. 新建 `controller/LoggerGroupController.java`（`@Controller`，try/catch 返回 `RestResult`，对齐 [LoggerLevelController](file:///e:/gitwork/maximo-manager-panel/server/src/main/java/com/sks/server/controller/LoggerLevelController.java)），路径 `/loggerlevel/group/...`：
- GET `/group/list`、`/group/detail?id=`、`/group/items?groupId=`
- POST `/group/create`(@Body)、`/group/update?id=`(@Body)、`/group/delete?id=`、`/group/items/save`(@Body)、`/group/items/delete?id=`、`/group/items/add`(@Body)

### A6. [LoggerLevelController.java](file:///e:/gitwork/maximo-manager-panel/server/src/main/java/com/sks/server/controller/LoggerLevelController.java) 新增 `POST /loggerlevel/import`(@Body LoggerLevelSaveReq) → `importConfigs`，返回 `{added,skipped,total}`。

## B. 前端 API — [loggerlevel.js](file:///e:/gitwork/maximo-manager-panel/src/api/loggerlevel.js) 追加（走 `solonRequest`）：
`listLoggerGroups` / `createLoggerGroup` / `updateLoggerGroup` / `deleteLoggerGroup` / `listLoggerGroupItems` / `saveLoggerGroupItems` / `deleteLoggerGroupItem` / `addLoggerToGroup` / `importLoggerConfig`

## C. 前端页面 — [LoggerLevelManager.vue](file:///e:/gitwork/maximo-manager-panel/src/views/maximo/maslog/LoggerLevelManager.vue)

### C1. 配置 tab 改为嵌套可编辑 `el-tabs`（`type="card"` addable）：
- 默认组 tab（name="default"，`:closable="false"`，绑 `defaultItems`）
- `v-for` 用户组 tab（closable，绑 `g.items`）
- `@tab-add`→打开新建分组弹窗；`@tab-remove`→`$confirm` 后删组
- 两个 tab 内的 el-table 列定义相同（序号 / loggerName 输入 / level 下拉[DEBUG/INFO/WARN/ERROR] / ignored 复选 / description 输入 / "读取当前" / 删除）

### C2. 工具栏按当前组动态显示：
- "更新到 Maximo"：`activeGroupId==='default'` 时 `:disabled="true"`
- "保存当前分组" / "添加行" / "重新加载"
- "跨组添加"（多选其它组的 logger 名加入当前组）
- 默认组：el-upload 导入 JSON（`auto-upload=false` + FileReader，对齐 [ApiCaller.vue](file:///e:/gitwork/maximo-manager-panel/src/views/maximo/apicaller/ApiCaller.vue) L302/L987）
- 用户组："重命名"

### C3. 查询 tab 改造：el-table 加 `type="selection"` 列 + `@selection-change`；工具栏加"加入分组"按钮 → 弹窗选目标组。

### C4. data 模型：`activeGroupId`、`defaultItems`/`defaultLoading`、`userGroups:[{id,name,description,items,loading}]`、`querySelection`、3 个弹窗状态。computed：`activeGroup`、`currentItems`、`currentGroupLoading`。

### C5. 方法：保留查询相关方法不动；删除 `jsonMode/jsonText/toggleJsonMode/collectConfigLoggers(JSON分支)`；`loadConfig`→`loadDefaultItems`；新增 `loadUserGroups`/`loadGroupItems`/`addRow`/`saveCurrentGroup`/`reloadCurrentGroup`/`updateToMaximo`(按当前组 items 过滤)/`onImportFile`/分组弹窗 CRUD/`openCrossGroupDialog`/`openAddToGroupDialog`。

### C6. 3 个对话框（`append-to-body`）：分组新建/重命名、跨组多选添加、从查询加入目标组选择。复用现有 pushResult 弹窗。

## D. 边界场景
- 空组保存 → `saveItems` 空列表 DELETE 全部（对齐 `saveConfigs`）
- 组内 loggerName 重复 → Service 抛错 + 前端查重
- 删组 → 事务先删 item 再删 group + `$confirm`
- 跨组/查询加入已存在 logger → 仅 INSERT 差集，提示"加入 N 跳过 M"
- 默认组导入跳过已存在 → `uk_logger_name` 兜底
- 删当前激活组 → 切回 `default`
- 保存时 groupId 不存在 → Service 校验抛错

## E. 验证
1. `cd server && mvn -q -DskipTests compile`（BUILD SUCCESS）
2. 启动后端，确认两张新表已建（可 MCP `query_by_sql`）
3. 前端 `npm run hd`，浏览器打开日志级别管理页
4. 手测：默认组不可关闭+更新按钮禁用+导入；新建组 g1→加行保存→更新到Maximo(仅含g1非忽略项)；跨组添加；查询tab勾选加入分组；删组级联；刷新持久
5. 回归查询 tab 过滤/改级别功能

## 改动文件清单
- 修改：MysqlDbConfig.java、LoggerLevelService.java、LoggerLevelController.java、loggerlevel.js、LoggerLevelManager.vue、router(已注册无需改)
- 新增：LoggerLevelGroup.java、LoggerLevelGroupItem.java、LoggerGroupCreateReq.java、LoggerGroupItemSaveReq.java、LoggerGroupAddReq.java、LoggerGroupService.java、LoggerGroupController.java
