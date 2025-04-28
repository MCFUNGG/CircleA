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

// 获取导师ID
$tutor_id = isset($_POST['tutor_id']) ? $_POST['tutor_id'] : null;

if ($tutor_id === null) {
    echo json_encode([
        "success" => false, 
        "message" => "Missing tutor_id parameter"
    ]);
    exit;
}

try {
    // 查询导师评价
    // 1. 找出该导师(member_id)创建的所有申请(app_id)，确保app_creator='T'表示导师创建的申请
    // 2. 然后查找这些申请中角色为parent的评价(学生对导师的评价)
    $query = "SELECT tr.*, m.username 
              FROM tutor_rating tr
              LEFT JOIN member m ON tr.member_id = m.member_id
              WHERE 
                  tr.application_id IN (
                      SELECT app_id FROM application 
                      WHERE member_id = ? AND app_creator = 'T'
                  ) 
                  AND tr.role = 'parent'
              ORDER BY tr.rate_times DESC";
    
    $stmt = $connect->prepare($query);
    $stmt->bind_param("i", $tutor_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    // 用于调试
    $debug_info = [
        "query" => $query,
        "tutor_id" => $tutor_id,
        "result_count" => $result->num_rows
    ];
    
    $ratings = [];
    while ($row = $result->fetch_assoc()) {
        // 隐藏学生真实姓名，只显示部分
        if (isset($row['username']) && !empty($row['username'])) {
            $name = $row['username'];
            if (strlen($name) > 2) {
                $row['username'] = mb_substr($name, 0, 1) . "***" . mb_substr($name, -1);
            }
        }
        
        $ratings[] = $row;
    }
    
    echo json_encode([
        "success" => true,
        "ratings" => $ratings,
        "debug" => $debug_info
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "message" => "Error fetching ratings: " . $e->getMessage()
    ]);
}

// 关闭连接
mysqli_close($connect);
?> 