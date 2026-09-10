# MAFAPPDATA 导入/导出 Web 页面 — 后端处理逻辑文档

## 1. 目标

在 maximo-manager-panel 中新增一个页面，**绕过 Maximo 应用层，直接对 DB2 的 `MAFAPPDATA` 表**做 graphite 应用(zip)的导入、导出、列表查询与删除。

页面操作的对象是 **Manage/移动端 graphite 应用包**（如 MASUSER、NAVIGATOR、ASSETMOBILE…），每个应用一行，`APP` 列是 BLOB(zip)。

后端遵循项目现有风格：Solon `@Controller` + `@Service` + JDBC(`PreparedStatement`)，参考
- [MaxAppXmlController.java / MaxAppXmlService.java](file:///E:/gitwork/maximo-manager-panel/server/src/main/java/com/sks/server/service/MaxAppXmlService.java)（分页/大字段读取）
- [ExcelImportController.java](file:///E:/gitwork/maximo-manager-panel/server/src/main/java/com/sks/server/controller/ExcelImportController.java)（文件上传 `UploadedFile`）
- [RestResult.java](file:///E:/gitwork/maximo-manager-panel/server/src/main/java/com/sks/server/model/RestResult.java)（统一响应）

## 2. 数据对象（以真实库为准）

真实库表结构（`SYSCAT.COLUMNS`，schema=MAXIMO，共 14 列，当前 34 条记录全部 `STATUS='ACTIVE', APPMODE='DEBUG'`）：

| 列名 | 类型 | 空 | 说明 / 样例值 |
|------|------|----|--------------|
| MAFAPPDATAID | BIGINT | N | 主键，非 IDENTITY，用序列生成 |
| APP | BLOB(1GB) | N | graphite 应用 zip（实测 4.5MB ~ 22MB） |
| APPID | VARGRAPHIC(40) | N | 应用名，如 `MASUSER`、`LOGIN` |
| APPMODE | VARGRAPHIC(50) | N | 实测全为 `DEBUG` |
| DEPLOYDATETIME | TIMESTAMP | N | 部署时间 |
| BUILDTIMESTAMP | TIMESTAMP | N | 构建时间（唯一索引组成部分） |
| DEPLOYBY | VARGRAPHIC(50) | N | 实测为 `UPDATEDB` |
| PRIVATEKEY | VARGRAPHIC(50) | Y | 全库为空 |
| STATUS | VARGRAPHIC(25) | Y | 实测 `ACTIVE`；OSLC 路由只认 ACTIVE |
| VERSION | VARGRAPHIC(15) | N | 如 `9.1.77.0` |
| REVISION | INTEGER | Y | 实测为 0 |
| CHECKSUM | VARGRAPHIC(64) | Y | zip 的 SHA-256 hex（64 位） |
| ISMOBILE | INTEGER | N | 0/1 |
| ROWSTAMP | BIGINT | N | 乐观锁，见下文生成规则 |

索引/主键（`SYSCAT.INDEXES`）：
- 主键：`MAFAPPDATAID`
- 唯一索引：`MAFAPPDATA_NDX1 (APPID + BUILDTIMESTAMP)` → **同一 APPID 允许多行（多版本），版本标识是 BUILDTIMESTAMP，导入幂等判断用它**

主键序列：`MAXIMO.MAFAPPDATASEQ`（BIGINT，MINVALUE=25），INSERT 用 `NEXT VALUE FOR MAXIMO.MAFAPPDATASEQ`。

ROWSTAMP 生成规则：库中 `MAXROWSTAMP` 表**没有** MAFAPPDATA 的记录，且现网行值（如 235894）是全局递增风格。直接操作 DB 时按以下策略：
- 新插入：`COALESCE(MAX(ROWSTAMP),0) + 1`（SELECT MAX(ROWSTAMP) FROM MAXIMO.MAFAPPDATA）
- 覆盖更新：保留原行 ROWSTAMP 不变

> 注意：MAXROWSTAMP 机制在 MAFAPPDATA 上不生效，不要照搬其它老表的 `UPDATE MAXROWSTAMP SET CURMAXROWSTAMP...` 逻辑。

## 3. 与 Maximo 运行时的联动（很重要）

- `/oslc/graphite/{app}/...` 路由由 [GraphiteRouteHandler](file:///e:/maximoProject/java_sources/imaximob/decompiled-businessobjects/com/ibm/tivoli/maximo/oslc/provider/GraphiteRouteHandler.java) 处理，只认 `STATUS='ACTIVE'` 的行，且按 **VERSION-REVISION-CHECKSUM** 拼目录到 `MAF_APP_ROOT/{app}/{版本-修订-checksum}/` 读静态文件；
- 磁盘无该 checksum 目录时，Maximo 会在请求时从 `APP` BLOB 自动解压到磁盘（`DatabaseResourceLoader.expandApp`）；
- 因此**导入改库后通常无需重启**：新 checksum 首次访问自动展开；但旧版本目录不会自动清理，挂载目录会累积，属正常行为；
- **风险**：直接更新 `STATUS`/`APP` 会影响石墨应用在线内容，导入前必须备份（导出原行）并在页面二次确认。

## 4. 后端代码结构

新增（完全参照现有风格）：

```
server/src/main/java/com/sks/server/
├── controller/MafAppDataController.java   # 路由入口，参数校验、RestResult 包装
├── service/MafAppDataService.java         # JDBC 业务逻辑（注入 DataSource）
└── model/
    ├── MafAppDataInfo.java                # 列表行/元数据模型（不含 BLOB 内容，含 APPLEN）
    └── MafAppDataImportResult.java        # 导入结果（成功/失败行、错误信息）
```

`/solonapi/*` 前缀与认证沿用现有 `ApiKeyFilter`（X-API-Key），无需额外鉴权逻辑。

## 5. 接口设计

全部走 `/solonapi/mafappdata/**`：

| # | 接口 | 方法 | 说明 |
|---|------|------|------|
| 1 | `/mafappdata/list` | GET | 分页列表（不出 BLOB，带 APPLEN=OCTET_LENGTH(APP)），支持 APPID/STATUS/ISMOBILE 过滤，`=x` 前缀精确匹配 |
| 2 | `/mafappdata/detail` | GET | 单行元数据（含 APPLEN、CHECKSUM、各时间戳，不含 BLOB 内容） |
| 3 | `/mafappdata/analyze` | POST | 上传 zip（原始 graphite 包）→ 校验 zip 合法性、读包内 build.json 回填 APPID/VERSION |
| 4 | `/mafappdata/import` | POST | 导入（两种模式：`.apkg` 完整包 / 原始 zip + 表单元数据），见 §7 |
| 5 | `/mafappdata/export` | GET | 按 MAFAPPDATAID（可多个）导出，返回 `.apkg` 二进制流下载 |
| 6 | `/mafappdata/delete` | POST | 删除（危险），返回受影响行数 |

响应统一 `RestResult`；导出接口直接返回二进制（`Content-Type: application/octet-stream` + `Content-Disposition`），不走 RestResult。

## 6. 各接口后端处理逻辑

### 6.1 列表 `/mafappdata/list`

```sql
-- 总数
SELECT COUNT(*) AS TOTAL FROM MAXIMO.MAFAPPDATA t WHERE 1=1 [过滤条件]

-- 分页（不 SELECT APP！避免大对象传输）
SELECT t.MAFAPPDATAID, t.APPID, t.APPMODE, t.STATUS, t.VERSION, t.REVISION,
       t.CHECKSUM, t.ISMOBILE, t.DEPLOYBY, t.DEPLOYDATETIME, t.BUILDTIMESTAMP,
       OCTET_LENGTH(t.APP) AS APPLEN
FROM MAXIMO.MAFAPPDATA t
[WHERE 过滤条件]
ORDER BY t.APPID, t.BUILDTIMESTAMP DESC
OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
```

过滤条件（借鉴 MaxAppXmlService 的写法）：
- `appid`：`=X` → `t.APPID = ?`；否则 `UPPER(t.APPID) LIKE ?`（`%X%`）
- `status`、`ismobile`：`t.STATUS = ?` / `t.ISMOBILE = ?`
- 返回结构：`{ rows, total, pageNum, pageSize }`

经验守则（与现有代码一致）：列表 SQL 永远不 SELECT `APP`，BLOB 只在导出/分析时按行读取。

### 6.2 详情 `/mafappdata/detail`

按 `mafappdataid` 查一行，SELECT 列同列表 + 不取 BLOB；找不到返回 `RestResult.error("未找到记录")`。

### 6.3 分析 `/mafappdata/analyze`（上传 zip 自动识别元数据）

逻辑：
1. `UploadedFile` 非空校验，大小上限 100MB（app.yml 已配置 `maxFileSize: 100MB`）；
2. `new ZipInputStream(file.getContent())` 校验是合法 zip（否则报"不是有效的 ZIP 文件"）；
3. 遍历 zip 条目，读取名为 `build.json`（或小写 `build.json`）的文件，解析 JSON 提取 `appName/APPID`、`version`；
4. 若包内没有 build.json，则页面上由用户手动填 APPID/VERSION；
5. 返回：`{ appid, version, zipBytes(可选不回传), size, valid }`。前端拿 appid/version 预填表单。

### 6.4 导出 `/mafappdata/export`

支持单条 `?ids=58` 与多条 `?ids=58,57,56`。

**导出产物格式（.apkg = zip）**，保证"导出的东西能原样导回"：

```
MASUSER.apkg
├── appdata.json      # 该行全部元数据（UTF-8，完整还原用）
└── app.zip           # 原始 APP BLOB（原样字节，不改动）
```

`appdata.json` 样例：

```json
{
  "MAFAPPDATAID": 58,
  "APPID": "MASUSER",
  "APPMODE": "DEBUG",
  "STATUS": "ACTIVE",
  "VERSION": "9.1.77.0",
  "REVISION": 0,
  "CHECKSUM": "d4a6ca15453ea35dd10bc3a5bbf09fcee3985906b2e9bb386209236db6cd35d5",
  "ISMOBILE": 0,
  "DEPLOYBY": "UPDATEDB",
  "DEPLOYDATETIME": "2026-05-07 00:22:49.687262",
  "BUILDTIMESTAMP": "2026-05-07 00:22:49.687262",
  "PRIVATEKEY": null,
  "ROWSTAMP": 235855
}
```

后端逻辑：
1. `SELECT APP, appdata.json 所需列 FROM MAXIMO.MAFAPPDATA WHERE MAFAPPDATAID IN (...)` —— **逐个 ID 单独查询 BLOB**（一次查多行 `APP` 多 blob 内存翻倍，单行处理更稳）；
2. `Blob blob = rs.getBlob("APP"); byte[] data = blob.getBytes(1, (int) blob.length());`（实测最大 22MB，内存安全；如后续超 200MB 再改流式写 zip）；
3. 用 `ZipOutputStream` 写 `appdata.json` + `app.zip` 两条目；文件名 `{APPID}-v{VERSION}-r{REVISION}-{CHECKSUM前8位}.apkg`；
4. 多条导出时可打包成 `mafappdata-export.zip`（外层再套一层 zip，内含多个 apkg）；
5. 响应：`Content-Disposition: attachment; filename*=UTF-8''{文件名}`（APPID 含中文/特殊字符时用 RFC5987 编码），`Oslc`/前端用 `responseType: 'blob'` 接收。

### 6.5 导入 `/mafappdata/import`

请求参数：`UploadedFile file` +（模式 B 时）`appid/version/appmode/status/ismobile/revision/deployby` 表单字段。

**Step 1 识别模式与解析包**
- 先认 `.apkg`：zip 内若同时存在 `appdata.json` 与 `app.zip` → 模式 A，从 `appdata.json` 还原全部元数据，`app.zip` 即 BLOB 字节；
- 否则视为原始 graphite zip（模式 B）：
  - `APP` 字节 = 上传文件原字节；
  - 元数据优先级：表单字段 > analyze 提取（前端 analyze 后已预填）> 默认值（`APPMODE=DEBUG`、`STATUS=ACTIVE`、`REVISION=0`、`ISMOBILE=0`、`DEPLOYBY=当前 IP 或 'MANAGE-PANEL'`）；
  - `APPID`、`VERSION` 必填（模式 B 下从 build.json 提取不到则前端强制手动填）。

**Step 2 校验**
1. zip 合法性：`ZipInputStream` 能正常读到条目（防直接把 zlib 流/损坏文件写库）；
2. `APPID` 长度 ≤ 40、`VERSION` ≤ 15（超长直接报错，DB2 VARGRAPHIC 会报错更晦涩）；
3. 计算 `CHECKSUM = SHA-256(APP 字节)` 的 hex（64 位，格式与现网一致），**以后端重算值为准**（模式 A 的 appdata.json 里旧 checksum 仅作比对提示）；
4. `REVISION` 缺省 0；`ISMOBILE` 归一为 0/1。

**Step 3 幂等 upsert（唯一键 APPID + BUILDTIMESTAMP）**

```sql
-- 存在性判断（模式 A 用 appdata.json 的 BUILDTIMESTAMP；模式 B 用当前时间 CURRENT TIMESTAMP）
SELECT MAFAPPDATAID FROM MAXIMO.MAFAPPDATA
 WHERE APPID = ? AND BUILDTIMESTAMP = ?

-- 不存在 → INSERT
INSERT INTO MAXIMO.MAFAPPDATA
  (MAFAPPDATAID, APP, APPID, APPMODE, DEPLOYDATETIME, BUILDTIMESTAMP,
   DEPLOYBY, PRIVATEKEY, STATUS, VERSION, REVISION, CHECKSUM, ISMOBILE, ROWSTAMP)
VALUES (NEXT VALUE FOR MAXIMO.MAFAPPDATASEQ, ?, ?, ?, CURRENT TIMESTAMP, ?, ?,?, ?, ?, ?, ?, ?, ?)

-- 存在 → UPDATE（BLOB 与元数据整体覆盖）
UPDATE MAXIMO.MAFAPPDATA
   SET APP = ?, APPMODE = ?, DEPLOYDATETIME = CURRENT TIMESTAMP,
       DEPLOYBY = ?, PRIVATEKEY = ?, STATUS = ?, VERSION = ?, REVISION = ?,
       CHECKSUM = ?, ISMOBILE = ?, ROWSTAMP = ?
 WHERE MAFAPPDATAID = ?   -- ROWSTAMP 保留原值
```

- BLOB 写入用 `ps.setBinaryStream(2, new ByteArrayInputStream(zipBytes), zipBytes.length)`；
- `ROWSTAMP`：INSERT 时 `COALESCE((SELECT MAX(ROWSTAMP) FROM MAXIMO.MAFAPPDATA),0)+1`；UPDATE 时不动；
- **同一事务**内完成判断+写入；导入多条（批量上传多个文件或一个多文件 zip）时每行独立 try，失败行记录错误信息继续下一行，最后返回 `MafAppDataImportResult{totalRows, successRows, failedRows, errors[]}`；
- 提交后**不清理** `MAF_APP_ROOT`：新 checksum 目录由 Maximo 首次访问时自动生成（见 §3）；若用户勾选"覆盖 ACTIVE 应用"，页面提示"将立即影响石墨路由版本"并二次确认。

**Step 4 返回结果**

```json
{ "rows": [ { "APPID":"MASUSER", "version":"9.1.77.0", "checksum":"d4a6...15d5",
              "action":"INSERT|UPDATE", "success":true, "error":null } ] }
```

### 6.6 删除 `/mafappdata/delete`

```sql
DELETE FROM MAXIMO.MAFAPPDATA WHERE MAFAPPDATAID = ?
```

- 前端二次确认 + 后端 `force=CONFIRM` 参数兜底；删除返回受影响行数。

## 7. 异常与边界清单

| 场景 | 处理 |
|------|------|
| 上传非 zip / 损坏 zip | analyze/import 直接报"不是有效的 ZIP 包" |
| APPID/VERSION 超长 | 导入前长度校验（40/15） |
| 文档/列表接口误 SELECT APP | 明确禁止，列表 SQL 用 `OCTET_LENGTH(APP)` 代替 |
| APP BLOB 超大 | 现网 5-22MB，内存读写安全；批量导出逐行处理，避免多行 BLOB 同时驻留 |
| 改库影响在线路由 | STATUS=ACTIVE / 覆盖已有 BUILD 版本时二次确认；导入前自动备份原行为 .apkg 下载链接 |
| DB2 报 -4460 等 | 与行 `rs.getString("APP")` 无关；BLOB 始终用 `getBlob/getBinaryStream` |
| 并发写同一 APPID | 依赖唯一索引 APPID+BUILDTIMESTAMP；模式 B 同一秒重复导入会触发唯一键冲突 → 捕获 SQLException 转友好提示 |

## 8. 前端对接要点（简）

- 页面：`src/views/maximo/mafappdata/index.vue`；API：`src/api/mafappdata.js`（用 `solonRequest` / 下载用 `request({responseType:'blob'})`）；
- 布局参照 `src/views/maximo/maxappxml/index.vue`：顶部查询区 + 表格 + 上传/导出/删除工具栏；
- 行按钮：导出、删除；顶部批量导出（选中行）；上传支持拖拽，上传后走 analyze 预填表单再确认导入；
- 操作结果用 ElMessageBox 二次确认（尤其"覆盖 ACTIVE 应用"）。