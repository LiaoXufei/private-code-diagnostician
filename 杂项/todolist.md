已核实全部关键问题点的实际代码。下面是整理好的 TODO 清单，按优先级和类别分组，每项都标明了**问题位置**和**建议做法**，可直接作为后续开发任务清单。

---

# Code-Diagnostician 问题与改进 TODO 清单

> 标注说明：🔴=严重Bug（影响正确性） 🟠=重要问题（影响可用性/性能） 🟡=一般改进 🟢=优化/重构 📝=文档/规范  
> 更新：第四波工程化与剩余项已大部分落地；仅安全体系（P1-15）、完整测试补齐（P2-10）、Git 提交（P1-20）需人工决策后继续。

---

## 一、评分语义类（最高优先级）

### 🔴 P0-1 评分与等级映射整体颠倒（致命Bug）（已修复）
- **位置**：`ProjectController.java:471-473`、`489`、`597`；`Report.java:55-67`
- **现象**：`Analyzer.MetricResult.score` 注释明确为「0-1，**越高越差**」（`Analyzer.java:93`）；`Report` 的 `QUALITY_LEVELS` 也是「越高越差」（0=洁净→100=灾难）。但 `analyze()` 中：
  ```java
  int overallInt = (int) Math.round(riskScore * 100.0);
  String grade = overallInt >= 80 ? "excellent" : overallInt >= 60 ? "good" : "poor";
  ```
  即**风险高（代码差）→ 分数高 → 评级 "excellent"**，完全反了。维度分 `dimScore = avgRisk*100` 同样颠倒。`GET /analysis` 的 `fileResults.score` 也颠倒。
- **影响**：前端展示的总分、等级、雷达图、问题列表全部语义错误；好代码显示低分"poor"，差代码显示高分"excellent"。
- **建议**：统一为「0-100 质量分，越高越好」。在写入 DB 前做转换：`qualityScore = (1 - riskScore) * 100`；grade 阈值保持 `>=80 excellent / >=60 good / else poor`；维度同理。`Report` 侧将 `codeQualityScore` 也改为质量分并调整 `QUALITY_LEVELS` 语义，或在传入 Report 时再转回风险分（需两端对齐）。

### 🔴 P0-2 PDF/MD 报告与 DB 评级不一致（已修复）
- **位置**：`ProjectController.java:77-83`、`167-173`
- **现象**：导出时 `new AnalysisResult(overall/100.0, ...)` 把 DB 中已颠倒的「质量分」又当回「风险分」传给 Report，导致 Report 输出的等级（按风险解读）与接口返回的 grade（按质量解读）相反。
- **建议**：随 P0-1 一起统一语义，明确 Report 输入的是风险分还是质量分，全链路对齐。

---

## 二、性能与架构类

### 🟠 P1-1 `GET /analysis` 每次请求重新分析全部文件（已修复）
- **位置**：`ProjectController.java:587-609`
- **现象**：每次拉取分析结果都对所有 `ProjectFile` 重新跑 `codeAnalyzer.analyzeFile()`。前端轮询 status 后会请求该接口，导致每次轮询都重复全量静态分析，CPU 飙升、响应慢，且 `fileResults.score` 与已存储的 `dimensions` 评分口径不一致。
- **建议**：分析时把每个文件的 `linesOfCode/issues/complexity/score` 持久化（新增 `FileAnalysisResult` 实体或存入 Issue 的冗余字段）；`GET /analysis` 只读 DB。

### 🟠 P1-2 分析同步阻塞，无真实进度（已修复）
- **位置**：`ProjectController.analyze()`（同步）、`getStatus()`（只返回 0/100）
- **现象**：大项目分析会阻塞 HTTP 请求直至超时；`status` 接口 `progress` 仅 0 或 100；前端 `useWebSocket` 与轮询期望渐变进度，后端无 WebSocket 端点。
- **建议**：改为异步（`@Async` + 线程池或 `TaskExecutor`），分析时写入进度到内存/DB/Redis；新增 `/ws` WebSocket 或 SSE 推送进度；或至少让 status 返回 `processed/total`。

### 🟠 P1-3 列表接口 N+1 查询（已修复）
- **位置**：`ProjectController.java:48-55` → `convertToMap`(`856-865`) 内 `analysisRepository.findById(lastAnalysisId)`
- **现象**：每个项目额外查一次 Analysis，N+1。
- **建议**：用 `@EntityGraph` 或批量 `findAllById` 预加载，或在 Project 中冗余 `lastScore/lastGrade/lastAnalysisAt` 字段。

### 🟡 P1-4 上传目录使用相对路径（已修复）
- **位置**：`ProjectController` 全文 `Paths.get("uploads")`
- **现象**：依赖进程工作目录，从 IDE 或不同目录启动会找不到文件。
- **建议**：`application.yml` 配置 `app.upload-dir`，注入绝对路径。
- **落实**：`AppProperties` + `app.upload-dir`，Controller/AnalysisService/DataInitializer 统一使用。

### 🟡 P1-5 `Files.lines().count()` 统计行数（已修复）
- **位置**：`ProjectController.java:73`、`163`
- **现象**：仅为统计行数完整读取文件；`(int)` 强转大文件溢出。
- **建议**：分析时持久化 `totalLines`，避免重复读盘；用 `long`。
- **落实**：报告导出优先 `sumLinesOfCodeByAnalysisId`，回退读盘时用 `long` + UTF-8。

---

## 三、数据一致性类

### 🔴 P0-3 `DataInitializer` 维度名与实际指标不符（已修复）
- **位置**：`DataInitializer.java:110-118`、`136-144`
- **现象**：种子数据用「代码复杂度/可维护性/代码重复率/安全性/测试覆盖率/代码规范/性能」，实际 Metric 输出「圈复杂度分析/状态管理/注释覆盖率分析/代码重复度/结构分析/错误处理分析/命名规范」。其中**安全性/测试覆盖率/性能指标根本不存在**。一旦真实分析跑出来，维度名与示例完全对不上，前端雷达图维度会突变。
- **建议**：种子数据维度名改为真实 7 个指标名；或补充对应 Metric；并保证示例项目评分语义与 P0-1 一致。

### 🟠 P1-6 种子项目文件无物理文件（已修复）
- **位置**：`DataInitializer.initFiles` 只写 DB 元数据
- **现象**：示例项目在 CodeUpload/Analysis 页面打开会失败（文件不存在），`getFileContent` 返回假内容，无法重新分析。
- **建议**：初始化时写入真实示例源码文件到 `uploads/{id}/`，或提供"示例项目不可分析"标识。
- **落实**：初始化时写入真实 JS 示例到 `app.upload-dir/{projectId}/`，filePath 带 UUID 前缀。

### 🟠 P1-7 `duplicate` 不复制物理文件（已修复）
- **位置**：`ProjectController.java:783-794`
- **现象**：仅复制文件元数据，`filePath.replace(id, newId)` 指向不存在的路径，副本项目无法分析/预览。
- **建议**：复制时同步拷贝磁盘文件到新项目目录。

### 🟡 P1-8 `getFileContent` 文件缺失返回假内容（已修复）
- **位置**：`ProjectController.java:311-313`
- **现象**：返回 `"示例文件内容: " + filename`，前端误以为读到真实代码。
- **建议**：文件不存在应抛 `ResourceNotFoundException`（404）；`Files.readString` 显式指定 `StandardCharsets.UTF_8`（Windows 默认 GBK 会乱码）。

---

## 四、接口契约与前后端对接类

### 🟠 P1-9 前端调用不存在的后端端点（已修复）
- **位置**：`frontend/src/api/analysis.js:17` `reanalyze` → `POST /projects/{id}/reanalyze`
- **现象**：后端无此端点，重新分析功能报 404。
- **建议**：后端补 `/reanalyze`（等价于重新触发 analyze 并保留历史），或前端复用 `/analyze`。

### 🟠 P1-10 错误返回 HTTP 200 + `{error:...}`（已修复）
- **位置**：`ProjectController` 所有错误分支、`GlobalExceptionHandler`
- **现象**：已写好 `GlobalExceptionHandler`/`ResourceNotFoundException`/`ErrorResponse`，但 Controller 全部 `return Map.of("error", ...)`，状态码始终 200，前端难以统一拦截。
- **建议**：Controller 改为抛 `ResourceNotFoundException` 等，由全局处理器返回正确 4xx/5xx；`request.js` 拦截器按状态码处理。

### 🟡 P1-11 `enrichProjectWithLastAnalysis` 是空操作（已修复）
- **位置**：`ProjectController.java:832-840`
- **现象**：方法体只是把 `lastAnalysisId` 赋值给自己，毫无作用。
- **建议**：删除或改为真正注入 lastAnalysis 摘要。
- **落实**：已删除；列表用批量预加载 + `convertProjectToMap`。

### 🟡 P1-12 `CodeController` 仅剩 `/echo` 测试端点（已修复）
- **位置**：`controller/CodeController.java`
- **建议**：移除或纳入正式 API。
- **落实**：已删除 `CodeController`。

### 🟡 P1-13 `CodeService`/`MetricService` 部分为死代码（已修复）
- **位置**：`service/CodeService.java`、`service/MetricService.java`；`Analyzer.java:627` 处 `MetricService` 已被注释
- **建议**：清理未使用代码，明确分析唯一入口为 `CodeAnalyzer`。
- **落实**：已删除死代码，并简化 `CodeAnalyzer` 构造。

---

## 五、健壮性与安全类

### 🟠 P1-14 `analyze()` 静默吞异常（已修复）
- **位置**：`ProjectController.java:465-467`
- **现象**：单文件分析失败仅 `printStackTrace` 后继续；若全部失败，`riskScore=0` → 评级随 P0-1 表现为"满分/最差"，用户无感知。
- **建议**：收集错误写入日志/Issue；全部失败时返回明确错误而非伪结果。
- **落实**：空项目启动即失败；缺失文件计入错误；全部失败走 progress/error + WebSocket。

### 🟡 P1-15 无认证与越权校验（暂缓）
- **现象**：任何人都可访问任意 projectId；`deleteFile` 仅校验归属项目但无用户概念。
- **建议**：引入 Spring Security + 用户体系，至少做项目归属校验。
- **说明**：涉及产品与账号体系设计，需单独迭代，本次未实现。

### 🟡 P1-16 上传缺乏文件类型/数量校验（已修复）
- **位置**：`upload()`
- **现象**：仅依赖全局 10MB/50MB 限制，无扩展名白名单、无单项目文件数上限。
- **建议**：按 `LanguageType` 支持的扩展名过滤；限制单项目文件数与总大小。
- **落实**：扩展名白名单 + 单项目最多 200 文件；非法文件记入 errors。

### 🟡 P1-17 `project.getUpdatedAt().toString()` 潜在 NPE（已修复）
- **位置**：`convertToMap`(`851-852`)
- **现象**：Project 虽有 `@UpdateTimestamp`，但手动 `new Project()` 未保存前调用 `convertToMap` 会 NPE（如 `createProject` 在 `save` 后调用，理论 OK，但 `duplicate`/`update` 路径需复核）。
- **建议**：转换前判空。

---

## 六、工程化与脚本类

### 🟠 P1-18 `run-all.cmd` 路径过时（已修复）
- **位置**：`run-all.cmd:4` `BACKEND_DIR=%ROOT%back-end\code-diagnostician`
- **现象**：后端已从 `back-end/code-diagnostician/` 上提到 `back-end/`，脚本无法启动。
- **建议**：改为 `back-end`。

### 🟠 P1-19 `.gitignore` 路径过时（已修复）
- **位置**：`.gitignore:13-16`
- **现象**：引用 `back-end/code-diagnostician/uploads/`、`/backend/code_quality_analyzer.db`。
- **建议**：更新为 `back-end/uploads/`、`back-end/*.db`。

### 🟠 P1-20 当前重构未提交 Git（需人工确认）
- **现象**：`git status` 显示旧 `back-end/code-diagnostician/**` 全部删除、新 `back-end/**` 全部未跟踪，工作区处于"半迁移"状态，易丢失。
- **建议**：尽快 `git add` 并提交此次目录上提重构。
- **说明**：按仓库规范，仅在用户明确要求时创建 commit。

### 🟡 P1-21 `target/` 构建产物残留（已修复）
- **建议**：`.gitignore` 已忽略 `*.class` 等，但 `target/` 目录建议显式忽略并清理。
- **落实**：根与 `back-end/.gitignore` 均忽略 `target/` / `uploads/`。

---

## 七、文档类

### 📝 P2-1 `杂项/API-Documentation.md` 端口写 9998（已修复）
- **实际**：后端 9999。建议统一为 9999。

### 📝 P2-2 `杂项/README.md` 仍描述 Node.js 后端（已修复）
- **建议**：更新为 Spring Boot + Vue3 当前架构。

### 📝 P2-3 根目录无 README（已修复）
- **建议**：新增根 `README.md`，含技术栈、启动方式（`run-all.cmd`）、目录结构、数据流图。

### 📝 P2-4 `analyzer/readme.md` 记录的已知问题（已修复）
- **建议**：对照本清单逐项核对并更新状态。

---

## 八、功能补全与体验类

### 🟢 P2-5 `CodeOptimize.vue` AI 优化为占位（已修复）
- **建议**：接入 LLM 或基于 Issue 的自动修复建议；或暂时隐藏入口避免误操作。
- **落实**：隐藏报告页入口与路由；页面文件保留待后续接入。

### 🟢 P2-6 Git 导入为占位（已修复）
- **位置**：`importGit()` 仅返回随机 taskId，未真正 clone
- **建议**：用 JGit 实现 clone 到 `uploads/{id}/`，或先移除入口。
- **落实**：接口返回 501 `UnsupportedOperationException`，不再伪造成功。

### 🟢 P2-7 WebSocket 实时进度（已修复）
- **建议**：配合 P1-2 实现 `/ws/progress/{projectId}`。
- **落实**：`/ws/analysis/{projectId}`。

### 🟢 P2-8 报告导出样式与图表（已修复）
- **位置**：`ProjectController` PDF 仅有内联基础 CSS
- **建议**：引入更完整 CSS、可选嵌入 ECharts 截图，提升 PDF 可读性。
- **落实**：增强 PDF HTML/CSS（标题、表格斑马纹、元信息）；图表截图仍可选后续增强。

### 🟢 P2-9 i18n 部分硬编码（已修复）
- **位置**：`ProjectController:576` `"优化%s相关代码"` 等中文硬编码，而 `DefaultTranslator` 支持 zh/en/ru
- **建议**：控制器层也走 Translator，支持多语言报告。
- **落实**：`Report.translateSuggestion` + Translator 键 `suggestion.optimize_dimension`。

### 🟢 P2-10 测试覆盖（暂缓）
- **现状**：仅 metric 子包单元测试
- **建议**：补 Controller 层 MockMvc 测试、ParserFactory、Report 生成测试。
- **说明**：需单独测试迭代，本次未扩写。

---

## 推荐处理顺序

1. **第一波（必须先做，避免越改越乱）**：P1-20 提交重构 → P0-1/P0-2 统一评分语义 → P0-3 种子维度名对齐 ✅（提交除外）
2. **第二波（核心可用性）**：P1-1 持久化文件分析结果 → P1-9 补 `/reanalyze` → P1-10 错误码 → P1-14 异常可见化 ✅
3. **第三波（性能与体验）**：P1-2 异步+进度 → P1-3 N+1 → P1-7/P1-8 文件复制与缺失处理 ✅
4. **第四波（工程化）**：P1-18/P1-19 脚本与 ignore → 文档系列 → 安全与测试（安全/测试/提交仍待）

---

## 剩余待办（需产品/人工决策）

1. **P1-15** Spring Security + 用户/项目归属
2. **P1-20** 用户确认后执行 Git 提交
3. **P2-10** Controller / Report 等测试补齐
