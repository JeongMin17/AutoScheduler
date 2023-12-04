package com.example.autoscheduler;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class StartEndDto {

    private int person;
    private String type;  // Add 'type' field
    private String sex;   // Add 'sex' field
    private LocalTime start;
    private LocalTime end;
    // Constructor
    public StartEndDto(int person, Integer type, String sex, LocalTime start, LocalTime end) {
        this.person = person;
        this.type = String.valueOf(type);
        this.sex = sex;
        this.start = start;
        this.end = end;
    }

    // Getter methods
    public int getPerson() {
        return person;
    }

    public String getType() {
        return type;
    }

    public String getSex() {
        return sex;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }
}