<?php
// Database connection configuration
$servername = "localhost";  // 或使用 "127.0.0.1"
$username = "root";
$password = "";  // 空密码
$dbname = "system001";

// 创建数据库连接函数
function getDbConnection() {
    global $servername, $username, $password, $dbname;
    
    // 使用mysqli对象创建连接
    $conn = new mysqli($servername, $username, $password, $dbname);
    
    // 检查连接
    if ($conn->connect_error) {
        error_log("数据库连接失败: " . $conn->connect_error);
        return false;
    }
    
    // 设置字符集
    $conn->set_charset("utf8mb4");
    
    return $conn;
}
?>