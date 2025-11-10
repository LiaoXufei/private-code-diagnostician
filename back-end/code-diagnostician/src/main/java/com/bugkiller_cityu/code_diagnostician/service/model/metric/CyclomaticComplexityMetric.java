package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

@Getter
@Setter
@ToString
public class CyclomaticComplexityMetric extends BaseMetric implements Metric {

    public CyclomaticComplexityMetric() {
        super(
                "圈复杂度分析",
                "检测代码的圈复杂度（Cyclomatic Complexity），评估函数的逻辑复杂度与可维护性。",
                0.3
        );
    }

    @Override
    public MetricResult analyze(ParseResult parseResult) {
        JavaASTInfo astInfo = JavaASTExtractor.extractJavaAST(parseResult);
        if (astInfo == null || astInfo.getCompilationUnit() == null) {
            return new MetricResult(0.0, new ArrayList<>(), this.getDescription(), this.getWeight());
        }

        ScoreAndIssues scoreAndIssues = analyzeComplexity(astInfo);
        return new MetricResult(
                scoreAndIssues.getScore(),
                scoreAndIssues.getIssues(),
                this.getDescription(),
                this.getWeight()
        );
    }

    private ScoreAndIssues analyzeComplexity(JavaASTInfo astInfo) {
        List<String> issues = new ArrayList<>();
        CompilationUnit cu = astInfo.getCompilationUnit();

        // 使用线程安全的累加器
        AtomicInteger funcCount = new AtomicInteger(0);
        DoubleAdder totalComplexity = new DoubleAdder();

        // 遍历方法节点
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                int complexity = calculateComplexity(node);
                String funcName = node.getName().getIdentifier();

                funcCount.incrementAndGet();
                totalComplexity.add(complexity);

                if (complexity > 15) {
                    issues.add(String.format("函数 '%s' 圈复杂度过高 (%d)，应尽快重构以降低逻辑分支。", funcName, complexity));
                } else if (complexity > 10) {
                    issues.add(String.format("函数 '%s' 圈复杂度偏高 (%d)，建议优化控制结构。", funcName, complexity));
                }
                return super.visit(node);
            }
        });

        int totalFuncs = funcCount.get();
        if (totalFuncs == 0) {
            return new ScoreAndIssues(0.0, issues);
        }

        double avgComplexity = totalComplexity.sum() / totalFuncs;
        double score = calculateComplexityScore(avgComplexity);

        return new ScoreAndIssues(score, issues);
    }

    /**
     * 计算单个方法的圈复杂度
     */
    private int calculateComplexity(MethodDeclaration node) {
        final AtomicInteger complexity = new AtomicInteger(1); // 基础复杂度为1

        node.accept(new ASTVisitor() {
            @Override
            public boolean visit(IfStatement stmt) {
                complexity.incrementAndGet();
                return super.visit(stmt);
            }

            @Override
            public boolean visit(ForStatement stmt) {
                complexity.incrementAndGet();
                return super.visit(stmt);
            }

            @Override
            public boolean visit(EnhancedForStatement stmt) {
                complexity.incrementAndGet();
                return super.visit(stmt);
            }

            @Override
            public boolean visit(WhileStatement stmt) {
                complexity.incrementAndGet();
                return super.visit(stmt);
            }

            @Override
            public boolean visit(DoStatement stmt) {
                complexity.incrementAndGet();
                return super.visit(stmt);
            }

            @Override
            public boolean visit(SwitchCase stmt) {
                if (!stmt.isDefault()) {
                    complexity.incrementAndGet();
                }
                return super.visit(stmt);
            }

            @Override
            public boolean visit(CatchClause stmt) {
                complexity.incrementAndGet();
                return super.visit(stmt);
            }

            @Override
            public boolean visit(ConditionalExpression expr) {
                complexity.incrementAndGet();
                return super.visit(expr);
            }

            @Override
            public boolean visit(InfixExpression expr) {
                InfixExpression.Operator op = expr.getOperator();
                if (op == InfixExpression.Operator.CONDITIONAL_AND || op == InfixExpression.Operator.CONDITIONAL_OR) {
                    complexity.incrementAndGet();
                }
                return super.visit(expr);
            }
        });

        return complexity.get();
    }

    /**
     * 根据平均复杂度计算得分
     * 越复杂得分越高（越差）
     */
    private double calculateComplexityScore(double avgComplexity) {
        if (avgComplexity <= 5) return 0.2;
        if (avgComplexity <= 10) return 0.4;
        if (avgComplexity <= 15) return 0.6;
        if (avgComplexity <= 20) return 0.8;
        return 1.0;
    }
}
