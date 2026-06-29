# Color Material Ingestion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 TonePilot 管理端增加“调色素材导入”闭环，让管理员可以登记调色知识来源、导入教程文本/参数记录，并一键生成待审核的风格知识。

**Architecture:** 第一版沿用管理端现有的 Spring Boot + `InMemoryTonePilotStore` + `DomainSnapshotRepository` 轻量存储模式，新增知识来源、素材、抽取任务三个领域对象。管理端前端新增“素材导入”视图，负责创建来源、录入素材、触发抽取；真正进入运行时 RAG 前仍走已有知识审核流程。

**Tech Stack:** Java 17, Spring Boot 3.3, JUnit 5, AssertJ, Vue 3, Element Plus, Axios.

---

### Task 1: 后端素材导入领域模型与服务

**Files:**
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/domain/KnowledgeSource.java`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/domain/KnowledgeMaterial.java`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/domain/KnowledgeExtractionJob.java`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/web/dto/KnowledgeSourceRequest.java`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/web/dto/KnowledgeMaterialRequest.java`
- Modify: `tonepilot-admin/backend/src/main/java/com/tonepilot/store/InMemoryTonePilotStore.java`
- Modify: `tonepilot-admin/backend/src/main/java/com/tonepilot/service/StyleKnowledgeService.java`
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/service/KnowledgeMaterialIngestionService.java`
- Test: `tonepilot-admin/backend/src/test/java/com/tonepilot/service/KnowledgeMaterialIngestionServiceTest.java`

- [ ] **Step 1: Write the failing service test**

```java
@SpringBootTest(properties = {
        "tonepilot.persistence.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class KnowledgeMaterialIngestionServiceTest {

    @Autowired
    private KnowledgeMaterialIngestionService service;

    @Autowired
    private StyleKnowledgeService styleKnowledgeService;

    @Test
    void importsTextMaterialAndExtractsPendingStyleKnowledge() {
        KnowledgeSource source = service.createSource(new KnowledgeSourceRequest(
                "douyin_video",
                "夜景电影感调色教程",
                "调色博主",
                "https://www.douyin.com/video/example",
                1L,
                "用于沉淀夜景高光压制和暗部恢复经验"
        ));

        KnowledgeMaterial material = service.importMaterial(source.id(), new KnowledgeMaterialRequest(
                "transcript",
                "字幕摘要",
                "夜景照片先压高光，提一点阴影，绿色饱和度降低，整体做冷暖对比。",
                "zh-CN"
        ));

        KnowledgeExtractionJob job = service.extractToKnowledge(source.id(), material.id());

        assertThat(job.status()).isEqualTo("succeeded");
        assertThat(job.generatedKnowledgeId()).isNotNull();

        StyleKnowledge knowledge = styleKnowledgeService.get(job.generatedKnowledgeId());
        assertThat(knowledge.status()).isEqualTo("pending");
        assertThat(knowledge.title()).contains("夜景电影感调色教程");
        assertThat(knowledge.content()).contains("来源类型：douyin_video");
        assertThat(knowledge.content()).contains("夜景照片先压高光");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd tonepilot-admin/backend && mvn -Dtest=KnowledgeMaterialIngestionServiceTest test`

Expected: FAIL because `KnowledgeMaterialIngestionService` and related domain classes do not exist.

- [ ] **Step 3: Write minimal domain records**

Add records with these fields:
- `KnowledgeSource`: `id`, `sourceType`, `title`, `author`, `originalUrl`, `styleId`, `notes`, `status`, `createdAt`, `updatedAt`
- `KnowledgeMaterial`: `id`, `sourceId`, `materialType`, `title`, `content`, `language`, `createdAt`
- `KnowledgeExtractionJob`: `id`, `sourceId`, `materialId`, `status`, `generatedKnowledgeId`, `message`, `createdAt`, `updatedAt`

- [ ] **Step 4: Extend the in-memory store**

Add three ID counters and three maps:

```java
public final AtomicLong knowledgeSourceIds = new AtomicLong(1);
public final AtomicLong knowledgeMaterialIds = new AtomicLong(1);
public final AtomicLong knowledgeExtractionJobIds = new AtomicLong(1);

public final Map<Long, KnowledgeSource> knowledgeSources = new ConcurrentHashMap<>();
public final Map<Long, KnowledgeMaterial> knowledgeMaterials = new ConcurrentHashMap<>();
public final Map<Long, KnowledgeExtractionJob> knowledgeExtractionJobs = new ConcurrentHashMap<>();
```

- [ ] **Step 5: Add a draft knowledge save method**

Add `StyleKnowledgeService.createDraftFromMaterial(...)` so the ingestion service can create a pending `StyleKnowledge` without pretending it came from a style sample. The method must set `status` to `pending`, fill `embeddingId` with `local-material-<uuid>`, and preserve the material/source trace in `content`.

- [ ] **Step 6: Implement `KnowledgeMaterialIngestionService`**

The service must:
- validate source and material existence
- sort `listSources()` by `updatedAt` descending
- sort `listMaterials(sourceId)` by `createdAt` descending
- create an extraction job with `succeeded`
- convert transcript/manual text/parameter text into a pending `StyleKnowledge`
- save source/material/job snapshots through `DomainSnapshotRepository`

- [ ] **Step 7: Run test to verify it passes**

Run: `cd tonepilot-admin/backend && mvn -Dtest=KnowledgeMaterialIngestionServiceTest test`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add tonepilot-admin/backend/src/main/java tonepilot-admin/backend/src/test/java
git commit -m "feat: add knowledge material ingestion service"
git push
```

### Task 2: 后端管理端 API

**Files:**
- Create: `tonepilot-admin/backend/src/main/java/com/tonepilot/web/admin/AdminKnowledgeMaterialController.java`
- Test: `tonepilot-admin/backend/src/test/java/com/tonepilot/web/admin/AdminKnowledgeMaterialControllerTest.java`

- [ ] **Step 1: Write the failing controller test**

Use `@SpringBootTest` + `@AutoConfigureMockMvc` and verify:
- `POST /api/admin/knowledge-sources` creates a source
- `POST /api/admin/knowledge-sources/{sourceId}/materials` imports material
- `POST /api/admin/knowledge-sources/{sourceId}/materials/{materialId}/extract` returns a succeeded job

- [ ] **Step 2: Run test to verify it fails**

Run: `cd tonepilot-admin/backend && mvn -Dtest=AdminKnowledgeMaterialControllerTest test`

Expected: FAIL with 404 because the controller does not exist.

- [ ] **Step 3: Implement controller**

Expose:
- `GET /api/admin/knowledge-sources`
- `POST /api/admin/knowledge-sources`
- `GET /api/admin/knowledge-sources/{sourceId}/materials`
- `POST /api/admin/knowledge-sources/{sourceId}/materials`
- `POST /api/admin/knowledge-sources/{sourceId}/materials/{materialId}/extract`

Every method returns `ApiResponse.ok(...)`, follows existing `@CrossOrigin` style, and delegates all business logic to `KnowledgeMaterialIngestionService`.

- [ ] **Step 4: Run controller test**

Run: `cd tonepilot-admin/backend && mvn -Dtest=AdminKnowledgeMaterialControllerTest test`

Expected: PASS.

- [ ] **Step 5: Run backend regression**

Run: `cd tonepilot-admin/backend && mvn test`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add tonepilot-admin/backend/src/main/java tonepilot-admin/backend/src/test/java
git commit -m "feat: expose knowledge material ingestion api"
git push
```

### Task 3: 管理端前端素材导入页面

**Files:**
- Modify: `tonepilot-admin/frontend/src/App.vue`
- Modify: `tonepilot-admin/frontend/src/api.ts` only if the current wrapper needs new helpers

- [ ] **Step 1: Add UI state**

Add `knowledgeSources`, `selectedSourceId`, `materialList`, `sourceForm`, `materialForm`, and `extractingMaterialId` in `App.vue`. Defaults:
- source type: `douyin_video`
- material type: `transcript`
- language: `zh-CN`

- [ ] **Step 2: Add sidebar menu**

Add menu item:

```vue
<el-menu-item index="materials">
  <el-icon><Files /></el-icon>
  <span>素材导入</span>
</el-menu-item>
```

- [ ] **Step 3: Add two-panel material import view**

Left panel:
- source create form
- source type select with `douyin_video`, `master_edit_record`, `manual_note`, `style_sample`
- title, author, original URL, style select, notes

Right panel:
- selected source list
- material import form with type/title/content/language
- material list with extract button

- [ ] **Step 4: Wire API calls**

Add functions:
- `loadKnowledgeSources()`
- `createKnowledgeSource()`
- `selectKnowledgeSource(row)`
- `loadKnowledgeMaterials(sourceId)`
- `importKnowledgeMaterial()`
- `extractKnowledge(materialId)`

After extraction, call `loadAdminKnowledge()` and show a success toast saying the knowledge has entered the review pool.

- [ ] **Step 5: Build frontend**

Run: `cd tonepilot-admin/frontend && npm run build`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add tonepilot-admin/frontend/src/App.vue tonepilot-admin/frontend/src/api.ts
git commit -m "feat: add knowledge material import page"
git push
```

### Task 4: Documentation and final verification

**Files:**
- Modify: `TonePilot-scaffold/README.md`

- [ ] **Step 1: Document material ingestion**

Add a concise section:
- management-side path: `素材导入`
- supported first-phase material types
- flow: source -> material -> extract -> pending review -> approve -> runtime retrieval
- note: Douyin auto crawling is a later connector; this phase imports URL plus transcript/summary text.

- [ ] **Step 2: Run final checks**

Run:
- `cd tonepilot-admin/backend && mvn test`
- `cd tonepilot-admin/frontend && npm run build`

Expected: both PASS.

- [ ] **Step 3: Commit**

```bash
git add README.md docs/superpowers/plans/2026-06-29-color-material-ingestion.md
git commit -m "docs: explain knowledge material ingestion"
git push
```

## Self-Review

- Spec coverage: 计划覆盖了多来源素材登记、文本/参数素材导入、生成待审核知识、前端管理入口和文档说明。抖音视频自动抓取器只预留来源类型和 URL，不在第一版直接下载视频，符合“最小化导入知识库”的可控范围。
- Placeholder scan: 没有 `TBD`、`TODO`、`implement later` 等占位内容。
- Type consistency: `KnowledgeSourceRequest`、`KnowledgeMaterialRequest`、`KnowledgeMaterialIngestionService`、`AdminKnowledgeMaterialController` 在各任务中命名一致。
