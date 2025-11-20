package com.bugkiller_cityu.code_diagnostician.service;

import org.springframework.stereotype.Service;

import com.bugkiller_cityu.code_diagnostician.service.model.common.LanguageType;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParserFactory;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParserResult;

@Service
public interface CodeService {
}

@Service
class CodeServiceImpl implements CodeService{
    
    public ParserResult analyzeJavaCode(String filePath, byte[] content) {
        try {
            Parser parser = ParserFactory.createParser(LanguageType.JAVA);
            return parser.parse(filePath, content);
        } catch (Exception e) {
            // Handle parsing errors
            e.printStackTrace();
            return null;
        }
    }

}
