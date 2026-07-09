package com.bugkiller_cityu.code_diagnostician.service;

import lombok.Data;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单项目分析任务的内存进度快照。
 */
@Data
public class AnalysisProgress {
    private final String projectId;
    private final String analysisId;
    private volatile String status = "analyzing"; // analyzing / completed / error / cancelled
    private volatile int processedFiles;
    private volatile int totalFiles;
    private volatile String currentFile = "";
    private volatile String stage = "preparing";
    private volatile String message = "准备分析";
    private volatile String error;
    private volatile Instant updatedAt = Instant.now();
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    public AnalysisProgress(String projectId, String analysisId) {
        this.projectId = projectId;
        this.analysisId = analysisId;
    }

    public int getProgressPercent() {
        if ("completed".equals(status)) {
            return 100;
        }
        if (totalFiles <= 0) {
            return 0;
        }
        return Math.min(99, (int) Math.round(processedFiles * 100.0 / totalFiles));
    }

    public boolean isCancelRequested() {
        return cancelRequested.get();
    }

    public void requestCancel() {
        cancelRequested.set(true);
    }
}
