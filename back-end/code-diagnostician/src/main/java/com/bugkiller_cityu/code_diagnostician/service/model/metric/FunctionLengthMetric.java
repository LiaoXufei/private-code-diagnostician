package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.service.model.parser.Function;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
import lombok.Data;
import lombok.ToString;
import org.eclipse.jdt.core.dom.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@ToString
public class FunctionLengthMetric extends BaseMetric implements Metric{
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

        ScoreAndIssues scoreAndIssues = analyzeFunctions(astInfo, parseResult);

        return new MetricResult(
                scoreAndIssues.getScore(),
                scoreAndIssues.getIssues(),
                this.getDescription(),
                this.getWeight()
        );
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

            // 检查函数复杂度
            totalComplexity += fn.getComplexity();
            if (fn.getComplexity() > 18) {
                locationInfo = this.getLocationInfo(fn, (CompilationUnit) parseResult.getASTRoot(), astInfo);
                issues.add(String.format("函数 '%s'%s 复杂度严重过高 (%d)，必须简化", fn.getName(), locationInfo, lineCount));
            } else if (fn.getComplexity() > 12) {
                locationInfo = this.getLocationInfo(fn, (CompilationUnit) parseResult.getASTRoot(), astInfo);
                issues.add(String.format("函数 '%s'%s 复杂度过高 (%d)，建议简化", fn.getName(), locationInfo, lineCount));
            }

            // 检查参数数量
            if (fn.getParameters() > 8) {
                locationInfo = this.getLocationInfo(fn, (CompilationUnit) parseResult.getASTRoot(), astInfo);
                issues.add(String.format("函数 '%s'%s 参数极多 (%d 个)，必须使用结构体封装", fn.getName(), locationInfo, lineCount));
            } else if (fn.getParameters() > 6) {
                locationInfo = this.getLocationInfo(fn, (CompilationUnit) parseResult.getASTRoot(), astInfo);
                issues.add(String.format("函数 '%s'%s 参数过多 (%d 个)，建议使用结构体封装", fn.getName(), locationInfo, lineCount));
            }
        }

        double longRatio = (double) longFunctions / (double) totalFunctions;
        double veryLongRatio = (double) veryLongFunctions / (double) totalFunctions;
        double extremeLongRatio = (double) extremeLongFunctions / (double) totalFunctions;

        // 加权计算函数长度评分，对更长的函数给予更高权重
        double lengthScore = longRatio * 0.3 + veryLongRatio * 0.5 + extremeLongRatio * 0.8;
        if (lengthScore > 1.0) {
            lengthScore = 1.0;
        }

        // 精细计算复杂度得分
        double avgComplexity = (double) totalComplexity / (double) totalFunctions;
        double complexityScore = this.calculateComplexityScore(avgComplexity);

        if (parseResult.getASTRoot() != null) {
            ScoreAndIssues stateScoreAndIssues = analyzeStateManagement((CompilationUnit) parseResult.getASTRoot());
            issues.addAll(stateScoreAndIssues.getIssues());

            // 综合得分，函数长度占 50%，复杂度占 20%，状态管理占 30%
            return new ScoreAndIssues(
                    lengthScore * 0.5 + complexityScore * 0.2 + stateScoreAndIssues.getScore() * 0.3,
                    issues
            );

        }

        return new ScoreAndIssues(lengthScore * 0.7 + complexityScore * 0.3, issues);
    }

    private double calculateComplexityScore(double avgComplexity) {
        // 基础分0.4，每点复杂度增加0.1分
        double baseScore = 0.4;
        double increasePerLevel = 0.1;

        double score = baseScore + (avgComplexity * increasePerLevel);

        // 限制范围
        return Math.min(score, 1.0);
    }

    private ScoreAndIssues analyzeStateManagement(CompilationUnit cu) {
        ScoreAndIssues scoreAndIssues = new ScoreAndIssues();
        int globalVars = 0;
        AtomicInteger mutableVars = new AtomicInteger(0);
        AtomicInteger totalVars = new AtomicInteger(0);
        Map<String, StateVarInfo> stateVars = new HashMap<>();

        // 检测全局变量
        for (Object typeObj : cu.types()) {
            if (typeObj instanceof TypeDeclaration typeDecl) {
                for (FieldDeclaration fieldDecl : typeDecl.getFields()) {
                    for (Object fragObj : fieldDecl.fragments()) {
                        if (fragObj instanceof VariableDeclarationFragment frag) {
                            String varName = frag.getName().getIdentifier();

                            // 忽略占位符变量 _
                            if (!varName.equals("_")) {
                                stateVars.put(varName, new StateVarInfo(true, true));
                                globalVars++;
                                mutableVars.incrementAndGet();
                                totalVars.incrementAndGet();

                                scoreAndIssues.getIssues().add(
                                        String.format("全局变量 '%s' 可能导致状态难以追踪", varName)
                                );
                            }
                        }
                    }
                }
            }
        }
        // 检测函数内部的状态变量
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                node.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(VariableDeclarationStatement stmt) {
                        String funcName = node.getName().getIdentifier();

                        // 检测函数参数中带指针类型（在Java中等价：引用或数组类型）
                        if (node.parameters() != null) {
                            for (Object pObj : node.parameters()) {
                                if (pObj instanceof SingleVariableDeclaration param) {
                                    Type paramType = param.getType();
                                    if (paramType.isArrayType() || paramType.isSimpleType() || paramType.isQualifiedType()) {
                                        // 在Go中检测 *T 指针类型；Java中没有指针，可认为数组或对象引用可变
                                        String paramName = param.getName().getIdentifier();
                                        scoreAndIssues.getIssues().add(String.format(
                                                "函数 '%s' 的指针参数 '%s' 可能导致状态被外部修改", funcName, paramName));
                                        mutableVars.incrementAndGet();
                                        totalVars.incrementAndGet();
                                    }
                                }
                            }
                        }

                        // 检测函数体中对状态变量的修改（赋值）
                        if (node.getBody() != null) {
                            node.getBody().accept(new ASTVisitor() {
                                @Override
                                public boolean visit(Assignment assign) {
                                    Expression left = assign.getLeftHandSide();
                                    if (left instanceof SimpleName ident) {
                                        String varName = ident.getIdentifier();
                                        StateVarInfo info = stateVars.get(varName);
                                        if (info != null && !info.isMutable) {
                                            scoreAndIssues.getIssues().add(String.format(
                                                    "在函数 '%s' 中修改了不应该变化的状态变量 '%s'", funcName, varName));
                                        }
                                    }
                                    return super.visit(assign);
                                }
                            });
                        }
                        return super.visit(node);
                    }
                });
                return super.visit(node);
            }
        });
        // 如果没有检测到变量
        if (totalVars.get() == 0) {
            return scoreAndIssues;
        }

        // 计算状态管理得分
        double globalRatio = (double) globalVars / (double) totalVars.get();
        double mutableRatio = (double) mutableVars.get() / (double) totalVars.get();

        // 全局变量比例和可变状态比例越高，得分越差
        double score = globalRatio * 0.6 + mutableRatio * 0.4;
        if (score > 1.0) {
            score = 1.0;
        }

        scoreAndIssues.setScore(score);
        return scoreAndIssues;

    }

    private String getLocationInfo(Function fn, CompilationUnit cu, JavaASTInfo astInfo) {
        Object methodDeclaration = fn.getNode();

        if (cu == null) {
            return fn.getName();
        }

        if (methodDeclaration != null) {
            if (methodDeclaration instanceof MethodDeclaration md) {
                int startLine = cu.getLineNumber(md.getStartPosition());
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

    static class StateVarInfo {
        boolean isGlobal;
        boolean isMutable;

        public StateVarInfo(boolean isGlobal, boolean isMutable) {
            this.isGlobal = isGlobal;
            this.isMutable = isMutable;
        }
    }
}
