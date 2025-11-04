package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;



public class NamingConventionMetric extends BaseMetric implements Metric {
    public NamingConventionMetric() {
        super("命名规范", "检查包名、类名、方法名、变量名、常量名是否符合规范", 0.08);
    }
    @Override
    public MetricResult analyze(ParseResult parseResult) {
        JavaASTInfo astInfo = JavaASTExtractor.extractJavaAST(parseResult);

        if (astInfo == null || astInfo.getCompilationUnit() == null) {
            return new MetricResult(0.0, new ArrayList<>(), getDescription(), getWeight());
        }

        CompilationUnit cu = astInfo.getCompilationUnit();
        List<String> issues = new ArrayList<>();

        // === 包名检测 ===
        PackageDeclaration pkg = cu.getPackage();
        int total = 0;
        int bad = 0;

        if (pkg != null) {
            String pkgName = pkg.getName().getFullyQualifiedName();
            total++;
            if (!NamingRules.isValidPackageName(pkgName)) {
                issues.add(String.format("包名 '%s' 不符合规范，应为小写字母且不包含下划线", pkgName));
                bad++;
            }
        }

        // === AST 遍历 ===
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                String name = node.getName().getIdentifier();
                synchronized (issues) {
                    totalCount[0]++;
                    if (!NamingRules.isValidTypeName(name)) {
                        issues.add(String.format("类型名 '%s' 不符合规范（应为PascalCase）", name));
                        badCount[0]++;
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(EnumDeclaration node) {
                String name = node.getName().getIdentifier();
                synchronized (issues) {
                    totalCount[0]++;
                    if (!NamingRules.isValidTypeName(name)) {
                        issues.add(String.format("枚举名 '%s' 不符合规范（应为PascalCase）", name));
                        badCount[0]++;
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodDeclaration node) {
                String name = node.getName().getIdentifier();
                synchronized (issues) {
                    totalCount[0]++;
                    if (!NamingRules.isValidMethodName(name)) {
                        issues.add(String.format("方法名 '%s' 不符合规范（应为camelCase）", name));
                        badCount[0]++;
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(VariableDeclarationFragment node) {
                String name = node.getName().getIdentifier();
                synchronized (issues) {
                    totalCount[0]++;
                    // 判断是否为常量
                    boolean isConst = false;
                    if (node.getParent() instanceof FieldDeclaration fd) {
                        int modifiers = fd.getModifiers();
                        isConst = Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);
                    }

                    if (isConst) {
                        if (!NamingRules.isValidConstName(name)) {
                            issues.add(String.format("常量名 '%s' 不符合规范（应为UPPER_SNAKE_CASE）", name));
                            badCount[0]++;
                        }
                    } else {
                        if (!NamingRules.isValidVariableName(name)) {
                            issues.add(String.format("变量名 '%s' 不符合规范（应为camelCase或小写）", name));
                            badCount[0]++;
                        }
                    }
                }
                return super.visit(node);
            }
        });

        double score = computeScore(totalCount[0], badCount[0]);
        return new MetricResult(score, issues, getDescription(), getWeight());
    }

    /**
     * 根据不良比例计算得分（越低越差）
     */
    private double computeScore(int total, int bad) {
        if (total == 0) return 0.0;
        double ratio = (double) bad / total;
        double score = 1.0 - ratio;
        if (score < 0) score = 0;
        return Math.round(score * 100.0) / 100.0;
    }

    // 使用内部字段记录计数（为AST匿名内部类服务）
    private final int[] totalCount = {0};
    private final int[] badCount = {0};
}