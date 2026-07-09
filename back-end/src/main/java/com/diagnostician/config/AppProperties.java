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
        return projectDir(projectId).resolve(storedFilename);
    }
}
