<?php
header("Content-Type: application/json");
error_reporting(E_ALL);
ini_set('display_errors', 1);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

require_once 'db_config.php';
require_once 'fcm_helper.php';

// 创建数据库连接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode([
        "success" => false, 
        "message" => "Database connection failed: " . mysqli_connect_error()
    ]);
    exit;
}

// 获取POST参数
$match_id = isset($_POST['match_id']) ? $_POST['match_id'] : null;
$member_id = isset($_POST['member_id']) ? $_POST['member_id'] : null;

if (!$match_id || !$member_id) {
    echo json_encode([
        "success" => false, 
        "message" => "Missing required parameters"
    ]);
    exit;
}

// 开始事务
mysqli_begin_transaction($connect);

try {
    // 首先获取匹配信息，以便知道是谁取消了请求（导师或学生）
    $query = "SELECT * FROM `match` WHERE match_id = ?";
    $stmt = $connect->prepare($query);
    $stmt->bind_param("i", $match_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        throw new Exception("Match not found");
    }
    
    $match_data = $result->fetch_assoc();
    $tutor_id = $match_data['tutor_id'];
    $ps_id = $match_data['ps_id'];
    $match_creator = $match_data['match_creator'];
    $status = $match_data['status'];
    
    // 检查请求取消的用户是否有权限（必须是匹配中的一方）
    if ($member_id != $tutor_id && $member_id != $ps_id) {
        throw new Exception("You don't have permission to cancel this request");
    }
    
    // 检查匹配状态，只允许取消WT或WPS状态的请求
    if ($status !== 'WT' && $status !== 'WPS') {
        throw new Exception("Only pending requests can be cancelled");
    }
    
    // 获取取消者的用户名
    $name_query = "SELECT username FROM member WHERE member_id = ?";
    $stmt = $connect->prepare($name_query);
    $stmt->bind_param("s", $member_id);
    $stmt->execute();
    $name_result = $stmt->get_result();
    $canceller_name = ($name_result->num_rows > 0) ? $name_result->fetch_assoc()['username'] : "Unknown";
    
    // 删除匹配记录
    $delete_query = "DELETE FROM `match` WHERE match_id = ?";
    $stmt = $connect->prepare($delete_query);
    $stmt->bind_param("i", $match_id);
    $stmt->execute();
    
    if ($stmt->affected_rows === 0) {
        throw new Exception("Failed to delete match record");
    }
    
    // 确定接收通知的用户
    $notification_recipient_id = ($member_id == $tutor_id) ? $ps_id : $tutor_id;
    
    // 插入取消通知
    $notification_title = "匹配请求已取消";
    $notification_message = $canceller_name . " 已取消匹配请求";
    $notification_type = "request_cancelled";
    
    $insert_notification = "INSERT INTO notifications (member_id, title, message, type, related_id, is_read, created_at) 
                            VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
    
    $stmt = $connect->prepare($insert_notification);
    $stmt->bind_param("ssssi", 
        $notification_recipient_id,
        $notification_title,
        $notification_message,
        $notification_type,
        $match_id
    );
    $stmt->execute();
    
    // 获取接收者的FCM令牌
    $token_query = "SELECT token FROM fcm_tokens WHERE member_id = ? ORDER BY created_at DESC LIMIT 1";
    $stmt = $connect->prepare($token_query);
    $stmt->bind_param("s", $notification_recipient_id);
    $stmt->execute();
    $token_result = $stmt->get_result();
    
    // 如果有FCM令牌，发送通知
    if ($token_result && $token_row = $token_result->fetch_assoc()) {
        $fcm_token = $token_row['token'];
        
        // 准备FCM消息
        $fcm_message = [
            "message" => [
                "token" => $fcm_token,
                "notification" => [
                    "title" => $notification_title,
                    "body" => $notification_message
                ],
                "android" => [
                    "notification" => [
                        "channel_id" => "CircleA_Channel",
                        "click_action" => "FLUTTER_NOTIFICATION_CLICK"
                    ]
                ],
                "data" => [
                    "type" => $notification_type,
                    "match_id" => (string)$match_id,
                    "canceller_name" => $canceller_name,
                    "click_action" => "FLUTTER_NOTIFICATION_CLICK"
                ]
            ]
        ];
        
        // 发送FCM消息
        // 这部分代码可以集成到fcm_helper.php中
        // 为简化示例，这里省略了FCM发送代码
    }
    
    // 提交事务
    mysqli_commit($connect);
    
    echo json_encode([
        "success" => true,
        "message" => "Request cancelled successfully"
    ]);
    
} catch (Exception $e) {
    // 回滚事务
    mysqli_rollback($connect);
    
    echo json_encode([
        "success" => false,
        "message" => $e->getMessage()
    ]);
} finally {
    // 关闭连接
    mysqli_close($connect);
}
?> 