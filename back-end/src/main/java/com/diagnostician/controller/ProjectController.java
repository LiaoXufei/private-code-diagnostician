package com.diagnostician.controller;

import com.diagnostician.config.AppProperties;
import com.diagnostician.entity.*;
import com.diagnostician.repository.*;
import com.diagnostician.exception.BadRequestException;
import com.diagnostician.exception.ResourceNotFoundException;
import com.diagnostician.service.AnalysisService;
import com.diagnostician.service.ai.EmbeddingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.diagnostician.service.model.report.Report;
import com.diagnostician.service.model.analyzer.Analyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CrossOrigin
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {
    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);
    private static final int MAX_FILES_PER_PROJECT = 200;
    private static final long MAX_FILE_CONTENT_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".java", ".js", ".ts", ".jsx", ".tsx", ".vue", ".py", ".go",
            ".c", ".cpp", ".h", ".hpp", ".cs", ".rs", ".lua", ".md", ".json", ".xml", ".yml", ".yaml", ".txt"
    );

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository fileRepository;
    private final AnalysisRepository analysisRepository;
    private final FileAnalysisResultRepository fileAnalysisResultRepository;
    private final AnalysisService analysisService;
    private final EmbeddingService embeddingService;
    private final Report report;
    private final AppProperties appProperties;

    // -------------------------- 项目管理 --------------------------
    @GetMapping("/projects")
    public List<Map<String, Object>> listProjects() {
        List<Project> projects = projectRepository.findAll();
        Map<String, Analysis> lastAnalysisById = loadLastAnalyses(projects);
        return projects.stream()
                .map(p -> convertProjectToMap(p, lastAnalysisById.get(p.getLastAnalysisId())))
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/projects/{id}/report.pdf")
    public ResponseEntity<byte[]> exportProjectReportPdf(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isEmpty()) {
            return new ResponseEntity<>(new byte[0], HttpStatus.NOT_FOUND);
        }
        Project project = projectOpt.get();
        Analyzer.AnalysisResult ar = buildAnalysisResultForReport(project, id);
        Report.ReportOptions opts = new Report.ReportOptions();
        opts.markdownOutput = true;
        String md = report.generateReport(ar, opts);

        Parser parser = Parser.builder().build();
        Node document = parser.parse(md);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String htmlBody = renderer.render(document);
        Optional<Analysis> latestOpt = analysisRepository.findTopByProjectIdOrderByCreatedAtDesc(id);
        int overall = latestOpt.map(Analysis::getOverallScore).orElse(0);
        long totalLines = resolveTotalLines(id, latestOpt);
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        sb.append("<head>\n");
        sb.append("  <meta charset=\"utf-8\"/>\n");
        sb.append("  <title>").append(escapeHtml(project.getName())).append(" - 代码质量报告</title>\n");
        sb.append("  <style>\n");
        sb.append("    body{font-family:\"Segoe UI\",\"Microsoft YaHei\",Arial,sans-serif;padding:32px;line-height:1.7;color:#1f2937;background:#fafafa;}\n");
        sb.append("    h1{font-size:28px;border-bottom:3px solid #2563eb;padding-bottom:12px;color:#111827;}\n");
        sb.append("    h2{font-size:20px;margin-top:28px;color:#1e40af;border-left:4px solid #3b82f6;padding-left:12px;}\n");
        sb.append("    h3{font-size:16px;margin-top:20px;color:#374151;}\n");
        sb.append("    table{border-collapse:collapse;width:100%;margin:16px 0;background:#fff;box-shadow:0 1px 3px rgba(0,0,0,.08);}\n");
        sb.append("    th{background:#eff6ff;color:#1e3a8a;font-weight:600;text-align:left;}\n");
        sb.append("    th,td{border:1px solid #e5e7eb;padding:10px 12px;font-size:14px;}\n");
        sb.append("    tr:nth-child(even){background:#f9fafb;}\n");
        sb.append("    code,pre{background:#f3f4f6;padding:8px 12px;border-radius:6px;font-family:Consolas,\"Courier New\",monospace;font-size:13px;}\n");
        sb.append("    pre{overflow-x:auto;border:1px solid #e5e7eb;}\n");
        sb.append("    .meta{color:#6b7280;font-size:13px;margin-bottom:24px;}\n");
        sb.append("  </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("<p class=\"meta\">项目：").append(escapeHtml(project.getName()))
                .append(" · 质量分：").append(overall)
                .append(" · 总行数：").append(totalLines).append("</p>\n");
        sb.append(htmlBody);
        sb.append("\n</body>\n</html>");
        String fullHtml = sb.toString();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(fullHtml, null);
            builder.toStream(baos);
            builder.run();
            byte[] pdf = baos.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/projects/{id}/report.md")
    public ResponseEntity<byte[]> exportProjectReportMarkdown(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isEmpty()) {
            return new ResponseEntity<>(new byte[0], HttpStatus.NOT_FOUND);
        }
        Project project = projectOpt.get();
        Analyzer.AnalysisResult ar = buildAnalysisResultForReport(project, id);
        Report.ReportOptions opts = new Report.ReportOptions();
        opts.markdownOutput = true;
        String md = report.generateReport(ar, opts);

        byte[] bytes = md.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/markdown"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id + ".md");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping("/projects")
    public Map<String, Object> createProject(@RequestBody Map<String, Object> body) {
        Project project = new Project();
        Object nameObj = body.get("name");
        String name = nameObj instanceof String s && !s.isBlank() ? s.trim() : "新项目";
        Object descObj = body.get("description");
        String description = descObj instanceof String s ? s : "";
        Object langObj = body.get("language");
        String language = langObj instanceof String s && !s.isBlank() ? s : "javascript";
        project.setName(name);
        project.setDescription(description);
        project.setLanguage(language);
        project.setStatus("active");
        project.setFileCount(0);
        Project saved = projectRepository.save(project);
        return convertProjectToMap(saved, null);
    }

    @GetMapping("/projects/{id}")
    public Map<String, Object> getProject(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            return convertProjectToMapWithLookup(projectOpt.get());
        } else {
            throw new ResourceNotFoundException("项目不存在");
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
            return convertProjectToMapWithLookup(updated);
        } else {
            throw new ResourceNotFoundException("项目不存在");
        }
    }

    @DeleteMapping("/projects/{id}")
    @Transactional
    public Map<String, Object> deleteProject(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            try {
                analysisService.cancelAnalysis(id);
            } catch (Exception e) {
                log.debug("删除项目前取消分析（可忽略）: projectId={}, {}", id, e.getMessage());
            }
            Path base = appProperties.projectDir(id);
            if (Files.exists(base)) {
                try (Stream<Path> walk = Files.walk(base)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ex) {
                            log.warn("删除项目文件失败: {}", p, ex);
                        }
                    });
                } catch (IOException e) {
                    log.warn("遍历项目目录失败: {}", base, e);
                }
            }

            projectRepository.delete(project);
            return Map.<String, Object>of("message", "项目删除成功", "id", id);
        } else {
            throw new ResourceNotFoundException("项目不存在");
        }
    }

    // -------------------------- 文件管理 --------------------------
    @GetMapping("/projects/{id}/files")
    public Map<String, Object> listFiles(@PathVariable String id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("项目不存在");
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
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("项目不存在");
        }

        // 优先按 DB 中的完整 filePath 解析；否则回退到 path 的文件名
        Path absPath = resolveProjectFilePath(id, path);
        if (absPath == null || !Files.exists(absPath)) {
            throw new ResourceNotFoundException("文件不存在: " + path);
        }

        long size = Files.size(absPath);
        if (size > MAX_FILE_CONTENT_BYTES) {
            throw new BadRequestException("文件过大，单文件内容读取上限为 5MB");
        }

        String content = Files.readString(absPath, StandardCharsets.UTF_8);
        return Map.of("path", path, "content", content, "size", size);
    }

    @PostMapping(value = "/projects/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@PathVariable String id, @RequestPart("files") MultipartFile[] files) throws IOException {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("项目不存在");
        }
        if (files == null || files.length == 0) {
            throw new BadRequestException("未选择任何文件");
        }

        int existingCount = fileRepository.findByProjectId(id).size();
        if (existingCount + files.length > MAX_FILES_PER_PROJECT) {
            throw new BadRequestException(
                    "单项目文件数不能超过 " + MAX_FILES_PER_PROJECT + "（当前已有 " + existingCount + "，本次上传 " + files.length + "）");
        }

        List<ProjectFile> uploadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Path baseDir = appProperties.projectDir(id);
        Files.createDirectories(baseDir);
        Instant uploadTime = Instant.now();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                errors.add("空文件已跳过");
                continue;
            }
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), ""));
            if (originalFilename.isBlank() || originalFilename.contains("..")) {
                errors.add("非法文件名已跳过");
                continue;
            }
            if (!isAllowedExtension(originalFilename)) {
                errors.add(originalFilename + ": 不支持的文件类型");
                continue;
            }

            String storedFilename = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = baseDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath);

            ProjectFile projectFile = new ProjectFile();
            projectFile.setProjectId(id);
            projectFile.setFilename(originalFilename);
            projectFile.setFilePath("/uploads/" + id + "/" + storedFilename);
            projectFile.setFileSize(file.getSize());
            projectFile.setUploadTime(uploadTime);
            uploadedFiles.add(fileRepository.save(projectFile));
        }

        if (uploadedFiles.isEmpty()) {
            throw new BadRequestException("没有成功上传任何文件"
                    + (errors.isEmpty() ? "" : "：" + String.join("; ", errors)));
        }

        projectRepository.findById(id).ifPresent(project -> {
            project.setFileCount(fileRepository.findByProjectId(id).size());
            project.setUpdatedAt(Instant.now());
            projectRepository.save(project);
        });

        embeddingService.clearIndexed(id);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", "成功上传 " + uploadedFiles.size() + " 个文件");
        resp.put("uploadedFiles", uploadedFiles.stream().map(this::convertToMap).collect(Collectors.toList()));
        resp.put("errors", errors);
        resp.put("totalFiles", files.length);
        return resp;
    }

    @DeleteMapping("/projects/{id}/files/{fileId}")
    @Transactional
    public Map<String, Object> deleteFile(@PathVariable String id, @PathVariable String fileId) {
        Optional<ProjectFile> fileOpt = fileRepository.findById(fileId);
        if (fileOpt.isPresent()) {
            ProjectFile file = fileOpt.get();
            if (file.getProjectId().equals(id)) {
                // 删除本地文件
                String filename = Paths.get(file.getFilePath()).getFileName().toString();
                Path absPath = appProperties.resolveStoredFile(id, filename);
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
            }
        }
        throw new ResourceNotFoundException("文件不存在");
    }

    @DeleteMapping("/projects/{id}/files")
    @Transactional
    public Map<String, Object> deleteFileByPath(@PathVariable String id, @RequestParam("path") String path) {
        Optional<ProjectFile> fileOpt = fileRepository.findByProjectIdAndFilePath(id, path);
        if (fileOpt.isPresent()) {
            ProjectFile file = fileOpt.get();
            String filename = Paths.get(path).getFileName().toString();
            Path absPath = appProperties.resolveStoredFile(id, filename);
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
            throw new ResourceNotFoundException("文件不存在");
        }
    }

    // -------------------------- 分析管理 --------------------------
    @PostMapping("/projects/{id}/analyze")
    public Map<String, Object> analyze(@PathVariable String id) {
        return analysisService.startAnalysis(id);
    }

    @DeleteMapping("/projects/{id}/analyze")
    public Map<String, Object> cancelAnalyze(@PathVariable String id) {
        return analysisService.cancelAnalysis(id);
    }

    @PostMapping("/projects/{id}/reanalyze")
    public Map<String, Object> reanalyze(@PathVariable String id) {
        // 重新分析等价于再次触发 analyze：每次新建 Analysis 记录，历史自动保留
        return analysisService.startAnalysis(id);
    }

    @GetMapping("/projects/{id}/analysis")
    public Map<String, Object> getAnalysis(@PathVariable String id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("项目不存在");
        }

        Optional<Analysis> analysisOpt = analysisRepository.findTopByProjectIdOrderByCreatedAtDesc(id);
        if (analysisOpt.isPresent()) {
            Analysis analysis = analysisOpt.get();
            Project project = projectRepository.findById(id).get();
            List<Issue> issues = analysis.getIssues() != null ? analysis.getIssues() : List.of();
            List<Dimension> dimensions = analysis.getDimensions() != null ? analysis.getDimensions() : List.of();

            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("fileCount", project.getFileCount());
            metrics.put("totalIssues", issues.size());
            metrics.put("criticalIssues", issues.stream().filter(i -> "high".equalsIgnoreCase(i.getSeverity())).count());
            metrics.put("majorIssues", issues.stream().filter(i -> "medium".equalsIgnoreCase(i.getSeverity())).count());
            metrics.put("minorIssues", issues.stream().filter(i -> "low".equalsIgnoreCase(i.getSeverity())).count());

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("suggestions", dimensions.stream()
                    .map(d -> report.translateSuggestion(d.getName()))
                    .collect(Collectors.toList()));

            List<Map<String, Object>> issuesOut = issues.stream().map(iss -> {
                Map<String, Object> m = convertToMap(iss);
                String raw = (String) m.get("file");
                String friendly = toFriendlyFilename(id, raw);
                m.put("file", friendly);
                return m;
            }).collect(Collectors.toList());

            // 直接读取已持久化的文件级分析结果，避免每次请求重复静态分析
            List<Map<String, Object>> fileResults = fileAnalysisResultRepository.findByAnalysisId(analysis.getId()).stream()
                    .map(fr -> {
                        Map<String, Object> fm = new LinkedHashMap<>();
                        fm.put("file", fr.getFilename());
                        fm.put("score", fr.getScore());
                        fm.put("issues", fr.getIssues());
                        fm.put("linesOfCode", fr.getLinesOfCode());
                        fm.put("complexity", fr.getComplexity());
                        return fm;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("projectId", id);
            resp.put("projectName", project.getName());
            resp.put("overallScore", analysis.getOverallScore());
            resp.put("grade", analysis.getGrade());
            resp.put("dimensions", dimensions.stream().map(this::convertToMap).collect(Collectors.toList()));
            resp.put("issues", issuesOut);
            resp.put("fileResults", fileResults);
            resp.put("completedAt", analysis.getCreatedAt().toString());
            resp.put("taskId", analysis.getId());
            resp.put("metrics", metrics);
            resp.put("summary", summary);
            return resp;
        } else {
            throw new ResourceNotFoundException("该项目暂无分析结果");
        }
    }

    private String toFriendlyFilename(String projectId, String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) return "";
        String stored = Paths.get(rawPath).getFileName().toString();
        try {
            List<ProjectFile> pfiles = fileRepository.findByProjectId(projectId);
            for (ProjectFile pf : pfiles) {
                String fp = Paths.get(pf.getFilePath()).getFileName().toString();
                if (stored.equals(fp)) {
                    return pf.getFilename();
                }
            }
        } catch (Exception ignored) {}
        int idx = stored.indexOf('_');
        return idx >= 0 ? stored.substring(idx + 1) : stored;
    }

    @GetMapping("/projects/{id}/analysis/status")
    public Map<String, Object> getStatus(@PathVariable String id) {
        return analysisService.getStatus(id);
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
                for (Dimension dim : analysis.getDimensions() != null ? analysis.getDimensions() : List.<Dimension>of()) {
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
            throw new ResourceNotFoundException("项目不存在");
        }
    }

    @GetMapping("/projects/{id}/analysis/history")
    public List<Map<String, Object>> getHistory(@PathVariable String id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("项目不存在");
        }

        List<Analysis> analyses = analysisRepository.findByProjectIdOrderByCreatedAtDesc(id);
        if (analyses.isEmpty()) {
            return List.of();
        }

        return analyses.stream()
                .map(analysis -> Map.of(
                        "taskId", analysis.getId(),
                        "completedAt", analysis.getCreatedAt().toString(),
                        "overallScore", analysis.getOverallScore(),
                        "grade", analysis.getGrade(),
                        "dimensions", (analysis.getDimensions() != null ? analysis.getDimensions() : List.<Dimension>of())
                                .stream().map(this::convertToMap).collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    // -------------------------- 辅助接口 --------------------------
    @PostMapping("/projects/{id}/git")
    public Map<String, Object> importGit(@PathVariable String id, @RequestBody Map<String, Object> body) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("项目不存在");
        }
        // Git 导入尚未实现（需 JGit clone）；明确返回 501，避免前端误以为已导入成功
        throw new UnsupportedOperationException("Git 仓库导入功能尚未实现，请使用本地文件上传");
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
            newProject.setFileCount(0);
            Project savedProject = projectRepository.save(newProject);

            // 2. 复制项目文件元数据，并同步拷贝磁盘文件
            Path sourceBase = appProperties.projectDir(id);
            Path targetBase = appProperties.projectDir(savedProject.getId());
            try {
                Files.createDirectories(targetBase);
            } catch (IOException e) {
                throw new RuntimeException("创建副本目录失败: " + e.getMessage(), e);
            }

            List<ProjectFile> sourceFiles = fileRepository.findByProjectId(id);
            List<ProjectFile> newFiles = new ArrayList<>();
            for (ProjectFile file : sourceFiles) {
                String sourceStoredName = Paths.get(file.getFilePath()).getFileName().toString();
                Path sourceAbs = sourceBase.resolve(sourceStoredName);
                String newStoredFilename = UUID.randomUUID() + "_" + file.getFilename();
                Path targetAbs = targetBase.resolve(newStoredFilename);

                if (Files.exists(sourceAbs)) {
                    try {
                        Files.copy(sourceAbs, targetAbs, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("复制文件失败: " + file.getFilename() + " - " + e.getMessage(), e);
                    }
                }

                ProjectFile newFile = new ProjectFile();
                newFile.setProjectId(savedProject.getId());
                newFile.setFilename(file.getFilename());
                newFile.setFilePath("/uploads/" + savedProject.getId() + "/" + newStoredFilename);
                newFile.setFileSize(file.getFileSize());
                newFile.setUploadTime(Instant.now());
                newFiles.add(newFile);
            }
            if (!newFiles.isEmpty()) {
                fileRepository.saveAll(newFiles);
            }
            savedProject.setFileCount(newFiles.size());
            projectRepository.save(savedProject);

            return convertProjectToMap(savedProject, null);
        } else {
            throw new ResourceNotFoundException("项目不存在");
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
            return convertProjectToMapWithLookup(updated);
        } else {
            throw new ResourceNotFoundException("项目不存在");
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
            return convertProjectToMapWithLookup(updated);
        } else {
            throw new ResourceNotFoundException("项目不存在");
        }
    }

    // -------------------------- 工具方法 --------------------------

    /** 批量预加载 lastAnalysis，消除列表接口 N+1 */
    private Map<String, Analysis> loadLastAnalyses(List<Project> projects) {
        Set<String> ids = projects.stream()
                .map(Project::getLastAnalysisId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return analysisRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Analysis::getId, a -> a, (a, b) -> a));
    }

    private Map<String, Object> convertProjectToMapWithLookup(Project project) {
        Analysis last = null;
        if (project.getLastAnalysisId() != null) {
            last = analysisRepository.findById(project.getLastAnalysisId()).orElse(null);
        }
        return convertProjectToMap(project, last);
    }

    private Map<String, Object> convertProjectToMap(Project project, Analysis lastAnalysis) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", project.getId());
        map.put("name", project.getName());
        map.put("description", project.getDescription());
        map.put("language", project.getLanguage());
        map.put("status", project.getStatus());
        map.put("created_at", project.getCreatedAt() != null ? project.getCreatedAt().toString() : Instant.now().toString());
        map.put("updated_at", project.getUpdatedAt() != null ? project.getUpdatedAt().toString() : Instant.now().toString());
        map.put("file_count", project.getFileCount());

        if (lastAnalysis != null) {
            Map<String, Object> last = new LinkedHashMap<>();
            last.put("overallScore", lastAnalysis.getOverallScore());
            last.put("grade", lastAnalysis.getGrade());
            last.put("completedAt", lastAnalysis.getCreatedAt() != null ? lastAnalysis.getCreatedAt().toString() : "");
            last.put("taskId", lastAnalysis.getId());
            map.put("lastAnalysis", last);
        }
        return map;
    }

    private Analyzer.AnalysisResult buildAnalysisResultForReport(Project project, String projectId) {
        Optional<Analysis> latestOpt = analysisRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId);
        int overall = latestOpt.map(Analysis::getOverallScore).orElse(0);
        long totalLines = resolveTotalLines(projectId, latestOpt);
        Analyzer.AnalysisResult ar = new Analyzer.AnalysisResult(
                (100 - overall) / 100.0,
                new HashMap<>(),
                new ArrayList<>(),
                project.getFileCount(),
                (int) Math.min(Integer.MAX_VALUE, totalLines)
        );
        latestOpt.ifPresent(latest -> {
            List<Dimension> dims = latest.getDimensions() != null ? latest.getDimensions() : List.of();
            for (Dimension d : dims) {
                ar.addMetric(d.getName(), new Analyzer.MetricResult(
                        d.getName(),
                        Math.max(0.0, Math.min(1.0, (100 - d.getScore()) / 100.0)),
                        d.getName(),
                        0.1
                ));
            }
            List<Issue> issues = latest.getIssues() != null ? latest.getIssues() : List.of();
            Map<String, List<Issue>> byFile = new LinkedHashMap<>();
            for (Issue is : issues) {
                String filePath = is.getFile() != null ? is.getFile() : "unknown";
                byFile.computeIfAbsent(filePath, k -> new ArrayList<>()).add(is);
            }
            for (Map.Entry<String, List<Issue>> e : byFile.entrySet()) {
                ar.addFileAnalysisResult(new Analyzer.FileAnalysisResult(
                        e.getKey(),
                        Math.max(0.0, Math.min(1.0, (100 - overall) / 100.0)),
                        e.getValue()
                ));
            }
        });
        return ar;
    }

    /** 解析项目文件的绝对路径：仅匹配 DB 记录，无记录时不回退磁盘直读 */
    private Path resolveProjectFilePath(String projectId, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Optional<ProjectFile> byPath = fileRepository.findByProjectIdAndFilePath(projectId, path);
        if (byPath.isPresent()) {
            String stored = Paths.get(byPath.get().getFilePath()).getFileName().toString();
            return appProperties.resolveStoredFile(projectId, stored);
        }
        String filename = Paths.get(path).getFileName().toString();
        List<ProjectFile> files = fileRepository.findByProjectId(projectId);
        for (ProjectFile pf : files) {
            String stored = Paths.get(pf.getFilePath()).getFileName().toString();
            if (stored.equals(filename) || pf.getFilename().equals(filename) || pf.getFilePath().equals(path)) {
                return appProperties.resolveStoredFile(projectId, stored);
            }
        }
        return null;
    }

    /** 优先使用已持久化的文件行数，避免报告导出时重复读盘 */
    private long resolveTotalLines(String projectId, Optional<Analysis> latestOpt) {
        if (latestOpt.isPresent()) {
            long sum = fileAnalysisResultRepository.sumLinesOfCodeByAnalysisId(latestOpt.get().getId());
            if (sum > 0) {
                return sum;
            }
        }
        long total = 0;
        for (ProjectFile pf : fileRepository.findByProjectId(projectId)) {
            String storedName = Paths.get(pf.getFilePath()).getFileName().toString();
            Path abs = appProperties.resolveStoredFile(projectId, storedName);
            if (Files.exists(abs)) {
                try (Stream<String> lines = Files.lines(abs, StandardCharsets.UTF_8)) {
                    total += lines.count();
                } catch (IOException e) {
                    log.debug("读取文件行数失败: {}", abs);
                }
            }
        }
        return total;
    }

    private boolean isAllowedExtension(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(lower.substring(dot));
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // 实体转Map（适配原接口返回格式）
    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Project project) {
            return convertProjectToMapWithLookup(project);
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
}
