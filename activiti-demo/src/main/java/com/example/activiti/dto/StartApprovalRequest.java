package com.example.activiti.dto;

import lombok.Data;

import java.util.List;

/**
 * 发起审批请求
 */
@Data
public class StartApprovalRequest {

    /** 申请人 */
    private String applicant;

    /** 请假类型:年假/事假/病假/调休 */
    private String leaveType;

    /** 请假天数 */
    private Integer days;

    /** 事由 */
    private String reason;

    /** 初审人列表(或签,任一人审批即流转) */
    private List<String> firstReviewers;

    /** 复审人列表(或签) */
    private List<String> secondReviewers;
}
