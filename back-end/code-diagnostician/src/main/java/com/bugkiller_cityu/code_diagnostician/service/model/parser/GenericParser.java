package com.bugkiller_cityu.code_diagnostician.service.model.parser;

import com.bugkiller_cityu.code_diagnostician.service.model.common.LanguageType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

// Generic parser for unsupported languages
public class GenericParser implements Parser {
    
    @Override
    public ParseResult parse(String filePath, byte[] content) throws Exception {
        String contentStr = new String(content);
        String[] lines = contentStr.split("\n");
        
        BaseParseResult result = new BaseParseResult();
        result.setFunctions(new ArrayList<>());
        result.setTotalLines(lines.length);
        result.setLanguage(LanguageType.UNSUPPORTED);
        result.setCommentLines(countGenericComments(lines));
        result.setFunctions(detectGenericFunctions(contentStr, lines));
        
        return result;
    }
    
    @Override
    public LanguageType[] getSupportedLanguages() {
        return new LanguageType[]{
            LanguageType.C,
            LanguageType.CPLUSPLUS,
            LanguageType.CSHARP,
            LanguageType.GO,
            LanguageType.JAVA,
            LanguageType.JAVASCRIPT,
            LanguageType.LUA,
            LanguageType.PYTHON,
            LanguageType.RUST,
            LanguageType.TYPESCRIPT,
            LanguageType.UNSUPPORTED
        };
    }
    
    private int countGenericComments(String[] lines) {
        int commentCount = 0;
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("//") ||
                trimmedLine.startsWith("#") ||
                trimmedLine.startsWith("/*") ||
                trimmedLine.startsWith("*")) {
                commentCount++;
            }
        }
        return commentCount;
    }
    
    private List<Function> detectGenericFunctions(String content, String[] lines) {
        List<Function> functions = new ArrayList<>();
        
        // Simple pattern to detect function-like structures
        Pattern genericPattern = Pattern.compile(
            "(?m)(function|def|void|int|bool|string|double|float)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\("
        );
        
        Matcher matcher = genericPattern.matcher(content);
        
        while (matcher.find()) {
            String funcName = matcher.group(2);
            int startPos = matcher.start();
            int startLine = getLineNumber(content, startPos);
            int endLine = findBlockEnd(content, startPos, lines, startLine);
            
            Function func = new Function();
            func.setName(funcName);
            func.setStartLine(startLine);
            func.setEndLine(endLine);
            func.setComplexity(1); // Default complexity
            func.setParameters(0); // Default parameters
            
            functions.add(func);
        }
        
        return functions;
    }
    
    private int getLineNumber(String content, int pos) {
        int line = 1;
        for (int i = 0; i < pos && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
    
    private int findBlockEnd(String content, int startPos, String[] lines, int startLine) {
        int bracketCount = 0;
        boolean foundFirstBracket = false;
        
        for (int i = startPos; i < content.length(); i++) {
            char c = content.charAt(i);
            
            switch (c) {
                case '{':
                    bracketCount++;
                    foundFirstBracket = true;
                    break;
                case '}':
                    if (foundFirstBracket) {
                        bracketCount--;
                        if (bracketCount == 0) {
                            return getLineNumber(content, i);
                        }
                    }
                    break;
            }
        }
        
        return lines.length;
    }
}
