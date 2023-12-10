package com.example.autoscheduler;
import java.util.Objects;

public class UserDto {
    private String person;
    private String sex;
    private String timeCode;

    // Constructor and getter methods...

    @Override
    public String toString() {
        return timeCode;
    }

    public UserDto(String person, String sex, String timeCode) {
        this.person = person;
        this.sex = "4";
        this.timeCode = timeCode;
    }

    // Getter methods
    public String getPerson() {
        return person;
    }

    public String getSex() {
        return sex;
    }

    public String getTimeCode() {
        return timeCode;
    }
}