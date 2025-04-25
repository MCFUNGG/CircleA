package com.example.circlea.home;

public class Advertisement {
    private String id;
    private String imageUrl;
    private String linkUrl;

    public Advertisement(String id, String imageUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.linkUrl = null;
    }
    
    public Advertisement(String id, String imageUrl, String linkUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
    }

    public String getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getLinkUrl() { return linkUrl; }
    public boolean hasLink() { return linkUrl != null && !linkUrl.isEmpty(); }
} 