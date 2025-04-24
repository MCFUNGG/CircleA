<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

require_once 'db_config.php';

// 創建數據庫連接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode([
        "success" => false, 
        "message" => "Database connection failed: " . mysqli_connect_error()
    ]);
    exit;
}

// 獲取POST請求參數
$match_id = isset($_POST['match_id']) ? $_POST['match_id'] : null;
$tutor_id = isset($_POST['tutor_id']) ? $_POST['tutor_id'] : null;

if (!$match_id || !$tutor_id) {
    echo json_encode([
        "success" => false, 
        "message" => "Missing required parameters"
    ]);
    exit;
}

// 添加詳細日誌
error_log("get_confirmed_booking.php - match_id: " . $match_id . ", tutor_id: " . $tutor_id);

// 添加一個直接查詢，檢查數據庫中的實際數據
$debug_query = "SELECT booking_id, match_id, student_id, tutor_id, start_time, end_time, status 
               FROM booking 
               WHERE match_id = ? AND status = 'confirmed'";
$debug_stmt = $connect->prepare($debug_query);
$debug_stmt->bind_param("s", $match_id);
$debug_stmt->execute();
$debug_result = $debug_stmt->get_result();

error_log("=== 調試數據庫查詢 ===");
while ($debug_row = $debug_result->fetch_assoc()) {
    error_log("預約ID: " . $debug_row['booking_id'] . 
              ", 狀態: " . $debug_row['status'] . 
              ", 開始時間: " . $debug_row['start_time'] . 
              ", 結束時間: " . $debug_row['end_time']);
}
error_log("=== 調試結束 ===");
$debug_stmt->close();

// 查詢 - 專門獲取confirmed狀態的預約
$query = "SELECT b.*, m.username as student_name
          FROM booking b
          JOIN member m ON b.student_id = m.member_id
          WHERE b.match_id = ? 
          AND b.tutor_id = ?
          AND b.status = 'confirmed'
          ORDER BY b.created_at DESC
          LIMIT 1";

$stmt = $connect->prepare($query);
if (!$stmt) {
    echo json_encode([
        "success" => false, 
        "message" => "Query preparation failed: " . mysqli_error($connect)
    ]);
    exit;
}

$stmt->bind_param("ss", $match_id, $tutor_id);

if (!$stmt->execute()) {
    echo json_encode([
        "success" => false, 
        "message" => "Query execution failed: " . mysqli_error($connect)
    ]);
    exit;
}

$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $row = $result->fetch_assoc();
    
    // 格式化日期時間為Android期望的格式 (yyyy-MM-dd HH:mm)
    $start_time = date('Y-m-d H:i', strtotime($row['start_time']));
    $end_time = date('Y-m-d H:i', strtotime($row['end_time']));
    
    // 詳細日誌記錄
    error_log("確認預約: ID=" . $row['booking_id'] . 
              ", 狀態=" . $row['status'] . 
              ", 開始時間=" . $start_time . 
              ", 結束時間=" . $end_time);
    
    // 檢查原始數據
    error_log("原始數據: start_time=" . $row['start_time'] . ", end_time=" . $row['end_time']);
    
    $booking = [
        'booking_id' => $row['booking_id'],
        'student_id' => $row['student_id'],
        'student_name' => $row['student_name'],
        'start_time' => $start_time,
        'end_time' => $end_time,
        'status' => $row['status'],
        'created_at' => $row['created_at'],
        'updated_at' => $row['updated_at']
    ];
    
    // 將完整數據添加到響應中以便調試
    echo json_encode([
        "success" => true,
        "has_confirmed" => true,
        "booking" => $booking,
        "debug_info" => [
            "raw_start_time" => $row['start_time'],
            "raw_end_time" => $row['end_time'],
            "formatted_start_time" => $start_time,
            "formatted_end_time" => $end_time
        ]
    ]);
} else {
    echo json_encode([
        "success" => true,
        "has_confirmed" => false,
        "message" => "No confirmed booking found"
    ]);
}

// 關閉連接
$stmt->close();
$connect->close();
?> 