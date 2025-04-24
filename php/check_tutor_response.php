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

// 記錄執行開始
error_log("check_tutor_response.php started");

// 包含數據庫配置
require_once 'db_config.php';

try {
    // 獲取輸入數據
    $data = json_decode(file_get_contents("php://input"), true);
    if (!$data) {
        $data = $_POST;
    }
    
    // 記錄接收到的數據
    error_log("Received data: " . print_r($data, true));
    
    // 驗證必要字段
    if (!isset($data['case_id']) || !isset($data['student_id'])) {
        throw new Exception('Missing required fields (case_id, student_id)');
    }
    
    // 提取和清理數據
    $case_id = trim($data['case_id']);
    $student_id = trim($data['student_id']);
    $check_both_incomplete = isset($data['check_both_incomplete']) ? filter_var($data['check_both_incomplete'], FILTER_VALIDATE_BOOLEAN) : false;
    $booking_id = isset($data['booking_id']) ? trim($data['booking_id']) : null;
    
    // 獲取數據庫連接
    $conn = getDbConnection();
    
    // 查找預約 ID (如果沒有直接提供)
    if (empty($booking_id)) {
        $booking_stmt = $conn->prepare("SELECT booking_id FROM booking 
                                        WHERE match_id = ? AND student_id = ? 
                                        AND status IN ('confirmed', 'conflict', 'incomplete')
                                        ORDER BY created_at DESC LIMIT 1");
        $booking_stmt->bind_param("ss", $case_id, $student_id);
        $booking_stmt->execute();
        $booking_result = $booking_stmt->get_result();
        
        if ($booking_result->num_rows > 0) {
            $booking_row = $booking_result->fetch_assoc();
            $booking_id = $booking_row['booking_id'];
        } else {
            // 沒有找到相關預約
            echo json_encode([
                'success' => true,
                'both_incomplete' => false,
                'tutor_responded_second' => false,
                'message' => 'No matching booking found'
            ]);
            exit;
        }
    }
    
    // 查詢課程狀態記錄
    $status_stmt = $conn->prepare("SELECT * FROM first_lesson WHERE booking_id = ?");
    $status_stmt->bind_param("i", $booking_id);
    $status_stmt->execute();
    $status_result = $status_stmt->get_result();
    
    if ($status_result->num_rows == 0) {
        // 沒有找到課程狀態記錄
        echo json_encode([
            'success' => true,
            'both_incomplete' => false,
            'tutor_responded_second' => false,
            'message' => 'No lesson status record found'
        ]);
        exit;
    }
    
    $status_record = $status_result->fetch_assoc();
    
    // 檢查是否符合條件：
    // 1. 學生和導師都標記為不完整
    // 2. 導師在學生之後回應 (导师是第二个响应的人)
    $both_incomplete = false;
    $tutor_responded_second = false;
    $reason = '';
    
    if ($status_record['ps_response'] === 'incomplete' && 
        $status_record['t_response'] === 'incomplete') {
        
        $both_incomplete = true;
        $reason = $status_record['t_reason'] ?: $status_record['ps_reason'];
        
        // 檢查時間順序, 確保導師在學生之後回應
        if (!empty($status_record['ps_response_time']) && 
            !empty($status_record['t_response_time'])) {
            
            $ps_time = strtotime($status_record['ps_response_time']);
            $t_time = strtotime($status_record['t_response_time']);
            
            if ($t_time > $ps_time) {
                $tutor_responded_second = true;
            }
        }
    }
    
    // 返回結果
    echo json_encode([
        'success' => true,
        'both_incomplete' => $both_incomplete,
        'tutor_responded_second' => $tutor_responded_second,
        'reason' => $reason,
        'booking_id' => $booking_id
    ]);
    
} catch (Exception $e) {
    // 記錄錯誤
    error_log("Error in check_tutor_response.php: " . $e->getMessage());
    
    // 發送錯誤響應
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
}
?> 