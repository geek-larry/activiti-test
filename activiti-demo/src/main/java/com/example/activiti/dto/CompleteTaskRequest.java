package com.example.activiti.dto;

import lombok.Data;

/**
 * 完成任务请求(审批操作)
 */
@Data
public class CompleteTaskRequest {

    /** 是否通过:true=通过,false=驳回 */
    private Boolean approved;

    /** 审批备注 */
    private String comment;
}
