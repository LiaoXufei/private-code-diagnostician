package com.bugkiller_cityu.code_diagnostician.service.model.analyzer;

import com.bugkiller_cityu.code_diagnostician.service.MetricService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.CodeAnalyzer;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.Translator;
import com.bugkiller_cityu.code_diagnostician.service.model.metric.Factory;

/**
 * Analyzer模块的Spring配置类
 * 负责定义和配置分析器相关的Spring Bean
 *
 * Spring框架说明：
 * - @Configuration: 表示这是一个配置类，Spring会在启动时加载它
 * - @Bean: 表示方法返回的对象应该被注册为Spring容器中的Bean
 * - @Primary: 当有多个相同类型的Bean时，标记这个为首选Bean
 * - Bean方法的参数会自动进行依赖注入，不需要@Autowired注解
 */
@Configuration
public class AnalyzerConfiguration {

    /**
     * 创建默认分析器Bean
     *
     * Spring会自动注入所需的依赖：
     * @param codeAnalyzer 代码分析引擎（由下面的codeAnalyzer()方法提供）
     * @param translator 翻译器（由TranslatorFactory创建或DefaultTranslator提供）
     * @return 配置好的默认分析器实例
     */
    @Bean
    @Primary
    public Analyzer defaultAnalyzer(CodeAnalyzer codeAnalyzer, Translator translator) {
        return new Analyzer.DefaultAnalyzer(codeAnalyzer, translator);
    }

    /**
     * 创建代码分析引擎Bean
     *
     * @param translator 翻译器（自动注入）
     * @param factory 指标工厂（自动注入，来自metric模块的Factory类）
     * @return 配置好的代码分析引擎实例
     */
    @Bean
    public CodeAnalyzer codeAnalyzer(Translator translator, Factory factory, MetricService metricService) {
        // 注意：Analyzer.CodeAnalyzer的构造函数期望MetricFactory类型
        // 但实际上metric模块使用的是Factory类名
        // 这里需要修复Analyzer.java中的类型引用
        return new Analyzer.CodeAnalyzer(translator, factory, metricService);
    }

    
}