package com.example.circlea.setting;

import android.net.Uri;

// Model class representing a selected file with its description
public class FileItem {
    private Uri fileUri;
    private String description;

    public FileItem(Uri fileUri) {
        this.fileUri = fileUri;
        this.description = "";
    }

    public Uri getFileUri() {
        return fileUri;
    }

    public void setFileUri(Uri fileUri) {
        this.fileUri = fileUri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
} 