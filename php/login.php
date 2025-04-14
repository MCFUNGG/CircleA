<?php
header('Content-Type: application/json');

// 引入数据库配置文件
require_once 'db_config.php';
$conn = getDbConnection();
    
// 检查连接
if ($conn->connect_error) {
    error_log("数据库连接失败: " . $conn->connect_error);
    return false;
}

// Get data from POST request
$email = $_POST["email"] ?? '';
$password = $_POST["password"] ?? '';

// Run the query to get the password hash based on the email
$query = "SELECT member_id, password FROM member WHERE email = '$email'";
$result = mysqli_query($conn, $query);

// Check if the query was successful
if (!$result) {
    echo json_encode(["success" => false, "message" => "Query failed"]);
    exit;
}

// Check if a result was returned
if (mysqli_num_rows($result) === 0) {
    echo json_encode(["success" => false, "message" => "No user found with that email"]);
    exit;
}

// Fetch the stored password hash
$row = mysqli_fetch_assoc($result);
$storedPassword = $row['password'];

// Verify the provided password against the stored hash
if (password_verify($password, $storedPassword)) {
    $member_id = $row['member_id'];
    echo json_encode(["success" => true, "message" => "Login successful", "member_id" => $member_id]);
} else {
    echo json_encode(["success" => false,   "message" => "Invalid email or password"]);
}

// Close the connection
mysqli_close($conn);
?>