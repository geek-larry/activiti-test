package com.example.activiti.config;

import com.example.activiti.entity.SysUser;
import com.example.activiti.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化 demo 用户数据(sys_user 表)
 */
@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private SysUserRepository sysUserRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (sysUserRepository.count() == 0) {
            save("zhangsan", "张三", "初审人");
            save("lisi", "李四", "初审人");
            save("wangwu", "王五", "复审人");
        }
    }

    private void save(String username, String displayName, String role) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setDisplayName(displayName);
        u.setRole(role);
        sysUserRepository.save(u);
    }
}
