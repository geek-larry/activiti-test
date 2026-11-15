package com.example.activiti.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 审批申请单(业务表,JPA 自动建表 approval_application)
 */
@Entity
@Table(name = "approval_application")
@Data
public class ApprovalApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务编号 */
    @Column(name = "business_code", unique = true, nullable = false, length = 64)
    private String businessCode;

    /** 申请人 */
    @Column(name = "applicant", length = 64)
    private String applicant;

    @Column(name = "leave_type", length = 32)
    private String leaveType;

    @Column(name = "days")
    private Integer days;

    @Column(name = "reason", length = 500)
    private String reason;

    /** 状态:PENDING_FIRST / PENDING_SECOND / APPROVED / REJECTED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private Status status;

    /** Activiti 流程实例ID */
    @Column(name = "process_instance_id", length = 64)
    private String processInstanceId;

    /** 实际初审人(或签下第一个完成审批的人) */
    @Column(name = "actual_first_approver", length = 64)
    private String actualFirstApprover;

    /** 实际复审人 */
    @Column(name = "actual_second_approver", length = 64)
    private String actualSecondApprover;

    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /** 申请单状态枚举 */
    public enum Status {
        PENDING_FIRST,
        PENDING_SECOND,
        APPROVED,
        REJECTED
    }
}
