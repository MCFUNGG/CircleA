package com.example.circlea.matching.cases;
public class MatchingCase {
    private String matchId;
    private String psAppId;
    private String tutorAppId;
    private String psUsername;
    private String tutorUsername;
    private String fee;
    private String classLevel;
    private String subjects;
    private String districts;
    private String status;
    private String profileIcon;
    private String matchCreator;
    // Add new fields
    private String psId;
    private String tutorId;
    private String psProfileIcon;
    private String tutorProfileIcon;

    public MatchingCase(String matchId, String psAppId, String tutorAppId,
                        String psUsername, String tutorUsername, String fee,
                        String classLevel, String subjects, String districts,
                        String status, String profileIcon, String matchCreator) {
        this.matchId = matchId;
        this.psAppId = psAppId;
        this.tutorAppId = tutorAppId;
        this.psUsername = psUsername;
        this.tutorUsername = tutorUsername;
        this.fee = fee;
        this.classLevel = classLevel;
        this.subjects = subjects;
        this.districts = districts;
        this.status = status;
        this.profileIcon = profileIcon;
        this.matchCreator = matchCreator;
    }

    // Existing getters
    public String getMatchId() { return matchId; }
    public String getPsAppId() { return psAppId; }
    public String getTutorAppId() { return tutorAppId; }
    public String getPsUsername() { return psUsername; }
    public String getTutorUsername() { return tutorUsername; }
    public String getFee() { return fee; }
    public String getClassLevel() { return classLevel; }
    public String getSubjects() { return subjects; }
    public String getDistricts() { return districts; }
    public String getStatus() { return status; }
    public String getProfileIcon() { return profileIcon; }
    public String getMatchCreator() { return matchCreator; }

    // Add new getters
    public String getPsId() { return psId; }
    public String getTutorId() { return tutorId; }
    public String getPsProfileIcon() { return psProfileIcon; }
    public String getTutorProfileIcon() { return tutorProfileIcon; }

    // Add setters for the new fields
    public void setPsId(String psId) { this.psId = psId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }
    public void setPsProfileIcon(String psProfileIcon) { this.psProfileIcon = psProfileIcon; }
    public void setTutorProfileIcon(String tutorProfileIcon) { this.tutorProfileIcon = tutorProfileIcon; }

    // Get the appropriate username based on the match creator
    public String getUsername() {
        return matchCreator.equals("PS") ? psUsername : tutorUsername;
    }
}