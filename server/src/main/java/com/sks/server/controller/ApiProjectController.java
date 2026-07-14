package com.sks.server.controller;

import com.sks.server.service.ApiProjectService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.List;
import java.util.Map;

@Controller
public class ApiProjectController {

    @Inject
    private ApiProjectService apiProjectService;

    @Mapping(value = "/apiproject/list", method = MethodType.GET)
    public Map<String, Object> listProjects(@Param(defaultValue = "default") String user) {
        List<Map<String, Object>> projects = apiProjectService.listProjects(user);
        return Map.of("code", 200, "data", projects);
    }

    @Mapping(value = "/apiproject/get", method = MethodType.GET)
    public Map<String, Object> getProject(@Param(defaultValue = "default") String user, String projectId) {
        Map<String, Object> project = apiProjectService.getProject(user, projectId);
        if (project == null) {
            return Map.of("code", 404, "message", "项目不存在");
        }
        return Map.of("code", 200, "data", project);
    }

    @Mapping(value = "/apiproject/copy", method = MethodType.POST)
    public Map<String, Object> copyProject(@Param(defaultValue = "default") String user, String sourceProjectId, String newName, @Param(defaultValue = "false") boolean toGlobal) {
        try {
            Map<String, Object> result = apiProjectService.copyProject(user, sourceProjectId, newName, toGlobal);
            return Map.of("code", 200, "data", result);
        } catch (Exception e) {
            return Map.of("code", 500, "message", e.getMessage());
        }
    }

    @Mapping(value = "/apiproject/update", method = MethodType.POST)
    public Map<String, Object> updateProject(@Param(defaultValue = "default") String user, String projectId, String name, String description) {
        try {
            Map<String, Object> result = apiProjectService.updateProject(user, projectId, name, description);
            return Map.of("code", 200, "data", result);
        } catch (Exception e) {
            return Map.of("code", 500, "message", e.getMessage());
        }
    }

    @Mapping(value = "/apiproject/delete", method = MethodType.POST)
    public Map<String, Object> deleteProject(@Param(defaultValue = "default") String user, String projectId) {
        try {
            apiProjectService.deleteProject(user, projectId);
            return Map.of("code", 200, "message", "删除成功");
        } catch (Exception e) {
            return Map.of("code", 500, "message", e.getMessage());
        }
    }

    @Mapping(value = "/apiproject/create", method = MethodType.POST)
    public Map<String, Object> createProject(@Param(defaultValue = "default") String user, String name, String description, @Param(defaultValue = "false") boolean isGlobal) {
        try {
            Map<String, Object> result = apiProjectService.createProject(user, name, description, isGlobal);
            return Map.of("code", 200, "data", result);
        } catch (Exception e) {
            return Map.of("code", 500, "message", e.getMessage());
        }
    }

    @Mapping(value = "/apiproject/import", method = MethodType.POST)
    public Map<String, Object> importProject(@Param(defaultValue = "default") String user, @Param(defaultValue = "false") boolean isGlobal, org.noear.solon.core.handle.Context ctx) {
        try {
            String jsonContent = ctx.body();
            Map<String, Object> result = apiProjectService.importProject(user, jsonContent, isGlobal);
            return Map.of("code", 200, "data", result);
        } catch (Exception e) {
            return Map.of("code", 500, "message", e.getMessage());
        }
    }

    @Mapping(value = "/apiproject/export", method = MethodType.GET)
    public Map<String, Object> exportProject(@Param(defaultValue = "default") String user, String projectId) {
        try {
            String content = apiProjectService.exportProject(user, projectId);
            return Map.of("code", 200, "data", content);
        } catch (Exception e) {
            return Map.of("code", 500, "message", e.getMessage());
        }
    }
}
