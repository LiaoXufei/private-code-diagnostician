package com.diagnostician.service.model.metric;

import com.diagnostician.entity.Issue;
import com.diagnostician.service.model.common.SeverityEnum;
import com.diagnostician.service.model.parser.ParseResult;
import org.eclipse.jdt.core.dom.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 命名规范度量指标 (Java 版)
 * 功能：分析Java代码的包名、类名、方法名、变量名、常量名的命名规范性。
 */
public class NamingConventionMetric extends BaseMetric implements Metric {
    private int totalNames;
    private int badNames;

    public NamingConventionMetric() {
        super(
                "命名规范",
                "检查Java代码中的命名是否符合规范，包括包名、变量名、方法名、类型名等",
                0.08
        );
        totalNames = 0;
        badNames = 0;
    }

    @Override
    public MetricResult analyze(ParseResult parseResult) {
        // 1 提取 AST
        JavaASTInfo astInfo = JavaASTExtractor.extractJavaAST(parseResult);
        if (astInfo == null) {
            return new MetricResult(0.0, new ArrayList<>(), this.getDescription(), this.getWeight());
        }

        // 2 确保内容存在
        if (astInfo.getContent() == null || astInfo.getContent().length == 0) {
            astInfo.setContent("\n".repeat(Math.max(1, parseResult.getTotalLines())).getBytes(StandardCharsets.UTF_8));
        }

        CompilationUnit cu = astInfo.getCompilationUnit();
        // 3 调用内部分析逻辑（传入parseResult用于获取文件路径）
        ScoreAndIssues scoreAndIssues = analyzeNaming(cu);

        // 4 组装 MetricResult
        return new MetricResult(
                scoreAndIssues.getScore(),
                scoreAndIssues.getIssues(),
                this.getDescription(),
                this.getWeight()
        );
    }

    /**
     * 命名规范分析逻辑（主核心）
     * 修改为返回List<Issue>类型
     */
    private ScoreAndIssues analyzeNaming(CompilationUnit cu) {
        List<Issue> issues = new ArrayList<>();
        String filePath = (String) cu.getProperty("filePath");

        if (cu == null) {
            return new ScoreAndIssues(0.0, issues);
        }

        // 检查包名
        PackageDeclaration pkg = cu.getPackage();
        if (pkg != null) {
            totalNames++;
            String pkgName = pkg.getName().getFullyQualifiedName();
            if (!NamingRules.isValidPackageName(pkgName)) {
                // 创建包名问题的Issue对象
                Issue issue = new Issue();
                issue.setType("naming_convention");
                issue.setSeverity(SeverityEnum.LOW.getMessage());
                issue.setFile(filePath);
                issue.setLine(cu.getLineNumber(pkg.getStartPosition())); // 获取行号
                issue.setMessage(String.format("包名 '%s' 不符合规范，应使用小写字母且不包含下划线。", pkgName));
                issue.setSuggestion("建议修改为全小写字母组成的包名，避免使用下划线或大写字母。");
                issues.add(issue);
                badNames++;
            }
        }

        // 遍历 AST，检查命名项
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                totalNames++;
                String name = node.getName().getIdentifier();
                if (!NamingRules.isValidTypeName(name)) {
                    Issue issue = new Issue();
                    issue.setType("naming_convention");
                    issue.setSeverity(SeverityEnum.LOW.getMessage());
                    issue.setFile(filePath);
                    issue.setLine(cu.getLineNumber(node.getStartPosition()));
                    issue.setMessage(String.format("类型名 '%s' 不符合规范，应使用帕斯卡命名法（大写开头）。", name));
                    issue.setSuggestion("建议修改为帕斯卡命名法，即首字母大写，后续每个单词首字母也大写（如：UserService）。");
                    issues.add(issue);
                    badNames++;
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                totalNames++;
                if (node.isConstructor()) {
                    return super.visit(node);
                }
                String name = node.getName().getIdentifier();
                if (!NamingRules.isValidFuncName(name)) {
                    Issue issue = new Issue();
                    issue.setType("naming_convention");
                    issue.setSeverity(SeverityEnum.LOW.getMessage());
                    issue.setFile(filePath);
                    issue.setLine(cu.getLineNumber(node.getStartPosition()));
                    issue.setMessage(String.format("方法名 '%s' 不符合规范，应使用小驼峰命名法（小写开头）。", name));
                    issue.setSuggestion("建议修改为小驼峰命名法，即首字母小写，后续每个单词首字母大写（如：getUserInfo）。");
                    issues.add(issue);
                    badNames++;
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(VariableDeclarationFragment node) {
                totalNames++;
                String name = node.getName().getIdentifier();
                if (isConstant(node)) {
                    if (!NamingRules.isValidConstName(name)) {
                        Issue issue = new Issue();
                        issue.setType("naming_convention");
                        issue.setSeverity(SeverityEnum.MEDIUM.getMessage());
                        issue.setFile(filePath);
                        issue.setLine(cu.getLineNumber(node.getStartPosition()));
                        issue.setMessage(String.format("常量名 '%s' 不符合规范，应使用全大写加下划线。", name));
                        issue.setSuggestion("建议修改为全大写字母，单词间用下划线分隔（如：MAX_COUNT）。");
                        issues.add(issue);
                        badNames++;
                    }
                } else {
                    if (!NamingRules.isValidVarName(name)) {
                        Issue issue = new Issue();
                        issue.setType("naming_convention");
                        issue.setSeverity(SeverityEnum.LOW.getMessage());
                        issue.setFile(filePath);
                        issue.setLine(cu.getLineNumber(node.getStartPosition()));
                        issue.setMessage(String.format("变量名 '%s' 不符合规范，应使用小驼峰命名法。", name));
                        issue.setSuggestion("建议修改为小驼峰命名法，即首字母小写，后续每个单词首字母大写（如：userName）。");
                        issues.add(issue);
                        badNames++;
                    }
                }
                return super.visit(node);
            }

            private boolean isConstant(VariableDeclarationFragment node) {
                ASTNode parent = node.getParent();
                if (parent instanceof FieldDeclaration field) {
                    int modifiers = field.getModifiers();
                    return Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
                }
                return false;
            }
        });

        // 无命名项情况
        if (totalNames == 0) {
            return new ScoreAndIssues(1.0, issues);
        }

        // 计算比例与得分
        double badRatio = (double) badNames / totalNames;
        double score = calculateScore(badRatio);

        return new ScoreAndIssues(score, issues);
    }

    /**
     * 计分规则：基础分 0.4，每1%不良命名增加0.1分，最高1.0
     */
    private double calculateScore(double badRatio) {
        double base = 1.0;
        double increase = badRatio;
        double score = base - increase;
        return Math.max(score, 0.0);
    }
}