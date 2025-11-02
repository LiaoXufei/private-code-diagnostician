package com.bugkiller_cityu.code_diagnostician.service.model.parser;

import com.bugkiller_cityu.code_diagnostician.service.model.common.LanguageType;
import com.sun.source.tree.CompilationUnitTree;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseParseResult implements ParseResult {
    private List<Function> functions;
    private int commentLines;
    private int totalLines;
    private LanguageType language;
    private CompilationUnitTree ASTRoot;
    private byte[] content;

}
