package com.tonepilot.web.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "tonepilot.persistence.enabled=false",
        "tonepilot.rate-limit.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@AutoConfigureMockMvc
class AdminKnowledgeMaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsSourceImportsMaterialAndExtractsKnowledge() throws Exception {
        JsonNode sourceResponse = postJson("/api/admin/knowledge-sources", """
                {
                  "sourceType": "master_edit_record",
                  "title": "大师夜景调色记录",
                  "author": "摄影师 A",
                  "originalUrl": "https://example.com/edit-record",
                  "styleId": 2,
                  "notes": "记录一次城市夜景成片的全局参数"
                }
                """);
        long sourceId = sourceResponse.path("data").path("id").asLong();

        JsonNode materialResponse = postJson("/api/admin/knowledge-sources/" + sourceId + "/materials", """
                {
                  "materialType": "param_delta",
                  "title": "参数变化说明",
                  "content": "高光降低，阴影提升，蓝色饱和度略降，暗角增强。",
                  "language": "zh-CN"
                }
                """);
        long materialId = materialResponse.path("data").path("id").asLong();

        JsonNode jobResponse = postJson(
                "/api/admin/knowledge-sources/" + sourceId + "/materials/" + materialId + "/extract",
                "{}"
        );

        assertThat(jobResponse.path("success").asBoolean()).isTrue();
        assertThat(jobResponse.path("data").path("status").asText()).isEqualTo("succeeded");
        assertThat(jobResponse.path("data").path("generatedKnowledgeId").asLong()).isPositive();
    }

    private JsonNode postJson(String url, String body) throws Exception {
        String response = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }
}
