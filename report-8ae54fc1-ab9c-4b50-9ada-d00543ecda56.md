# Code Quality Report

**Overall Score: 66.00%** (Severe Disaster)

Severe and widespread issues ⚠️

## Metrics Overview

|    Metric    |  Score  | Weight | Description |
|--------------|---------|--------|-------------|
| 代码重复度 | 100.00% | 0.10 | 代码重复度 |
| 错误处理分析 | 100.00% | 0.10 | 错误处理分析 |
| 圈复杂度分析 | 74.00% | 0.10 | 圈复杂度分析 |
| 结构分析 | 74.00% | 0.10 | 结构分析 |
| 注释覆盖率分析 | 70.00% | 0.10 | 注释覆盖率分析 |
| 命名规范 | 46.00% | 0.10 | 命名规范 |
| 状态管理 | 0.00% | 0.10 | 状态管理 |


## All Issues

- `uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java` (66.00%)
  - [high] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:- - 注释覆盖率极低（4.08%），可读性较差。
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:32 - 公共类缺少文档注释：ProjectController
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:46 - 公共方法缺少文档注释：listProjects
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:56 - 公共方法缺少文档注释：exportProjectReportPdf
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:115 - 公共方法缺少文档注释：createProject
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:128 - 公共方法缺少文档注释：getProject
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:139 - 公共方法缺少文档注释：updateProject
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:156 - 公共方法缺少文档注释：deleteProject
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:183 - 公共方法缺少文档注释：listFiles
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:206 - 公共方法缺少文档注释：getFileContent
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:223 - 公共方法缺少文档注释：upload
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:267 - 公共方法缺少文档注释：deleteFile
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:307 - 公共方法缺少文档注释：deleteFileByPath
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:342 - 公共方法缺少文档注释：analyze
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:440 - 公共方法缺少文档注释：cancelAnalyze
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:454 - 公共方法缺少文档注释：getAnalysis
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:497 - 公共方法缺少文档注释：getStatus
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:515 - 公共方法缺少文档注释：getLogs
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:562 - 公共方法缺少文档注释：getHistory
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:588 - 公共方法缺少文档注释：importGit
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:602 - 公共方法缺少文档注释：stats
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:620 - 公共方法缺少文档注释：duplicate
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:654 - 公共方法缺少文档注释：archive
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:668 - 公共方法缺少文档注释：restore
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:342 - 函数 'analyze': 函数圈复杂度过高, 圈复杂度为 20
  - [low] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:1 - 包名 'com.bugkiller_cityu.code_diagnostician.controller' 不符合规范，应使用小写字母且不包含下划线。
  - [low] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:454 - 可能存在重复实现: getAnalysis, getHistory
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:128 - 可能存在重复实现: getProject, listFiles, deleteFileByPath, cancelAnalyze, getStatus, importGit, archive, restore
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:515 - 函数 getLogs 的嵌套深度较高 (4)
  - [high] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:1 - 导入包数量过多 (27)
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:82 - 调用可能抛出异常的方法 'parse' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:124 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:149 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:172 - 捕获异常但未处理 (变量 'ignored' 未被使用)
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:169 - 捕获异常但未处理 (变量 'ignored' 未被使用)
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:175 - 调用可能抛出异常的方法 'delete' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:196 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:214 - 调用可能抛出异常的方法 'readString' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:248 - 调用可能抛出异常的方法 'setUploadTime' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:249 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:256 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:278 - 捕获异常但未处理 (变量 'ignored' 未被使用)
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:281 - 调用可能抛出异常的方法 'delete' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:287 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:316 - 捕获异常但未处理 (变量 'ignored' 未被使用)
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:319 - 调用可能抛出异常的方法 'delete' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:325 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:369 - 捕获异常但未处理 (变量 'ignored' 未被使用)
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:447 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:595 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:633 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:643 - 调用可能抛出异常的方法 'setUploadTime' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:646 - 调用可能抛出异常的方法 'saveAll' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:661 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:675 - 调用可能抛出异常的方法 'save' 但未使用 try-catch
  - [medium] uploads\8ae54fc1-ab9c-4b50-9ada-d00543ecda56\5a279d96-af56-4259-bf07-83b13246150a_ProjectController.java:726 - 调用可能抛出异常的方法 'getUploadTime' 但未使用 try-catch


## Summary

- Files: 1
- Lines: 759
- Score: 66.00%
