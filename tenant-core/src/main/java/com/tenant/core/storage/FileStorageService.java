package com.tenant.core.storage;

import com.tenant.core.config.MinioConfig;
import com.tenant.core.tenant.TenantContextHolder;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储服务
 * <p>基于 MinIO 实现租户隔离的文件上传、下载、删除功能
 * <p>租户隔离策略：
 * <ul>
 *   <li>每个租户拥有独立的 Bucket，命名规则：{@code tenant-{tenantCode}}</li>
 *   <li>Bucket 不存在时自动创建</li>
 *   <li>文件路径按日期分目录：{@code {category}/{yyyy-MM-dd}/{uuid}.{ext}}</li>
 * </ul>
 * <p>仅在 MinioClient Bean 存在时激活（ConditionalOnBean），未配置 MinIO 时不加载
 *
 * @author Aze
 */
@Slf4j
@Service
@ConditionalOnBean(MinioClient.class)
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public FileStorageService(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
    }

    /**
     * 上传文件
     * <p>自动创建租户 Bucket（如不存在），文件存储路径：{@code {category}/{date}/{uuid}.{ext}}
     *
     * @param file     上传的文件
     * @param category 文件分类（如 avatar、document、attachment）
     * @return 文件存储路径（相对路径，用于后续访问）
     * @throws Exception 上传异常
     */
    public String uploadFile(MultipartFile file, String category) throws Exception {
        // 校验文件大小
        if (file.getSize() > minioConfig.getMaxFileSize()) {
            throw new IllegalArgumentException("文件大小超过限制，最大允许 " +
                    (minioConfig.getMaxFileSize() / 1024 / 1024) + "MB");
        }

        String bucketName = resolveBucketName();
        ensureBucketExists(bucketName);

        // 生成文件存储路径：category/yyyy-MM-dd/uuid.ext
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String datePath = LocalDate.now().format(DATE_FORMAT);
        String objectName = category + "/" + datePath + "/" + UUID.randomUUID() +
                (extension.isEmpty() ? "" : "." + extension);

        // 上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        log.info("文件上传成功: bucket={}, object={}, size={}", bucketName, objectName, file.getSize());
        return objectName;
    }

    /**
     * 下载文件
     *
     * @param objectName 文件存储路径（uploadFile 返回的路径）
     * @return 文件输入流
     * @throws Exception 下载异常
     */
    public InputStream downloadFile(String objectName) throws Exception {
        String bucketName = resolveBucketName();

        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 获取文件访问 URL（预签名，默认1小时有效）
     *
     * @param objectName 文件存储路径
     * @return 预签名访问 URL
     * @throws Exception 生成 URL 异常
     */
    public String getFileUrl(String objectName) throws Exception {
        return getFileUrl(objectName, 3600);
    }

    /**
     * 获取文件访问 URL（预签名，指定有效期）
     *
     * @param objectName 文件存储路径
     * @param expiry     有效期（秒）
     * @return 预签名访问 URL
     * @throws Exception 生成 URL 异常
     */
    public String getFileUrl(String objectName, int expiry) throws Exception {
        String bucketName = resolveBucketName();

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expiry)
                        .build()
        );
    }

    /**
     * 删除文件
     *
     * @param objectName 文件存储路径
     * @throws Exception 删除异常
     */
    public void deleteFile(String objectName) throws Exception {
        String bucketName = resolveBucketName();

        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );

        log.info("文件删除成功: bucket={}, object={}", bucketName, objectName);
    }

    /**
     * 获取文件元信息
     *
     * @param objectName 文件存储路径
     * @return 文件元信息
     * @throws Exception 查询异常
     */
    public StatObjectResponse getFileInfo(String objectName) throws Exception {
        String bucketName = resolveBucketName();

        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    // ======================== 私有方法 ========================

    /**
     * 解析当前租户的 Bucket 名称
     * <p>命名规则：{@code tenant-{tenantCode}}，无租户上下文时使用默认 Bucket
     *
     * @return Bucket 名称
     */
    private String resolveBucketName() {
        String tenantId = TenantContextHolder.getCurrentTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            // Bucket 名称必须小写，且符合 DNS 命名规范
            return "tenant-" + tenantId.toLowerCase();
        }
        return minioConfig.getDefaultBucket();
    }

    /**
     * 确保 Bucket 存在，不存在则自动创建
     *
     * @param bucketName Bucket 名称
     * @throws Exception 操作异常
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            log.info("自动创建租户Bucket: {}", bucketName);
        }
    }

    /**
     * 提取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（不含点），无扩展名返回空串
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
