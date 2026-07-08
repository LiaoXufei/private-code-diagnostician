package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import org.eclipse.jdt.core.dom.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeDuplicationMetricTest extends BasicMetricTest{
    private MethodDeclaration parseMethod(String code) {
        CompilationUnit cu = parse(code);
        TypeDeclaration typeDecl = (TypeDeclaration) cu.types().get(0);
        return typeDecl.getMethods()[0];
    }
    @Test
    void testExtractSimpleMethod() {
        String code = """
            public class TestClass {
                public void foo() {
                    int a = 0;
                    if (a < 5) {
                        a++;
                    }
                    for (int i = 0; i < 10; i++) {
                        a += i;
                    }
                    return;
                }
            }
            """;

        MethodDeclaration method = parseMethod(code);
        CodeDuplicationMetric extractor = new CodeDuplicationMetric();
        String result = extractor.extractFunctionSignature(method);

        // 验证提取结果包含关键结构
        assertTrue(result.contains("="));
        assertTrue(result.contains("if"));
        assertTrue(result.contains("for"));
        assertTrue(result.contains("return"));
    }
    @Test
    public void testSwitchMethod() {
        String code = """
            public class TestClass {
                public void bar(int x) {
                    switch (x) {
                        case 1: x++; break;
                        case 2: x += 2; break;
                        default: return;
                    }
                }
            }
            """;

        MethodDeclaration method = parseMethod(code);
        CodeDuplicationMetric extractor = new CodeDuplicationMetric();
        String result = extractor.extractFunctionSignature(method);

        assertTrue(result.contains("switch"));
        assertTrue(result.contains("case"));
        assertTrue(result.contains("="));
        assertTrue(result.contains("return"));
    }
}