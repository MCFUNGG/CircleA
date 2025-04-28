package com.example.circlea;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.example.circlea.application.ApplicationFragment;
import com.example.circlea.home.HomeFragment;
import com.example.circlea.matching.Matching;
import com.example.circlea.setting.SettingFragment;
import com.example.circlea.utils.TranslationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LanguageManager {
    private static final String PREFS_NAME = "LanguagePrefs";
    private static final String LANGUAGE_KEY = "SelectedLanguage";

    private Context context;
    private final SharedPreferences preferences;

    public LanguageManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void switchLanguage(Activity activity) {
        // Store current fragment state before changing language
        if (activity instanceof Home) {
            // Find which fragment is currently visible
            Fragment currentFragment = ((Home) activity).getSupportFragmentManager()
                    .findFragmentById(R.id.content_frame);

            String fragmentType = Home.FRAGMENT_HOME; // Default to home

            if (currentFragment instanceof ApplicationFragment) {
                fragmentType = Home.FRAGMENT_APPLICATION;
            } else if (currentFragment instanceof HomeFragment) {
                fragmentType = Home.FRAGMENT_HOME;
            } else if (currentFragment instanceof SettingFragment) {
                fragmentType = Home.FRAGMENT_SETTING;
            } else if (currentFragment instanceof Matching) {
                fragmentType = Home.FRAGMENT_MATCHING;
            }

            // Save fragment state SYNCHRONOUSLY before language change
            SharedPreferences prefs = activity.getSharedPreferences("FragmentState", Context.MODE_PRIVATE);
            prefs.edit().putString("current_fragment", fragmentType).commit();
            Log.d("LanguageManager", "SAVED FRAGMENT STATE: " + fragmentType);
        }

        // Continue with language switch
        String currentLang = getCurrentLanguage();
        if (currentLang.equals("zh")) {
            setLanguage("en", activity);
        } else {
            setLanguage("zh", activity);
        }
    }
    
    public void setLanguage(String languageCode, Activity activity) {
        try {
            // Save the selected language
            preferences.edit().putString(LANGUAGE_KEY, languageCode).apply();
            Log.d("LanguageManager", "Language code set to: " + languageCode);

            // Create locale with language and region if needed
            Locale locale;
            if (languageCode.equals("zh")) {
                locale = Locale.TRADITIONAL_CHINESE;
            } else {
                locale = Locale.ENGLISH;
            }

            // Set the locale as default
            Locale.setDefault(locale);

            // Update configuration for context
            Resources resources = context.getResources();
            Configuration config = new Configuration(resources.getConfiguration());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale);
                context = context.createConfigurationContext(config);
            } else {
                config.locale = locale;
                resources.updateConfiguration(config, resources.getDisplayMetrics());
            }

            // Update activity configuration if provided
            if (activity != null) {
                Resources activityResources = activity.getResources();
                Configuration activityConfig = new Configuration(activityResources.getConfiguration());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    activityConfig.setLocale(locale);
                } else {
                    activityConfig.locale = locale;
                }

                activityResources.updateConfiguration(activityConfig, activityResources.getDisplayMetrics());

                // If Home activity, refresh the navigation menu before restarting
                if (activity instanceof Home) {
                    ((Home) activity).refreshNavigationMenu();
                }

                // Restart activity to apply changes
                Intent intent = activity.getIntent();
                activity.finish();
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0); // Prevent animation during restart
            }
        } catch (Exception e) {
            Log.e("LanguageManager", "Error setting language", e);
        }
    }

    public String getCurrentLanguage() {
        return preferences.getString(LANGUAGE_KEY, "en");
    }

    public void applyLanguage() {
        setLanguage(getCurrentLanguage(), null);
    }
    
    /**
     * 翻译单个数据库字段
     * @param fieldText 数据库中的文本
     * @param fieldType 字段类型 (district, subject, class_level, status)
     * @return 翻译后的文本
     */
    public String translateDatabaseField(String fieldText, String fieldType) {
        if (getCurrentLanguage().equals("en")) {
            return fieldText; // 如果是英文，直接返回
        }
        
        switch (fieldType) {
            case "district":
                return TranslationHelper.translateDistrict(context, fieldText);
            case "subject":
                return TranslationHelper.translateSubject(context, fieldText);
            case "class_level":
                return TranslationHelper.translateClassLevel(context, fieldText);
            case "status":
                return TranslationHelper.translateStatus(context, fieldText);
            default:
                return fieldText;
        }
    }
    
    /**
     * 翻译逗号分隔的多个项目（如多个科目、地区等）
     * @param commaSeparatedText 逗号分隔的文本
     * @param fieldType 字段类型
     * @return 翻译后的文本
     */
    public String translateCommaSeparatedList(String commaSeparatedText, String fieldType) {
        if (commaSeparatedText == null || commaSeparatedText.isEmpty() || 
            getCurrentLanguage().equals("en")) {
            return commaSeparatedText;
        }
        
        String[] items = commaSeparatedText.split(",\\s*");
        List<String> translatedItems = new ArrayList<>();
        
        for (String item : items) {
            String trimmedItem = item.trim();
            if (!trimmedItem.isEmpty()) {
                translatedItems.add(translateDatabaseField(trimmedItem, fieldType));
            }
        }
        
        return String.join(", ", translatedItems);
    }
    
    /**
     * 翻译地区列表
     */
    public String translateDistrictList(String commaSeparatedDistricts) {
        return translateCommaSeparatedList(commaSeparatedDistricts, "district");
    }
    
    /**
     * 翻译科目列表
     */
    public String translateSubjectList(String commaSeparatedSubjects) {
        return translateCommaSeparatedList(commaSeparatedSubjects, "subject");
    }
    
    /**
     * 判断当前是否为中文环境
     */
    public boolean isChineseLanguage() {
        return "zh".equals(getCurrentLanguage());
    }
}