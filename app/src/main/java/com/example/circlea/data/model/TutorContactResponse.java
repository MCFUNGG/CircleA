package com.example.circlea.data.model;

import com.google.gson.annotations.SerializedName;

public class TutorContactResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("tutor")
    private TutorContact tutor;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public TutorContact getTutor() {
        return tutor;
    }

    public static class TutorContact {
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