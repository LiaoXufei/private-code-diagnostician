package com.bugkiller_cityu.code_diagnostician.controller;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class ProjectController {
    private final Map<String, Map<String, Object>> projects = new LinkedHashMap<>();
    private final Map<String, List<Map<String, Object>>> filesByProject = new HashMap<>();
    private final Map<String, List<Map<String, Object>>> analysesByProject = new HashMap<>();

    public ProjectController() {
        Map<String, Object> p1 = new LinkedHashMap<>();
        p1.put("id", "proj-high-001");
        p1.put("name", "高质量电商项目");
        p1.put("description", "示例：结构清晰、测试完善、规范良好");
        p1.put("language", "javascript");
        p1.put("status", "completed");
        p1.put("created_at", Instant.now().minusSeconds(2 * 24 * 3600).toString());
        p1.put("updated_at", Instant.now().toString());
        p1.put("file_count", 5);
        projects.put((String) p1.get("id"), p1);

        Map<String, Object> p2 = new LinkedHashMap<>();
        p2.put("id", "proj-low-001");
        p2.put("name", "低质量教学系统");
        p2.put("description", "示例：复杂度高、重复多、规范较差");
        p2.put("language", "javascript");
        p2.put("status", "active");
        p2.put("created_at", Instant.now().minusSeconds(24 * 3600).toString());
        p2.put("updated_at", Instant.now().toString());
        p2.put("file_count", 5);
        projects.put((String) p2.get("id"), p2);

        filesByProject.put(p1.get("id").toString(), new ArrayList<>(List.of(
                fileItem("src/main.js", p1.get("id").toString(), 2456),
                fileItem("src/App.vue", p1.get("id").toString(), 3892),
                fileItem("src/components/ProductList.vue", p1.get("id").toString(), 5634),
                fileItem("src/utils/formatters.js", p1.get("id").toString(), 1234),
                fileItem("src/api/products.js", p1.get("id").toString(), 2876)
        )));
        filesByProject.put(p2.get("id").toString(), new ArrayList<>(List.of(
                fileItem("src/main.js", p2.get("id").toString(), 1823),
                fileItem("src/App.vue", p2.get("id").toString(), 2156),
                fileItem("src/student-service.js", p2.get("id").toString(), 3456),
                fileItem("src/auth.js", p2.get("id").toString(), 1567),
                fileItem("components/StudentForm.vue", p2.get("id").toString(), 4234)
        )));

        Map<String, Object> highAnalysis = new LinkedHashMap<>();
        highAnalysis.put("id", UUID.randomUUID().toString());
        highAnalysis.put("overall_score", 92);
        highAnalysis.put("grade", "excellent");
        highAnalysis.put("dimensions", List.of(
                dim("代码复杂度", 88, 100, 12),
                dim("可维护性", 90, 100, 8),
                dim("代码重复率", 85, 100, 6),
                dim("安全性", 95, 100, 2),
                dim("测试覆盖率", 89, 100, 5),
                dim("代码规范", 93, 100, 3),
                dim("性能", 90, 100, 4)
        ));
        highAnalysis.put("issues", List.of());
        highAnalysis.put("created_at", Instant.now().minusSeconds(2 * 24 * 3600).toString());
        analysesByProject.put(p1.get("id").toString(), new ArrayList<>(List.of(highAnalysis)));
        p1.put("last_analysis_id", highAnalysis.get("id"));

        Map<String, Object> lowAnalysis = new LinkedHashMap<>();
        lowAnalysis.put("id", UUID.randomUUID().toString());
        lowAnalysis.put("overall_score", 58);
        lowAnalysis.put("grade", "poor");
        lowAnalysis.put("dimensions", List.of(
                dim("代码复杂度", 42, 100, 38),
                dim("代码规范", 55, 100, 68),
                dim("代码重复率", 60, 100, 23),
                dim("安全性", 65, 100, 12),
                dim("测试覆盖率", 25, 100, 45),
                dim("可维护性", 42, 100, 38),
                dim("性能", 68, 100, 15)
        ));
        lowAnalysis.put("issues", List.of());
        lowAnalysis.put("created_at", Instant.now().minusSeconds(24 * 3600).toString());
        analysesByProject.put(p2.get("id").toString(), new ArrayList<>(List.of(lowAnalysis)));
        p2.put("last_analysis_id", lowAnalysis.get("id"));
    }

    @GetMapping("/projects")
    public List<Map<String, Object>> listProjects() {
        return projects.values().stream().map(p -> {
            Object lastId = p.get("last_analysis_id");
            if (lastId != null) {
                Map<String, Object> last = findAnalysis(p.get("id").toString(), lastId.toString());
                if (last != null) {
                    Map<String, Object> lastAnalysis = new LinkedHashMap<>();
                    lastAnalysis.put("overallScore", last.get("overall_score"));
                    lastAnalysis.put("grade", last.get("grade"));
                    lastAnalysis.put("completedAt", last.get("created_at"));
                    lastAnalysis.put("taskId", last.get("id"));
                    Map<String, Object> copy = new LinkedHashMap<>(p);
                    copy.put("lastAnalysis", lastAnalysis);
                    return copy;
                }
            }
            return p;
        }).collect(Collectors.toList());
    }

    @PostMapping("/projects")
    public Map<String, Object> createProject(@RequestBody Map<String, Object> body) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", id);
        p.put("name", String.valueOf(body.getOrDefault("name", "新项目")));
        p.put("description", String.valueOf(body.getOrDefault("description", "")));
        p.put("language", String.valueOf(body.getOrDefault("language", "javascript")));
        p.put("status", "active");
        p.put("created_at", Instant.now().toString());
        p.put("updated_at", Instant.now().toString());
        p.put("file_count", 0);
        projects.put(id, p);
        return p;
    }

    @GetMapping("/projects/{id}")
    public Map<String, Object> getProject(@PathVariable String id) {
        Map<String, Object> p = projects.get(id);
        if (p == null) return Map.of("error", "项目不存在");
        Object lastId = p.get("last_analysis_id");
        if (lastId != null) {
            Map<String, Object> last = findAnalysis(id, lastId.toString());
            if (last != null) {
                Map<String, Object> lastAnalysis = new LinkedHashMap<>();
                lastAnalysis.put("overallScore", last.get("overall_score"));
                lastAnalysis.put("grade", last.get("grade"));
                lastAnalysis.put("completedAt", last.get("created_at"));
                lastAnalysis.put("taskId", last.get("id"));
                Map<String, Object> copy = new LinkedHashMap<>(p);
                copy.put("lastAnalysis", lastAnalysis);
                return copy;
            }
        }
        return p;
    }

    @GetMapping("/projects/{id}/files")
    public Map<String, Object> listFiles(@PathVariable String id) {
        List<Map<String, Object>> files = filesByProject.getOrDefault(id, new ArrayList<>());
        int totalFiles = files.size();
        long totalSize = files.stream().mapToLong(f -> Long.parseLong(String.valueOf(f.getOrDefault("file_size", 0)))).sum();
        projects.computeIfPresent(id, (k, p) -> {
            p.put("file_count", totalFiles);
            p.put("updated_at", Instant.now().toString());
            return p;
        });
        return Map.of("files", files, "totalFiles", totalFiles, "totalSize", totalSize);
    }

    @GetMapping("/projects/{id}/files/content")
    public Map<String, Object> getFileContent(@PathVariable String id, @RequestParam("path") String path) throws IOException {
        String filename = Paths.get(path).getFileName().toString();
        Path abs = Paths.get("uploads").resolve(id).resolve(filename);
        String content;
        long size = 0;
        if (Files.exists(abs)) {
            content = Files.readString(abs);
            size = Files.size(abs);
        } else {
            content = "示例文件内容: " + filename;
        }
        return Map.of("path", path, "content", content, "size", size);
    }

    @PostMapping(value = "/projects/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@PathVariable String id, @RequestPart("files") MultipartFile[] files) throws IOException {
        List<Map<String, Object>> list = filesByProject.computeIfAbsent(id, k -> new ArrayList<>());
        Path base = Paths.get("uploads").resolve(id);
        Files.createDirectories(base);
        List<Map<String, Object>> uploaded = new ArrayList<>();
        String uploadTime = Instant.now().toString();
        for (MultipartFile f : files) {
            String original = StringUtils.cleanPath(Objects.requireNonNull(f.getOriginalFilename()));
            String name = UUID.randomUUID() + "_" + original;
            Path target = base.resolve(name);
            Files.copy(f.getInputStream(), target);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", UUID.randomUUID().toString());
            item.put("project_id", id);
            item.put("filename", original);
            item.put("file_path", "/uploads/" + id + "/" + name);
            item.put("file_size", f.getSize());
            item.put("upload_time", uploadTime);
            list.add(item);
            uploaded.add(item);
        }
        projects.computeIfPresent(id, (k, p) -> {
            p.put("file_count", list.size());
            p.put("updated_at", Instant.now().toString());
            return p;
        });
        return Map.of("message", "成功上传 " + uploaded.size() + " 个文件", "uploadedFiles", uploaded, "errors", List.of(), "totalFiles", files.length);
    }

    @DeleteMapping("/projects/{id}/files/{fileId}")
    public Map<String, Object> deleteFile(@PathVariable String id, @PathVariable String fileId) {
        List<Map<String, Object>> list = filesByProject.getOrDefault(id, new ArrayList<>());
        Map<String, Object> found = null;
        for (Map<String, Object> f : list) {
            if (Objects.equals(f.get("id"), fileId)) {
                found = f;
                break;
            }
        }
        if (found == null) return Map.of("error", "文件不存在");
        String filePath = String.valueOf(found.getOrDefault("file_path", ""));
        String filename = Paths.get(filePath).getFileName().toString();
        Path abs = Paths.get("uploads").resolve(id).resolve(filename);
        try {
            Files.deleteIfExists(abs);
        } catch (IOException ignored) {}
        list.remove(found);
        projects.computeIfPresent(id, (k, p) -> {
            p.put("file_count", list.size());
            p.put("updated_at", Instant.now().toString());
            return p;
        });
        return Map.of("message", "文件删除成功", "fileId", fileId, "deletedFile", Map.of("id", found.get("id"), "filename", found.get("filename"), "file_path", found.get("file_path")));
    }

    @DeleteMapping("/projects/{id}/files")
    public Map<String, Object> deleteFileByPath(@PathVariable String id, @RequestParam("path") String path) {
        List<Map<String, Object>> list = filesByProject.getOrDefault(id, new ArrayList<>());
        Map<String, Object> found = null;
        for (Map<String, Object> f : list) {
            if (Objects.equals(String.valueOf(f.getOrDefault("file_path", "")), path)) {
                found = f;
                break;
            }
        }
        if (found == null) return Map.of("error", "文件不存在");
        String filename = Paths.get(path).getFileName().toString();
        Path abs = Paths.get("uploads").resolve(id).resolve(filename);
        try { Files.deleteIfExists(abs); } catch (IOException ignored) {}
        list.remove(found);
        projects.computeIfPresent(id, (k, p) -> {
            p.put("file_count", list.size());
            p.put("updated_at", Instant.now().toString());
            return p;
        });
        return Map.of("message", "文件删除成功", "deletedFile", Map.of("id", found.get("id"), "filename", found.get("filename"), "file_path", found.get("file_path")));
    }

    @PostMapping("/projects/{id}/analyze")
    public Map<String, Object> analyze(@PathVariable String id) {
        Map<String, Object> p = projects.get(id);
        if (p == null) return Map.of("error", "项目不存在");
        Map<String, Object> mock = new LinkedHashMap<>();
        mock.put("id", UUID.randomUUID().toString());
        mock.put("overall_score", 70 + new Random().nextInt(30));
        mock.put("grade", "good");
        mock.put("dimensions", List.of(
                dim("代码复杂度", 75, 100, 3),
                dim("可维护性", 78, 100, 2),
                dim("重复度", 82, 100, 1),
                dim("安全性", 85, 100, 1),
                dim("性能", 70, 100, 4),
                dim("代码风格", 80, 100, 2),
                dim("测试覆盖", 65, 100, 6)
        ));
        mock.put("issues", List.of(Map.of("id", UUID.randomUUID().toString(), "type", "complexity", "severity", "medium", "file", "/src/components/Example.vue", "line", 45, "message", "函数复杂度过高", "suggestion", "建议拆分为多个小函数")));
        mock.put("created_at", Instant.now().toString());
        analysesByProject.computeIfAbsent(id, k -> new ArrayList<>()).add(mock);
        p.put("last_analysis_id", mock.get("id"));
        p.put("status", "completed");
        p.put("updated_at", Instant.now().toString());
        return Map.of("message", "分析完成", "analysisId", mock.get("id"), "result", Map.of(
                "id", mock.get("id"),
                "projectId", id,
                "overallScore", mock.get("overall_score"),
                "grade", mock.get("grade"),
                "dimensions", mock.get("dimensions"),
                "issues", mock.get("issues"),
                "created_at", mock.get("created_at")
        ));
    }

    @DeleteMapping("/projects/{id}/analyze")
    public Map<String, Object> cancelAnalyze(@PathVariable String id) {
        Map<String, Object> p = projects.get(id);
        if (p == null) return Map.of("error", "项目不存在");
        p.put("status", "cancelled");
        p.put("updated_at", Instant.now().toString());
        return Map.of("message", "已取消分析", "projectId", id, "status", "cancelled");
    }

    @PostMapping("/projects/{id}/git")
    public Map<String, Object> importGit(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String repo = String.valueOf(body.getOrDefault("repo", ""));
        projects.computeIfPresent(id, (k, p) -> {
            p.put("updated_at", Instant.now().toString());
            return p;
        });
        return Map.of("message", "导入成功", "repo", repo, "taskId", UUID.randomUUID().toString());
    }

    @GetMapping("/projects/{id}/analysis")
    public Map<String, Object> getAnalysis(@PathVariable String id) {
        List<Map<String, Object>> list = analysesByProject.getOrDefault(id, new ArrayList<>());
        if (list.isEmpty()) return Map.of("error", "该项目暂无分析结果");
        Map<String, Object> analysis = list.get(list.size() - 1);
        Map<String, Object> project = projects.get(id);
        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) analysis.get("dimensions");
        List<Map<String, Object>> issues = (List<Map<String, Object>>) analysis.get("issues");
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("fileCount", project.get("file_count"));
        metrics.put("totalIssues", issues.size());
        metrics.put("criticalIssues", issues.stream().filter(i -> Objects.equals(i.get("severity"), "critical")).count());
        metrics.put("majorIssues", issues.stream().filter(i -> Objects.equals(i.get("severity"), "major")).count());
        metrics.put("minorIssues", issues.stream().filter(i -> Objects.equals(i.get("severity"), "minor")).count());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("suggestions", dimensions.stream().map(d -> String.format("优化%s相关代码", String.valueOf(d.get("name")))).collect(Collectors.toList()));
        return Map.of(
                "projectId", id,
                "projectName", String.valueOf(project.get("name")),
                "overallScore", analysis.get("overall_score"),
                "grade", analysis.get("grade"),
                "dimensions", dimensions,
                "issues", issues,
                "completedAt", analysis.get("created_at"),
                "taskId", analysis.get("id"),
                "metrics", metrics,
                "summary", summary
        );
    }

    @GetMapping("/projects/{id}/analysis/status")
    public Map<String, Object> getStatus(@PathVariable String id) {
        Map<String, Object> p = projects.get(id);
        if (p == null) return Map.of("error", "项目不存在");
        String status = String.valueOf(p.getOrDefault("status", "idle"));
        int progress = Objects.equals(status, "completed") ? 100 : 0;
        return Map.of("projectId", id, "status", status, "progress", progress, "updatedAt", p.get("updated_at"));
    }

    @GetMapping("/projects/{id}/analysis/logs")
    public Map<String, Object> getLogs(@PathVariable String id) {
        Map<String, Object> p = projects.get(id);
        if (p == null) return Map.of("error", "项目不存在");
        List<Map<String, Object>> logs = new ArrayList<>();
        logs.add(Map.of("timestamp", p.get("created_at"), "level", "info", "message", "项目创建: " + p.get("name"), "source", "system"));
        List<Map<String, Object>> list = analysesByProject.getOrDefault(id, new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> a = list.get(i);
            logs.add(Map.of("timestamp", a.get("created_at"), "level", "info", "message", "分析完成 #" + (list.size() - i) + " - 总分: " + a.get("overall_score") + "分", "source", "analyzer"));
            List<Map<String, Object>> dims = (List<Map<String, Object>>) a.get("dimensions");
            for (Map<String, Object> d : dims) {
                logs.add(Map.of("timestamp", a.get("created_at"), "level", "debug", "message", d.get("name") + ": " + d.get("score") + "分 (问题: " + d.get("issues") + "个)", "source", "analyzer"));
            }
        }
        Collections.reverse(logs);
        return Map.of("projectId", id, "logs", logs, "totalLogs", logs.size());
    }

    @GetMapping("/projects/{id}/analysis/history")
    public List<Map<String, Object>> getHistory(@PathVariable String id) {
        return analysesByProject.getOrDefault(id, new ArrayList<>()).stream().map(a -> Map.of(
                "taskId", a.get("id"),
                "completedAt", a.get("created_at"),
                "overallScore", a.get("overall_score"),
                "grade", a.get("grade"),
                "dimensions", a.get("dimensions")
        )).collect(Collectors.toList());
    }

    @PutMapping("/projects/{id}")
    public Map<String, Object> updateProject(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> p = projects.get(id);
        if (p == null) return Map.of("error", "项目不存在");
        if (body.containsKey("name")) p.put("name", String.valueOf(body.get("name")));
        if (body.containsKey("description")) p.put("description", String.valueOf(body.get("description")));
        if (body.containsKey("language")) p.put("language", String.valueOf(body.get("language")));
        if (body.containsKey("status")) p.put("status", String.valueOf(body.get("status")));
        if (body.containsKey("config")) p.put("config", body.get("config"));
        p.put("updated_at", Instant.now().toString());
        Object lastId = p.get("last_analysis_id");
        if (lastId != null) {
            Map<String, Object> last = findAnalysis(id, lastId.toString());
            if (last != null) {
                Map<String, Object> lastAnalysis = new LinkedHashMap<>();
                lastAnalysis.put("overallScore", last.get("overall_score"));
                lastAnalysis.put("grade", last.get("grade"));
                lastAnalysis.put("completedAt", last.get("created_at"));
                lastAnalysis.put("taskId", last.get("id"));
                Map<String, Object> copy = new LinkedHashMap<>(p);
                copy.put("lastAnalysis", lastAnalysis);
                return copy;
            }
        }
        return p;
    }

    @DeleteMapping("/projects/{id}")
    public Map<String, Object> deleteProject(@PathVariable String id) {
        Map<String, Object> removed = projects.remove(id);
        filesByProject.remove(id);
        analysesByProject.remove(id);
        try {
            Path base = Paths.get("uploads").resolve(id);
            if (Files.exists(base)) {
                Files.walk(base)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            }
        } catch (Exception ignored) {}
        if (removed == null) return Map.of("error", "项目不存在");
        return Map.of("message", "项目删除成功", "id", id);
    }

    @GetMapping("/projects/stats")
    public Map<String, Object> stats() {
        int total = projects.size();
        long active = projects.values().stream().filter(p -> "active".equals(String.valueOf(p.get("status")))).count();
        long archived = projects.values().stream().filter(p -> "archived".equals(String.valueOf(p.get("status")))).count();
        long completed = projects.values().stream().filter(p -> "completed".equals(String.valueOf(p.get("status")))).count();
        long files = filesByProject.values().stream().mapToLong(List::size).sum();
        return Map.of("totalProjects", total, "activeProjects", active, "archivedProjects", archived, "completedProjects", completed, "totalFiles", files);
    }

    @PostMapping("/projects/{id}/duplicate")
    public Map<String, Object> duplicate(@PathVariable String id) {
        Map<String, Object> p = projects.get(id);
        if (p == null) return Map.of("error", "项目不存在");
        String newId = UUID.randomUUID().toString();
        Map<String, Object> copy = new LinkedHashMap<>(p);
        copy.put("id", newId);
        copy.put("name", String.valueOf(p.get("name")) + " - 副本");
        copy.put("created_at", Instant.now().toString());
        copy.put("updated_at", Instant.now().toString());
        projects.put(newId, copy);
        List<Map<String, Object>> files = filesByProject.getOrDefault(id, new ArrayList<>());
        filesByProject.put(newId, files.stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>(f);
            m.put("id", UUID.randomUUID().toString());
            m.put("project_id", newId);
            return m;
        }).collect(Collectors.toList()));
        return copy;
    }

    @PostMapping("/projects/{id}/archive")
    public Map<String, Object> archive(@PathVariable String id) {
        Map<String, Object> p = projects.get(id);
        if (p == null) return Map.of("error", "项目不存在");
        p.put("status", "archived");
        p.put("updated_at", Instant.now().toString());
        return p;
    }

    @PostMapping("/projects/{id}/restore")
    public Map<String, Object> restore(@PathVariable String id) {
        Map<String, Object> p = projects.get(id);
        if (p == null) return Map.of("error", "项目不存在");
        p.put("status", "active");
        p.put("updated_at", Instant.now().toString());
        return p;
    }

    private Map<String, Object> fileItem(String path, String projectId, long size) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", UUID.randomUUID().toString());
        m.put("project_id", projectId);
        m.put("filename", Paths.get(path).getFileName().toString());
        m.put("file_path", "/uploads/" + projectId + "/" + path.replace("\\", "/"));
        m.put("file_size", size);
        m.put("upload_time", Instant.now().toString());
        return m;
    }

    private Map<String, Object> dim(String name, int score, int maxScore, int issues) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("score", score);
        m.put("maxScore", maxScore);
        m.put("issues", issues);
        return m;
    }

    private Map<String, Object> findAnalysis(String projectId, String id) {
        List<Map<String, Object>> list = analysesByProject.getOrDefault(projectId, new ArrayList<>());
        for (Map<String, Object> a : list) {
            if (Objects.equals(a.get("id"), id)) return a;
        }
        return null;
    }
}