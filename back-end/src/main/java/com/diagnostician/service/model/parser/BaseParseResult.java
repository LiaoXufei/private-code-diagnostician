package com.diagnostician.service.model.parser;

import com.diagnostician.service.model.common.LanguageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseParseResult implements ParseResult {
    private List<Function> functions;
    private int commentLines;
    private int totalLines;
    private LanguageType language;
    private Object ASTRoot;
    private byte[] content;

}
