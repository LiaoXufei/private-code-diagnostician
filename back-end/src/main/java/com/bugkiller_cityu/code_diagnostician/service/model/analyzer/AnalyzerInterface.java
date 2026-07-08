package com.bugkiller_cityu.code_diagnostician.service.model.analyzer;

import java.util.List;

import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.AnalysisResult;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.AnalysisException;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.Language;

/**
 * 代码分析器接口
 */
public interface AnalyzerInterface {
    /**
     * 分析指定路径的代码
     * @param path 要分析的路径
     * @return 分析结果
     * @throws AnalysisException 分析失败时抛出
     */
    AnalysisResult analyze(String path) throws AnalysisException;

    /**
     * 分析单个文件
     * @param filePath 文件路径
     * @return 分析结果
     * @throws AnalysisException 分析失败时抛出
     */
    AnalysisResult analyzeFile(String filePath) throws AnalysisException;

    /**
     * 使用指定的包含/排除模式分析目录
     * @param path 要分析的路径
     * @param includePatterns 包含模式列表
     * @param excludePatterns 排除模式列表
     * @return 分析结果
     * @throws AnalysisException 分析失败时抛出
     */
    AnalysisResult analyzeWithExcludes(String path,
                                       List<String> includePatterns,
                                       List<String> excludePatterns) throws AnalysisException;

    /**
     * 设置分析器使用的语言
     * @param lang 语言类型
     */
    void setLanguage(Language lang);

    /**
     * 设置静默模式
     * @param silent 是否静默（不输出进度信息）
     */
    void setSilent(boolean silent);
}