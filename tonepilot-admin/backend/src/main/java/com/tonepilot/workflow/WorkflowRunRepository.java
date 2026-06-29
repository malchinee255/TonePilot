package com.tonepilot.workflow;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.persistence.PersistenceProperties;
import com.tonepilot.store.InMemoryTonePilotStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WorkflowRunRepository {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRunRepository.class);

    private final InMemoryTonePilotStore store;
    private final ObjectMapper objectMapper;
    private final WorkflowProperties properties;
    private final PersistenceProperties persistenceProperties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final Map<String, WorkflowRunSnapshot> localSnapshots = new ConcurrentHashMap<>();

    @Autowired
    public WorkflowRunRepository(
            InMemoryTonePilotStore store,
            ObjectMapper objectMapper,
            WorkflowProperties properties,
            PersistenceProperties persistenceProperties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider
    ) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.persistenceProperties = persistenceProperties;
        this.redisTemplateProvider = redisTemplateProvider;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public void save(TonePilotAgentContext context) {
        store.workflowRuns.put(context.getRunId(), context);
        WorkflowRunSnapshot localSnapshot = WorkflowRunSnapshot.from(context, "local-cache");
        localSnapshots.put(context.getRunId(), localSnapshot);
        saveToRedis(context);
        saveToDatabase(context);
    }

    public Optional<WorkflowRunSnapshot> find(String runId) {
        Optional<WorkflowRunSnapshot> redisSnapshot = findFromRedis(runId);
        if (redisSnapshot.isPresent()) {
            return redisSnapshot;
        }
        Optional<WorkflowRunSnapshot> databaseSnapshot = findFromDatabase(runId);
        if (databaseSnapshot.isPresent()) {
            return databaseSnapshot;
        }
        WorkflowRunSnapshot localSnapshot = localSnapshots.get(runId);
        if (localSnapshot != null) {
            return Optional.of(localSnapshot);
        }
        TonePilotAgentContext context = store.workflowRuns.get(runId);
        if (context == null) {
            return Optional.empty();
        }
        return Optional.of(WorkflowRunSnapshot.from(context, "in-memory-store"));
    }

    private void saveToDatabase(TonePilotAgentContext context) {
        if (!persistenceProperties.isEnabled()) {
            return;
        }
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
            if (jdbcTemplate == null) {
                return;
            }
            WorkflowRunSnapshot snapshot = WorkflowRunSnapshot.from(context, "database");
            String json = objectMapper.writeValueAsString(snapshot);
            int updated = jdbcTemplate.update("""
                            UPDATE workflow_run_snapshot
                            SET photo_id = ?, status = ?, provider = ?, target_style = ?, current_agent = ?,
                                storage = ?, snapshot_json = ?, created_at = ?, updated_at = ?
                            WHERE run_id = ?
                            """,
                    snapshot.photoId(),
                    snapshot.status(),
                    snapshot.provider(),
                    snapshot.targetStyle(),
                    snapshot.currentAgent(),
                    snapshot.storage(),
                    json,
                    Timestamp.from(snapshot.createdAt()),
                    Timestamp.from(snapshot.updatedAt()),
                    snapshot.runId()
            );
            if (updated == 0) {
                jdbcTemplate.update("""
                                INSERT INTO workflow_run_snapshot (
                                    run_id, photo_id, status, provider, target_style, current_agent,
                                    storage, snapshot_json, created_at, updated_at
                                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                """,
                        snapshot.runId(),
                        snapshot.photoId(),
                        snapshot.status(),
                        snapshot.provider(),
                        snapshot.targetStyle(),
                        snapshot.currentAgent(),
                        snapshot.storage(),
                        json,
                        Timestamp.from(snapshot.createdAt()),
                        Timestamp.from(snapshot.updatedAt())
                );
            }
        } catch (Exception exception) {
            log.debug("工作流快照写入数据库失败，已保留在 Redis/本地缓存：{}", exception.getMessage());
        }
    }

    private Optional<WorkflowRunSnapshot> findFromDatabase(String runId) {
        if (!persistenceProperties.isEnabled()) {
            return Optional.empty();
        }
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
            if (jdbcTemplate == null) {
                return Optional.empty();
            }
            return jdbcTemplate.query(
                    "SELECT snapshot_json FROM workflow_run_snapshot WHERE run_id = ?",
                    resultSet -> {
                        if (!resultSet.next()) {
                            return Optional.<WorkflowRunSnapshot>empty();
                        }
                        WorkflowRunSnapshot snapshot = readSnapshot(resultSet.getString("snapshot_json"));
                        return Optional.of(snapshot.withStorage("database"));
                    },
                    runId
            );
        } catch (Exception exception) {
            log.debug("工作流快照读取数据库失败，已尝试本地缓存：{}", exception.getMessage());
            return Optional.empty();
        }
    }

    private void saveToRedis(TonePilotAgentContext context) {
        if (!properties.isRedisEnabled()) {
            return;
        }
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return;
            }
            WorkflowRunSnapshot snapshot = WorkflowRunSnapshot.from(context, "redis");
            redisTemplate.opsForValue().set(key(context.getRunId()), objectMapper.writeValueAsString(snapshot), properties.getTtl());
        } catch (Exception exception) {
            log.debug("Redis 工作流上下文写入失败，已降级到本地缓存：{}", exception.getMessage());
        }
    }

    private Optional<WorkflowRunSnapshot> findFromRedis(String runId) {
        if (!properties.isRedisEnabled()) {
            return Optional.empty();
        }
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return Optional.empty();
            }
            String json = redisTemplate.opsForValue().get(key(runId));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, WorkflowRunSnapshot.class).withStorage("redis"));
        } catch (Exception exception) {
            log.debug("Redis 工作流上下文读取失败，已尝试本地缓存：{}", exception.getMessage());
            return Optional.empty();
        }
    }

    private String key(String runId) {
        return properties.getRedisKeyPrefix() + runId;
    }

    private WorkflowRunSnapshot readSnapshot(String json) {
        try {
            return objectMapper.readValue(json, WorkflowRunSnapshot.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("工作流快照 JSON 解析失败：" + exception.getMessage(), exception);
        }
    }
}


