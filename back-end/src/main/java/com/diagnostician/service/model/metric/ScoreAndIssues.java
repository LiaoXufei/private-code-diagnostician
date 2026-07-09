package com.diagnostician.service.model.metric;

import com.diagnostician.entity.Issue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreAndIssues {
    private double score;
    private List<Issue> issues;
}
