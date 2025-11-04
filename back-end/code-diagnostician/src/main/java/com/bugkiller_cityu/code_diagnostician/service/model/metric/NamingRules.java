package com.bugkiller_cityu.code_diagnostician.service.model.metric;

import java.util.regex.Pattern;

public class NamingRules {

    // 是否是小写命名（包名）
    public static boolean isLowerCase(String name) {
        return name.matches("[a-z0-9]+");
    }

    // 是否是小驼峰命名 (camelCase)
    public static boolean isCamelCase(String name) {
        if (name == null || name.isEmpty()) return false;
        if (Character.isUpperCase(name.charAt(0)) || name.contains("_")) return false;
        return name.matches("[a-z][a-zA-Z0-9]*");
    }

    // 是否是帕斯卡命名 (PascalCase)
    public static boolean isPascalCase(String name) {
        if (name == null || name.isEmpty()) return false;
        if (Character.isLowerCase(name.charAt(0)) || name.contains("_")) return false;
        return name.matches("[A-Z][a-zA-Z0-9]*");
    }

    // 是否是全大写蛇形命名 (UPPER_SNAKE_CASE)
    public static boolean isUpperSnakeCase(String name) {
        return Pattern.matches("[A-Z0-9_]+", name);
    }

    // 包名规范
    public static boolean isValidPackageName(String name) {
        return isLowerCase(name) && !name.contains("_");
    }

    // 函数名规范（驼峰）
    public static boolean isValidFuncName(String name) {
        return isCamelCase(name);
    }

    // 类型名规范（帕斯卡）
    public static boolean isValidTypeName(String name) {
        return isPascalCase(name);
    }

    // 常量名规范（全大写或帕斯卡）
    public static boolean isValidConstName(String name) {
        return isUpperSnakeCase(name) || isPascalCase(name);
    }

    // 变量名规范（小驼峰或小写）
    public static boolean isValidVarName(String name) {
        return isCamelCase(name) || isLowerCase(name);
    }
}



