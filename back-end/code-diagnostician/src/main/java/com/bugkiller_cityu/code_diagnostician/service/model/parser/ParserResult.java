package com.bugkiller_cityu.code_diagnostician.service.model.parser;

import java.util.List;

public interface ParserResult {
    // getFunctions 获取解析出的所有函数
    List<Function> getFunctions();

    // getCommentLines 获取注释行数
    int getCommentLines();

    // getTotalLines 获取总行数
    int getTotalLines();

    // getASTRoot 获取AST根节点(如果支持)
    Object getASTRoot();
}
