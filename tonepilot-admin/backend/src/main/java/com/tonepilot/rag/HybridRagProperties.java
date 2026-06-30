package com.tonepilot.rag;

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
