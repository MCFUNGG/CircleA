<?php
header('Content-Type: application/json');

// 資料庫設定
require_once 'db_config.php';

// 參數檢查
if (!isset($_POST['booking_id'])) {
    echo json_encode([
        'success' => false,
        'message' => '缺少必要參數'
    ]);
    exit;
}

$booking_id = $_POST['booking_id'];

try {
    // 建立資料庫連接
    $conn = getDbConnection();

    // 檢查連接
    if (!$conn) {
        throw new Exception("資料庫連接失敗");
    }

    // 記錄請求
    error_log("Tutor checking payment status for booking_id: " . $booking_id);

    // 檢查預約狀態並獲取match_id
    $booking_sql = "SELECT status, match_id FROM booking WHERE booking_id = ?";
    $booking_stmt = $conn->prepare($booking_sql);
    $booking_stmt->bind_param("s", $booking_id);
    $booking_stmt->execute();
    $booking_result = $booking_stmt->get_result();
    
    $booking_status = "";
    $match_id = "";
    if ($booking_result->num_rows > 0) {
        $booking_row = $booking_result->fetch_assoc();
        $booking_status = $booking_row['status'];
        $match_id = $booking_row['match_id'];
        error_log("Booking status for booking_id " . $booking_id . " is: " . $booking_status . ", match_id: " . $match_id);
        
        // 衝突或已完成狀態下，導師可以看到學生聯繫方式，付款狀態顯示為已驗證
        if ($booking_status == 'conflict' || $booking_status == 'completed') {
            error_log("Booking is in " . $booking_status . " state, reporting payment as verified to tutor");
            $booking_stmt->close();
            echo json_encode([
                'success' => true,
                'status' => 'verified',
                'message' => $booking_status . ' status detected, payment shown as verified'
            ]);
            exit;
        }
    } else {
        $booking_stmt->close();
        echo json_encode([
            'success' => false,
            'message' => 'Booking not found'
        ]);
        exit;
    }
    $booking_stmt->close();
    
    // 檢查實際付款狀態
    // 使用 match_id 從 payment 表獲取最新付款狀態
    $sql = "SELECT p.status, p.payment_id, p.submitted_at as created_at 
            FROM payment p 
            WHERE p.match_id = ? 
            ORDER BY p.submitted_at DESC 
            LIMIT 1";
            
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("s", $match_id);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        $payment_status = $row['status'];
        $payment_id = $row['payment_id'];
        $created_at = $row['created_at'];
        
        error_log("Found payment status: " . $payment_status . " for match_id: " . $match_id . 
                  ", payment_id: " . $payment_id . ", created_at: " . $created_at);

        // 返回狀態
        echo json_encode([
            'success' => true,
            'status' => $payment_status,
            'booking_status' => $booking_status,
            'payment_id' => $payment_id,
            'payment_date' => $created_at
        ]);
    } else {
        // 如果沒有付款記錄，顯示未提交
        error_log("No payment record found for match_id: " . $match_id);
        echo json_encode([
            'success' => true,
            'status' => 'not_submitted',
            'booking_status' => $booking_status,
            'message' => 'No payment record found'
        ]);
    }

    $stmt->close();
    $conn->close();

} catch (Exception $e) {
    error_log("Error checking payment status for tutor: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
}
?> 