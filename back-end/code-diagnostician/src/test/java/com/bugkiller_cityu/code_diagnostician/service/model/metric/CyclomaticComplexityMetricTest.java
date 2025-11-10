package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import org.eclipse.jdt.core.dom.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CyclomaticComplexityMetricTest {

    // 工具：将字符串解析为 CompilationUnit
    private static CompilationUnit parse(String code) {
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(code.toCharArray());
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null);
    }

    // 调用私有 analyzeComplexity 方法
    private static ScoreAndIssues invokeAnalyze(CompilationUnit cu) {
        try {
            CyclomaticComplexityMetric analyzer = new CyclomaticComplexityMetric();
            var field = JavaASTInfo.class.getDeclaredConstructor(CompilationUnit.class, java.util.Set.class, byte[].class);
            JavaASTInfo astInfo = new JavaASTInfo(cu, java.util.Collections.emptySet(), new byte[0]);

            var method = CyclomaticComplexityMetric.class.getDeclaredMethod("analyzeComplexity", JavaASTInfo.class);
            method.setAccessible(true);
            return (ScoreAndIssues) method.invoke(analyzer, astInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 测试简单函数（复杂度低，应得分较低）
     */
    @Test
    void testSimpleFunction() {
        String code = """
            public class Demo {
                void hello() {
                    System.out.println("Hi");
                }
            }
        """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("Score: " + result.getScore());
        result.getIssues().forEach(System.out::println);

        assertNotNull(result);
        assertTrue(result.getScore() < 0.5);
        assertTrue(result.getIssues().isEmpty());
    }

    /**
     * 测试中等复杂度函数（if/for/while）
     */
    @Test
    void testMediumComplexityFunction() {
        String code = """
            public class Demo {
                void process(int x) {
                    if (x > 0) {
                        for (int i = 0; i < x; i++) {
                            System.out.println(i);
                        }
                    } else if (x < 0) {
                        while (x < 0) {
                            x++;
                        }
                    }
                }
            }
        """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("Score: " + result.getScore());
        result.getIssues().forEach(System.out::println);

        assertNotNull(result);
        assertTrue(result.getScore() >= 0.4 && result.getScore() <= 0.8);
        assertTrue(result.getIssues().size() >= 1);
    }

    /**
     * 测试极高复杂度函数（多分支 + 循环）
     */
    @Test
    void testHighComplexityFunction() {
        String code = """
            public class ComplexDemo {
                void compute(int x, int y) {
                    for (int i = 0; i < 5; i++) {
                        if (x > y && y > 0 || i < 3) {
                            x += y;
                        } else if (x < 0) {
                            switch (y) {
                                case 1 -> x++;
                                case 2 -> y--;
                                case 3 -> x *= y;
                                default -> x -= y;
                            }
                        } else {
                            while (x < 100) {
                                x += 2;
                                if (x % 10 == 0) break;
                            }
                        }
                    }
                    try {
                        if (x == 10) throw new Exception("err");
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("Score: " + result.getScore());
        result.getIssues().forEach(System.out::println);

        assertNotNull(result);
        assertTrue(result.getScore() >= 0.8);
        assertTrue(result.getIssues().stream().anyMatch(msg -> msg.contains("复杂度过高")));
    }

    /**
     * 测试无函数的情况（应返回空结果）
     */
    @Test
    void testNoFunction() {
        String code = """
            public class EmptyClass {
                int x = 10;
            }
        """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("Score: " + result.getScore());
        result.getIssues().forEach(System.out::println);

        assertNotNull(result);
        assertEquals(0.0, result.getScore(), 1e-6);
        assertTrue(result.getIssues().isEmpty());
    }
}

