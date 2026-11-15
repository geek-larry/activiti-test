package com.example.activiti.controller;

import com.example.activiti.common.Result;
import com.example.activiti.entity.SysUser;
import com.example.activiti.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户接口(供前端选择审批人)
 */
@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private SysUserRepository sysUserRepository;

    /** demo 用户列表 */
    @GetMapping("/users")
    public Result<List<SysUser>> users() {
        return Result.success(sysUserRepository.findAll());
    }
}
