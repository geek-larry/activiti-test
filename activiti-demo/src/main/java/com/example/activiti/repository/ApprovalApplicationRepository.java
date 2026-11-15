package com.example.activiti.repository;

import com.example.activiti.entity.ApprovalApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalApplicationRepository extends JpaRepository<ApprovalApplication, Long> {

    Optional<ApprovalApplication> findByBusinessCode(String businessCode);

    Optional<ApprovalApplication> findByProcessInstanceId(String processInstanceId);
}
