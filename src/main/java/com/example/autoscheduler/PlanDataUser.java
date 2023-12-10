package com.example.autoscheduler;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


// PlanDataUser 클래스
@Entity
@Table(name = "day_plan")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class PlanDataUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private int planId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "plan_name")
    private String planname;

    @Column(name = "plan_type")
    private String plantype;

    @Column(name = "start", nullable = false)
    private LocalDateTime start;

    @Column(name = "end", nullable = false)
    private LocalDateTime end;

    @Column(name = "plan")
    private String plan;

    @Column(name = "success")
    private int success;

    @Column(name = "day_of_week")
    private String day_of_week;

    public String getPlanType() {
        return plantype;
    }

    public String getSex(){
        return "4";
    }

    public String getPlantype() {
        return plantype;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDate getStartDate() {
        return start.toLocalDate();
    }
}