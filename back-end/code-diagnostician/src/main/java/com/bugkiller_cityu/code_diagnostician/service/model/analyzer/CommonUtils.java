package com.bugkiller_cityu.code_diagnostician.service.model.analyzer;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 文件查找工具，为目录分析提供源码文件列表
public class CommonUtils {
    public static List<String> findSourceFiles(
            String root,
            List<String> includePatterns,
            List<String> excludePatterns,
            Consumer<Integer> progressCallback
    ) throws IOException {
        Path start = Paths.get(root);
        if (!Files.exists(start)) {
            return List.of();
        }

        List<Pattern> includes = toRegexPatterns(includePatterns);
        List<Pattern> excludes = toRegexPatterns(excludePatterns);

        List<String> results = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(start, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)) {
            List<Path> paths = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> isSourceExtension(p))
                    .filter(p -> matchesPosix(start, p, includes, true))
                    .filter(p -> !matchesPosix(start, p, excludes, false))
                    .collect(Collectors.toList());

            for (int i = 0; i < paths.size(); i++) {
                results.add(paths.get(i).toString());
                if (progressCallback != null) {
                    progressCallback.accept(i + 1);
                }
            }
        }
        return results;
    }

    private static boolean isSourceExtension(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        return name.endsWith(".java") || name.endsWith(".go") || name.endsWith(".js") ||
               name.endsWith(".ts") || name.endsWith(".py") || name.endsWith(".c") ||
               name.endsWith(".cpp") || name.endsWith(".cs") || name.endsWith(".rs") ||
               name.endsWith(".lua");
    }

    private static List<Pattern> toRegexPatterns(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) return List.of();
        return patterns.stream()
                .map(CommonUtils::globToRegex)
                .map(r -> Pattern.compile("^" + r + "$"))
                .collect(Collectors.toList());
    }

    private static boolean matchesPosix(Path root, Path p, List<Pattern> patterns, boolean emptyMeansAll) {
        if (patterns == null || patterns.isEmpty()) return emptyMeansAll;
        Path rel = root.relativize(p);
        String s = rel.toString().replace('\\','/');
        for (Pattern pattern : patterns) {
            if (pattern.matcher(s).matches()) return true;
        }
        return false;
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            if (c == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    sb.append(".*");
                    i++;
                } else {
                    sb.append("[^/]*");
                }
            } else if (c == '?') {
                sb.append("[^/]");
            } else if (".\\[]{}()+-^$|".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }
}