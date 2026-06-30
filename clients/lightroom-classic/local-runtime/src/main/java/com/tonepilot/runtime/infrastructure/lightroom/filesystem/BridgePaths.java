package com.tonepilot.runtime.infrastructure.lightroom.filesystem;

import com.tonepilot.runtime.application.agent.*;
import com.tonepilot.runtime.application.config.*;
import com.tonepilot.runtime.application.lightroom.*;
import com.tonepilot.runtime.domain.agent.*;
import com.tonepilot.runtime.infrastructure.admin.*;
import com.tonepilot.runtime.infrastructure.config.*;
import com.tonepilot.runtime.infrastructure.lightroom.filesystem.*;
import com.tonepilot.runtime.infrastructure.lightroom.repository.*;
import com.tonepilot.runtime.infrastructure.model.*;
import com.tonepilot.runtime.infrastructure.observability.*;
import com.tonepilot.runtime.repository.lightroom.*;
import com.tonepilot.runtime.server.*;


import com.tonepilot.runtime.infrastructure.config.RuntimeProperties;

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
