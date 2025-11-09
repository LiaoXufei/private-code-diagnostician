package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StructureAnalysisMetric extends BaseMetric implements Metric{
    public StructureAnalysisMetric(){
        super(
                "结构分析",
                "分析代码结构，检测乱嵌套乱引用",
                0.15
        );
    }
    // Analyze 分析代码结构
    public MetricResult analyze(ParseResult parseResult){
        JavaASTInfo astInfo = JavaASTExtractor.extractJavaAST(parseResult);
        List<String> issues = new ArrayList<>();
        if (astInfo == null){
            return new MetricResult(
                    0.3,
                    issues,
                    this.getDescription(),
                    this.getWeight()
            );
        }

        // 分析嵌套深度
        CompilationUnit cu = astInfo.getCompilationUnit();
        int[] maxNestingDepthRef = {0};
        List<String> nestingIssues = analyzeNestingDepth(cu, maxNestingDepthRef);
        issues.addAll(nestingIssues);

        // 分析循环引用
        List<String> circularIssues = analyzeCircularDependencies(cu);
        issues.addAll(circularIssues);

        // 分析导入复杂度
        List<String> importIssues = analyzeImportComplexity(cu);
        issues.addAll(importIssues);

        // 计算结构得分
        double score = calculateScore(
                maxNestingDepthRef[0],
                circularIssues.size(),
                importIssues.size()
        );

        return new MetricResult(
                score,
                issues,
                this.getDescription(),
                this.getWeight()
        );
    }

    // analyzeNestingDepth：分析代码嵌套深度
    public List<String> analyzeNestingDepth(CompilationUnit cu, int[] maxDepthRef) {
        List<String> issues = new ArrayList<>();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                int depth = calculateNestingDepth(node.getBody());
                if (depth > maxDepthRef[0]) {
                    maxDepthRef[0] = depth;
                }

                String methodName = node.getName().getIdentifier();
                if (depth > 5) {
                    issues.add(String.format("函数 %s 的循环复杂度过高 (%d)，考虑重构", methodName, depth));
                } else if (depth > 3) {
                    issues.add(String.format("函数 %s 的循环复杂度较高 (%d)，建议简化", methodName, depth));
                }

                return super.visit(node);
            }
        });

        return issues;
    }

    // calculateNestingDepth：计算代码块的最大嵌套深度
    private int calculateNestingDepth(Block block) {
        if (block == null) {
            return 0;
        }

        final int[] maxDepth = {0};

        // 递归分析嵌套结构
        inspectBlock(block, 1, maxDepth);

        return maxDepth[0];
    }

    private void inspectBlock(Block block, int currentDepth, int[] maxDepth) {
        if (block == null) return;

        if (currentDepth > maxDepth[0]) {
            maxDepth[0] = currentDepth;
        }

        List<?> statements = block.statements();
        for (Object obj : statements) {
            if (!(obj instanceof Statement)) continue;
            Statement stmt = (Statement) obj;

            if (stmt instanceof IfStatement) {
                IfStatement ifStmt = (IfStatement) stmt;
                inspectBlock(extractBlock(ifStmt.getThenStatement()), currentDepth + 1, maxDepth);
                Statement elseStmt = ifStmt.getElseStatement();
                if (elseStmt != null) {
                    if (elseStmt instanceof Block) {
                        inspectBlock((Block) elseStmt, currentDepth + 1, maxDepth);
                    } else if (elseStmt instanceof IfStatement) {
                        // 递归处理 else-if
                        inspectBlock(extractBlock(((IfStatement) elseStmt).getThenStatement()), currentDepth + 1, maxDepth);
                    }
                }
            } else if (stmt instanceof ForStatement) {
                inspectBlock(extractBlock(((ForStatement) stmt).getBody()), currentDepth + 1, maxDepth);
            } else if (stmt instanceof EnhancedForStatement) {
                inspectBlock(extractBlock(((EnhancedForStatement) stmt).getBody()), currentDepth + 1, maxDepth);
            } else if (stmt instanceof WhileStatement) {
                inspectBlock(extractBlock(((WhileStatement) stmt).getBody()), currentDepth + 1, maxDepth);
            } else if (stmt instanceof DoStatement) {
                inspectBlock(extractBlock(((DoStatement) stmt).getBody()), currentDepth + 1, maxDepth);
            } else if (stmt instanceof SwitchStatement) {
                SwitchStatement switchStmt = (SwitchStatement) stmt;
                for (Object o : switchStmt.statements()) {
                    if (o instanceof Block) {
                        inspectBlock((Block) o, currentDepth + 1, maxDepth);
                    }
                }
            } else if (stmt instanceof SynchronizedStatement) {
                inspectBlock(((SynchronizedStatement) stmt).getBody(), currentDepth + 1, maxDepth);
            } else if (stmt instanceof Block) {
                inspectBlock((Block) stmt, currentDepth + 1, maxDepth);
            }
        }
    }

    // 提取 block（防止 body 是单语句而非 Block）
    private Block extractBlock(Statement stmt) {
        if (stmt instanceof Block) {
            return (Block) stmt;
        }
        if (stmt == null) return null;

        // 创建一个虚拟 block 包裹单语句
        AST ast = stmt.getAST();
        Block block = ast.newBlock();
        block.statements().add(ASTNode.copySubtree(ast, stmt));
        return block;
    }

    // analyzeCircularDependencies 分析循环依赖
    public List<String> analyzeCircularDependencies(CompilationUnit cu) {
        List<String> issues = new ArrayList<>();

        // 获取当前包名（相当于 Go 的 file.Name.Name）
        PackageDeclaration pkgDecl = cu.getPackage();
        if (pkgDecl == null) {
            return issues; // 无包声明则直接返回
        }
        String packageName = pkgDecl.getName().getFullyQualifiedName();

        // 遍历所有 import 声明
        for (Object obj : cu.imports()) {
            if (obj instanceof ImportDeclaration imp) {
                String importName = imp.getName().getFullyQualifiedName();

                // 判断是否包含包名（类似 strings.Contains）
                if (importName.contains(packageName)) {
                    issues.add(String.format(
                            "可能存在循环引用: %s 引用了包含自身名称的包 %s",
                            packageName, importName
                    ));
                }
            }
        }

        return issues;
    }

    // analyzeImportComplexity 分析导入复杂度
    public List<String> analyzeImportComplexity(CompilationUnit cu) {
        List<String> issues = new ArrayList<>();

        int importCount = cu.imports().size();

        if (importCount > 20) {
            issues.add(String.format(
                    "导入包数量过多 (%d)，考虑拆分文件或重构", importCount
            ));
        } else if (importCount > 15) {
            issues.add(String.format(
                    "导入包数量较多 (%d)，建议检查是否需要全部导入", importCount
            ));
        }

        return issues;
    }

    /**
     * 计算结构得分
     *
     * @param nestingDepth     嵌套深度
     * @param circularCount    循环依赖数量
     * @param importIssueCount 导入问题数量
     * @return 综合结构得分（0.0-1.0）
     */
    public double calculateScore(int nestingDepth, int circularCount, int importIssueCount) {
        // 嵌套深度分数
        double nestingScore = 0.4;
        if (nestingDepth > 1) {
            nestingScore += (nestingDepth - 1) * 0.15;
        }
        if (nestingScore > 1.0) {
            nestingScore = 1.0;
        }

        // 循环依赖分数
        double circularScore = 0.5;
        if (circularCount > 0) {
            circularScore = 1.0;
        }

        // 导入问题分数
        double importScore = 0.4;
        if (importIssueCount > 0) {
            importScore += importIssueCount * 0.3;
        }
        if (importScore > 1.0) {
            importScore = 1.0;
        }

        // 综合得分
        return nestingScore * 0.6 + circularScore * 0.25 + importScore * 0.15;
    }
}
