package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
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

        // 3 调用内部分析逻辑
        ScoreAndIssues scoreAndIssues = analyzeNaming(astInfo, parseResult);

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
     */
    private ScoreAndIssues analyzeNaming(JavaASTInfo astInfo) {
        List<String> issues = new ArrayList<>();

        CompilationUnit cu = astInfo.getCompilationUnit();
        if (cu == null) {
            return new ScoreAndIssues(0.0, issues);
        }

        // 检查包名
        PackageDeclaration pkg = cu.getPackage();
        if (pkg != null) {
            totalNames++;
            String pkgName = pkg.getName().getFullyQualifiedName();
            if (!NamingRules.isValidPackageName(pkgName)) {
                issues.add(String.format("包名 '%s' 不符合规范，应使用小写字母且不包含下划线。", pkgName));
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
                    issues.add(String.format("类型名 '%s' 不符合规范，应使用帕斯卡命名法（大写开头）。", name));
                    badNames++;
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                totalNames++;
                String name = node.getName().getIdentifier();
                if (!NamingRules.isValidFuncName(name)) {
                    issues.add(String.format("方法名 '%s' 不符合规范，应使用小驼峰命名法（小写开头）。", name));
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
                        issues.add(String.format("常量名 '%s' 不符合规范，应使用全大写加下划线或帕斯卡命名法。", name));
                        badNames++;
                    }
                } else {
                    if (!NamingRules.isValidVarName(name)) {
                        issues.add(String.format("变量名 '%s' 不符合规范，应使用小驼峰或小写命名。", name));
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

        //  无命名项情况
        if (totalNames == 0) {
            return new ScoreAndIssues(0.0, issues);
        }

        //  计算比例与得分
        double badRatio = (double) badNames / totalNames;
        double score = calculateScore(badRatio);

        return new ScoreAndIssues(score, issues);
    }

    /**
     * 计分规则：基础分 0.4，每1%不良命名增加0.1分，最高1.0
     */
    private double calculateScore(double badRatio) {
        double baseScore = 0.4;
        double increase = badRatio * 10.0;
        double score = baseScore + increase;
        return Math.min(score, 1.0);
    }
}

