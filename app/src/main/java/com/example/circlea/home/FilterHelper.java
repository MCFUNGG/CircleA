package com.example.circlea.home;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.circlea.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 過濾器幫助類，處理應用程序中的過濾邏輯
 */
public class FilterHelper {
    private static final String TAG = "FilterHelper";
    
    private View filterLayout;
    private Context context;
    
    private EditText minFeeEditText;
    private EditText maxFeeEditText;
    private Spinner classLevelSpinner;
    private ChipGroup subjectChipGroup;
    private ChipGroup districtChipGroup;
    private Button applyFilterButton;
    private Button resetFilterButton;
    
    private String selectedClassLevel = null;
    private Set<String> selectedSubjects = new HashSet<>();
    private Set<String> selectedDistricts = new HashSet<>();
    private int minFee = -1;
    private int maxFee = -1;
    
    private FilterListener filterListener;
    
    /**
     * 過濾器監聽器接口
     */
    public interface FilterListener {
        void onApplyFilter(FilterCriteria filterCriteria);
        void onResetFilter();
    }
    
    /**
     * 過濾條件類
     */
    public static class FilterCriteria {
        private int minFee;
        private int maxFee;
        private String classLevel;
        private Set<String> subjects;
        private Set<String> districts;
        
        public FilterCriteria(int minFee, int maxFee, String classLevel, 
                              Set<String> subjects, Set<String> districts) {
            this.minFee = minFee;
            this.maxFee = maxFee;
            this.classLevel = classLevel;
            this.subjects = subjects;
            this.districts = districts;
        }
        
        public int getMinFee() { return minFee; }
        public int getMaxFee() { return maxFee; }
        public String getClassLevel() { return classLevel; }
        public Set<String> getSubjects() { return subjects; }
        public Set<String> getDistricts() { return districts; }
        
        /**
         * 過濾應用項目列表
         */
        public ArrayList<ApplicationItem> filter(ArrayList<ApplicationItem> items) {
            Log.d(TAG, "開始過濾，原始項目數量: " + items.size());
            Log.d(TAG, "過濾條件 - 最高價格: " + maxFee + ", 年級: " + classLevel + 
                  ", 科目數量: " + subjects.size() + ", 地區數量: " + districts.size());
            
            ArrayList<ApplicationItem> filteredItems = new ArrayList<>();
            
            for (ApplicationItem item : items) {
                boolean shouldAdd = true;
                
                // 檢查費用
                int itemFee;
                try {
                    String feeStr = item.getFee().replaceAll("[^0-9]", "");
                    itemFee = TextUtils.isEmpty(feeStr) ? 0 : Integer.parseInt(feeStr);
                    Log.d(TAG, "處理項目 - 原始費用: " + item.getFee() + ", 轉換後費用: " + itemFee);
                } catch (NumberFormatException e) {
                    itemFee = 0;
                    Log.e(TAG, "費用解析錯誤: " + item.getFee());
                }
                
                // 只在有設置價格範圍時進行檢查
                if (maxFee >= 0 && itemFee > maxFee) {
                    shouldAdd = false;
                    Log.d(TAG, "項目費用 " + itemFee + " 超過最高限制 " + maxFee + "，排除此項目");
                }
                
                // 檢查年級（如果已選擇）
                if (!TextUtils.isEmpty(classLevel) && !classLevel.equals("所有年級") && 
                    !item.getClassLevel().equals(classLevel)) {
                    shouldAdd = false;
                    Log.d(TAG, "年級不匹配 - 要求: " + classLevel + ", 實際: " + item.getClassLevel());
                }
                
                // 檢查科目（如果已選擇）
                if (!subjects.isEmpty()) {
                    boolean hasMatchingSubject = false;
                    for (String subject : item.getSubjects()) {
                        if (subjects.contains(subject)) {
                            hasMatchingSubject = true;
                            break;
                        }
                    }
                    if (!hasMatchingSubject) {
                        shouldAdd = false;
                        Log.d(TAG, "沒有匹配的科目 - 要求: " + subjects + ", 實際: " + item.getSubjects());
                    }
                }
                
                // 檢查地區（如果已選擇）
                if (!districts.isEmpty()) {
                    boolean hasMatchingDistrict = false;
                    for (String district : item.getDistricts()) {
                        if (districts.contains(district)) {
                            hasMatchingDistrict = true;
                            break;
                        }
                    }
                    if (!hasMatchingDistrict) {
                        shouldAdd = false;
                        Log.d(TAG, "沒有匹配的地區 - 要求: " + districts + ", 實際: " + item.getDistricts());
                    }
                }
                
                // 如果通過所有過濾條件，添加到過濾後的列表
                if (shouldAdd) {
                    filteredItems.add(item);
                    Log.d(TAG, "項目通過所有過濾條件，已添加到結果列表");
                }
            }
            
            Log.d(TAG, "過濾完成，結果數量: " + filteredItems.size());
            return filteredItems;
        }
        
        /**
         * 檢查過濾條件是否為空
         */
        public boolean isEmpty() {
            return maxFee < 0 && 
                   (classLevel == null || classLevel.equals("所有年級")) && 
                   subjects.isEmpty() && districts.isEmpty();
        }
    }
    
    /**
     * 構造函數
     */
    public FilterHelper(View filterLayout, Context context) {
        this.filterLayout = filterLayout;
        this.context = context;
        
        initViews();
        setupListeners();
    }
    
    /**
     * 初始化視圖
     */
    private void initViews() {
        minFeeEditText = filterLayout.findViewById(R.id.min_fee);
        maxFeeEditText = filterLayout.findViewById(R.id.max_fee);
        classLevelSpinner = filterLayout.findViewById(R.id.class_level_spinner);
        subjectChipGroup = filterLayout.findViewById(R.id.subject_chip_group);
        districtChipGroup = filterLayout.findViewById(R.id.district_chip_group);
        applyFilterButton = filterLayout.findViewById(R.id.apply_filter_button);
        resetFilterButton = filterLayout.findViewById(R.id.reset_filter_button);
    }
    
    /**
     * 設置監聽器
     */
    private void setupListeners() {
        // 應用過濾按鈕
        applyFilterButton.setOnClickListener(v -> {
            readFilterValues();
            if (filterListener != null) {
                FilterCriteria criteria = new FilterCriteria(
                        minFee, maxFee, selectedClassLevel, 
                        selectedSubjects, selectedDistricts);
                filterListener.onApplyFilter(criteria);
            }
        });
        
        // 重置過濾按鈕
        resetFilterButton.setOnClickListener(v -> {
            resetFilter();
            if (filterListener != null) {
                filterListener.onResetFilter();
            }
        });
        
        // 年級下拉選擇
        classLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedClassLevel = position == 0 ? null : parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedClassLevel = null;
            }
        });
    }
    
    /**
     * 讀取過濾值
     */
    private void readFilterValues() {
        // 讀取費用範圍
        try {
            String minFeeStr = minFeeEditText.getText().toString().trim();
            minFee = TextUtils.isEmpty(minFeeStr) ? -1 : Integer.parseInt(minFeeStr);
        } catch (NumberFormatException e) {
            minFee = -1;
        }
        
        try {
            String maxFeeStr = maxFeeEditText.getText().toString().trim();
            maxFee = TextUtils.isEmpty(maxFeeStr) ? -1 : Integer.parseInt(maxFeeStr);
        } catch (NumberFormatException e) {
            maxFee = -1;
        }
        
        // 檢查費用範圍邏輯
        if (maxFee >= 0 && minFee > maxFee) {
            Toast.makeText(context, "最低價格不能大於最高價格", Toast.LENGTH_SHORT).show();
            minFee = -1;
            minFeeEditText.setText("");
        }
        
        // 獲取所選科目
        selectedSubjects.clear();
        Chip allSubjectsChip = (Chip) subjectChipGroup.getChildAt(0);
        
        // 如果"所有科目"被選中，不添加任何具體科目
        if (!allSubjectsChip.isChecked()) {
            // 只有當"所有科目"未被選中時，才收集選中的具體科目
            for (int i = 1; i < subjectChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) subjectChipGroup.getChildAt(i);
                if (chip.isChecked()) {
                    selectedSubjects.add(chip.getText().toString());
                }
            }
        }
        
        // 獲取所選地區
        selectedDistricts.clear();
        Chip allDistrictsChip = (Chip) districtChipGroup.getChildAt(0);
        
        // 如果"所有地區"被選中，不添加任何具體地區
        if (!allDistrictsChip.isChecked()) {
            // 只有當"所有地區"未被選中時，才收集選中的具體地區
            for (int i = 1; i < districtChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) districtChipGroup.getChildAt(i);
                if (chip.isChecked()) {
                    selectedDistricts.add(chip.getText().toString());
                }
            }
        }
    }
    
    /**
     * 重置過濾器
     */
    public void resetFilter() {
        minFeeEditText.setText("");
        maxFeeEditText.setText("");
        classLevelSpinner.setSelection(0); // 第一個是"所有年級"
        
        // 重置科目選擇（只選中"所有科目"）
        Chip allSubjectsChip = (Chip) subjectChipGroup.getChildAt(0);
        allSubjectsChip.setChecked(true);
        for (int i = 1; i < subjectChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) subjectChipGroup.getChildAt(i);
            chip.setChecked(false);
        }
        
        // 重置地區選擇（只選中"所有地區"）
        Chip allDistrictsChip = (Chip) districtChipGroup.getChildAt(0);
        allDistrictsChip.setChecked(true);
        for (int i = 1; i < districtChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) districtChipGroup.getChildAt(i);
            chip.setChecked(false);
        }
        
        // 重置存儲的值
        minFee = -1;
        maxFee = -1;
        selectedClassLevel = null;
        selectedSubjects.clear();
        selectedDistricts.clear();
    }
    
    /**
     * 設置監聽器
     */
    public void setFilterListener(FilterListener listener) {
        this.filterListener = listener;
    }
    
    /**
     * 設置年級選項
     */
    public void setClassLevelItems(List<String> classLevels) {
        List<String> items = new ArrayList<>();
        items.add(context.getString(R.string.all_grades)); // 添加默認選項
        items.addAll(classLevels);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classLevelSpinner.setAdapter(adapter);
    }
    
    /**
     * 設置科目選項
     */
    private void setupChip(Chip chip, boolean isAllOption) {
        chip.setCheckable(true);
        chip.setChecked(isAllOption);
        chip.setChipBackgroundColorResource(isAllOption ? R.color.chip_background_selected : R.color.chip_background_normal);
        chip.setTextColor(context.getResources().getColor(isAllOption ? R.color.chip_text_color_selected : R.color.chip_text_color_normal));
        chip.setChipIconVisible(false);
        chip.setCheckedIconVisible(false);  // 移除勾選圖標
        chip.setRippleColor(context.getResources().getColorStateList(R.color.transparent));
        chip.setElevation(2f);
    }

    private void updateChipState(Chip chip, boolean isChecked) {
        chip.setChipBackgroundColorResource(isChecked ? R.color.chip_background_selected : R.color.chip_background_normal);
        chip.setTextColor(context.getResources().getColor(isChecked ? R.color.chip_text_color_selected : R.color.chip_text_color_normal));
    }

    public void setSubjectItems(List<String> subjects) {
        subjectChipGroup.removeAllViews();
        selectedSubjects.clear();
        
        // 添加"所有科目"選項
        Chip allSubjectsChip = new Chip(context);
        allSubjectsChip.setText(context.getString(R.string.all_subjects));
        setupChip(allSubjectsChip, true);
        subjectChipGroup.addView(allSubjectsChip);
        
        // 監聽"所有科目"的狀態
        allSubjectsChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                updateChipState(allSubjectsChip, isChecked);
                if (isChecked) {
                    for (int i = 1; i < subjectChipGroup.getChildCount(); i++) {
                        Chip chip = (Chip) subjectChipGroup.getChildAt(i);
                        chip.setChecked(false);
                        updateChipState(chip, false);
                    }
                }
            }
        });
        
        // 添加其他科目選項
        for (String subject : subjects) {
            Chip chip = new Chip(context);
            chip.setText(subject);
            setupChip(chip, false);
            subjectChipGroup.addView(chip);
            
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    updateChipState(chip, isChecked);
                    if (isChecked) {
                        allSubjectsChip.setChecked(false);
                        updateChipState(allSubjectsChip, false);
                    } else {
                        boolean anySelected = false;
                        for (int i = 1; i < subjectChipGroup.getChildCount(); i++) {
                            if (((Chip) subjectChipGroup.getChildAt(i)).isChecked()) {
                                anySelected = true;
                                break;
                            }
                        }
                        if (!anySelected) {
                            allSubjectsChip.setChecked(true);
                            updateChipState(allSubjectsChip, true);
                        }
                    }
                }
            });
        }
    }
    
    /**
     * 設置地區選項
     */
    public void setDistrictItems(List<String> districts) {
        districtChipGroup.removeAllViews();
        selectedDistricts.clear();
        
        // 添加"所有地區"選項
        Chip allDistrictsChip = new Chip(context);
        allDistrictsChip.setText(context.getString(R.string.all_districts));
        setupChip(allDistrictsChip, true);
        districtChipGroup.addView(allDistrictsChip);
        
        // 監聽"所有地區"的狀態
        allDistrictsChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                updateChipState(allDistrictsChip, isChecked);
                if (isChecked) {
                    for (int i = 1; i < districtChipGroup.getChildCount(); i++) {
                        Chip chip = (Chip) districtChipGroup.getChildAt(i);
                        chip.setChecked(false);
                        updateChipState(chip, false);
                    }
                }
            }
        });
        
        // 添加其他地區選項
        for (String district : districts) {
            Chip chip = new Chip(context);
            chip.setText(district);
            setupChip(chip, false);
            districtChipGroup.addView(chip);
            
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    updateChipState(chip, isChecked);
                    if (isChecked) {
                        allDistrictsChip.setChecked(false);
                        updateChipState(allDistrictsChip, false);
                    } else {
                        boolean anySelected = false;
                        for (int i = 1; i < districtChipGroup.getChildCount(); i++) {
                            if (((Chip) districtChipGroup.getChildAt(i)).isChecked()) {
                                anySelected = true;
                                break;
                            }
                        }
                        if (!anySelected) {
                            allDistrictsChip.setChecked(true);
                            updateChipState(allDistrictsChip, true);
                        }
                    }
                }
            });
        }
    }
    
    /**
     * 顯示過濾器
     */
    public void show() {
        filterLayout.setVisibility(View.VISIBLE);
    }
    
    /**
     * 隱藏過濾器
     */
    public void hide() {
        filterLayout.setVisibility(View.GONE);
    }
    
    /**
     * 切換過濾器的可見性
     */
    public void toggle() {
        if (filterLayout.getVisibility() == View.VISIBLE) {
            filterLayout.setVisibility(View.GONE);
        } else {
            filterLayout.setVisibility(View.VISIBLE);
        }
    }
} 