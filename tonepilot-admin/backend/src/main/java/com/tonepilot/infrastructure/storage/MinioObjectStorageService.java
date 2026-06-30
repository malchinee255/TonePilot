package com.tonepilot.infrastructure.storage;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.observability.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.common.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.config.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.shared.security.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.server.dto.*;


import org.springframework.beans.factory.annotation.Autowired;

import com.tonepilot.infrastructure.storage.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "tonepilot.storage", name = "type", havingValue = "minio")
public class MinioObjectStorageService implements ObjectStorageService {

    private final MinioClient minioClient;
    private final String bucket;
    private volatile boolean bucketReady = false;

    @Autowired
    public MinioObjectStorageService(StorageProperties properties) {
        StorageProperties.Minio minio = properties.getMinio();
        this.bucket = minio.getBucket();
        this.minioClient = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    @Override
    public StoredFile storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }
        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("image");
        String safeName = safeFileName(originalName);
        String extension = extensionOf(safeName);
        if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png")) {
            throw new IllegalArgumentException("MVP 阶段仅支持 JPG 和 PNG 文件");
        }

        String storedName = Instant.now().toEpochMilli() + "_" + safeName;
        String objectName = folder + "/" + storedName;
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(mediaType(storedName))
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("上传文件到 MinIO 失败", exception);
        }

        return new StoredFile(storedName, "/files/" + objectName, extension, null);
    }

    @Override
    public String writeTextFile(String folder, String fileName, String content) {
        String safeName = safeFileName(fileName);
        String objectName = folder + "/" + safeName;
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (InputStream inputStream = new java.io.ByteArrayInputStream(bytes)) {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, bytes.length, -1)
                    .contentType(mediaType(safeName))
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("写入文件到 MinIO 失败", exception);
        }
        return "/files/" + objectName;
    }

    @Override
    public String writeBinaryFile(String folder, String fileName, byte[] bytes, String contentType) {
        String safeName = safeFileName(fileName);
        String objectName = folder + "/" + safeName;
        try (InputStream inputStream = new java.io.ByteArrayInputStream(bytes)) {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, bytes.length, -1)
                    .contentType(contentType == null || contentType.isBlank() ? mediaType(safeName) : contentType)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("写入二进制文件到 MinIO 失败", exception);
        }
        return "/files/" + objectName;
    }

    @Override
    public StoredObject readObject(String fileUrl) {
        String objectName = objectNameFromUrl(fileUrl);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build())) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            inputStream.transferTo(outputStream);
            return new StoredObject(outputStream.toByteArray(), mediaType(objectName), objectName.substring(objectName.lastIndexOf('/') + 1));
        } catch (Exception exception) {
            throw new IllegalStateException("从 MinIO 读取文件失败", exception);
        }
    }

    @Override
    public String readAsDataUrl(String fileUrl) {
        StoredObject object = readObject(fileUrl);
        return "data:" + object.mediaType() + ";base64," + Base64.getEncoder().encodeToString(object.bytes());
    }

    @Override
    public String slug(String value) {
        String slug = Optional.ofNullable(value).orElse("tonepilot-preset")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "tonepilot-preset" : slug;
    }

    private void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            bucketReady = true;
        }
    }

    private String objectNameFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/files/")) {
            throw new IllegalArgumentException("文件地址必须以 /files/ 开头");
        }
        String objectName = fileUrl.substring("/files/".length());
        if (objectName.isBlank() || objectName.contains("..")) {
            throw new IllegalArgumentException("文件路径非法");
        }
        return objectName;
    }

    private String safeFileName(String name) {
        String normalized = name.replace('\\', '/');
        String lastSegment = normalized.substring(normalized.lastIndexOf('/') + 1);
        String safe = lastSegment.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        return safe.isBlank() ? "file" : safe;
    }

    private String extensionOf(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String mediaType(String name) {
        return switch (extensionOf(name)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "xmp" -> "application/xml";
            default -> "application/octet-stream";
        };
    }
}


