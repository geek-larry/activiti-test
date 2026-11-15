package com.example.activiti.controller;

import com.example.activiti.common.Result;
import com.example.activiti.dto.ApprovalView;
import com.example.activiti.dto.CompleteTaskRequest;
import com.example.activiti.dto.StartApprovalRequest;
import com.example.activiti.dto.TaskResponse;
import com.example.activiti.service.ApprovalService;
import org.activiti.engine.ActivitiOptimisticLockingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批流程 REST API
 */
@RestController
@RequestMapping("/api")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    /** 发起审批 */
    @PostMapping("/approval/start")
    public Result<String> start(@RequestBody StartApprovalRequest req) {
        return Result.success(approvalService.start(req));
    }

    /** 当前用户待办 */
    @GetMapping("/tasks")
    public Result<List<TaskResponse>> tasks(@RequestParam String assignee) {
        return Result.success(approvalService.myTasks(assignee));
    }

    /** 任务详情 */
    @GetMapping("/tasks/{taskId}")
    public Result<TaskResponse> taskDetail(@PathVariable String taskId) {
        return Result.success(approvalService.getTaskDetail(taskId));
    }

    /** 完成任务(审批)。或签下并发竞态返回 409 */
    @PostMapping("/tasks/{taskId}/complete")
    public Result<?> complete(@PathVariable String taskId, @RequestBody CompleteTaskRequest req) {
        try {
            approvalService.completeTask(taskId, req);
            return Result.success();
        } catch (ActivitiOptimisticLockingException e) {
            return Result.error(409, "任务已被他人处理");
        }
    }

    /** 申请单详情 + 审批轨迹 */
    @GetMapping("/approval/{businessCode}")
    public Result<ApprovalView> detail(@PathVariable String businessCode) {
        return Result.success(approvalService.detail(businessCode));
    }
}
