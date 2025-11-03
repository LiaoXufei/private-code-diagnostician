package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
@NoArgsConstructor
@AllArgsConstructor
public class Factory {
    public List<Metric> createAllMetrics() {
        return new LinkedList<Metric>(){
            {
                add(createFunctionLength());
            }
        };
    }

    public Metric createFunctionLength(){
        return new FunctionLengthMetric();
    }

}
