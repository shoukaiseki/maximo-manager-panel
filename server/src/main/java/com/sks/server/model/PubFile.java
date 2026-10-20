package com.sks.server.model;

/**
 * 公开文件记录。
 * 记录信息全部由存储目录中的文件推导（文件名、大小、文件修改时间），无需额外的元数据表/索引文件；
 * url 由后端按 pubfile.url-prefix 配置生成。
 */
public class PubFile {

    /** 文件标识（等于存储文件名） */
    private String id;
    /** 清洗后的原始文件名 */
    private String originalName;
    /** 磁盘上的存储文件名 */
    private String storedName;
    /** 文件大小（字节） */
    private long size;
    /** MIME 类型（按扩展名推断） */
    private String contentType;
    /** 上传时间（毫秒） */
    private long uploadTime;
    /** 扩展名（小写，不含点） */
    private String extension;
    /** 公开访问 URL（后端生成，主页图片链接直接用） */
    private String url;

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getOriginalName() { return originalName; }

    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }

    public void setStoredName(String storedName) { this.storedName = storedName; }

    public long getSize() { return size; }

    public void setSize(long size) { this.size = size; }

    public String getContentType() { return contentType; }

    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getUploadTime() { return uploadTime; }

    public void setUploadTime(long uploadTime) { this.uploadTime = uploadTime; }

    public String getExtension() { return extension; }

    public void setExtension(String extension) { this.extension = extension; }

    public String getUrl() { return url; }

    public void setUrl(String url) { this.url = url; }
}
