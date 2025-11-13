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
            JavaASTInfo astInfo = new JavaASTInfo(cu, java.util.Collections.emptySet(), new byte[0]);
            var method = CyclomaticComplexityMetric.class.getDeclaredMethod("analyzeComplexity", JavaASTInfo.class);
            method.setAccessible(true);
            return (ScoreAndIssues) method.invoke(analyzer, astInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 简单函数（复杂度低，应得分 = 0.5） */
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

        System.out.println("Score (simple): " + result.getScore());
        result.getIssues().forEach(System.out::println);

        assertNotNull(result);
        // 复杂度 = 1 -> score = 0.4 + 0.1*1 = 0.5
        assertEquals(0.5, result.getScore(), 1e-6);
        assertTrue(result.getIssues().isEmpty());
    }

    /** 中等复杂度函数（if/for/while），按当前 scoring 预计 score = 0.9 */
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

        System.out.println("Score (medium): " + result.getScore());
        result.getIssues().forEach(System.out::println);

        assertNotNull(result);
        // 根据当前复杂度计数（if + else-if + for + while => 5），score = 0.4 + 0.1*5 = 0.9
        assertEquals(0.9, result.getScore(), 1e-6);
        // 中等复杂度不会产生 issues（issues 仅在复杂度 > 10/15 触发）
        assertTrue(result.getIssues().isEmpty());
    }

    /** 高复杂度函数（只包含一个极复杂函数，确保 avgComplexity 很高 -> score = 1.0） */
    @Test
    void testHighComplexityFunction() {
        String code = """
            public class ComplexDemo {
                void compute(int a, int b, int c, int d) {
                    // 多层循环 + 多重条件 + 多个 switch 和嵌套 if 来显著增加复杂度
                    for (int i = 0; i < 5; i++) {
                        if ((a > b && b > c) || (i % 2 == 0 && (c < d || d % 3 == 0))) {
                            a += b;
                            if (a > 100) {
                                switch (d % 5) {
                                    case 0 -> a++;
                                    case 1 -> b--;
                                    case 2 -> c += a;
                                    case 3 -> d -= b;
                                    default -> {
                                        if (a % 2 == 0) {
                                            a /= 2;
                                        } else {
                                            b *= 2;
                                        }
                                    }
                                }
                            } else {
                                for (int j = 0; j < 3; j++) {
                                    if (j == 0) {
                                        a++;
                                    } else if (j == 1) {
                                        b++;
                                    } else {
                                        c++;
                                    }
                                    if (a + b > c && c - d < a) {
                                        a += c;
                                    } else if (a - d > b || (c > 0 && d < 0)) {
                                        b -= d;
                                    } else {
                                        d++;
                                    }
                                }
                            }
                        } else if (a < 0 && b != 0) {
                            while (a < 50) {
                                a += 2;
                                if (a % 7 == 0) {
                                    if (b % 2 == 0) {
                                        b /= 2;
                                    } else {
                                        b = b + a;
                                    }
                                } else if (c > 10 && d < 100) {
                                    c -= d;
                                } else {
                                    a++;
                                }
                            }
                        } else {
                            try {
                                for (int x = 0; x < 4; x++) {
                                    if (x == 0) {
                                        if ((a & b) == 0) a++;
                                        else if ((b | c) > 0) b++;
                                        else c++;
                                    } else if (x == 1) {
                                        if (a > b && b > c && c > d) {
                                            a += x;
                                        } else if ((a + b) % 3 == 0) {
                                            b -= x;
                                        } else {
                                            d += x;
                                        }
                                    } else {
                                        switch (x) {
                                            case 2 -> a += 2;
                                            case 3 -> b -= 3;
                                            default -> c += 1;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // just to keep structure; not reducing complexity count
                                System.out.println(e);
                            }
                        }
                    }
                    
                    // 额外大量的分支堆叠，进一步提高路径数
                    if (a > 0) {
                        if (b > 0) {
                            if (c > 0) {
                                if (d > 0) {
                                    a += 1;
                                } else {
                                    b += 1;
                                }
                            } else {
                                c += 1;
                            }
                        } else if (a % 2 == 0) {
                            a /= 2;
                        } else {
                            b /= 2;
                        }
                    } else if (a == 0) {
                        c = d + a;
                    } else {
                        for (int z = 0; z < 5; z++) {
                            if (z % 2 == 0) {
                                a += z;
                            } else if (z % 3 == 0) {
                                b += z;
                            } else if (z % 5 == 0) {
                                c += z;
                            } else {
                                d += z;
                            }
                        }
                    }
                }
            }
        """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        System.out.println("High Complexity Function Score: " + result.getScore());
        result.getIssues().forEach(System.out::println);

        assertNotNull(result);
        // 由于这是唯一的函数，avgComplexity == compute 的复杂度，应落到最高分段 -> 1.0
        assertEquals(1.0, result.getScore(), 1e-6);
        assertTrue(result.getIssues().stream().anyMatch(msg -> msg.contains("圈复杂度过高") || msg.contains("复杂度过高")));
    }

    /** 无函数（空类） */
    @Test
    void testNoFunction() {
        String code = """
            public class EmptyClass {
                int x = 10;
            }
        """;
        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);
        assertNotNull(result);
        assertEquals(0.0, result.getScore(), 1e-6);
        assertTrue(result.getIssues().isEmpty());
    }
}



