package com.example.circlea.data.model;

import com.google.gson.annotations.SerializedName;

public class StudentContactResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("student")
    private StudentContact student;

    @SerializedName("status")
    private String status;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public StudentContact getStudent() {
        return student;
    }

    public String getStatus() {
        return status;
    }

    public static class StudentContact {
        @SerializedName("name")
        private String name;

        @SerializedName("phone")
        private String phone;

        @SerializedName("email")
        private String email;

        public String getName() {
            return name;
        }

        public String getPhone() {
            return phone;
        }

        public String getEmail() {
            return email;
        }
    }
} 