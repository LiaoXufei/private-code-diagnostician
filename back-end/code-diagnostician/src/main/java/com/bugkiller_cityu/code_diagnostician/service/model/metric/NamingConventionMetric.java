package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 命名规范度量指标 (Java 版)
 * 对类名、方法名、变量名、常量名、包名进行检查
 */
public class NamingConventionMetric extends BaseMetric implements Metric {

    public NamingConventionMetric() {
        super("命名规范",
                "检查Java代码的命名是否符合规范，包括包名、变量名、方法名、类型名等",
                0.08);
    }

    @Override
    public MetricResult analyze(ParseResult parseResult) {
        JavaASTInfo astInfo = JavaASTExtractor.extractJavaAST(parseResult);
        if (astInfo == null || astInfo.getCompilationUnit() == null) {
            return new MetricResult(0.0, new ArrayList<>(), getDescription(), getWeight());
        }

        CompilationUnit cu = astInfo.getCompilationUnit();
        List<String> issues = new ArrayList<>();
        final int[] totalNames = {0};
        final int[] badNames = {0};

        // 包名检查
        PackageDeclaration pkg = cu.getPackage();
        if (pkg != null) {
            totalNames[0]++;
            String pkgName = pkg.getName().getFullyQualifiedName();
            if (!NamingRules.isValidPackageName(pkgName)) {
                issues.add(String.format("包名 '%s' 不符合规范，应使用小写字母且不包含下划线。", pkgName));
                badNames[0]++;
            }
        }

        // 遍历AST中类、方法、变量、常量声明
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                totalNames[0]++;
                String name = node.getName().getIdentifier();
                if (!NamingRules.isValidTypeName(name)) {
                    issues.add(String.format("类型名 '%s' 不符合规范，应使用帕斯卡命名法。", name));
                    badNames[0]++;
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                totalNames[0]++;
                String name = node.getName().getIdentifier();
                if (!NamingRules.isValidFuncName(name)) {
                    issues.add(String.format("方法名 '%s' 不符合规范，应使用驼峰命名法。", name));
                    badNames[0]++;
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(VariableDeclarationFragment node) {
                totalNames[0]++;
                String name = node.getName().getIdentifier();

                if (isConstant(node)) {
                    if (!NamingRules.isValidConstName(name)) {
                        issues.add(String.format("常量名 '%s' 不符合规范，应使用全大写加下划线或帕斯卡命名法。", name));
                        badNames[0]++;
                    }
                } else {
                    if (!NamingRules.isValidVarName(name)) {
                        issues.add(String.format("变量名 '%s' 不符合规范，应使用小驼峰或小写命名。", name));
                        badNames[0]++;
                    }
                }
                return super.visit(node);
            }

            private boolean isConstant(VariableDeclarationFragment node) {
                ASTNode parent = node.getParent();
                if (parent instanceof FieldDeclaration) {
                    FieldDeclaration field = (FieldDeclaration) parent;
                    int modifiers = field.getModifiers();
                    return Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
                }
                return false;
            }
        });

        double score;
        if (totalNames[0] == 0) {
            score = 0.0;
        } else {
            double badRatio = (double) badNames[0] / totalNames[0];
            score = calculateScore(badRatio);
        }

        return new MetricResult(score, issues, getDescription(), getWeight());
    }

    // 计分规则：基础分 0.4，每1%不良命名增加0.1分，最高1.0
    private double calculateScore(double badRatio) {
        double baseScore = 0.4;
        double increase = badRatio * 10.0;
        double score = baseScore + increase;
        return Math.min(score, 1.0);
    }
}
