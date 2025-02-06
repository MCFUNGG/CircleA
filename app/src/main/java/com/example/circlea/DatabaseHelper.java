package com.example.circlea;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.content.ContentValues;

import com.example.circlea.home.ApplicationItem;

import java.util.ArrayList;
import java.util.Arrays;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "circleA.db";
    private static final int DATABASE_VERSION = 2; // Increased version for schema update

    // Table for saved applications
    public static final String TABLE_SAVED_APPS = "saved_applications";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_APP_ID = "app_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_CLASS_LEVEL = "class_level";
    public static final String COLUMN_SUBJECTS = "subjects";
    public static final String COLUMN_DISTRICTS = "districts";
    public static final String COLUMN_FEE = "fee";
    public static final String COLUMN_APP_TYPE = "application_type"; // New column

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUsersTable = "CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, member_id TEXT)";
        db.execSQL(createUsersTable);

        // Create saved applications table with application_type column
        String createSavedAppsTable = "CREATE TABLE " + TABLE_SAVED_APPS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_APP_ID + " TEXT UNIQUE, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_CLASS_LEVEL + " TEXT, " +
                COLUMN_SUBJECTS + " TEXT, " +
                COLUMN_DISTRICTS + " TEXT, " +
                COLUMN_FEE + " TEXT, " +
                COLUMN_APP_TYPE + " TEXT)"; // Added new column
        db.execSQL(createSavedAppsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add application_type column to existing table
            db.execSQL("ALTER TABLE " + TABLE_SAVED_APPS +
                    " ADD COLUMN " + COLUMN_APP_TYPE + " TEXT DEFAULT 'student'");
        }
    }

    // Add application to saved list
    public boolean saveApplication(ApplicationItem app) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_APP_ID, app.getAppId());
        values.put(COLUMN_USERNAME, app.getUsername());
        values.put(COLUMN_CLASS_LEVEL, app.getClassLevel());
        values.put(COLUMN_SUBJECTS, String.join(",", app.getSubjects()));
        values.put(COLUMN_DISTRICTS, String.join(",", app.getDistricts()));
        values.put(COLUMN_FEE, app.getFee());
        values.put(COLUMN_APP_TYPE, app.getApplicationType());

        long result = db.insertWithOnConflict(TABLE_SAVED_APPS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    // Remove application from saved list
    public boolean removeApplication(String appId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_SAVED_APPS, COLUMN_APP_ID + "=?",
                new String[]{appId}) > 0;
    }

    // Check if application is saved
    public boolean isApplicationSaved(String appId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SAVED_APPS +
                " WHERE " + COLUMN_APP_ID + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{appId});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Get all saved applications of a specific type
    public ArrayList<ApplicationItem> getSavedApplicationsByType(String type) {
        ArrayList<ApplicationItem> applications = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_SAVED_APPS +
                " WHERE " + COLUMN_APP_TYPE + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{type});

        if (cursor.moveToFirst()) {
            do {
                String appId = cursor.getString(cursor.getColumnIndex(COLUMN_APP_ID));
                String username = cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME));
                String classLevel = cursor.getString(cursor.getColumnIndex(COLUMN_CLASS_LEVEL));
                String subjects = cursor.getString(cursor.getColumnIndex(COLUMN_SUBJECTS));
                String districts = cursor.getString(cursor.getColumnIndex(COLUMN_DISTRICTS));
                String fee = cursor.getString(cursor.getColumnIndex(COLUMN_FEE));

                ArrayList<String> subjectsList = new ArrayList<>(Arrays.asList(subjects.split(",")));
                ArrayList<String> districtsList = new ArrayList<>(Arrays.asList(districts.split(",")));

                ApplicationItem item = new ApplicationItem(
                        appId, subjectsList, classLevel, fee, districtsList,
                        "", "", username, type);
                applications.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return applications;
    }

    // Get all saved applications (for backward compatibility)
    public ArrayList<ApplicationItem> getAllSavedApplications() {
        ArrayList<ApplicationItem> applications = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_SAVED_APPS;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String appId = cursor.getString(cursor.getColumnIndex(COLUMN_APP_ID));
                String username = cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME));
                String classLevel = cursor.getString(cursor.getColumnIndex(COLUMN_CLASS_LEVEL));
                String subjects = cursor.getString(cursor.getColumnIndex(COLUMN_SUBJECTS));
                String districts = cursor.getString(cursor.getColumnIndex(COLUMN_DISTRICTS));
                String fee = cursor.getString(cursor.getColumnIndex(COLUMN_FEE));
                String type = cursor.getString(cursor.getColumnIndex(COLUMN_APP_TYPE));

                ArrayList<String> subjectsList = new ArrayList<>(Arrays.asList(subjects.split(",")));
                ArrayList<String> districtsList = new ArrayList<>(Arrays.asList(districts.split(",")));

                ApplicationItem item = new ApplicationItem(
                        appId, subjectsList, classLevel, fee, districtsList,
                        "", "", username, type != null ? type : "student");
                applications.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return applications;
    }
}