package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.service.model.common.LanguageType;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.BaseParseResult;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
//import com.sun.source.tree.CompilationUnitTree;
//import com.sun.source.util.JavacTask;
//import com.sun.source.util.Trees;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

// Java AST提取工具类
public class JavaASTExtractor {

    // 从解析结果中提取Java语言的AST信息
    public static JavaASTInfo extractJavaAST(ParseResult parseResult) {
        // 检查是否为Java语言
        if (parseResult.getLanguage() != LanguageType.JAVA) {
            return null;
        }

        // 获取AST根节点并检查类型
        Object astRoot = parseResult.getASTRoot();
        if (!(astRoot instanceof CompilationUnit)) {
            return null;
        }

        CompilationUnit compilationUnit = (CompilationUnit) astRoot;

        Set<File> fileSet = new HashSet<File>();

        // 获取源代码内容
        byte[] content = new byte[0];
        if (parseResult instanceof BaseParseResult) {
            content = ((BaseParseResult) parseResult).getContent();
        }


        return new JavaASTInfo(compilationUnit, fileSet, content);
    }

}
