package com.diagnostician.service.model.metric;

import com.diagnostician.entity.Issue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MetricResult {
    double score;           // 得分 (0-1，越低越好)
    List<Issue> issues;    // 问题列表
    String description;     // 结果描述
    double weight;          // 权重 (0-1)
}
