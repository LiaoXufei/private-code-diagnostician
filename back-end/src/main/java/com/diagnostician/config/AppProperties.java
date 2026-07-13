package com.diagnostician.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 应用级配置（上传目录等）。
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 上传根目录，可为相对路径（相对进程工作目录）或绝对路径 */
    private String uploadDir = "uploads";

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public Path uploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path projectDir(String projectId) {
        return uploadRoot().resolve(projectId);
    }

    public Path resolveStoredFile(String projectId, String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String basename = java.nio.file.Paths.get(storedFilename).getFileName().toString();
        if (basename.isBlank() || basename.contains("..")) {
            throw new IllegalArgumentException("非法文件名");
        }
        Path base = projectDir(projectId).normalize();
        Path resolved = base.resolve(basename).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return resolved;
    }
}
