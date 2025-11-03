package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import lombok.*;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
// BaseMetric 提供指标的基础实现
public class BaseMetric {
    String name;
    String description;
    double weight;

}
