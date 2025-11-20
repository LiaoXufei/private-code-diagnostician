package com.bugkiller_cityu.code_diagnostician.service.model.report;

import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.AnalysisResult;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.FileAnalysisResult;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.MetricResult;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.Translator;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.DefaultTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    // 重要：生成报告主入口，返回可直接打印的字符串
    public String generateReport(AnalysisResult result, ReportOptions options) {
        if (options == null) options = new ReportOptions();
        return options.markdownOutput ? generateMarkdownReport(result, options) : generateConsoleReport(result, options);
    }

    // 控制台报告（含可选 ANSI 颜色）
    public String generateConsoleReport(AnalysisResult result, ReportOptions options) {
        StringBuilder out = new StringBuilder();

        printDivider(out);
        out.append("\n  ").append(emoji("📣 ")).append(t("report.title")).append("\n");
        printDivider(out);

        double displayScore = clamp01(result.getCodeQualityScore()) * 100.0;
        QualityLevel level = getQualityLevel(displayScore);

        out.append("\n  ").append(t("report.overall_score", String.format(Locale.ROOT, "%.2f", displayScore))).append(" - ");
        out.append(scoreComment(displayScore)).append("\n");

        out.append("  ").append(t("report.level", t(level.nameKey))).append(" - ").append(t(level.descriptionKey)).append(" ").append(level.emoji).append("\n\n");

        if (!options.summaryOnly) {
            out.append(sectionTitle(t("report.section.metrics"))).append("\n");
            out.append(renderMetrics(result.getMetrics())).append("\n\n");

            if (options.verbose) {
                out.append(sectionTitle(t("report.section.files_all"))).append("\n");
                out.append(renderAllFiles(result.getFilesAnalyzed(), options.maxIssues)).append("\n\n");
            } else {
                out.append(sectionTitle(t("report.section.top_issues"))).append("\n");
                out.append(renderTopIssues(result.getFilesAnalyzed(), options.topFiles, options.maxIssues)).append("\n\n");
            }
        }

        out.append(sectionTitle(t("report.section.summary"))).append("\n");
        out.append(renderSummary(result)).append("\n");

        if (options.verbose) {
            out.append(sectionTitle(t("report.section.details"))).append("\n");
            out.append(renderVerboseInfo(result)).append("\n");
        }

        printDivider(out);
        out.append("\n");
        return maybeColorize(out.toString(), options.useColor);
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

            if (options.verbose) {
                md.append("## ").append(t("report.section.files_all")).append("\n\n");
                md.append(renderAllFilesMarkdown(result.getFilesAnalyzed(), options.maxIssues)).append("\n\n");
            } else {
                md.append("## ").append(t("report.section.top_issues")).append("\n\n");
                md.append(renderTopIssuesMarkdown(result.getFilesAnalyzed(), options.topFiles, options.maxIssues)).append("\n\n");
            }
        }

        md.append("## ").append(t("report.section.summary")).append("\n\n");
        md.append(renderSummaryMarkdown(result)).append("\n");

        if (options.verbose) {
            md.append("## ").append(t("report.section.details")).append("\n\n");
            md.append(renderVerboseInfoMarkdown(result)).append("\n");
        }

        return md.toString();
    }

    private String renderMetrics(Map<String, MetricResult> metrics) {
        if (metrics == null || metrics.isEmpty()) return t("report.metrics.none");
        StringBuilder sb = new StringBuilder();
        List<Map.Entry<String, MetricResult>> ordered = metrics.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> -clamp01(e.getValue().getScore())))
                .collect(Collectors.toList());
        for (Map.Entry<String, MetricResult> e : ordered) {
            MetricResult m = e.getValue();
            double score = clamp01(m.getScore()) * 100.0;
            sb.append(String.format(Locale.ROOT, "- %s: %.2f%% (w=%.2f) - %s\n", e.getKey(), score, m.getWeight(), nullSafe(m.getDescription())));
        }
        return sb.toString();
    }

    private String renderMetricsMarkdown(Map<String, MetricResult> metrics) {
        if (metrics == null || metrics.isEmpty()) return "_" + t("report.metrics.none") + "_";
        StringBuilder sb = new StringBuilder();
        sb.append("| Metric | Score | Weight | Description |\n|---|---:|---:|---|\n");
        List<Map.Entry<String, MetricResult>> ordered = metrics.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> -clamp01(e.getValue().getScore())))
                .collect(Collectors.toList());
        for (Map.Entry<String, MetricResult> e : ordered) {
            MetricResult m = e.getValue();
            double score = clamp01(m.getScore()) * 100.0;
            sb.append(String.format(Locale.ROOT, "| %s | %.2f%% | %.2f | %s |\n", e.getKey(), score, m.getWeight(), escapeMd(nullSafe(m.getDescription()))));
        }
        return sb.toString();
    }

    private String renderTopIssues(List<FileAnalysisResult> files, int topFiles, int maxIssues) {
        if (files == null || files.isEmpty()) return t("report.files.none");
        StringBuilder sb = new StringBuilder();
        List<FileAnalysisResult> ordered = files.stream()
                .sorted(Comparator.comparingDouble(f -> -clamp01(f.getFileScore())))
                .limit(Math.max(1, topFiles))
                .collect(Collectors.toList());
        for (FileAnalysisResult f : ordered) {
            sb.append(String.format(Locale.ROOT, "* %s (%.2f%%)\n", f.getFilePath(), clamp01(f.getFileScore()) * 100.0));
            List<String> issues = takeIssues(f.getIssues(), maxIssues);
            for (String issue : issues) {
                sb.append("  - ").append(issue).append("\n");
            }
        }
        return sb.toString();
    }

    private String renderTopIssuesMarkdown(List<FileAnalysisResult> files, int topFiles, int maxIssues) {
        if (files == null || files.isEmpty()) return "_" + t("report.files.none") + "_";
        StringBuilder sb = new StringBuilder();
        List<FileAnalysisResult> ordered = files.stream()
                .sorted(Comparator.comparingDouble(f -> -clamp01(f.getFileScore())))
                .limit(Math.max(1, topFiles))
                .collect(Collectors.toList());
        for (FileAnalysisResult f : ordered) {
            sb.append(String.format(Locale.ROOT, "- `%s` (%.2f%%)\n", f.getFilePath(), clamp01(f.getFileScore()) * 100.0));
            List<String> issues = takeIssues(f.getIssues(), maxIssues);
            for (String issue : issues) {
                sb.append("  - ").append(escapeMd(issue)).append("\n");
            }
        }
        return sb.toString();
    }

    private String renderAllFiles(List<FileAnalysisResult> files, int maxIssues) {
        if (files == null || files.isEmpty()) return t("report.files.none");
        StringBuilder sb = new StringBuilder();
        List<FileAnalysisResult> ordered = files.stream()
                .sorted(Comparator.comparingDouble(f -> -clamp01(f.getFileScore())))
                .collect(Collectors.toList());
        for (FileAnalysisResult f : ordered) {
            sb.append(String.format(Locale.ROOT, "* %s (%.2f%%)\n", f.getFilePath(), clamp01(f.getFileScore()) * 100.0));
            List<String> issues = takeIssues(f.getIssues(), maxIssues);
            for (String issue : issues) {
                sb.append("  - ").append(issue).append("\n");
            }
        }
        return sb.toString();
    }

    private String renderAllFilesMarkdown(List<FileAnalysisResult> files, int maxIssues) {
        if (files == null || files.isEmpty()) return "_" + t("report.files.none") + "_";
        StringBuilder sb = new StringBuilder();
        List<FileAnalysisResult> ordered = files.stream()
                .sorted(Comparator.comparingDouble(f -> -clamp01(f.getFileScore())))
                .collect(Collectors.toList());
        for (FileAnalysisResult f : ordered) {
            sb.append(String.format(Locale.ROOT, "- `%s` (%.2f%%)\n", f.getFilePath(), clamp01(f.getFileScore()) * 100.0));
            List<String> issues = takeIssues(f.getIssues(), maxIssues);
            for (String issue : issues) {
                sb.append("  - ").append(escapeMd(issue)).append("\n");
            }
        }
        return sb.toString();
    }

    private String renderSummary(AnalysisResult result) {
        int fileCount = result.getTotalFiles();
        int totalLines = result.getTotalLines();
        double score = clamp01(result.getCodeQualityScore()) * 100.0;
        return String.format(Locale.ROOT, "%s: %d | %s: %d | %s: %.2f%%",
                t("report.summary.files"), fileCount,
                t("report.summary.lines"), totalLines,
                t("report.summary.score"), score);
    }

    private String renderSummaryMarkdown(AnalysisResult result) {
        int fileCount = result.getTotalFiles();
        int totalLines = result.getTotalLines();
        double score = clamp01(result.getCodeQualityScore()) * 100.0;
        return String.format(Locale.ROOT, "- %s: %d\n- %s: %d\n- %s: %.2f%%",
                t("report.summary.files"), fileCount,
                t("report.summary.lines"), totalLines,
                t("report.summary.score"), score);
    }

    private String renderVerboseInfo(AnalysisResult result) {
        return String.format(Locale.ROOT, "%s: %d, %s: %d",
                t("report.verbose.metrics"), result.getMetrics().size(),
                t("report.verbose.files"), result.getFilesAnalyzed().size());
    }

    private String renderVerboseInfoMarkdown(AnalysisResult result) {
        return String.format(Locale.ROOT, "- %s: %d\n- %s: %d",
                t("report.verbose.metrics"), result.getMetrics().size(),
                t("report.verbose.files"), result.getFilesAnalyzed().size());
    }

    private List<String> takeIssues(List<String> issues, int maxIssues) {
        if (issues == null || issues.isEmpty()) return List.of(t("report.issues.none"));
        int n = Math.min(Math.max(1, maxIssues), issues.size());
        return new ArrayList<>(issues.subList(0, n));
    }

    private String sectionTitle(String text) {
        return "== " + text + " ==";
    }

    private void printDivider(StringBuilder out) {
        out.append("----------------------------------------\n");
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

    private String scoreComment(double percentScore) {
        if (percentScore >= 90) return t("report.score.excellent");
        if (percentScore >= 80) return t("report.score.good");
        if (percentScore >= 60) return t("report.score.average");
        return t("report.score.poor");
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String emoji(String s) {
        return s;
    }

    private String maybeColorize(String s, boolean useColor) {
        return s; // 控制台颜色按需后续扩展
    }

    private String escapeMd(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|");
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
