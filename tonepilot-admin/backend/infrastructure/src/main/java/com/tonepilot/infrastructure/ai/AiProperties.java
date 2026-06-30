package com.tonepilot.infrastructure.ai;

import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;







import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tonepilot.ai")
public class AiProperties {

    private String provider = "rule";
    private boolean fallbackEnabled = true;
    private double temperature = 0.2;
    private boolean responseFormatEnabled = false;
    private ProviderConfig openai = new ProviderConfig();
    private ProviderConfig qwen2 = new ProviderConfig();

    public String activeProvider() {
        String override = AiProviderContext.current();
        return normalize(override == null || override.isBlank() ? provider : override);
    }

    public ProviderConfig activeConfig() {
        return switch (activeProvider()) {
            case "openai" -> openai;
            case "qwen2" -> qwen2;
            case "rule" -> null;
            default -> throw new IllegalArgumentException("不支持的大模型供应商：" + activeProvider());
        };
    }

    public boolean modelEnabled() {
        return !"rule".equals(activeProvider());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "rule";
        }
        String normalized = value.trim().toLowerCase();
        if ("qwen".equals(normalized) || "ali".equals(normalized) || "aliyun".equals(normalized)) {
            return "qwen2";
        }
        return normalized;
    }

    @Getter
    @Setter
    public static class ProviderConfig {
        private String baseUrl;
        private String apiKey;
        private String chatModel;
        private String visionModel;
    }
}
