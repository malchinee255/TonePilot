package com.tonepilot.service;

public record StoredObject(
        byte[] bytes,
        String mediaType,
        String fileName
) {
}
