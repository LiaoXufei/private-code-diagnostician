package com.bugkiller_cityu.code_diagnostician.service.model.parser;

import com.bugkiller_cityu.code_diagnostician.service.model.common.LanguageType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class JavaParser implements Parser {
    
    @Override
    public ParserResult parse(String filePath, byte[] content) throws Exception {
        String contentStr = new String(content);
        String[] lines = contentStr.split("\n");
        
        BaseParserResult result = new BaseParserResult();
        result.setFunctions(new ArrayList<>());
        result.setTotalLines(lines.length);
        result.setLanguage(LanguageType.JAVA);
        result.setCommentLines(countCommentLines(contentStr));
        result.setFunctions(detectJavaFunctions(lines));
        
        return result;
    }
    
    @Override
    public LanguageType[] getSupportedLanguages() {
        return new LanguageType[]{LanguageType.JAVA};
    }
    
    // Count comment lines in Java code
    private int countCommentLines(String content) {
        int commentCount = 0;
        String[] lines = content.split("\n");
        boolean inBlockComment = false;
        boolean inJavadoc = false;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            if (inBlockComment || inJavadoc) {
                commentCount++;
                if (trimmedLine.contains("*/")) {
                    inBlockComment = false;
                    inJavadoc = false;
                }
                continue;
            }
            
            if (trimmedLine.startsWith("//")) {
                commentCount++;
                continue;
            }
            
            if (trimmedLine.startsWith("/*")) {
                commentCount++;
                inBlockComment = true;
                if (trimmedLine.contains("*/")) {
                    inBlockComment = false;
                }
                continue;
            }
            
            if (trimmedLine.startsWith("/**")) {
                commentCount++;
                inJavadoc = true;
                if (trimmedLine.contains("*/")) {
                    inJavadoc = false;
                }
                continue;
            }
        }
        
        return commentCount;
    }
    
    // Detect Java functions based on text analysis
    private List<Function> detectJavaFunctions(String[] lines) {
        List<Function> functions = new ArrayList<>();
        
        boolean inComment = false;
        int bracketLevel = 0;
        String currentFunc = "";
        int startLine = 0;
        boolean inMethod = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();
            
            // Skip single line comments
            if (trimmedLine.startsWith("//") ||
                (trimmedLine.startsWith("/*") && trimmedLine.endsWith("*/"))) {
                continue;
            }
            
            // Skip multi-line comments
            if (!inComment && trimmedLine.startsWith("/*")) {
                inComment = true;
                continue;
            }
            if (inComment) {
                if (trimmedLine.startsWith("*/")) {
                    inComment = false;
                }
                continue;
            }
            
            // Detect method start
            if (!inMethod && (trimmedLine.contains("public ") ||
                trimmedLine.contains("private ") ||
                trimmedLine.contains("protected ") ||
                trimmedLine.contains("static ")) &&
                trimmedLine.contains("(") &&
                !trimmedLine.contains("=") &&
                !trimmedLine.endsWith(";") &&
                !trimmedLine.startsWith("//")) {
                
                // Simple method name extraction
                String[] parts = trimmedLine.split("\\(");
                if (parts.length > 0) {
                    String[] nameSegments = parts[0].trim().split("\\s+");
                    if (nameSegments.length > 0) {
                        currentFunc = nameSegments[nameSegments.length - 1];
                        startLine = i + 1;
                        inMethod = true;
                    }
                }
            }
            
            // Bracket counting
            if (inMethod) {
                bracketLevel += countOccurrences(line, "{");
                bracketLevel -= countOccurrences(line, "}");
                
                // Method end
                if (bracketLevel <= 0 && line.contains("}")) {
                    // Calculate parameter count
                    int paramCount = 0;
                    if (lines[startLine - 1].split("\\(").length > 1) {
                        String paramStr = lines[startLine - 1].split("\\(")[1].split("\\)")[0];
                        if (!paramStr.trim().isEmpty()) {
                            paramCount = countOccurrences(paramStr, ",") + 1;
                        }
                    }
                    
                    // Recalculate function content
                    StringBuilder functionContent = new StringBuilder();
                    for (int j = startLine - 1; j <= i; j++) {
                        functionContent.append(lines[j]).append("\n");
                    }
                    
                    // Estimate complexity
                    String content = functionContent.toString();
                    int complexity = 1 + countOccurrences(content, "if ") + 
                                   countOccurrences(content, "for ") +
                                   countOccurrences(content, "while ") + 
                                   countOccurrences(content, "catch ") +
                                   countOccurrences(content, "case ");
                    
                    Function func = new Function();
                    func.setName(currentFunc);
                    func.setStartLine(startLine);
                    func.setEndLine(i + 1);
                    func.setComplexity(complexity);
                    func.setParameters(paramCount);
                    
                    functions.add(func);
                    
                    inMethod = false;
                    bracketLevel = 0;
                }
            }
        }
        
        return functions;
    }
    
    // Helper method to count occurrences of a substring
    private int countOccurrences(String str, String subStr) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(subStr, index)) != -1) {
            count++;
            index += subStr.length();
        }
        return count;
    }
}
