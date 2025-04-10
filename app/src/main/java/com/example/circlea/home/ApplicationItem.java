package com.example.circlea.home;

import java.util.ArrayList;

public class ApplicationItem {
    private String appId;
    private ArrayList<String> subjects; // Changed to ArrayList
    private String classLevel;
    private String fee;
    private ArrayList<String> districts; // Changed to ArrayList
    private String memberId;
    private String profileIcon, username, tutorAppId;
    private String applicationType; // New property for profile icon
    private String rating;
    private String education; // Added education field

    public ApplicationItem(String appId, ArrayList<String> subjects, String classLevel, String fee,
                           ArrayList<String> districts, String memberId, String profileIcon, String username, String applicationType) {
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
        this.rating = "0.0";
        this.education = ""; // Initialize education as empty string
    }

    public ApplicationItem(String appId, ArrayList<String> subjects, String classLevel,
                           String fee, ArrayList<String> districts, String memberId,
                           String profileIcon, String username, String applicationType,
                           String rating) {
        this.appId = appId;
        this.subjects = subjects;
        this.classLevel = classLevel;
        this.fee = fee;
        this.districts = districts;
        this.memberId = memberId;
        this.profileIcon = profileIcon;
        this.username = username;
        this.tutorAppId = null; // This seems to be not initialized in the constructor you provided
        this.applicationType = applicationType;
        this.rating = rating;
        this.education = ""; // Initialize education as empty string
    }

    // Add constructor with education
    public ApplicationItem(String appId, ArrayList<String> subjects, String classLevel,
                           String fee, ArrayList<String> districts, String memberId,
                           String profileIcon, String username, String applicationType,
                           String rating, String education) {
        this.appId = appId;
        this.subjects = subjects;
        this.classLevel = classLevel;
        this.fee = fee;
        this.districts = districts;
        this.memberId = memberId;
        this.profileIcon = profileIcon;
        this.username = username;
        this.tutorAppId = null;
        this.applicationType = applicationType;
        this.rating = rating;
        this.education = education;
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

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getRating() {
        return rating != null ? rating : "0.0";
    }

    public String getEducation() {
        return education != null ? education : "";
    }

    public void setEducation(String education) {
        this.education = education;
    }
}