package com.example.activiti.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 申请单详情 + 审批轨迹
 */
@Data
public class ApprovalView {

    private String businessCode;
    private String applicant;
    private String leaveType;
    private Integer days;
    private String reason;
    private String status;
    private String actualFirstApprover;
    private String actualSecondApprover;
    private String processInstanceId;
    private LocalDateTime createTime;

    /** 审批轨迹 */
    private List<TrackItem> tracks;

    @Data
    public static class TrackItem {
        /** 节点名称 */
        private String activityName;
        /** 办理人 */
        private String assignee;
        /** 办理时间 */
        private LocalDateTime endTime;
        /** 备注 */
        private String comment;
        /** 是否通过(用于判断结果) */
        private String outcome;
    }
}
