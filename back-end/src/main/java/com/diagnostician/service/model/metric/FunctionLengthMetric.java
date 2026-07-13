package com.diagnostician.service.model.metric;

import com.diagnostician.entity.Issue;
import com.diagnostician.service.model.common.SeverityEnum;
import com.diagnostician.service.model.parser.Function;
import com.diagnostician.service.model.parser.ParseResult;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.jdt.core.dom.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class FunctionLengthMetric extends BaseMetric implements Metric{
    public FunctionLengthMetric() {
        super(
                "状态管理",
                "检测函数长度及状态变量管理，合理的函数长度和状态管理能提高代码可维护性",
                0.2
        );
    }

    public MetricResult analyze(ParseResult parseResult) {
        JavaASTInfo astInfo = JavaASTExtractor.extractJavaAST(parseResult);

        if (astInfo == null) {
            ScoreAndIssues scoreAndIssues = analyzeFunctions(null, parseResult);
            return new MetricResult(
                    scoreAndIssues.getScore(),
                    scoreAndIssues.getIssues(),
                    this.getDescription(),
                    this.getWeight()
            );
        }
        if (astInfo.getContent().length == 0) {
            astInfo.setContent("\n".repeat(parseResult.getTotalLines()).getBytes(StandardCharsets.UTF_8));
        }

        ScoreAndIssues scoreAndIssues = analyzeFunctions(astInfo, parseResult);

        return new MetricResult(
                scoreAndIssues.getScore(),
                scoreAndIssues.getIssues(),
                this.getDescription(),
                this.getWeight()
        );
    }

    private ScoreAndIssues analyzeFunctions(JavaASTInfo astInfo, ParseResult parseResult) {
        List<Issue> issues = new ArrayList<>();
        List<Function> functions = parseResult.getFunctions();
        if (functions == null || functions.isEmpty()) {
            return new ScoreAndIssues(1.0, issues);
        }

        int totalComplexity = 0;
        int longFunctions = 0;
        int veryLongFunctions = 0;
        int extremeLongFunctions = 0;
        int totalFunctions = functions.size();
        CompilationUnit cu = (CompilationUnit) parseResult.getASTRoot();
        String filePath = cu != null ? (String) cu.getProperty("filePath") : null;

        for (Function fn : functions) {
            int lineCount = fn.getEndLine() - fn.getStartLine() + 1;
            int startLine = fn.getStartLine();

            // 函数长度检查
            if (lineCount > 120) {
                Issue issue = createIssue(
                        "function_length",
                        SeverityEnum.HIGH.getMessage(),
                        filePath,
                        startLine,
                        String.format("函数 '%s' 极度过长 (%d 行)", fn.getName(), lineCount),
                        "必须拆分该函数，将其分解为多个单一职责的小函数"
                );
                issues.add(issue);
                extremeLongFunctions++;
            } else if (lineCount > 70) {
                Issue issue = createIssue(
                        "function_length",
                        SeverityEnum.MEDIUM.getMessage(),
                        filePath,
                        startLine,
                        String.format("函数 '%s' 过长 (%d 行)", fn.getName(), lineCount),
                        "建议拆分该函数，提高代码可维护性"
                );
                issues.add(issue);
                veryLongFunctions++;
            } else if (lineCount > 40) {
                Issue issue = createIssue(
                        "function_length",
                        SeverityEnum.LOW.getMessage(),
                        filePath,
                        startLine,
                        String.format("函数 '%s' 较长 (%d 行)", fn.getName(), lineCount),
                        "可考虑重构，简化函数逻辑"
                );
                issues.add(issue);
                longFunctions++;
            }

            // 函数复杂度检查
            int complexity = fn.getComplexity();
            totalComplexity += complexity;
            if (complexity > 18) {
                Issue issue = createIssue(
                        "complexity",
                        SeverityEnum.HIGH.getMessage(),
                        filePath,
                        startLine,
                        String.format("函数 '%s' 复杂度严重过高 (%d)", fn.getName(), complexity),
                        "必须简化函数逻辑，减少条件判断和循环嵌套"
                );
                issues.add(issue);
            } else if (complexity > 12) {
                Issue issue = createIssue(
                        "complexity",
                        SeverityEnum.MEDIUM.getMessage(),
                        filePath,
                        startLine,
                        String.format("函数 '%s' 复杂度过高 (%d)", fn.getName(), complexity),
                        "建议简化函数，降低条件复杂度（如拆分条件逻辑）"
                );
                issues.add(issue);
            }

            // 函数参数数量检查
            int paramCount = fn.getParameters();
            if (paramCount > 8) {
                Issue issue = createIssue(
                        "parameter_count",
                        SeverityEnum.MEDIUM.getMessage(),
                        filePath,
                        startLine,
                        String.format("函数 '%s' 参数极多 (%d 个)", fn.getName(), paramCount),
                        "必须使用结构体或对象封装参数，减少参数数量"
                );
                issues.add(issue);
            } else if (paramCount > 6) {
                Issue issue = createIssue(
                        "parameter_count",
                        SeverityEnum.LOW.getMessage(),
                        filePath,
                        startLine,
                        String.format("函数 '%s' 参数过多 (%d 个)", fn.getName(), paramCount),
                        "建议使用结构体或对象封装参数，提高代码可读性"
                );
                issues.add(issue);
            }
        }

        // 计算长度相关评分
        double longRatio = (double) longFunctions / totalFunctions;
        double veryLongRatio = (double) veryLongFunctions / totalFunctions;
        double extremeLongRatio = (double) extremeLongFunctions / totalFunctions;
        double lengthScore = longRatio * 0.3 + veryLongRatio * 0.5 + extremeLongRatio * 0.8;
        lengthScore = 1.0 - Math.min(lengthScore, 1.0);

        // 计算复杂度评分
        double avgComplexity = (double) totalComplexity / totalFunctions;
        double complexityScore = calculateComplexityScore(avgComplexity);

        // 状态管理检查
        if (cu != null) {
            ScoreAndIssues stateScoreAndIssues = analyzeStateManagement(cu);
            issues.addAll(stateScoreAndIssues.getIssues());

            // 综合得分计算
            return new ScoreAndIssues(
                    lengthScore * 0.5 + complexityScore * 0.2 + stateScoreAndIssues.getScore() * 0.3,
                    issues
            );
        }

        return new ScoreAndIssues(lengthScore * 0.7 + complexityScore * 0.3, issues);
    }

    private double calculateComplexityScore(double avgComplexity) {
        double baseScore = 0.1;
        double increasePerLevel = 0.1;
        double score = baseScore + (avgComplexity * increasePerLevel);
        return 1.0 - Math.min(score, 1.0);
    }

    private ScoreAndIssues analyzeStateManagement(CompilationUnit cu) {
        return new ScoreAndIssues(1.0, new ArrayList<>());
    }

    private Issue createIssue(String type, String severity, String file, Integer line, String message, String suggestion) {
        Issue issue = new Issue();
        issue.setType(type);
        issue.setSeverity(severity);
        issue.setFile(file);
        issue.setLine(line);
        issue.setMessage(message);
        issue.setSuggestion(suggestion);
        // analysisId暂不设置（后续关联分析结果时补充）
        return issue;
    }
}
