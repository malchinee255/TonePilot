package com.tonepilot.service;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {

    StoredFile storeImage(MultipartFile file, String folder);

    String writeTextFile(String folder, String fileName, String content);

    String writeBinaryFile(String folder, String fileName, byte[] bytes, String contentType);

    StoredObject readObject(String fileUrl);

    String readAsDataUrl(String fileUrl);

    String slug(String value);
}
