package com.diagnostician.service.model.analyzer;

import org.springframework.stereotype.Component;
import com.diagnostician.service.model.analyzer.Analyzer.Language;
import com.diagnostician.service.model.analyzer.Analyzer.Translator;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认翻译器实现
 * 提供多语言支持的翻译服务
 */
@Component
public class DefaultTranslator implements Translator {

    // 当前使用的语言
    private Language language;

    // 翻译字典 - 存储所有翻译文本
    // 结构: Map<语言, Map<翻译键, 翻译文本>>
    private static final Map<Language, Map<String, String>> translations = new HashMap<>();

    // 静态初始化块 - 在类加载时初始化翻译字典
    static {
        initTranslations();
    }

    /**
     * 构造函数
     * @param language 初始语言
     */
    public DefaultTranslator(Language language) {
        this.language = language != null ? language : Language.EN_US;
    }

    /**
     * 默认构造函数 - Spring需要无参构造函数
     * 默认使用英语
     */
    public DefaultTranslator() {
        this(Language.EN_US);
    }

    /**
     * 翻译指定的键
     * @param key 翻译键
     * @param args 参数（用于格式化字符串，如 %s, %d等）
     * @return 翻译后的字符串
     */
    @Override
    public String translate(String key, Object... args) {
        // 获取当前语言的翻译字典
        Map<String, String> langTranslations = translations.get(language);

        if (langTranslations == null) {
            // 如果当前语言没有翻译，返回原始键
            return key;
        }

        // 获取翻译文本
        String translation = langTranslations.get(key);

        if (translation == null) {
            // 如果找不到翻译，返回原始键
            return key;
        }

        // 如果有参数，使用String.format格式化
        if (args != null && args.length > 0) {
            try {
                return String.format(translation, args);
            } catch (Exception e) {
                // 格式化失败，返回原始翻译
                return translation;
            }
        }

        return translation;
    }

    /**
     * 获取当前语言
     * @return 语言类型
     */
    @Override
    public Language getLanguage() {
        return this.language;
    }

    /**
     * 设置语言
     * @param language 要设置的语言
     */
    public void setLanguage(Language language) {
        this.language = language != null ? language : Language.EN_US;
    }

    /**
     * 初始化翻译字典
     * 这里添加所有支持的语言翻译
     */
    private static void initTranslations() {
        // 初始化英文翻译
        Map<String, String> enTranslations = new HashMap<>();
        enTranslations.put("analysis.start", "Starting code analysis...");
        enTranslations.put("analysis.complete", "Analysis completed");
        enTranslations.put("analysis.scanning", "Scanning files...");
        enTranslations.put("analysis.found_files", "Found %d files to analyze");
        enTranslations.put("analysis.analyzing_file", "Analyzing: %s (%d/%d)");
        enTranslations.put("analysis.progress", "Progress: %d%%");
        enTranslations.put("analysis.error", "Error analyzing file: %s");
        enTranslations.put("metric.calculating", "Calculating metrics...");
        enTranslations.put("result.quality_score", "Code Quality Score: %.2f");
        enTranslations.put("result.total_files", "Total Files: %d");
        enTranslations.put("result.total_lines", "Total Lines: %d");
        // Report 模块
        enTranslations.put("report.title", "Code Quality Report");
        enTranslations.put("report.overall_score", "Overall Score: %s");
        enTranslations.put("report.level", "Level: %s");
        enTranslations.put("report.section.metrics", "Metrics Overview");
        enTranslations.put("report.section.top_issues", "Top Issues");
        enTranslations.put("report.section.files_all", "Files Overview");
        enTranslations.put("report.section.summary", "Summary");
        enTranslations.put("report.section.details", "Details");
        enTranslations.put("report.metrics.none", "No metrics available");
        enTranslations.put("report.files.none", "No files analyzed");
        enTranslations.put("report.issues.none", "No issues");
        enTranslations.put("report.summary.files", "Files");
        enTranslations.put("report.summary.lines", "Lines");
        enTranslations.put("report.summary.score", "Score");
        enTranslations.put("report.verbose.metrics", "Metric count");
        enTranslations.put("report.verbose.files", "File count");
        enTranslations.put("report.score.excellent", "Excellent");
        enTranslations.put("report.score.good", "Good");
        enTranslations.put("report.score.average", "Average");
        enTranslations.put("report.score.poor", "Poor");
        enTranslations.put("suggestion.optimize_dimension", "Improve code related to %s");
        // Quality levels
        enTranslations.put("level.clean", "Clean");
        enTranslations.put("level.clean.description", "Codebase is clean and well-structured");
        enTranslations.put("level.mild", "Mild Issues");
        enTranslations.put("level.mild.description", "Minor issues detected");
        enTranslations.put("level.moderate", "Moderate Issues");
        enTranslations.put("level.moderate.description", "Some issues require attention");
        enTranslations.put("level.bad", "Bad");
        enTranslations.put("level.bad.description", "Quality is below expectations");
        enTranslations.put("level.terrible", "Terrible");
        enTranslations.put("level.terrible.description", "Serious problems found");
        enTranslations.put("level.disaster", "Disaster");
        enTranslations.put("level.disaster.description", "Critical quality problems");
        enTranslations.put("level.disaster.severe", "Severe Disaster");
        enTranslations.put("level.disaster.severe.description", "Severe and widespread issues");
        enTranslations.put("level.disaster.very_bad", "Very Bad");
        enTranslations.put("level.disaster.very_bad.description", "Very high risk areas");
        enTranslations.put("level.disaster.extreme", "Extreme");
        enTranslations.put("level.disaster.extreme.description", "Extremely poor quality");
        enTranslations.put("level.disaster.worst", "Worst");
        enTranslations.put("level.disaster.worst.description", "Unacceptable state");
        enTranslations.put("level.disaster.ultimate", "Ultimate Disaster");
        enTranslations.put("level.disaster.ultimate.description", "Catastrophic quality level");
        translations.put(Language.EN_US, enTranslations);

        // 初始化中文翻译
        Map<String, String> zhTranslations = new HashMap<>();
        zhTranslations.put("analysis.start", "开始代码分析...");
        zhTranslations.put("analysis.complete", "分析完成");
        zhTranslations.put("analysis.scanning", "扫描文件中...");
        zhTranslations.put("analysis.found_files", "找到 %d 个文件待分析");
        zhTranslations.put("analysis.analyzing_file", "正在分析: %s (%d/%d)");
        zhTranslations.put("analysis.progress", "进度: %d%%");
        zhTranslations.put("analysis.error", "分析文件出错: %s");
        zhTranslations.put("metric.calculating", "计算指标中...");
        zhTranslations.put("result.quality_score", "代码质量评分: %.2f");
        zhTranslations.put("result.total_files", "总文件数: %d");
        zhTranslations.put("result.total_lines", "总代码行数: %d");
        // Report 模块
        zhTranslations.put("report.title", "代码质量报告");
        zhTranslations.put("report.overall_score", "总体评分: %s%%");
        zhTranslations.put("report.level", "质量等级: %s");
        zhTranslations.put("report.section.metrics", "指标概览");
        zhTranslations.put("report.section.top_issues", "重点问题");
        zhTranslations.put("report.section.files_all", "文件概览");
        zhTranslations.put("report.section.summary", "总结");
        zhTranslations.put("report.section.details", "详情");
        zhTranslations.put("report.metrics.none", "暂无指标数据");
        zhTranslations.put("report.files.none", "暂无文件分析数据");
        zhTranslations.put("report.issues.none", "无问题");
        zhTranslations.put("report.summary.files", "文件数");
        zhTranslations.put("report.summary.lines", "总代码行数");
        zhTranslations.put("report.summary.score", "评分");
        zhTranslations.put("report.verbose.metrics", "指标数量");
        zhTranslations.put("report.verbose.files", "文件数量");
        zhTranslations.put("report.score.excellent", "优秀");
        zhTranslations.put("report.score.good", "良好");
        zhTranslations.put("report.score.average", "及格");
        zhTranslations.put("report.score.poor", "不及格");
        zhTranslations.put("suggestion.optimize_dimension", "优化%s相关代码");
        // Quality levels
        zhTranslations.put("level.clean", "洁净");
        zhTranslations.put("level.clean.description", "代码整洁、结构良好");
        zhTranslations.put("level.mild", "轻微问题");
        zhTranslations.put("level.mild.description", "存在少量问题");
        zhTranslations.put("level.moderate", "中等问题");
        zhTranslations.put("level.moderate.description", "有一些需要关注的问题");
        zhTranslations.put("level.bad", "较差");
        zhTranslations.put("level.bad.description", "质量低于预期");
        zhTranslations.put("level.terrible", "很差");
        zhTranslations.put("level.terrible.description", "存在严重问题");
        zhTranslations.put("level.disaster", "灾难");
        zhTranslations.put("level.disaster.description", "关键质量问题");
        zhTranslations.put("level.disaster.severe", "严重灾难");
        zhTranslations.put("level.disaster.severe.description", "问题严重且广泛");
        zhTranslations.put("level.disaster.very_bad", "非常差");
        zhTranslations.put("level.disaster.very_bad.description", "风险极高的区域");
        zhTranslations.put("level.disaster.extreme", "极差");
        zhTranslations.put("level.disaster.extreme.description", "质量极其低下");
        zhTranslations.put("level.disaster.worst", "最差");
        zhTranslations.put("level.disaster.worst.description", "不可接受的状态");
        zhTranslations.put("level.disaster.ultimate", "终极灾难");
        zhTranslations.put("level.disaster.ultimate.description", "灾难性的质量等级");
        translations.put(Language.ZH_CN, zhTranslations);

        // 初始化俄语翻译
        Map<String, String> ruTranslations = new HashMap<>();
        ruTranslations.put("analysis.start", "Начало анализа кода...");
        ruTranslations.put("analysis.complete", "Анализ завершен");
        ruTranslations.put("analysis.scanning", "Сканирование файлов...");
        ruTranslations.put("analysis.found_files", "Найдено %d файлов для анализа");
        ruTranslations.put("analysis.analyzing_file", "Анализ: %s (%d/%d)");
        ruTranslations.put("analysis.progress", "Прогресс: %d%%");
        ruTranslations.put("analysis.error", "Ошибка анализа файла: %s");
        ruTranslations.put("metric.calculating", "Расчет метрик...");
        ruTranslations.put("result.quality_score", "Оценка качества кода: %.2f");
        ruTranslations.put("result.total_files", "Всего файлов: %d");
        ruTranslations.put("result.total_lines", "Всего строк: %d");
        ruTranslations.put("suggestion.optimize_dimension", "Улучшите код, связанный с %s");
        translations.put(Language.RU_RU, ruTranslations);
    }
}
