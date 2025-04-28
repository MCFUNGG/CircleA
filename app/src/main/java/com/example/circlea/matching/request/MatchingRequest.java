package com.example.circlea.matching.request;

public class MatchingRequest {
    private String matchId;
    private String psUsername;
    private String tutorUsername;
    private String psAppId;
    private String tutorAppId;
    private String fee;
    private String classLevel;
    private String subjects;
    private String districts;
    private String matchMark;
    private String profileIcon;
    private String matchCreator;
    private String senderRole;  // New field to help with sorting
    private String recipientUsername; // 接收者用户名
    private String recipientAppId; // 接收者应用ID

    // Constructor
    public MatchingRequest(String matchId, String psAppId, String tutorAppId,
                           String psUsername, String tutorUsername, String fee,
                           String classLevel, String subjects, String districts,
                           String matchMark, String profileIcon, String matchCreator) {
        this.matchId = matchId;
        this.psUsername = psUsername;
        this.tutorUsername = tutorUsername;
        this.psAppId = psAppId;
        this.tutorAppId = tutorAppId;
        this.fee = fee;
        this.classLevel = classLevel;
        this.subjects = subjects;
        this.districts = districts;
        this.matchMark = matchMark;
        this.profileIcon = profileIcon;
        this.matchCreator = matchCreator;
    }

    // Getters
    public String getRequestId() { return matchId; }
    public String getPsUsername() { return psUsername; }
    public String getTutorUsername() { return tutorUsername; }
    public String getMatchId() { return matchId; }
    public String getPsAppId() { return psAppId; }
    public String getTutorAppId() { return tutorAppId; }
    public String getFee() { return fee; }
    public String getClassLevel() { return classLevel; }
    public String getSubjects() { return subjects; }
    public String getDistricts() { return districts; }
    public String getMatchMark() { return matchMark; }
    public String getProfileIcon() { return profileIcon; }
    public String getMatchCreator() { return matchCreator; }
    public String getSenderRole() { return senderRole; }
    public String getRecipientUsername() { return recipientUsername; }
    public String getRecipientAppId() { return recipientAppId; }

    // Setters
    public void setPsUsername(String psUsername) { this.psUsername = psUsername; }
    public void setTutorUsername(String tutorUsername) { this.tutorUsername = tutorUsername; }
    public void setPsAppId(String psAppId) { this.psAppId = psAppId; }
    public void setTutorAppId(String tutorAppId) { this.tutorAppId = tutorAppId; }
    public void setFee(String fee) { this.fee = fee; }
    public void setClassLevel(String classLevel) { this.classLevel = classLevel; }
    public void setSubjects(String subjects) { this.subjects = subjects; }
    public void setDistricts(String districts) { this.districts = districts; }
    public void setMatchMark(String matchMark) { this.matchMark = matchMark; }
    public void setProfileIcon(String profileIcon) { this.profileIcon = profileIcon; }
    public void setMatchCreator(String matchCreator) { this.matchCreator = matchCreator; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }
    public void setRecipientAppId(String recipientAppId) { this.recipientAppId = recipientAppId; }

    // Helper method to get the display name based on request type
    public String getDisplayName(boolean isReceived, String currentUsername) {
        if (isReceived) {
            // For received requests, show the sender's name
            return matchCreator.equals("PS") ? psUsername : tutorUsername;
        } else {
            // For sent requests, show the recipient's name
            if (recipientUsername != null && !recipientUsername.isEmpty()) {
                return recipientUsername;
            }
            return currentUsername.equals(psUsername) ? tutorUsername : psUsername;
        }
    }

    // Helper method to determine if the request is from/to a PS
    public boolean isParentStudentRequest() {
        return matchCreator.equals("PS");
    }

    // Helper method to determine if the current user is the creator
    public boolean isCreatedByUser(String username) {
        return (matchCreator.equals("PS") && psUsername.equals(username)) ||
                (matchCreator.equals("T") && tutorUsername.equals(username));
    }
}