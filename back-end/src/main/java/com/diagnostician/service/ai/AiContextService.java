package com.diagnostician.service.ai;

import com.diagnostician.config.AppProperties;
import com.diagnostician.entity.*;
import com.diagnostician.exception.AiServiceException;
import com.diagnostician.exception.ResourceNotFoundException;
import com.diagnostician.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiContextService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository fileRepository;
    private final AnalysisRepository analysisRepository;
    private final IssueRepository issueRepository;
    private final AppProperties appProperties;

    public Project requireProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("项目不存在"));
    }

    public Analysis requireLatestAnalysis(String projectId) {
        return analysisRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("该项目暂无分析结果，请先完成代码分析"));
    }

    public List<Issue> getIssuesForAnalysis(String analysisId) {
        return issueRepository.findByAnalysisId(analysisId);
    }

    public String readFileContent(String projectId, String filePath) {
        Path absPath = resolveProjectFilePath(projectId, filePath);
        if (absPath == null || !Files.exists(absPath)) {
            throw new ResourceNotFoundException("文件不存在: " + filePath);
        }
        try {
            return Files.readString(absPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AiServiceException("读取文件失败: " + filePath, e);
        }
    }

    public List<ProjectFile> listFiles(String projectId) {
        return fileRepository.findByProjectId(projectId);
    }

    public Map<String, Object> buildAnalysisSnapshot(String projectId) {
        Project project = requireProject(projectId);
        Analysis analysis = requireLatestAnalysis(projectId);
        List<Issue> issues = getIssuesForAnalysis(analysis.getId());

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("projectName", project.getName());
        snapshot.put("language", project.getLanguage());
        snapshot.put("overallScore", analysis.getOverallScore());
        snapshot.put("grade", analysis.getGrade());
        snapshot.put("issueCount", issues.size());
        snapshot.put("dimensions", analysis.getDimensions().stream().map(d -> Map.of(
                "name", d.getName(),
                "score", d.getScore(),
                "issues", d.getIssues()
        )).toList());
        snapshot.put("topIssues", issues.stream().limit(10).map(this::issueToMap).toList());
        return snapshot;
    }

    public Map<String, Object> issueToMap(Issue issue) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", issue.getId());
        map.put("type", issue.getType());
        map.put("severity", issue.getSeverity());
        map.put("file", issue.getFile());
        map.put("line", issue.getLine());
        map.put("message", issue.getMessage());
        map.put("suggestion", issue.getSuggestion());
        return map;
    }

    public String extractCodeContext(String content, int line, int radius) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String[] lines = content.split("\n", -1);
        int idx = Math.max(0, Math.min(lines.length - 1, line - 1));
        int start = Math.max(0, idx - radius);
        int end = Math.min(lines.length, idx + radius + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(String.format("%4d| %s%n", i + 1, lines[i]));
        }
        return sb.toString();
    }

    private Path resolveProjectFilePath(String projectId, String path) {
        List<ProjectFile> files = fileRepository.findByProjectId(projectId);
        for (ProjectFile pf : files) {
            if (path.equals(pf.getFilename()) || path.equals(pf.getFilePath())) {
                return Paths.get(pf.getFilePath());
            }
        }
        Path byName = Paths.get(appProperties.getUploadDir(), projectId, path);
        if (Files.exists(byName)) {
            return byName;
        }
        return null;
    }
}
