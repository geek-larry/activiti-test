# Activiti 7 审批工作流 Demo

一个「请假审批」流程示例，演示 Activiti 7 的**传统 Service API**（`RuntimeService` / `TaskService` / `HistoryService`）用法，包含**或签（并行多实例 + 完成条件）**与**排他网关**分流。

## 技术栈

| 组件 | 版本 |
|---|---|
| Spring Boot | 2.5.15 |
| Activiti | 7.1.0.M6（`activiti-spring-boot-starter`） |
| JDK | 1.8 |
| 数据库 | PostgreSQL 18 |
| Spring Security | 随 Boot 版本（Activiti 7 强依赖） |

> Activiti 7 使用 `org.activiti.engine.*` 传统引擎 API；`ProcessRuntime` / `TaskRuntime` 那套新 API 本项目未使用。

## 流程模型

```
开始 → 申请提交 → 初审(或签) → 网关 ─通过→ 复审(或签) → 网关 ─通过→ 审批通过(结束)
                              └驳回→ 审批驳回(结束)          └驳回→ 审批驳回(结束)
```

- **申请提交**：发起时由后端自动 `complete`，用户无感知
- **初审 / 复审**：并行多实例（或签），任一审批人处理即结束该节点
- 两个排他网关分别依据流程变量 `firstApproved` / `secondApproved` 分流

## 快速开始

```bash
# 1. 启动数据库
docker compose up -d

# 2. 启动后端（自动建表 + 自动部署 classpath:processes/*.bpmn20.xml）
mvn spring-boot:run
# 服务地址 http://localhost:8080
```

当前操作人通过请求头 `X-Current-User` 传递（`zhangsan` / `lisi` / `wangwu`），未传时默认 `zhangsan`。

## Activiti 核心 API

所有 Activiti 调用集中在 `src/main/java/com/example/activiti/service/ApprovalService.java`，Controller 层仅引用了异常类型。

### 一、三个引擎服务入口

由 `activiti-spring-boot-starter` 自动装配，直接注入使用：

```java
// ApprovalService.java:39-41
@Autowired private RuntimeService runtimeService;
@Autowired private TaskService taskService;
@Autowired private HistoryService historyService;
```

| 服务 | 职责 | 项目中的用途 |
|---|---|---|
| `RuntimeService` | 流程实例运行时操作 | 启动流程实例 |
| `TaskService` | 任务运行时操作 | 查待办、完成任务、加/查审批意见 |
| `HistoryService` | 历史数据查询 | 审批轨迹、审批结果变量 |

### 二、RuntimeService — 启动流程

| API | 位置 | 说明 |
|---|---|---|
| `startProcessInstanceByKey(String key, Map<String,Object> vars)` | `ApprovalService.java:64` | 按流程定义 key（`"approval"`）启动，返回 `ProcessInstance` |

```java
// ApprovalService.java:64
ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, vars);
```

启动时一次性灌入 8 个流程变量：`applicant`、`leaveType`、`days`、`reason`、`firstReviewers`、`secondReviewers`、`firstApproved`、`secondApproved`（后两个作为网关判断的初始值，均为 `false`）。

流程实例 id 取自 `instance.getId()`，回写到业务表 `approval_application.process_instance_id`，作为业务表与引擎表的关联键。

### 三、TaskService — 任务查询

统一以 `createTaskQuery()` 构造链式 `TaskQuery`：

| 方法 | 位置 | 用途 |
|---|---|---|
| `.processInstanceId(id)` | `:78` | 限定流程实例 |
| `.taskDefinitionKey("submitTask")` | `:79` | 按 BPMN 节点 id 精确定位（用于自动完成「申请提交」节点） |
| `.taskAssignee(user)` | `:92` | 按办理人查待办 |
| `.active()` | `:93` | 只取活跃任务（排除已挂起/已完成） |
| `.orderByTaskCreateTime().desc()` | `:94` | 按创建时间倒序 |
| `.taskId(id)` | `:103`、`:116` | 按任务 id 查单个任务 |
| `.singleResult()` / `.list()` | `:80` / `:95` | 取一条 / 取列表 |

```java
// ApprovalService.java:91-95  当前用户待办
List<Task> tasks = taskService.createTaskQuery()
        .taskAssignee(assignee)
        .active()
        .orderByTaskCreateTime().desc()
        .list();
```

```java
// ApprovalService.java:77-83  自动完成"申请提交"节点
Task submitTask = taskService.createTaskQuery()
        .processInstanceId(instance.getId())
        .taskDefinitionKey("submitTask")
        .singleResult();
if (submitTask != null) {
    taskService.complete(submitTask.getId());
}
```

### 四、TaskService — 任务操作（本项目最关键的一组）

| API | 位置 | 说明 |
|---|---|---|
| `complete(String taskId)` | `:82` | 无变量完成，用于自动提交节点 |
| `complete(String taskId, Map vars, boolean localScope)` | `:142` | 审批完成，**`localScope=true`** |
| `addComment(taskId, processInstanceId, message)` | `:139` | 写审批意见（落到 `ACT_HI_COMMENT`） |
| `getTaskComments(taskId)` | `:193` | 按任务读意见 |

```java
// ApprovalService.java:132-142
Map<String, Object> vars = new HashMap<>();
if (isFirst || isSecond) {
    vars.put(isFirst ? "firstApproved" : "secondApproved", Boolean.TRUE.equals(req.getApproved()));
}

// 记录审批备注
if (req.getComment() != null && !req.getComment().trim().isEmpty()) {
    taskService.addComment(taskId, task.getProcessInstanceId(), req.getComment());
}

taskService.complete(taskId, vars, true);
```

> `localScope` 的取值原因见文末「关键注意事项」。

### 五、HistoryService — 历史查询

| API | 位置 | 用途 |
|---|---|---|
| `createHistoricTaskInstanceQuery()` → `.processInstanceId()` `.finished()` `.orderByHistoricTaskInstanceEndTime().asc()` `.list()` | `:181-185` | 已完成的审批轨迹，按结束时间正序 |
| `createHistoricVariableInstanceQuery()` → `.processInstanceId()` `.executionId()` `.variableName()` `.list()` | `:240-244` | 读取流程实例级变量的最终值 |

```java
// ApprovalService.java:181-185  审批轨迹
List<HistoricTaskInstance> hisTasks = historyService.createHistoricTaskInstanceQuery()
        .processInstanceId(app.getProcessInstanceId())
        .orderByHistoricTaskInstanceEndTime().asc()
        .finished()
        .list();
```

```java
// ApprovalService.java:240-245  读取流程实例级变量
List<HistoricVariableInstance> list = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(processInstanceId)
        .executionId(processInstanceId)
        .variableName(name)
        .list();
return list.isEmpty() ? null : (T) list.get(0).getValue();
```

### 六、异常处理

| 类型 | 位置 | 说明 |
|---|---|---|
| `ActivitiOptimisticLockingException` | `ApprovalController.java:9,49` | 或签并发下两人同时审批，捕获后返回 409「任务已被他人处理」 |

```java
// ApprovalController.java:46-51
try {
    approvalService.completeTask(taskId, req);
    return Result.success();
} catch (ActivitiOptimisticLockingException e) {
    return Result.error(409, "任务已被他人处理");
}
```

### 七、领域对象与常用方法

| 类型 | 包 | 用到的方法 |
|---|---|---|
| `ProcessInstance` | `org.activiti.engine.runtime` | `getId()` — 流程实例 id |
| `Task` | `org.activiti.engine.task` | `getId()` `getName()` `getAssignee()` `getProcessInstanceId()` `getCreateTime()` `getTaskDefinitionKey()` |
| `HistoricTaskInstance` | `org.activiti.engine.history` | `getId()` `getName()` `getAssignee()` `getEndTime()` `getTaskDefinitionKey()` |
| `HistoricVariableInstance` | `org.activiti.engine.history` | `getValue()` |
| `Comment` | `org.activiti.engine.task` | `getFullMessage()` |

`task.getTaskDefinitionKey()` 是业务分发的依据：`submitTask` / `firstReview` / `secondReview`，据此决定写 `firstApproved` 还是 `secondApproved`，以及审批轨迹中显示「提交 / 通过 / 驳回」。

### 八、BPMN 建模元素

文件：`src/main/resources/processes/approval.bpmn20.xml`

| 元素 | 用法 |
|---|---|
| `<process id="approval">` | 流程定义 key，对应 `startProcessInstanceByKey("approval")` |
| `<userTask activiti:assignee="${applicant}">` | 表达式动态指定办理人 |
| `<multiInstanceLoopCharacteristics isSequential="false" activiti:collection="${firstReviewers}" activiti:elementVariable="firstApprover">` | **或签**：集合变量驱动并行多实例，元素变量作为每个实例的 assignee |
| `<completionCondition>${nrOfCompletedInstances >= 1}</completionCondition>` | 或签核心：任一人完成即结束多实例，其余实例自动取消 |
| `<exclusiveGateway>` + `<sequenceFlow><conditionExpression>${firstApproved == true}</conditionExpression>` | 排他网关按变量分流 |
| 内置变量 `nrOfCompletedInstances` | Activiti 多实例自动维护，无需手工赋值 |

```xml
<!-- approval.bpmn20.xml:20-26  或签节点 -->
<userTask id="firstReview" name="初审" activiti:assignee="${firstApprover}">
    <multiInstanceLoopCharacteristics isSequential="false"
                                      activiti:collection="${firstReviewers}"
                                      activiti:elementVariable="firstApprover">
        <completionCondition xsi:type="tFormalExpression">${nrOfCompletedInstances >= 1}</completionCondition>
    </multiInstanceLoopCharacteristics>
</userTask>
```

### 九、配置项

`src/main/resources/application.yml`：

| 配置 | 值 | 作用 |
|---|---|---|
| `spring.activiti.database-schema-update` | `true` | 自动创建/更新 `ACT_*` 表 |
| `spring.activiti.history-level` | `audit` | 只记任务/实例级历史，够用且省空间 |
| `spring.activiti.db-history-used` | `true` | 启用历史库 |
| `spring.activiti.check-process-definitions` | `true` | **自动部署 `classpath:processes/*.bpmn20.xml`** |
| `spring.activiti.async-executor-activate` | `false` | 关闭异步作业执行器 |

配合 `spring.jpa.hibernate.ddl-auto: update` 只管理业务表，与引擎表互不干涉。

> 项目**没有**手工调用 `RepositoryService.createDeployment().addClasspathResource(...).deploy()`，流程部署完全由 starter 在启动时自动完成。

### 十、身份集成

Activiti 7 强依赖 Spring Security，操作任务的用户需具备 `ROLE_ACTIVITI_USER` 权限：

1. `SecurityConfig` 注册 InMemory 用户 `zhangsan` / `lisi` / `wangwu`，均赋 `ROLE_ACTIVITI_USER`（`SecurityConfig.java:56-75`），并放行所有 API。
2. `CurrentUserFilter` 读取请求头 `X-Current-User`，调用 `SecurityUtil.logInAs()` 写入 `SecurityContext`（`CurrentUserFilter.java:31-36`）。
3. 该 Filter 通过 `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)` 注册，保证在后续逻辑执行前身份已就位。
4. `SecurityUtil.logInAs()` 内部用 `UsernamePasswordAuthenticationToken` 构造认证对象塞进 `SecurityContextHolder`。

前端只需在每个请求带上 `X-Current-User` 请求头即可切换操作人。

### 十一、关键注意事项

#### 1. `complete()` 的 `localScope` 必须传 `true`

Activiti 7 开启 `copyVariablesToLocalForTasks` 后：

- **创建任务时**会把流程变量快照复制一份到 task-local；
- **任务完成时**，`UserTaskActivityBehavior#propagateVariablesToProcess` 会把这份 task-local 快照**整体回写**到流程实例，覆盖任务期间对流程实例级变量的修改。

因此若写成 `complete(taskId, vars)`（默认 `localScope=false`），审批结果只落到执行作用域，回写时仍用快照里的旧值，导致排他网关**恒走「驳回」分支**。必须：

```java
taskService.complete(taskId, vars, true);
```

#### 2. 历史变量查询不能直接 `singleResult()`

同名变量在 `ACT_HI_VARINST` 中可能有多行（任务级历史变量 + 多实例子执行副本），直接 `singleResult()` 会抛 `Query return N results instead of max 1`。需额外限定 `executionId` 为流程实例本身，取流程实例级那一行。

### 十二、相关数据表

| 表 | 内容 |
|---|---|
| `ACT_RE_PROCDEF` / `ACT_RE_DEPLOYMENT` / `ACT_GE_BYTEARRAY` | 流程定义与部署资源 |
| `ACT_RU_EXECUTION` / `ACT_RU_TASK` / `ACT_RU_VARIABLE` | 运行时执行、任务、变量 |
| `ACT_HI_PROCINST` / `ACT_HI_TASKINST` / `ACT_HI_VARINST` / `ACT_HI_ACTINST` | 历史实例、任务、变量、活动 |
| `ACT_HI_COMMENT` | 审批意见（`addComment` 写入） |

业务表 `approval_application` 通过 `process_instance_id` 与引擎表关联。

### 十三、REST API

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/approval/start` | 发起审批，返回业务单号 |
| `GET` | `/api/tasks?assignee={user}` | 查询指定用户待办 |
| `GET` | `/api/tasks/{taskId}` | 任务详情 |
| `POST` | `/api/tasks/{taskId}/complete` | 完成任务（审批）；并发冲突返回 409 |
| `GET` | `/api/approval/{businessCode}` | 申请单详情 + 审批轨迹 |
| `GET` | `/api/users` | 用户列表 |

统一响应信封：`{"code":200,"message":"success","data":...}`

## 未涉及的能力

本项目未使用：`RepositoryService`、`IdentityService`、`ManagementService`、流程跳转/回退、定时边界事件、`ProcessRuntime` / `TaskRuntime` 新 API。
