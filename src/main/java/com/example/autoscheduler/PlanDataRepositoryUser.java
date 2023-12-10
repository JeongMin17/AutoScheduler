package com.example.autoscheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PlanDataRepositoryUser extends JpaRepository<PlanDataUser, Integer> {
    List<PlanDataUser> findByUserId(String userId);
}