package com.bugkiller_cityu.code_diagnostician.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 独立 Bean，确保 @Async 经 Spring 代理生效（避免同类自调用失效）。
 */
@Component
public class AnalysisAsyncLauncher {

    private final AnalysisService analysisService;

    public AnalysisAsyncLauncher(@Lazy AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Async("analysisExecutor")
    public void launch(String projectId, String analysisId) {
        analysisService.executeAnalysis(projectId, analysisId);
    }
}
