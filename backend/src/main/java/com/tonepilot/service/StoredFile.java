package com.tonepilot.service;

import java.nio.file.Path;

public record StoredFile(
        String fileName,
        String fileUrl,
        String fileType,
        Path path
) {
}
