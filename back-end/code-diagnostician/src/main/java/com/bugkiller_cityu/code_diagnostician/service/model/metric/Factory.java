package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
@NoArgsConstructor
public class Factory {
    public List<Metric> createAllMetrics() {
        return new LinkedList<>() {
            {
                add(createFunctionLengthMetric());
                add(createNamingConventionMetric());
                add(createCommentCoverageMetric());
                add(createCodeDuplicationMetric());
                add(createStructureAnalysisMetric());
                add(createCyclomaticComplexityMetric());
                add(createErrorHandlingMetric());
            }
        };
    }

    public Metric createFunctionLengthMetric(){
        return new FunctionLengthMetric();
    }
    public Metric createNamingConventionMetric(){
        return new NamingConventionMetric();
    }
    public Metric createCommentCoverageMetric(){
        return new CommentCoverageMetric();
    }
    public Metric createCodeDuplicationMetric(){
        return new CodeDuplicationMetric();
    }
    public Metric createStructureAnalysisMetric(){
        return new StructureAnalysisMetric();
    }
    public Metric createCyclomaticComplexityMetric(){
        return new CyclomaticComplexityMetric();
    }
    public Metric createErrorHandlingMetric(){
        return new ErrorHandlingMetric();
    }

}

