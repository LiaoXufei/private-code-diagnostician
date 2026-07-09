package com.diagnostician.service.model.metric;

import com.diagnostician.entity.Issue;
import com.diagnostician.service.model.common.SeverityEnum;
import com.diagnostician.service.model.parser.ParseResult;
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
            return new MetricResult(1.0, new ArrayList<>(), this.getDescription(), this.getWeight());
        }

        CompilationUnit cu = astInfo.getCompilationUnit();
        String filePath = (String) cu.getProperty("filePath");
        ScoreAndIssues scoreAndIssues = analyzeComplexity(astInfo, filePath);
        return new MetricResult(
                scoreAndIssues.getScore(),
                scoreAndIssues.getIssues(),
                this.getDescription(),
                this.getWeight()
        );
    }

    private ScoreAndIssues analyzeComplexity(JavaASTInfo astInfo, String filePath) {
        List<Issue> issues = new ArrayList<>();
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
                // 获取方法所在行号（1-based）
                int lineNumber = cu.getLineNumber(node.getStartPosition());

                funcCount.incrementAndGet();
                totalComplexity.add(complexity);

                // 根据复杂度创建不同严重程度的Issue
                if (complexity > 15) {
                    issues.add(createIssue(
                            filePath,
                            lineNumber,
                            funcName,
                            complexity,
                            SeverityEnum.MEDIUM.getMessage(),
                            "函数圈复杂度过高，应尽快重构以降低逻辑分支，可考虑拆分函数或使用多态等设计模式优化。"
                    ));
                } else if (complexity > 10) {
                    issues.add(createIssue(
                            filePath,
                            lineNumber,
                            funcName,
                            complexity,
                            SeverityEnum.LOW.getMessage(),
                            "函数圈复杂度偏高，建议优化控制结构，减少条件判断数量，提高代码可读性和可维护性。"
                    ));
                }
                return super.visit(node);
            }
        });

        int totalFuncs = funcCount.get();
        if (totalFuncs == 0) {
            return new ScoreAndIssues(1.0, issues);
        }

        double avgComplexity = totalComplexity.sum() / totalFuncs;
        double score = calculateComplexityScore(avgComplexity);

        return new ScoreAndIssues(score, issues);
    }

    /**
     * 创建Issue对象
     */
    private Issue createIssue(String file, int line, String funcName, int complexity, String severity, String suggestion) {
        Issue issue = new Issue();
        issue.setType("cyclomatic_complexity");
        issue.setSeverity(severity);
        issue.setFile(file);
        issue.setLine(line);
        String problem = "";
        if(complexity > 15){
            problem = "函数圈复杂度过高,";
        } else if (complexity > 10) {
            problem = "函数圈复杂度偏高,";
        }
        issue.setMessage(String.format("函数 '%s': %s 圈复杂度为 %d", funcName, problem, complexity));
        issue.setSuggestion(suggestion);
        // analysisId暂时留空，由上层分析逻辑设置
        return issue;
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
     */
    private double calculateComplexityScore(double avgComplexity) {
        double baseScore = 0.1;
        double increasePerLevel = 0.1;

        double score = baseScore + (avgComplexity * increasePerLevel);
        return 1.0 - Math.min(score, 1.0);
    }
}