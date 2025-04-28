<?php
header("Content-Type: application/json");
require_once 'db_config.php';

// 获取传入的tutor_id
$tutorId = isset($_POST['tutor_id']) ? trim($_POST['tutor_id']) : '';

// 验证参数
if (empty($tutorId)) {
    echo json_encode(["success" => false, "message" => "Tutor ID is required"]);
    exit;
}

// 创建数据库连接
$conn = getDbConnection();

// 查询导师基本信息
$query = "SELECT m.username as name, md.profile as profile_pic, md.description 
          FROM member m 
          LEFT JOIN member_detail md ON m.id = md.member_id 
          WHERE m.id = ? AND (md.version = (SELECT MAX(version) FROM member_detail WHERE member_id = m.id) OR md.version IS NULL)";

$stmt = $conn->prepare($query);
$stmt->bind_param("s", $tutorId);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $row = $result->fetch_assoc();
    
    // 处理头像路径
    $profilePic = $row['profile_pic'];
    
    // 构建响应数据
    $data = [
        "name" => $row['name'], 
        "profile_pic" => $profilePic, 
        "description" => $row['description']
    ];
    
    echo json_encode(["success" => true, "data" => $data]);
} else {
    echo json_encode(["success" => false, "message" => "Tutor not found"]);
}

// 关闭连接
$stmt->close();
$conn->close();
?> 