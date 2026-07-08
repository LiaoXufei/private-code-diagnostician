package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NamingConventionMetricTest extends BasicMetricTest {
    private static ScoreAndIssues invokeAnalyze(CompilationUnit cu) {
        try {
            NamingConventionMetric analyzer = new NamingConventionMetric();
            var method = NamingConventionMetric.class.getDeclaredMethod("analyzeNaming", CompilationUnit.class);
            method.setAccessible(true);
            return (ScoreAndIssues) method.invoke(analyzer, cu);
        } catch (Exception e) {
            throw new RuntimeException("反射调用 analyzeNaming 失败", e);
        }
    }

    @Test
    void testBadNaming() {
        String code = """
                package bad_Pkg;
                
                public class myclass {
                    public void Say_Hello() {}
                    private static final int myConst = 10;
                    private int MyVar = 5;
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("Score: " + result.getScore());
        result.getIssues().forEach(issue -> System.out.println(issue.getMessage()));

        assertNotNull(result);
        assertFalse(result.getIssues().isEmpty(), "应检测出命名问题");
        assertTrue(result.getIssues().stream().anyMatch(s -> s.getMessage().contains("不符合规范")));
    }

    @Test
    void testGoodNaming() {
        String code = """
                package good.pkg;
                
                public class MyClass {
                    public void sayHello() {}
                    private static final int MAX_COUNT = 10;
                    private int value = 5;
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("Score: " + result.getScore());
        System.out.println("Issues: " + result.getIssues());

        assertNotNull(result);
        assertTrue(result.getIssues().isEmpty(), "规范命名不应有问题");
        assertTrue(result.getScore() >= 0);
    }

}