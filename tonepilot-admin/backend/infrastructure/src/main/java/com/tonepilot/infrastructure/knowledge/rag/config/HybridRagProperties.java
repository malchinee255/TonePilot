package com.tonepilot.infrastructure.knowledge.rag.config;

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







import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tonepilot.rag.hybrid")
public class HybridRagProperties {

    private boolean enabled = true;
    private double keywordWeight = 0.45;
    private double vectorWeight = 0.45;
    private double rerankWeight = 0.10;
    private Embedding embedding = new Embedding();
    private VectorStore vectorStore = new VectorStore();
    private Rerank rerank = new Rerank();

    @Data
    public static class Embedding {
        private String provider = "openai";
        private String model = "text-embedding-3-small";
        private int dimensions = 1536;
    }

    @Data
    public static class VectorStore {
        private String type = "memory";
        private String milvusUri = "http://localhost:19530";
        private String collection = "tonepilot_knowledge";
    }

    @Data
    public static class Rerank {
        private String provider = "none";
        private String model = "";
    }
}
