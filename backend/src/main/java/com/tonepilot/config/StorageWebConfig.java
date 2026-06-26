package com.tonepilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(prefix = "tonepilot.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class StorageWebConfig implements WebMvcConfigurer {

    private final String storageRoot;

    public StorageWebConfig(@Value("${tonepilot.storage.root}") String storageRoot) {
        this.storageRoot = storageRoot;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(storageRoot).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/files/**").addResourceLocations(location + "/");
    }
}
