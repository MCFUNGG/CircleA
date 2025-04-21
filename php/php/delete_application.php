<?php
// 設置響應頭
header('Content-Type: application/json; charset=utf-8');

// 啟用詳細錯誤報告
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// 引入資料庫連接
include_once 'db_config.php';

// 取得POST參數並記錄
$app_id = isset($_POST['app_id']) ? $_POST['app_id'] : '';
$member_id = isset($_POST['member_id']) ? $_POST['member_id'] : '';
$app_creator = isset($_POST['app_creator']) ? $_POST['app_creator'] : '';

// 記錄詳細請求內容
error_log("Delete Application Request: app_id='$app_id', member_id='$member_id', app_creator='$app_creator'");
error_log("POST data: " . print_r($_POST, true));

// 驗證必要參數
if (empty($app_id) || empty($member_id) || empty($app_creator)) {
    error_log("Missing required parameters for delete application");
    echo json_encode([
        'success' => false,
        'message' => '缺少必要參數',
        'debug' => [
            'app_id' => $app_id,
            'member_id' => $member_id,
            'app_creator' => $app_creator
        ]
    ]);
    exit;
}

try {
    // 使用 getDbConnection 函數獲取數據庫連接
    $conn = getDbConnection();
    
    // 檢查連接
    if (!$conn) {
        throw new Exception("連接資料庫失敗");
    }
    
    error_log("Database connection successful");
    
    // 先檢查申請是否存在且屬於該用戶
    $check_query = "SELECT * FROM application WHERE app_id = ? AND member_id = ? AND app_creator = ?";
    error_log("Check query: $check_query with params: app_id=$app_id, member_id=$member_id, app_creator=$app_creator");
    
    $stmt = $conn->prepare($check_query);
    if (!$stmt) {
        throw new Exception("準備查詢語句失敗: " . $conn->error);
    }
    
    $stmt->bind_param('sis', $app_id, $member_id, $app_creator);
    $stmt->execute();
    $result = $stmt->get_result();
    
    error_log("Check query rows found: " . $result->num_rows);
    
    if ($result->num_rows == 0) {
        echo json_encode([
            'success' => false,
            'message' => '找不到該申請或您無權刪除該申請',
            'debug' => [
                'query' => $check_query,
                'app_id' => $app_id,
                'member_id' => $member_id,
                'app_creator' => $app_creator
            ]
        ]);
        exit;
    }
    
    // 開始事務處理
    $conn->begin_transaction();
    error_log("Transaction started");
    
    // 1. 刪除申請相關的科目
    $delete_subjects = "DELETE FROM application_subject WHERE app_id = ?";
    error_log("Delete subjects query: $delete_subjects with app_id=$app_id");
    
    $stmt = $conn->prepare($delete_subjects);
    if (!$stmt) {
        throw new Exception("準備刪除科目語句失敗: " . $conn->error);
    }
    
    $stmt->bind_param('i', $app_id);
    $stmt->execute();
    error_log("Subjects deleted: " . $stmt->affected_rows);
    
    // 2. 刪除申請相關的地區
    $delete_districts = "DELETE FROM application_district WHERE app_id = ?";
    error_log("Delete districts query: $delete_districts with app_id=$app_id");
    
    $stmt = $conn->prepare($delete_districts);
    if (!$stmt) {
        throw new Exception("準備刪除地區語句失敗: " . $conn->error);
    }
    
    $stmt->bind_param('i', $app_id);
    $stmt->execute();
    error_log("Districts deleted: " . $stmt->affected_rows);
    
    // 3. 刪除申請相關的日期
    $delete_dates = "DELETE FROM application_date WHERE app_id = ?";
    error_log("Delete dates query: $delete_dates with app_id=$app_id");
    
    $stmt = $conn->prepare($delete_dates);
    if (!$stmt) {
        throw new Exception("準備刪除日期語句失敗: " . $conn->error);
    }
    
    $stmt->bind_param('i', $app_id);
    $stmt->execute();
    error_log("Dates deleted: " . $stmt->affected_rows);
    
    // 4. 刪除應用本身
    $delete_application = "DELETE FROM application WHERE app_id = ? AND member_id = ? AND app_creator = ?";
    error_log("Delete application query: $delete_application with app_id=$app_id, member_id=$member_id, app_creator=$app_creator");
    
    $stmt = $conn->prepare($delete_application);
    if (!$stmt) {
        throw new Exception("準備刪除應用語句失敗: " . $conn->error);
    }
    
    $stmt->bind_param('sis', $app_id, $member_id, $app_creator);
    $stmt->execute();
    
    $affected_rows = $stmt->affected_rows;
    error_log("Application deleted: " . $affected_rows);
    
    if ($affected_rows > 0) {
        // 提交事務
        $conn->commit();
        error_log("Transaction committed successfully");
        
        echo json_encode([
            'success' => true,
            'message' => '申請已成功刪除'
        ]);
    } else {
        // 回滾事務
        $conn->rollback();
        error_log("Transaction rolled back - no rows affected");
        
        echo json_encode([
            'success' => false,
            'message' => '刪除申請失敗，請稍後再試',
            'debug' => [
                'query' => $delete_application,
                'error' => $conn->error,
                'app_id' => $app_id,
                'member_id' => $member_id,
                'app_creator' => $app_creator
            ]
        ]);
    }
    
} catch (Exception $e) {
    // 如果事務已啟動，回滾事務
    if (isset($conn) && $conn) {
        $conn->rollback();
        error_log("Transaction rolled back due to exception");
    }
    
    // 記錄錯誤
    error_log("Error deleting application: " . $e->getMessage());
    
    echo json_encode([
        'success' => false,
        'message' => '刪除過程中發生錯誤',
        'debug' => [
            'error_message' => $e->getMessage(),
            'trace' => $e->getTraceAsString()
        ]
    ]);
} finally {
    // 關閉資料庫連接
    if (isset($stmt)) {
        $stmt->close();
    }
    if (isset($conn) && $conn) {
        $conn->close();
    }
    error_log("Database connection closed");
}
?> 