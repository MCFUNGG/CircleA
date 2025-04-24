<?php
header("Content-Type: application/json");
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

// 記錄腳本執行開始
error_log("cancel_booking_and_find_other_tutor.php started");

// 嘗試引入資料庫配置文件
require_once 'db_config.php';

// 創建數據庫連接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode([
        "success" => false, 
        "message" => "數據庫連接失敗: " . mysqli_connect_error(),
        "status" => "error"
    ]);
    exit;
}

try {
    // 開始事務，確保所有操作要麼全部成功，要麼全部失敗
    mysqli_begin_transaction($connect);
    
    // 獲取POST參數
    $case_id = isset($_POST['case_id']) ? $_POST['case_id'] : null;
    $student_id = isset($_POST['student_id']) ? $_POST['student_id'] : null;
    $booking_id = isset($_POST['booking_id']) ? $_POST['booking_id'] : null;
    $action = isset($_POST['action']) ? $_POST['action'] : null;
    
    error_log("Received parameters: case_id=$case_id, student_id=$student_id, booking_id=$booking_id, action=$action");
    
    // 驗證必要參數
    if (!$case_id || !$student_id) {
        throw new Exception("Missing required parameters: case_id and student_id are required");
    }
    
    // 如果沒有提供booking_id，嘗試根據case_id和student_id查找
    if (!$booking_id) {
        $booking_query = "SELECT booking_id FROM booking 
                         WHERE match_id = ? AND student_id = ? AND status = 'confirmed'
                         ORDER BY created_at DESC LIMIT 1";
        
        $booking_stmt = mysqli_prepare($connect, $booking_query);
        if (!$booking_stmt) {
            throw new Exception("無法準備查詢: " . mysqli_error($connect));
        }
        
        mysqli_stmt_bind_param($booking_stmt, "ss", $case_id, $student_id);
        if (!mysqli_stmt_execute($booking_stmt)) {
            throw new Exception("執行查詢失敗: " . mysqli_stmt_error($booking_stmt));
        }
        
        $booking_result = mysqli_stmt_get_result($booking_stmt);
        if (mysqli_num_rows($booking_result) == 0) {
            throw new Exception("未找到匹配的confirmed預約記錄");
        }
        
        $booking_row = mysqli_fetch_assoc($booking_result);
        $booking_id = $booking_row['booking_id'];
        mysqli_stmt_close($booking_stmt);
        
        error_log("Found booking_id: $booking_id");
    }
    
    // 1. 刪除first_lesson表中的記錄
    $delete_lesson_query = "DELETE FROM first_lesson WHERE booking_id = ?";
    $delete_lesson_stmt = mysqli_prepare($connect, $delete_lesson_query);
    if (!$delete_lesson_stmt) {
        throw new Exception("無法準備刪除first_lesson查詢: " . mysqli_error($connect));
    }
    
    mysqli_stmt_bind_param($delete_lesson_stmt, "i", $booking_id);
    if (!mysqli_stmt_execute($delete_lesson_stmt)) {
        throw new Exception("刪除first_lesson記錄失敗: " . mysqli_stmt_error($delete_lesson_stmt));
    }
    
    $affected_rows = mysqli_stmt_affected_rows($delete_lesson_stmt);
    mysqli_stmt_close($delete_lesson_stmt);
    error_log("Deleted $affected_rows records from first_lesson table");
    
    // 2. 更新booking狀態為cancelled（或刪除）
    $update_booking_query = "UPDATE booking SET status = 'cancelled', student_id = NULL WHERE booking_id = ?";
    $update_booking_stmt = mysqli_prepare($connect, $update_booking_query);
    if (!$update_booking_stmt) {
        throw new Exception("無法準備更新booking查詢: " . mysqli_error($connect));
    }
    
    mysqli_stmt_bind_param($update_booking_stmt, "i", $booking_id);
    if (!mysqli_stmt_execute($update_booking_stmt)) {
        throw new Exception("更新booking狀態失敗: " . mysqli_stmt_error($update_booking_stmt));
    }
    
    $affected_rows = mysqli_stmt_affected_rows($update_booking_stmt);
    mysqli_stmt_close($update_booking_stmt);
    error_log("Updated $affected_rows records in booking table");
    
    // 3. 更新match表，設置狀態為可找其他導師
    $update_match_query = "UPDATE `match` SET status = 'open' WHERE match_id = ?";
    $update_match_stmt = mysqli_prepare($connect, $update_match_query);
    if (!$update_match_stmt) {
        throw new Exception("無法準備更新match查詢: " . mysqli_error($connect));
    }
    
    mysqli_stmt_bind_param($update_match_stmt, "s", $case_id);
    if (!mysqli_stmt_execute($update_match_stmt)) {
        throw new Exception("更新match狀態失敗: " . mysqli_stmt_error($update_match_stmt));
    }
    
    $affected_rows = mysqli_stmt_affected_rows($update_match_stmt);
    mysqli_stmt_close($update_match_stmt);
    error_log("Updated $affected_rows records in match table");
    
    // 4. 插入通知記錄，通知導師學生已取消並尋找其他導師
    $tutor_query = "SELECT tutor_id FROM booking WHERE booking_id = ?";
    $tutor_stmt = mysqli_prepare($connect, $tutor_query);
    mysqli_stmt_bind_param($tutor_stmt, "i", $booking_id);
    mysqli_stmt_execute($tutor_stmt);
    $tutor_result = mysqli_stmt_get_result($tutor_stmt);
    
    if ($tutor_row = mysqli_fetch_assoc($tutor_result)) {
        $tutor_id = $tutor_row['tutor_id'];
        
        // 獲取學生姓名
        $student_name_query = "SELECT username FROM member WHERE member_id = ?";
        $student_name_stmt = mysqli_prepare($connect, $student_name_query);
        mysqli_stmt_bind_param($student_name_stmt, "s", $student_id);
        mysqli_stmt_execute($student_name_stmt);
        $student_name_result = mysqli_stmt_get_result($student_name_stmt);
        $student_name_row = mysqli_fetch_assoc($student_name_result);
        $student_name = $student_name_row ? $student_name_row['username'] : "學生";
        mysqli_stmt_close($student_name_stmt);
        
        // 插入通知
        $notification_query = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                              VALUES (?, '課程取消通知', ?, 'booking_cancelled', ?, 0, NOW())";
        $notification_stmt = mysqli_prepare($connect, $notification_query);
        $message = "學生 $student_name 已取消課程並選擇尋找其他導師。";
        
        mysqli_stmt_bind_param($notification_stmt, "ssi", $tutor_id, $message, $booking_id);
        mysqli_stmt_execute($notification_stmt);
        mysqli_stmt_close($notification_stmt);
        
        error_log("Notification inserted for tutor: $tutor_id");
    }
    mysqli_stmt_close($tutor_stmt);
    
    // 提交事務
    mysqli_commit($connect);
    
    // 返回成功響應
    echo json_encode([
        "success" => true,
        "message" => "操作成功完成。您現在可以尋找其他導師。",
        "status" => "success"
    ]);
    
    error_log("cancel_booking_and_find_other_tutor.php completed successfully");
    
} catch (Exception $e) {
    // 回滾事務
    if (isset($connect) && $connect instanceof mysqli) {
        mysqli_rollback($connect);
    }
    
    error_log("Error in cancel_booking_and_find_other_tutor.php: " . $e->getMessage());
    
    // 返回錯誤響應
    echo json_encode([
        "success" => false,
        "message" => $e->getMessage(),
        "status" => "error"
    ]);
} finally {
    // 關閉連接
    if (isset($connect) && $connect instanceof mysqli) {
        mysqli_close($connect);
    }
    
    error_log("cancel_booking_and_find_other_tutor.php finished");
}
?> 