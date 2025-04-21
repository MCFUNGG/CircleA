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

// 檢查是否收到 member_id
if (!isset($_POST['member_id'])) {
    echo json_encode(['success' => false, 'message' => 'Missing member_id']);
    exit;
}

$member_id = $_POST['member_id'];

try {
    // 創建數據庫連接
    $conn = getDbConnection();
    
    if (!$conn) {
        throw new Exception("Database connection failed");
    }
    
    // 準備 SQL 查詢
    $sql = "SELECT * FROM notifications WHERE member_id = ? ORDER BY created_at DESC";
    $stmt = $conn->prepare($sql);
    
    if (!$stmt) {
        throw new Exception("Prepare failed: " . $conn->error);
    }
    
    // 綁定參數
    $stmt->bind_param("s", $member_id);
    
    // 執行查詢
    if (!$stmt->execute()) {
        throw new Exception("Execute failed: " . $stmt->error);
    }
    
    // 獲取結果
    $result = $stmt->get_result();
    $messages = array();
    
    while ($row = $result->fetch_assoc()) {
        $messages[] = array(
            'id' => $row['id'],
            'sender_id' => 'system',
            'receiver_id' => $row['member_id'],
            'title' => $row['title'],
            'content' => $row['message'],
            'type' => $row['type'],
            'is_read' => $row['is_read'],
            'created_at' => $row['created_at']
        );
    }
    
    // 返回成功響應
    echo json_encode([
        'success' => true,
        'data' => $messages
    ]);
    
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