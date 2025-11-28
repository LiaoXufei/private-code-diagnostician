package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.entity.Issue;
import com.bugkiller_cityu.code_diagnostician.service.model.common.SeverityEnum;
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

    // 分析代码结构，返回包含Issue对象的结果
    public MetricResult analyze(ParseResult parseResult){
        JavaASTInfo astInfo = JavaASTExtractor.extractJavaAST(parseResult);
        List<Issue> issues = new ArrayList<>();  // 改为List<Issue>类型
        if (astInfo == null){
            return new MetricResult(
                    0.7,
                    issues,
                    this.getDescription(),
                    this.getWeight()
            );
        }

        // 分析嵌套深度
        CompilationUnit cu = astInfo.getCompilationUnit();
        int[] maxNestingDepthRef = {0};
        List<Issue> nestingIssues = analyzeNestingDepth(cu, maxNestingDepthRef);
        issues.addAll(nestingIssues);

        // 分析循环引用
        List<Issue> circularIssues = analyzeCircularDependencies(cu);
        issues.addAll(circularIssues);

        // 分析导入复杂度
        List<Issue> importIssues = analyzeImportComplexity(cu);
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

    // 分析嵌套深度，返回List<Issue>
    public List<Issue> analyzeNestingDepth(CompilationUnit cu, int[] maxDepthRef) {
        List<Issue> issues = new ArrayList<>();
        String filePath = (String) cu.getProperty("filePath");

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                int depth = calculateNestingDepth(node.getBody());
                if (depth > maxDepthRef[0]) {
                    maxDepthRef[0] = depth;
                }

                String methodName = node.getName().getIdentifier();
                int lineNumber = cu.getLineNumber(node.getStartPosition());  // 获取行号
                Issue issue = new Issue();
                issue.setFile(filePath);
                issue.setLine(lineNumber);
                issue.setType("nesting_depth");  // 问题类型：嵌套深度

                if (depth > 5) {
                    issue.setSeverity(SeverityEnum.HIGH.getMessage());  // 严重程度：高
                    issue.setMessage(String.format("函数 %s 的嵌套深度过高 (%d)", methodName, depth));
                    issue.setSuggestion("考虑重构函数，拆分复杂逻辑以降低嵌套层次");
                    issues.add(issue);
                } else if (depth > 3) {
                    issue.setSeverity(SeverityEnum.MEDIUM.getMessage());  // 严重程度：中等
                    issue.setMessage(String.format("函数 %s 的嵌套深度较高 (%d)", methodName, depth));
                    issue.setSuggestion("建议简化逻辑，减少条件判断或循环的嵌套层级");
                    issues.add(issue);
                }

                return super.visit(node);
            }
        });

        return issues;
    }

    // 计算代码块的最大嵌套深度（保持不变）
    private int calculateNestingDepth(Block block) {
        if (block == null) {
            return 0;
        }

        final int[] maxDepth = {0};
        inspectBlock(block, 1, maxDepth);
        return maxDepth[0];
    }

    // 递归检查代码块嵌套（保持不变）
    private void inspectBlock(Block block, int currentDepth, int[] maxDepth) {
        if (block == null) return;

        if (currentDepth > maxDepth[0]) {
            maxDepth[0] = currentDepth;
        }

        List<?> statements = block.statements();
        for (Object obj : statements) {
            if (!(obj instanceof Statement)) continue;
            Statement stmt = (Statement) obj;

            switch (stmt) {
                case IfStatement ifStmt -> {
                    inspectBlock(extractBlock(ifStmt.getThenStatement()), currentDepth + 1, maxDepth);
                    Statement elseStmt = ifStmt.getElseStatement();
                    if (elseStmt != null) {
                        if (elseStmt instanceof Block) {
                            inspectBlock((Block) elseStmt, currentDepth + 1, maxDepth);
                        } else if (elseStmt instanceof IfStatement) {
                            inspectBlock(extractBlock(((IfStatement) elseStmt).getThenStatement()), currentDepth + 1, maxDepth);
                        }
                    }
                }
                case ForStatement forStatement ->
                        inspectBlock(extractBlock(forStatement.getBody()), currentDepth + 1, maxDepth);
                case EnhancedForStatement enhancedForStatement ->
                        inspectBlock(extractBlock(enhancedForStatement.getBody()), currentDepth + 1, maxDepth);
                case WhileStatement whileStatement ->
                        inspectBlock(extractBlock(whileStatement.getBody()), currentDepth + 1, maxDepth);
                case DoStatement doStatement ->
                        inspectBlock(extractBlock(doStatement.getBody()), currentDepth + 1, maxDepth);
                case SwitchStatement switchStmt -> {
                    for (Object o : switchStmt.statements()) {
                        if (o instanceof Block) {
                            inspectBlock((Block) o, currentDepth + 1, maxDepth);
                        }
                    }
                }
                case SynchronizedStatement synchronizedStatement ->
                        inspectBlock(synchronizedStatement.getBody(), currentDepth + 1, maxDepth);
                case Block block1 -> inspectBlock(block1, currentDepth + 1, maxDepth);
                default -> {
                }
            }
        }
    }

    // 提取代码块（保持不变）
    private Block extractBlock(Statement stmt) {
        if (stmt instanceof Block) {
            return (Block) stmt;
        }
        if (stmt == null) return null;

        AST ast = stmt.getAST();
        Block block = ast.newBlock();
        block.statements().add(ASTNode.copySubtree(ast, stmt));
        return block;
    }

    // 分析循环依赖，返回List<Issue>
    public List<Issue> analyzeCircularDependencies(CompilationUnit cu) {
        List<Issue> issues = new ArrayList<>();
        String filePath = (String) cu.getProperty("filePath");

        PackageDeclaration pkgDecl = cu.getPackage();
        if (pkgDecl == null) {
            return issues;
        }
        String packageName = pkgDecl.getName().getFullyQualifiedName();

        for (Object obj : cu.imports()) {
            if (obj instanceof ImportDeclaration imp) {
                String importName = imp.getName().getFullyQualifiedName();
                if (importName.contains(packageName)) {
                    Issue issue = new Issue();
                    issue.setFile(filePath);
                    issue.setLine(cu.getLineNumber(imp.getStartPosition()));  // 导入语句行号
                    issue.setType("circular_dependency");  // 问题类型：循环依赖
                    issue.setSeverity(SeverityEnum.HIGH.getMessage());  // 严重程度：主要
                    issue.setMessage(String.format("可能存在循环引用: %s 引用了包含自身包 %s", packageName, importName));
                    issue.setSuggestion("检查包依赖关系，避免相互引用或拆分依赖模块");
                    issues.add(issue);
                }
            }
        }

        return issues;
    }

    // 分析导入复杂度，返回List<Issue>
    public List<Issue> analyzeImportComplexity(CompilationUnit cu) {
        List<Issue> issues = new ArrayList<>();
        String filePath = (String) cu.getProperty("filePath");
        int importCount = cu.imports().size();

        if (importCount > 20) {
            Issue issue = new Issue();
            issue.setFile(filePath);
            issue.setLine(1);  // 导入通常在文件顶部，行号设为1
            issue.setType("import_complexity");  // 问题类型：导入复杂度
            issue.setSeverity(SeverityEnum.HIGH.getMessage());
            issue.setMessage(String.format("导入包数量过多 (%d)", importCount));
            issue.setSuggestion("考虑拆分文件或移除不必要的导入，降低模块耦合度");
            issues.add(issue);
        } else if (importCount > 15) {
            Issue issue = new Issue();
            issue.setFile(filePath);
            issue.setLine(1);
            issue.setType("import_complexity");
            issue.setSeverity(SeverityEnum.MEDIUM.getMessage());
            issue.setMessage(String.format("导入包数量较多 (%d)", importCount));
            issue.setSuggestion("检查是否有冗余导入，可使用通配符或拆分到多个文件");
            issues.add(issue);
        }

        return issues;
    }

    // 计算结构得分（保持不变）
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
        double circularScore = 0.0;
        if (circularCount > 0) {
            circularScore = 1.0;
        }

        // 导入问题分数
        double importScore = 0.0;
        if (importIssueCount > 0) {
            importScore += importIssueCount * 0.3;
        }
        if (importScore > 1.0) {
            importScore = 1.0;
        }

        // 综合得分
        return (1.0 - nestingScore) * 0.6 + (1.0 - circularScore) * 0.25 + (1.0 - importScore) * 0.15;
    }
}