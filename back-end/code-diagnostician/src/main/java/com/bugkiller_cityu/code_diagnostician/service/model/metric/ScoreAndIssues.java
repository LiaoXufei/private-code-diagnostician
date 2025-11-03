package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreAndIssues {
    private double score;
    private List<String> issues;
}
