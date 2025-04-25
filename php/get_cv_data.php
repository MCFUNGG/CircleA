<?php
header("Content-Type: application/json");

require_once 'db_config.php';

// 创建数据库连接
$connect = getDbConnection();

if (!$connect) {
    echo json_encode(["success" => false, "message" => "Database connection failed: " . mysqli_connect_error()]);
    exit;
}

// Get member_id from POST
$memberId = isset($_POST['member_id']) ? $_POST['member_id'] : null;

if ($memberId === null) {
    echo json_encode(["success" => false, "message" => "Member ID not provided"]);
    exit;
}

// Query to get CV data from cv_data table including status field
$query = "SELECT cv_id, member_id, contact, skills, education, language, other, cv_path, created_at, status
          FROM cv_data
          WHERE member_id = ?
          ORDER BY created_at DESC";

$stmt = $connect->prepare($query);
$stmt->bind_param("i", $memberId);
$stmt->execute();
$result = $stmt->get_result();

if (!$result) {
    echo json_encode(["success" => false, "message" => "Query failed: " . mysqli_error($connect)]);
    exit;
}

if ($result->num_rows > 0) {
    $cvData = [];
    while ($row = $result->fetch_assoc()) {
        $cvData[] = $row;
    }
    
    echo json_encode([
        "success" => true,
        "cv_data" => $cvData
    ]);
} else {
    echo json_encode(["success" => false, "message" => "No CV found for this member"]);
}

// Close connections
$stmt->close();
mysqli_close($connect);
?>
