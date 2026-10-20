package com.sks.server.service;

import com.sks.server.config.AppConfig;
import com.sks.server.model.PubFile;
import com.sks.server.util.FilenameUtils;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 公开文件存储服务：文件落地到 pubfile.docpath 目录，文件名按 FilenameUtils 规则清洗后直接作为存储名，
 * 同名时追加序号避免覆盖；列表信息全部由存储目录中的文件推导，无需元数据表/索引文件。
 */
@Component
public class PubFileService {

    private static final Logger log = LoggerFactory.getLogger(PubFileService.class);

    /** 公开访问路径（无需 apiKey，统一 /api/anon/ 前缀）；url-prefix 未配置时作为默认前缀 */
    private static final String PUBLIC_PATH = "/api/anon/pubfiles/";

    @Inject
    private AppConfig appConfig;

    /** 公开文件存储目录（不存在时自动创建） */
    public Path docDir() throws Exception {
        Path dir = Path.of(appConfig.getPubFileDocPath());
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            log.info("[pubfile] 存储目录不存在，已自动创建: {}", dir.toAbsolutePath());
        }
        log.debug("[pubfile] 存储目录: {} (pubfile.docpath={})", dir.toAbsolutePath(), appConfig.getPubFileDocPath());
        return dir;
    }

    /**
     * 保存上传的文件并返回文件记录
     */
    public PubFile save(UploadedFile file) throws Exception {
        Path dir = docDir();
        String originalName = file.getName() == null ? "unknown" : file.getName();
        // 清洗后的文件名直接作为存储名
        String storedName = FilenameUtils.sanitize(originalName);
        String extension = FilenameUtils.getExtension(storedName);
        String extSuffix = extension.isEmpty() ? "" : "." + extension;
        String baseName = FilenameUtils.getBaseName(storedName);

        Path target = dir.resolve(storedName);
        // 同名文件追加序号，避免覆盖已有文件
        int idx = 1;
        while (Files.exists(target)) {
            storedName = baseName + "(" + (idx++) + ")" + extSuffix;
            target = dir.resolve(storedName);
        }

        try (InputStream in = file.getContent()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("[pubfile] 上传文件: {} -> {}", originalName, target.toAbsolutePath());
        return toPubFile(target);
    }

    /**
     * 文件列表（按上传时间倒序）
     */
    public List<PubFile> list() throws Exception {
        Path dir = docDir();
        List<PubFile> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    result.add(toPubFile(path));
                } catch (Exception ignored) {
                    // 单个文件读取失败不影响整体列表
                }
            });
        }
        result.sort((a, b) -> Long.compare(b.getUploadTime(), a.getUploadTime()));
        log.info("[pubfile] 读取存储目录: {}，共 {} 个文件", dir.toAbsolutePath(), result.size());
        log.debug("[pubfile] 目录内文件: {}", joinNames(result));
        return result;
    }

    /** 拼接文件名用于日志输出 */
    private String joinNames(List<PubFile> files) {
        StringBuilder sb = new StringBuilder();
        for (PubFile file : files) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(file.getStoredName());
        }
        return sb.toString();
    }

    /**
     * 按存储文件名定位磁盘文件（校验路径，防止目录穿越）
     *
     * @return 文件路径；不存在或非法时返回 null
     */
    public Path resolve(String storedName) throws Exception {
        if (storedName == null || storedName.isEmpty()
                || storedName.contains("/") || storedName.contains("\\") || storedName.contains("..")) {
            return null;
        }
        Path dir = docDir().toAbsolutePath().normalize();
        Path target = dir.resolve(storedName).normalize();
        if (!target.startsWith(dir) || !Files.isRegularFile(target)) {
            log.warn("[pubfile] 文件不存在或路径非法: {} (存储目录 {})", storedName, dir);
            return null;
        }
        return target;
    }

    /**
     * 按扩展名推断 MIME 类型
     */
    public String contentTypeOf(String storedName) {
        String extension = FilenameUtils.getExtension(storedName);
        switch (extension) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "ico":
                return "image/x-icon";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain;charset=UTF-8";
            case "json":
                return "application/json;charset=UTF-8";
            case "zip":
                return "application/zip";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 按配置的 url-prefix 拼出公开访问 URL。前缀即完整的公开访问基址（已包含 /api/anon/pubfiles 路径），
     * 这里只在其后拼文件名；前缀为空时返回相对路径，由前端按当前站点/代理前缀补全。
     */
    public String buildUrl(String storedName) throws Exception {
        String encoded = URLEncoder.encode(storedName, "UTF-8").replace("+", "%20");
        String prefix = appConfig.getPubFileUrlPrefix();
        if (prefix == null || prefix.isEmpty()) {
            return PUBLIC_PATH + encoded;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix + "/" + encoded;
    }

    private PubFile toPubFile(Path path) throws Exception {
        String storedName = path.getFileName().toString();

        PubFile pubFile = new PubFile();
        pubFile.setId(storedName);
        // 存储名即清洗后的文件名，其余信息全部取自文件本身
        pubFile.setOriginalName(storedName);
        pubFile.setStoredName(storedName);
        pubFile.setSize(Files.size(path));
        pubFile.setExtension(FilenameUtils.getExtension(storedName));
        pubFile.setUploadTime(Files.getLastModifiedTime(path).toMillis());
        pubFile.setContentType(contentTypeOf(storedName));
        pubFile.setUrl(buildUrl(storedName));
        return pubFile;
    }
}
