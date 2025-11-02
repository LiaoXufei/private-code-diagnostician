package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.service.model.parser.Function;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
import lombok.Data;
import lombok.ToString;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class FunctionLengthMetric extends BaseMetric {
    public FunctionLengthMetric() {
        super(
                "状态管理",
                "检测函数长度及状态变量管理，合理的函数长度和状态管理能提高代码可维护性",
                0.2
        );
    }

    public MetricResult analyze(ParseResult parseResult) {
        JavaASTInfo astInfo = JavaASTExtractor.extractJavaAST(parseResult);

        if (astInfo == null) {
            return new MetricResult(0.0, null, this.getDescription(), this.getWeight());
        }
        if (astInfo.getContent().length == 0) {
            astInfo.setContent("\n".repeat(parseResult.getTotalLines()).getBytes(StandardCharsets.UTF_8));
        }

        // TODO：score, issues = analyzeFunctions(parseResult);

        return new MetricResult(0.0, null, this.getDescription(), this.getWeight());
    }

    private ScoreAndIssues analyzeFunctions(JavaASTInfo astInfo, ParseResult parseResult) {
        List<String> issues = new ArrayList<>();
        List<Function> functions = parseResult.getFunctions();
        // if empty, return empty issues
        if (functions.isEmpty()) {
            return new ScoreAndIssues(0.0, issues);
        }

        int totalComplexity = 0;
        int longFunctions = 0;
        int veryLongFunctions = 0;
        int extremeLongFunctions = 0;
        int totalFunctions = functions.size();

        for (Function fn : functions) {
            int lineCount = fn.getEndLine() - fn.getStartLine() + 1;

            String locationInfo;
            if (lineCount > 120) {
                locationInfo = this.getLocationInfo(fn, (CompilationUnit) parseResult.getASTRoot(), astInfo);
                issues.add(String.format("函数 '%s'%s 极度过长 (%d 行)，必须拆分", fn.getName(), locationInfo, lineCount));
                ++extremeLongFunctions;
            } else if (lineCount > 70) {
                locationInfo = this.getLocationInfo(fn, (CompilationUnit) parseResult.getASTRoot(), astInfo);
                issues.add(String.format("函数 '%s'%s 过长 (%d 行)，建议拆分", fn.getName(), locationInfo, lineCount));
                ++veryLongFunctions;
            } else if (lineCount > 40) {
                locationInfo = this.getLocationInfo(fn, (CompilationUnit) parseResult.getASTRoot(), astInfo);
                issues.add(String.format("函数 '%s'%s 较长 (%d 行)，可考虑重构", fn.getName(), locationInfo, lineCount));
                ++longFunctions;
            }
        }
        // TODO: 函数复杂度

        double longRatio = (double) longFunctions / (double) totalFunctions;
        double veryLongRatio = (double) veryLongFunctions / (double) totalFunctions;
        double extremeLongRatio = (double) extremeLongFunctions / (double) totalFunctions;

        double lengthScore = longRatio * 0.3 + veryLongRatio * 0.5 + extremeLongRatio * 0.8;
        if (lengthScore > 1.0) {
            lengthScore = 1.0;
        }
        double complexityScore = 0.0;

        return new ScoreAndIssues(lengthScore * 0.7 + complexityScore * 0.3, issues);
    }

    private String getLocationInfo(Function fn, CompilationUnit cu, JavaASTInfo astInfo) {
        Object methodDeclaration = fn.getNode();

        if (methodDeclaration != null) {
            if (methodDeclaration instanceof MethodDeclaration) {
                MethodDeclaration md = (MethodDeclaration) methodDeclaration;

                int startLine = cu.getLineNumber(md.getStartPosition());
                int endLine = cu.getLineNumber(md.getStartPosition() + md.getLength());
                String fileName = (String) cu.getProperty("filePath");
                return String.format("(位于 %s:%d)", fileName, startLine);
            }
        }

        String[] lines = new String(astInfo.getContent(), StandardCharsets.UTF_8).split("\n", -1);
        if (astInfo.getContent().length > 0 && fn.getStartLine() > 0 && fn.getStartLine() < lines.length) {
            String firstLine = lines[fn.getStartLine() - 1].trim();
            if (firstLine.length() > 30) {
                firstLine = firstLine.substring(0, 30) + "...";
            }
            return String.format(" (%s)", firstLine);
        }

        return "";
    }
}
