package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import java.util.regex.Pattern;

public class NamingRules {
    // ======== 命名规范判断主接口 ========

    // 包名：全部小写、无下划线
    public static boolean isValidPackageName(String name) {
        return name != null && name.matches("^[a-z][a-z0-9\\.]*$") && !name.contains("_");
    }
    // 类型名（类/接口/枚举）：PascalCase（大写开头）
    public static boolean isValidTypeName(String name) {
        return isPascalCase(name);
    }
    // 方法名：camelCase（小写开头）
    public static boolean isValidMethodName(String name) {
        return isCamelCase(name);
    }
    // 变量名：camelCase 或 全小写
    public static boolean isValidVariableName(String name) {
        return isCamelCase(name) || isLowerCase(name);
    }
    // 常量名：UPPER_SNAKE_CASE
    public static boolean isValidConstName(String name) {
        return isUpperSnakeCase(name);
    }
    // ======== 规则基础检查函数 ========

    // 小写命名（常用于包名）
    public static boolean isLowerCase(String name) {
        return name != null && name.matches("^[a-z][a-z0-9]*$");
    }
    // camelCase（方法名、变量名）
    public static boolean isCamelCase(String name) {
        return name != null && Pattern.matches("^[a-z][a-zA-Z0-9]*$", name)
                && !name.contains("_");
    }
    // PascalCase（类名）
    public static boolean isPascalCase(String name) {
        return name != null && Pattern.matches("^[A-Z][a-zA-Z0-9]*$", name)
                && !name.contains("_");
    }

    // UPPER_SNAKE_CASE（常量）
    public static boolean isUpperSnakeCase(String name) {
        return name != null && Pattern.matches("^[A-Z][A-Z0-9_]*$", name);
    }
}
