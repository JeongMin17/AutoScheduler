package com.example.autoscheduler;

import java.time.LocalDateTime;

class ToTime {
    private String type;
    private LocalDateTime start;
    private LocalDateTime end;

    // Getter and setter methods

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }
}