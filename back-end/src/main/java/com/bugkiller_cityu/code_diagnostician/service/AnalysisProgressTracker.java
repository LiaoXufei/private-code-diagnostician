package com.bugkiller_cityu.code_diagnostician.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存中维护各项目分析进度，供 status 轮询与 WebSocket 推送共用。
 */
@Component
public class AnalysisProgressTracker {

    private final Map<String, AnalysisProgress> progressByProject = new ConcurrentHashMap<>();

    public AnalysisProgress start(String projectId, String analysisId) {
        AnalysisProgress progress = new AnalysisProgress(projectId, analysisId);
        progressByProject.put(projectId, progress);
        return progress;
    }

    public Optional<AnalysisProgress> get(String projectId) {
        return Optional.ofNullable(progressByProject.get(projectId));
    }

    public boolean isAnalyzing(String projectId) {
        return get(projectId)
                .map(p -> "analyzing".equals(p.getStatus()))
                .orElse(false);
    }

    public void update(String projectId, int processed, int total, String currentFile, String stage, String message) {
        AnalysisProgress progress = progressByProject.get(projectId);
        if (progress == null) {
            return;
        }
        progress.setProcessedFiles(processed);
        progress.setTotalFiles(total);
        progress.setCurrentFile(currentFile != null ? currentFile : "");
        progress.setStage(stage);
        progress.setMessage(message);
        progress.setUpdatedAt(Instant.now());
    }

    public void complete(String projectId) {
        AnalysisProgress progress = progressByProject.get(projectId);
        if (progress == null) {
            return;
        }
        progress.setStatus("completed");
        progress.setProcessedFiles(progress.getTotalFiles());
        progress.setStage("done");
        progress.setMessage("分析完成");
        progress.setUpdatedAt(Instant.now());
    }

    public void fail(String projectId, String error) {
        AnalysisProgress progress = progressByProject.get(projectId);
        if (progress == null) {
            return;
        }
        progress.setStatus("error");
        progress.setError(error);
        progress.setStage("error");
        progress.setMessage(error);
        progress.setUpdatedAt(Instant.now());
    }

    public void cancel(String projectId) {
        AnalysisProgress progress = progressByProject.get(projectId);
        if (progress == null) {
            return;
        }
        progress.requestCancel();
        progress.setStatus("cancelled");
        progress.setStage("cancelled");
        progress.setMessage("分析已取消");
        progress.setUpdatedAt(Instant.now());
    }

    public void requestCancel(String projectId) {
        AnalysisProgress progress = progressByProject.get(projectId);
        if (progress != null) {
            progress.requestCancel();
        }
    }
}
