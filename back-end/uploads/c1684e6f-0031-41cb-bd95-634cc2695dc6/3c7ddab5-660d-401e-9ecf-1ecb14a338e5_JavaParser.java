package com.bugkiller_cityu.code_diagnostician.service.model.parser;

import com.bugkiller_cityu.code_diagnostician.service.model.common.LanguageType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaParser implements Parser {

    @Override
    public ParseResult parse(String filePath, byte[] content) throws Exception {
        String contentStr = new String(content);
        String[] lines = contentStr.split("\n");

        BaseParseResult result = new BaseParseResult();
        result.setTotalLines(lines.length);
        result.setLanguage(LanguageType.JAVA);
        result.setCommentLines(countCommentLines(contentStr));
        // 将 contentStr 也传递进去，便于全局搜索注解
        result.setFunctions(detectJavaFunctions(lines, contentStr));

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
    private List<Function> detectJavaFunctions(String[] lines, String fullContent) {
        List<Function> functions = new ArrayList<>();

        // 1. 尝试提取类名，用于Lombok构造函数的命名
        String className = extractClassName(fullContent);

        // 2. 处理 Lombok 注解 (虚拟生成方法)
        handleLombokAnnotations(functions, fullContent, className);

        // 3. 原有的常规方法解析逻辑
        boolean inComment = false;
        int bracketLevel = 0;
        String currentFunc = "";
        int startLine = 0;
        boolean inMethod = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            // Skip comments
            if (trimmedLine.startsWith("//") || (trimmedLine.startsWith("/*") && trimmedLine.endsWith("*/"))) continue;
            if (!inComment && trimmedLine.startsWith("/*")) { inComment = true; continue; }
            if (inComment) { if (trimmedLine.contains("*/")) inComment = false; continue; }

            // Detect method start
            // 增加 !trimmedLine.contains("class ") 避免误把 class 定义当成方法
            if (!inMethod &&
                    (trimmedLine.contains("public ") || trimmedLine.contains("private ") || trimmedLine.contains("protected ") || trimmedLine.contains("static ")) &&
                    trimmedLine.contains("(") &&
                    !trimmedLine.contains("=") &&
                    !trimmedLine.endsWith(";") &&
                    !trimmedLine.contains("class ") && // 防止解析 'public class X'
                    !trimmedLine.startsWith("//")) {

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
                    int paramCount = 0;
                    // 安全检查：防止数组越界
                    if (startLine - 1 < lines.length) {
                        String defLine = lines[startLine - 1];
                        if(defLine.contains("(") && defLine.contains(")")) {
                            String paramStr = defLine.substring(defLine.indexOf("(") + 1, defLine.lastIndexOf(")"));
                            if (!paramStr.trim().isEmpty()) {
                                paramCount = countOccurrences(paramStr, ",") + 1;
                            }
                        }
                    }

                    StringBuilder functionContent = new StringBuilder();
                    for (int j = startLine - 1; j <= i; j++) {
                        functionContent.append(lines[j]).append("\n");
                    }

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

    /**
     * 专门处理 Lombok 注解逻辑
     */
    private void handleLombokAnnotations(List<Function> functions, String content, String className) {
        // 如果没有类名，默认为 Constructor
        String validClassName = (className != null && !className.isEmpty()) ? className : "Constructor";

        // 检测 @NoArgsConstructor
        if (content.contains("@NoArgsConstructor")) {
            Function func = new Function();
            func.setName(validClassName); // 构造函数名与类名相同
            func.setStartLine(0); // 虚拟行号
            func.setEndLine(0);
            func.setComplexity(1); // 默认构造函数复杂度为 1
            func.setParameters(0); // 无参
            functions.add(func);
        }

        // 检测 @AllArgsConstructor
        if (content.contains("@AllArgsConstructor")) {
            // 估算字段数量作为参数数量
            int fieldCount = countFields(content);

            Function func = new Function();
            func.setName(validClassName);
            func.setStartLine(0);
            func.setEndLine(0);
            func.setComplexity(1);
            func.setParameters(fieldCount);
            functions.add(func);
        }

        // 你还可以添加 @Data, @Getter, @Setter 的处理，但由于这些生成方法太多，通常不建议全部加进去
    }

    // 简单提取类名
    private String extractClassName(String content) {
        // 匹配 "public class ClassName" 或 "class ClassName"
        Pattern pattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "UnknownClass";
    }

    // 简单估算字段数量 (private Type name;)
    private int countFields(String content) {
        int count = 0;
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trim = line.trim();
            // 这是一个非常粗略的估算，假设字段以 private 开头并以分号结尾，且不包含括号（排除方法）
            if (trim.startsWith("private ") && trim.endsWith(";") && !trim.contains("(")) {
                count++;
            }
        }
        return count;
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