package com.example.activiti.service;

import com.example.activiti.config.SecurityUtil;
import com.example.activiti.dto.ApprovalView;
import com.example.activiti.dto.CompleteTaskRequest;
import com.example.activiti.dto.StartApprovalRequest;
import com.example.activiti.dto.TaskResponse;
import com.example.activiti.entity.ApprovalApplication;
import com.example.activiti.entity.ApprovalApplication.Status;
import com.example.activiti.repository.ApprovalApplicationRepository;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批流程服务。
 * 使用 Activiti 的 RuntimeService/TaskService/HistoryService(传统 API,灵活度高)。
 * 当前操作人身份由 CurrentUserFilter 在请求开始时通过 SecurityUtil.logInAs() 写入 SecurityContext。
 */
@Service
public class ApprovalService {

    @Autowired private RuntimeService runtimeService;
    @Autowired private TaskService taskService;
    @Autowired private HistoryService historyService;
    @Autowired private ApprovalApplicationRepository appRepo;
    @Autowired private SecurityUtil securityUtil;

    private static final String PROCESS_KEY = "approval";

    /**
     * 发起审批:启动流程 → 落库业务单 → 自动完成"申请提交"节点 → 流转到初审
     */
    @Transactional
    public String start(StartApprovalRequest req) {
        String businessCode = "LV" + System.currentTimeMillis();

        Map<String, Object> vars = new HashMap<>();
        vars.put("applicant", req.getApplicant());
        vars.put("leaveType", req.getLeaveType());
        vars.put("days", req.getDays());
        vars.put("reason", req.getReason());
        vars.put("firstReviewers", req.getFirstReviewers());
        vars.put("secondReviewers", req.getSecondReviewers());
        vars.put("firstApproved", false);
        vars.put("secondApproved", false);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, vars);

        ApprovalApplication app = new ApprovalApplication();
        app.setBusinessCode(businessCode);
        app.setApplicant(req.getApplicant());
        app.setLeaveType(req.getLeaveType());
        app.setDays(req.getDays());
        app.setReason(req.getReason());
        app.setStatus(Status.PENDING_FIRST);
        app.setProcessInstanceId(instance.getId());
        appRepo.save(app);

        // 自动完成"申请提交"节点(发起即提交),流转到初审
        Task submitTask = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .taskDefinitionKey("submitTask")
                .singleResult();
        if (submitTask != null) {
            taskService.complete(submitTask.getId());
        }
        return businessCode;
    }

    /**
     * 查询当前用户待办(或签下所有审批人都可见自己的待办)
     */
    public List<TaskResponse> myTasks(String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .active()
                .orderByTaskCreateTime().desc()
                .list();
        return tasks.stream().map(this::toTaskResponse).collect(Collectors.toList());
    }

    /**
     * 任务详情
     */
    public TaskResponse getTaskDetail(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return toTaskResponse(task);
    }

    /**
     * 完成任务(审批)。
     * 或签下:一人完成,Activiti 自动取消其他活跃实例,completionCondition=${nrOfCompletedInstances>=1}
     */
    @Transactional
    public void completeTask(String taskId, CompleteTaskRequest req) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        String key = task.getTaskDefinitionKey();
        boolean isFirst = "firstReview".equals(key);
        boolean isSecond = "secondReview".equals(key);
        String currentUser = securityUtil.getCurrentUser();

        // 审批人是流程实例的子执行上的任务,且 Activiti 7 开启
        // copyVariablesToLocalForTasks 后:创建任务时会把流程变量快照复制到 task-local;
        // 任务完成时 UserTaskActivityBehavior#propagateVariablesToProcess 又会把这份
        // task-local 快照整体回写到流程实例,覆盖任务期间对流程实例级变量的修改。
        // 若直接用 complete(taskId, vars)(localScope=false),审批结果只写到了执行作用域,
        // 回写时仍用快照里的旧值,导致排他网关一直走"驳回"分支。
        // 所以必须以 localScope=true 传入,先落到 task-local 快照,回写时才是新值。
        Map<String, Object> vars = new HashMap<>();
        if (isFirst || isSecond) {
            vars.put(isFirst ? "firstApproved" : "secondApproved", Boolean.TRUE.equals(req.getApproved()));
        }

        // 记录审批备注
        if (req.getComment() != null && !req.getComment().trim().isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), req.getComment());
        }

        taskService.complete(taskId, vars, true);

        // 更新业务表
        appRepo.findByProcessInstanceId(task.getProcessInstanceId()).ifPresent(app -> {
            if (isFirst) {
                app.setActualFirstApprover(currentUser);
                app.setStatus(Boolean.TRUE.equals(req.getApproved()) ? Status.PENDING_SECOND : Status.REJECTED);
            } else if (isSecond) {
                app.setActualSecondApprover(currentUser);
                app.setStatus(Boolean.TRUE.equals(req.getApproved()) ? Status.APPROVED : Status.REJECTED);
            }
            appRepo.save(app);
        });
    }

    /**
     * 申请单详情 + 审批轨迹
     */
    public ApprovalView detail(String businessCode) {
        ApprovalApplication app = appRepo.findByBusinessCode(businessCode)
                .orElseThrow(() -> new IllegalArgumentException("申请单不存在: " + businessCode));

        ApprovalView view = new ApprovalView();
        view.setBusinessCode(app.getBusinessCode());
        view.setApplicant(app.getApplicant());
        view.setLeaveType(app.getLeaveType());
        view.setDays(app.getDays());
        view.setReason(app.getReason());
        view.setStatus(app.getStatus().name());
        view.setActualFirstApprover(app.getActualFirstApprover());
        view.setActualSecondApprover(app.getActualSecondApprover());
        view.setProcessInstanceId(app.getProcessInstanceId());
        view.setCreateTime(app.getCreateTime());

        // 审批结果(从历史变量)
        Boolean firstApproved = getHistoricVariable(app.getProcessInstanceId(), "firstApproved");
        Boolean secondApproved = getHistoricVariable(app.getProcessInstanceId(), "secondApproved");

        // 审批轨迹(已完成的历史任务)
        List<HistoricTaskInstance> hisTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(app.getProcessInstanceId())
                .orderByHistoricTaskInstanceEndTime().asc()
                .finished()
                .list();

        List<ApprovalView.TrackItem> tracks = hisTasks.stream().map(h -> {
            ApprovalView.TrackItem item = new ApprovalView.TrackItem();
            item.setActivityName(h.getName());
            item.setAssignee(h.getAssignee());
            item.setEndTime(toLocalDateTime(h.getEndTime()));

            List<Comment> comments = taskService.getTaskComments(h.getId());
            if (!comments.isEmpty()) {
                item.setComment(comments.get(0).getFullMessage());
            }

            String defKey = h.getTaskDefinitionKey();
            if ("submitTask".equals(defKey)) {
                item.setOutcome("提交");
            } else if ("firstReview".equals(defKey) && firstApproved != null) {
                item.setOutcome(firstApproved ? "通过" : "驳回");
            } else if ("secondReview".equals(defKey) && secondApproved != null) {
                item.setOutcome(secondApproved ? "通过" : "驳回");
            }
            return item;
        }).collect(Collectors.toList());

        view.setTracks(tracks);
        return view;
    }

    // -------- helpers --------

    private TaskResponse toTaskResponse(Task task) {
        TaskResponse r = new TaskResponse();
        r.setTaskId(task.getId());
        r.setTaskName(task.getName());
        r.setAssignee(task.getAssignee());
        r.setProcessInstanceId(task.getProcessInstanceId());
        r.setCreateTime(toLocalDateTime(task.getCreateTime()));
        appRepo.findByProcessInstanceId(task.getProcessInstanceId()).ifPresent(app -> {
            r.setBusinessCode(app.getBusinessCode());
            r.setApplicant(app.getApplicant());
            r.setLeaveType(app.getLeaveType());
            r.setDays(app.getDays());
            r.setReason(app.getReason());
        });
        return r;
    }

    /**
     * 读取流程实例级的变量。
     * 同名变量在 ACT_HI_VARINST 中可能有多行(任务级历史变量、多实例子执行副本),
     * 直接用 singleResult() 会抛 "Query return N results instead of max 1",
     * 所以这里限定 executionId 为流程实例本身。
     */
    @SuppressWarnings("unchecked")
    private <T> T getHistoricVariable(String processInstanceId, String name) {
        List<HistoricVariableInstance> list = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .executionId(processInstanceId)
                .variableName(name)
                .list();
        return list.isEmpty() ? null : (T) list.get(0).getValue();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
