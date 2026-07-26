package com.sks.server.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class ApiProjectService {

    @Inject("mysql")
    private DataSource mysqlDataSource;

    // ==================== 列表 ====================

    public List<Map<String, Object>> listProjects(String user) {
        List<Map<String, Object>> projects = new ArrayList<>();
        String sql = "SELECT p.id, p.name, p.description, p.type, p.user_name, " +
                "(SELECT COUNT(*) FROM api_request r WHERE r.project_id = p.id) AS request_count " +
                "FROM api_project p " +
                "ORDER BY p.updated_at DESC";

        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> project = new LinkedHashMap<>();
                    project.put("id", rs.getString("id"));
                    project.put("name", rs.getString("name"));
                    project.put("description", rs.getString("description"));
                    project.put("type", rs.getString("type"));
                    project.put("user", rs.getString("user_name"));
                    project.put("requestCount", rs.getInt("request_count"));
                    projects.add(project);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询项目列表失败: " + e.getMessage(), e);
        }
        return projects;
    }

    // ==================== 获取单个项目（含所有关联数据） ====================

    public Map<String, Object> getProject(String user, String projectId) {
        // 1. 查项目
        Map<String, Object> project = getProjectById(projectId);
        if (project == null) {
            return null;
        }

        // 2. 查文件夹
        project.put("folders", getFoldersByProject(projectId));

        // 3. 查请求（含 params / headers / body）
        project.put("requests", getRequestsByProject(projectId));

        return project;
    }

    private Map<String, Object> getProjectById(String projectId) {
        String sql = "SELECT id, name, description, type, user_name FROM api_project WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> project = new LinkedHashMap<>();
                    project.put("id", rs.getString("id"));
                    project.put("name", rs.getString("name"));
                    project.put("description", rs.getString("description"));
                    return project;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询项目失败: " + e.getMessage(), e);
        }
        return null;
    }

    private List<Map<String, Object>> getFoldersByProject(String projectId) {
        String sql = "SELECT id, name, parent_id FROM api_folder WHERE project_id = ? ORDER BY sort_order, created_at";
        List<Map<String, Object>> folders = new ArrayList<>();
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> folder = new LinkedHashMap<>();
                    folder.put("id", rs.getString("id"));
                    folder.put("name", rs.getString("name"));
                    String parentId = rs.getString("parent_id");
                    if (parentId != null) {
                        folder.put("parentId", parentId);
                    }
                    folders.add(folder);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询文件夹列表失败: " + e.getMessage(), e);
        }
        return folders;
    }

    private List<Map<String, Object>> getRequestsByProject(String projectId) {
        List<Map<String, Object>> requests = new ArrayList<>();
        String sql = "SELECT id, name, folder_id, method, url FROM api_request WHERE project_id = ? ORDER BY sort_order, created_at";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String reqId = rs.getString("id");
                    Map<String, Object> req = new LinkedHashMap<>();
                    req.put("id", reqId);
                    req.put("name", rs.getString("name"));
                    req.put("folderId", rs.getString("folder_id"));
                    req.put("method", rs.getString("method"));
                    req.put("url", rs.getString("url"));

                    // 查询 params
                    Map<String, Object> params = getParamsByRequest(reqId);
                    if (!params.isEmpty()) {
                        req.put("params", params);
                    }

                    // 查询 headers
                    Map<String, Object> headers = getHeadersByRequest(reqId);
                    if (!headers.isEmpty()) {
                        req.put("headers", headers);
                    }

                    // 查询 body
                    Map<String, Object> body = getBodyByRequest(reqId);
                    if (body != null) {
                        req.put("body", body);
                    }

                    requests.add(req);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询请求列表失败: " + e.getMessage(), e);
        }
        return requests;
    }

    private Map<String, Object> getParamsByRequest(String requestId) {
        Map<String, Object> params = new LinkedHashMap<>();
        String sql = "SELECT param_key, param_value FROM api_request_param WHERE request_id = ? AND enabled = 1";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    params.put(rs.getString("param_key"), rs.getString("param_value"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询请求参数失败: " + e.getMessage(), e);
        }
        return params;
    }

    private Map<String, Object> getHeadersByRequest(String requestId) {
        Map<String, Object> headers = new LinkedHashMap<>();
        String sql = "SELECT header_key, header_value FROM api_request_header WHERE request_id = ? AND enabled = 1";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    headers.put(rs.getString("header_key"), rs.getString("header_value"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询请求Header失败: " + e.getMessage(), e);
        }
        return headers;
    }

    private Map<String, Object> getBodyByRequest(String requestId) {
        String sql = "SELECT id, body_type, body_content FROM api_request_body WHERE request_id = ? LIMIT 1";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String bodyType = rs.getString("body_type");
                    long bodyId = rs.getLong("id");
                    String bodyContent = rs.getString("body_content");
                    Map<String, Object> body = new LinkedHashMap<>();

                    if ("json".equals(bodyType)) {
                        body.put("type", "json");
                        if (bodyContent != null && !bodyContent.isEmpty()) {
                            try {
                                body.put("json", JSON.parse(bodyContent));
                            } catch (Exception e) {
                                body.put("json", bodyContent);
                            }
                        }
                    } else if ("form-data".equals(bodyType)) {
                        body.put("formData", getBodyParams(bodyId));
                    } else if ("urlencoded".equals(bodyType)) {
                        body.put("urlEncoded", getBodyParams(bodyId));
                    } else {
                        return null;
                    }
                    return body;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询请求Body失败: " + e.getMessage(), e);
        }
        return null;
    }

    private List<Map<String, String>> getBodyParams(long bodyId) {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = "SELECT param_key, param_value FROM api_request_body_param WHERE body_id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bodyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("key", rs.getString("param_key"));
                    item.put("value", rs.getString("param_value"));
                    list.add(item);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询Body参数失败: " + e.getMessage(), e);
        }
        return list;
    }

    // ==================== 创建项目 ====================

    public Map<String, Object> createProject(String user, String name, String description) {
        return createProject(user, name, description, false);
    }

    public Map<String, Object> createProject(String user, String name, String description, boolean isGlobal) {
        try {
            if (name == null || name.isEmpty()) {
                name = "未命名项目";
            }
            String projectId = UUID.randomUUID().toString();
            String type = isGlobal ? "global" : "user";

            String sql = "INSERT INTO api_project (id, name, description, type, user_name) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = mysqlDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, projectId);
                ps.setString(2, name);
                ps.setString(3, description != null ? description : "");
                ps.setString(4, type);
                ps.setString(5, isGlobal ? "global" : user);
                ps.executeUpdate();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", projectId);
            result.put("name", name);
            result.put("description", description);
            result.put("type", type);
            result.put("user", isGlobal ? "global" : user);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("创建项目失败: " + e.getMessage(), e);
        }
    }

    // ==================== 更新项目 ====================

    public Map<String, Object> updateProject(String user, String projectId, String name, String description) {
        String sql = "UPDATE api_project SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description != null ? description : "");
            ps.setString(3, projectId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new RuntimeException("项目不存在");
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("更新项目失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", projectId);
        result.put("name", name);
        result.put("description", description);
        return result;
    }

    // ==================== 删除项目（级联） ====================

    public void deleteProject(String user, String projectId) {
        try (Connection conn = mysqlDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. 删除 body 参数
                deleteBodyParamsByProject(conn, projectId);
                // 2. 删除 body
                deleteBodiesByProject(conn, projectId);
                // 3. 删除 params
                String delParams = "DELETE rp FROM api_request_param rp " +
                        "INNER JOIN api_request r ON r.id = rp.request_id WHERE r.project_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(delParams)) {
                    ps.setString(1, projectId);
                    ps.executeUpdate();
                }
                // 4. 删除 headers
                String delHeaders = "DELETE rh FROM api_request_header rh " +
                        "INNER JOIN api_request r ON r.id = rh.request_id WHERE r.project_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(delHeaders)) {
                    ps.setString(1, projectId);
                    ps.executeUpdate();
                }
                // 5. 删除 requests
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM api_request WHERE project_id = ?")) {
                    ps.setString(1, projectId);
                    ps.executeUpdate();
                }
                // 6. 删除 folders
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM api_folder WHERE project_id = ?")) {
                    ps.setString(1, projectId);
                    ps.executeUpdate();
                }
                // 7. 删除 project
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM api_project WHERE id = ?")) {
                    ps.setString(1, projectId);
                    int affected = ps.executeUpdate();
                    if (affected == 0) {
                        throw new RuntimeException("项目不存在");
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("删除项目失败: " + e.getMessage(), e);
        }
    }

    private void deleteBodyParamsByProject(Connection conn, String projectId) throws SQLException {
        String sql = "DELETE bdp FROM api_request_body_param bdp " +
                "INNER JOIN api_request_body b ON b.id = bdp.body_id " +
                "INNER JOIN api_request r ON r.id = b.request_id WHERE r.project_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId);
            ps.executeUpdate();
        }
    }

    private void deleteBodiesByProject(Connection conn, String projectId) throws SQLException {
        String sql = "DELETE b FROM api_request_body b " +
                "INNER JOIN api_request r ON r.id = b.request_id WHERE r.project_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId);
            ps.executeUpdate();
        }
    }

    // ==================== 复制项目 ====================

    public Map<String, Object> copyProject(String user, String sourceProjectId, String newName) {
        return copyProject(user, sourceProjectId, newName, false);
    }

    public Map<String, Object> copyProject(String user, String sourceProjectId, String newName, boolean toGlobal) {
        // 读取原项目
        Map<String, Object> source = getProject("global", sourceProjectId);
        if (source == null) {
            source = getProject(user, sourceProjectId);
            if (source == null) {
                throw new RuntimeException("源项目不存在");
            }
        }

        String type = toGlobal ? "global" : "user";
        String newProjectId = UUID.randomUUID().toString();

        try (Connection conn = mysqlDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 创建新项目
                String insertProject = "INSERT INTO api_project (id, name, description, type, user_name) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertProject)) {
                    ps.setString(1, newProjectId);
                    ps.setString(2, newName);
                    ps.setString(3, (String) source.getOrDefault("description", ""));
                    ps.setString(4, type);
                    ps.setString(5, toGlobal ? "global" : user);
                    ps.executeUpdate();
                }

                // 复制文件夹（保持层级结构）
                Map<String, String> folderIdMap = new HashMap<>(); // oldId -> newId
                Map<String, String> oldParentMap = new HashMap<>(); // oldId -> oldParentId
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> folders = (List<Map<String, Object>>) source.getOrDefault("folders", Collections.emptyList());
                // 第一遍：插入所有文件夹（无 parent_id），记录映射关系
                String insertFolder = "INSERT INTO api_folder (id, project_id, name) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertFolder)) {
                    for (Map<String, Object> folder : folders) {
                        String oldId = (String) folder.get("id");
                        String newId = UUID.randomUUID().toString();
                        String oldParentId = (String) folder.get("parentId");
                        folderIdMap.put(oldId, newId);
                        if (oldParentId != null) {
                            oldParentMap.put(oldId, oldParentId);
                        }
                        ps.setString(1, newId);
                        ps.setString(2, newProjectId);
                        ps.setString(3, (String) folder.get("name"));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                // 第二遍：更新 parent_id
                if (!oldParentMap.isEmpty()) {
                    String updateParent = "UPDATE api_folder SET parent_id = ? WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateParent)) {
                        for (Map.Entry<String, String> entry : oldParentMap.entrySet()) {
                            String newId = folderIdMap.get(entry.getKey());
                            String newParentId = folderIdMap.get(entry.getValue());
                            if (newId != null && newParentId != null) {
                                ps.setString(1, newParentId);
                                ps.setString(2, newId);
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    }
                }

                // 复制请求
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> requests = (List<Map<String, Object>>) source.getOrDefault("requests", Collections.emptyList());
                for (Map<String, Object> req : requests) {
                    copyRequest(conn, newProjectId, folderIdMap, req);
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("复制项目失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", newProjectId);
        result.put("name", newName);
        result.put("type", type);
        result.put("user", toGlobal ? "global" : user);
        return result;
    }

    private void copyRequest(Connection conn, String newProjectId, Map<String, String> folderIdMap,
                              Map<String, Object> req) throws SQLException {
        String newReqId = UUID.randomUUID().toString();
        String oldFolderId = (String) req.get("folderId");
        String newFolderId = oldFolderId != null ? folderIdMap.get(oldFolderId) : null;

        // 插入 request
        String insertReq = "INSERT INTO api_request (id, project_id, folder_id, name, method, url) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertReq)) {
            ps.setString(1, newReqId);
            ps.setString(2, newProjectId);
            if (newFolderId != null) {
                ps.setString(3, newFolderId);
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, (String) req.getOrDefault("name", ""));
            ps.setString(5, (String) req.getOrDefault("method", "GET"));
            ps.setString(6, (String) req.getOrDefault("url", ""));
            ps.executeUpdate();
        }

        // 插入 params
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) req.get("params");
        if (params != null && !params.isEmpty()) {
            String insertParam = "INSERT INTO api_request_param (request_id, param_key, param_value, enabled) VALUES (?, ?, ?, 1)";
            try (PreparedStatement ps = conn.prepareStatement(insertParam)) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    ps.setString(1, newReqId);
                    ps.setString(2, entry.getKey());
                    ps.setString(3, entry.getValue() != null ? entry.getValue().toString() : "");
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        // 插入 headers
        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) req.get("headers");
        if (headers != null && !headers.isEmpty()) {
            String insertHeader = "INSERT INTO api_request_header (request_id, header_key, header_value, enabled) VALUES (?, ?, ?, 1)";
            try (PreparedStatement ps = conn.prepareStatement(insertHeader)) {
                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                    ps.setString(1, newReqId);
                    ps.setString(2, entry.getKey());
                    ps.setString(3, entry.getValue() != null ? entry.getValue().toString() : "");
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        // 插入 body
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) req.get("body");
        if (body != null && !body.isEmpty()) {
            copyRequestBody(conn, newReqId, body);
        }
    }

    private void copyRequestBody(Connection conn, String requestId, Map<String, Object> body) throws SQLException {
        String bodyType = null;
        String bodyContent = null;
        List<Map<String, String>> bodyParams = new ArrayList<>();

        if (body.containsKey("type") && "json".equals(body.get("type"))) {
            bodyType = "json";
            Object json = body.get("json");
            bodyContent = json != null ? JSON.toJSONString(json) : null;
        } else if (body.containsKey("formData")) {
            bodyType = "form-data";
            @SuppressWarnings("unchecked")
            List<Map<String, String>> formData = (List<Map<String, String>>) body.get("formData");
            if (formData != null) bodyParams = formData;
        } else if (body.containsKey("urlEncoded")) {
            bodyType = "urlencoded";
            @SuppressWarnings("unchecked")
            List<Map<String, String>> urlEncoded = (List<Map<String, String>>) body.get("urlEncoded");
            if (urlEncoded != null) bodyParams = urlEncoded;
        }

        if (bodyType == null) return;

        String insertBody = "INSERT INTO api_request_body (request_id, body_type, body_content) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertBody, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, requestId);
            ps.setString(2, bodyType);
            ps.setString(3, bodyContent);
            ps.executeUpdate();

            long bodyId;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    bodyId = rs.getLong(1);
                } else {
                    return;
                }
            }

            if (!bodyParams.isEmpty()) {
                String insertBp = "INSERT INTO api_request_body_param (body_id, param_key, param_value) VALUES (?, ?, ?)";
                try (PreparedStatement ps2 = conn.prepareStatement(insertBp)) {
                    for (Map<String, String> p : bodyParams) {
                        ps2.setLong(1, bodyId);
                        ps2.setString(2, p.get("key"));
                        ps2.setString(3, p.get("value"));
                        ps2.addBatch();
                    }
                    ps2.executeBatch();
                }
            }
        }
    }

    // ==================== 导入 ====================

    public Map<String, Object> importProject(String user, String jsonContent) {
        return importProject(user, jsonContent, false);
    }

    public Map<String, Object> importProject(String user, String jsonContent, boolean isGlobal) {
        try {
            JSONObject collection = JSON.parseObject(jsonContent);
            String type = isGlobal ? "global" : "user";
            String projectId = UUID.randomUUID().toString();
            String name;
            String description;

            try (Connection conn = mysqlDataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    Map<String, String> folderIdMap = new HashMap<>();

                    if (collection.containsKey("openapi")) {
                        // OpenAPI 格式
                        JSONObject info = collection.getJSONObject("info");
                        name = info != null ? info.getString("title") : null;
                        description = info != null ? info.getString("description") : null;
                        if (name == null || name.isEmpty()) name = "未命名项目";
                        if (description == null) description = "";

                        // 创建项目
                        createProjectRecord(conn, projectId, name, description, type, isGlobal, user);

                        // 转换 OpenAPI tags → folders（支持 / 分隔的多层目录）
                        JSONArray tags = collection.getJSONArray("tags");
                        if (tags != null) {
                            for (int i = 0; i < tags.size(); i++) {
                                JSONObject tag = tags.getJSONObject(i);
                                String tagName = tag.getString("name");
                                if (tagName != null && !tagName.isEmpty()) {
                                    // 使用 ensureFolderHierarchy 支持 a/b/c 多层目录
                                    String folderId = ensureFolderHierarchy(conn, projectId, tagName);
                                    if (folderId != null) {
                                        folderIdMap.put(tagName, folderId);
                                    }
                                }
                            }
                        }

                        // 转换 OpenAPI paths → requests
                        JSONObject paths = collection.getJSONObject("paths");
                        if (paths != null) {
                            for (String url : paths.keySet()) {
                                JSONObject pathItem = paths.getJSONObject(url);
                                if (pathItem == null) continue;
                                for (String method : Arrays.asList("get", "post", "put", "delete", "patch")) {
                                    JSONObject operation = pathItem.getJSONObject(method);
                                    if (operation == null) continue;
                                    importOpenApiRequest(conn, projectId, folderIdMap, operation, method, url);
                                }
                            }
                        }
                    } else {
                        // 标准 APIPost 格式（有 name、folders、requests）
                        name = collection.getString("name");
                        if (name == null || name.isEmpty()) name = "未命名项目";
                        description = collection.getString("description");
                        if (description == null) description = "";

                        // 创建项目
                        createProjectRecord(conn, projectId, name, description, type, isGlobal, user);

                        // 导入文件夹
                        JSONArray folders = collection.getJSONArray("folders");
                        if (folders != null) {
                            String insertFolder = "INSERT INTO api_folder (id, project_id, name) VALUES (?, ?, ?)";
                            try (PreparedStatement ps = conn.prepareStatement(insertFolder)) {
                                for (int i = 0; i < folders.size(); i++) {
                                    JSONObject folder = folders.getJSONObject(i);
                                    String oldId = folder.getString("id");
                                    String newId = UUID.randomUUID().toString();
                                    folderIdMap.put(oldId, newId);
                                    ps.setString(1, newId);
                                    ps.setString(2, projectId);
                                    ps.setString(3, folder.getString("name"));
                                    ps.addBatch();
                                }
                                ps.executeBatch();
                            }
                        }

                        // 导入请求
                        JSONArray requests = collection.getJSONArray("requests");
                        if (requests != null) {
                            for (int i = 0; i < requests.size(); i++) {
                                JSONObject req = requests.getJSONObject(i);
                                importRequest(conn, projectId, folderIdMap, req);
                            }
                        }
                    }

                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", projectId);
            result.put("name", name);
            result.put("description", description);
            result.put("type", type);
            result.put("user", isGlobal ? "global" : user);
            return result;
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("导入项目失败: " + e.getMessage(), e);
        }
    }

    private void createProjectRecord(Connection conn, String projectId, String name, String description,
                                       String type, boolean isGlobal, String user) throws SQLException {
        String insertProject = "INSERT INTO api_project (id, name, description, type, user_name) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertProject)) {
            ps.setString(1, projectId);
            ps.setString(2, name);
            ps.setString(3, description != null ? description : "");
            ps.setString(4, type);
            ps.setString(5, isGlobal ? "global" : user);
            ps.executeUpdate();
        }
    }

    /**
     * 创建层级文件夹，支持 / 分隔的多层路径
     * 返回最底层文件夹的 ID
     */
    private String ensureFolderHierarchy(Connection conn, String projectId, String fullName) throws SQLException {
        if (fullName == null || fullName.isEmpty()) return null;
        String[] parts = fullName.split("/");
        String parentId = null;
        String currentParentId = null;

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            parentId = currentParentId;

            // 查找同级是否已存在
            String findSql;
            if (parentId != null) {
                findSql = "SELECT id FROM api_folder WHERE project_id = ? AND parent_id = ? AND name = ?";
            } else {
                findSql = "SELECT id FROM api_folder WHERE project_id = ? AND parent_id IS NULL AND name = ?";
            }
            String existingId = null;
            try (PreparedStatement ps = conn.prepareStatement(findSql)) {
                ps.setString(1, projectId);
                if (parentId != null) {
                    ps.setString(2, parentId);
                    ps.setString(3, trimmed);
                } else {
                    ps.setString(2, trimmed);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getString("id");
                    }
                }
            }

            if (existingId != null) {
                currentParentId = existingId;
            } else {
                String newId = UUID.randomUUID().toString();
                String insertSql;
                if (parentId != null) {
                    insertSql = "INSERT INTO api_folder (id, project_id, parent_id, name) VALUES (?, ?, ?, ?)";
                } else {
                    insertSql = "INSERT INTO api_folder (id, project_id, name) VALUES (?, ?, ?)";
                }
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, newId);
                    ps.setString(2, projectId);
                    if (parentId != null) {
                        ps.setString(3, parentId);
                        ps.setString(4, trimmed);
                    } else {
                        ps.setString(3, trimmed);
                    }
                    ps.executeUpdate();
                }
                currentParentId = newId;
            }
        }
        return currentParentId;
    }

    private void importOpenApiRequest(Connection conn, String projectId, Map<String, String> folderIdMap,
                                       JSONObject operation, String method, String url) throws SQLException {
        String reqId = UUID.randomUUID().toString();
        String name = operation.getString("summary");
        if (name == null || name.isEmpty()) {
            name = method.toUpperCase() + " " + url;
        }

        // 确定文件夹（支持 / 分隔的多层目录）
        String folderId = null;
        JSONArray opTags = operation.getJSONArray("tags");
        if (opTags != null && opTags.size() > 0) {
            String tagName = opTags.getString(0);
            folderId = folderIdMap.get(tagName);
            if (folderId == null && tagName != null && !tagName.isEmpty()) {
                // 自动创建层级文件夹
                folderId = ensureFolderHierarchy(conn, projectId, tagName);
                if (folderId != null) {
                    folderIdMap.put(tagName, folderId);
                }
            }
        }

        // 插入 request
        String insertReq = "INSERT INTO api_request (id, project_id, folder_id, name, method, url) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertReq)) {
            ps.setString(1, reqId);
            ps.setString(2, projectId);
            if (folderId != null) {
                ps.setString(3, folderId);
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, name);
            ps.setString(5, method.toUpperCase());
            ps.setString(6, url);
            ps.executeUpdate();
        }

        // 提取 headers 和 params
        JSONObject requestHeaders = new JSONObject();
        JSONObject requestParams = new JSONObject();

        JSONArray parameters = operation.getJSONArray("parameters");
        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                JSONObject param = parameters.getJSONObject(i);
                String paramName = param.getString("name");
                String paramIn = param.getString("in");
                String example = param.getString("example");
                if (paramName == null || paramIn == null) continue;

                if ("header".equalsIgnoreCase(paramIn)) {
                    requestHeaders.put(paramName, example != null ? example : "");
                } else if ("query".equalsIgnoreCase(paramIn)) {
                    requestParams.put(paramName, example != null ? example : "");
                }
            }
        }

        // 保存 headers
        if (!requestHeaders.isEmpty()) {
            String insertHeader = "INSERT INTO api_request_header (request_id, header_key, header_value, enabled) VALUES (?, ?, ?, 1)";
            try (PreparedStatement ps = conn.prepareStatement(insertHeader)) {
                for (String key : requestHeaders.keySet()) {
                    ps.setString(1, reqId);
                    ps.setString(2, key);
                    ps.setString(3, requestHeaders.getString(key));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        // 保存 params
        if (!requestParams.isEmpty()) {
            String insertParam = "INSERT INTO api_request_param (request_id, param_key, param_value, enabled) VALUES (?, ?, ?, 1)";
            try (PreparedStatement ps = conn.prepareStatement(insertParam)) {
                for (String key : requestParams.keySet()) {
                    ps.setString(1, reqId);
                    ps.setString(2, key);
                    ps.setString(3, requestParams.getString(key));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        // 提取 body
        JSONObject requestBody = operation.getJSONObject("requestBody");
        if (requestBody != null) {
            JSONObject content = requestBody.getJSONObject("content");
            if (content != null) {
                for (String contentType : content.keySet()) {
                    if (contentType.contains("json")) {
                        JSONObject mediaType = content.getJSONObject(contentType);
                        Object example = mediaType.get("example");
                        if (example != null) {
                            JSONObject bodyObj = new JSONObject();
                            bodyObj.put("type", "json");
                            bodyObj.put("json", example);
                            importRequestBody(conn, reqId, bodyObj);
                        }
                        break;
                    }
                }
            }
        }
    }

    private void importRequest(Connection conn, String projectId, Map<String, String> folderIdMap,
                                JSONObject req) throws SQLException {
        String reqId = UUID.randomUUID().toString();
        String oldFolderId = req.getString("folderId");
        String newFolderId = oldFolderId != null ? folderIdMap.get(oldFolderId) : null;

        String insertReq = "INSERT INTO api_request (id, project_id, folder_id, name, method, url) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertReq)) {
            ps.setString(1, reqId);
            ps.setString(2, projectId);
            if (newFolderId != null) {
                ps.setString(3, newFolderId);
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, req.getString("name"));
            ps.setString(5, req.getString("method"));
            ps.setString(6, req.getString("url"));
            ps.executeUpdate();
        }

        // 导入 params
        JSONObject params = req.getJSONObject("params");
        if (params != null && !params.isEmpty()) {
            String insertParam = "INSERT INTO api_request_param (request_id, param_key, param_value, enabled) VALUES (?, ?, ?, 1)";
            try (PreparedStatement ps = conn.prepareStatement(insertParam)) {
                for (String key : params.keySet()) {
                    ps.setString(1, reqId);
                    ps.setString(2, key);
                    ps.setString(3, params.getString(key));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        // 导入 headers
        JSONObject headers = req.getJSONObject("headers");
        if (headers != null && !headers.isEmpty()) {
            String insertHeader = "INSERT INTO api_request_header (request_id, header_key, header_value, enabled) VALUES (?, ?, ?, 1)";
            try (PreparedStatement ps = conn.prepareStatement(insertHeader)) {
                for (String key : headers.keySet()) {
                    ps.setString(1, reqId);
                    ps.setString(2, key);
                    ps.setString(3, headers.getString(key));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        // 导入 body
        JSONObject body = req.getJSONObject("body");
        if (body != null && !body.isEmpty()) {
            importRequestBody(conn, reqId, body);
        }
    }

    private void importRequestBody(Connection conn, String requestId, JSONObject body) throws SQLException {
        String bodyType = body.getString("type");
        String bodyContent = null;
        List<Map<String, String>> bodyParams = new ArrayList<>();

        if ("json".equals(bodyType)) {
            Object json = body.get("json");
            bodyContent = json != null ? JSON.toJSONString(json) : null;
        } else if ("form-data".equals(bodyType)) {
            JSONArray formData = body.getJSONArray("formData");
            if (formData != null) {
                for (int i = 0; i < formData.size(); i++) {
                    JSONObject item = formData.getJSONObject(i);
                    Map<String, String> p = new LinkedHashMap<>();
                    p.put("key", item.getString("key"));
                    p.put("value", item.getString("value"));
                    bodyParams.add(p);
                }
            }
        } else if ("urlencoded".equals(bodyType)) {
            JSONArray urlEncoded = body.getJSONArray("urlEncoded");
            if (urlEncoded != null) {
                for (int i = 0; i < urlEncoded.size(); i++) {
                    JSONObject item = urlEncoded.getJSONObject(i);
                    Map<String, String> p = new LinkedHashMap<>();
                    p.put("key", item.getString("key"));
                    p.put("value", item.getString("value"));
                    bodyParams.add(p);
                }
            }
        }

        if (bodyType == null) return;

        String insertBody = "INSERT INTO api_request_body (request_id, body_type, body_content) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertBody, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, requestId);
            ps.setString(2, bodyType);
            ps.setString(3, bodyContent);
            ps.executeUpdate();

            long bodyId;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    bodyId = rs.getLong(1);
                } else {
                    return;
                }
            }

            if (!bodyParams.isEmpty()) {
                String insertBp = "INSERT INTO api_request_body_param (body_id, param_key, param_value) VALUES (?, ?, ?)";
                try (PreparedStatement ps2 = conn.prepareStatement(insertBp)) {
                    for (Map<String, String> p : bodyParams) {
                        ps2.setLong(1, bodyId);
                        ps2.setString(2, p.get("key"));
                        ps2.setString(3, p.get("value"));
                        ps2.addBatch();
                    }
                    ps2.executeBatch();
                }
            }
        }
    }

    // ==================== 导出 ====================

    public String exportProject(String user, String projectId) {
        Map<String, Object> project = getProject(user, projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        return JSON.toJSONString(project);
    }

    // ==================== OpenAPI 导入（通过 importProject 最终实现） ====================

    // OpenAPI 转换在 importProject 的 JSON 解析层由前端或上层处理，保持不变
}
