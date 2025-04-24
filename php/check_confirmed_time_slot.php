<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// 設置錯誤處理
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

// 包含數據庫配置
require_once 'db_config.php';

try {
    // 獲取輸入數據
    $data = json_decode(file_get_contents("php://input"), true);
    if (!$data) {
        $data = $_POST;
    }
    
    // 記錄接收到的數據
    error_log("Received data in check_confirmed_time_slot: " . print_r($data, true));
    
    // 驗證必要字段
    if (!isset($data['match_id'])) {
        throw new Exception('Missing required field: match_id');
    }
    
    // 提取和清理數據
    $match_id = trim($data['match_id']);
    
    // 獲取數據庫連接
    $conn = getDbConnection();
    
    // 查詢是否存在已確認的預約
    $stmt = $conn->prepare("SELECT booking_id, date, start_time, end_time 
                         FROM booking 
                         WHERE match_id = ? 
                         AND status = 'confirmed'
                         LIMIT 1");
    
    $stmt->bind_param("s", $match_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        // 存在已確認的時間槽
        $booking = $result->fetch_assoc();
        echo json_encode([
            'success' => true,
            'has_confirmed_slot' => true,
            'booking' => $booking
        ]);
    } else {
        // 不存在已確認的時間槽
        echo json_encode([
            'success' => true,
            'has_confirmed_slot' => false
        ]);
    }
    
} catch (Exception $e) {
    // 記錄錯誤
    error_log("Error in check_confirmed_time_slot.php: " . $e->getMessage());
    
    // 發送錯誤響應
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
} finally {
    // 關閉連接
    if (isset($conn) && $conn instanceof mysqli) {
        $conn->close();
    }
} 