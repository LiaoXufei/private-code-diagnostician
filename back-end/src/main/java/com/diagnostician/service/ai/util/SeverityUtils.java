package com.diagnostician.service.ai.util;

public final class SeverityUtils {

    private SeverityUtils() {
    }

    public static int severityRank(String severity) {
        if ("high".equalsIgnoreCase(severity) || "critical".equalsIgnoreCase(severity)) {
            return 0;
        }
        if ("medium".equalsIgnoreCase(severity) || "major".equalsIgnoreCase(severity)) {
            return 1;
        }
        return 2;
    }
}
