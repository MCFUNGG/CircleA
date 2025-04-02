package com.example.circlea;

public class Advertisement {
    private String id;
    private String imageUrl;

    public Advertisement(String id, String imageUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public String getImageUrl() { return imageUrl; }
}