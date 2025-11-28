package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.entity.Issue;
import com.bugkiller_cityu.code_diagnostician.service.model.common.SeverityEnum;
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
        // 获取当前分析的文件路径
        String filePath = (String) cu.getProperty("filePath");
        ScoreAndIssues result = analyzeErrorHandling(cu, filePath);

        return new MetricResult(
                result.getScore(),
                result.getIssues(),
                this.getDescription(),
                this.getWeight()
        );
    }

    /**
     * 主体分析函数，增加文件路径参数
     */
    private ScoreAndIssues analyzeErrorHandling(CompilationUnit cu, String filePath) {
        List<Issue> issues = new ArrayList<>();  // 改为List<Issue>类型

        AtomicInteger totalErrorRelatedMethods = new AtomicInteger(0);
        AtomicInteger ignoredErrors = new AtomicInteger(0);

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                if (!node.thrownExceptionTypes().isEmpty()) {
                    totalErrorRelatedMethods.incrementAndGet();
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(TryStatement node) {
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
                        // 创建Issue对象并设置属性
                        Issue issue = new Issue();
                        issue.setType("error_handling");
                        issue.setSeverity(SeverityEnum.MEDIUM.getMessage());  // 未处理异常属于较严重问题
                        issue.setFile(filePath);
                        issue.setLine(line);
                        issue.setMessage(String.format("捕获异常但未处理 (变量 '%s' 未被使用)", varName));
                        issue.setSuggestion("应处理捕获的异常，如记录日志、修复错误或根据业务逻辑重新抛出");
                        issues.add(issue);
                        ignoredErrors.incrementAndGet();
                    }
                }
                return super.visit(node);
            }

            @Override
            public boolean visit(MethodInvocation node) {
                String name = node.getName().getIdentifier();

                if (isRiskyCall(name) && !isInsideTry(node)) {
                    int line = cu.getLineNumber(node.getStartPosition());
                    // 创建Issue对象并设置属性
                    Issue issue = new Issue();
                    issue.setType("error_handling");
                    issue.setSeverity(SeverityEnum.MEDIUM.getMessage());  // 未捕获风险方法异常属于较严重问题
                    issue.setFile(filePath);
                    issue.setLine(line);
                    issue.setMessage(String.format("调用可能抛出异常的方法 '%s' 但未使用 try-catch", name));
                    issue.setSuggestion("应对可能抛出异常的方法调用添加try-catch块，妥善处理异常情况");
                    issues.add(issue);
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

//        if (totalErrorRelatedMethods.get() == 0) {
//            return new ScoreAndIssues(0.0, issues);
//        }

        double score = calculateScore(ignoredErrors.get(), totalErrorRelatedMethods.get());
        return new ScoreAndIssues(score, issues);
    }

    private double calculateScore(int ignoredErrors, int totalErrorReturns) {
        if (totalErrorReturns == 0) return 1.0;

        double ignoredRatio = (double) ignoredErrors / (double) totalErrorReturns;
        double score = ignoredRatio;

        return 1.0 - Math.min(score, 1.0);
    }
}