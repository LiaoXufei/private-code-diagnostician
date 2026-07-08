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
        assertEquals(1.0, result.getScore(), 1e-6);
        assertTrue(result.getIssues().isEmpty());
    }

    /**
     * 测试 10：所有方法都抛出异常（完美情况）
     */
    @Test
    void testAllMethodsThrowExceptions() {
        String code = """
                import java.io.IOException;
                public class AllThrowing {
                    void method1() throws IOException {
                        throw new IOException("Error1");
                    }
                    
                    void method2() throws IOException {
                        throw new IOException("Error2");
                    }
                    
                    void method3() throws IOException {
                        throw new IOException("Error3");
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertEquals(1.0, result.getScore(), 1e-6, "所有方法都声明异常应得满分");
        assertTrue(result.getIssues().isEmpty());
    }

    /**
     * 测试 11：verify isRiskyCall 方法识别各种风险方法名
     */
    @Test
    void testVariousRiskMethodNames() {
        String code = """
                public class RiskMethods {
                    void test() {
                        openConnection();
                        readFile();
                        writeData();
                        deleteFile();
                        connectToServer();
                        executeQuery();
                        loadConfig();
                        saveData();
                        renameFile();
                        closeStream();
                        sendRequest();
                        receiveResponse();
                        parseXML();
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertFalse(result.getIssues().isEmpty(), "应检测到多个风险方法调用");
        assertTrue(result.getIssues().size() >= 10, "大部分风险方法都应被检测到");
    }

    /**
     * 测试 12：部分风险方法被 try-catch 包裹
     */
    @Test
    void testPartialRiskMethodsInTryCatch() {
        String code = """
                import java.io.*;
                public class PartialSafe {
                    void process() {
                        try {
                            FileReader fr = new FileReader("in.txt");
                            fr.read();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        
                        FileWriter fw = new FileWriter("out.txt");
                        fw.write("data");
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertEquals(1, result.getIssues().size(), "应检测到 1 个未处理的风险方法");
        assertTrue(result.getIssues().get(0).getMessage().contains("write"));
    }

    /**
     * 测试 13：嵌套 try-catch 块
     */
    @Test
    void testNestedTryCatch() {
        String code = """
                import java.io.*;
                public class NestedTry {
                    void complex() {
                        try {
                            FileReader fr = new FileReader("test.txt");
                            try {
                                int data = fr.read();
                                if (data > 0) {
                                    process(data);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (FileNotFoundException e) {
                            System.out.println("File not found");
                        }
                    }
                    
                    void process(int data) {}
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertTrue(result.getScore() > 0.5, "嵌套 try-catch 正确处理应得高分");
        assertTrue(result.getIssues().isEmpty(), "嵌套 try-catch 不应产生问题");
    }

    /**
     * 测试 14：try-with-resources 语句
     */
    @Test
    void testTryWithResources() {
        String code = """
                import java.io.*;
                public class TryWithResources {
                    void readFile() {
                        try (FileReader fr = new FileReader("test.txt")) {
                            int data = fr.read();
                            System.out.println(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertTrue(result.getScore() > 0.5, "try-with-resources 应得高分");
        assertTrue(result.getIssues().isEmpty(), "try-with-resources 不应产生问题");
    }

    /**
     * 测试 15：catch 块中重新抛出异常
     */
    @Test
    void testRethrowException() {
        String code = """
                import java.io.*;
                public class Rethrow {
                    void process() throws IOException {
                        try {
                            FileReader fr = new FileReader("test.txt");
                            fr.read();
                        } catch (IOException e) {
                            throw new IOException("Wrapped error", e);
                        }
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertTrue(result.getScore() > 0.5, "重新抛出异常应视为已处理");
        assertTrue(result.getIssues().isEmpty(), "重新抛出不应产生问题");
    }

    /**
     * 测试 16：verify 文件路径是否正确设置到 Issue 中
     */
    @Test
    void testFilePathInIssue() {
        String code = """
                public class FilePathTest {
                    void test() {
                        try {
                            int x = 10 / 0;
                        } catch (Exception e) {
                            // 未处理
                        }
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertFalse(result.getIssues().isEmpty());
        assertEquals("TestFile.java", result.getIssues().get(0).getFile());
        assertNotNull(result.getIssues().get(0).getLine());
        assertTrue(result.getIssues().get(0).getLine() > 0);
    }

    /**
     * 测试 17：calculateScore 边界情况 - 除零保护
     */
    @Test
    void testCalculateScoreEdgeCases() {
        // 当 totalErrorRelatedMethods 为 0 时，应返回 1.0
        String code = """
                public class NoErrorMethods {
                    void simple() {
                        int x = 10;
                        int y = 20;
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertEquals(1.0, result.getScore(), 1e-6, "无错误处理方法时应返回满分");
    }

    /**
     * 测试 18：多种异常类型同时捕获
     */
    @Test
    void testMultiCatchBlock() {
        String code = """
                import java.io.*;
                public class MultiCatch {
                    void process() {
                        try {
                            FileReader fr = new FileReader("test.txt");
                            fr.read();
                        } catch (IOException | IllegalArgumentException e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertTrue(result.getScore() > 0.5, "多异常捕获且已处理应得高分");
        assertTrue(result.getIssues().isEmpty(), "已处理的多异常不应产生问题");
    }

    /**
     * 测试 19：catch 块中使用异常对象的方法
     */
    @Test
    void testCatchBlockUsesExceptionMethod() {
        String code = """
                public class UseExceptionMethod {
                    void test() {
                        try {
                            int x = 10 / 0;
                        } catch (ArithmeticException e) {
                            String msg = e.getMessage();
                            System.out.println(msg);
                        }
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertTrue(result.getScore() > 0.5, "使用异常对象的方法应视为已处理");
        assertTrue(result.getIssues().isEmpty(), "使用异常对象不应产生问题");
    }

    /**
     * 测试 20：极端情况 - 所有方法都有问题
     */
    @Test
    void testWorstCaseScenario() {
        String code = """
                import java.io.*;
                public class WorstCase {
                    void method1() {
                        try {
                            int x = 10 / 0;
                        } catch (Exception e) {
                            // 未处理
                        }
                    }
                    
                    void method2() {
                        FileReader fr = new FileReader("test.txt");
                        fr.read();
                    }
                    
                    void method3() {
                        try {
                            String s = null;
                            s.length();
                        } catch (NullPointerException e) {
                            // 未处理
                        }
                    }
                }
                """;

        CompilationUnit cu = parse(code);
        ScoreAndIssues result = invokeAnalyze(cu);

        assertNotNull(result);
        assertFalse(result.getIssues().isEmpty(), "应检测到多个问题");
        assertTrue(result.getScore() < 0.3, "极端糟糕情况应得低分");
    }
}
