package com.example.activiti.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 系统用户(demo 审批人)
 */
@Entity
@Table(name = "sys_user")
@Data
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 64)
    private String username;

    @Column(name = "display_name", length = 64)
    private String displayName;

    @Column(name = "role", length = 64)
    private String role;
}
