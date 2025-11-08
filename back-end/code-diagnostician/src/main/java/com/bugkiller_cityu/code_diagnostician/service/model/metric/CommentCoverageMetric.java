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

        ScoreAndIssues scoreAndIssues = analyzeCommentCoverage(cu, source);

        return new MetricResult(
                scoreAndIssues.getScore(),
                scoreAndIssues.getIssues(),
                getDescription(),
                getWeight()
        );
    }

    private ScoreAndIssues analyzeCommentCoverage(CompilationUnit cu, String source) {
        List<String> issues = new ArrayList<>();

        int totalLines = countTotalLines(source);
        int commentLines = countCommentLines(cu);

        double commentRatio = (totalLines == 0) ? 0.0 : (double) commentLines / totalLines;
        double score = calculateScore(commentRatio);

        // 生成基本注释覆盖率问题
        if (commentRatio < 0.05) {
            issues.add(String.format("注释覆盖率极低（%.2f%%），可读性较差。", commentRatio * 100));
        } else if (commentRatio < 0.10) {
            issues.add(String.format("注释覆盖率偏低（%.2f%%），建议增加必要注释。", commentRatio * 100));
        } else if (commentRatio > 0.40) {
            issues.add(String.format("注释覆盖率较高（%.2f%%），可能存在冗余注释。", commentRatio * 100));
        }

        // 检查公共类与方法文档注释
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

        return new ScoreAndIssues(score, issues);
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
        //  Go 版计分机制
        double baseScore = 0.9;
        double reducePerPercent = 0.05;
        double percent = ratio * 100;
        double score = baseScore - (percent * reducePerPercent);
        if (score < 0.0) score = 0.0;
        return score;
    }
}


