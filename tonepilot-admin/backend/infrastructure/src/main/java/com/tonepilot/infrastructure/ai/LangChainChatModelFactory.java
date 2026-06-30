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







import org.springframework.beans.factory.annotation.Autowired;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

@Component
public class LangChainChatModelFactory {

    private final AiProperties properties;

    @Autowired
    public LangChainChatModelFactory(AiProperties properties) {
        this.properties = properties;
    }

    public ChatModel chatModel(boolean vision) {
        AiProperties.ProviderConfig config = properties.activeConfig();
        if (config == null) {
            throw new IllegalStateException("当前供应商为 rule，不需要创建 LangChain4j 模型");
        }

        String modelName = modelName(vision);
        validate(config, modelName);

        return OpenAiChatModel.builder()
                .baseUrl(trimRight(config.getBaseUrl(), "/"))
                .apiKey(config.getApiKey())
                .modelName(modelName)
                .temperature(properties.getTemperature())
                .build();
    }

    public String modelName(boolean vision) {
        AiProperties.ProviderConfig config = properties.activeConfig();
        if (config == null) {
            return "rule";
        }
        return vision && !isBlank(config.getVisionModel()) ? config.getVisionModel() : config.getChatModel();
    }

    private void validate(AiProperties.ProviderConfig config, String modelName) {
        if (isBlank(config.getBaseUrl())) {
            throw new IllegalArgumentException("大模型 base-url 未配置");
        }
        if (isBlank(config.getApiKey())) {
            throw new IllegalArgumentException("大模型 api-key 未配置");
        }
        if (isBlank(modelName)) {
            throw new IllegalArgumentException("大模型 model 未配置");
        }
    }

    private String trimRight(String value, String suffix) {
        String result = value;
        while (result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


