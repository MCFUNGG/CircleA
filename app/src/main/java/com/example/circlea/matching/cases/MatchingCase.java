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

    // Getters
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

    // Get the appropriate username based on the match creator
    public String getUsername() {
        return matchCreator.equals("PS") ? psUsername : tutorUsername;
    }
}