package com.sks.server.service;

import org.apache.poi.ss.usermodel.*;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多语言 Excel 导入服务。上传后缓存原始文件，执行时读取目标工作表并逐行提交。
 */
@Component
public class MessageImportService {

    private static final Logger log = LoggerFactory.getLogger(MessageImportService.class);

    /** 文件缓存会话有效期：30 分钟 */
    private static final long SESSION_TTL_MS = 30 * 60 * 1000L;
    /** 预览返回行数 */
    private static final int PREVIEW_LIMIT = 10;

    /** 与前端 trimAll 等价：清除首尾空格、全角空格、NBSP、零宽字符等 */
    private static final Pattern TRIM_PATTERN = Pattern.compile(
            "^[\\s\\u3000\\u00A0\\u2000-\\u200A\\u202F\\u205F\\uFEFF]+|" +
                    "[\\s\\u3000\\u00A0\\u2000-\\u200A\\u202F\\u205F\\uFEFF]+$");

    /** 形如 ZH_VALUE 的语言列 */
    private static final Pattern LANG_COLUMN_PATTERN = Pattern.compile("^(.+)_VALUE$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE_LANG_COLUMN_PATTERN = Pattern.compile("^(.+)_(TITLE|REMARKS)$");
    private static final Pattern DESCRIPTION_LANG_COLUMN_PATTERN = Pattern.compile("^(.+)_(VALUE|DESC)$");

    /** sessionId -> 文件缓存 */
    private static final Map<String, CachedFile> FILE_CACHE = new ConcurrentHashMap<>();

    @Inject
    private DataSource dataSource;

    // ==================== 对外接口 ====================

    /**
     * 查询已启用语言及基准语言
     */
    public Map<String, Object> queryLanguages() {
        try (Connection conn = dataSource.getConnection()) {
            return buildLanguagesResult(conn);
        } catch (SQLException e) {
            throw new RuntimeException("查询 LANGUAGE 表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传预览：缓存文件并返回前 10 行数据
     *
     * @param bytes    文件字节
     * @param fileName 文件名
     * @param trim     是否清除前后空字符
     */
    public Map<String, Object> preview(byte[] bytes, String fileName, boolean trim, boolean attribute,
                                       String targetName) throws IOException {
        ImportTarget target = importTarget(attribute, targetName);
        cleanupExpiredSessions();

        List<LangInfo> languages;
        String baseLanguage;
        try (Connection conn = dataSource.getConnection()) {
            languages = loadEnabledLanguages(conn);
            baseLanguage = loadBaseLanguage(conn, languages, target != ImportTarget.MESSAGE);
        } catch (SQLException e) {
            throw new RuntimeException("查询 LANGUAGE 表失败: " + e.getMessage(), e);
        }

        Set<String> supported = new LinkedHashSet<>();
        for (LangInfo lang : languages) {
            supported.add(lang.code);
        }

        ParsedExcel parsed = parseExcel(bytes, trim, supported, target);

        String sessionId = UUID.randomUUID().toString();
        FILE_CACHE.put(sessionId, new CachedFile(bytes, fileName, System.currentTimeMillis(), target));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("fileName", fileName);
        result.put("fileSize", bytes.length);
        result.put("baseLanguage", baseLanguage);
        result.put("languages", toLanguageList(languages, baseLanguage));
        result.put("sheetName", parsed.sheetName);
        result.put("columns", parsed.columns);
        result.put("rows", buildPreviewRows(parsed));
        result.put("totalRows", parsed.rows.size());
        result.put("previewRows", Math.min(PREVIEW_LIMIT, parsed.rows.size()));
        result.put("invalidLanguages", parsed.invalidLanguages);
        result.put("missingLanguages", buildMissingLanguages(parsed, languages, target == ImportTarget.ATTRIBUTE));

        String disabledReason = buildDisabledReason(parsed, languages, target);
        result.put("canUpdate", disabledReason == null);
        result.put("disabledReason", disabledReason);

        return result;
    }

    /**
     * 执行更新：按缓存会话读取完整 Excel，仅更新勾选的语言
     */
    public Map<String, Object> execute(String sessionId, boolean trim, String langcodes, boolean attribute,
                                       String targetName) {
        ImportTarget target = importTarget(attribute, targetName);
        cleanupExpiredSessions();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("缺少文件缓存会话标识，请重新上传 Excel");
        }
        CachedFile cached = FILE_CACHE.get(sessionId.trim());
        if (cached == null || cached.target != target) {
            throw new IllegalArgumentException("文件缓存会话不存在、已过期或导入目标不一致，请重新上传 Excel");
        }

        List<LangInfo> languages;
        String baseLanguage;
        try (Connection conn = dataSource.getConnection()) {
            languages = loadEnabledLanguages(conn);
            baseLanguage = loadBaseLanguage(conn, languages, target != ImportTarget.MESSAGE);
        } catch (SQLException e) {
            throw new RuntimeException("查询 LANGUAGE 表失败: " + e.getMessage(), e);
        }

        Set<String> supported = new LinkedHashSet<>();
        for (LangInfo lang : languages) {
            supported.add(lang.code);
        }

        Set<String> selected = parseSelectedLanguages(langcodes);
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("请至少勾选一种要更新的语言");
        }
        List<LangInfo> selectedLanguages = new ArrayList<>();
        for (LangInfo lang : languages) {
            if (selected.contains(lang.code)) {
                selectedLanguages.add(lang);
            }
        }
        if (selectedLanguages.isEmpty()) {
            throw new IllegalArgumentException("所选语言不在当前启用语言中，请重新勾选");
        }

        ParsedExcel parsed;
        try {
            parsed = parseExcel(cached.bytes, trim, supported, target);
        } catch (IOException e) {
            throw new RuntimeException("读取缓存的 Excel 失败: " + e.getMessage(), e);
        }
        String disabledReason = buildDisabledReason(parsed, languages, target);
        if (disabledReason != null) {
            throw new IllegalArgumentException(disabledReason);
        }
        if (target != ImportTarget.MESSAGE && selectedLanguages.stream().noneMatch(lang ->
                target == ImportTarget.ATTRIBUTE
                        ? parsed.languageColumns.containsKey(lang.code + "_TITLE")
                            || parsed.languageColumns.containsKey(lang.code + "_REMARKS")
                        : parsed.languageColumns.containsKey(lang.code))) {
            throw new IllegalArgumentException("Excel 中没有所选语言的列");
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> failedRows = new ArrayList<>();
        int successCount = 0;
        int abnormalCount = 0;
        int errorCount = 0;
        int skippedCount = 0;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            for (ExcelRow er : parsed.rows) {
                Map<String, Object> rowResult = switch (target) {
                    case MESSAGE -> processRow(conn, er, selectedLanguages, baseLanguage);
                    case ATTRIBUTE -> processAttributeRow(conn, er, selectedLanguages, baseLanguage);
                    default -> processTranslationRow(conn, er, selectedLanguages, baseLanguage, target);
                };
                rows.add(rowResult);

                String status = String.valueOf(rowResult.get("_status"));
                if ("success".equals(status)) {
                    successCount++;
                } else if ("skipped".equals(status)) {
                    skippedCount++;
                } else {
                    failedRows.add(rowResult);
                    if ("ERROR".equals(rowResult.get("sks:failType"))) {
                        errorCount++;
                    } else {
                        abnormalCount++;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("执行更新失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("fileName", cached.fileName);
        result.put("_apiAction", "imp");
        result.put("status", "success");
        result.put("totalRows", parsed.rows.size());
        result.put("baseLanguage", baseLanguage);
        result.put("selectedLanguages", selected);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put(target.table.toLowerCase(Locale.ROOT), rows);
        result.put("data", data);

        result.put("sks:successSumTotal", successCount);
        result.put("sks:failSumTotal", abnormalCount + errorCount);
        result.put("sks:abnormalSumTotal", abnormalCount);
        result.put("sks:errorSumTotal", errorCount);
        result.put("sks:skippedSumTotal", skippedCount);
        result.put("failedRows", failedRows);

        return result;
    }

    // ==================== 单行处理 ====================

    private Set<String> parseSelectedLanguages(String langcodes) {
        Set<String> selected = new LinkedHashSet<>();
        if (langcodes == null) {
            return selected;
        }
        for (String code : langcodes.split(",")) {
            String normalized = trimAll(code).toUpperCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                selected.add(normalized);
            }
        }
        return selected;
    }

    private Map<String, Object> processRow(Connection conn, ExcelRow er,
                                           List<LangInfo> languages, String baseLanguage) {
        Map<String, Object> rowResult = new LinkedHashMap<>();
        rowResult.put("sks:excelRow", er.rowNumber);
        rowResult.put("MSGGROUP", er.group);
        rowResult.put("MSGKEY", er.key);
        rowResult.put("EXCEL_MAXMESSAGESID", er.maxMessagesId);
        rowResult.put("sks:uuid", UUID.randomUUID().toString());
        rowResult.put("_apiAction", "imp");

        if (isBlank(er.group) || isBlank(er.key)) {
            return failRow(rowResult, "NOT_FOUND",
                    "MSGGROUP 或 MSGKEY 为空，无法匹配 MAXMESSAGES",
                    "MAXMESSAGES", null, null, Collections.emptyList(), 0, 0);
        }

        MessageRow message;
        try {
            message = findMessage(conn, er.group, er.key);
        } catch (SQLException e) {
            return failRow(rowResult, "ERROR", "查询 MAXMESSAGES 失败: " + e.getMessage(),
                    "MAXMESSAGES", null, null, Collections.emptyList(), 0, 0);
        }

        if (message == null) {
            return failRow(rowResult, "NOT_FOUND",
                    "MAXMESSAGES 中不存在 MSGGROUP=" + er.group + "、MSGKEY=" + er.key + " 的消息，已忽略",
                    "MAXMESSAGES", null, null, Collections.emptyList(), 0, 0);
        }

        rowResult.put("sks:objectName", "MAXMESSAGES");
        rowResult.put("sks:uniqueID", message.id);
        rowResult.put("sks:uniqueIDName", "MAXMESSAGESID");
        rowResult.put("sks:ref", er.group + "/" + er.key);
        rowResult.put("MSGID", message.msgId);

        List<Map<String, Object>> languageResults = new ArrayList<>();
        List<String> failMessages = new ArrayList<>();
        int okCount = 0;
        int failCount = 0;
        int skipCount = 0;
        boolean hasError = false;

        for (LangInfo lang : languages) {
            String value = er.values.get(lang.code);
            if (isBlank(value)) {
                Map<String, Object> detail = languageDetail(lang.code, null, "skipped", null, null,
                        "Excel 中该语言值为空，跳过");
                languageResults.add(detail);
                skipCount++;
                continue;
            }

            boolean isBase = lang.code.equalsIgnoreCase(baseLanguage);
            String table = isBase ? "MAXMESSAGES" : "L_MAXMESSAGES";
            try {
                if (isBase) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE MAXMESSAGES SET VALUE = ? WHERE MAXMESSAGESID = ?")) {
                        ps.setString(1, value);
                        ps.setLong(2, message.id);
                        if (ps.executeUpdate() == 0) {
                            throw new SQLException("未找到 MAXMESSAGESID=" + message.id + " 的记录");
                        }
                    }
                    languageResults.add(languageDetail(lang.code, "MAXMESSAGES", "success", message.id,
                            "MAXMESSAGESID", "更新成功"));
                    okCount++;
                } else {
                    Long lMaxMessagesId = findLanguageRecordId(conn, message.id, lang.code);
                    boolean inserted = false;
                    if (lMaxMessagesId == null) {
                        lMaxMessagesId = insertLanguageRecord(conn, message.id, lang.code, value);
                        inserted = true;
                    } else {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE L_MAXMESSAGES SET VALUE = ? WHERE L_MAXMESSAGESID = ?")) {
                            ps.setString(1, value);
                            ps.setLong(2, lMaxMessagesId);
                            if (ps.executeUpdate() == 0) {
                                throw new SQLException("未找到 L_MAXMESSAGESID=" + lMaxMessagesId + " 的记录");
                            }
                        }
                    }
                    Map<String, Object> detail = languageDetail(lang.code, "L_MAXMESSAGES", "success",
                            lMaxMessagesId, "L_MAXMESSAGESID", inserted ? "新增成功" : "更新成功");
                    if (inserted) {
                        detail.put("_operation", "created");
                    }
                    languageResults.add(detail);
                    okCount++;
                }
            } catch (Exception e) {
                log.warn("更新消息多语言值失败: {}/{} lang={}", er.group, er.key, lang.code, e);
                Map<String, Object> detail = languageDetail(lang.code, table, "failed", null,
                        isBase ? "MAXMESSAGESID" : "L_MAXMESSAGESID",
                        "更新失败: " + e.getMessage());
                detail.put("sks:failType", "ERROR");
                languageResults.add(detail);
                failMessages.add(lang.code + ": " + e.getMessage());
                failCount++;
                hasError = true;
            }
        }

        // 执行异常：整行回滚，避免只更新了部分语言值
        if (hasError) {
            try {
                conn.rollback();
            } catch (SQLException ignore) {
                // ignore
            }
            for (Map<String, Object> detail : languageResults) {
                if ("success".equals(detail.get("_status"))) {
                    detail.put("_status", "failed");
                    detail.put("_operation", "none");
                    detail.put("sks:statusLabel", "错误");
                    detail.put("sks:failType", "ERROR");
                    detail.put("_message", "该行执行出错，已回滚");
                }
            }
            int rolledBackFail = 0;
            for (Map<String, Object> detail : languageResults) {
                if ("failed".equals(detail.get("_status"))) {
                    rolledBackFail++;
                }
            }
            rowResult.put("sks:languages", languageResults);
            return failRow(rowResult, "ERROR",
                    "更新失败，已回滚该行: " + String.join("；", failMessages),
                    "MAXMESSAGES", message.id, message.msgId, languageResults, 0, rolledBackFail);
        }

        try {
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignore) {
                // ignore
            }
            log.warn("提交消息多语言更新失败: {}/{}", er.group, er.key, e);
            return failRow(rowResult, "ERROR", "提交更新失败: " + e.getMessage(),
                    "MAXMESSAGES", message.id, message.msgId, languageResults, 0, failCount);
        }

        rowResult.put("sks:languages", languageResults);
        rowResult.put("sks:successSumTotal", okCount);
        rowResult.put("sks:failSumTotal", failCount);

        if (okCount == 0 && failCount == 0) {
            rowResult.put("sks:successTotal", 0);
            rowResult.put("sks:failTotal", 0);
            rowResult.put("_status", "skipped");
            rowResult.put("_operation", "none");
            rowResult.put("_message", "Excel 中该行没有可更新的语言值，已跳过");
            rowResult.put("sks:statusLabel", "跳过");
            return rowResult;
        }

        if (failCount > 0) {
            rowResult.put("sks:successTotal", 0);
            rowResult.put("sks:failTotal", 1);
            rowResult.put("_status", "failed");
            rowResult.put("_operation", "failed");
            rowResult.put("sks:failType", "ERROR");
            rowResult.put("sks:statusLabel", hasError ? "错误" : "异常");
            rowResult.put("_message", String.join("；", failMessages));
            return rowResult;
        }

        rowResult.put("sks:successTotal", 1);
        rowResult.put("sks:failTotal", 0);
        rowResult.put("_status", "success");
        rowResult.put("_operation", "updated");
        rowResult.put("sks:statusLabel", "成功");
        rowResult.put("_message", "更新成功" + (skipCount > 0 ? "（跳过 " + skipCount + " 个空值）" : ""));
        return rowResult;
    }

    private Map<String, Object> failRow(Map<String, Object> rowResult, String failType, String message,
                                        String objectName, Long uniqueId, String msgId,
                                        List<Map<String, Object>> languageResults, int okCount, int failCount) {
        rowResult.put("_status", "failed");
        rowResult.put("_operation", "failed");
        rowResult.put("_message", message);
        rowResult.put("sks:failType", failType);
        rowResult.put("sks:statusLabel", "ERROR".equals(failType) ? "错误" : "异常");
        rowResult.put("sks:objectName", objectName);
        rowResult.put("sks:uniqueID", uniqueId);
        rowResult.putIfAbsent("sks:uniqueIDName", "MAXATTRIBUTECFG".equals(objectName) ? "MAXATTRIBUTEID" : "MAXMESSAGESID");
        rowResult.put("sks:successTotal", 0);
        rowResult.put("sks:failTotal", 1);
        rowResult.put("sks:successSumTotal", okCount);
        rowResult.put("sks:failSumTotal", Math.max(failCount, 1));
        rowResult.put("sks:languages", languageResults);
        if (msgId != null) {
            rowResult.put("MSGID", msgId);
        }
        return rowResult;
    }

    private Map<String, Object> languageDetail(String langCode, String table, String status,
                                               Long uniqueId, String uniqueIdName, String message) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("LANGCODE", langCode);
        detail.put("TABLE", table);
        detail.put("_status", status);
        detail.put("_operation", "success".equals(status) ? "updated" : "none");
        detail.put("_message", message);
        detail.put("sks:objectName", table);
        detail.put("sks:uniqueID", uniqueId);
        detail.put("sks:uniqueIDName", uniqueIdName);
        detail.put("sks:statusLabel", "success".equals(status) ? "成功" : ("failed".equals(status) ? "异常" : "跳过"));
        return detail;
    }

    // ==================== 数据库查询 ====================

    private MessageRow findMessage(Connection conn, String group, String key) throws SQLException {
        String sql = "SELECT MAXMESSAGESID, MSGID FROM MAXMESSAGES WHERE MSGGROUP = ? AND MSGKEY = ? FETCH FIRST 1 ROW ONLY";
        MessageRow row = queryMessage(conn, sql, group, key);
        if (row != null) {
            return row;
        }
        String ignoreCaseSql = "SELECT MAXMESSAGESID, MSGID FROM MAXMESSAGES " +
                "WHERE UPPER(MSGGROUP) = UPPER(?) AND UPPER(MSGKEY) = UPPER(?) FETCH FIRST 1 ROW ONLY";
        return queryMessage(conn, ignoreCaseSql, group, key);
    }

    private MessageRow queryMessage(Connection conn, String sql, String group, String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, group);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MessageRow row = new MessageRow();
                    row.id = rs.getLong("MAXMESSAGESID");
                    row.msgId = rs.getString("MSGID");
                    return row;
                }
            }
        }
        return null;
    }

    private Long findLanguageRecordId(Connection conn, long ownerId, String langCode) throws SQLException {
        String sql = "SELECT L_MAXMESSAGESID FROM L_MAXMESSAGES WHERE OWNERID = ? AND LANGCODE = ? FETCH FIRST 1 ROW ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ownerId);
            ps.setString(2, langCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("L_MAXMESSAGESID");
                }
            }
        }
        return null;
    }

    private Long insertLanguageRecord(Connection conn, long ownerId, String langCode, String value) throws SQLException {
        String sql = "INSERT INTO L_MAXMESSAGES (L_MAXMESSAGESID, OWNERID, LANGCODE, VALUE) " +
                "VALUES (L_MAXMESSAGESSEQ.nextval, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ownerId);
            ps.setString(2, langCode);
            ps.setString(3, value);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("新增 L_MAXMESSAGES 记录失败");
            }
        }
        Long id = findLanguageRecordId(conn, ownerId, langCode);
        if (id == null) {
            throw new SQLException("新增 L_MAXMESSAGES 记录后未查询到 L_MAXMESSAGESID");
        }
        return id;
    }

    private List<LangInfo> loadEnabledLanguages(Connection conn) throws SQLException {
        List<LangInfo> list = new ArrayList<>();
        String sql = "SELECT MAXLANGCODE, LANGUAGENAME FROM LANGUAGE WHERE ENABLED = 1 ORDER BY MAXLANGCODE";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LangInfo info = new LangInfo();
                info.code = trimAll(rs.getString("MAXLANGCODE")).toUpperCase(Locale.ROOT);
                info.name = trimAll(rs.getString("LANGUAGENAME"));
                if (!info.code.isEmpty()) {
                    list.add(info);
                }
            }
        }
        return list;
    }

    private String loadBaseLanguage(Connection conn, List<LangInfo> languages, boolean requireConfigured) throws SQLException {
        String base = null;
        String sql = "SELECT VARVALUE FROM MAXVARS WHERE VARNAME = 'BASELANGUAGE' FETCH FIRST 1 ROW ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                base = trimAll(rs.getString("VARVALUE"));
            }
        }
        if (base != null && !base.isEmpty()) {
            return base.toUpperCase(Locale.ROOT);
        }
        if (requireConfigured) {
            throw new SQLException("MAXVARS 中未设置 BASELANGUAGE，无法确定基准语言");
        }
        return languages.isEmpty() ? "EN" : languages.get(0).code;
    }

    // ==================== Excel 解析 ====================

    private ParsedExcel parseExcel(byte[] bytes, boolean trim, Set<String> supportedLanguages,
                                   ImportTarget target) throws IOException {
        ParsedExcel parsed = new ParsedExcel();
        boolean attribute = target == ImportTarget.ATTRIBUTE;
        Map<String, Integer> identifierColumns = new LinkedHashMap<>();
        Pattern languagePattern = attribute ? ATTRIBUTE_LANG_COLUMN_PATTERN
                : target == ImportTarget.MENU || target == ImportTarget.SIGOPTION
                    ? DESCRIPTION_LANG_COLUMN_PATTERN : LANG_COLUMN_PATTERN;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel 文件中没有工作表");
            }
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = findExcelSheet(workbook, target, languagePattern, formatter, evaluator);
            parsed.sheetName = sheet.getSheetName();

            Row header = sheet.getRow(0);

            int lastCellNum = Math.max(0, header.getLastCellNum());
            for (int col = 0; col < lastCellNum; col++) {
                String headerValue = readCell(header, col, formatter, evaluator, true);
                if (headerValue.isEmpty()) {
                    continue;
                }
                String normalized = headerValue.toUpperCase(Locale.ROOT);
                if (target.keys.contains(normalized) || target.idColumn.equals(normalized)
                        || (target == ImportTarget.MODULE && "DESCRIPTION".equals(normalized))) {
                    if (identifierColumns.putIfAbsent(normalized, col) == null) {
                        parsed.columns.add(column(normalized, headerValue, null));
                    } else if (target != ImportTarget.MESSAGE) {
                        throw new IllegalArgumentException("Excel 表头重复: " + normalized);
                    }
                    continue;
                }

                Matcher matcher = languagePattern.matcher(normalized);
                String langCode = null;
                String suffix = "";
                if (matcher.matches()) {
                    langCode = matcher.group(1).trim();
                    if (attribute) {
                        suffix = "_" + matcher.group(2);
                    }
                } else if (target == ImportTarget.MESSAGE && supportedLanguages.contains(normalized)) {
                    langCode = normalized;
                }

                if (langCode != null && !langCode.isEmpty()) {
                    if (!supportedLanguages.contains(langCode)) {
                        if (!parsed.invalidLanguages.contains(langCode)) {
                            parsed.invalidLanguages.add(langCode);
                        }
                    } else {
                        String prop = langCode + suffix;
                        if (parsed.languageColumns.putIfAbsent(prop, col) == null) {
                            parsed.columns.add(column(prop, headerValue, langCode));
                        } else if (target != ImportTarget.MESSAGE) {
                            throw new IllegalArgumentException("Excel 中有重复的语言列: " + normalized);
                        }
                    }
                }
            }

            int lastRowNum = sheet.getLastRowNum();
            for (int rowNum = 1; rowNum <= lastRowNum; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                Map<String, String> identifiers = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> entry : identifierColumns.entrySet()) {
                    identifiers.put(entry.getKey(), readCell(row, entry.getValue(), formatter, evaluator, trim));
                }

                Map<String, String> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (Map.Entry<String, Integer> entry : parsed.languageColumns.entrySet()) {
                    String value = readCell(row, entry.getValue(), formatter, evaluator, trim);
                    values.put(entry.getKey(), value);
                    if (!isBlank(value)) {
                        hasValue = true;
                    }
                }

                if (!hasValue && identifiers.values().stream().allMatch(this::isBlank)) {
                    continue;
                }

                ExcelRow excelRow = new ExcelRow();
                excelRow.rowNumber = rowNum + 1;
                excelRow.identifiers = identifiers;
                if (target.sequence == null) {
                    excelRow.group = identifiers.get(target.keys.get(0));
                    excelRow.key = identifiers.get(target.keys.get(1));
                    excelRow.maxMessagesId = identifiers.getOrDefault(target.idColumn, "");
                }
                excelRow.values = values;
                parsed.rows.add(excelRow);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
        return parsed;
    }

    private String readCell(Row row, int column, DataFormatter formatter,
                            FormulaEvaluator evaluator, boolean trim) {
        if (row == null || column < 0) {
            return "";
        }
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        String value;
        try {
            CellType type = cell.getCellType();
            if (type == CellType.FORMULA) {
                value = formatter.formatCellValue(cell, evaluator);
            } else {
                value = formatter.formatCellValue(cell);
            }
        } catch (Exception e) {
            value = formatter.formatCellValue(cell);
        }
        return trim ? trimAll(value) : value;
    }

    /**
     * 与前端 trimAll 等价的清前后空字符
     */
    private String trimAll(String value) {
        if (value == null) {
            return "";
        }
        return TRIM_PATTERN.matcher(value).replaceAll("");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // ==================== 结果组装 ====================

    private Map<String, Object> buildLanguagesResult(Connection conn) throws SQLException {
        List<LangInfo> languages = loadEnabledLanguages(conn);
        String baseLanguage = loadBaseLanguage(conn, languages, false);
        String configuredDefaultLang = trimAll(Solon.cfg().get("solon.mmpDefaultLang", ""))
                .toUpperCase(Locale.ROOT);
        boolean supportedDefault = !configuredDefaultLang.isEmpty()
                && languages.stream().anyMatch(lang -> lang.code.equals(configuredDefaultLang));
        String mmpDefaultLang = supportedDefault ? configuredDefaultLang : baseLanguage;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseLanguage", baseLanguage);
        result.put("mmpDefaultLang", mmpDefaultLang);
        result.put("languages", toLanguageList(languages, baseLanguage));
        return result;
    }

    private List<Map<String, Object>> toLanguageList(List<LangInfo> languages, String baseLanguage) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (LangInfo lang : languages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", lang.code);
            item.put("name", lang.name);
            item.put("base", lang.code.equalsIgnoreCase(baseLanguage));
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> column(String prop, String label, String langCode) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("prop", prop);
        column.put("label", label);
        if (langCode != null) {
            column.put("langcode", langCode);
        }
        return column;
    }

    private List<Map<String, Object>> buildPreviewRows(ParsedExcel parsed) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int limit = Math.min(PREVIEW_LIMIT, parsed.rows.size());
        for (int i = 0; i < limit; i++) {
            ExcelRow er = parsed.rows.get(i);
            Map<String, Object> row = new LinkedHashMap<>(er.identifiers);
            row.putAll(er.values);
            rows.add(row);
        }
        return rows;
    }

    private List<String> buildMissingLanguages(ParsedExcel parsed, List<LangInfo> languages, boolean attribute) {
        List<String> missing = new ArrayList<>();
        for (LangInfo lang : languages) {
            for (String suffix : attribute ? List.of("_TITLE", "_REMARKS") : List.of("")) {
                if (!parsed.languageColumns.containsKey(lang.code + suffix)) {
                    missing.add(lang.code + suffix);
                }
            }
        }
        return missing;
    }

    /**
     * 返回禁用原因；null 表示可以执行更新
     */
    private String buildDisabledReason(ParsedExcel parsed, List<LangInfo> languages, ImportTarget target) {
        if (target == ImportTarget.MESSAGE && !parsed.invalidLanguages.isEmpty()) {
            List<String> supported = new ArrayList<>();
            for (LangInfo lang : languages) {
                supported.add(lang.code);
            }
            return "Excel 中存在不支持的语言列: " + String.join("、", parsed.invalidLanguages) +
                    "（当前启用语言: " + String.join("、", supported) + "）";
        }
        if (parsed.languageColumns.isEmpty()) {
            if (target == ImportTarget.ATTRIBUTE) {
                return "Excel 中没有已启用语言的 TITLE 或 REMARKS 列，无法执行更新";
            }
            return "Excel 中没有可识别的语言列（如 ZH_VALUE、EN_VALUE），无法执行更新";
        }
        if (parsed.rows.isEmpty()) {
            return "Excel 中没有可导入的数据行";
        }
        return null;
    }

    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        FILE_CACHE.entrySet().removeIf(entry -> now - entry.getValue().createdAt > SESSION_TTL_MS);
    }

    // ==================== 内部类 ====================

    private static class LangInfo {
        String code;
        String name;
    }

    private static class CachedFile {
        final byte[] bytes;
        final String fileName;
        final long createdAt;
        final ImportTarget target;

        CachedFile(byte[] bytes, String fileName, long createdAt, ImportTarget target) {
            this.bytes = bytes;
            this.fileName = fileName;
            this.createdAt = createdAt;
            this.target = target;
        }
    }

    private static class ParsedExcel {
        String sheetName;
        final Map<String, Integer> languageColumns = new LinkedHashMap<>();
        final List<Map<String, Object>> columns = new ArrayList<>();
        final List<String> invalidLanguages = new ArrayList<>();
        final List<ExcelRow> rows = new ArrayList<>();
    }

    private static class ExcelRow {
        int rowNumber;
        String group;
        String key;
        String maxMessagesId;
        Map<String, String> identifiers;
        Map<String, String> values;
    }

    private static class MessageRow {
        long id;
        String msgId;
    }

    private Map<String, Object> processAttributeRow(Connection conn, ExcelRow row,
                                                    List<LangInfo> languages, String baseLanguage) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sks:excelRow", row.rowNumber);
        result.put("OBJECTNAME", row.group);
        result.put("ATTRIBUTENAME", row.key);
        result.put("EXCEL_MAXATTRIBUTEID", row.maxMessagesId);
        result.put("sks:uuid", UUID.randomUUID().toString());
        result.put("_apiAction", "imp");
        result.put("sks:objectName", "MAXATTRIBUTECFG");
        result.put("sks:uniqueIDName", "MAXATTRIBUTEID");
        List<Map<String, Object>> details = new ArrayList<>();
        result.put("sks:languages", details);

        if (isBlank(row.group) || isBlank(row.key)) {
            return failRow(result, "NOT_FOUND", "OBJECTNAME 或 ATTRIBUTENAME 为空",
                    "MAXATTRIBUTECFG", null, null, details, 0, 0);
        }
        if (languages.stream().allMatch(lang -> trimAll(row.values.get(lang.code + "_TITLE")).isEmpty()
                && trimAll(row.values.get(lang.code + "_REMARKS")).isEmpty())) {
            result.put("_status", "skipped");
            result.put("sks:statusLabel", "跳过");
            result.put("_message", "所选语言没有可更新的值");
            return result;
        }

        Long cfgId = null;
        try {
            cfgId = findAttribute(conn, row, true);
            if (cfgId == null) {
                conn.rollback();
                return failRow(result, "NOT_FOUND", "MAXATTRIBUTECFG 中没有匹配且未标记删除的属性",
                        "MAXATTRIBUTECFG", null, null, details, 0, 0);
            }
            result.put("sks:uniqueID", cfgId);
            Long activeId = findAttribute(conn, row, false);
            if (activeId != null && !activeId.equals(cfgId)) {
                throw new SQLException("配置表和运行表的 MAXATTRIBUTEID 不一致");
            }
            for (LangInfo lang : languages) {
                String title = row.values.get(lang.code + "_TITLE");
                String remarks = row.values.get(lang.code + "_REMARKS");
                title = trimAll(title).isEmpty() ? null : title;
                remarks = trimAll(remarks).isEmpty() ? null : remarks;
                if (title == null && remarks == null) {
                    details.add(languageDetail(lang.code, null, "skipped", null, null, "TITLE 和 REMARKS 均为空"));
                    continue;
                }
                boolean base = lang.code.equals(baseLanguage);
                details.add(writeAttributeLanguage(conn, cfgId, lang.code, title, remarks, base, true));
                if (activeId != null) {
                    details.add(writeAttributeLanguage(conn, activeId, lang.code, title, remarks, base, false));
                }
            }
            conn.commit();
            result.put("_status", "success");
            result.put("sks:statusLabel", "成功");
            result.put("_message", activeId == null ? "配置表已更新，运行表尚无对应属性" : "配置表和运行表已更新");
            return result;
        } catch (SQLException e) {
            conn.rollback();
            log.warn("更新属性多语言失败: {}/{}", row.group, row.key, e);
            for (Map<String, Object> detail : details) {
                if ("success".equals(detail.get("_status"))) {
                    detail.put("_status", "failed");
                    detail.put("_operation", "none");
                    detail.put("sks:statusLabel", "错误");
                    detail.put("sks:failType", "ERROR");
                    detail.put("_message", "该行执行出错，已回滚");
                }
            }
            return failRow(result, "ERROR", "更新失败，已回滚该行: " + e.getMessage(),
                    "MAXATTRIBUTECFG", cfgId, null, details, 0, 1);
        }
    }

    private Long findAttribute(Connection conn, ExcelRow row, boolean config) throws SQLException {
        String table = config ? "MAXATTRIBUTECFG" : "MAXATTRIBUTE";
        // Keep the owner locked while checking for and inserting translations.
        String sql = "SELECT MAXATTRIBUTEID FROM " + table + " WHERE OBJECTNAME = ? AND ATTRIBUTENAME = ?"
                + (config ? " AND CHANGED <> 'R'" : "") + " WITH RS USE AND KEEP UPDATE LOCKS";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trimAll(row.group).toUpperCase(Locale.ROOT));
            ps.setString(2, trimAll(row.key).toUpperCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long id = rs.getLong(1);
                if (rs.next()) {
                    throw new SQLException(table + " 中存在多个匹配属性");
                }
                return id;
            }
        }
    }

    private Map<String, Object> writeAttributeLanguage(Connection conn, long ownerId, String langCode,
                                                        String title, String remarks, boolean base,
                                                        boolean config) throws SQLException {
        String table = base ? (config ? "MAXATTRIBUTECFG" : "MAXATTRIBUTE")
                : (config ? "L_MAXATTRCFG" : "L_MAXATTRIBUTE");
        String idColumn = base ? "MAXATTRIBUTEID" : table + "ID";
        String where = base ? "MAXATTRIBUTEID = ?" : "OWNERID = ? AND LANGCODE = ?";
        String update = "UPDATE " + table + " SET TITLE = COALESCE(?, TITLE), REMARKS = COALESCE(?, REMARKS) WHERE " + where;
        int affected;
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setString(1, title);
            ps.setString(2, remarks);
            ps.setLong(3, ownerId);
            if (!base) {
                ps.setString(4, langCode);
            }
            affected = ps.executeUpdate();
        }
        if (affected == 1) {
            return languageDetail(langCode, table, "success", base ? ownerId : null, idColumn, "更新成功");
        }
        if (base || affected != 0) {
            throw new SQLException(table + " 更新了 " + affected + " 条记录，预期为 1 条");
        }

        String sequence = config ? "L_MAXATTRCFGSEQ" : "L_MAXATTRIBUTEUSEQ";
        String insert = "INSERT INTO " + table + " (" + idColumn + ", OWNERID, LANGCODE, TITLE, REMARKS) "
                + "VALUES (" + sequence + ".NEXTVAL, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setLong(1, ownerId);
            ps.setString(2, langCode);
            ps.setString(3, title);
            ps.setString(4, remarks);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("新增 " + table + " 记录失败");
            }
        }
        Map<String, Object> detail = languageDetail(langCode, table, "success", null, idColumn, "新增成功");
        detail.put("_operation", "created");
        return detail;
    }

    private ImportTarget importTarget(boolean attribute, String name) {
        if (name == null || name.isBlank()) {
            return attribute ? ImportTarget.ATTRIBUTE : ImportTarget.MESSAGE;
        }
        ImportTarget target;
        try {
            target = ImportTarget.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("不支持的导入目标");
        }
        if (attribute && target != ImportTarget.ATTRIBUTE) {
            throw new IllegalArgumentException("导入目标参数冲突");
        }
        return target;
    }

    private Map<String, Object> processTranslationRow(Connection conn, ExcelRow row, List<LangInfo> languages,
                                                     String baseLanguage, ImportTarget target) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>(row.identifiers);
        result.put("sks:excelRow", row.rowNumber);
        result.put("sks:uuid", UUID.randomUUID().toString());
        result.put("sks:objectName", target.table);
        result.put("sks:uniqueIDName", target.idColumn);
        result.put("_apiAction", "imp");
        List<Map<String, Object>> details = new ArrayList<>();
        result.put("sks:languages", details);

        for (String key : target.keys) {
            if (trimAll(row.identifiers.get(key)).isEmpty()) {
                return failRow(result, "NOT_FOUND", key + " 为空", target.table, null, null, details, 0, 0);
            }
        }
        Long sourceId = null;
        if (target.keys.contains(target.idColumn)) {
            try {
                sourceId = Long.parseLong(trimAll(row.identifiers.get(target.idColumn)));
                if (sourceId <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                return failRow(result, "NOT_FOUND", target.idColumn + " 必须是有效的正整数",
                        target.table, null, null, details, 0, 0);
            }
        }
        if (languages.stream().allMatch(lang -> trimAll(row.values.get(lang.code)).isEmpty())) {
            result.put("_status", "skipped");
            result.put("sks:statusLabel", "跳过");
            result.put("_message", "所选语言没有可更新的值");
            return result;
        }

        Long ownerId = null;
        try {
            ownerId = findTranslationOwner(conn, row, target, sourceId);
            if (ownerId == null) {
                conn.rollback();
                return failRow(result, "NOT_FOUND", target.table + " 中没有匹配记录",
                        target.table, null, null, details, 0, 0);
            }
            result.put("sks:uniqueID", ownerId);
            for (LangInfo lang : languages) {
                String value = row.values.get(lang.code);
                details.add(trimAll(value).isEmpty()
                        ? languageDetail(lang.code, null, "skipped", null, null, "Excel 中该语言值为空")
                        : writeTranslation(conn, ownerId, lang.code, value, lang.code.equals(baseLanguage), target));
            }
            conn.commit();
            result.put("_status", "success");
            result.put("sks:statusLabel", "成功");
            result.put("_message", "更新成功");
            return result;
        } catch (SQLException e) {
            conn.rollback();
            log.warn("更新 {} 多语言失败，Excel 第 {} 行", target.table, row.rowNumber, e);
            for (Map<String, Object> detail : details) {
                if ("success".equals(detail.get("_status"))) {
                    detail.put("_status", "failed");
                    detail.put("_operation", "none");
                    detail.put("sks:statusLabel", "错误");
                    detail.put("sks:failType", "ERROR");
                    detail.put("_message", "该行执行出错，已回滚");
                }
            }
            return failRow(result, "ERROR", "更新失败，已回滚该行: " + e.getMessage(),
                    target.table, ownerId, null, details, 0, 1);
        }
    }

    private Long findTranslationOwner(Connection conn, ExcelRow row, ImportTarget target, Long sourceId)
            throws SQLException {
        String where = String.join(" = ? AND ", target.keys) + " = ?";
        // Hold the owner lock through the translation update or insert.
        String sql = "SELECT " + target.idColumn + " FROM " + target.table + " WHERE " + where
                + " WITH RS USE AND KEEP UPDATE LOCKS";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < target.keys.size(); i++) {
                String key = target.keys.get(i);
                if (key.equals(target.idColumn)) {
                    ps.setLong(i + 1, sourceId);
                } else {
                    ps.setString(i + 1, row.identifiers.get(key));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long id = rs.getLong(1);
                if (rs.next()) {
                    throw new SQLException(target.table + " 中存在多个匹配记录");
                }
                return id;
            }
        }
    }

    private Map<String, Object> writeTranslation(Connection conn, long ownerId, String langCode, String value,
                                                 boolean base, ImportTarget target) throws SQLException {
        String table = base ? target.table : "L_" + target.table;
        String idColumn = base ? target.idColumn : "L_" + target.idColumn;
        String where = base ? idColumn + " = ?" : "OWNERID = ? AND LANGCODE = ?";
        String update = "UPDATE " + table + " SET " + target.valueColumn + " = ? WHERE " + where;
        int affected;
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setString(1, value);
            ps.setLong(2, ownerId);
            if (!base) {
                ps.setString(3, langCode);
            }
            affected = ps.executeUpdate();
        }
        if (affected == 1) {
            return languageDetail(langCode, table, "success", base ? ownerId : null, idColumn, "更新成功");
        }
        if (base || affected != 0) {
            throw new SQLException(table + " 更新了 " + affected + " 条记录，预期为 1 条");
        }
        String insert = "INSERT INTO " + table + " (" + idColumn + ", OWNERID, LANGCODE, " + target.valueColumn
                + ") VALUES (NEXT VALUE FOR " + target.sequence + ", ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setLong(1, ownerId);
            ps.setString(2, langCode);
            ps.setString(3, value);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("新增 " + table + " 记录失败");
            }
        }
        Map<String, Object> detail = languageDetail(langCode, table, "success", null, idColumn, "新增成功");
        detail.put("_operation", "created");
        return detail;
    }

    enum ImportTarget {
        MESSAGE("MAXMESSAGES", "MAXMESSAGESID", "VALUE", null, "MSGGROUP", "MSGKEY"),
        ATTRIBUTE("MAXATTRIBUTECFG", "MAXATTRIBUTEID", null, null, "OBJECTNAME", "ATTRIBUTENAME"),
        DOMAIN("ALNDOMAIN", "ALNDOMAINID", "DESCRIPTION", "L_ALNDOMAINUSEQ", "DOMAINID", "VALUE", "VALUEID"),
        SYNONYM("SYNONYMDOMAIN", "SYNONYMDOMAINID", "DESCRIPTION", "L_SYNONYMDOMAINUSEQ",
                "DOMAINID", "MAXVALUE", "VALUE", "SYNONYMDOMAINID"),
        APP("MAXAPPS", "MAXAPPSID", "DESCRIPTION", "L_MAXAPPSUSEQ", "APP"),
        MODULE("MAXMODULES", "MAXMODULESID", "DESCRIPTION", "L_MAXMODULESUSEQ", "MODULE"),
        MENU("MAXMENU", "MAXMENUID", "HEADERDESCRIPTION", "L_MAXMENUUSEQ", "MAXMENUID", "KEYVALUE"),
        SIGOPTION("SIGOPTION", "SIGOPTIONID", "DESCRIPTION", "L_SIGOPTIONUSEQ", "APP", "OPTIONNAME");

        final String table;
        final String idColumn;
        final String valueColumn;
        final String sequence;
        final List<String> keys;

        ImportTarget(String table, String idColumn, String valueColumn, String sequence, String... keys) {
            this.table = table;
            this.idColumn = idColumn;
            this.valueColumn = valueColumn;
            this.sequence = sequence;
            this.keys = List.of(keys);
        }
    }

    private Sheet findExcelSheet(Workbook workbook, ImportTarget target, Pattern languagePattern,
                                 DataFormatter formatter, FormulaEvaluator evaluator) {
        // APP alone also appears in other import templates, so require the application ID header.
        List<String> requiredHeaders = target == ImportTarget.APP ? List.of("APP", target.idColumn) : target.keys;
        List<Sheet> matchingKeys = new ArrayList<>();
        List<Sheet> matchingTranslations = new ArrayList<>();
        for (Sheet sheet : workbook) {
            Row header = sheet.getRow(0);
            if (header == null) {
                continue;
            }
            Set<String> names = new HashSet<>();
            for (Cell cell : header) {
                String name = readCell(header, cell.getColumnIndex(), formatter, evaluator, true);
                names.add(name.toUpperCase(Locale.ROOT));
            }
            if (names.containsAll(requiredHeaders)) {
                matchingKeys.add(sheet);
                if (names.stream().anyMatch(name -> languagePattern.matcher(name).matches())) {
                    matchingTranslations.add(sheet);
                }
            }
        }
        if (matchingKeys.isEmpty()) {
            throw new IllegalArgumentException("Excel 中没有首行表头包含 "
                    + String.join("、", requiredHeaders) + " 的工作表");
        }
        List<Sheet> matches = matchingTranslations.isEmpty() ? matchingKeys : matchingTranslations;
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Excel 中有多个匹配表头的工作表："
                    + String.join("、", matches.stream().map(Sheet::getSheetName).toList())
                    + "。请保留本次要导入的工作表后重新上传");
        }
        return matches.get(0);
    }
}
