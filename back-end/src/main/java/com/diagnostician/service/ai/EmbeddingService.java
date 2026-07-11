package com.diagnostician.service.ai;

import com.diagnostician.config.AiProperties;
import com.diagnostician.entity.ProjectFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.File;
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

    private VectorStore getVectorStore() {
        return vectorStoreProvider.getIfAvailable();
    }

    public boolean isRagEnabled() {
        return getVectorStore() != null;
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

        contextService.requireProject(projectId);
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
            if (content.length() > aiProperties.getMaxCodeChars()) {
                content = content.substring(0, aiProperties.getMaxCodeChars());
            }
            try {
                List<String> chunks = codeDocumentSplitter.splitByFunctions(file.getFilename(), content, 2000);
                if (chunks.isEmpty()) {
                    chunks = codeDocumentSplitter.splitByLines(file.getFilename(), content, 2000);
                }
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
            // 项目无文件可索引，标记完成避免每次提问重复空跑
            indexedProjects.put(projectId, true);
            result.put("indexed", true);
            result.put("note", "项目暂无文件可索引");
            return result;
        }

        if (chunkCount > 0) {
            indexedProjects.put(projectId, true);
            persistVectorStore(vs);
            result.put("indexed", true);
            return result;
        }

        // 文件读取或 Embedding 失败导致无任何 chunk：不标记为已索引，下次提问会自动重试
        result.put("indexed", false);
        String reason;
        if (readFail > 0 && embedFail == 0) {
            reason = "全部文件读取失败（磁盘文件缺失或路径解析异常），readFail=" + readFail;
        } else if (embedFail > 0 && readFail == 0) {
            reason = "全部文件 Embedding 失败，请检查 Embedding 模型配置（provider/api-key/base-url/model），embedFail=" + embedFail;
        } else {
            reason = "文件读取与 Embedding 均失败，readFail=" + readFail + ", embedFail=" + embedFail;
        }
        result.put("reason", reason);
        return result;
    }

    /**
     * 将内存向量库持久化到磁盘，避免重启后丢失需要重新索引。
     */
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
        // 不依赖 SimpleVectorStore 的 filterExpression（其对 metadata 过滤在某些版本下不稳定），
        // 改为多取候选 + 应用层按 projectId 过滤；阈值设 0 避免被相似度门槛滤空，再由 topK 截断
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
