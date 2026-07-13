package com.diagnostician.service.model.report;

import com.diagnostician.entity.Issue;
import com.diagnostician.service.model.analyzer.Analyzer;
import com.diagnostician.service.model.analyzer.Analyzer.AnalysisResult;
import com.diagnostician.service.model.analyzer.Analyzer.FileAnalysisResult;
import com.diagnostician.service.model.analyzer.Analyzer.MetricResult;
import com.diagnostician.service.model.analyzer.Analyzer.Translator;
import com.diagnostician.service.model.analyzer.DefaultTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class Report {
    private Translator translator;

    @Autowired
    public Report(DefaultTranslator translator) {
        this.translator = translator;
        // API 建议文案默认中文，与前端界面一致
        if (translator instanceof DefaultTranslator dt) {
            dt.setLanguage(Analyzer.Language.ZH_CN);
        }
    }

    /** 按当前 Translator 语言生成维度优化建议 */
    public String translateSuggestion(String dimensionName) {
        return translator.translate("suggestion.optimize_dimension", dimensionName);
    }

    // 重要：报告选项，控制报告输出内容
    public static class ReportOptions {
        public boolean verbose = false;
        public int topFiles = 3;
        public int maxIssues = 3;
        public boolean summaryOnly = false;
        public boolean markdownOutput = false;
        public boolean useColor = false;
    }

    // 重要：质量等级映射（与 Go 版本一致，基于百分制阈值）
    public static class QualityLevel {
        public final double minScore;
        public final String nameKey;
        public final String descriptionKey;
        public final String emoji;

        public QualityLevel(double minScore, String nameKey, String descriptionKey, String emoji) {
            this.minScore = minScore;
            this.nameKey = nameKey;
            this.descriptionKey = descriptionKey;
            this.emoji = emoji;
        }
    }

    private static final List<QualityLevel> QUALITY_LEVELS = List.of(
            new QualityLevel(0, "level.clean", "level.clean.description", "🧹"),
            new QualityLevel(5, "level.mild", "level.mild.description", "🙂"),
            new QualityLevel(15, "level.moderate", "level.moderate.description", "🤔"),
            new QualityLevel(25, "level.bad", "level.bad.description", "😬"),
            new QualityLevel(40, "level.terrible", "level.terrible.description", "😵"),
            new QualityLevel(55, "level.disaster", "level.disaster.description", "🔥"),
            new QualityLevel(65, "level.disaster.severe", "level.disaster.severe.description", "⚠️"),
            new QualityLevel(75, "level.disaster.very_bad", "level.disaster.very_bad.description", "💀"),
            new QualityLevel(85, "level.disaster.extreme", "level.disaster.extreme.description", "🚨"),
            new QualityLevel(95, "level.disaster.worst", "level.disaster.worst.description", "🆘"),
            new QualityLevel(100, "level.disaster.ultimate", "level.disaster.ultimate.description", "💥")
    );

    // 生成报告主入口，返回可直接打印的字符串
    public String generateReport(AnalysisResult result, ReportOptions options) {
        if (options == null) options = new ReportOptions();
        return generateMarkdownReport(result, options);
    }

    // Markdown 报告
    public String generateMarkdownReport(AnalysisResult result, ReportOptions options) {
        StringBuilder md = new StringBuilder();
        double displayScore = clamp01(result.getCodeQualityScore()) * 100.0;
        QualityLevel level = getQualityLevel(displayScore);

        md.append("# ").append(t("report.title")).append("\n\n");
        md.append(String.format(Locale.ROOT, "**%s** (%s)\n\n", t("report.overall_score", String.format(Locale.ROOT, "%.2f", displayScore)), t(level.nameKey))).append(t(level.descriptionKey)).append(" ").append(level.emoji).append("\n\n");

        if (!options.summaryOnly) {
            md.append("## ").append(t("report.section.metrics")).append("\n\n");
            md.append(renderMetricsMarkdown(result.getMetrics())).append("\n\n");

            md.append("## All Issues\n\n");
            md.append(renderAllIssuesMarkdown(result.getFilesAnalyzed())).append("\n\n");
        }

        md.append("## ").append(t("report.section.summary")).append("\n\n");
        md.append(renderSummaryMarkdown(result)).append("\n");

        if (options.verbose) {
            md.append("## ").append(t("report.section.details")).append("\n\n");
            md.append(renderVerboseInfoMarkdown(result)).append("\n");
        }

        return md.toString();
    }

    private String renderMetricsMarkdown(Map<String, MetricResult> metrics) {
        if (metrics == null || metrics.isEmpty()) return "_" + t("report.metrics.none") + "_";
        StringBuilder sb = new StringBuilder();
        sb.append("|    Metric    |  Score  | Weight | Description |\n");
        sb.append("|--------------|---------|--------|-------------|\n");
        List<Map.Entry<String, MetricResult>> ordered = metrics.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> -clamp01(e.getValue().getScore())))
                .collect(Collectors.toList());
        for (Map.Entry<String, MetricResult> e : ordered) {
            MetricResult m = e.getValue();
            double score = clamp01(m.getScore()) * 100.0;
            sb.append(String.format(Locale.ROOT, "| %s | %.2f | %.2f | %s |\n",
                    escapeMd(e.getKey()), score, m.getWeight(), escapeMd(nullSafe(m.getDescription()))));
        }
        return sb.toString();
    }

    private String renderAllIssuesMarkdown(List<FileAnalysisResult> files) {
        if (files == null || files.isEmpty()) return "_" + t("report.files.none") + "_";
        StringBuilder sb = new StringBuilder();
        List<FileAnalysisResult> ordered = files.stream()
                .sorted(Comparator.comparingDouble(f -> -clamp01(f.getFileScore())))
                .collect(Collectors.toList());
        for (FileAnalysisResult f : ordered) {
            sb.append(String.format(Locale.ROOT, "- `%s` (%.2f)\n", escapeMd(f.getFilePath()), clamp01(f.getFileScore()) * 100.0));
            List<Issue> issues = f.getIssues() != null ? f.getIssues() : List.of();
            for (Issue is : issues) {
                String file = is.getFile() != null ? is.getFile() : f.getFilePath();
                String lineStr = is.getLine() != null ? String.valueOf(is.getLine()) : "-";
                String sev = String.valueOf(is.getSeverity());
                String msg = is.getMessage() != null ? is.getMessage() : "";
                String suggestion = is.getSuggestion() != null ? is.getSuggestion() : "";
                sb.append(String.format(Locale.ROOT, "  - [%s] %s:%s - %s\n建议：%s\n", sev, escapeMd(file), lineStr, escapeMd(msg), escapeMd(suggestion)));
            }
        }
        return sb.toString();
    }

    private String renderSummaryMarkdown(AnalysisResult result) {
        int fileCount = result.getTotalFiles();
        int totalLines = result.getTotalLines();
        double score = clamp01(result.getCodeQualityScore()) * 100.0;
        return String.format(Locale.ROOT, "- %s: %d\n- %s: %d\n- %s: %.2f",
                t("report.summary.files"), fileCount,
                t("report.summary.lines"), totalLines,
                t("report.summary.score"), score);
    }

    private String renderVerboseInfoMarkdown(AnalysisResult result) {
        return String.format(Locale.ROOT, "- %s: %d\n- %s: %d",
                t("report.verbose.metrics"), result.getMetrics().size(),
                t("report.verbose.files"), result.getFilesAnalyzed().size());
    }

    private double clamp01(double v) {
        if (Double.isNaN(v)) return 0.0;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private QualityLevel getQualityLevel(double percentScore) {
        QualityLevel current = QUALITY_LEVELS.get(0);
        for (QualityLevel q : QUALITY_LEVELS) {
            if (percentScore >= q.minScore) current = q;
        }
        return current;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String escapeMd(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("|", "\\|")
                .replace("`", "\\`");
    }

    private String t(String key, Object... args) {
        try {
            String v = translator.translate(key, args);
            return Objects.equals(v, key) ? formatFallback(key, args) : v;
        } catch (Exception e) {
            return formatFallback(key, args);
        }
    }

    private String formatFallback(String key, Object... args) {
        if (args == null || args.length == 0) return key;
        return String.format(Locale.ROOT, key, args);
    }
}
