package com.example.autoscheduler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PlanDataRepositoryStudent extends JpaRepository<PlanData, Integer> {
    // Custom query method to find PlanData by student
    List<PlanData> findByStudent(String student);

    List<PlanData> findByPerson(Integer person);
    List<PlanData> findByStudentIs(String student);

    // Custom query method to find PlanData by student '고등학생'
    List<PlanData> findByStudentEquals(String student);

    List<PlanData> findByPersonIn(List<Integer> persons);

}