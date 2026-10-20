package com.sks.server.util;

import java.util.HashSet;
import java.util.Set;

/**
 * 文件名清洗工具类
 * 参考 Maximo 的 removeSpaces() + invalidAttachmentFilename.properties 逻辑，
 * 与 web-uploadfile-demo/solonFileManager 中的 FilenameUtils 保持一致。
 */
public class FilenameUtils {

    private static final Set<Character> INVALID_CHARS = new HashSet<>();
    private static final Set<Character> INVALID_LEADING_CHARS = new HashSet<>();

    static {
        String chars = "!,$,#,?,:,*,,\\,/,<,>,|,`,;";
        for (String c : chars.split(",")) {
            String trimmed = c.trim();
            if (!trimmed.isEmpty()) {
                INVALID_CHARS.add(trimmed.charAt(0));
            }
        }
        String leadingChars = ".,-";
        for (String c : leadingChars.split(",")) {
            String trimmed = c.trim();
            if (!trimmed.isEmpty()) {
                INVALID_LEADING_CHARS.add(trimmed.charAt(0));
            }
        }
    }

    /**
     * 清洗文件名，移除空格和非法字符
     *
     * @param original 原始文件名
     * @return 清洗后的文件名，清洗后为空时返回 unknown
     */
    public static String sanitize(String original) {
        if (original == null || original.isEmpty()) {
            return "unknown";
        }

        // 1. 去除所有空白字符（参考 Maximo removeSpaces: replaceAll "[\\p{Z}\\s]"）
        String name = original.replaceAll("[\\p{Z}\\s]", "");

        // 2. 去除非法字符
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!INVALID_CHARS.contains(c)) {
                sb.append(c);
            }
        }
        name = sb.toString();

        // 3. 去除非法开头字符
        while (!name.isEmpty() && INVALID_LEADING_CHARS.contains(name.charAt(0))) {
            name = name.substring(1);
        }

        // 4. 如果清洗后为空，返回占位名
        if (name.isEmpty()) {
            return "unknown";
        }

        return name;
    }

    /**
     * 提取文件扩展名（小写）
     */
    public static String getExtension(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < fileName.length() - 1) {
            return fileName.substring(dotIdx + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 获取文件名（不含扩展名）
     */
    public static String getBaseName(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            return fileName.substring(0, dotIdx);
        }
        return fileName;
    }
}
