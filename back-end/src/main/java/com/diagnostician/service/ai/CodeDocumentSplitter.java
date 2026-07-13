package com.diagnostician.service.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CodeDocumentSplitter {

    private static final Pattern JAVA_METHOD = Pattern.compile(
            "(?m)^\\s*(public|protected|private|static|final|synchronized|native|abstract|default|\\s)+[\\w<>\\[\\],\\s]+\\s+\\w+\\s*\\([^)]*\\)\\s*(throws\\s+[\\w,\\s]+)?\\s*\\{"
    );

    public List<String> splitByFunctions(String filename, String content, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return chunks;
        }

        Matcher matcher = JAVA_METHOD.matcher(content);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }

        if (starts.isEmpty()) {
            chunks.addAll(splitByLines(filename, content, maxChunkSize));
            return chunks;
        }

        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : content.length();
            String chunk = content.substring(start, end).trim();
            if (!chunk.isBlank()) {
                if (chunk.length() > maxChunkSize) {
                    chunks.addAll(splitByLines(filename + "#part" + i, chunk, maxChunkSize));
                } else {
                    chunks.add(chunk);
                }
            }
        }
        return chunks;
    }

    public List<String> splitByLines(String label, String content, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] lines = content.split("\n", -1);
        StringBuilder current = new StringBuilder();
        int lineStart = 1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] + "\n";
            if (current.length() + line.length() > maxChunkSize && !current.isEmpty()) {
                chunks.add(formatChunk(label, lineStart, i, current.toString()));
                current = new StringBuilder();
                lineStart = i + 1;
            }
            current.append(line);
        }
        if (!current.isEmpty()) {
            chunks.add(formatChunk(label, lineStart, lines.length, current.toString()));
        }
        return chunks;
    }

    private String formatChunk(String label, int fromLine, int toLine, String body) {
        return "File: " + label + " (lines " + fromLine + "-" + toLine + ")\n" + body;
    }
}
