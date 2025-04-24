<?php
header("Content-Type: application/json");
error_reporting(E_ALL);
ini_set('display_errors', 1);
ini_set('log_errors', 1);
ini_set('error_log', 'php_error.log');

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

// 获取参数
$member_id = isset($_POST['member_id']) ? $_POST['member_id'] : null;
$application_id = isset($_POST['application_id']) ? $_POST['application_id'] : null;
$role = isset($_POST['role']) ? $_POST['role'] : 'parent'; // 默认为parent
$rate_score = isset($_POST['rate_score']) ? floatval($_POST['rate_score']) : 0;
$comment = isset($_POST['comment']) ? $_POST['comment'] : '';

// 验证必要参数
if ($member_id === null || $application_id === null || $rate_score <= 0) {
    echo json_encode([
        "success" => false, 
        "message" => "Missing required parameters or invalid rating"
    ]);
    exit;
}

// 开始事务
mysqli_begin_transaction($connect);

try {
    // 检查评分是否已存在
    $checkQuery = "SELECT rate_id FROM tutor_rating 
                  WHERE application_id = ? AND member_id = ? AND role = ?";
    
    $stmt = $connect->prepare($checkQuery);
    if (!$stmt) {
        throw new Exception("Query preparation failed: " . mysqli_error($connect));
    }
    
    $stmt->bind_param("iis", $application_id, $member_id, $role);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        // 评分已存在，更新它
        $updateQuery = "UPDATE tutor_rating 
                       SET rate_score = ?, comment = ?, rate_times = NOW() 
                       WHERE application_id = ? AND member_id = ? AND role = ?";
        
        $stmt = $connect->prepare($updateQuery);
        if (!$stmt) {
            throw new Exception("Update query preparation failed: " . mysqli_error($connect));
        }
        
        $stmt->bind_param("dsiis", $rate_score, $comment, $application_id, $member_id, $role);
        if (!$stmt->execute()) {
            throw new Exception("Update execution failed: " . $stmt->error);
        }
        
        $message = "Feedback updated successfully";
    } else {
        // 插入新评分
        $insertQuery = "INSERT INTO tutor_rating 
                       (member_id, application_id, role, rate_times, rate_score, comment) 
                       VALUES (?, ?, ?, NOW(), ?, ?)";
        
        $stmt = $connect->prepare($insertQuery);
        if (!$stmt) {
            throw new Exception("Insert query preparation failed: " . mysqli_error($connect));
        }
        
        $stmt->bind_param("iisds", $member_id, $application_id, $role, $rate_score, $comment);
        if (!$stmt->execute()) {
            throw new Exception("Insert execution failed: " . $stmt->error);
        }
        
        $message = "Feedback submitted successfully";
    }
    
    // 提交事务
    mysqli_commit($connect);
    
    echo json_encode([
        "success" => true,
        "message" => $message
    ]);
    
} catch (Exception $e) {
    // 发生错误时回滚事务
    mysqli_rollback($connect);
    
    echo json_encode([
        "success" => false,
        "message" => "Error submitting feedback: " . $e->getMessage()
    ]);
} finally {
    // 关闭语句和连接
    if (isset($stmt) && $stmt instanceof mysqli_stmt) {
        $stmt->close();
    }
    mysqli_close($connect);
}
?> 