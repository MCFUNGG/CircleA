<?php
// 設置錯誤報告
error_reporting(0);
ini_set('display_errors', 0);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// 檢查 db_config.php 是否存在
if (!file_exists('db_config.php')) {
    echo json_encode([
        'success' => false,
        'message' => 'Database configuration file not found'
    ]);
    exit;
}

require_once 'db_config.php';

// 檢查是否收到 message_id
if (!isset($_POST['message_id'])) {
    echo json_encode(['success' => false, 'message' => 'Missing message_id']);
    exit;
}

$message_id = $_POST['message_id'];

try {
    // 創建數據庫連接
    $conn = getDbConnection();
    
    if (!$conn) {
        throw new Exception("Database connection failed");
    }
    
    // 準備 SQL 查詢
    $sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
    $stmt = $conn->prepare($sql);
    
    if (!$stmt) {
        throw new Exception("Prepare failed: " . $conn->error);
    }
    
    // 綁定參數
    $stmt->bind_param("s", $message_id);
    
    // 執行查詢
    if (!$stmt->execute()) {
        throw new Exception("Execute failed: " . $stmt->error);
    }
    
    // 檢查是否有更新任何行
    if ($stmt->affected_rows === 0) {
        echo json_encode([
            'success' => false,
            'message' => 'No message found with the given ID'
        ]);
    } else {
        // 返回成功響應
        echo json_encode([
            'success' => true,
            'message' => 'Message marked as read'
        ]);
    }
    
} catch (Exception $e) {
    // 返回錯誤響應
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
} finally {
    // 關閉數據庫連接
    if (isset($stmt)) {
        $stmt->close();
    }
    if (isset($conn)) {
        $conn->close();
    }
}
?> 