package com.tenant.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 文件上传安全校验工具
 * <p>提供文件类型白名单、大小限制、文件名安全校验能力
 * <p>防止上传可执行文件、脚本、 oversized 文件和路径遍历攻击
 *
 * @author Aze
 */
public class FileUploadValidator {

    /**
     * 默认文件大小限制：10 MB
     */
    public static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;

    /**
     * 允许上传的文件扩展名白名单
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "csv",
            "zip", "rar", "7z", "tar", "gz"
    );

    /**
     * 禁止上传的危险扩展名（即使伪装在白名单MIME中也会被拦截）
     */
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "php", "jsp", "asp", "aspx",
            "jar", "war", "ear", "class", "dll", "so", "py", "rb",
            "js", "vbs", "wsf", "msi", "scr", "com"
    );

    /**
     * 校验文件是否安全可上传
     *
     * @param file 上传的文件
     * @return 校验结果，通过返回null，不通过返回错误信息
     */
    public static String validate(MultipartFile file) {
        return validate(file, DEFAULT_MAX_SIZE);
    }

    /**
     * 校验文件是否安全可上传（指定大小限制）
     *
     * @param file    上传的文件
     * @param maxSize 最大允许大小（字节）
     * @return 校验结果，通过返回null，不通过返回错误信息
     */
    public static String validate(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) {
            return "上传文件不能为空";
        }

        // 1. 文件大小校验
        if (file.getSize() > maxSize) {
            return "文件大小超过限制，最大允许 " + (maxSize / 1024 / 1024) + " MB";
        }

        // 2. 原始文件名校验（防止路径遍历）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return "文件名不能为空";
        }

        // 去除路径分隔符，防止路径遍历（如 ../../etc/passwd）
        String safeFilename = originalFilename.replaceAll(".*[/\\\\]", "");
        if (safeFilename.isBlank()) {
            return "文件名不合法";
        }

        // 3. 扩展名校验
        String ext = getExtension(safeFilename).toLowerCase();
        if (ext.isEmpty()) {
            return "文件缺少扩展名，无法识别文件类型";
        }

        if (DANGEROUS_EXTENSIONS.contains(ext)) {
            return "禁止上传可执行文件或脚本（." + ext + "）";
        }

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return "不支持的文件类型（." + ext + "），请上传常见文档或图片格式";
        }

        // 4. MIME类型简单校验（如果客户端提供了）
        String contentType = file.getContentType();
        if (contentType != null && isDangerousMimeType(contentType)) {
            return "不安全的文件类型：" + contentType;
        }

        return null;
    }

    /**
     * 获取文件扩展名（不含点号）
     */
    private static String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    /**
     * 判断MIME类型是否为危险类型
     */
    private static boolean isDangerousMimeType(String mimeType) {
        String lower = mimeType.toLowerCase();
        return lower.contains("executable") || lower.contains("script")
                || lower.contains("java") || lower.contains("php")
                || lower.contains("x-msdownload") || lower.contains("octet-stream");
    }
}
