package com.example.circlea.matching.cases.detail;

import java.util.Calendar;
import java.util.Locale;

public class TimeSlot {
    private String slotId;
    private Calendar startTime;
    private Calendar endTime;
    private boolean isSelected;
    private String status;  // "available", "pending", "booked"
    private String requestId; // For tracking slot requests
    private boolean editable;
    private boolean modified = false;
    private String studentId;

    public TimeSlot(Calendar startTime, Calendar endTime, boolean isSelected) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.isSelected = isSelected;
        this.status = "available"; // Default status
        this.modified = false;     // 新创建的时间段初始为未修改状态
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isModified() {
        return modified;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public void resetModified() {
        this.modified = false;
    }
    public TimeSlot clone() {
        TimeSlot clone = new TimeSlot(
                (Calendar) this.startTime.clone(),
                (Calendar) this.endTime.clone(),
                this.isSelected
        );
        clone.setSlotId(this.slotId);
        clone.setStatus(this.status);
        clone.setRequestId(this.requestId);
        clone.setEditable(this.editable);
        clone.setModified(this.modified);
        return clone;
    }

    public boolean hasSameTime(TimeSlot other) {
        return this.startTime.equals(other.startTime) &&
                this.endTime.equals(other.endTime);
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }



    public boolean isConfirmed() {
        return status.equals("confirmed");
    }

    public boolean isExpired() {
        return status.equals("expired");
    }

    public boolean isEditable() {
        return editable;
    }

    // Getters and setters for existing fields
    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        if (this.startTime == null || !this.startTime.equals(startTime)) {
            this.startTime = startTime;
            this.modified = true;
        }
    }


    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        if (this.endTime == null || !this.endTime.equals(endTime)) {
            this.endTime = endTime;
            this.modified = true;
        }
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    // New getters and setters
    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDateString() {
        return startTime.get(Calendar.DAY_OF_MONTH) + " " +
                startTime.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " " +
                startTime.get(Calendar.YEAR);
    }

    public String getTimeString() {
        return String.format("%02d:%02d - %02d:%02d",
                startTime.get(Calendar.HOUR_OF_DAY),
                startTime.get(Calendar.MINUTE),
                endTime.get(Calendar.HOUR_OF_DAY),
                endTime.get(Calendar.MINUTE));
    }

    // Helper method to check if slot is available
    public boolean isAvailable() {
        return "available".equals(status);
    }

    // Helper method to check if slot is pending
    public boolean isPending() {
        return "pending".equals(status);
    }

    // Helper method to check if slot is booked
    public boolean isBooked() {
        return "booked".equals(status);
    }

    // Format date for server communication
    public String getFormattedStartTime() {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d:00",
                startTime.get(Calendar.YEAR),
                startTime.get(Calendar.MONTH) + 1,
                startTime.get(Calendar.DAY_OF_MONTH),
                startTime.get(Calendar.HOUR_OF_DAY),
                startTime.get(Calendar.MINUTE));
    }

    // Format date for server communication
    public String getFormattedEndTime() {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d:00",
                endTime.get(Calendar.YEAR),
                endTime.get(Calendar.MONTH) + 1,
                endTime.get(Calendar.DAY_OF_MONTH),
                endTime.get(Calendar.HOUR_OF_DAY),
                endTime.get(Calendar.MINUTE));
    }
}