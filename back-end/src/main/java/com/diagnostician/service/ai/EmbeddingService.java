package com.diagnostician.service.ai;

import com.diagnostician.config.AiProperties;
import com.diagnostician.entity.ProjectFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final AiContextService contextService;
    private final CodeDocumentSplitter codeDocumentSplitter;
    private final AiProperties aiProperties;

    private final Map<String, Boolean> indexedProjects = new ConcurrentHashMap<>();

    public Map<String, Object> indexProject(String projectId) {
        contextService.requireProject(projectId);
        List<ProjectFile> files = contextService.listFiles(projectId);
        int chunkCount = 0;

        for (ProjectFile file : files) {
            try {
                String content = contextService.readFileContent(projectId, file.getFilename());
                if (content.length() > aiProperties.getMaxCodeChars()) {
                    content = content.substring(0, aiProperties.getMaxCodeChars());
                }
                List<String> chunks = codeDocumentSplitter.splitByFunctions(file.getFilename(), content, 2000);
                if (chunks.isEmpty()) {
                    chunks = codeDocumentSplitter.splitByLines(file.getFilename(), content, 2000);
                }
                for (int i = 0; i < chunks.size(); i++) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("projectId", projectId);
                    metadata.put("filename", file.getFilename());
                    metadata.put("chunkIndex", i);
                    vectorStore.add(List.of(new Document(chunks.get(i), metadata)));
                    chunkCount++;
                }
            } catch (Exception e) {
                log.warn("索引文件失败: {} - {}", file.getFilename(), e.getMessage());
            }
        }

        indexedProjects.put(projectId, true);
        return Map.of(
                "projectId", projectId,
                "fileCount", files.size(),
                "chunkCount", chunkCount,
                "indexed", true
        );
    }

    public boolean isIndexed(String projectId) {
        return indexedProjects.getOrDefault(projectId, false);
    }

    public List<Document> search(String projectId, String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.5)
                .filterExpression("projectId == '" + projectId + "'")
                .build();
        return vectorStore.similaritySearch(request);
    }
}
