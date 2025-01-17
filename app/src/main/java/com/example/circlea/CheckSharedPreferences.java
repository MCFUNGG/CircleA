package com.example.circlea;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Map;

public class CheckSharedPreferences {

    private Context context;

    // Constructor to receive the context
    public CheckSharedPreferences(Context context) {
        this.context = context;
    }

    public void printSharedPreferences() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("CircleA", MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Log.d("SharedPreferences", key + ": " + value);
        }
    }
}