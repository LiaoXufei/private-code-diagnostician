package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructureAnalysisMetricTest extends BasicMetricTest{

    @Test
    void analyzeNestingDepth() {
        String source = """
            public class Example {
                void foo() {
                    if (true) {
                        for (int i = 0; i < 10; i++) {
                            while (i < 5) {
                                if (i > 2) {
                                    System.out.println(i);
                                }
                            }
                        }
                    }
                }
            }
        """;
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        StructureAnalysisMetric metric = new StructureAnalysisMetric();
        int[] maxDepthRef = {0};
        List<String> issues = metric.analyzeNestingDepth(cu, maxDepthRef);

        System.out.println("Max Depth: " + maxDepthRef[0]);
        issues.forEach(System.out::println);
    }

    @Test
    void analyzeCircularDependenciesTest(){
        String source = """
            package com.example.util;
    
            import com.example.util.helper;
            import com.other.project.*;
    
            public class Demo {}
        """;

        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        StructureAnalysisMetric metric = new StructureAnalysisMetric();
        List<String> issues = metric.analyzeCircularDependencies(cu);

        issues.forEach(System.out::println);
    }

    @Test
    void analyzeImportComplexityTest(){
        String source = """
            package com.example;
    
            import java.util.*;
            import java.io.*;
            import java.net.*;
            import java.math.*;
            import java.text.*;
            import java.nio.*;
            import java.nio.file.*;
            import java.time.*;
            import java.sql.*;
            import java.lang.reflect.*;
            import java.security.*;
            import java.util.concurrent.*;
            import java.util.function.*;
            import java.util.stream.*;
            import java.awt.*;
            import javax.swing.*;
            import javax.xml.parsers.*;
            import javax.annotation.*;
            import org.w3c.dom.*;
            import org.xml.sax.*;
            import com.example.utils.*;
            import com.example.common.*;
    
            public class Demo {}
        """;

        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        StructureAnalysisMetric metric = new StructureAnalysisMetric();
        List<String> issues = metric.analyzeImportComplexity(cu);

        issues.forEach(System.out::println);
    }
}