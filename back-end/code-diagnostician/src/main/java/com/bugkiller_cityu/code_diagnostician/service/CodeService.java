package com.bugkiller_cityu.code_diagnostician.service;

import com.bugkiller_cityu.code_diagnostician.service.model.parser.Parser;
import org.springframework.stereotype.Service;

import com.bugkiller_cityu.code_diagnostician.service.model.common.LanguageType;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParserFactory;
import com.bugkiller_cityu.code_diagnostician.service.model.parser.ParseResult;

@Service
public interface CodeService {
}

@Service
class CodeServiceImpl implements CodeService{
    
    public ParseResult analyzeJavaCode(String filePath, byte[] content) {
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
