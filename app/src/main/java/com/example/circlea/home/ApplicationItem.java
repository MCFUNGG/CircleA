package com.example.circlea.home;

public class ApplicationItem {
    private String appId;
    private String subject;
    private String classLevel;
    private String fee;
    private String district;
    private String memberId;

    public ApplicationItem(String appId, String subject, String classLevel, String fee, String district, String memberId) {
        this.appId = appId;
        this.subject = subject;
        this.classLevel = classLevel;
        this.fee = fee;
        this.district = district;
        this.memberId = memberId;
    }

    public String getAppId() {
        return appId;
    }

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

    public String getMemberId() {
        return memberId;
    }
}