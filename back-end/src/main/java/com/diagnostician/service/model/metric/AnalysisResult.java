package com.diagnostician.service.model.metric;

import com.diagnostician.entity.Issue;
import com.diagnostician.service.model.parser.Function;
import com.diagnostician.service.model.parser.ParseResult;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@ToString
// AnalysisResult 表示分析结果
public class AnalysisResult {
    String filePath;                            // 文件路径
    int totalLines;                             // 总行数
    int commentLines;                           // 注释行数
    List<String> imports;                       // 导入的包
    Map<String, MetricResult> metricResults;    // 各指标结果
    List<Function> functions;                   // 函数列表
    ParseResult parseResult;                   // 解析结果

    // getOverallScore 获取总体评分
    public double getOverallScore() {
        if (metricResults.isEmpty()) {
            return 0.0;
        }
        double totalScore = 0.0, totalWeight = 0.0;
        for (MetricResult result : metricResults.values()) {
            totalScore += result.score * result.weight;
            totalWeight += result.weight;
        }

        if (Math.abs(totalWeight) < 1e-6) {
            return 0.0;
        }

        double finalScore = totalScore / totalWeight;

        if (finalScore > 1.0) {
            finalScore = 1.0;
        } else if (finalScore < 0.0) {
            finalScore = 0.0;
        }

        return finalScore;
    }

    // AddMetricResult 添加指标结果
    public void addMetricResult(String name, MetricResult result) {
        this.metricResults.put(name, result);
    }

    // GetIssues 获取所有问题
    public List<Issue> getIssues() {
        List<Issue> issues = new LinkedList<>();
        for(MetricResult metricResult: metricResults.values()){
            if (metricResult == null || metricResult.getIssues() == null) {
                continue;
            }
            issues.addAll(metricResult.getIssues());
        }
        return issues;
    }
}
