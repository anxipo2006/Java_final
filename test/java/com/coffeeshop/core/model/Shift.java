package com.coffeeshop.core.model;

import java.sql.Time; // Dùng java.sql.Time cho start_time và end_time

public class Shift {

    private String shiftId;
    private String dayOfWeek;
    private Time startTime;
    private Time endTime;

    // Constructors
    public Shift() {
    }

    public Shift(String shiftId, String dayOfWeek, Time startTime, Time endTime) {
        this.shiftId = shiftId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and Setters
    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    // toString để dễ debug và hiển thị trong JComboBox
    @Override
    public String toString() {
        return String.format("%s - %s (%s - %s)",
                shiftId,
                dayOfWeek,
                startTime != null ? startTime.toString().substring(0, 5) : "N/A", // Hiển thị HH:mm
                endTime != null ? endTime.toString().substring(0, 5) : "N/A"
        );
    }
}
