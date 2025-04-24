<?php
header("Content-Type: application/json");

// Database connection parameters
require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode([
        "success" => false, 
        "message" => "Database connection failed: " . mysqli_connect_error()
    ]);
    exit;
}

try {
    mysqli_begin_transaction($connect);
    
    // 记录运行状态
    error_log("reset_booking_status.php started");

    // 获取 POST 参数
    $case_id = $_POST['case_id'] ?? null;
    $student_id = $_POST['student_id'] ?? null;
    $reason = $_POST['reason'] ?? null;
    
    error_log("Parameters: case_id=$case_id, student_id=$student_id, reason=$reason");

    // 验证必要参数
    if (!$case_id || !$student_id) {
        throw new Exception("Required parameters are missing.");
    }

    // 获取需要重置的 booking 记录
    $booking_query = "SELECT b.booking_id, b.tutor_id, b.start_time, b.end_time
                     FROM booking b
                     JOIN first_lesson fl ON b.booking_id = fl.booking_id
                     WHERE b.match_id = ? 
                     AND b.student_id = ? 
                     AND b.status = 'confirmed'
                     AND (fl.ps_response = 'incomplete' AND fl.t_response = 'incomplete')
                     ORDER BY b.booking_id ASC 
                     LIMIT 1";
    
    $booking_stmt = mysqli_prepare($connect, $booking_query);
    mysqli_stmt_bind_param($booking_stmt, "ss", $case_id, $student_id);
    mysqli_stmt_execute($booking_stmt);
    $booking_result = mysqli_stmt_get_result($booking_stmt);
    $booking_row = mysqli_fetch_assoc($booking_result);

    if (!$booking_row) {
        throw new Exception("No booking found that needs to be reset");
    }

    $booking_id = $booking_row['booking_id'];
    $tutor_id = $booking_row['tutor_id'];
    $start_time = $booking_row['start_time'];
    $end_time = $booking_row['end_time'];
    
    error_log("Found booking_id: $booking_id to reset");

    // 删除 first_lesson 记录
    $delete_lesson_query = "DELETE FROM first_lesson WHERE booking_id = ?";
    $delete_lesson_stmt = mysqli_prepare($connect, $delete_lesson_query);
    mysqli_stmt_bind_param($delete_lesson_stmt, "i", $booking_id);
    
    if (!mysqli_stmt_execute($delete_lesson_stmt)) {
        throw new Exception("Failed to delete first lesson record: " . mysqli_error($connect));
    }
    
    error_log("Deleted first_lesson record for booking_id: $booking_id");

    // 更新 booking 状态为 available 并清除 student_id
    $reset_booking_query = "UPDATE booking 
                           SET status = 'available', 
                               student_id = NULL 
                           WHERE booking_id = ?";
    $reset_booking_stmt = mysqli_prepare($connect, $reset_booking_query);
    mysqli_stmt_bind_param($reset_booking_stmt, "i", $booking_id);
    
    if (!mysqli_stmt_execute($reset_booking_stmt)) {
        throw new Exception("Failed to reset booking status: " . mysqli_error($connect));
    }
    
    error_log("Reset booking status for booking_id: $booking_id");
    
    // 檢查是否已經創建了新的可用時間段
    $check_time_slot_query = "SELECT COUNT(*) as slot_count 
                             FROM booking 
                             WHERE match_id = ? 
                             AND tutor_id = ? 
                             AND start_time = ? 
                             AND end_time = ? 
                             AND status = 'available' 
                             AND booking_id != ?";
    
    $check_slot_stmt = mysqli_prepare($connect, $check_time_slot_query);
    mysqli_stmt_bind_param($check_slot_stmt, "ssssi", $case_id, $tutor_id, $start_time, $end_time, $booking_id);
    mysqli_stmt_execute($check_slot_stmt);
    $slot_result = mysqli_stmt_get_result($check_slot_stmt);
    $slot_row = mysqli_fetch_assoc($slot_result);
    
    // 如果沒有找到重複的時間段，創建一個新的
    if ($slot_row['slot_count'] == 0) {
        $create_slot_query = "INSERT INTO booking (match_id, tutor_id, start_time, end_time, status, created_at, updated_at) 
                             VALUES (?, ?, ?, ?, 'available', NOW(), NOW())";
        $create_slot_stmt = mysqli_prepare($connect, $create_slot_query);
        mysqli_stmt_bind_param($create_slot_stmt, "ssss", $case_id, $tutor_id, $start_time, $end_time);
        
        if (!mysqli_stmt_execute($create_slot_stmt)) {
            error_log("Warning: Failed to create new time slot: " . mysqli_error($connect));
            // 不中斷流程，繼續執行
        } else {
            error_log("Created new available time slot for match_id: $case_id");
        }
    } else {
        error_log("Duplicate time slot found, skipped creation");
    }

    mysqli_commit($connect);
    error_log("Transaction committed successfully");
    
    echo json_encode([
        "success" => true,
        "message" => "Booking status reset successfully",
        "booking_id" => $booking_id
    ]);

} catch (Exception $e) {
    mysqli_rollback($connect);
    error_log("Error in reset_booking_status.php: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "message" => $e->getMessage()
    ]);
} finally {
    if (isset($booking_stmt)) mysqli_stmt_close($booking_stmt);
    if (isset($delete_lesson_stmt)) mysqli_stmt_close($delete_lesson_stmt);
    if (isset($reset_booking_stmt)) mysqli_stmt_close($reset_booking_stmt);
    if (isset($check_slot_stmt)) mysqli_stmt_close($check_slot_stmt);
    if (isset($create_slot_stmt)) mysqli_stmt_close($create_slot_stmt);
    mysqli_close($connect);
    error_log("reset_booking_status.php finished");
}
?>