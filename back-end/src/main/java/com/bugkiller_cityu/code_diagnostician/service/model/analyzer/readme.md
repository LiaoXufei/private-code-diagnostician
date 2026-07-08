
关系与调用:
- DefaultAnalyzer 是对外注入的主入口，基于 AnalyzerInterface 规范；在分析过程中调用 CommonUtils.findSourceFiles(...) 获取文件列表（ back-end/.../Analyzer.java:366 ），逐个委托给 CodeAnalyzer.analyzeFile(...) 执行指标分析（ back-end/.../Analyzer.java:439 ），最终聚合为 AnalysisResult 并计算总体评分（ back-end/.../Analyzer.java:539 ）。
- AnalyzerConfiguration 将 CodeAnalyzer/DefaultTranslator 与指标 Factory 组装为可用的 Bean，保证 DefaultAnalyzer 的依赖完整（ back-end/.../AnalyzerConfiguration.java:33 、 back-end/.../AnalyzerConfiguration.java:45 、 back-end/.../AnalyzerConfiguration.java:59 ）。
- DefaultTranslator 与 TranslatorFactory 负责国际化文案，分析与报告阶段的字符串均通过翻译器生成（例如 report.Report 中的 translator 使用， back-end/.../report/Report.java:25 ）。



核心问题

- 进度输出的 ANSI 控制符不兼容
  
  - 位置： back-end/.../analyzer/Analyzer.java:426 、 back-end/.../analyzer/Analyzer.java:432 、 back-end/.../analyzer/Analyzer.java:435 、 back-end/.../analyzer/Analyzer.java:458
  - 问题：使用 \033[K 与移动光标的 ANSI 转义在 Windows 控制台不可用，且在 Web 后端环境下不应直接向 stdout 打印。
  - 方案：
    - 改为使用日志框架（如 SLF4J ）按级别记录进度。
    - 在非 CLI 环境下改用事件推送（WebSocket）或回调接口上报进度。
    - 提供 silent 模式默认开启，输出仅限日志且不含 ANSI。


- 路径处理只按 / 分隔
  
  - 位置： back-end/.../analyzer/Analyzer.java:545 （ shortenPath ）
  - 问题：仅按 / 分割在 Windows \ 路径下失效，显示“正在处理”时的路径不正确（ back-end/.../analyzer/Analyzer.java:432 ）。
  - 方案：
    - 使用 Path / Paths API 或正则 [/\\] 统一处理分隔符。
    - shortenPath 通过 Path#getNameCount() 和 subpath(...) 构造尾部片段。


- Glob 匹配与分隔符兼容问题
  
  - 位置： back-end/.../analyzer/CommonUtils.java:58 、 back-end/.../analyzer/Analyzer.java:279
  - 问题：
    - PathMatcher("glob:" + p) 对绝对路径的匹配与传入的相对模式（如 */node_modules/* ）可能不一致；Windows 下 \ 与 glob / 不一致。
    - 默认排除模式 */vendor/* 、 */node_modules/* 对绝对路径匹配不可靠。
  - 方案：
    - 统一将待匹配路径规范化为 POSIX 风格（替换 \ 为 / ）再进行匹配。
    - 将默认排除改为 **/node_modules/** 、 **/.git/** 、 **/vendor/** 。
    - 可考虑按 start 根目录计算相对路径后再匹配。


- AST 类型不一致且未填充
  
  - 位置：
    - back-end/.../parser/BaseParseResult.java:12 将 ASTRoot 声明为 com.sun.source.tree.CompilationUnitTree
    - back-end/.../metric/JavaASTExtractor.java:27 期待的是 org.eclipse.jdt.core.dom.CompilationUnit
    - back-end/.../analyzer/Analyzer.java:582 构造 BaseParseResult 时 ASTRoot 置为 null
  - 问题：解析结果中的 AST 类型与指标模块使用的 JDT AST 不一致，导致 AST 相关指标（函数长度、圈复杂度、结构分析等）无法工作或退化为 0 分。
  - 方案：
    - 将 BaseParseResult 的 ASTRoot 字段类型改为 Object （接口 ParseResult.getASTRoot() 本身就是 Object ），以便存放 JDT CompilationUnit 。
    - 在 CodeAnalyzer.analyzeFile 中用 JDT ASTParser 生成 CompilationUnit ，同时填充 functions 列表（ Function 的起止行、参数数、复杂度、AST 节点）。
    - 可利用已有测试 BasicMetricTest.parse(...) 的 JDT 解析方式作为实现参考（ back-end/.../metric/BasicMetricTest.java:8 ）。


- 指标聚合权重处理不正确

  - 位置： back-end/.../analyzer/Analyzer.java:518 – back-end/.../analyzer/Analyzer.java:532
  - 问题：聚合各文件指标为平均值时， totalWeight 被重复赋值为“最后一个指标的权重”，并未对权重做合理聚合。
  - 方案：
    - 明确权重语义：若各文件同一指标权重相同，可直接保留该权重；若权重可能不同，应计算平均或总和。
    - 推荐：只在展示层保留权重（用于报告说明），聚合分数统一使用“平均值”或“加权平均（权重来源一致时）”。


- 语言类型硬编码为 Java
  
  - 位置： back-end/.../analyzer/Analyzer.java:587
  - 问题： ParseResult.language 被固定为 LanguageType.JAVA ，与 CommonUtils 支持的多语言扩展不一致。
  - 方案：
    - 根据文件扩展名推断语言类型（ .java/.go/.js/... ），或接收外部配置。
    - 按语言选择不同的指标集（当前 Factory 为 Java 指标，可后续扩展）。


- 文件读取重复与性能问题
  
  - 位置： back-end/.../analyzer/Analyzer.java:573 （读取字节）、 back-end/.../analyzer/Analyzer.java:574 （统计行数）、 back-end/.../analyzer/Analyzer.java:577 （读所有行统计注释）
  - 问题：同一文件读取三次，会影响性能；大文件下内存占用与 IO 压力增加。
  - 方案：
    - 单次流式读取：迭代行时同时计算总行数与注释行数并构建内容缓冲（必要时）。
    - 对于内容字节缓存需要谨慎：仅在指标需要时再读取。


- 进度显示中文未走翻译器
  
  - 位置： back-end/.../analyzer/Analyzer.java:432 文本“正在处理: %s”
  - 问题：硬编码中文未通过 Translator ，无法随语言切换。
  - 方案：
    - 将该文本替换为翻译键（如 analyzer.processing_file ），在 DefaultTranslator 中提供对应语言条目。


- 国际化键缺失/不一致
  
  - 位置： back-end/.../analyzer/Analyzer.java:350 、 back-end/.../analyzer/Analyzer.java:377 、 back-end/.../analyzer/Analyzer.java:274 、 back-end/.../analyzer/Analyzer.java:369
  - 问题：代码中使用 analyzer.searching_files 、 analyzer.files_found 、 error.path_not_accessible 、 error.source_files_not_found 、 error.file_analysis_failed 等键，而 DefaultTranslator 未定义这些键（ back-end/.../analyzer/DefaultTranslator.java:103 ）。
  - 方案：
    - 在 DefaultTranslator 添加这些键的中英文翻译，统一命名规范（ module.section.key ）。
    - 通过测试检查翻译键存在性，避免回退到原始键。


- Spring Bean 重复与注入歧义
  
  - 位置： back-end/.../analyzer/DefaultTranslator.java:15 （ @Component ）、 back-end/.../analyzer/AnalyzerConfiguration.java:58 （ @Bean defaultTranslator() ）
  - 问题：同类型 Translator 同时由 @Component 与 @Bean 注册，可能导致 NoUniqueBeanDefinitionException 。
  - 方案：
    - 二选一：保留 @Component ，移除 @Bean defaultTranslator() ；或保留 @Bean ，去掉 @Component 。
    - 或者为其一加 @Primary ，明确首选注入。


- 符号链接与递归遍历风险
  
  - 位置： back-end/.../analyzer/CommonUtils.java:32 （ FileVisitOption.FOLLOW_LINKS ）
  - 问题：追随符号链接在存在循环链接时可能导致无限遍历。
  - 方案：
    - 移除 FOLLOW_LINKS 或维护“已访问”集合并限制最大深度。
    - 提供配置开关以明确是否处理符号链接。

    
- 进度延迟影响吞吐
  
  - 位置： back-end/.../analyzer/Analyzer.java:449 （ Thread.sleep(20) ）
  - 问题：为“好看”的进度条引入延迟，不适合批量分析或服务端环境。
  - 方案：
    - 移除延迟或仅在文件数很少且明确选择“演示模式”时启用。