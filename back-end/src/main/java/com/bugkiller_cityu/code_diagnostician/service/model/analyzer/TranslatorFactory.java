package com.bugkiller_cityu.code_diagnostician.service.model.analyzer;

import org.springframework.stereotype.Component;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.Language;
import com.bugkiller_cityu.code_diagnostician.service.model.analyzer.Analyzer.Translator;

@Component
public class TranslatorFactory {
    public Translator createTranslator(Language language) {
        return new DefaultTranslator(language);
    }
}