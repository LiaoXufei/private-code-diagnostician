package com.bugkiller_cityu.code_diagnostician.service.model.parser;

import com.bugkiller_cityu.code_diagnostician.service.model.common.LanguageType;

import java.util.List;

public interface ParseResult {
    // getFunctions (Get all parsed functions)
    List<Function> getFunctions();

    // getCommentLines (Get the number of comment lines)
    int getCommentLines();

    // getTotalLines (Get the total number of lines)
    int getTotalLines();

    // getLanguage (Get language type)
    LanguageType getLanguage();

    // getASTRoot (Get AST root node (if supported))
    Object getASTRoot();
}
