package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.entity.Issue;
import com.bugkiller_cityu.code_diagnostician.service.model.common.SeverityEnum;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class CommentCoverageMetric extends BaseMetric implements Metric {

    public CommentCoverageMetric() {
        super("注释覆盖率分析",
                "检测代码的注释覆盖率，评估代码的可读性与自解释性",
                0.15);
    }

    @Override
    public MetricResult analyze(ParseResult parseResult) {
        JavaASTInfo astInfo = JavaASTExtractor.extractJavaAST(parseResult);
        if (astInfo == null || astInfo.getCompilationUnit() == null) {
            return new MetricResult(0.0, new ArrayList<>(), this.getDescription(), this.getWeight());
        }
        CompilationUnit cu = astInfo.getCompilationUnit();
        String filePath = (String) cu.getProperty("filePath");
        if (astInfo == null || astInfo.getCompilationUnit() == null) {
            // 创建无法解析AST的Issue
            Issue issue = createBaseIssue(
                    filePath,
                    null,
                    SeverityEnum.HIGH.getMessage(),
                    "无法解析AST",
                    "请检查代码语法是否正确"
            );
            return new MetricResult(0.0, List.of(issue), getDescription(), getWeight());
        }

        String source = new String(astInfo.getContent());

        ScoreAndIssues scoreAndIssues = analyzeCommentCoverage(cu, source, filePath);

        return new MetricResult(
                scoreAndIssues.getScore(),
                scoreAndIssues.getIssues(),
                getDescription(),
                getWeight()
        );
    }

    private ScoreAndIssues analyzeCommentCoverage(CompilationUnit cu, String source, String filePath) {
        List<Issue> issues = new ArrayList<>();

        int totalLines = countTotalLines(source);
        int commentLines = countCommentLines(cu);

        double commentRatio = (totalLines == 0) ? 0.0 : (double) commentLines / totalLines;
        double score = calculateScore(commentRatio);

        // 生成基本注释覆盖率问题
        if (commentRatio < 0.05) {
            Issue issue = createBaseIssue(
                    filePath,
                    null,
                    SeverityEnum.HIGH.getMessage(),
                    String.format("注释覆盖率极低（%.2f%%），可读性较差。", commentRatio * 100),
                    "建议增加关键代码（如复杂逻辑、公共接口）的注释，提高代码可维护性。"
            );
            issues.add(issue);
        } else if (commentRatio < 0.10) {
            Issue issue = createBaseIssue(
                    filePath,
                    null,
                    SeverityEnum.MEDIUM.getMessage(),
                    String.format("注释覆盖率偏低（%.2f%%），建议增加必要注释。", commentRatio * 100),
                    "建议为核心方法、复杂算法和关键业务逻辑添加说明注释。"
            );
            issues.add(issue);
        } else if (commentRatio > 0.40) {
            Issue issue = createBaseIssue(
                    filePath,
                    null,
                    SeverityEnum.LOW.getMessage(),
                    String.format("注释覆盖率较高（%.2f%%），可能存在冗余注释。", commentRatio * 100),
                    "建议精简重复或不必要的注释，保持注释与代码的一致性。"
            );
            issues.add(issue);
        }

        // 检查公共类与方法文档注释
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                if (Modifier.isPublic(node.getModifiers()) && node.getJavadoc() == null) {
                    int lineNumber = cu.getLineNumber(node.getStartPosition());
                    Issue issue = createBaseIssue(
                            filePath,
                            lineNumber,
                            SeverityEnum.MEDIUM.getMessage(),
                            "公共类缺少文档注释：" + node.getName().getIdentifier(),
                            "建议添加Javadoc注释，说明类的功能、设计目的和使用场景。"
                    );
                    issues.add(issue);
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                if (Modifier.isPublic(node.getModifiers()) && node.getJavadoc() == null) {
                    int lineNumber = cu.getLineNumber(node.getStartPosition());
                    Issue issue = createBaseIssue(
                            filePath,
                            lineNumber,
                            SeverityEnum.MEDIUM.getMessage(),
                            "公共方法缺少文档注释：" + node.getName().getIdentifier(),
                            "建议添加Javadoc注释，说明方法功能、参数含义、返回值及异常情况。"
                    );
                    issues.add(issue);
                }
                return super.visit(node);
            }
        });

        return new ScoreAndIssues(score, issues);
    }

    // 工具方法：创建基础Issue对象
    private Issue createBaseIssue(String filePath, Integer line, String severity, String message, String suggestion) {
        Issue issue = new Issue();
        issue.setId(UUID.randomUUID().toString()); // 生成UUID作为ID
        issue.setAnalysisId(null); // 分析ID后续关联时设置
        issue.setType("comment_coverage"); // 问题类型：注释覆盖率
        issue.setSeverity(severity);
        issue.setFile(filePath);
        issue.setLine(line);
        issue.setMessage(message);
        issue.setSuggestion(suggestion);
        return issue;
    }

    private int countTotalLines(String source) {
        return (int) source.lines().count();
    }

    private int countCommentLines(CompilationUnit cu) {
        List<Comment> comments = cu.getCommentList();
        Set<Integer> commentLines = new HashSet<>();
        for (Comment c : comments) {
            int startLine = cu.getLineNumber(c.getStartPosition());
            int endLine = cu.getLineNumber(c.getStartPosition() + c.getLength());
            for (int i = startLine; i <= endLine; i++) {
                commentLines.add(i);
            }
        }
        return commentLines.size();
    }

    private double calculateScore(double ratio) {
        double baseScore = 0.9;
        double reducePerPercent = 0.05;
        double percent = ratio * 100;
        double score = baseScore - (percent * reducePerPercent);
        if (score < 0.0) score = 0.0;
        return score;
    }
}