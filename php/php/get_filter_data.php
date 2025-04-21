<?php
/**
 * 獲取過濾器數據（年級、科目和地區）
 * 用於HomeFragment中的過濾功能
 */

// 設置響應頭
header('Content-Type: application/json; charset=utf-8');

// 包含數據庫配置文件
require_once 'db_config.php';

// 創建數據庫連接
$conn = new mysqli($servername, $username, $password, $dbname);

// 檢查連接
if ($conn->connect_error) {
    die(json_encode([
        'success' => false,
        'message' => '連接失敗: ' . $conn->connect_error
    ]));
}

// 設置字符集
$conn->set_charset("utf8mb4");

try {
    // 獲取年級數據
    $levelsSql = "SELECT class_level_id, class_level_name FROM class_level ORDER BY class_level_id";
    $levelsResult = $conn->query($levelsSql);
    
    if (!$levelsResult) {
        throw new Exception("獲取年級數據失敗: " . $conn->error);
    }
    
    $levels = [];
    while ($row = $levelsResult->fetch_assoc()) {
        $levels[] = $row;
    }
    
    // 獲取科目數據
    $subjectsSql = "SELECT subject_id, subject_name FROM subject ORDER BY subject_id";
    $subjectsResult = $conn->query($subjectsSql);
    
    if (!$subjectsResult) {
        throw new Exception("獲取科目數據失敗: " . $conn->error);
    }
    
    $subjects = [];
    while ($row = $subjectsResult->fetch_assoc()) {
        $subjects[] = $row;
    }
    
    // 獲取地區數據
    $districtsSql = "SELECT district_id, district_name FROM district ORDER BY district_id";
    $districtsResult = $conn->query($districtsSql);
    
    if (!$districtsResult) {
        throw new Exception("獲取地區數據失敗: " . $conn->error);
    }
    
    $districts = [];
    while ($row = $districtsResult->fetch_assoc()) {
        $districts[] = $row;
    }
    
    // 構建響應
    $response = [
        'success' => true,
        'levels' => $levels,
        'subjects' => $subjects,
        'districts' => $districts
    ];
    
    echo json_encode($response);
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
} finally {
    // 關閉數據庫連接
    $conn->close();
}
?> 