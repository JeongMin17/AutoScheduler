package com.example.autoscheduler;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "plan_data")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class PlanData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "start", nullable = false)
    private LocalDateTime start;

    @Column(name = "end", nullable = false)
    private LocalDateTime end;

    @Column(name = "sex", length = 2)
    private String sex;

    @Column(name = "student", length = 20)
    private String student;

    @Column(name = "person")
    private int person;

    public int getId() {
        return id;
    }

    // Setter for id
    public void setId(int id) {
        this.id = id;
    }

    // Getter for type
    public String getType() {
        return type;
    }

    // Setter for type
    public void setType(String type) {
        this.type = type;
    }

    // Getter for start
    public LocalDateTime getStart() {
        return start;
    }

    // Setter for start
    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    // Getter for end
    public LocalDateTime getEnd() {
        return end;
    }

    // Setter for end
    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    // Getter for sex
    public String getSex() {
        return sex;
    }

    // Setter for sex
    public void setSex(String sex) {
        this.sex = sex;
    }

    // Getter for student
    public String getStudent() {
        return student;
    }

    // Setter for student
    public void setStudent(String student) {
        this.student = student;
    }
}