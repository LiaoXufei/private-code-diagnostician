package com.diagnostician.service.model.parser;

import com.diagnostician.service.model.common.LanguageType;
import com.sun.source.tree.CompilationUnitTree;

// Parser interface - defines standard methods for code parsers
public interface Parser {
    /**
     * Parse code content
     * param:
     *      filePath file path
     *      content file content
     * return: 
     *      ParserResult parsing result
     * throws: 
     *      Exception possible parsing errors
     */
    ParseResult parse(String filePath, byte[] content) throws Exception;
    
    /**
     * Get supported language types
     * return:
     *      array of supported language types
     */
    LanguageType[] getSupportedLanguages();
}
