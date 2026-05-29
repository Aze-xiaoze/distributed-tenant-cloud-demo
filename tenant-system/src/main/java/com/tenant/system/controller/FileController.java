package com.tenant.system.controller;

import com.tenant.common.util.FileUploadValidatorUtil;
import com.tenant.common.vo.ResultVO;
import com.tenant.core.storage.FileStorageService;
import io.minio.StatObjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件管理控制器
 * <p>提供文件上传、下载、删除、预览等接口，基于 MinIO 实现
 * <p>租户隔离：FileStorageService 自动按租户 Bucket 隔离存储
 * <p>接口前缀：{@code /file}（经网关 StripPrefix=1 后映射为 {@code /system/file/**}）
 *
 * @author Aze
 */
@RestController
@RequestMapping("/file")
@Tag(name = "文件管理", description = "文件上传、下载、删除接口")
public class FileController {

    @Autowired(required = false)
    private FileStorageService fileStorageService;

    /**
     * 上传文件
     *
     * @param file     上传的文件
     * @param category 文件分类（如 avatar、document、attachment），默认 general
     * @return 上传结果，包含文件路径和访问URL
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传文件到MinIO，自动按租户隔离存储")
    public ResultVO<Map<String, Object>> upload(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件分类") @RequestParam(value = "category", defaultValue = "general") String category) {
        if (fileStorageService == null) {
            return ResultVO.error("文件存储服务未配置");
        }

        String validateMsg = FileUploadValidatorUtil.validate(file);
        if (validateMsg != null) {
            return ResultVO.error(validateMsg);
        }

        try {
            String objectName = fileStorageService.uploadFile(file, category);
            String fileUrl = fileStorageService.getFileUrl(objectName);

            Map<String, Object> data = new HashMap<>();
            data.put("objectName", objectName);
            data.put("fileName", file.getOriginalFilename());
            data.put("fileSize", file.getSize());
            data.put("contentType", file.getContentType());
            data.put("fileUrl", fileUrl);

            return ResultVO.success("上传成功", data);
        } catch (IllegalArgumentException e) {
            return ResultVO.error(e.getMessage());
        } catch (Exception e) {
            return ResultVO.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param objectName 文件存储路径
     * @return 文件二进制流
     */
    @GetMapping("/download")
    @Operation(summary = "下载文件", description = "根据文件路径下载文件")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "文件存储路径") @RequestParam String objectName) {
        if (fileStorageService == null) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = fileStorageService.downloadFile(objectName)) {
            StatObjectResponse fileInfo = fileStorageService.getFileInfo(objectName);
            byte[] bytes = inputStream.readAllBytes();

            String encodedFilename = URLEncoder.encode(
                    objectName.substring(objectName.lastIndexOf("/") + 1), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileInfo.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取文件预览 URL（预签名）
     *
     * @param objectName 文件存储路径
     * @return 预签名访问 URL
     */
    @GetMapping("/preview-url")
    @Operation(summary = "获取文件预览URL", description = "获取文件临时访问URL（预签名，1小时有效）")
    public ResultVO<String> getPreviewUrl(
            @Parameter(description = "文件存储路径") @RequestParam String objectName) {
        if (fileStorageService == null) {
            return ResultVO.error("文件存储服务未配置");
        }

        try {
            String url = fileStorageService.getFileUrl(objectName);
            return ResultVO.success(url);
        } catch (Exception e) {
            return ResultVO.error("获取文件URL失败：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param objectName 文件存储路径
     * @return 操作结果
     */
    @DeleteMapping
    @Operation(summary = "删除文件", description = "根据文件路径删除文件")
    public ResultVO<String> delete(
            @Parameter(description = "文件存储路径") @RequestParam String objectName) {
        if (fileStorageService == null) {
            return ResultVO.error("文件存储服务未配置");
        }

        try {
            fileStorageService.deleteFile(objectName);
            return ResultVO.success("删除成功");
        } catch (Exception e) {
            return ResultVO.error("文件删除失败：" + e.getMessage());
        }
    }
}
