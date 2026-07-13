package com.diagnostician.service.ai.tools;

import com.diagnostician.entity.Analysis;
import com.diagnostician.entity.Issue;
import com.diagnostician.entity.ProjectFile;
import com.diagnostician.service.ai.AiContextService;
import com.diagnostician.service.ai.util.SeverityUtils;
import com.diagnostician.service.model.analyzer.Analyzer;
import com.diagnostician.service.model.metric.AnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CodeAnalysisTools {

    private final AiContextService contextService;
    private final Analyzer.CodeAnalyzer codeAnalyzer;

    @Tool(description = "列出项目中的所有源码文件")
    public String listProjectFiles(
            @ToolParam(description = "项目ID") String projectId
    ) {
        try {
            List<ProjectFile> files = contextService.listFiles(projectId);
            if (files.isEmpty()) {
                return "项目暂无文件";
            }
            return files.stream()
                    .map(f -> "- " + f.getFilename() + " (" + f.getFileSize() + " bytes)")
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "列出文件失败: " + e.getMessage();
        }
    }

    @Tool(description = "读取项目中指定文件的内容（最多返回前8000字符）")
    public String readProjectFile(
            @ToolParam(description = "项目ID") String projectId,
            @ToolParam(description = "文件路径或文件名") String filePath
    ) {
        try {
            String content = contextService.readFileContent(projectId, filePath);
            return content.length() > 8000 ? content.substring(0, 8000) + "\n...(truncated)" : content;
        } catch (Exception e) {
            return "读取文件失败: " + e.getMessage();
        }
    }

    @Tool(description = "获取项目最新的静态分析结果摘要")
    public String getAnalysisSummary(
            @ToolParam(description = "项目ID") String projectId
    ) {
        try {
            Map<String, Object> snapshot = contextService.buildAnalysisSnapshot(projectId);
            return snapshot.toString();
        } catch (Exception e) {
            return "获取分析摘要失败: " + e.getMessage();
        }
    }

    @Tool(description = "对项目中指定文件运行静态分析并返回评分和问题数")
    public String analyzeProjectFile(
            @ToolParam(description = "项目ID") String projectId,
            @ToolParam(description = "文件路径或文件名") String filePath
    ) {
        try {
            ProjectFile target = contextService.listFiles(projectId).stream()
                    .filter(f -> filePath.equals(f.getFilename()) || filePath.equals(f.getFilePath()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("文件不存在: " + filePath));

            Path diskPath = contextService.resolveDiskPath(projectId, target.getFilename());
            if (diskPath == null) {
                return "分析失败: 无法解析文件磁盘路径: " + filePath;
            }

            AnalysisResult result = codeAnalyzer.analyzeFile(diskPath.toString());
            double score = result.getOverallScore() * 100;
            int issueCount = result.getIssues() != null ? result.getIssues().size() : 0;
            return String.format("文件 %s 分析完成：评分 %.1f，问题数 %d", filePath, score, issueCount);
        } catch (Analyzer.AnalysisException e) {
            return "分析失败: " + e.getMessage();
        } catch (Exception e) {
            return "分析失败: " + e.getMessage();
        }
    }

    @Tool(description = "列出项目中严重程度最高的前10个问题")
    public String listTopIssues(
            @ToolParam(description = "项目ID") String projectId
    ) {
        try {
            Analysis analysis = contextService.requireLatestAnalysis(projectId);
            List<Issue> issues = contextService.getIssuesForAnalysis(analysis.getId());
            return issues.stream()
                    .sorted(Comparator.comparingInt(i -> SeverityUtils.severityRank(i.getSeverity())))
                    .limit(10)
                    .map(i -> String.format("[%s] %s:%s - %s", i.getSeverity(), i.getFile(), i.getLine(), i.getMessage()))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "获取问题清单失败: " + e.getMessage();
        }
    }
}
