package com.bugkiller_cityu.code_diagnostician.controller;

import com.bugkiller_cityu.code_diagnostician.entity.*;
import com.bugkiller_cityu.code_diagnostician.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectRepository projectRepository;
    private final ProjectFileRepository fileRepository;
    private final AnalysisRepository analysisRepository;
    private final DimensionRepository dimensionRepository;
    private final IssueRepository issueRepository;
    private final com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.CodeAnalyzer codeAnalyzer;

    // -------------------------- 项目管理 --------------------------
    @GetMapping("/projects")
    public List<Map<String, Object>> listProjects() {
        List<Project> projects = projectRepository.findAll();
        System.out.println("getProject called");
        return projects.stream()
                .map(this::enrichProjectWithLastAnalysis)
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    @PostMapping("/projects")
    public Map<String, Object> createProject(@RequestBody Map<String, Object> body) {
        Project project = new Project();
        project.setName((String) body.getOrDefault("name", "新项目"));
        project.setDescription((String) body.getOrDefault("description", ""));
        project.setLanguage((String) body.getOrDefault("language", "javascript"));
        project.setStatus("active");
        project.setFileCount(0);
        System.out.println("createProject called");
        Project saved = projectRepository.save(project);
        return convertToMap(enrichProjectWithLastAnalysis(saved));
    }

    @GetMapping("/projects/{id}")
    public Map<String, Object> getProject(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = enrichProjectWithLastAnalysis(projectOpt.get());
            return convertToMap(project);
        } else {
            return Map.<String, Object>of("error", "项目不存在");
        }
    }

    @PutMapping("/projects/{id}")
    public Map<String, Object> updateProject(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            if (body.containsKey("name")) project.setName((String) body.get("name"));
            if (body.containsKey("description")) project.setDescription((String) body.get("description"));
            if (body.containsKey("language")) project.setLanguage((String) body.get("language"));
            if (body.containsKey("status")) project.setStatus((String) body.get("status"));
            project.setUpdatedAt(Instant.now());
            Project updated = projectRepository.save(project);
            return convertToMap(enrichProjectWithLastAnalysis(updated));
        } else {
            return Map.<String, Object>of("error", "项目不存在");
        }
    }

    @DeleteMapping("/projects/{id}")
    @Transactional
    public Map<String, Object> deleteProject(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            // 1. 删除本地文件
            try {
                Path base = Paths.get("uploads").resolve(id);
                if (Files.exists(base)) {
                    Files.walk(base)
                            .sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                            });
                }
            } catch (Exception ignored) {}

            // 2. 删除数据库记录（级联删除文件、分析结果等）
            projectRepository.delete(project);
            return Map.<String, Object>of("message", "项目删除成功", "id", id);
        } else {
            return Map.<String, Object>of("error", "项目不存在");
        }
    }

    // -------------------------- 文件管理 --------------------------
    @GetMapping("/projects/{id}/files")
    public Map<String, Object> listFiles(@PathVariable String id) {
        if (!projectRepository.existsById(id)) {
            return Map.<String, Object>of("error", "项目不存在");
        }
        List<ProjectFile> files = fileRepository.findByProjectId(id);
        int totalFiles = files.size();
        long totalSize = files.stream().mapToLong(ProjectFile::getFileSize).sum();

        // 更新项目文件数
        projectRepository.findById(id).ifPresent(project -> {
            project.setFileCount(totalFiles);
            project.setUpdatedAt(Instant.now());
            projectRepository.save(project);
        });

        return Map.of(
                "files", files.stream().map(this::convertToMap).collect(Collectors.toList()),
                "totalFiles", totalFiles,
                "totalSize", totalSize
        );
    }

    @GetMapping("/projects/{id}/files/content")
    public Map<String, Object> getFileContent(@PathVariable String id, @RequestParam("path") String path) throws IOException {
        String filename = Paths.get(path).getFileName().toString();
        Path absPath = Paths.get("uploads").resolve(id).resolve(filename);
        String content;
        long size = 0;

        if (Files.exists(absPath)) {
            content = Files.readString(absPath);
            size = Files.size(absPath);
        } else {
            content = "示例文件内容: " + filename;
        }

        return Map.of("path", path, "content", content, "size", size);
    }

    @PostMapping(value = "/projects/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@PathVariable String id, @RequestPart("files") MultipartFile[] files) throws IOException {
        if (!projectRepository.existsById(id)) {
            return Map.<String, Object>of("error", "项目不存在");
        }

        List<ProjectFile> uploadedFiles = new ArrayList<>();
        Path baseDir = Paths.get("uploads").resolve(id);
        Files.createDirectories(baseDir);
        Instant uploadTime = Instant.now();

        for (MultipartFile file : files) {
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String storedFilename = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = baseDir.resolve(storedFilename);

            // 保存文件到本地
            Files.copy(file.getInputStream(), targetPath);

            // 保存文件元数据到数据库
            ProjectFile projectFile = new ProjectFile();
            projectFile.setProjectId(id);
            projectFile.setFilename(originalFilename);
            projectFile.setFilePath("/uploads/" + id + "/" + storedFilename);
            projectFile.setFileSize(file.getSize());
            projectFile.setUploadTime(uploadTime);
            uploadedFiles.add(fileRepository.save(projectFile));
        }

        // 更新项目文件数和更新时间
        projectRepository.findById(id).ifPresent(project -> {
            project.setFileCount(fileRepository.findByProjectId(id).size());
            project.setUpdatedAt(Instant.now());
            projectRepository.save(project);
        });

        return Map.of(
                "message", "成功上传 " + uploadedFiles.size() + " 个文件",
                "uploadedFiles", uploadedFiles.stream().map(this::convertToMap).collect(Collectors.toList()),
                "errors", List.of(),
                "totalFiles", files.length
        );
    }

    @DeleteMapping("/projects/{id}/files/{fileId}")
    @Transactional
    public Map<String, Object> deleteFile(@PathVariable String id, @PathVariable String fileId) {
        Optional<ProjectFile> fileOpt = fileRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            ProjectFile file = fileOpt.get();
            if (file.getProjectId().equals(id)) {
                // 删除本地文件
                String filePath = file.getFilePath();
                String filename = Paths.get(filePath).getFileName().toString();
                Path absPath = Paths.get("uploads").resolve(id).resolve(filename);
                try { Files.deleteIfExists(absPath); } catch (IOException ignored) {}

                // 删除数据库记录
                fileRepository.delete(file);

                // 更新项目文件数
                projectRepository.findById(id).ifPresent(project -> {
                    project.setFileCount(fileRepository.findByProjectId(id).size());
                    project.setUpdatedAt(Instant.now());
                    projectRepository.save(project);
                });

                return Map.of(
                        "message", "文件删除成功",
                        "fileId", fileId,
                        "deletedFile", Map.of(
                                "id", file.getId(),
                                "filename", file.getFilename(),
                                "file_path", file.getFilePath()
                        )
                );
            } else {
                return Map.<String, Object>of("error", "文件不属于该项目");
            }
        } else {
            return Map.<String, Object>of("error", "文件不存在");
        }
    }

    @DeleteMapping("/projects/{id}/files")
    @Transactional
    public Map<String, Object> deleteFileByPath(@PathVariable String id, @RequestParam("path") String path) {
        Optional<ProjectFile> fileOpt = fileRepository.findByProjectIdAndFilePath(id, path);
        if (fileOpt.isPresent()) {
            ProjectFile file = fileOpt.get();
            // 删除本地文件
            String filename = Paths.get(path).getFileName().toString();
            Path absPath = Paths.get("uploads").resolve(id).resolve(filename);
            try { Files.deleteIfExists(absPath); } catch (IOException ignored) {}

            // 删除数据库记录
            fileRepository.delete(file);

            // 更新项目文件数
            projectRepository.findById(id).ifPresent(project -> {
                project.setFileCount(fileRepository.findByProjectId(id).size());
                project.setUpdatedAt(Instant.now());
                projectRepository.save(project);
            });

            return Map.of(
                    "message", "文件删除成功",
                    "deletedFile", Map.of(
                            "id", file.getId(),
                            "filename", file.getFilename(),
                            "file_path", file.getFilePath()
                    )
            );
        } else {
            return Map.<String, Object>of("error", "文件不存在");
        }
    }

    // -------------------------- 分析管理 --------------------------
    @PostMapping("/projects/{id}/analyze")
    @Transactional
    public Map<String, Object> analyze(@PathVariable String id) {
        try {
            Optional<Project> projectOpt = projectRepository.findById(id);
            if (projectOpt.isEmpty()) {
                return Map.<String, Object>of("error", "项目不存在");
            }

            Project project = projectOpt.get();

            Analysis analysis = new Analysis();
            analysis.setProjectId(id);
            analysis.setOverallScore(0);
            analysis.setGrade("good");
            analysis.setCreatedAt(Instant.now());
            Analysis savedAnalysis = analysisRepository.save(analysis);

            List<ProjectFile> files = fileRepository.findByProjectId(id);
            List<com.bugkiller_cityu.code_diagnostician.service.model.metric.AnalysisResult> fileResults = new ArrayList<>();
            for (ProjectFile f : files) {
                String storedName = Paths.get(f.getFilePath()).getFileName().toString();
                Path abs = Paths.get("uploads").resolve(id).resolve(storedName);
                if (Files.exists(abs)) {
                    try {
                        com.bugkiller_cityu.code_diagnostician.service.model.metric.AnalysisResult r = codeAnalyzer.analyzeFile(abs.toString());
                        fileResults.add(r);
                    } catch (Exception ignored) {}
                }
            }

            double overall = fileResults.isEmpty() ? 0.0 : codeAnalyzer.calculateOverallScore(fileResults);
            int overallInt = (int) Math.round(overall * 100.0);
            String grade = overallInt >= 85 ? "excellent" : overallInt >= 70 ? "good" : "poor";

            Map<String, List<com.bugkiller_cityu.code_diagnostician.service.model.metric.MetricResult>> metricsMap = new LinkedHashMap<>();
            for (var fr : fileResults) {
                if (fr.getMetricResults() == null) continue;
                for (var e : fr.getMetricResults().entrySet()) {
                    metricsMap.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue());
                }
            }

            List<Dimension> dimensions = new ArrayList<>();
            for (var entry : metricsMap.entrySet()) {
                String name = entry.getKey();
                List<com.bugkiller_cityu.code_diagnostician.service.model.metric.MetricResult> mrs = entry.getValue();
                double avg = mrs.stream().mapToDouble(com.bugkiller_cityu.code_diagnostician.service.model.metric.MetricResult::getScore).average().orElse(0.0);
                int issuesCount = mrs.stream().mapToInt(m -> m.getIssues() == null ? 0 : m.getIssues().size()).sum();
                dimensions.add(createDimension(savedAnalysis.getId(), name, (int) Math.round(avg * 100.0), 100, issuesCount));
            }
            if (!dimensions.isEmpty()) dimensionRepository.saveAll(dimensions);

            List<Issue> allIssues = new ArrayList<>();
            for (var fr : fileResults) {
                List<Issue> frIssues = fr.getIssues();
                if (frIssues != null) {
                    for (Issue is : frIssues) {
                        if (is == null) continue;
                        is.setAnalysisId(savedAnalysis.getId());
                        allIssues.add(is);
                    }
                }
            }
            if (!allIssues.isEmpty()) issueRepository.saveAll(allIssues);

            savedAnalysis.setOverallScore(overallInt);
            savedAnalysis.setGrade(grade);
            savedAnalysis.setDimensions(dimensions);
            savedAnalysis.setIssues(allIssues);
            analysisRepository.save(savedAnalysis);

            project.setStatus("completed");
            project.setLastAnalysisId(savedAnalysis.getId());
            project.setUpdatedAt(Instant.now());
            projectRepository.save(project);

            List<Map<String, Object>> dimsOut = dimensions.stream().map(this::convertToMap).collect(Collectors.toList());
            List<Map<String, Object>> issuesOut = allIssues.stream().map(this::convertToMap).collect(Collectors.toList());

            return Map.of(
                    "message", "分析完成",
                    "analysisId", savedAnalysis.getId(),
                    "result", Map.of(
                            "id", savedAnalysis.getId(),
                            "projectId", id,
                            "overallScore", savedAnalysis.getOverallScore(),
                            "grade", savedAnalysis.getGrade(),
                            "dimensions", dimsOut,
                            "issues", issuesOut,
                            "created_at", savedAnalysis.getCreatedAt().toString()
                    )
            );
        } catch (Exception e) {
            return Map.of("error", "分析失败", "details", e.getMessage());
        }
    }

    @DeleteMapping("/projects/{id}/analyze")
    public Map<String, Object> cancelAnalyze(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            project.setStatus("cancelled");
            project.setUpdatedAt(Instant.now());
            projectRepository.save(project);
            return Map.of("message", "已取消分析", "projectId", id, "status", "cancelled");
        } else {
            return Map.<String, Object>of("error", "项目不存在");
        }
    }

    @GetMapping("/projects/{id}/analysis")
    public Map<String, Object> getAnalysis(@PathVariable String id) {
        if (!projectRepository.existsById(id)) {
            return Map.<String, Object>of("error", "项目不存在");
        }

        Optional<Analysis> analysisOpt = analysisRepository.findTopByProjectIdOrderByCreatedAtDesc(id);
        if (analysisOpt.isPresent()) {
            Analysis analysis = analysisOpt.get();
            Project project = projectRepository.findById(id).get(); // 已判断项目存在，直接get
            List<Issue> issues = analysis.getIssues();

            // 统计指标
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("fileCount", project.getFileCount());
            metrics.put("totalIssues", issues.size());
            metrics.put("criticalIssues", issues.stream().filter(i -> "critical".equals(i.getSeverity())).count());
            metrics.put("majorIssues", issues.stream().filter(i -> "major".equals(i.getSeverity())).count());
            metrics.put("minorIssues", issues.stream().filter(i -> "minor".equals(i.getSeverity())).count());

            // 优化建议
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("suggestions", analysis.getDimensions().stream()
                    .map(d -> String.format("优化%s相关代码", d.getName()))
                    .collect(Collectors.toList()));

            return Map.of(
                    "projectId", id,
                    "projectName", project.getName(),
                    "overallScore", analysis.getOverallScore(),
                    "grade", analysis.getGrade(),
                    "dimensions", analysis.getDimensions().stream().map(this::convertToMap).collect(Collectors.toList()),
                    "issues", issues.stream().map(this::convertToMap).collect(Collectors.toList()),
                    "completedAt", analysis.getCreatedAt().toString(),
                    "taskId", analysis.getId(),
                    "metrics", metrics,
                    "summary", summary
            );
        } else {
            return Map.<String, Object>of("error", "该项目暂无分析结果");
        }
    }

    @GetMapping("/projects/{id}/analysis/status")
    public Map<String, Object> getStatus(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            String status = project.getStatus();
            int progress = "completed".equals(status) ? 100 : 0;
            return Map.of(
                    "projectId", id,
                    "status", status,
                    "progress", progress,
                    "updatedAt", project.getUpdatedAt().toString()
            );
        } else {
            return Map.<String, Object>of("error", "项目不存在");
        }
    }

    @GetMapping("/projects/{id}/analysis/logs")
    public Map<String, Object> getLogs(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            List<Map<String, Object>> logs = new ArrayList<>();
            // 项目创建日志
            logs.add(Map.of(
                    "timestamp", project.getCreatedAt().toString(),
                    "level", "info",
                    "message", "项目创建: " + project.getName(),
                    "source", "system"
            ));

            // 分析日志
            List<Analysis> analyses = analysisRepository.findByProjectIdOrderByCreatedAtDesc(id);
            for (int i = 0; i < analyses.size(); i++) {
                Analysis analysis = analyses.get(i);
                logs.add(Map.of(
                        "timestamp", analysis.getCreatedAt().toString(),
                        "level", "info",
                        "message", "分析完成 #" + (analyses.size() - i) + " - 总分: " + analysis.getOverallScore() + "分",
                        "source", "analyzer"
                ));

                // 维度日志
                for (Dimension dim : analysis.getDimensions()) {
                    logs.add(Map.of(
                            "timestamp", analysis.getCreatedAt().toString(),
                            "level", "debug",
                            "message", dim.getName() + ": " + dim.getScore() + "分 (问题: " + dim.getIssues() + "个)",
                            "source", "analyzer"
                    ));
                }
            }

            Collections.reverse(logs);
            return Map.of(
                    "projectId", id,
                    "logs", logs,
                    "totalLogs", logs.size()
            );
        } else {
            return Map.<String, Object>of("error", "项目不存在");
        }
    }

    @GetMapping("/projects/{id}/analysis/history")
    public List<Map<String, Object>> getHistory(@PathVariable String id) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!projectRepository.existsById(id)) {
            result.add(Map.<String, Object>of("error", "项目不存在"));
            return result;
        }

        List<Analysis> analyses = analysisRepository.findByProjectIdOrderByCreatedAtDesc(id);
        if (analyses.isEmpty()) {
            result.add(Map.<String, Object>of("error", "该项目暂无分析历史"));
            return result;
        }

        return analyses.stream()
                .map(analysis -> Map.of(
                        "taskId", analysis.getId(),
                        "completedAt", analysis.getCreatedAt().toString(),
                        "overallScore", analysis.getOverallScore(),
                        "grade", analysis.getGrade(),
                        "dimensions", analysis.getDimensions().stream().map(this::convertToMap).collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    // -------------------------- 辅助接口 --------------------------
    @PostMapping("/projects/{id}/git")
    public Map<String, Object> importGit(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            String repo = (String) body.getOrDefault("repo", "");
            project.setUpdatedAt(Instant.now());
            projectRepository.save(project);
            return Map.of("message", "导入成功", "repo", repo, "taskId", UUID.randomUUID().toString());
        } else {
            return Map.<String, Object>of("error", "项目不存在");
        }
    }

    @GetMapping("/projects/stats")
    public Map<String, Object> stats() {
        List<Project> allProjects = projectRepository.findAll();
        int total = allProjects.size();
        long active = allProjects.stream().filter(p -> "active".equals(p.getStatus())).count();
        long archived = allProjects.stream().filter(p -> "archived".equals(p.getStatus())).count();
        long completed = allProjects.stream().filter(p -> "completed".equals(p.getStatus())).count();
        long totalFiles = fileRepository.count();

        return Map.of(
                "totalProjects", total,
                "activeProjects", active,
                "archivedProjects", archived,
                "completedProjects", completed,
                "totalFiles", totalFiles
        );
    }

    @PostMapping("/projects/{id}/duplicate")
    @Transactional
    public Map<String, Object> duplicate(@PathVariable String id) {
        Optional<Project> sourceProjectOpt = projectRepository.findById(id);
        if (sourceProjectOpt.isPresent()) {
            Project sourceProject = sourceProjectOpt.get();
            // 1. 复制项目基本信息
            Project newProject = new Project();
            newProject.setName(sourceProject.getName() + " - 副本");
            newProject.setDescription(sourceProject.getDescription());
            newProject.setLanguage(sourceProject.getLanguage());
            newProject.setStatus("active");
            newProject.setFileCount(sourceProject.getFileCount());
            Project savedProject = projectRepository.save(newProject);

            // 2. 复制项目文件（仅元数据，不复制物理文件）
            List<ProjectFile> sourceFiles = fileRepository.findByProjectId(id);
            List<ProjectFile> newFiles = sourceFiles.stream().map(file -> {
                ProjectFile newFile = new ProjectFile();
                newFile.setProjectId(savedProject.getId());
                newFile.setFilename(file.getFilename());
                newFile.setFilePath(file.getFilePath().replace(id, savedProject.getId()));
                newFile.setFileSize(file.getFileSize());
                newFile.setUploadTime(Instant.now());
                return newFile;
            }).collect(Collectors.toList());
            fileRepository.saveAll(newFiles);

            return convertToMap(enrichProjectWithLastAnalysis(savedProject));
        } else {
            return Map.<String, Object>of("error", "项目不存在");
        }
    }

    @PostMapping("/projects/{id}/archive")
    public Map<String, Object> archive(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            project.setStatus("archived");
            project.setUpdatedAt(Instant.now());
            Project updated = projectRepository.save(project);
            return convertToMap(enrichProjectWithLastAnalysis(updated));
        } else {
            return Map.<String, Object>of("error", "项目不存在");
        }
    }

    @PostMapping("/projects/{id}/restore")
    public Map<String, Object> restore(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            project.setStatus("active");
            project.setUpdatedAt(Instant.now());
            Project updated = projectRepository.save(project);
            return convertToMap(enrichProjectWithLastAnalysis(updated));
        } else {
            return Map.<String, Object>of("error", "项目不存在");
        }
    }

    // -------------------------- 工具方法 --------------------------
    // 为项目添加最新分析结果信息
    private Project enrichProjectWithLastAnalysis(Project project) {
        if (project.getLastAnalysisId() != null) {
            analysisRepository.findById(project.getLastAnalysisId()).ifPresent(analysis -> {
                // 仅保留分析ID关联，避免循环引用
                project.setLastAnalysisId(analysis.getId());
            });
        }
        return project;
    }

    // 实体转Map（适配原接口返回格式）
    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Project project) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", project.getId());
            map.put("name", project.getName());
            map.put("description", project.getDescription());
            map.put("language", project.getLanguage());
            map.put("status", project.getStatus());
            map.put("created_at", project.getCreatedAt().toString());
            map.put("updated_at", project.getUpdatedAt().toString());
            map.put("file_count", project.getFileCount());

            // 添加最新分析结果摘要
            if (project.getLastAnalysisId() != null) {
                analysisRepository.findById(project.getLastAnalysisId()).ifPresent(analysis -> {
                    Map<String, Object> lastAnalysis = new LinkedHashMap<>();
                    lastAnalysis.put("overallScore", analysis.getOverallScore());
                    lastAnalysis.put("grade", analysis.getGrade());
                    lastAnalysis.put("completedAt", analysis.getCreatedAt().toString());
                    lastAnalysis.put("taskId", analysis.getId());
                    map.put("lastAnalysis", lastAnalysis);
                });
            }
            return map;
        } else if (obj instanceof ProjectFile file) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", file.getId());
            map.put("project_id", file.getProjectId());
            map.put("filename", file.getFilename());
            map.put("file_path", file.getFilePath());
            map.put("file_size", file.getFileSize());
            map.put("upload_time", file.getUploadTime().toString());
            return map;
        } else if (obj instanceof Dimension dim) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", dim.getName());
            map.put("score", dim.getScore());
            map.put("maxScore", dim.getMaxScore());
            map.put("issues", dim.getIssues());
            return map;
        } else if (obj instanceof Issue issue) {
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
        return Collections.emptyMap();
    }

    // 创建维度评分（辅助方法）
    private Dimension createDimension(String analysisId, String name, int score, int maxScore, int issues) {
        Dimension dim = new Dimension();
        dim.setAnalysisId(analysisId);
        dim.setName(name);
        dim.setScore(score);
        dim.setMaxScore(maxScore);
        dim.setIssues(issues);
        return dim;
    }
}
