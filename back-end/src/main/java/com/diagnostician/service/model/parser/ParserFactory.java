package com.diagnostician.service.model.parser;

import com.diagnostician.service.model.common.LanguageType;

// Parser factory - creates parsers based on language type
public class ParserFactory {
    
    // Create parser based on language type
    public static Parser createParser(LanguageType language) {
        switch (language) {
            case JAVA:
                return new JavaParser();
            default:
                return new GenericParser();
        }
    }
    
    // Create parser based on file extension
    public static Parser createParserForFile(String filePath) {
        String extension = getFileExtension(filePath);
        switch (extension.toLowerCase()) {
            case "java":
                return new JavaParser();
            default:
                return new GenericParser();
        }
    }
    
    private static String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }
}
