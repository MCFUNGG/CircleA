package com.example.circlea.matching;

public class MatchingRequest {
    private String matchId;
    private String psUsername;
    private String psAppId;
    private String fee;
    private String classLevel;
    private String subjects;
    private String districts;
    private String matchMark;
    private String profileIcon;

    // Constructor remains the same
    public MatchingRequest(String matchId, String psAppId, String psUsername,String fee, String classLevel,
                           String subjects, String districts, String matchMark, String profileIcon) {
        this.matchId = matchId;
        this.psUsername = psUsername;
        this.psAppId = psAppId;
        this.fee = fee;
        this.classLevel = classLevel;
        this.subjects = subjects;
        this.districts = districts;
        this.matchMark = matchMark;
        this.profileIcon = profileIcon;
    }

    // Add this method to match what the adapter is looking for
    public String getRequestId() { return matchId; }  // This is the new method
    public String getPsUsername(){return psUsername;}
    public String getMatchId() { return matchId; }
    public String getPsAppId() { return psAppId; }
    public String getFee() { return fee; }
    public String getClassLevel() { return classLevel; }
    public String getSubjects() { return subjects; }
    public String getDistricts() { return districts; }
    public String getMatchMark() { return matchMark; }
    public String getProfileIcon() { return profileIcon; }
}