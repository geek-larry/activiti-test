package com.example.activiti.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办任务响应
 */
@Data
public class TaskResponse {

    private String taskId;
    private String taskName;
    private String assignee;
    private String businessCode;
    private String applicant;
    private String leaveType;
    private Integer days;
    private String reason;
    private String processInstanceId;
    private LocalDateTime createTime;
}
