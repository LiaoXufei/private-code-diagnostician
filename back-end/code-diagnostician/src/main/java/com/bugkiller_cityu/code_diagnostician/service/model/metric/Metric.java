package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;

// Metric 代码质量指标接口
public interface Metric {
    // Name 返回指标名称
    String getName();

    // Description 返回指标描述
    String getDescription();

    // Weight 返回指标权重
    double getWeight();

    // Analyze 分析代码并返回结果
    MetricResult analyze(ParseResult parseResult);
}
