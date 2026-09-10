package com.sks.server.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.io.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * MAFAPPDATA（graphite 移动端应用包）管理服务。
 * 直接操作 DB2 MAXIMO.MAFAPPDATA 表（绕过 Maximo 应用层）：
 * - 列表/详情：绝不 SELECT APP BLOB，用 OCTET_LENGTH(APP) 取大小
 * - 导入：支持 .apkg（appdata.json + app.zip）与原始 graphite zip 两种模式，按 APPID+BUILDTIMESTAMP 幂等 upsert
 * - 导出：.apkg（zip 内含 appdata.json + app.zip），多导出时外套一层 zip
 */
@Component
public class MafAppDataService {

    @Inject
    private DataSource dataSource;

    private static final String TABLE = "MAXIMO.MAFAPPDATA";

    /** 列表/详情 SELECT 列（不含 APP BLOB，APPLEN 用 OCTET_LENGTH） */
    private static final String META_COLUMNS =
            "t.MAFAPPDATAID, t.APPID, t.APPMODE, t.STATUS, t.VERSION, t.REVISION, " +
            "t.CHECKSUM, t.ISMOBILE, t.DEPLOYBY, t.PRIVATEKEY, t.DEPLOYDATETIME, t.BUILDTIMESTAMP, t.ROWSTAMP, " +
            "OCTET_LENGTH(t.APP) AS APPLEN";

    // ==================== 列表 / 详情 ====================

    /**
     * 分页列表（不读 BLOB）
     */
    public Map<String, Object> queryList(String appid, String status, String ismobile, int pageNum, int pageSize) {
        StringBuilder whereSql = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (appid != null && !appid.trim().isEmpty()) {
            String s = appid.trim();
            if (s.startsWith("=")) {
                whereSql.append(" AND t.APPID = ?");
                params.add(s.substring(1).toUpperCase());
            } else {
                whereSql.append(" AND UPPER(t.APPID) LIKE ?");
                params.add("%" + s.toUpperCase() + "%");
            }
        }
        if (status != null && !status.trim().isEmpty()) {
            whereSql.append(" AND t.STATUS = ?");
            params.add(status.trim().toUpperCase());
        }
        if (ismobile != null && !ismobile.trim().isEmpty()) {
            whereSql.append(" AND t.ISMOBILE = ?");
            params.add("1".equals(ismobile.trim()) ? 1 : 0);
        }
        String whereStr = whereSql.toString();

        int total = 0;
        String countSql = "SELECT COUNT(*) AS TOTAL FROM " + TABLE + " t" + whereStr;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) total = rs.getInt("TOTAL");
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAFAPPDATA 总数失败: " + e.getMessage(), e);
        }

        String dataSql = "SELECT " + META_COLUMNS + " FROM " + TABLE + " t" + whereStr +
                " ORDER BY t.APPID, t.BUILDTIMESTAMP DESC " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ps.setInt(params.size() + 1, offset);
            ps.setInt(params.size() + 2, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(rowToMap(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAFAPPDATA 列表失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("MAFAPPDATAID", rs.getLong("MAFAPPDATAID"));
        row.put("APPID", rs.getString("APPID"));
        row.put("APPMODE", rs.getString("APPMODE"));
        row.put("STATUS", rs.getString("STATUS"));
        row.put("VERSION", rs.getString("VERSION"));
        row.put("REVISION", rs.getInt("REVISION"));
        row.put("CHECKSUM", rs.getString("CHECKSUM"));
        row.put("ISMOBILE", rs.getInt("ISMOBILE"));
        row.put("DEPLOYBY", rs.getString("DEPLOYBY"));
        row.put("PRIVATEKEY", rs.getString("PRIVATEKEY"));
        row.put("DEPLOYDATETIME", rs.getTimestamp("DEPLOYDATETIME") != null ? rs.getTimestamp("DEPLOYDATETIME").toString() : null);
        row.put("BUILDTIMESTAMP", rs.getTimestamp("BUILDTIMESTAMP") != null ? rs.getTimestamp("BUILDTIMESTAMP").toString() : null);
        row.put("ROWSTAMP", rs.getLong("ROWSTAMP"));
        row.put("APPLEN", rs.getLong("APPLEN"));
        return row;
    }

    /**
     * 按 ID 查详情元数据（不含 BLOB，含 APPLEN）。不存在时抛异常。
     */
    public Map<String, Object> queryDetail(long id) {
        try {
            Map<String, Object> meta = queryMetaById(id);
            if (meta == null) throw new RuntimeException("未找到记录: " + id);
            return meta;
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAFAPPDATA 详情失败: " + e.getMessage(), e);
        }
    }

    // ==================== 分析上传 zip ====================

    /**
     * 分析上传的 zip：校验合法性并尝试读取包内 build.json 提取 appid/version。
     * 返回 { valid, appid, version, size, fileName }
     */
    public Map<String, Object> analyze(byte[] fileBytes, String fileName) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", fileName);
        result.put("size", fileBytes == null ? 0 : fileBytes.length);
        if (fileBytes == null || fileBytes.length == 0) {
            result.put("valid", false);
            result.put("message", "文件为空");
            return result;
        }
        // .apkg 包含 build.json 的可能性低，但仍尝试按原始 zip 读 build.json
        String appid = null;
        String version = null;
        boolean validZip = false;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                validZip = true;
                String name = entry.getName().toLowerCase();
                if (!entry.isDirectory() && name.endsWith("build.json")) {
                    try {
                        JSONObject build = JSON.parseObject(new String(readAll(zis), "UTF-8"));
                        if (build != null) {
                            appid = firstNonBlank(build.getString("appName"), build.getString("appId"),
                                    build.getString("APPID"), build.getString("app"));
                            version = firstNonBlank(build.getString("version"), build.getString("VERSION"),
                                    build.getString("appVersion"));
                        }
                    } catch (Exception ignore) {
                        // build.json 解析失败不影响 zip 合法性
                    }
                }
            }
        } catch (IOException e) {
            result.put("valid", false);
            result.put("message", "不是有效的 ZIP 文件: " + e.getMessage());
            return result;
        }
        if (!validZip) {
            result.put("valid", false);
            result.put("message", "不是有效的 ZIP 文件（包内无条目）");
            return result;
        }
        result.put("valid", true);
        result.put("appid", appid);
        result.put("version", version);
        result.put("message", "ZIP 校验通过");
        return result;
    }

    // ==================== 导入 ====================

    /**
     * 导入应用包。
     * @param fileBytes 上传文件字节
     * @param fileName  原始文件名
     * @param formAppid     表单 APPID（模式 B 必填）
     * @param formVersion   表单 VERSION
     * @param appmode       APPMODE（默认 DEBUG）
     * @param status        STATUS（默认 ACTIVE）
     * @param ismobile      ISMOBILE（默认 0）
     * @param revision      REVISION（默认 0）
     * @param deployby      DEPLOYBY
     * @return { rows: [{ appid, version, checksum, action, success, error }] }
     */
    public Map<String, Object> importApp(byte[] fileBytes, String fileName,
                                         String formAppid, String formVersion,
                                         String appmode, String status,
                                         String ismobile, Integer revision, String deployby,
                                         Long targetId, Boolean updateOnly) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (fileBytes == null || fileBytes.length == 0) {
            throw new RuntimeException("上传文件为空");
        }

        byte[] appBytes;
        String appid;
        String version;
        String metaAppmode;
        String metaStatus;
        int metaIsmobile;
        int metaRevision;
        String metaDeployby;
        Timestamp buildTimestamp;

        // 尝试解析 .apkg（zip 内含 appdata.json + app.zip）
        Map<String, Object> apkg = parseApkg(fileBytes);
        if (apkg != null) {
            // 模式 A：.apkg 完整包
            appBytes = (byte[]) apkg.get("appBytes");
            JSONObject meta = (JSONObject) apkg.get("meta");
            appid = meta.getString("APPID");
            version = meta.getString("VERSION");
            metaAppmode = meta.getString("APPMODE");
            metaStatus = meta.getString("STATUS");
            metaIsmobile = meta.getIntValue("ISMOBILE");
            metaRevision = meta.containsKey("REVISION") ? meta.getIntValue("REVISION") : 0;
            metaDeployby = meta.getString("DEPLOYBY");
            String bts = meta.getString("BUILDTIMESTAMP");
            buildTimestamp = parseTimestamp(bts);
        } else {
            // 模式 B：原始 graphite zip
            appBytes = fileBytes;
            // 元数据优先级：表单 > build.json 提取 > 默认值
            Map<String, Object> analysis = analyze(fileBytes, fileName);
            if (!Boolean.TRUE.equals(analysis.get("valid"))) {
                throw new RuntimeException(analysis.get("message") == null ? "不是有效的 ZIP 包" : String.valueOf(analysis.get("message")));
            }
            appid = firstNonBlank(formAppid, (String) analysis.get("appid"));
            version = firstNonBlank(formVersion, (String) analysis.get("version"));
            metaAppmode = firstNonBlank(appmode, "DEBUG");
            metaStatus = firstNonBlank(status, "ACTIVE");
            metaIsmobile = "1".equals(ismobile == null ? "" : ismobile.trim()) ? 1 : 0;
            metaRevision = revision != null ? revision : 0;
            metaDeployby = firstNonBlank(deployby, "MANAGE-PANEL");
            buildTimestamp = new Timestamp(System.currentTimeMillis());
        }

        // 校验
        if (appid == null || appid.trim().isEmpty()) {
            throw new RuntimeException("APPID 不能为空（未能从包内 build.json 识别，请手动填写）");
        }
        appid = appid.trim().toUpperCase();
        if (appid.length() > 40) {
            throw new RuntimeException("APPID 长度不能超过 40（当前 " + appid.length() + "）");
        }
        if (version == null || version.trim().isEmpty()) {
            version = "0.0.0.0";
        }
        version = version.trim();
        if (version.length() > 15) {
            throw new RuntimeException("VERSION 长度不能超过 15（当前 " + version.length() + "）");
        }
        if (metaAppmode == null || metaAppmode.trim().isEmpty()) metaAppmode = "DEBUG";
        if (metaStatus == null || metaStatus.trim().isEmpty()) metaStatus = "ACTIVE";

        // 重算 checksum（以后端重算值为准）
        String checksum = sha256Hex(appBytes);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("appid", appid);
        row.put("version", version);
        row.put("size", appBytes.length);
        try {
            String action;
            if (Boolean.TRUE.equals(updateOnly)) {
                // 更新模式（详情"导入更新"）：仅更新已有记录，绝不新增
                if (targetId == null || targetId <= 0) {
                    throw new RuntimeException("更新模式必须指定目标记录 ID");
                }
                action = updateRow(targetId, appid, appBytes, version, metaAppmode, metaStatus,
                        metaIsmobile, metaRevision, metaDeployby, checksum);
            } else {
                action = upsertRow(appBytes, appid, version, metaAppmode, metaStatus,
                        metaIsmobile, metaRevision, metaDeployby, checksum, buildTimestamp);
            }
            row.put("checksum", checksum);
            row.put("action", action);
            row.put("success", true);
            row.put("error", null);
        } catch (Exception e) {
            row.put("checksum", checksum);
            row.put("action", "FAILED");
            row.put("success", false);
            row.put("error", e.getMessage());
        }
        rows.add(row);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        return result;
    }

    /**
     * 更新模式（详情"导入更新"）：按 MAFAPPDATAID 更新已有记录，绝不新增。
     * 校验包内 APPID 与目标记录一致；保留 APPID/BUILDTIMESTAMP/ROWSTAMP/PRIVATEKEY，
     * 其余元数据与 APP BLOB 更新为本次导入内容，DEPLOYDATETIME 置为当前时间。
     */
    private String updateRow(long targetId, String appid, byte[] appBytes, String version, String appmode,
                             String status, int ismobile, int revision, String deployby, String checksum)
            throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 校验目标记录存在且 APPID 一致
                String curAppid = null;
                String selSql = "SELECT APPID FROM " + TABLE + " WHERE MAFAPPDATAID = ?";
                try (PreparedStatement ps = conn.prepareStatement(selSql)) {
                    ps.setLong(1, targetId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) curAppid = rs.getString(1);
                    }
                }
                if (curAppid == null) {
                    throw new RuntimeException("未找到要更新的记录（MAFAPPDATAID=" + targetId + "），更新模式不会新增记录");
                }
                if (!curAppid.trim().toUpperCase().equals(appid.trim().toUpperCase())) {
                    throw new RuntimeException("包内 APPID 与目标记录不一致（" + curAppid + " != " + appid + "），更新模式仅支持更新同一应用");
                }

                String updateSql = "UPDATE " + TABLE +
                        " SET APP = ?, APPMODE = ?, DEPLOYDATETIME = CURRENT TIMESTAMP, " +
                        " DEPLOYBY = ?, STATUS = ?, VERSION = ?, REVISION = ?, " +
                        " CHECKSUM = ?, ISMOBILE = ? " +
                        " WHERE MAFAPPDATAID = ?";
                int affected;
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setBinaryStream(1, new ByteArrayInputStream(appBytes), appBytes.length);
                    ps.setString(2, appmode);
                    ps.setString(3, deployby);
                    ps.setString(4, status);
                    ps.setString(5, version);
                    ps.setInt(6, revision);
                    ps.setString(7, checksum);
                    ps.setInt(8, ismobile);
                    ps.setLong(9, targetId);
                    affected = ps.executeUpdate();
                }
                conn.commit();
                if (affected <= 0) {
                    throw new RuntimeException("更新失败：未匹配到记录（MAFAPPDATAID=" + targetId + "）");
                }
                return "UPDATE";
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 幂等 upsert：按 APPID + BUILDTIMESTAMP 判断存在与否。
     */
    private String upsertRow(byte[] appBytes, String appid, String version, String appmode, String status,
                             int ismobile, int revision, String deployby, String checksum, Timestamp buildTimestamp)
            throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long existingId = null;
                String selSql = "SELECT MAFAPPDATAID FROM " + TABLE + " WHERE APPID = ? AND BUILDTIMESTAMP = ?";
                try (PreparedStatement ps = conn.prepareStatement(selSql)) {
                    ps.setString(1, appid);
                    ps.setTimestamp(2, buildTimestamp);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) existingId = rs.getLong(1);
                    }
                }

                if (existingId == null) {
                    // INSERT
                    long maxRowstamp = 0;
                    try (Statement st = conn.createStatement();
                         ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(ROWSTAMP),0) AS M FROM " + TABLE)) {
                        if (rs.next()) maxRowstamp = rs.getLong(1);
                    }
                    String insertSql = "INSERT INTO " + TABLE +
                            " (MAFAPPDATAID, APP, APPID, APPMODE, DEPLOYDATETIME, BUILDTIMESTAMP, " +
                            " DEPLOYBY, PRIVATEKEY, STATUS, VERSION, REVISION, CHECKSUM, ISMOBILE, ROWSTAMP) " +
                            "VALUES (NEXT VALUE FOR MAXIMO.MAFAPPDATASEQ, ?, ?, ?, CURRENT TIMESTAMP, ?, " +
                            " ?, NULL, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setBinaryStream(1, new ByteArrayInputStream(appBytes), appBytes.length);
                        ps.setString(2, appid);
                        ps.setString(3, appmode);
                        ps.setTimestamp(4, buildTimestamp);
                        ps.setString(5, deployby);
                        ps.setString(6, status);
                        ps.setString(7, version);
                        ps.setInt(8, revision);
                        ps.setString(9, checksum);
                        ps.setInt(10, ismobile);
                        ps.setLong(11, maxRowstamp + 1);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    return "INSERT";
                } else {
                    // UPDATE（ROWSTAMP、BUILDTIMESTAMP 保留原值）
                    String updateSql = "UPDATE " + TABLE +
                            " SET APP = ?, APPMODE = ?, DEPLOYDATETIME = CURRENT TIMESTAMP, " +
                            " DEPLOYBY = ?, PRIVATEKEY = NULL, STATUS = ?, VERSION = ?, REVISION = ?, " +
                            " CHECKSUM = ?, ISMOBILE = ? " +
                            " WHERE MAFAPPDATAID = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setBinaryStream(1, new ByteArrayInputStream(appBytes), appBytes.length);
                        ps.setString(2, appmode);
                        ps.setString(3, deployby);
                        ps.setString(4, status);
                        ps.setString(5, version);
                        ps.setInt(6, revision);
                        ps.setString(7, checksum);
                        ps.setInt(8, ismobile);
                        ps.setLong(9, existingId);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    return "UPDATE";
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 解析 .apkg 包（zip 内同时含 appdata.json 与 app.zip）。
     * 不是 apkg 时返回 null。
     */
    private Map<String, Object> parseApkg(byte[] fileBytes) {
        byte[] appBytes = null;
        JSONObject meta = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                if (entry.isDirectory()) continue;
                if (name.equals("appdata.json") || name.endsWith("/appdata.json")) {
                    try {
                        meta = JSON.parseObject(new String(readAll(zis), "UTF-8"));
                    } catch (Exception ignore) {
                    }
                } else if (name.equals("app.zip") || name.endsWith("/app.zip")) {
                    appBytes = readAll(zis);
                }
            }
        } catch (IOException e) {
            return null;
        }
        if (meta != null && appBytes != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("meta", meta);
            map.put("appBytes", appBytes);
            return map;
        }
        return null;
    }

    // ==================== 导出 ====================

    /**
     * 根据 IDs 计算导出文件名（只查元数据，不读 BLOB）。
     * 单条：{APPID}__ver-{VERSION}.zip（生产 zip 命名，如 masuserprofile__ver-9.1.893.0.zip）
     * 多条：mafappdata-export.zip
     */
    public String buildExportFileName(String ids) {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) throw new RuntimeException("未指定要导出的记录");
        if (idList.size() == 1) {
            Map<String, Object> meta;
            try {
                meta = queryMetaById(idList.get(0));
            } catch (SQLException e) {
                throw new RuntimeException("查询导出记录失败: " + e.getMessage(), e);
            }
            if (meta == null) throw new RuntimeException("未找到记录: " + idList.get(0));
            String appid = String.valueOf(meta.get("APPID"));
            String version = String.valueOf(meta.get("VERSION"));
            if (meta.get("VERSION") == null || version.trim().isEmpty()) {
                version = "0.0.0.0";
            }
            return appid + "__ver-" + version + ".zip";
        }
        return "mafappdata-export.zip";
    }

    /**
     * 导出到输出流：单条直接输出 APP BLOB（生产 zip）；多条写外套 zip（内含多个生产 zip）。
     */
    public void writeExport(String ids, OutputStream out) {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) throw new RuntimeException("未指定要导出的记录");
        try {
            if (idList.size() == 1) {
                writeAppBlob(idList.get(0), out);
            } else {
                try (ZipOutputStream outer = new ZipOutputStream(out)) {
                    for (Long id : idList) {
                        Map<String, Object> meta = queryMetaById(id);
                        if (meta == null) continue;
                        String appid = String.valueOf(meta.get("APPID"));
                        String version = String.valueOf(meta.get("VERSION"));
                        if (meta.get("VERSION") == null || version.trim().isEmpty()) {
                            version = "0.0.0.0";
                        }
                        outer.putNextEntry(new ZipEntry(appid + "__ver-" + version + ".zip"));
                        writeAppBlob(id, outer);
                        outer.closeEntry();
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取单条记录 APP BLOB 并直接写入输出流（按行读取流式写出，避免整包驻留内存）。
     */
    private void writeAppBlob(long id, OutputStream out) throws SQLException, IOException {
        String sql = "SELECT t.APP FROM " + TABLE + " t WHERE t.MAFAPPDATAID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new RuntimeException("未找到记录: " + id);
                Blob blob = rs.getBlob("APP");
                if (blob == null) throw new RuntimeException("记录 " + id + " 的 APP 内容为空");
                try (InputStream is = blob.getBinaryStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }
                blob.free();
            }
        }
    }

    /**
     * 按 ID 查元数据（不含 BLOB）。
     */
    private Map<String, Object> queryMetaById(long id) throws SQLException {
        String sql = "SELECT " + META_COLUMNS + " FROM " + TABLE + " t WHERE t.MAFAPPDATAID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rowToMap(rs);
            }
        }
        return null;
    }

    // ==================== 工具方法 ====================

    private List<Long> parseIds(String ids) {
        List<Long> list = new ArrayList<>();
        if (ids == null || ids.trim().isEmpty()) return list;
        for (String part : ids.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) continue;
            try {
                list.add(Long.parseLong(s));
            } catch (NumberFormatException ignore) {
            }
        }
        return list;
    }

    private Timestamp parseTimestamp(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new Timestamp(System.currentTimeMillis());
        }
        String s = text.trim();
        // 兼容 "2026-05-07 00:22:49.687262"（微秒）与标准 Timestamp 格式
        try {
            if (s.length() > 26) s = s.substring(0, 26);
            return Timestamp.valueOf(s);
        } catch (Exception e) {
            return new Timestamp(System.currentTimeMillis());
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算 SHA-256 失败: " + e.getMessage(), e);
        }
    }

    private String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
