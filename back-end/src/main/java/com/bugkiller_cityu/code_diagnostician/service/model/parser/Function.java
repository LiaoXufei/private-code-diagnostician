package com.bugkiller_cityu.code_diagnostician.service.model.parser;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
// Function Information
public class Function {
    String name;        // Function Name
    int startLine;      // Start Line
    int endLine;        // End Line
    int complexity;     // Complexity
    int parameters;     // Number of Parameters
    Object node;        // AST node (optional)
}
