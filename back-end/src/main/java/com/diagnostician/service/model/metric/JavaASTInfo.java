package com.diagnostician.service.model.metric;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JavaASTInfo {
    private CompilationUnit compilationUnit; // AST结点
    private Set<File> fileSet;
    private byte[] content;
}
