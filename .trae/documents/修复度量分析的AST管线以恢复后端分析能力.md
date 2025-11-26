## 问题概述
- 指标模块依赖 Eclipse JDT 的 `CompilationUnit`，但当前解析链未向 `ParseResult` 注入 JDT AST，导致 `JavaASTExtractor` 返回 `null`，多数指标退化为默认分值或空问题。
- 相关代码：
  - `Analyzer.CodeAnalyzer.analyzeFile` 未设置 AST 根（`service/model/analyzer/Analyzer.java:583-591`）。
  - `JavaASTExtractor` 仅接受 `CompilationUnit`（`service/model/metric/JavaASTExtractor.java:26-33`）。
  - `BaseParseResult` 的 `ASTRoot` 字段类型为 `CompilationUnitTree`，与 JDT 类型不匹配（`service/model/parser/BaseParseResult.java:19`）。

## 改动内容
1. 将 `BaseParseResult.ASTRoot` 字段类型改为 `Object`，与 `ParseResult.getASTRoot()` 的签名保持一致，容纳 JDT 的 `CompilationUnit`。
2. 在 `Analyzer.CodeAnalyzer.analyzeFile` 中使用 JDT `ASTParser` 解析源代码，得到 `CompilationUnit`：
   - `parser.setSource(contentCharArray)`、`parser.setKind(ASTParser.K_COMPILATION_UNIT)`、`createAST(null)`。
   - 为 AST 设置文件路径：`cu.setProperty("filePath", filePath)`。
   - 将 AST 注入 `BaseParseResult`：`parseResult.setASTRoot(cu)`。
   - 同步填充导入列表 `imports`（从 `cu.imports()` 提取），保留已有的行数与注释行统计。
3. 保持现有 `Factory` 指标集合与度量流程不变，无需改动各指标类。

## 实现步骤
- 编辑 `service/model/parser/BaseParseResult.java`：
  - 将字段 `ASTRoot` 类型由 `CompilationUnitTree` 改为 `Object`，并更新 getter/setter。
- 编辑 `service/model/analyzer/Analyzer.java` 的 `CodeAnalyzer.analyzeFile`：
  - 引入 `org.eclipse.jdt.core.dom.ASTParser`、`AST`、`CompilationUnit`。
  - 创建并注入 `CompilationUnit` 至 `BaseParseResult.ASTRoot`，设置 `filePath` 属性；从 AST 提取 `imports` 设置到 `AnalysisResult`。
- 编译检查，确保无类型冲突；运行后端并执行 `POST /api/projects/{id}/analyze` 验证分析成功，问题与维度生成完整。

## 验证方式
- 启动后端（端口 `9999`）。
- 通过接口：
  - `GET /api/projects` 查看项目列表与最新分析摘要。
  - `POST /api/projects/{id}/analyze` 触发分析；预期响应包含：`overallScore`、`grade`、`dimensions`、`issues` 数组。
- 核对日志：指标类（如 `CyclomaticComplexityMetric`、`StructureAnalysisMetric`）应生成非空问题；`CommentCoverageMetric` 能从 `cu.getCommentList()` 统计注释。

## 风险与回滚
- 风险：JDT AST 解析对非 Java 文件不可用；但我们仅在 `CodeAnalyzer.analyzeFile` 针对当前项目的 Java 文件调用，且 `Factory` 指标面向 Java。
- 回滚：若出现异常，可暂时移除 AST 注入逻辑，恢复旧行为（但指标退化）。变更集中在两处文件，易于回滚。

## 后续优化（可选）
- 清理 Bean 重复定义：`Analyzer.DefaultAnalyzer` 与 `AnalyzerConfiguration` 不应同时提供相同类型 Bean；保留单一来源并仅设置一次 `@Primary`。
- 统一 `Issue` ID 生成，避免在指标中手动设置 ID（如 `CommentCoverageMetric`）。
- 将文档端口统一为 `9999`（`API-Documentation.md`），避免联调混淆。