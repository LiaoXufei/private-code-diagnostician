package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FunctionLengthMetricTest extends BasicMetricTest{

    private static ScoreAndIssues invokeAnalyze(CompilationUnit cu) {
        try {
            FunctionLengthMetric analyzer = new FunctionLengthMetric();
            var method = FunctionLengthMetric.class.getDeclaredMethod("analyzeStateManagement", CompilationUnit.class);
            method.setAccessible(true);
            return (ScoreAndIssues) method.invoke(analyzer, cu);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * 测试检测全局变量的情况
     */
    @Test
    void testGlobalVariableDetected(){
        String code = """
            public class Demo {
                int globalCount = 0;
               
                void increase() {
                    globalCount++;
                }
            }
           """;

        // 构建 AST
        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("score: " + result.getScore());
        result.getIssues().forEach(System.out::println);
        // 断言
//        assertNotNull(result);
//        assertFalse(result.getIssues().isEmpty());
//        assertTrue(result.getIssues().get(0).contains("全局变量"));
//        assertTrue(result.getScore() > 0.0);
    }

    /**
     * 测试没有变量的情况（应返回空问题和默认得分）
     */
    @Test
    void testNoVariables() {
        String code = """
            public class EmptyClass {
                void doNothing() {}
            }
            """;
        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertEquals(1.0, result.getScore(), 1e-6);
        assertTrue(result.getIssues().isEmpty());
    }
}