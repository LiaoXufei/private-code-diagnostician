package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.entity.Issue;
import com.bugkiller_cityu.code_diagnostician.service.model.common.SeverityEnum;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
// CodeDuplicationMetric 评估代码重复度
public class CodeDuplicationMetric extends BaseMetric implements Metric{

    public CodeDuplicationMetric() {
        super(
                "代码重复度",
                "评估代码中重复逻辑的比例，重复代码越多，越需要抽象和重构",
                0.15
        );

    }

    public MetricResult analyze(ParseResult parseResult) {
        JavaASTInfo javaASTInfo = JavaASTExtractor.extractJavaAST(parseResult);
        List<Issue> issues = new ArrayList<>();  // 改为Issue类型列表
        if (javaASTInfo == null){
            return new MetricResult(
                    0.6,
                    issues,
                    this.getDescription(),
                    this.getWeight()
            );
        }
        Map<String, List<MethodDeclaration>> similarFunctions = new HashMap<>();  // 存储方法声明便于获取行号等信息
        int totalFunctions = 0;

        CompilationUnit cu = javaASTInfo.getCompilationUnit();
        String filePath = (String) cu.getProperty("filePath");
//        String filePath = parseResult.getFilePath();  // 假设ParseResult有获取文件路径的方法

        // 遍历所有类型声明（类、接口等）
        for (Object typeObj : cu.types()) {
            if (typeObj instanceof TypeDeclaration typeDecl) {

                // 遍历类中的方法
                for (MethodDeclaration methodDecl : typeDecl.getMethods()) {
                    if (methodDecl.getBody() == null) {
                        continue;
                    }

                    totalFunctions++;
                    // 提取函数签名
                    String signature = extractFunctionSignature(methodDecl);

                    similarFunctions
                            .computeIfAbsent(signature, k -> new ArrayList<>())
                            .add(methodDecl);  // 存储方法声明而非仅方法名
                }
            }
        }

        // 查找重复函数
        int duplicatedCount = findDuplicatedFunctions(similarFunctions, issues, filePath, cu);

//        // 如果函数数量太少，返回低分
//        if (totalFunctions < 3) {
//            return new MetricResult(
//                    0.1,
//                    issues,
//                    this.getDescription(),
//                    this.getWeight()
//            );
//        }

        if(totalFunctions == 0){
            return new MetricResult(
                    1,
                    issues,
                    this.getDescription(),
                    this.getWeight()
            );
        }
        // 计算重复率和得分
        double duplicationRate = (double) duplicatedCount / totalFunctions;
        double score = calculateScore(duplicationRate);

        return new MetricResult(
                score,
                issues,
                this.getDescription(),
                this.getWeight()
        );
    }

    /**
     * 查找重复函数并生成Issue列表
     *
     * @param similarFunctions 键为函数签名，值为相似方法声明列表
     * @param issues           存储Issue对象的列表
     * @param filePath         代码文件路径
     * @param cu               编译单元（用于计算行号）
     * @return 重复函数的数量
     */
    public int findDuplicatedFunctions(Map<String, List<MethodDeclaration>> similarFunctions,
                                       List<Issue> issues,
                                       String filePath,
                                       CompilationUnit cu) {
        int duplicatedCount = 0;

        for (Map.Entry<String, List<MethodDeclaration>> entry : similarFunctions.entrySet()) {
            String signature = entry.getKey();
            List<MethodDeclaration> functions = entry.getValue();

            // 忽略太短的函数签名
            if (functions.size() > 1 && signature.length() > 10) {
                duplicatedCount += functions.size() - 1;

                // 拼接函数名列表
                List<String> functionNames = functions.stream()
                        .map(m -> m.getName().getIdentifier())
                        .toList();
                String joinedFunctions = String.join(", ", functionNames);
                String issueMsg = String.format("可能存在重复实现: %s", joinedFunctions);

                // 创建Issue对象
                Issue issue = new Issue();
                issue.setType("code_duplication");  // 问题类型：代码重复
                if (functions.size() > 10){
                    issue.setSeverity(SeverityEnum.HIGH.getMessage());
                } else if (functions.size() > 5){
                    issue.setSeverity(SeverityEnum.MEDIUM.getMessage());
                } else {
                    issue.setSeverity(SeverityEnum.LOW.getMessage());
                }
                issue.setFile(filePath);  // 设置文件路径
                // 取第一个重复方法的行号作为问题行号
                int startPos = functions.get(0).getStartPosition();
                issue.setLine(cu.getLineNumber(startPos));  // 计算行号
                issue.setMessage(issueMsg);  // 问题描述
                issue.setSuggestion("建议抽象重复逻辑，提取为公共方法以减少代码冗余，提高可维护性");  // 优化建议

                issues.add(issue);
            }
        }

        return duplicatedCount;
    }

    /**
     * 根据重复率计算得分
     * 基础分 0.4，每增加 1% 重复率增加 0.1 分（即 duplicationRate * 10）
     * 最大得分为 1.0
     *
     * @param duplicationRate 重复率（0~1）
     * @return 计算后的得分（0~1）
     */
    public double calculateScore(double duplicationRate) {
        double baseScore = 0.0;
        double increasePerPercent = 1.0;

        double score = baseScore + (duplicationRate * increasePerPercent);

        // 限制范围
        return 1.0 - Math.min(score, 1.0);
    }

    // 提取方法结构签名
    public String extractFunctionSignature(MethodDeclaration methodDecl) {
        StringBuilder signature = new StringBuilder();

        // 获取方法体
        Block body = methodDecl.getBody();
        if (body == null) {
            return "";
        }

        // 遍历 AST
        body.accept(new ASTVisitor() {
            @Override
            public boolean visit(IfStatement node) {
                signature.append("if");
                return true; // 继续访问子节点
            }

            @Override
            public boolean visit(ForStatement node) {
                signature.append("for");
                return true;
            }

            @Override
            public boolean visit(EnhancedForStatement node) {
                signature.append("range");
                return true;
            }

            @Override
            public boolean visit(SwitchStatement node) {
                signature.append("switch");
                return true;
            }

            @Override
            public boolean visit(SwitchCase node) {
                signature.append("case");
                return true;
            }

            @Override
            public boolean visit(Assignment node) {
                signature.append("=");
                return true;
            }

            @Override
            public boolean visit(ReturnStatement node) {
                signature.append("return");
                return true;
            }
        });

        return signature.toString();
    }

}