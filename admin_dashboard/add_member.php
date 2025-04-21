<?php
session_start(); // 开始会话

// 检查用户是否已登录
if (!isset($_SESSION['loggedin']) || $_SESSION['loggedin'] !== true) {
    header("Location: login.html"); // 如果未登录，重定向到登录页面
    exit();
}

// 数据库连接
$servername = "localhost"; // 服务器地址
$username = "root"; // 数据库用户名
$password = ""; // 数据库密码
$dbname = "system001"; // 数据库名称

$conn = new mysqli($servername, $username, $password, $dbname);

// 检查连接
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// 处理表单提交
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $email = $_POST['email'];
    $phone = $_POST['phone'];
    $password = password_hash($_POST['password'], PASSWORD_DEFAULT); // 对密码进行加密
    $status = $_POST['status'];
    $isAdmin = $_POST['isAdmin'];

    // 插入新会员数据
    $sql = "INSERT INTO member (email, phone, password, status, isAdmin) VALUES (?, ?, ?, ?, ?)";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ssssi", $email, $phone, $password, $status, $isAdmin);

    if ($stmt->execute()) {
        echo "<script>alert('New member added successfully!');</script>";
    } else {
        echo "<script>alert('Error: " . $stmt->error . "');</script>";
    }

    $stmt->close();
}

$conn->close(); // 关闭连接
?>

<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Add New Member</title>

    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons+Outlined" rel="stylesheet">
    <link rel="stylesheet" href="css/styles.css">
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/header.css">

    <style>
        .form-container {
            max-width: 600px;
            margin: 2rem auto;
            padding: 2rem;
            background: #ffffff;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }

        .form-title {
            font-size: 1.8rem;
            color: #2d3748;
            margin-bottom: 2rem;
            font-weight: 600;
            border-bottom: 2px solid #e2e8f0;
            padding-bottom: 0.5rem;
        }

        .form-group {
            margin-bottom: 1.5rem;
        }

        label {
            display: block;
            margin-bottom: 0.5rem;
            color: #4a5568;
            font-weight: 500;
        }

        input, select {
            width: 100%;
            padding: 0.75rem;
            border: 1px solid #e2e8f0;
            border-radius: 6px;
            font-size: 1rem;
            transition: border-color 0.3s ease;
        }

        input:focus, select:focus {
            outline: none;
            border-color: #4299e1;
            box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.2);
        }

        button[type="submit"] {
            background: #4299e1;
            color: white;
            padding: 0.75rem 1.5rem;
            border: none;
            border-radius: 6px;
            font-size: 1rem;
            cursor: pointer;
            transition: background 0.3s ease;
            width: 100%;
            margin-top: 1rem;
        }

        button[type="submit"]:hover {
            background: #3182ce;
        }

        @media (min-width: 640px) {
            .form-container {
                padding: 3rem;
            }
            
            button[type="submit"] {
                width: auto;
                float: right;
            }
        }
    </style>
</head>

<body>
    <div class="grid-container">

        <!-- header -->
        <?php include 'includes/header.php'; ?>
        <!-- end header -->
        <!-- Sidebar -->
        <?php include 'includes/sidebar.php'; ?>
        <!-- End Sidebar -->

        <!-- Main -->
        <main class="main-container">
            <div class="form-container">
                <h2 class="form-title">Add New Member</h2>
                <form method="post" action="">
                    <div class="form-group">
                        <label for="email">Email Address</label>
                        <input type="email" name="email" required placeholder="example@domain.com">
                    </div>

                    <div class="form-group">
                        <label for="phone">Contact Number</label>
                        <input type="text" name="phone" placeholder="Enter 8 digits">
                    </div>

                    <div class="form-group">
                        <label for="password">Password</label>
                        <input type="password" name="password" required placeholder="Minimum 8 characters">
                    </div>

                    <div class="form-group">
                        <label for="status">Account Status</label>
                        <select name="status">
                            <option value="1">Active</option>
                            <option value="0">Inactive</option>
                        </select>
                    </div>


                    <div class="form-group">
                        <label for="isAdmin">Admin Permission</label>
                        <select name="isAdmin">
                            <option value="Y">Administrator</option>
                            <option value="N">Regular Member</option>
                        </select>
                    </div>

                    <button type="submit">Add Member →</button>
                </form>
            </div>
        </main>
        <!-- End Main -->

    </div>
</body>
  <!-- Custom JS -->
<script src="js/scripts.js"></script>
</html>