package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@ToString
public class ErrorHandlingMetric extends BaseMetric implements Metric {

    public ErrorHandlingMetric() {
        super("错误处理分析", "检测Java代码中异常处理的规范性", 0.15);
    }

    @Override
    public MetricResult analyze(ParseResult parseResult) {
        JavaASTInfo astInfo = JavaASTExtractor.extractJavaAST(parseResult);
        if (astInfo == null || astInfo.getCompilationUnit() == null) {
            return new MetricResult(0.0, new ArrayList<>(), this.getDescription(), this.getWeight());
        }

        CompilationUnit cu = astInfo.getCompilationUnit();
        ScoreAndIssues result = analyzeErrorHandling(cu);

        return new MetricResult(
                result.getScore(),
                result.getIssues(),
                this.getDescription(),
                this.getWeight()
        );
    }

    /**
     * 主体分析函数
     */
    private ScoreAndIssues analyzeErrorHandling(CompilationUnit cu) {
        List<String> issues = new ArrayList<>();

        // 使用 AtomicInteger 来解决匿名类中的变量修改问题
        AtomicInteger totalErrorRelatedMethods = new AtomicInteger(0);
        AtomicInteger ignoredErrors = new AtomicInteger(0);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                // 检查是否 throws 异常
                if (!node.thrownExceptionTypes().isEmpty()) {
                    totalErrorRelatedMethods.incrementAndGet();
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(TryStatement node) {
                // 检查是否存在空的 catch 块或未使用异常变量
                for (Object obj : node.catchClauses()) {
                    CatchClause cc = (CatchClause) obj;
                    String varName = cc.getException().getName().getIdentifier();
                    Block body = cc.getBody();
                    boolean used = false;
                    for (Object s : body.statements()) {
                        if (s.toString().contains(varName)) {
                            used = true;
                            break;
                        }
                    }
                    if (!used) {
                        int line = cu.getLineNumber(cc.getStartPosition());
                        issues.add(String.format("第 %d 行: 捕获异常但未处理 (变量 '%s' 未被使用)", line, varName));
                        ignoredErrors.incrementAndGet();
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodInvocation node) {
                String name = node.getName().getIdentifier();

                // 如果调用的是高风险 API 且不在 try 块中
                if (isRiskyCall(name) && !isInsideTry(node)) {
                    int line = cu.getLineNumber(node.getStartPosition());
                    issues.add(String.format("第 %d 行: 调用可能抛出异常的方法 '%s' 但未使用 try-catch", line, name));
                    ignoredErrors.incrementAndGet();
                }
                return super.visit(node);
            }

            private boolean isInsideTry(ASTNode node) {
                ASTNode parent = node.getParent();
                while (parent != null) {
                    if (parent instanceof TryStatement) {
                        return true;
                    }
                    parent = parent.getParent();
                }
                return false;
            }

            private boolean isRiskyCall(String name) {
                String[] risky = {
                        "open", "read", "write", "delete", "connect", "execute", "query",
                        "load", "save", "rename", "close", "send", "receive", "parse"
                };
                for (String s : risky) {
                    if (name.toLowerCase().contains(s)) return true;
                }
                return false;
            }
        });

        // 如果没有检测到与异常相关的函数，不评分
        if (totalErrorRelatedMethods.get() == 0) {
            return new ScoreAndIssues(0.0, issues);
        }

        double score = calculateScore(ignoredErrors.get(), totalErrorRelatedMethods.get());
        return new ScoreAndIssues(score, issues);
    }

    /**
     * 与 Go 版本一致的计分逻辑：
     * baseScore = 0.4
     * score = baseScore + (ignoredErrors / totalErrorReturns) * 10
     * capped to 1.0
     */
    private double calculateScore(int ignoredErrors, int totalErrorReturns) {
        if (totalErrorReturns == 0) return 0.4;

        double ignoredRatio = (double) ignoredErrors / (double) totalErrorReturns;
        double baseScore = 0.4;
        double score = baseScore + ignoredRatio * 10.0;

        return Math.min(score, 1.0);
    }
}
