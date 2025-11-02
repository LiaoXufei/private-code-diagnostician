package com.bugkiller_cityu.code_diagnostician.service.model.parser;

import com.bugkiller_cityu.code_diagnostician.service.model.common.LanguageType;

import java.util.List;

public interface ParseResult {
    // getFunctions 获取解析出的所有函数
    List<Function> getFunctions();

    // getCommentLines 获取注释行数
    int getCommentLines();

    // getTotalLines 获取总行数
    int getTotalLines();

    // getLanguage 获取语言类型
    LanguageType getLanguage();

    // getASTRoot 获取AST根节点(如果支持)
    Object getASTRoot();
}
