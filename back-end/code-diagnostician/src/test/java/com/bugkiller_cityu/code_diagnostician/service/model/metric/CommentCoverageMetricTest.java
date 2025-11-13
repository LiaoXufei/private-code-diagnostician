package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommentCoverageMetricTest {
    private CompilationUnit parse(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        return (CompilationUnit) parser.createAST(null);
    }

    private static ScoreAndIssues invokeAnalyze(CompilationUnit cu, String code) {
        try {
            CommentCoverageMetric analyzer = new CommentCoverageMetric();
            var method = CommentCoverageMetric.class.getDeclaredMethod("analyzeCommentCoverage", CompilationUnit.class, String.class);
            method.setAccessible(true);
            return (ScoreAndIssues) method.invoke(analyzer, cu, code);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    void testLowCommentCoverage() {
        String code = """
                public class Demo {
                    public void sayHello() {
                        System.out.println("Hello");
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu, code);

        System.out.println("Score: " + result.getScore());
        System.out.println("Issues: " + result.getIssues());

        assertNotNull(result);
        assertTrue(result.getIssues().stream().anyMatch(s -> s.contains("缺少文档注释")));
    }
    @Test
    void testHighCommentCoverage() {
        String code = """
                /**
                 * 测试类
                 */
                public class Demo {
                    /**
                     * 打印问候
                     */
                    public void sayHello() {
                        // 打印
                        System.out.println("Hello");
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu, code);

        System.out.println("Score: " + result.getScore());
        System.out.println("Issues: " + result.getIssues());

        assertNotNull(result);
        // 高注释覆盖率不应报告“缺少文档注释”
        assertTrue(result.getIssues().stream().noneMatch(s -> s.contains("缺少文档注释")));
    }
}