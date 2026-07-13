package com.diagnostician.service.model.analyzer;

import com.diagnostician.entity.Issue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import lombok.Getter;
import com.diagnostician.service.model.metric.Factory;
import org.eclipse.jdt.core.dom.*;

/**
 * Analyzer - 代码分析器抽象基类
 *
 * 架构说明：
 * - 这是一个抽象基类，定义了代码分析的核心流程
 * - 文件查找 → 代码解析 → 指标分析 → 结果聚合 → 评分计算
 * - 不应该被直接实例化，请使用DefaultAnalyzer实现类
 * - 在Spring环境中，通过@Autowired注入Analyzer类型即可获得DefaultAnalyzer实例
 */
public abstract class Analyzer {

    // 核心组件字段 - 由子类通过构造函数注入
    protected CodeAnalyzer codeAnalyzer;
    protected Translator translator;
    protected boolean silent = false;

    // 分析结果
    @Data
    @NoArgsConstructor
    public static class AnalysisResult {
        // 代码质量得分
        private double codeQualityScore;

        // 各项指标结果
        private Map<String, MetricResult> metrics = new HashMap<>();

        // 分析的文件结果
        private List<FileAnalysisResult> filesAnalyzed = new ArrayList<>();

        // 总文件数
        private int totalFiles;

        // 总代码行数
        private int totalLines;

        // 带参数的构造函数（保留，因为需要null检查）
        public AnalysisResult(double codeQualityScore,
                              Map<String, MetricResult> metrics,
                              List<FileAnalysisResult> filesAnalyzed,
                              int totalFiles,
                              int totalLines) {
            this.codeQualityScore = codeQualityScore;
            this.metrics = metrics != null ? metrics : new HashMap<>();
            this.filesAnalyzed = filesAnalyzed != null ? filesAnalyzed : new ArrayList<>();
            this.totalFiles = totalFiles;
            this.totalLines = totalLines;
        }

        // 添加指标结果
        public void addMetric(String name, MetricResult metric) {
            this.metrics.put(name, metric);
        }

        // 添加文件分析结果
        public void addFileAnalysisResult(FileAnalysisResult fileResult) {
            this.filesAnalyzed.add(fileResult);
        }
    }

    // MetricResult 类
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricResult {
        // 指标名称
        private String name;

        // 得分(0-1，越高越差)
        private double score;

        // 描述
        private String description;

        // 权重
        private double weight;
    }

    // FileAnalysisResult 类
    @Data
    @NoArgsConstructor
    public static class FileAnalysisResult {
        // 文件路径
        private String filePath;

        // 文件得分
        private double fileScore;

        // 问题列表
        private List<Issue> issues = new ArrayList<>();

        // 带参数的构造函数（保留，因为需要null检查）
        public FileAnalysisResult(String filePath, double fileScore, List<Issue> issues) {
            this.filePath = filePath;
            this.fileScore = fileScore;
            this.issues = issues != null ? issues : new ArrayList<>();
        }

        // 添加问题
        public void addIssue(Issue issue) {
            this.issues.add(issue);
        }
    }

    /**
     * DefaultAnalyzer - 默认分析器实现类
     *
     * Spring Bean说明：
     * - @Component: 标记为Spring管理的Bean
     * - @Primary: 当有多个Analyzer类型的Bean时，优先使用这个
     * - 实现AnayzerInterface接口，提供完整的分析功能
     * - 用户交互层：处理分析请求，协调CodeAnalyzer进行实际分析
     *
     * 使用示例：
     * @Autowired
     * private Analyzer analyzer;  // Spring会自动注入DefaultAnalyzer实例
     */
    public static class DefaultAnalyzer extends Analyzer implements AnalyzerInterface {

        /**
         * 通过构造函数注入依赖
         * Spring框架会自动调用这个构造函数，并注入所需的Bean
         *
         * @param codeAnalyzer 代码分析引擎
         * @param translator 翻译器
         */
        @Autowired
        public DefaultAnalyzer(CodeAnalyzer codeAnalyzer, Translator translator) {
            // 初始化父类的字段
            super.codeAnalyzer = codeAnalyzer;
            super.translator = translator;
            super.silent = false;
        }

        /**
         * 设置分析器使用的语言
         * 实现AnayzerInterface接口
         * @param lang 语言类型
         */
        @Override
        public void setLanguage(Language lang) {
            if (translator instanceof DefaultTranslator) {
                ((DefaultTranslator) translator).setLanguage(lang);
            }
        }

        /**
         * 设置静默模式
         * 实现AnayzerInterface接口
         * @param silent 是否静默（不输出进度信息）
         */
        @Override
        public void setSilent(boolean silent) {
            this.silent = silent;
        }

        // 移除 setter，或保留用于配置
    }

    // 分析异常
    public static class AnalysisException extends Exception {
        public AnalysisException(String message) {
            super(message);
        }

        public AnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // 表示支持的语言
    public enum Language {
        // 简体中文
        ZH_CN("zh-CN"),

        // 英文（美国）
        EN_US("en-US"),

        // 俄语
        RU_RU("ru-RU");

        @Getter
        private final String code;

        Language(String code) {
            this.code = code;
        }
    }

    // 翻译器接口
    public interface Translator {
        // 翻译指定的键
        // @param key 翻译键
        // @param args 参数
        // @return 翻译后的字符串
        String translate(String key, Object... args);

        // 获取当前语言
        // @return 语言类型
        Language getLanguage();
    }

    /////////////////////////////////////

    /**
     * NewAnalyzer - 创建新的代码分析器（静态工厂方法）
     *
     * @deprecated 不推荐使用此方法。在Spring环境中，请使用依赖注入：
     * <pre>
     * @Autowired
     * private Analyzer analyzer;  // Spring会自动注入DefaultAnalyzer实例
     * </pre>
     *
     * 这个静态工厂方法绕过了Spring的依赖注入机制，会导致：
     * - 创建的对象不受Spring管理
     * - 无法享受Spring的AOP、事务等功能
     * - 难以进行单元测试（无法Mock依赖）
     *
     * @return 新的分析器实例（不推荐使用）
     */
    @Deprecated
    public static Analyzer newAnalyzer() {
        // 这个方法保留仅为向后兼容，不应在新代码中使用
        Factory factory = new Factory();
        Translator translator = new DefaultTranslator(Language.EN_US);
        return new DefaultAnalyzer(
                new CodeAnalyzer(translator, factory),
                translator
        );
    }

    // SetLanguage 设置分析器使用的语言
    public void setLanguage(Language lang) {
        if (this.translator instanceof DefaultTranslator) {
            ((DefaultTranslator) this.translator).setLanguage(lang);
        }
        if (this.codeAnalyzer != null) {
            this.codeAnalyzer.setTranslator(this.translator);
        }
    }

    // SetSilent 设置静默模式
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    // Analyze 分析指定路径的代码
    public AnalysisResult analyze(String path) throws AnalysisException {
        Path pathObj = Paths.get(path);
        if (!Files.exists(pathObj)) {
            throw new AnalysisException(
                    translator.translate("error.path_not_accessible", path)
            );
        }

        if (Files.isDirectory(pathObj)) {
            return analyzeWithExcludes(path, null,
                    List.of("**/vendor/**", "**/node_modules/**", "**/.git/**"));
        }

        return analyzeFile(path);
    }

    // AnalyzeFile 分析单个文件
    public AnalysisResult analyzeFile(String filePath) throws AnalysisException {
        // 使用内部的CodeAnalyzer分析文件
        com.diagnostician.service.model.metric.AnalysisResult fileResult;
        try {
            fileResult = codeAnalyzer.analyzeFile(filePath);
        } catch (AnalysisException e) {
            throw e;
        }

        // 转换为AnalysisResult
        AnalysisResult result = new AnalysisResult();
        result.setCodeQualityScore(fileResult.getOverallScore());
        result.setMetrics(new HashMap<>());
        result.setFilesAnalyzed(new ArrayList<>());
        result.setTotalFiles(1);
        result.setTotalLines(fileResult.getTotalLines());

        // 添加指标结果
        for (Map.Entry<String, com.diagnostician.service.model.metric.MetricResult> entry : fileResult.getMetricResults().entrySet()) {
            String name = entry.getKey();
            com.diagnostician.service.model.metric.MetricResult metricResult = entry.getValue();

            MetricResult resultMetric = new MetricResult();
            resultMetric.setName(name);
            resultMetric.setScore(metricResult.getScore());
            resultMetric.setDescription(metricResult.getDescription());
            resultMetric.setWeight(metricResult.getWeight());
            result.getMetrics().put(name, resultMetric);
        }

        // 添加文件分析结果
        FileAnalysisResult fileAnalysisResult = new FileAnalysisResult();
        fileAnalysisResult.setFilePath(filePath);
        fileAnalysisResult.setFileScore(fileResult.getOverallScore());
        fileAnalysisResult.setIssues(fileResult.getIssues());
        result.getFilesAnalyzed().add(fileAnalysisResult);

        return result;
    }

    // AnalyzeWithExcludes 使用指定的包含/排除模式分析目录
    public AnalysisResult analyzeWithExcludes(String path,
                                              List<String> includePatterns,
                                              List<String> excludePatterns) throws AnalysisException {
        Path pathObj = Paths.get(path);
        if (!Files.exists(pathObj)) {
            throw new AnalysisException(
                    translator.translate("error.path_not_accessible", path)
                    //无法访问路径
            );
        }

        if (!Files.isDirectory(pathObj)) {
            return analyzeFile(path);
        }

        int[] lastFoundCount = {0}; // 使用数组来模拟 Go 的变量捕获

        // 定义进度回调函数
        Consumer<Integer> progressCallback = found -> {
            if (!silent && found > lastFoundCount[0]) {
                lastFoundCount[0] = found;
                // 更新搜索进度
                System.out.printf("\r🔍 %s %d",
                        translator.translate("analyzer.searching_files"), found);
            }
        };

        if (!silent) {
            System.out.printf("🔍 %s...%n",
                    translator.translate("analyzer.searching_files"));
        } else {
            // 静默模式下的空回调
            progressCallback = found -> {};
        }

        // 查找匹配的源码文件
        List<String> files;
        try {
            files = CommonUtils.findSourceFiles(path, includePatterns, excludePatterns, progressCallback);
        } catch (IOException e) {
            throw new AnalysisException(
                    translator.translate("error.source_files_not_found", e.getMessage()), e
            );
        }

        // 只在非静默模式下清除进度显示并显示文件总数
        if (!silent && lastFoundCount[0] > 0) {
            System.out.printf("\r%s\r", " ".repeat(80));
            System.out.printf("📂 %s: %d%n",
                    translator.translate("analyzer.files_found"), files.size());
        }

        // 如果没有找到文件，直接返回空结果
        if (files.isEmpty()) {
            AnalysisResult result = new AnalysisResult();
            result.setMetrics(new HashMap<>());
            result.setFilesAnalyzed(new ArrayList<>());
            result.setTotalFiles(0);
            result.setTotalLines(0);
            return result;
        }

        // 保存分析结果
        List<com.diagnostician.service.model.metric.AnalysisResult> fileResults = new ArrayList<>(files.size());

        // 在分析文件前显示进度条
        if (!silent) {
            // 不要添加多余的空行
            // 进度显示简化，无需 ANSI 支持

            // 根据语言选择进度文本
            String progressText;
            switch (translator.getLanguage()) {
                case EN_US:
                    progressText = "Analyzing files";
                    break;
                default:
                    progressText = "正在分析文件";
                    break;
            }

            // 保存文件名历史
            String currentFile = "";
            int fileCount = files.size();

            // 分析每个文件并显示进度
            for (int i = 0; i < files.size(); i++) {
                String filePath = files.get(i);
                // 更新文件名
                currentFile = shortenPath(filePath);

                // 计算和显示进度条
                double percent = (double)(i + 1) / fileCount;
                int barWidth = 30;
                int barCompleted = (int)(barWidth * percent);
                int barRemaining = barWidth - barCompleted;

                // 显示进度条
            System.out.print("\r\033[K  ");
            System.out.print(progressText + ": ");
            System.out.printf("%d/%d ", i + 1, fileCount);
            System.out.print("[" + "#".repeat(barCompleted) + "-".repeat(barRemaining) + "]");

                // 显示当前处理的文件
            System.out.printf("%n\033[K  正在处理: %s", currentFile);

                // 回到进度条行
                System.out.print("\033[A\r");

                // 分析文件
                try {
                    com.diagnostician.service.model.metric.AnalysisResult result = codeAnalyzer.analyzeFile(filePath);
                    fileResults.add(result);
                } catch (AnalysisException e) {
                    System.err.printf(translator.translate("error.file_analysis_failed"),
                            filePath, e.getMessage());
                    continue;
                }

                // 大量文件时不要延迟
                if (files.size() < 30) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // 清理进度条行
            System.out.print("\r\033[K\n");
        } else {
            // 静默模式下直接分析文件
            for (String filePath : files) {
                try {
                    com.diagnostician.service.model.metric.AnalysisResult result = codeAnalyzer.analyzeFile(filePath);
                    fileResults.add(result);
                } catch (AnalysisException e) {
                    System.err.printf(translator.translate("error.file_analysis_failed"),
                            filePath, e.getMessage());
                    continue;
                }
            }
        }

        // 创建结果对象
        AnalysisResult result = new AnalysisResult();
        result.setMetrics(new HashMap<>());
        result.setFilesAnalyzed(new ArrayList<>(fileResults.size()));
        result.setTotalFiles(fileResults.size());

        // 收集所有指标结果
        Map<String, List<com.diagnostician.service.model.metric.MetricResult>> allMetrics = new HashMap<>();
        int totalLines = 0;

        // 处理每个文件的结果
        for (com.diagnostician.service.model.metric.AnalysisResult fileResult : fileResults) {
            totalLines += fileResult.getTotalLines();

            // 添加文件分析结果
            FileAnalysisResult fileAnalysisResult = new FileAnalysisResult();
            fileAnalysisResult.setFilePath(fileResult.getFilePath());
            fileAnalysisResult.setFileScore(fileResult.getOverallScore());
            fileAnalysisResult.setIssues(fileResult.getIssues());
            result.getFilesAnalyzed().add(fileAnalysisResult);

            // 收集各指标结果
            for (Map.Entry<String, com.diagnostician.service.model.metric.MetricResult> entry : fileResult.getMetricResults().entrySet()) {
                String name = entry.getKey();
                com.diagnostician.service.model.metric.MetricResult metricResult = entry.getValue();

                allMetrics.computeIfAbsent(name, k -> new ArrayList<>(fileResults.size()))
                        .add(metricResult);
            }
        }

        // 计算各指标的平均分数
        for (Map.Entry<String, List<com.diagnostician.service.model.metric.MetricResult>> entry : allMetrics.entrySet()) {
            String name = entry.getKey();
            List<com.diagnostician.service.model.metric.MetricResult> metricResults = entry.getValue();

            if (metricResults.isEmpty()) {
                continue;
            }

            // 计算平均分
            double totalScore = 0.0;
            double totalWeight = 0.0;
            String description = "";

            for (com.diagnostician.service.model.metric.MetricResult m : metricResults) {
                totalScore += m.getScore();
                totalWeight = m.getWeight();
                description = m.getDescription();
            }

            double avgScore = totalScore / metricResults.size();

            // 添加到结果中
            MetricResult resultMetric = new MetricResult();
            resultMetric.setName(name);
            resultMetric.setScore(avgScore);
            resultMetric.setDescription(description);
            resultMetric.setWeight(totalWeight);
            result.getMetrics().put(name, resultMetric);
        }

        // 设置总行数
        result.setTotalLines(totalLines);

        // 计算总体评分
        result.setCodeQualityScore(codeAnalyzer.calculateOverallScore(fileResults));

        return result;
    }

    // shortenPath 缩短文件路径，只显示最后几个部分
    private String shortenPath(String path) {
        Path p = Paths.get(path);
        int count = p.getNameCount();
        if (count <= 4) {
            return p.toString().replace('\\','/');
        }
        Path tail = p.subpath(count - 3, count);
        return "./" + tail.toString().replace('\\','/');
    }

    // 内层：CodeAnalyzer - 核心分析层
    // CodeAnalyzer 代码分析器
    @Component
    public static class CodeAnalyzer {
        private final Factory metricFactory;
        private Translator translator;

        public CodeAnalyzer(Translator translator, Factory metricFactory) {
            this.translator = translator;
            this.metricFactory = metricFactory;
        }

        public void setTranslator(Translator translator) {
            this.translator = translator;
        }

        public com.diagnostician.service.model.metric.AnalysisResult analyzeFile(String filePath) throws AnalysisException {
            try {
                byte[] content = Files.readAllBytes(Paths.get(filePath));

                com.diagnostician.service.model.parser.Parser parserImpl =
                        com.diagnostician.service.model.parser.ParserFactory.createParserForFile(filePath);

                com.diagnostician.service.model.parser.ParseResult parseResult;
                try {
                    parseResult = parserImpl.parse(filePath, content);
                } catch (Exception e) {
                    throw new AnalysisException("Failed to parse file: " + filePath, e);
                }

                if (parseResult instanceof com.diagnostician.service.model.parser.BaseParseResult bp) {
                    if (bp.getContent() == null || bp.getContent().length == 0) {
                        bp.setContent(content);
                    }
                }

                CompilationUnit cu = null;
                if (parseResult.getLanguage() == com.diagnostician.service.model.common.LanguageType.JAVA) {
                    ASTParser jdt = ASTParser.newParser(AST.JLS21);
                    jdt.setKind(ASTParser.K_COMPILATION_UNIT);
                    jdt.setSource(new String(content, StandardCharsets.UTF_8).toCharArray());
                    jdt.setResolveBindings(false);
                    cu = (CompilationUnit) jdt.createAST(null);
                    if (cu != null) {
                        cu.setProperty("filePath", filePath);
                        if (parseResult instanceof com.diagnostician.service.model.parser.BaseParseResult bp2) {
                            bp2.setASTRoot(cu);
                        }
                    }
                }

                com.diagnostician.service.model.metric.AnalysisResult result =
                        new com.diagnostician.service.model.metric.AnalysisResult();
                result.setFilePath(filePath);
                result.setTotalLines(parseResult.getTotalLines());
                result.setCommentLines(parseResult.getCommentLines());

                List<String> imports = new ArrayList<>();
                if (cu != null) {
                    for (Object obj : cu.imports()) {
                        if (obj instanceof ImportDeclaration id) {
                            imports.add(id.getName().getFullyQualifiedName());
                        }
                    }
                }

                result.setImports(imports);
                result.setMetricResults(new HashMap<>());
                result.setFunctions(parseResult.getFunctions());
                result.setParseResult(parseResult);

                for (com.diagnostician.service.model.metric.Metric metric : metricFactory.createAllMetrics()) {
                    try {
                        com.diagnostician.service.model.metric.MetricResult mr = metric.analyze(parseResult);
                        result.addMetricResult(metric.getName(), mr);
                    } catch (Exception metricEx) {
                        result.addMetricResult(metric.getName(), new com.diagnostician.service.model.metric.MetricResult(
                                0.0,
                                new ArrayList<>(),
                                metric.getDescription() + " (分析失败: " + metricEx.getMessage() + ")",
                                metric.getWeight()
                        ));
                    }
                }
                return result;
            } catch (IOException e) {
                throw new AnalysisException("Failed to analyze file: " + filePath, e);
            }
        }

        public double calculateOverallScore(List<com.diagnostician.service.model.metric.AnalysisResult> results) {
            if (results == null || results.isEmpty()) return 0.0;
            double total = 0.0;
            int count = 0;
            for (com.diagnostician.service.model.metric.AnalysisResult r : results) {
                total += r.getOverallScore();
                count++;
            }
            return count == 0 ? 0.0 : total / count;
        }
    }
}
