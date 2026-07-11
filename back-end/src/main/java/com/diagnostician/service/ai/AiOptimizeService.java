package com.diagnostician.service.ai;

import com.diagnostician.config.AiProperties;
import com.diagnostician.entity.Analysis;
import com.diagnostician.entity.Issue;
import com.diagnostician.service.ai.util.SeverityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiOptimizeService {

    private final ChatClient chatClient;
    private final AiContextService contextService;
    private final AiProperties aiProperties;

    public Flux<String> streamSummary(String projectId) {
        Map<String, Object> snapshot = contextService.buildAnalysisSnapshot(projectId);
        String prompt = """
                你是资深代码审查专家。请根据以下静态分析结果，用中文生成一份简洁的质量诊断摘要（200-400字）：
                - 总体评分与等级
                - 主要风险维度（按优先级）
                - 3-5 条可执行的修复建议

                分析数据：
                %s
                """.formatted(snapshot);
        return chatClient.prompt().user(prompt).stream().content();
    }

    public Flux<String> streamOptimize(String projectId, String issueId) {
        Analysis analysis = contextService.requireLatestAnalysis(projectId);
        List<Issue> issues = contextService.getIssuesForAnalysis(analysis.getId());
        Issue target = issues.stream()
                .filter(i -> issueId == null || issueId.isBlank() || issueId.equals(i.getId()))
                .findFirst()
                .orElseThrow(() -> new com.diagnostician.exception.ResourceNotFoundException("未找到对应问题"));

        String codeContext = "";
        try {
            String content = contextService.readFileContent(projectId, target.getFile());
            codeContext = contextService.extractCodeContext(content, target.getLine() != null ? target.getLine() : 1, 12);
        } catch (Exception ignored) {
            codeContext = "（无法读取源码上下文）";
        }

        String prompt = """
                你是代码质量优化专家。请针对以下 Issue 给出详细优化方案（中文）：
                1. 问题成因分析
                2. 具体重构步骤
                3. 优化后的代码示例（使用 Markdown 代码块）
                4. 预期收益

                Issue:
                - 类型: %s
                - 严重度: %s
                - 文件: %s
                - 行号: %s
                - 描述: %s
                - 规则建议: %s

                相关代码上下文:
                ```
                %s
                ```
                """.formatted(
                target.getType(),
                target.getSeverity(),
                target.getFile(),
                target.getLine(),
                target.getMessage(),
                target.getSuggestion(),
                codeContext
        );
        return chatClient.prompt().user(prompt).stream().content();
    }

    public Flux<String> streamBatchOptimize(String projectId) {
        Analysis analysis = contextService.requireLatestAnalysis(projectId);
        List<Issue> issues = contextService.getIssuesForAnalysis(analysis.getId()).stream()
                .sorted(Comparator.comparingInt(i -> SeverityUtils.severityRank(i.getSeverity())))
                .limit(aiProperties.getMaxContextIssues())
                .collect(Collectors.toList());

        String issueList = issues.stream()
                .map(i -> String.format("- [%s] %s:%s %s", i.getSeverity(), i.getFile(), i.getLine(), i.getMessage()))
                .collect(Collectors.joining("\n"));

        String prompt = """
                你是代码质量顾问。请基于以下静态分析问题列表，输出结构化优化报告（中文 Markdown）：
                ## 优先级修复清单（P0/P1/P2）
                ## 分维度改进建议
                ## 快速收益项（可立即修复）

                问题列表：
                %s
                """.formatted(issueList);
        return chatClient.prompt().user(prompt).stream().content();
    }

    public Flux<String> streamFixCode(String projectId, String issueId) {
        Analysis analysis = contextService.requireLatestAnalysis(projectId);
        Issue target = contextService.getIssuesForAnalysis(analysis.getId()).stream()
                .filter(i -> issueId != null && issueId.equals(i.getId()))
                .findFirst()
                .orElseThrow(() -> new com.diagnostician.exception.ResourceNotFoundException("未找到对应问题"));

        String codeContext;
        try {
            String content = contextService.readFileContent(projectId, target.getFile());
            codeContext = contextService.extractCodeContext(content, target.getLine() != null ? target.getLine() : 1, 20);
        } catch (Exception e) {
            codeContext = "（无法读取源码，文件: " + target.getFile() + "，错误: " + e.getMessage() + "）\n" +
                    "请基于问题描述和规则建议给出修复方案。";
        }

        String prompt = """
                你是代码修复助手。请只输出修复后的代码片段（Markdown 代码块），并附一行修改说明。
                要求：保持原有功能，修复以下问题。

                问题: %s
                文件: %s
                行号: %s

                原始代码:
                ```
                %s
                ```
                """.formatted(target.getMessage(), target.getFile(), target.getLine(), codeContext);
        return chatClient.prompt().user(prompt).stream().content();
    }
}
