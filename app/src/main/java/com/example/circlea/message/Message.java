package com.example.circlea.message;

import java.util.Date;

public class Message {
    private String id;
    private String senderId;
    private String receiverId;
    private String title;
    private String content;
    private String type;
    private boolean isRead;
    private Date createdAt;

    public Message(String id, String senderId, String receiverId, String title, String content, String type, boolean isRead, Date createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public Date getCreatedAt() { return createdAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setType(String type) { this.type = type; }
    public void setRead(boolean read) { isRead = read; }
} 