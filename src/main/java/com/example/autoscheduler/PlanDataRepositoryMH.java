package com.example.autoscheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanDataRepositoryMH extends JpaRepository<PlanData, Integer> {
    // You can add custom query methods if needed
}