package com.bugkiller_cityu.code_diagnostician.controller;

import com.bugkiller_cityu.code_diagnostician.entity.*;
import com.bugkiller_cityu.code_diagnostician.repository.*;
import com.bugkiller_cityu.code_diagnostician.exception.ResourceNotFoundException;
import com.bugkiller_cityu.code_diagnostician.exception.AnalysisFailedException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.bugkiller_cityu.code_diagnostician.service.model.report.Report;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer;

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
    private final FileAnalysisResultRepository fileAnalysisResultRepository;
    private final com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.CodeAnalyzer codeAnalyzer;
    private final Report report;

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

    @GetMapping(value = "/projects/{id}/report.pdf")
    public ResponseEntity<byte[]> exportProjectReportPdf(@PathVariable String id) {
        Optional<Project> projectOpt = projectRepository.findById(id);
        if (projectOpt.isEmpty()) {
            return new ResponseEntity<>(new byte[0], HttpStatus.NOT_FOUND);
        }
        Project project = projectOpt.get();
        Optional<Analysis> latestOpt = analysisRepository.findTopByProjectIdOrderByCreatedAtDesc(id);
        int overall = latestOpt.map(Analysis::getOverallScore).orElse(0);
        int totalLines = 0;
        try {
            List<ProjectFile> pfiles = fileRepository.findByProjectId(id);
            for (ProjectFile pf : pfiles) {
                String storedName = Paths.get(pf.getFilePath()).getFileName().toString();
                Path abs = Paths.get("uploads").resolve(id).resolve(storedName);
                if (Files.exists(abs)) {
                    totalLines += (int) Files.lines(abs).count();
                }
            }
        } catch (Exception ignored) {}
        Analyzer.AnalysisResult ar = new Analyzer.AnalysisResult(
                (100 - overall) / 100.0,
                new HashMap<>(),
                new ArrayList<>(),
                project.getFileCount(),
                totalLines
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
        Report.ReportOptions opts = new Report.ReportOptions();
        opts.markdownOutput = true;
        String md = report.generateReport(ar, opts);

        Parser parser = Parser.builder().build();
        Node document = parser.parse(md);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String htmlBody = renderer.render(document);
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        sb.append("<head>\n");
        sb.append("  <meta charset=\"utf-8\"/>\n");
        sb.append("  <title>报告</title>\n");
        sb.append("  <style>body{font-family:Arial,\"Microsoft YaHei\",sans-serif;padding:24px;line-height:1.6;} h1,h2,h3{margin:16px 0;} table{border-collapse:collapse;width:100%;} th,td{border:1px solid #ddd;padding:8px;} code,pre{background:#f6f8fa;padding:8px;border-radius:4px;} </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
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

        Optional<Analysis> latestOpt = analysisRepository.findTopByProjectIdOrderByCreatedAtDesc(id);
        int overall = latestOpt.map(Analysis::getOverallScore).orElse(0);
        int totalLines = 0;
        try {
            List<ProjectFile> pfiles = fileRepository.findByProjectId(id);
            for (ProjectFile pf : pfiles) {
                String storedName = Paths.get(pf.getFilePath()).getFileName().toString();
                Path abs = Paths.get("uploads").resolve(id).resolve(storedName);
                if (Files.exists(abs)) {
                    totalLines += (int) Files.lines(abs).count();
                }
            }
        } catch (Exception ignored) {}
        Analyzer.AnalysisResult ar = new Analyzer.AnalysisResult(
                (100 - overall) / 100.0,
                new HashMap<>(),
                new ArrayList<>(),
                project.getFileCount(),
                totalLines
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
            return convertToMap(enrichProjectWithLastAnalysis(updated));
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
            throw new ResourceNotFoundException("项目不存在");
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
                throw new ResourceNotFoundException("文件不属于该项目");
            }
        } else {
            throw new ResourceNotFoundException("文件不存在");
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
            throw new ResourceNotFoundException("文件不存在");
        }
    }

    // -------------------------- 分析管理 --------------------------
    @PostMapping("/projects/{id}/analyze")
    @Transactional
    public Map<String, Object> analyze(@PathVariable String id) {
        System.out.println("AnalyseRe");
        try {
            Optional<Project> projectOpt = projectRepository.findById(id);
            if (projectOpt.isEmpty()) {
                throw new ResourceNotFoundException("项目不存在");
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
            List<FileAnalysisResult> fileAnalysisResults = new ArrayList<>();
            List<String> analysisErrors = new ArrayList<>();
            for (ProjectFile f : files) {
                String storedName = Paths.get(f.getFilePath()).getFileName().toString();
                Path abs = Paths.get("uploads").resolve(id).resolve(storedName);
                if (!Files.exists(abs)) {
                    continue;
                }
                try {
                    com.bugkiller_cityu.code_diagnostician.service.model.metric.AnalysisResult r = codeAnalyzer.analyzeFile(abs.toString());
                    fileResults.add(r);
                    // 持久化文件级分析结果，避免 GET /analysis 每次重复静态分析
                    int lines = r.getTotalLines();
                    int issuesCount = r.getIssues() != null ? r.getIssues().size() : 0;
                    int complexity = 0;
                    var ccMr = r.getMetricResults().get("圈复杂度分析");
                    if (ccMr != null && ccMr.getIssues() != null) complexity = ccMr.getIssues().size();
                    int fileScore = (int) Math.round((1.0 - codeAnalyzer.calculateOverallScore(java.util.List.of(r))) * 100.0);
                    FileAnalysisResult far = new FileAnalysisResult();
                    far.setAnalysisId(savedAnalysis.getId());
                    far.setFilename(f.getFilename());
                    far.setScore(fileScore);
                    far.setLinesOfCode(lines);
                    far.setIssues(issuesCount);
                    far.setComplexity(complexity);
                    fileAnalysisResults.add(far);
                } catch (Exception e) {
                    e.printStackTrace();
                    analysisErrors.add(f.getFilename() + ": " + e.getMessage());
                }
            }

            // 全部文件均分析失败时抛错，避免 riskScore=0 被误判为"满分"
            if (!files.isEmpty() && fileResults.isEmpty()) {
                throw new AnalysisFailedException("项目分析失败，所有文件均无法分析" + (analysisErrors.isEmpty() ? "" : "：" + String.join("; ", analysisErrors)));
            }

            double riskScore = fileResults.isEmpty() ? 0.0 : codeAnalyzer.calculateOverallScore(fileResults);
            // 底层 riskScore 为风险分（0-1，越高越差），统一换算为质量分（0-100，越高越好）后再入库
            int overallInt = fileResults.isEmpty() ? 0 : (int) Math.round((1.0 - riskScore) * 100.0);
            String grade = overallInt >= 80 ? "excellent" : overallInt >= 60 ? "good" : "poor";

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
                double avgRisk = mrs.stream().mapToDouble(com.bugkiller_cityu.code_diagnostician.service.model.metric.MetricResult::getScore).average().orElse(0.0);
                int issuesCount = mrs.stream().mapToInt(m -> m.getIssues() == null ? 0 : m.getIssues().size()).sum();
                int dimScore = (int) Math.round((1.0 - avgRisk) * 100.0);
                dimensions.add(createDimension(savedAnalysis.getId(), name, dimScore, 100, issuesCount));
            }
            if (!dimensions.isEmpty()) dimensionRepository.saveAll(dimensions);
            if (!fileAnalysisResults.isEmpty()) fileAnalysisResultRepository.saveAll(fileAnalysisResults);

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
        } catch (AnalysisFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new AnalysisFailedException("分析失败：" + e.getMessage());
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
            throw new ResourceNotFoundException("项目不存在");
        }
    }

    @PostMapping("/projects/{id}/reanalyze")
    @Transactional
    public Map<String, Object> reanalyze(@PathVariable String id) {
        // 重新分析等价于再次触发 analyze：每次新建 Analysis 记录，历史自动保留
        return analyze(id);
    }

    @GetMapping("/projects/{id}/analysis")
    public Map<String, Object> getAnalysis(@PathVariable String id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("项目不存在");
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
            metrics.put("criticalIssues", issues.stream().filter(i -> "high".equalsIgnoreCase(i.getSeverity())).count());
            metrics.put("majorIssues", issues.stream().filter(i -> "medium".equalsIgnoreCase(i.getSeverity())).count());
            metrics.put("minorIssues", issues.stream().filter(i -> "low".equalsIgnoreCase(i.getSeverity())).count());

            // 优化建议
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("suggestions", analysis.getDimensions().stream()
                    .map(d -> String.format("优化%s相关代码", d.getName()))
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
            resp.put("dimensions", analysis.getDimensions().stream().map(this::convertToMap).collect(Collectors.toList()));
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
            throw new ResourceNotFoundException("项目不存在");
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
            throw new ResourceNotFoundException("项目不存在");
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
            return convertToMap(enrichProjectWithLastAnalysis(updated));
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
            return convertToMap(enrichProjectWithLastAnalysis(updated));
        } else {
            throw new ResourceNotFoundException("项目不存在");
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
