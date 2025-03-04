package com.example.circlea.home;

import java.util.ArrayList;

public class ApplicationItem {
    private String appId;
    private ArrayList<String> subjects; // Changed to ArrayList
    private String classLevel;
    private String fee;
    private ArrayList<String> districts; // Changed to ArrayList
    private String memberId;
    private String profileIcon,username,tutorAppId;
    private String applicationType; // New property for profile icon

    public ApplicationItem(String appId, ArrayList<String> subjects, String classLevel, String fee, ArrayList<String> districts, String memberId, String profileIcon, String username,  String applicationType) {
        this.appId = appId;
        this.subjects = subjects;
        this.classLevel = classLevel;
        this.fee = fee;
        this.districts = districts; // Updated to accept the list
        this.memberId = memberId;
        this.profileIcon = profileIcon;
        this.username = username;// Initialize profile icon
        this.tutorAppId = tutorAppId;
        this.applicationType = applicationType;
    }



    public String getAppId() {
        return appId;
    }

    public ArrayList<String> getSubjects() { // Updated getter
        return subjects;
    }

    public String getClassLevel() {
        return classLevel;
    }

    public String getFee() {
        return fee;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public ArrayList<String> getDistricts() { // Updated getter
        return districts;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getProfileIcon() { // New getter for profile icon
        return profileIcon;
    }

    public String getUsername() { // New getter for profile icon
        return username;
    }
}