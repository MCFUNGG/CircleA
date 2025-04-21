<?php
// 添加會話檢查
require_once 'includes/session.php';

header('Content-Type: application/json');
error_reporting(E_ALL);
ini_set('display_errors', 0);

require_once 'includes/fcm_helper.php';
require_once 'includes/db_connect.php';

try {
    // 記錄請求信息
    error_log("Request Method: " . $_SERVER['REQUEST_METHOD']);
    error_log("Content Type: " . ($_SERVER["CONTENT_TYPE"] ?? 'not set'));
    
    // 檢查是否為 POST 請求
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        throw new Exception('Invalid request method: ' . $_SERVER['REQUEST_METHOD']);
    }

    // 獲取輸入數據（支持 JSON 和表單數據）
    $contentType = isset($_SERVER["CONTENT_TYPE"]) ? $_SERVER["CONTENT_TYPE"] : '';
    
    if (strpos($contentType, 'application/json') !== false) {
        $rawInput = file_get_contents("php://input");
        error_log("Raw JSON input: " . $rawInput);
        $input = json_decode($rawInput, true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new Exception('JSON parsing error: ' . json_last_error_msg());
        }
        $cv_id = $input['cv_id'] ?? null;
        $status = $input['status'] ?? null;
    } else {
        error_log("POST data: " . print_r($_POST, true));
        $cv_id = $_POST['cv_id'] ?? null;
        $status = $_POST['new_status'] ?? null;
    }

    if (!$cv_id) {
        throw new Exception('Missing CV ID');
    }
    if (!$status) {
        throw new Exception('Missing status parameter');
    }

    // 驗證狀態值
    if (!in_array($status, ['A', 'N', 'W', 'P'])) {
        throw new Exception('Invalid status value: ' . $status);
    }

    // 開始事務
    $conn->begin_transaction();

    try {
        // 檢查CV是否存在
        $check_stmt = $conn->prepare("SELECT cv.status, cv.member_id FROM cv_data cv WHERE cv.cv_id = ?");
        $check_stmt->bind_param("i", $cv_id);
        $check_stmt->execute();
        $check_result = $check_stmt->get_result();
        
        if ($check_result->num_rows === 0) {
            throw new Exception('CV not found: ' . $cv_id);
        }
        
        $cv_info = $check_result->fetch_assoc();
        $current_status = $cv_info['status'];
        $member_id = $cv_info['member_id'];

        if ($current_status === $status) {
            throw new Exception('CV is already in ' . $status . ' status');
        }

        // 更新CV狀態
        $stmt = $conn->prepare("UPDATE cv_data SET status = ? WHERE cv_id = ?");
        $stmt->bind_param("si", $status, $cv_id);
        
        if (!$stmt->execute()) {
            throw new Exception('Failed to update CV status: ' . $conn->error);
        }

        if ($stmt->affected_rows === 0) {
            throw new Exception('No changes made to CV status');
        }

        // 如果是批准或拒絕，發送通知
        if ($status === 'A' || $status === 'N') {
            // 準備通知內容
            $title = $status === 'A' ? 'CV已通過審核' : 'CV未通過審核';
            $message = $status === 'A' ? 
                '您的CV已通過管理員審核，現在可以開始使用配對功能。' : 
                '很抱歉，您的CV未通過管理員審核。請檢查您的CV是否完整，或聯繫客服了解詳情。';
            $type = $status === 'A' ? 'cv_approved' : 'cv_rejected';
            
            // 插入通知記錄
            $stmt = $conn->prepare("INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) VALUES (?, ?, ?, ?, ?, 0, NOW())");
            $stmt->bind_param("ssssi", $member_id, $title, $message, $type, $cv_id);
            
            if (!$stmt->execute()) {
                throw new Exception('Failed to insert notification: ' . $conn->error);
            }

            $fcm_debug = [];
            try {
                // 初始化 FCM 輔助類
                $fcm = new FCMHelper();
                $fcm_debug['initialization'] = 'FCM Helper initialized successfully';
                
                // 獲取用戶的 FCM token
                $token = $fcm->getFCMToken($member_id, $conn);
                $fcm_debug['token_lookup'] = $token ? 'Token found' : 'No token found';
                
                if ($token) {
                    // 準備額外數據
                    $additional_data = [
                        'type' => $type,
                        'cv_id' => (string)$cv_id,
                        'status' => $status,
                        'timestamp' => time()
                    ];
                    
                    // 發送 FCM 通知
                    $notification_sent = $fcm->sendNotification(
                        $token,
                        $title,
                        $message,
                        $additional_data
                    );
                    
                    $fcm_debug['notification_sent'] = $notification_sent;
                    $fcm_debug['notification_data'] = [
                        'title' => $title,
                        'message' => $message,
                        'additional_data' => $additional_data
                    ];
                    
                    error_log("FCM Debug Info: " . json_encode($fcm_debug));
                    
                    if (!$notification_sent) {
                        error_log("FCM notification failed for cv_id: " . $cv_id);
                        // 不拋出異常，因為通知失敗不應影響狀態更新
                        $fcm_debug['error'] = 'Failed to send FCM notification';
                    }
                } else {
                    error_log("No FCM token found for member_id: " . $member_id);
                    $fcm_debug['error'] = 'No FCM token found';
                }
            } catch (Exception $e) {
                error_log("FCM Error: " . $e->getMessage());
                $fcm_debug['error'] = $e->getMessage();
                // 不拋出異常，讓事務繼續
            }
        }

        // 提交事務
        $conn->commit();

        $success_message = 'CV status updated successfully to ' . $status;
        
        // 根據請求類型返回不同的響應
        if (strpos($contentType, 'application/json') !== false) {
            echo json_encode([
                'success' => true,
                'message' => $success_message
            ]);
        } else {
            // 對於表單提交，重定向回CV驗證頁面
            header('Location: verify_cv.php?success=' . urlencode($success_message));
            exit;
        }

    } catch (Exception $e) {
        // 回滾事務
        $conn->rollback();
        throw $e;
    }

} catch (Exception $e) {
    $error_message = $e->getMessage();
    error_log('Error in update_cv_status.php: ' . $error_message);
    error_log('Stack trace: ' . $e->getTraceAsString());
    
    if (strpos($contentType, 'application/json') !== false) {
        echo json_encode([
            'success' => false,
            'message' => $error_message,
            'trace' => $e->getTraceAsString()
        ]);
    } else {
        // 對於表單提交，重定向回CV驗證頁面並顯示錯誤
        header('Location: verify_cv.php?error=' . urlencode($error_message));
        exit;
    }
}
?> 
