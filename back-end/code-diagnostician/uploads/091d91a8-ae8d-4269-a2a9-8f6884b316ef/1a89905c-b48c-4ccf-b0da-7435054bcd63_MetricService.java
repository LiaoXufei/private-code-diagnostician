package com.bugkiller_cityu.code_diagnostician.service;

import com.bugkiller_cityu.code_diagnostician.service.model.metric.AnalysisResult;
import com.bugkiller_cityu.code_diagnostician.service.model.metric.Factory;
import com.bugkiller_cityu.code_diagnostician.service.model.metric.Metric;
import com.bugkiller_cityu.code_diagnostician.service.model.metric.MetricResult;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

public interface MetricService{
    /**
     * 应用每个指标进行分析
     *
     * @param parseResult 文件解析的结果
     * @return 所有指标的分析结果
     */
    AnalysisResult analyzeAllMetrics(ParseResult parseResult);
}


@Service
class MetricServiceImpl implements MetricService{
    @Autowired
    private final Factory factory;

    public MetricServiceImpl(Factory factory){
        this.factory = factory;
    }

    public AnalysisResult analyzeAllMetrics(ParseResult parseResult){
        List<Metric> allMetrics = factory.createAllMetrics();
        AnalysisResult analysisResult = new AnalysisResult();

        for (Metric metric : allMetrics){
            MetricResult metricResult = metric.analyze(parseResult);
            analysisResult.addMetricResult(metric.getName(), metricResult);
        }

        return analysisResult;
    }

}
