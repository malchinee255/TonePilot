package com.tonepilot.service;

import com.tonepilot.storage.StorageProperties;
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
