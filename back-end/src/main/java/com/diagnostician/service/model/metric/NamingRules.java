package com.diagnostician.service.model.metric;

import java.util.regex.Pattern;

public class NamingRules {

    private static final Pattern LOWER_CASE = Pattern.compile("[a-z0-9]+");
    private static final Pattern CAMEL_CASE = Pattern.compile("[a-z][a-zA-Z0-9]*");
    private static final Pattern PASCAL_CASE = Pattern.compile("[A-Z][a-zA-Z0-9]*");
    private static final Pattern UPPER_SNAKE_CASE = Pattern.compile("[A-Z0-9_]+");
    private static final Pattern PACKAGE_NAME = Pattern.compile("^[a-z]+(\\.[a-z][a-z0-9]*)*$");

    public static boolean isLowerCase(String name) {
        return LOWER_CASE.matcher(name).matches();
    }

    public static boolean isCamelCase(String name) {
        if (name == null || name.isEmpty()) return false;
        if (Character.isUpperCase(name.charAt(0)) || name.contains("_")) return false;
        return CAMEL_CASE.matcher(name).matches();
    }

    public static boolean isPascalCase(String name) {
        if (name == null || name.isEmpty()) return false;
        if (Character.isLowerCase(name.charAt(0)) || name.contains("_")) return false;
        return PASCAL_CASE.matcher(name).matches();
    }

    public static boolean isUpperSnakeCase(String name) {
        return UPPER_SNAKE_CASE.matcher(name).matches();
    }

    public static boolean isValidPackageName(String name) {
        return PACKAGE_NAME.matcher(name).matches();
    }

    public static boolean isValidFuncName(String name) {
        return isCamelCase(name);
    }

    public static boolean isValidTypeName(String name) {
        return isPascalCase(name);
    }

    public static boolean isValidConstName(String name) {
        return isUpperSnakeCase(name) || isPascalCase(name);
    }

    public static boolean isValidVarName(String name) {
        return isCamelCase(name) || isLowerCase(name);
    }
}
