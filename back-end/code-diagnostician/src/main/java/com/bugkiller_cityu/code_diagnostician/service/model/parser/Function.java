package com.bugkiller_cityu.code_diagnostician.service.model.parser;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
// Function 函数信息
public class Function {
    String name;        // 函数名
    int startLine;      // 开始行
    int endLine;        // 结束行
    int complexity;     // 复杂度
    int parameters;     // 参数数量
    Object node;        // AST节点(可选)
}
