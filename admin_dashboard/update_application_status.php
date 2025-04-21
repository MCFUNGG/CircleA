<?php
// 添加會話檢查
require_once 'includes/session.php';

header('Content-Type: application/json');
error_reporting(E_ALL);
ini_set('display_errors', 0);

require_once 'includes/fcm_helper.php';
require_once 'includes/db_connect.php';  // 使用統一的數據庫連接

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
        $app_id = $input['app_id'] ?? null;
        $status = $input['status'] ?? null;
    } else {
        error_log("POST data: " . print_r($_POST, true));
        $app_id = $_POST['app_id'] ?? null;
        $status = $_POST['new_status'] ?? null;  // 注意這裡使用 new_status
    }

    if (!$app_id) {
        throw new Exception('Missing application ID');
    }
    if (!$status) {
        throw new Exception('Missing status parameter');
    }

    // 驗證狀態值
    if (!in_array($status, ['A', 'R', 'P'])) {
        throw new Exception('Invalid status value: ' . $status);
    }

    // 開始事務
    $conn->begin_transaction();

    try {
        // 檢查應用是否存在
        $check_stmt = $conn->prepare("SELECT status FROM application WHERE app_id = ?");
        $check_stmt->bind_param("i", $app_id);
        $check_stmt->execute();
        $check_result = $check_stmt->get_result();
        
        if ($check_result->num_rows === 0) {
            throw new Exception('Application not found: ' . $app_id);
        }
        
        $current_status = $check_result->fetch_assoc()['status'];
        if ($current_status === $status) {
            throw new Exception('Application is already in ' . $status . ' status');
        }

        // 更新申請狀態
        $stmt = $conn->prepare("UPDATE application SET status = ? WHERE app_id = ?");
        $stmt->bind_param("si", $status, $app_id);
        
        if (!$stmt->execute()) {
            throw new Exception('Failed to update application status: ' . $conn->error);
        }

        if ($stmt->affected_rows === 0) {
            throw new Exception('No changes made to application status');
        }

        // 如果是批准或拒絕，發送通知
        if ($status === 'A' || $status === 'R') {
            // 獲取申請的用戶 ID
            $stmt = $conn->prepare("SELECT member_id FROM application WHERE app_id = ?");
            $stmt->bind_param("i", $app_id);
            $stmt->execute();
            $result = $stmt->get_result();
            
            if ($row = $result->fetch_assoc()) {
                $member_id = $row['member_id'];
                
                // 準備通知內容
                $title = $status === 'A' ? '申請已通過審核' : '申請未通過審核';
                $message = $status === 'A' ? 
                    '您的申請已通過管理員審核，現在可以開始使用配對功能。' : 
                    '很抱歉，您的申請未通過管理員審核。請檢查您的資料是否完整，或聯繫客服了解詳情。';
                $type = $status === 'A' ? 'application_approved' : 'application_rejected';
                
                // 插入通知記錄
                $stmt = $conn->prepare("INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) VALUES (?, ?, ?, ?, ?, 0, NOW())");
                $stmt->bind_param("ssssi", $member_id, $title, $message, $type, $app_id);
                
                if (!$stmt->execute()) {
                    throw new Exception('Failed to insert notification: ' . $conn->error);
                }

                try {
                    // 初始化 FCM 輔助類
                    $fcm = new FCMHelper();
                    
                    // 獲取用戶的 FCM token
                    $token = $fcm->getFCMToken($member_id, $conn);
                    
                    if ($token) {
                        // 發送 FCM 通知
                        $notification_sent = $fcm->sendNotification(
                            $token,
                            $title,
                            $message,
                            [
                                'type' => $type,
                                'app_id' => (string)$app_id
                            ]
                        );
                        
                        error_log("FCM notification " . ($notification_sent ? "sent" : "failed") . " for app_id: " . $app_id);
                    } else {
                        error_log("No FCM token found for member_id: " . $member_id);
                    }
                } catch (Exception $e) {
                    // FCM 錯誤不應該影響整個事務
                    error_log("FCM Error: " . $e->getMessage());
                }
            }
        }

        // 提交事務
        $conn->commit();

        $success_message = 'Application status updated successfully to ' . $status;
        
        // 根據請求類型返回不同的響應
        if (strpos($contentType, 'application/json') !== false) {
            echo json_encode([
                'success' => true,
                'message' => $success_message
            ]);
        } else {
            // 對於表單提交，重定向回應用列表頁面
            header('Location: applications.php?success=' . urlencode($success_message));
            exit;
        }

    } catch (Exception $e) {
        // 回滾事務
        $conn->rollback();
        throw $e;
    }

} catch (Exception $e) {
    $error_message = $e->getMessage();
    error_log('Error in update_application_status.php: ' . $error_message);
    error_log('Stack trace: ' . $e->getTraceAsString());
    
    if (strpos($contentType, 'application/json') !== false) {
        echo json_encode([
            'success' => false,
            'message' => $error_message,
            'trace' => $e->getTraceAsString()
        ]);
    } else {
        // 對於表單提交，重定向回應用列表頁面並顯示錯誤
        header('Location: applications.php?error=' . urlencode($error_message));
        exit;
    }
}
?> 