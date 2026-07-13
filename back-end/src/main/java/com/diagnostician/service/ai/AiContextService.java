package com.diagnostician.service.ai;

import com.diagnostician.config.AppProperties;
import com.diagnostician.entity.*;
import com.diagnostician.exception.AiServiceException;
import com.diagnostician.exception.ResourceNotFoundException;
import com.diagnostician.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Project project = requireProject(projectId);
        String lastAnalysisId = project.getLastAnalysisId();
        if (lastAnalysisId == null || lastAnalysisId.isBlank()) {
            throw new ResourceNotFoundException("该项目暂无分析结果，请先完成代码分析");
        }
        return analysisRepository.findById(lastAnalysisId)
                .orElseThrow(() -> new ResourceNotFoundException("该项目暂无分析结果，请先完成代码分析"));
    }

    public List<Issue> getIssuesForAnalysis(String analysisId) {
        return issueRepository.findByAnalysisId(analysisId);
    }

    public String readFileContent(String projectId, String filePath) {
        return readFileContent(projectId, filePath, null);
    }

    /**
     * 读取文件内容。可传入预加载的文件列表避免循环内重复查库（索引场景）。
     */
    public String readFileContent(String projectId, String filePath, List<ProjectFile> preloadedFiles) {
        Path absPath = resolveProjectFilePath(projectId, filePath, preloadedFiles);
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

    @Transactional(readOnly = true)
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

    public Path resolveDiskPath(String projectId, String filePath) {
        return resolveProjectFilePath(projectId, filePath, null);
    }

    private Path resolveProjectFilePath(String projectId, String path, List<ProjectFile> preloadedFiles) {
        if (path == null || path.isBlank()) {
            return null;
        }
        List<ProjectFile> files = preloadedFiles != null ? preloadedFiles : fileRepository.findByProjectId(projectId);
        // 1. 精确匹配 filename 或 filePath
        for (ProjectFile pf : files) {
            if (path.equals(pf.getFilename()) || path.equals(pf.getFilePath())) {
                Path resolved = resolveStoredPath(pf.getFilePath());
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        // 回退：按文件名部分匹配（Issue 中存的可能是带 uuid 前缀的物理路径）
        String pathFileName = Paths.get(path).getFileName().toString();
        for (ProjectFile pf : files) {
            String pfFileName = Paths.get(pf.getFilePath()).getFileName().toString();
            if (pathFileName.equals(pfFileName)) {
                return resolveStoredPath(pf.getFilePath());
            }
        }
        Path byName = Paths.get(appProperties.getUploadDir(), projectId, path);
        if (Files.exists(byName)) {
            return byName;
        }
        return null;
    }

    /**
     * 将 DB 中存储的 filePath（形如 /uploads/{projectId}/{storedFilename}）
     * 解析为磁盘绝对路径。filePath 带前导斜杠，不能直接 Paths.get(filePath)
     * （Windows 下会被当成当前盘根下的 uploads 目录），需相对 uploadRoot 重建。
     */
    private Path resolveStoredPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        String relative = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        Path root = appProperties.uploadRoot();
        // relative 形如 uploads/{projectId}/{storedFilename}，去掉首段 uploads/ 后拼到 uploadRoot
        int firstSep = relative.indexOf('/');
        String tail = firstSep >= 0 ? relative.substring(firstSep + 1) : relative;
        Path resolved = root.resolve(tail).normalize();
        if (!resolved.startsWith(root)) {
            return null; // 防止路径穿越逃逸出 uploadRoot
        }
        return resolved;
    }
}
