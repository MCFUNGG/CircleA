package com.example.circlea.home;

public class ApplicationItem {
    private String subject;
    private String classLevel;
    private String fee;
    private String district;

    // Constructor
    public ApplicationItem(String subject, String classLevel, String fee, String district) {
        this.subject = subject;
        this.classLevel = classLevel;
        this.fee = fee;
        this.district = district;
    }

    // Getters
    public String getSubject() {
        return subject;
    }

    public String getClassLevel() {
        return classLevel;
    }

    public String getFee() {
        return fee;
    }

    public String getDistrict() {
        return district;
    }
}