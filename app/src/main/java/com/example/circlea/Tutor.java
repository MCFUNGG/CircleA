package com.example.circlea;

public class Tutor {
    private String name;
    private String subject;
    private float rating;

    public Tutor(String name, String subject, float rating) {
        this.name = name;
        this.subject = subject;
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public float getRating() {
        return rating;
    }
}