<?php
// 資料庫連接參數
$servername = "127.0.0.1";
$username = "root";
$password = "";
$dbname = "system001";

// 創建資料庫連接
$conn = new mysqli($servername, $username, $password, $dbname);

// 檢查連接
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// 設定要插入的用戶資料
$email = "admin";
$plain_password = "admin"; // 明文密碼
$member_id = 2;

// 使用 PHP 的 password_hash() 進行加密
$hashed_password = password_hash($plain_password, PASSWORD_DEFAULT); // 加密密碼

// 插入資料到資料庫
$stmt = $conn->prepare("INSERT INTO member (member_id,email, password, isAdmin) VALUES (?, ?, ?, 'Y')");
$stmt->bind_param("iss",$member_id, $email, $hashed_password);

// 執行插入操作
if ($stmt->execute()) {
    echo "New account created successfully!";
} else {
    echo "Error: " . $stmt->error;
}

// 關閉語句和資料庫連接
$stmt->close();
$conn->close();
?>
