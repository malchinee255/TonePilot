package com.tonepilot.runtime.bridge;

import com.tonepilot.runtime.config.RuntimeProperties;

import java.nio.file.Path;

public class BridgePaths {

    private final Path fsRoot;
    private final String lightroomRoot;

    public BridgePaths(RuntimeProperties properties) {
        this(Path.of(properties.getBridge().getRoot()), properties.getBridge().getLightroomRoot());
    }

    public BridgePaths(Path fsRoot, String lightroomRoot) {
        this.fsRoot = fsRoot;
        this.lightroomRoot = trimTrailingSeparator(lightroomRoot == null || lightroomRoot.isBlank()
                ? fsRoot.toString()
                : lightroomRoot);
    }

    public Path fs(String first, String... more) {
        return fsRoot.resolve(Path.of(first, more));
    }

    public String lightroom(String... segments) {
        String separator = usesWindowsSeparator(lightroomRoot) ? "\\" : "/";
        StringBuilder builder = new StringBuilder(lightroomRoot);
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            builder.append(separator).append(segment.replaceAll("^[\\\\/]+|[\\\\/]+$", ""));
        }
        return builder.toString();
    }

    public Path fsRoot() {
        return fsRoot;
    }

    public String lightroomRoot() {
        return lightroomRoot;
    }

    private boolean usesWindowsSeparator(String value) {
        return value.matches("^[a-zA-Z]:[\\\\/].*") || value.contains("\\");
    }

    private String trimTrailingSeparator(String value) {
        if (value.matches("^[a-zA-Z]:[\\\\/]?$")) {
            return value.replace("/", "\\");
        }
        return value.replaceAll("[\\\\/]+$", "");
    }
}
