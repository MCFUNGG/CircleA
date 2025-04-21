<?php
session_start(); // 開始會話

// 資料庫連接參數
$servername = "127.0.0.1";
$username = "root"; // 使用者名稱
$password = ""; // 密碼
$dbname = "system001"; // 資料庫名稱

// 創建連接
$conn = new mysqli($servername, $username, $password, $dbname);

// 檢查連接
if ($conn->connect_error) {
    // 在生產環境中，不應顯示詳細錯誤信息
    // 可以記錄錯誤或顯示通用錯誤消息
    // die("Database connection error."); 
    die("Connection failed: " . $conn->connect_error); // 保持開發時的詳細錯誤
}

$error_message = ''; // 初始化錯誤消息

// 檢查表單提交
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // 簡單驗證輸入
    if (empty($_POST['email']) || empty($_POST['password'])) {
        $error_message = "Please enter both email and password.";
    } else {
        $email = $_POST['email'];
        $password_attempt = $_POST['password'];

        // 預備 SQL 語句，明確選取所需欄位，並檢查 isAdmin = 'Y'
        // 使用大寫 'Y' 與 session.php 中的檢查保持一致
        $stmt = $conn->prepare("SELECT member_id, password, isAdmin FROM member WHERE email = ? AND isAdmin = 'Y'");
        
        if ($stmt) { // 檢查 prepare 是否成功
            $stmt->bind_param("s", $email);
            $stmt->execute();
            $result = $stmt->get_result();

            // 檢查結果
            if ($result->num_rows === 1) { // 應該只有一個管理員帳號匹配
                $user = $result->fetch_assoc();
                
                // 驗證密碼
                if (password_verify($password_attempt, $user['password'])) {
                    // 驗證成功
                    session_regenerate_id(true); // 重新生成 Session ID，防止 Session 固定攻擊

                    // *** 設定所有必要的 Session 變數 ***
                    $_SESSION['loggedin'] = true; 
                    $_SESSION['member_id'] = $user['member_id']; // 設定 member_id
                    $_SESSION['isAdmin'] = $user['isAdmin'];   // 設定 isAdmin ('Y')
                    $_SESSION['email'] = $email;             // 可選，如果其他地方需要

                    // 清除可能存在的舊錯誤消息
                    unset($error_message); 

                    // 重定向到儀表板首頁
                    header("Location: index.php");
                    exit(); // 確保腳本在重定向後停止執行
                } else {
                    // 密碼錯誤
                    $error_message = "Invalid email or password, or you do not have admin privileges.";
                }
            } else {
                // 查無該管理員用戶或 Email 錯誤
                $error_message = "Invalid email or password, or you do not have admin privileges.";
            }
            // 關閉語句
            $stmt->close();
        } else {
             // Prepare 失敗
            error_log("Prepare failed: (" . $conn->errno . ") " . $conn->error); // 記錄錯誤
            $error_message = "An error occurred during login preparation. Please try again later.";
        }
    }
}

// 關閉資料庫連接
$conn->close();

// 如果有錯誤消息，顯示登入頁面並帶上錯誤
if (!empty($error_message)) {
    // 可以將錯誤消息存入 Session 傳遞給 login.html，或者直接顯示
    // 這裡選擇直接包含 HTML 並顯示錯誤
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Login</title>
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@100;200;300;400;500;600;700;800;900&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/login.css">
</head>
<body>
    <div class="login-container">
        <div class="login-title">Login</div>
        <?php if (!empty($error_message)): ?>
            <p class="error-message" style="color: red; text-align: center; margin-bottom: 15px;"><?php echo htmlspecialchars($error_message); ?></p>
        <?php endif; ?>
        <form method="post" action="login.php">
            <div class="input-group">
                <label for="email">Email</label>
                <input type="text" id="email" name="email" placeholder="Enter your Email" required value="<?= htmlspecialchars($_POST['email'] ?? '') ?>">
            </div>
            <div class="input-group">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" placeholder="Enter your password" required>
            </div>
            <button type="submit" class="login-button">Login</button>
        </form>
    </div>
</body>
</html>
<?php
    exit(); // 顯示完錯誤後停止執行
}
// 如果沒有 POST 請求，或者意外執行到這裡，可以重定向回 login.html
// header("Location: login.html"); 
// exit();

?>
