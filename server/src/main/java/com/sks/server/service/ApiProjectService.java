package com.sks.server.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.noear.solon.annotation.Component;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Arrays;

@Component
public class ApiProjectService {

    

    private static final String BASE_DIR = "E:/gitwork/maximo-manager-panel/server/data/apiprojects";
    private static final String GLOBAL_DIR = BASE_DIR + File.separator + "global";
    private static final String PROJECT_FILE = "projects.json";
    private static final String COLLECTION_FILE = "collection.json";

    private void createDirectory(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            return;
        }
        boolean created = file.mkdirs();
        if (!created && !file.exists()) {
            throw new IOException("无法创建目录: " + path);
        }
    }

    public List<Map<String, Object>> listProjects(String user) {
        List<Map<String, Object>> projects = new ArrayList<>();
        
        File globalDir = new File(GLOBAL_DIR);
        if (globalDir.exists() && globalDir.isDirectory()) {
            File[] projectDirs = globalDir.listFiles(File::isDirectory);
            if (projectDirs != null) {
                for (File projectDir : projectDirs) {
                    File projectFile = new File(projectDir, PROJECT_FILE);
                    File collectionFile = new File(projectDir, COLLECTION_FILE);
                    if (projectFile.exists()) {
                        try {
                            String content = new String(Files.readAllBytes(projectFile.toPath()));
                            JSONObject json = JSON.parseObject(content);
                            JSONArray items = json.getJSONArray("items");
                            if (items != null && items.size() > 0) {
                                JSONObject item = items.getJSONObject(0);
                                Map<String, Object> project = new LinkedHashMap<>();
                                project.put("id", item.getString("id"));
                                project.put("name", item.getString("name"));
                                project.put("description", item.getString("description"));
                                project.put("type", "global");
                                project.put("user", "global");
                                
                                if (collectionFile.exists()) {
                                    String collectionContent = new String(Files.readAllBytes(collectionFile.toPath()));
                                    JSONObject collectionJson = JSON.parseObject(collectionContent);
                                    JSONArray requests = collectionJson.getJSONArray("requests");
                                    project.put("requestCount", requests != null ? requests.size() : 0);
                                } else {
                                    project.put("requestCount", 0);
                                }
                                
                                projects.add(project);
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
        }

        String userDir = BASE_DIR + File.separator + user;
        File userDirFile = new File(userDir);
        if (userDirFile.exists() && userDirFile.isDirectory()) {
            File[] projectDirs = userDirFile.listFiles(File::isDirectory);
            if (projectDirs != null) {
                for (File projectDir : projectDirs) {
                    File projectFile = new File(projectDir, PROJECT_FILE);
                    File collectionFile = new File(projectDir, COLLECTION_FILE);
                    if (projectFile.exists()) {
                        try {
                            String content = new String(Files.readAllBytes(projectFile.toPath()));
                            JSONObject json = JSON.parseObject(content);
                            JSONArray items = json.getJSONArray("items");
                            if (items != null && items.size() > 0) {
                                JSONObject item = items.getJSONObject(0);
                                Map<String, Object> project = new LinkedHashMap<>();
                                project.put("id", item.getString("id"));
                                project.put("name", item.getString("name"));
                                project.put("description", item.getString("description"));
                                project.put("type", "user");
                                project.put("user", user);
                                
                                if (collectionFile.exists()) {
                                    String collectionContent = new String(Files.readAllBytes(collectionFile.toPath()));
                                    JSONObject collectionJson = JSON.parseObject(collectionContent);
                                    JSONArray requests = collectionJson.getJSONArray("requests");
                                    project.put("requestCount", requests != null ? requests.size() : 0);
                                } else {
                                    project.put("requestCount", 0);
                                }
                                
                                projects.add(project);
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
        }

        return projects;
    }

    public Map<String, Object> getProject(String user, String projectId) {
        String projectPath = findProjectPath(user, projectId);
        if (projectPath == null) {
            return null;
        }

        File collectionFile = new File(projectPath, COLLECTION_FILE);
        if (!collectionFile.exists()) {
            return null;
        }

        try {
            String content = new String(Files.readAllBytes(collectionFile.toPath()));
            JSONObject json = JSON.parseObject(content);
            
            Map<String, Object> project = new LinkedHashMap<>();
            project.put("id", projectId);
            project.put("name", json.getString("name"));
            project.put("description", json.getString("description"));
            project.put("folders", processFolders(json.getJSONArray("folders")));
            project.put("requests", processRequests(json.getJSONArray("requests")));
            
            return project;
        } catch (Exception e) {
            throw new RuntimeException("读取项目失败: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> copyProject(String user, String sourceProjectId, String newName) {
        return copyProject(user, sourceProjectId, newName, false);
    }

    public Map<String, Object> copyProject(String user, String sourceProjectId, String newName, boolean toGlobal) {
        String sourcePath = findProjectPath("global", sourceProjectId);
        if (sourcePath == null) {
            sourcePath = findProjectPath(user, sourceProjectId);
            if (sourcePath == null) {
                throw new RuntimeException("源项目不存在");
            }
        }

        String targetDir = toGlobal ? GLOBAL_DIR : BASE_DIR + File.separator + user;
        File targetDirFile = new File(targetDir);
        if (!targetDirFile.exists()) {
            targetDirFile.mkdirs();
        }

        String newProjectId = UUID.randomUUID().toString();
        String destPath = targetDir + File.separator + newProjectId;
        File destDir = new File(destPath);
        if (destDir.exists()) {
            throw new RuntimeException("项目已存在");
        }

        try {
            copyDirectory(new File(sourcePath), destDir);
            
            File projectFile = new File(destPath, PROJECT_FILE);
            if (projectFile.exists()) {
                String content = new String(Files.readAllBytes(projectFile.toPath()));
                JSONObject json = JSON.parseObject(content);
                JSONArray items = json.getJSONArray("items");
                if (items != null && items.size() > 0) {
                    JSONObject item = items.getJSONObject(0);
                    item.put("id", newProjectId);
                    item.put("name", newName);
                    Files.write(projectFile.toPath(), json.toJSONString().getBytes());
                }
            }

            File collectionFile = new File(destPath, COLLECTION_FILE);
            if (collectionFile.exists()) {
                String content = new String(Files.readAllBytes(collectionFile.toPath()));
                JSONObject json = JSON.parseObject(content);
                json.put("id", newProjectId);
                json.put("name", newName);
                Files.write(collectionFile.toPath(), json.toJSONString().getBytes());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", newProjectId);
            result.put("name", newName);
            result.put("type", toGlobal ? "global" : "user");
            result.put("user", toGlobal ? "global" : user);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("复制项目失败: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> updateProject(String user, String projectId, String name, String description) {
        String projectPath = findProjectPath(user, projectId);
        if (projectPath == null) {
            throw new RuntimeException("项目不存在");
        }

        try {
            File projectFile = new File(projectPath, PROJECT_FILE);
            if (projectFile.exists()) {
                String content = new String(Files.readAllBytes(projectFile.toPath()));
                JSONObject json = JSON.parseObject(content);
                JSONArray items = json.getJSONArray("items");
                if (items != null && items.size() > 0) {
                    JSONObject item = items.getJSONObject(0);
                    if (name != null) item.put("name", name);
                    if (description != null) item.put("description", description);
                    Files.write(projectFile.toPath(), json.toJSONString().getBytes());
                }
            }

            File collectionFile = new File(projectPath, COLLECTION_FILE);
            if (collectionFile.exists()) {
                String content = new String(Files.readAllBytes(collectionFile.toPath()));
                JSONObject json = JSON.parseObject(content);
                if (name != null) json.put("name", name);
                if (description != null) json.put("description", description);
                Files.write(collectionFile.toPath(), json.toJSONString().getBytes());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", projectId);
            result.put("name", name);
            result.put("description", description);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("更新项目失败: " + e.getMessage(), e);
        }
    }

    public void deleteProject(String user, String projectId) {
        String projectPath = findProjectPath(user, projectId);
        if (projectPath == null) {
            throw new RuntimeException("项目不存在");
        }

        try {
            deleteDirectory(new File(projectPath));
        } catch (Exception e) {
            throw new RuntimeException("删除项目失败: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> createProject(String user, String name, String description) {
        return createProject(user, name, description, false);
    }

    public Map<String, Object> createProject(String user, String name, String description, boolean isGlobal) {
        try {
            String targetDir = isGlobal ? GLOBAL_DIR : BASE_DIR + File.separator + user;
            File targetDirFile = new File(targetDir);
            if (!targetDirFile.exists()) {
                createDirectory(targetDir);
            }

            String projectId = UUID.randomUUID().toString();
            String projectPathStr = targetDir + File.separator + projectId;
            File projectDir = new File(projectPathStr);
            if (!projectDir.exists()) {
                createDirectory(projectPathStr);
            }

            if (name == null || name.isEmpty()) {
                name = "未命名项目";
            }

            JSONObject projectJson = new JSONObject();
            JSONArray projectItems = new JSONArray();
            JSONObject projectItem = new JSONObject();
            projectItem.put("id", projectId);
            projectItem.put("name", name);
            projectItem.put("description", description);
            projectItems.add(projectItem);
            projectJson.put("items", projectItems);
            File projectFile = new File(projectDir, PROJECT_FILE);
            try (FileOutputStream fos = new FileOutputStream(projectFile)) {
                fos.write(projectJson.toJSONString().getBytes("UTF-8"));
            }

            JSONObject collectionJson = new JSONObject();
            collectionJson.put("id", projectId);
            collectionJson.put("name", name);
            collectionJson.put("description", description);
            collectionJson.put("folders", new JSONArray());
            collectionJson.put("requests", new JSONArray());
            File collectionFile = new File(projectDir, COLLECTION_FILE);
            try (FileOutputStream fos = new FileOutputStream(collectionFile)) {
                fos.write(collectionJson.toJSONString().getBytes("UTF-8"));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", projectId);
            result.put("name", name);
            result.put("description", description);
            result.put("type", isGlobal ? "global" : "user");
            result.put("user", isGlobal ? "global" : user);
            return result;
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("创建项目失败: ").append(e.getMessage());
            sb.append("\nStack Trace: ");
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append(element.toString()).append("; ");
            }
            throw new RuntimeException(sb.toString(), e);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("创建项目失败: ").append(e.getMessage());
            sb.append("\nStack Trace: ");
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append(element.toString()).append("; ");
            }
            throw new RuntimeException(sb.toString(), e);
        }
    }

    public Map<String, Object> importProject(String user, String jsonContent) {
        return importProject(user, jsonContent, false);
    }

    public Map<String, Object> importProject(String user, String jsonContent, boolean isGlobal) {
        try {
            JSONObject collection = JSON.parseObject(jsonContent);
            
            String targetDir = isGlobal ? GLOBAL_DIR : BASE_DIR + File.separator + user;
            File targetDirFile = new File(targetDir);
            if (!targetDirFile.exists()) {
                createDirectory(targetDir);
            }

            String projectId = UUID.randomUUID().toString();
            String projectPath = targetDir + File.separator + projectId;
            File projectDir = new File(projectPath);
            if (!projectDir.exists()) {
                createDirectory(projectPath);
            }

            String name = collection.getString("name");
            if (name == null || name.isEmpty()) {
                name = "未命名项目";
            }

            JSONObject projectJson = new JSONObject();
            JSONArray projectItems = new JSONArray();
            JSONObject projectItem = new JSONObject();
            projectItem.put("id", projectId);
            projectItem.put("name", name);
            projectItem.put("description", collection.getString("description"));
            projectItems.add(projectItem);
            projectJson.put("items", projectItems);
            File projectFile = new File(projectDir, PROJECT_FILE);
            try (FileOutputStream fos = new FileOutputStream(projectFile)) {
                fos.write(projectJson.toJSONString().getBytes("UTF-8"));
            }

            JSONObject outputCollection = new JSONObject();
            outputCollection.put("id", projectId);
            outputCollection.put("name", name);
            outputCollection.put("description", collection.getString("description"));

            JSONArray folders = new JSONArray();
            JSONArray requests = new JSONArray();

            if (collection.containsKey("openapi")) {
                convertOpenApiToRequests(collection, folders, requests);
            } else if (collection.containsKey("requests")) {
                requests = collection.getJSONArray("requests");
                if (collection.containsKey("folders")) {
                    folders = collection.getJSONArray("folders");
                }
            }

            outputCollection.put("folders", folders);
            outputCollection.put("requests", requests);

            File collectionFile = new File(projectDir, COLLECTION_FILE);
            try (FileOutputStream fos = new FileOutputStream(collectionFile)) {
                fos.write(outputCollection.toJSONString().getBytes("UTF-8"));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", projectId);
            result.put("name", name);
            result.put("description", outputCollection.getString("description"));
            result.put("type", isGlobal ? "global" : "user");
            result.put("user", isGlobal ? "global" : user);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("导入项目失败: " + e.getMessage(), e);
        }
    }

    private void convertOpenApiToRequests(JSONObject openapi, JSONArray folders, JSONArray requests) {
        JSONObject paths = openapi.getJSONObject("paths");
        if (paths == null) {
            return;
        }

        Map<String, String> tagFolderMap = new LinkedHashMap<>();
        JSONArray tags = openapi.getJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                JSONObject tag = tags.getJSONObject(i);
                String tagName = tag.getString("name");
                if (tagName != null && !tagName.isEmpty()) {
                    String folderId = UUID.randomUUID().toString();
                    tagFolderMap.put(tagName, folderId);
                    JSONObject folder = new JSONObject();
                    folder.put("id", folderId);
                    folder.put("name", tagName);
                    folders.add(folder);
                }
            }
        }

        for (String url : paths.keySet()) {
            JSONObject pathItem = paths.getJSONObject(url);
            if (pathItem == null) {
                continue;
            }

            for (String method : Arrays.asList("get", "post", "put", "delete", "patch")) {
                JSONObject operation = pathItem.getJSONObject(method);
                if (operation == null) {
                    continue;
                }

                JSONObject request = new JSONObject();
                request.put("id", UUID.randomUUID().toString());
                request.put("name", operation.getString("summary"));
                request.put("method", method.toUpperCase());
                request.put("url", url);

                JSONArray opTags = operation.getJSONArray("tags");
                if (opTags != null && opTags.size() > 0) {
                    String tagName = opTags.getString(0);
                    String folderId = tagFolderMap.get(tagName);
                    if (folderId == null) {
                        folderId = UUID.randomUUID().toString();
                        tagFolderMap.put(tagName, folderId);
                        JSONObject folder = new JSONObject();
                        folder.put("id", folderId);
                        folder.put("name", tagName);
                        folders.add(folder);
                    }
                    request.put("folderId", folderId);
                }

                JSONObject requestHeaders = new JSONObject();
                JSONObject requestParams = new JSONObject();

                JSONArray parameters = operation.getJSONArray("parameters");
                if (parameters != null) {
                    for (int i = 0; i < parameters.size(); i++) {
                        JSONObject param = parameters.getJSONObject(i);
                        String paramName = param.getString("name");
                        String paramIn = param.getString("in");
                        String example = param.getString("example");

                        if ("header".equalsIgnoreCase(paramIn)) {
                            requestHeaders.put(paramName, example != null ? example : "");
                        } else if ("query".equalsIgnoreCase(paramIn)) {
                            requestParams.put(paramName, example != null ? example : "");
                        }
                    }
                }

                if (requestHeaders.size() > 0) {
                    request.put("headers", requestHeaders);
                }
                if (requestParams.size() > 0) {
                    request.put("params", requestParams);
                }

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
                                    request.put("body", bodyObj);
                                }
                                break;
                            }
                        }
                    }
                }

                requests.add(request);
            }
        }
    }

    public String exportProject(String user, String projectId) {
        String projectPath = findProjectPath(user, projectId);
        if (projectPath == null) {
            throw new RuntimeException("项目不存在");
        }

        File collectionFile = new File(projectPath, COLLECTION_FILE);
        if (!collectionFile.exists()) {
            throw new RuntimeException("项目文件不存在");
        }

        try {
            return new String(Files.readAllBytes(collectionFile.toPath()));
        } catch (Exception e) {
            throw new RuntimeException("导出项目失败: " + e.getMessage(), e);
        }
    }

    private String findProjectPath(String user, String projectId) {
        File globalProjectDir = new File(GLOBAL_DIR, projectId);
        if (globalProjectDir.exists()) {
            return globalProjectDir.getAbsolutePath();
        }

        String userDir = BASE_DIR + File.separator + user;
        File userProjectDir = new File(userDir, projectId);
        if (userProjectDir.exists()) {
            return userProjectDir.getAbsolutePath();
        }

        return null;
    }

    private List<Map<String, Object>> processFolders(JSONArray folders) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (folders == null) {
            return result;
        }

        for (int i = 0; i < folders.size(); i++) {
            JSONObject folder = folders.getJSONObject(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", folder.getString("id"));
            item.put("name", folder.getString("name"));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> processRequests(JSONArray requests) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (requests == null) {
            return result;
        }

        for (int i = 0; i < requests.size(); i++) {
            JSONObject request = requests.getJSONObject(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", request.getString("id"));
            item.put("name", request.getString("name"));
            item.put("folderId", request.getString("folderId"));
            item.put("method", request.getString("method"));
            item.put("url", request.getString("url"));
            
            JSONObject headers = request.getJSONObject("headers");
            if (headers != null) {
                item.put("headers", headers);
            }
            
            JSONObject params = request.getJSONObject("params");
            if (params != null) {
                item.put("params", params);
            }
            
            JSONObject body = request.getJSONObject("body");
            if (body != null) {
                item.put("body", body);
            }

            result.add(item);
        }
        return result;
    }

    private void copyDirectory(File source, File dest) throws IOException {
        if (source.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdirs();
            }

            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(dest, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}
