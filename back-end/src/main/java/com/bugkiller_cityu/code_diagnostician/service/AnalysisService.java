package com.bugkiller_cityu.code_diagnostician.service;

import com.bugkiller_cityu.code_diagnostician.config.AppProperties;
import com.bugkiller_cityu.code_diagnostician.entity.*;
import com.bugkiller_cityu.code_diagnostician.exception.AnalysisFailedException;
import com.bugkiller_cityu.code_diagnostician.exception.ResourceNotFoundException;
import com.bugkiller_cityu.code_diagnostician.repository.*;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository fileRepository;
    private final AnalysisRepository analysisRepository;
    private final DimensionRepository dimensionRepository;
    private final IssueRepository issueRepository;
    private final FileAnalysisResultRepository fileAnalysisResultRepository;
    private final Analyzer.CodeAnalyzer codeAnalyzer;
    private final AnalysisProgressTracker progressTracker;
    private final AnalysisWebSocketHub webSocketHub;
    private final AnalysisAsyncLauncher asyncLauncher;
    private final TransactionTemplate transactionTemplate;
    private final AppProperties appProperties;

    public AnalysisService(
            ProjectRepository projectRepository,
            ProjectFileRepository fileRepository,
            AnalysisRepository analysisRepository,
            DimensionRepository dimensionRepository,
            IssueRepository issueRepository,
            FileAnalysisResultRepository fileAnalysisResultRepository,
            Analyzer.CodeAnalyzer codeAnalyzer,
            AnalysisProgressTracker progressTracker,
            AnalysisWebSocketHub webSocketHub,
            AnalysisAsyncLauncher asyncLauncher,
            PlatformTransactionManager transactionManager,
            AppProperties appProperties
    ) {
        this.projectRepository = projectRepository;
        this.fileRepository = fileRepository;
        this.analysisRepository = analysisRepository;
        this.dimensionRepository = dimensionRepository;
        this.issueRepository = issueRepository;
        this.fileAnalysisResultRepository = fileAnalysisResultRepository;
        this.codeAnalyzer = codeAnalyzer;
        this.progressTracker = progressTracker;
        this.webSocketHub = webSocketHub;
        this.asyncLauncher = asyncLauncher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.appProperties = appProperties;
    }

    /**
     * 同步启动分析：校验项目、创建 Analysis 记录、标记 analyzing，并提交异步任务。
     * 立即返回 taskId，避免阻塞 HTTP 请求。
     */
    @Transactional
    public Map<String, Object> startAnalysis(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("项目不存在"));

        if (progressTracker.isAnalyzing(projectId)) {
            throw new AnalysisFailedException("该项目已有分析任务正在进行");
        }

        List<ProjectFile> existingFiles = fileRepository.findByProjectId(projectId);
        if (existingFiles.isEmpty()) {
            throw new AnalysisFailedException("项目中没有可分析的文件，请先上传源码");
        }

        Analysis analysis = new Analysis();
        analysis.setProjectId(projectId);
        analysis.setOverallScore(0);
        analysis.setGrade("good");
        analysis.setCreatedAt(Instant.now());
        Analysis savedAnalysis = analysisRepository.save(analysis);

        project.setStatus("analyzing");
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);

        AnalysisProgress progress = progressTracker.start(projectId, savedAnalysis.getId());
        progress.setTotalFiles(existingFiles.size());
        progress.setStage("queued");
        progress.setMessage("分析任务已排队");

        pushProgress(projectId, progress);

        // 事务提交后再启动异步任务，避免异步线程读不到未提交的 Analysis
        final String analysisId = savedAnalysis.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    asyncLauncher.launch(projectId, analysisId);
                }
            });
        } else {
            asyncLauncher.launch(projectId, analysisId);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", "分析任务已启动");
        resp.put("taskId", analysisId);
        resp.put("analysisId", analysisId);
        resp.put("status", "analyzing");
        resp.put("progress", 0);
        return resp;
    }

    /**
     * 异步执行入口。文件分析不包长事务，仅在最终落库时短事务提交。
     */
    public void executeAnalysis(String projectId, String analysisId) {
        try {
            doAnalyze(projectId, analysisId);
        } catch (AnalysisFailedException e) {
            if (!progressTracker.get(projectId)
                    .map(p -> "error".equals(p.getStatus()) || "cancelled".equals(p.getStatus()))
                    .orElse(false)) {
                progressTracker.fail(projectId, e.getMessage());
                markProjectError(projectId);
                webSocketHub.send(projectId, "analysis_error", Map.of("error", e.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            String err = e.getMessage() != null ? e.getMessage() : "分析失败";
            progressTracker.fail(projectId, err);
            markProjectError(projectId);
            webSocketHub.send(projectId, "analysis_error", Map.of("error", err));
        }
    }

    private void doAnalyze(String projectId, String analysisId) {
        AnalysisProgress progress = progressTracker.get(projectId)
                .orElseGet(() -> progressTracker.start(projectId, analysisId));

        Analysis savedAnalysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisFailedException("分析记录不存在"));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("项目不存在"));

        List<ProjectFile> files = fileRepository.findByProjectId(projectId);
        progress.setTotalFiles(files.size());
        progress.setStage("analyzing");
        progress.setMessage("开始分析文件");
        pushProgress(projectId, progress);

        List<com.bugkiller_cityu.code_diagnostician.service.model.metric.AnalysisResult> fileResults = new ArrayList<>();
        List<FileAnalysisResult> fileAnalysisResults = new ArrayList<>();
        List<String> analysisErrors = new ArrayList<>();
        int processed = 0;

        for (ProjectFile f : files) {
            if (progress.isCancelRequested()) {
                markCancelled(projectId, project);
                return;
            }

            progressTracker.update(projectId, processed, files.size(), f.getFilename(), "analyzing",
                    "正在分析: " + f.getFilename());
            pushProgress(projectId, progressTracker.get(projectId).orElse(progress));

            String storedName = Paths.get(f.getFilePath()).getFileName().toString();
            Path abs = appProperties.resolveStoredFile(projectId, storedName);
            if (!Files.exists(abs)) {
                analysisErrors.add(f.getFilename() + ": 物理文件缺失");
                processed++;
                progressTracker.update(projectId, processed, files.size(), f.getFilename(), "analyzing",
                        "跳过缺失文件: " + f.getFilename());
                pushProgress(projectId, progressTracker.get(projectId).orElse(progress));
                continue;
            }
            try {
                com.bugkiller_cityu.code_diagnostician.service.model.metric.AnalysisResult r =
                        codeAnalyzer.analyzeFile(abs.toString());
                fileResults.add(r);
                int lines = r.getTotalLines();
                int issuesCount = r.getIssues() != null ? r.getIssues().size() : 0;
                int complexity = 0;
                var ccMr = r.getMetricResults().get("圈复杂度分析");
                if (ccMr != null && ccMr.getIssues() != null) {
                    complexity = ccMr.getIssues().size();
                }
                int fileScore = (int) Math.round(
                        (1.0 - codeAnalyzer.calculateOverallScore(List.of(r))) * 100.0);
                FileAnalysisResult far = new FileAnalysisResult();
                far.setAnalysisId(savedAnalysis.getId());
                far.setFilename(f.getFilename());
                far.setScore(fileScore);
                far.setLinesOfCode(lines);
                far.setIssues(issuesCount);
                far.setComplexity(complexity);
                fileAnalysisResults.add(far);
            } catch (Exception e) {
                System.err.println("[Analysis] 文件分析失败: " + f.getFilename() + " - " + e.getMessage());
                analysisErrors.add(f.getFilename() + ": " + e.getMessage());
            }

            processed++;
            progressTracker.update(projectId, processed, files.size(), f.getFilename(), "analyzing",
                    "已完成: " + f.getFilename());
            pushProgress(projectId, progressTracker.get(projectId).orElse(progress));
        }

        if (progress.isCancelRequested()) {
            markCancelled(projectId, project);
            return;
        }

        if (!files.isEmpty() && fileResults.isEmpty()) {
            String err = "项目分析失败，所有文件均无法分析"
                    + (analysisErrors.isEmpty() ? "" : "：" + String.join("; ", analysisErrors));
            progressTracker.fail(projectId, err);
            markProjectError(projectId);
            webSocketHub.send(projectId, "analysis_error", Map.of("error", err));
            return;
        }

        progressTracker.update(projectId, processed, files.size(), "", "aggregating", "汇总分析结果");
        pushProgress(projectId, progressTracker.get(projectId).orElse(progress));

        double riskScore = fileResults.isEmpty() ? 0.0 : codeAnalyzer.calculateOverallScore(fileResults);
        int overallInt = fileResults.isEmpty() ? 0 : (int) Math.round((1.0 - riskScore) * 100.0);
        String grade = overallInt >= 80 ? "excellent" : overallInt >= 60 ? "good" : "poor";

        Map<String, List<com.bugkiller_cityu.code_diagnostician.service.model.metric.MetricResult>> metricsMap =
                new LinkedHashMap<>();
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
            double avgRisk = mrs.stream()
                    .mapToDouble(com.bugkiller_cityu.code_diagnostician.service.model.metric.MetricResult::getScore)
                    .average().orElse(0.0);
            int issuesCount = mrs.stream()
                    .mapToInt(m -> m.getIssues() == null ? 0 : m.getIssues().size()).sum();
            int dimScore = (int) Math.round((1.0 - avgRisk) * 100.0);
            dimensions.add(createDimension(savedAnalysis.getId(), name, dimScore, 100, issuesCount));
        }

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

        persistAnalysisResult(projectId, analysisId, overallInt, grade, dimensions, fileAnalysisResults, allIssues);

        Project completedProject = projectRepository.findById(projectId).orElse(project);
        Analysis completedAnalysis = analysisRepository.findById(analysisId).orElse(savedAnalysis);

        progressTracker.complete(projectId);

        Map<String, Object> result = buildResultPayload(
                projectId, completedProject, completedAnalysis, dimensions, allIssues);
        webSocketHub.send(projectId, "analysis_completed", Map.of("result", result));
        pushProgress(projectId, progressTracker.get(projectId).orElse(progress));
    }

    private void persistAnalysisResult(
            String projectId,
            String analysisId,
            int overallInt,
            String grade,
            List<Dimension> dimensions,
            List<FileAnalysisResult> fileAnalysisResults,
            List<Issue> allIssues
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            Analysis savedAnalysis = analysisRepository.findById(analysisId)
                    .orElseThrow(() -> new AnalysisFailedException("分析记录不存在"));
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("项目不存在"));

            // 子实体通过 analysisId 外键独立落库；切勿对带 orphanRemoval 的集合做整表 set 替换，
            // 否则 Hibernate 会报 "collection ... was no longer referenced by the owning entity"
            if (!dimensions.isEmpty()) {
                dimensionRepository.saveAll(dimensions);
            }
            if (!fileAnalysisResults.isEmpty()) {
                fileAnalysisResultRepository.saveAll(fileAnalysisResults);
            }
            if (!allIssues.isEmpty()) {
                issueRepository.saveAll(allIssues);
            }

            savedAnalysis.setOverallScore(overallInt);
            savedAnalysis.setGrade(grade);
            analysisRepository.save(savedAnalysis);

            project.setStatus("completed");
            project.setLastAnalysisId(savedAnalysis.getId());
            project.setUpdatedAt(Instant.now());
            projectRepository.save(project);
        });
    }

    private void markCancelled(String projectId, Project project) {
        progressTracker.cancel(projectId);
        transactionTemplate.executeWithoutResult(status -> {
            Project p = projectRepository.findById(projectId).orElse(project);
            p.setStatus("cancelled");
            p.setUpdatedAt(Instant.now());
            projectRepository.save(p);
        });
        webSocketHub.send(projectId, "analysis_error", Map.of("error", "分析已取消"));
    }

    public Map<String, Object> getStatus(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("项目不存在"));

        Optional<AnalysisProgress> progressOpt = progressTracker.get(projectId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("projectId", projectId);
        resp.put("updatedAt", project.getUpdatedAt() != null ? project.getUpdatedAt().toString() : Instant.now().toString());

        if (progressOpt.isPresent()) {
            AnalysisProgress p = progressOpt.get();
            resp.put("status", p.getStatus());
            resp.put("progress", p.getProgressPercent());
            resp.put("processedFiles", p.getProcessedFiles());
            resp.put("totalFiles", p.getTotalFiles());
            resp.put("currentFile", p.getCurrentFile());
            resp.put("stage", p.getStage());
            resp.put("message", p.getMessage());
            resp.put("taskId", p.getAnalysisId());
            if (p.getError() != null) {
                resp.put("error", p.getError());
            }
        } else {
            String status = project.getStatus();
            int progress = "completed".equals(status) ? 100 : 0;
            resp.put("status", status);
            resp.put("progress", progress);
            resp.put("processedFiles", 0);
            resp.put("totalFiles", project.getFileCount() != null ? project.getFileCount() : 0);
            resp.put("currentFile", "");
            resp.put("stage", "completed".equals(status) ? "done" : "idle");
            resp.put("message", "");
            if (project.getLastAnalysisId() != null) {
                resp.put("taskId", project.getLastAnalysisId());
            }
        }
        return resp;
    }

    @Transactional
    public Map<String, Object> cancelAnalysis(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("项目不存在"));

        progressTracker.requestCancel(projectId);
        progressTracker.cancel(projectId);
        project.setStatus("cancelled");
        project.setUpdatedAt(Instant.now());
        projectRepository.save(project);

        webSocketHub.send(projectId, "analysis_error", Map.of("error", "分析已取消"));

        return Map.of("message", "已取消分析", "projectId", projectId, "status", "cancelled");
    }

    private void pushProgress(String projectId, AnalysisProgress progress) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("progress", progress.getProgressPercent());
        payload.put("stage", progress.getStage());
        payload.put("message", progress.getMessage());
        payload.put("processedFiles", progress.getProcessedFiles());
        payload.put("totalFiles", progress.getTotalFiles());
        payload.put("currentFile", progress.getCurrentFile());
        webSocketHub.send(projectId, "analysis_progress", payload);
    }

    private void markProjectError(String projectId) {
        transactionTemplate.executeWithoutResult(status ->
                projectRepository.findById(projectId).ifPresent(p -> {
                    p.setStatus("error");
                    p.setUpdatedAt(Instant.now());
                    projectRepository.save(p);
                })
        );
    }

    private Map<String, Object> buildResultPayload(
            String projectId,
            Project project,
            Analysis analysis,
            List<Dimension> dimensions,
            List<Issue> issues
    ) {
        List<Map<String, Object>> dimsOut = dimensions.stream().map(this::dimensionToMap).collect(Collectors.toList());
        List<Map<String, Object>> issuesOut = issues.stream().map(this::issueToMap).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", analysis.getId());
        result.put("projectId", projectId);
        result.put("projectName", project.getName());
        result.put("overallScore", analysis.getOverallScore());
        result.put("grade", analysis.getGrade());
        result.put("dimensions", dimsOut);
        result.put("issues", issuesOut);
        result.put("created_at", analysis.getCreatedAt().toString());
        result.put("completedAt", analysis.getCreatedAt().toString());
        result.put("taskId", analysis.getId());
        return result;
    }

    private Map<String, Object> dimensionToMap(Dimension dim) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", dim.getName());
        map.put("score", dim.getScore());
        map.put("maxScore", dim.getMaxScore());
        map.put("issues", dim.getIssues());
        return map;
    }

    private Map<String, Object> issueToMap(Issue issue) {
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
