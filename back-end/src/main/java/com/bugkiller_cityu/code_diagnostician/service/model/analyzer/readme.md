# Analyzer 模块说明

## 关系与调用

- `DefaultAnalyzer` 是对外注入的主入口；分析时调用 `CommonUtils.findSourceFiles` 获取文件列表，再委托 `CodeAnalyzer.analyzeFile` 执行指标分析，最后聚合为 `AnalysisResult`。
- `AnalyzerConfiguration` 组装 `CodeAnalyzer` / `Translator` / `Factory` Bean。
- HTTP 分析入口为 `AnalysisService`（异步 + 进度 + WebSocket），不再阻塞 Controller。
- `DefaultTranslator` 负责国际化；报告与 API 建议文案通过 Translator 生成。

## 与项目 todolist 的对照（已处理部分）

| 主题 | 状态 |
|------|------|
| Web 分析进度 | 已通过 `AnalysisProgressTracker` + `/ws/analysis/{projectId}` 实现，不再依赖 CLI ANSI 进度 |
| 评分语义 | DB 存质量分（越高越好）；Report 导出时转回风险分 |
| 死代码 | 已移除未使用的 `CodeController` / `CodeService` / `MetricService` |
| Translator Bean | 仅保留 `@Component` DefaultTranslator，配置类不再重复注册 |

## 仍可能存在的底层问题（CLI / 指标层）

以下问题主要影响 CLI 批量分析路径或指标实现细节，与当前 Web 主流程部分解耦：

- CLI 进度仍可能向 stdout 打印 ANSI（Windows 控制台兼容性）
- `shortenPath` / Glob 在 Windows 路径分隔符下的兼容性
- AST 类型与部分指标的填充完整性
- 指标聚合权重语义
- CLI 路径中的 `Thread.sleep` 演示延迟

Web 主流程请以 `AnalysisService` + `CodeAnalyzer.analyzeFile` 为准。
