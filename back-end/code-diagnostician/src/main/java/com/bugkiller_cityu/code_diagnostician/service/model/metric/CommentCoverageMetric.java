package com.bugkiller_cityu.code_diagnostician.service.model.metric;

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
            return new MetricResult(0.0, List.of("无法解析AST"), getDescription(), getWeight());
        }

        CompilationUnit cu = astInfo.getCompilationUnit();
        String source = new String(astInfo.getContent());

        int totalLines = countTotalLines(source);
        int commentLines = countCommentLines(cu);

        double commentRatio = (totalLines == 0) ? 0.0 : (double) commentLines / totalLines;
        double score = calculateScore(commentRatio);
        List<String> issues = generateIssues(cu, commentRatio);

        return new MetricResult(score, issues, getDescription(), getWeight());
    }

    /** 统计总行数 */
    private int countTotalLines(String source) {
        return (int) source.lines().count();
    }

    /** 统计注释行数 */
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

    /** 按 Go 版计分机制计算分数 */
    private double calculateScore(double ratio) {
        double baseScore = 0.9;              // 基础分
        double reducePerPercent = 0.05;      // 每1%注释，减0.05分
        double percent = ratio * 100;        // 转换为百分比

        double score = baseScore - (percent * reducePerPercent);
        if (score < 0.0) score = 0.0;
        return score;
    }

    /** 生成报告 */
    private List<String> generateIssues(CompilationUnit cu, double ratio) {
        List<String> issues = new ArrayList<>();

        if (ratio < 0.05) {
            issues.add(String.format("注释覆盖率极低（%.2f%%），可读性较差。", ratio * 100));
        } else if (ratio < 0.10) {
            issues.add(String.format("注释覆盖率偏低（%.2f%%），建议增加必要注释。", ratio * 100));
        } else if (ratio > 0.40) {
            issues.add(String.format("注释覆盖率较高（%.2f%%），可能存在冗余注释。", ratio * 100));
        }

        // 检查 public 类与方法的文档注释
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                if (Modifier.isPublic(node.getModifiers()) && node.getJavadoc() == null) {
                    issues.add("公共类缺少文档注释：" + node.getName().getIdentifier());
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                if (Modifier.isPublic(node.getModifiers()) && node.getJavadoc() == null) {
                    issues.add("公共方法缺少文档注释：" + node.getName().getIdentifier());
                }
                return super.visit(node);
            }
        });

        return issues;
    }
}

