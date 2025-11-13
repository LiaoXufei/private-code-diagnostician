package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class BasicMetricTest {
    protected CompilationUnit parse(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null);
    }
}
