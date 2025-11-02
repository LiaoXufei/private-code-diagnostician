package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@ToString
// CodeDuplicationMetric 评估代码重复度
public class CodeDuplicationMetric extends BaseMetric {

    public CodeDuplicationMetric() {
        super(
                "代码重复度",
                "评估代码中重复逻辑的比例，重复代码越多，越需要抽象和重构",
                0.15
        );

    }

    /**
     * 查找重复函数并生成问题列表
     *
     * @param similarFunctions 键为函数签名，值为相似函数列表
     * @param issues           存储问题描述的列表
     * @return 重复函数的数量
     */
    public int findDuplicatedFunctions(Map<String, List<String>> similarFunctions, List<String> issues) {
        int duplicatedCount = 0;

        for (Map.Entry<String, List<String>> entry : similarFunctions.entrySet()) {
            String signature = entry.getKey();
            List<String> functions = entry.getValue();

            // 忽略太短的函数签名
            if (functions.size() > 1 && signature.length() > 10) {
                duplicatedCount += functions.size() - 1;

                // 拼接函数名列表
                String joinedFunctions = String.join(", ", functions);

                // 添加问题描述
                String issueMsg = String.format("可能存在重复实现: %s", joinedFunctions);
                issues.add(issueMsg);
            }
        }

        return duplicatedCount;
    }

    /**
     * 根据重复率计算得分
     * 基础分 0.4，每增加 1% 重复率增加 0.1 分（即 duplicationRate * 10）
     * 最大得分为 1.0
     *
     * @param duplicationRate 重复率（0~1）
     * @return 计算后的得分（0~1）
     */
    public double calculateScore(double duplicationRate) {
        double baseScore = 0.4;
        double increasePerPercent = 10.0;

        double score = baseScore + (duplicationRate * increasePerPercent);

        // 限制范围
        return Math.min(score, 1.0);
    }

}
