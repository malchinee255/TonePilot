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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "tonepilot.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements ObjectStorageService {

    private final Path storageRoot;

    @Autowired
    public LocalFileStorageService(StorageProperties properties) {
        this.storageRoot = Path.of(properties.getRoot()).toAbsolutePath().normalize();
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
        Path directory = storageRoot.resolve(folder).normalize();
        Path target = directory.resolve(storedName).normalize();
        ensureInsideRoot(target);

        try {
            Files.createDirectories(directory);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new IllegalStateException("保存文件失败", exception);
        }

        return new StoredFile(storedName, "/files/" + folder + "/" + storedName, extension, target);
    }

    @Override
    public String writeTextFile(String folder, String fileName, String content) {
        String safeName = safeFileName(fileName);
        Path directory = storageRoot.resolve(folder).normalize();
        Path target = directory.resolve(safeName).normalize();
        ensureInsideRoot(target);
        try {
            Files.createDirectories(directory);
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("写入文件失败", exception);
        }
        return "/files/" + folder + "/" + safeName;
    }

    @Override
    public String writeBinaryFile(String folder, String fileName, byte[] bytes, String contentType) {
        String safeName = safeFileName(fileName);
        Path directory = storageRoot.resolve(folder).normalize();
        Path target = directory.resolve(safeName).normalize();
        ensureInsideRoot(target);
        try {
            Files.createDirectories(directory);
            Files.write(target, bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("写入二进制文件失败", exception);
        }
        return "/files/" + folder + "/" + safeName;
    }

    @Override
    public StoredObject readObject(String fileUrl) {
        Path target = resolveFileUrl(fileUrl);
        try {
            return new StoredObject(Files.readAllBytes(target), mediaType(target), target.getFileName().toString());
        } catch (IOException exception) {
            throw new IllegalStateException("读取文件失败", exception);
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

    private void ensureInsideRoot(Path target) {
        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("文件路径非法");
        }
    }

    private String safeFileName(String name) {
        String normalized = name.replace('\\', '/');
        String lastSegment = normalized.substring(normalized.lastIndexOf('/') + 1);
        String safe = lastSegment.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        return safe.isBlank() ? "file" : safe;
    }

    private Path resolveFileUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/files/")) {
            throw new IllegalArgumentException("文件地址必须以 /files/ 开头");
        }
        String relativePath = fileUrl.substring("/files/".length());
        Path target = storageRoot.resolve(relativePath).normalize();
        ensureInsideRoot(target);
        return target;
    }

    private String extensionOf(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String mediaType(Path path) {
        String extension = extensionOf(path.getFileName().toString());
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}


