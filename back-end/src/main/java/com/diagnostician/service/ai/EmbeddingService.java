package com.diagnostician.service.ai;

import com.diagnostician.config.AiProperties;
import com.diagnostician.entity.ProjectFile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final AiContextService contextService;
    private final CodeDocumentSplitter codeDocumentSplitter;
    private final AiProperties aiProperties;

    private final Map<String, Boolean> indexedProjects = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> projectLocks = new ConcurrentHashMap<>();

    private VectorStore getVectorStore() {
        return vectorStoreProvider.getIfAvailable();
    }

    @PostConstruct
    public void initIndexedProjects() {
        VectorStore vs = getVectorStore();
        if (vs != null) {
            scanIndexedProjectsFromStore(vs);
        }
    }

    public boolean isRagEnabled() {
        return getVectorStore() != null;
    }

    public void clearIndexed(String projectId) {
        indexedProjects.remove(projectId);
    }

    public Map<String, Object> indexProject(String projectId) {
        VectorStore vs = getVectorStore();
        if (vs == null) {
            return Map.of(
                    "projectId", projectId,
                    "indexed", false,
                    "reason", "Embedding 模型未配置（app.ai.embedding-provider=none），RAG 功能已禁用"
            );
        }

        Object lock = projectLocks.computeIfAbsent(projectId, k -> new Object());
        synchronized (lock) {
            if (indexedProjects.getOrDefault(projectId, false)) {
                Map<String, Object> skip = new LinkedHashMap<>();
                skip.put("projectId", projectId);
                skip.put("indexed", true);
                skip.put("note", "项目已索引，跳过");
                return skip;
            }

            contextService.requireProject(projectId);
            clearProjectVectors(vs, projectId);

            List<ProjectFile> files = contextService.listFiles(projectId);
            int chunkCount = 0;
            int readFail = 0;
            int embedFail = 0;

            for (ProjectFile file : files) {
                String content;
                try {
                    content = contextService.readFileContent(projectId, file.getFilename());
                } catch (Exception e) {
                    log.warn("读取文件失败: {} - {}", file.getFilename(), e.getMessage());
                    readFail++;
                    continue;
                }
                if (content.isBlank()) {
                    continue;
                }
                if (content.length() > aiProperties.getMaxCodeChars()) {
                    content = content.substring(0, aiProperties.getMaxCodeChars());
                }
                try {
                    List<String> chunks = codeDocumentSplitter.splitByFunctions(file.getFilename(), content, 2000);
                    for (int i = 0; i < chunks.size(); i++) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("projectId", projectId);
                        metadata.put("filename", file.getFilename());
                        metadata.put("chunkIndex", i);
                        vs.add(List.of(new Document(chunks.get(i), metadata)));
                        chunkCount++;
                    }
                } catch (Exception e) {
                    log.warn("Embedding 失败: {} - {}", file.getFilename(), e.getMessage());
                    embedFail++;
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("projectId", projectId);
            result.put("fileCount", files.size());
            result.put("chunkCount", chunkCount);

            if (files.isEmpty()) {
                result.put("indexed", false);
                result.put("note", "项目暂无文件可索引");
                return result;
            }

            if (chunkCount > 0) {
                indexedProjects.put(projectId, true);
                persistVectorStore(vs);
                result.put("indexed", true);
                return result;
            }

            result.put("indexed", false);
            String reason;
            if (readFail > 0 && embedFail == 0) {
                reason = "全部文件读取失败（磁盘文件缺失或路径解析异常），readFail=" + readFail;
            } else if (embedFail > 0 && readFail == 0) {
                reason = "全部文件 Embedding 失败，请检查 Embedding 模型配置（provider/api-key/base-url/model），embedFail=" + embedFail;
            } else if (readFail == 0 && embedFail == 0) {
                reason = "无有效内容可索引";
            } else {
                reason = "文件读取与 Embedding 均失败，readFail=" + readFail + ", embedFail=" + embedFail;
            }
            result.put("reason", reason);
            return result;
        }
    }

    /**
     * 通过反射清理指定项目在 SimpleVectorStore 中的旧向量（Spring AI 1.0.0 store 字段）。
     */
    @SuppressWarnings("unchecked")
    private void clearProjectVectors(VectorStore vs, String projectId) {
        if (!(vs instanceof SimpleVectorStore)) {
            return;
        }
        try {
            Field storeField = SimpleVectorStore.class.getDeclaredField("store");
            storeField.setAccessible(true);
            Map<String, Document> store = (Map<String, Document>) storeField.get(vs);
            store.entrySet().removeIf(e -> projectId.equals(String.valueOf(e.getValue().getMetadata().get("projectId"))));
        } catch (Exception e) {
            log.warn("清理项目向量失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void scanIndexedProjectsFromStore(VectorStore vs) {
        if (!(vs instanceof SimpleVectorStore)) {
            return;
        }
        try {
            Field storeField = SimpleVectorStore.class.getDeclaredField("store");
            storeField.setAccessible(true);
            Map<String, Document> store = (Map<String, Document>) storeField.get(vs);
            for (Document doc : store.values()) {
                Object pid = doc.getMetadata().get("projectId");
                if (pid != null) {
                    indexedProjects.put(String.valueOf(pid), true);
                }
            }
            log.info("从向量库恢复已索引项目: {}", indexedProjects.keySet());
        } catch (Exception e) {
            log.warn("初始化 indexedProjects 失败: {}", e.getMessage());
        }
    }

    private void persistVectorStore(VectorStore vs) {
        try {
            File persistFile = new File(aiProperties.getVectorStoreDir(), "vectorstore.json");
            if (vs instanceof SimpleVectorStore svs) {
                svs.save(persistFile);
            }
        } catch (Exception e) {
            log.warn("向量库持久化失败: {}", e.getMessage());
        }
    }

    public boolean isIndexed(String projectId) {
        return indexedProjects.getOrDefault(projectId, false);
    }

    public List<Document> search(String projectId, String query, int topK) {
        VectorStore vs = getVectorStore();
        if (vs == null) {
            return Collections.emptyList();
        }
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(Math.max(topK * 3, 10))
                .similarityThreshold(0.0)
                .build();
        List<Document> raw = vs.similaritySearch(request);
        List<Document> filtered = raw.stream()
                .filter(d -> projectId.equals(String.valueOf(d.getMetadata().get("projectId"))))
                .limit(topK)
                .toList();
        String scores = filtered.stream()
                .map(d -> String.format("%.3f", d.getScore()))
                .collect(Collectors.joining(","));
        log.info("RAG search: query='{}', projectId={}, rawHits={}, filtered={}, scores=[{}]",
                query.length() > 60 ? query.substring(0, 60) + "..." : query,
                projectId, raw.size(), filtered.size(), scores);
        return filtered;
    }
}
