package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorHandlingMetric 测试类
 * 模拟不同的异常处理场景，验证分析逻辑与得分计算。
 */
class ErrorHandlingMetricTest extends BasicMetricTest {

    /**
     * 反射调用私有方法 analyzeErrorHandling
     */
    private static ScoreAndIssues invokeAnalyze(CompilationUnit cu) {
        try {
            ErrorHandlingMetric analyzer = new ErrorHandlingMetric();
            var method = ErrorHandlingMetric.class.getDeclaredMethod("analyzeErrorHandling", CompilationUnit.class, String.class);
            method.setAccessible(true);
            return (ScoreAndIssues) method.invoke(analyzer, cu, "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 测试1：捕获异常但未处理（应检测到违规）
     */
    @Test
    void testIgnoredCatchVariable() {
        String code = """
                public class Demo {
                    void risky() {
                        try {
                            int x = 10 / 0;
                        } catch (Exception e) {
                            // 未使用异常变量
                        }
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("score: " + result.getScore());
        result.getIssues().forEach(System.out::println);

        assertNotNull(result);
        assertFalse(result.getIssues().isEmpty(), "应检测到未使用异常变量的违规情况");
        assertTrue(result.getIssues().get(0).getMessage().contains("捕获异常但未处理"));
    }

    /**
     * 测试2：函数声明 throws 异常（应视为合规）
     */
    @Test
    void testThrowsExceptionDeclaration() {
        String code = """
                import java.io.IOException;
                public class FileDemo {
                    void readFile() throws IOException {
                        throw new IOException("Error");
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("score: " + result.getScore());
        result.getIssues().forEach(System.out::println);

        assertNotNull(result);
        assertTrue(result.getScore() >= 0.4, "throws 异常应产生基础得分");
    }

    /**
     * 测试3：无异常相关代码（得分应为 0，issues 为空）
     */
    @Test
    void testNoExceptionCode() {
        String code = """
                public class CleanCode {
                    void hello() {
                        int a = 10 + 20;
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("score: " + result.getScore());
        result.getIssues().forEach(System.out::println);

        assertNotNull(result);
        assertEquals(0.0, result.getScore(), 1e-6);
        assertTrue(result.getIssues().isEmpty());
    }
}
