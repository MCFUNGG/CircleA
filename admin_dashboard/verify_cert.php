<?php
session_start(); // Start session

// Check if user is logged in
if (!isset($_SESSION['loggedin']) || $_SESSION['loggedin'] !== true) {
    header("Location: login.html"); // Redirect to login page if not logged in
    exit();
}

// Database connection
$servername = "localhost";
$username = "root";
$password = ""; // Database password
$dbname = "system001"; // Database name

$conn = new mysqli($servername, $username, $password, $dbname);

// Handle status update
$message = '';
if ($_SERVER["REQUEST_METHOD"] == "POST" && isset($_POST['cert_id']) && isset($_POST['new_status'])) {
    $cert_id = $_POST['cert_id'];
    $new_status = $_POST['new_status'];

    $update_sql = "UPDATE member_cert SET status = ? WHERE member_cert_id = ?";
    if ($stmt = $conn->prepare($update_sql)) {
        $stmt->bind_param("si", $new_status, $cert_id);
        if ($stmt->execute()) {
            $message = '<div class="alert alert-success">Status updated successfully!</div>';
        } else {
            $message = '<div class="alert alert-danger">Update failed: ' . $stmt->error . '</div>';
        }
        $stmt->close();
    }
}

// 處理搜尋條件
$whereClauses = [];
if (!empty($_GET['member_id'])) {
    $member_id = $conn->real_escape_string($_GET['member_id']);
    $whereClauses[] = "m.member_id = '$member_id'";
}
if (!empty($_GET['email'])) {
    $email = $conn->real_escape_string($_GET['email']);
    $whereClauses[] = "m.email LIKE '%$email%'";
}
if (!empty($_GET['phone'])) {
    $phone = $conn->real_escape_string($_GET['phone']);
    $whereClauses[] = "m.phone LIKE '%$phone%'";
}
if (!empty($_GET['status'])) {
    $status = $conn->real_escape_string($_GET['status']);
    $whereClauses[] = "mc.status = '$status'";
}

$whereSQL = '';
if (count($whereClauses) > 0) {
    $whereSQL = 'WHERE ' . implode(' AND ', $whereClauses);
}

// Retrieve all certificate records
$sql = "SELECT mc.*, m.username, m.email 
        FROM member_cert mc 
        JOIN member m ON mc.member_id = m.member_id 
        $whereSQL
        ORDER BY mc.created_time DESC";
$result = $conn->query($sql);

// Add error checking
if (!$result) {
    die("Query failed: " . $conn->error);
}

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

?>

<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Members List</title>
    <link rel="stylesheet" href="css/styles.css"> <!-- 引入统一样式 -->
    <link rel="stylesheet" href="css/sidebar.css">  <!-- 添加這行 -->
    <link rel="stylesheet" href="css/header.css">   <!-- 如果有用到 header -->
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons+Outlined" rel="stylesheet">
    <style>
        .filter-form input[type="text"],
        .filter-form select,
        .btn-filter,
        .btn-refresh {
            padding: 8px;
            margin-right: 5px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 14px;
        }

        .btn-filter,
        .btn-refresh {
            background-color: #007bff;
            color: white;
            border: none;
            cursor: pointer;
            transition: background-color 0.3s;
        }

        .btn-filter:hover,
        .btn-refresh:hover {
            background-color: #0056b3;
        }

        .btn-refresh {
            background-color: #4CAF50;
            /* Green */
            color: white;
            border: none;
            padding: 10px 20px;
            font-size: 16px;
            cursor: pointer;
            border-radius: 5px;
            transition: background-color 0.3s ease, transform 0.2s ease;
        }

        .btn-refresh:hover {
            background-color: #45a049;
            transform: scale(1.05);
        }

        .btn-refresh:active {
            background-color: #3e8e41;
            transform: scale(0.95);
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
            <div class="content-header" style="display: flex; align-items: center; justify-content: center; height: 50px; gap: 10px;">
                <!-- 搜尋篩選器 -->
                <form method="GET" class="filter-form" style="display: flex; align-items: center; gap: 5px;">
                    <input type="text" name="member_id" placeholder="Search by Member ID">
                    <input type="text" name="email" placeholder="Search by Email">
                    <input type="text" name="phone" placeholder="Search by Phone">
                    <select name="status">
                        <option value="">-- Select Status --</option>
                        <option value="W">Pending Review</option>
                        <option value="A">Approved</option>
                        <option value="N">Not Approved</option>
                    </select>
                    <button type="submit" class="btn-filter">Search</button>
                </form>
                <button onclick="refreshPage()" class="btn-refresh">Refresh</button>
            </div>

            <!-- 將訊息移到這裡 -->
            <?php if (!empty($message)): ?>
                <div class="alert-message" style="margin-top: 10px;">
                    <?php echo $message; ?>
                </div>
            <?php endif; ?>

            <div class="cert-table">
                <?php if ($result && $result->num_rows > 0): ?>
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Username</th>
                                <th>Email</th>
                                <th>Certificate File</th>
                                <th>Description</th>
                                <th>Upload Time</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php while ($row = $result->fetch_assoc()): ?>
                                <tr>
                                    <td><?php echo $row['member_cert_id']; ?></td>
                                    <td><?php echo htmlspecialchars($row['username']); ?></td>
                                    <td><?php echo htmlspecialchars($row['email']); ?></td>
                                    <td>
                                        <a href="http://localhost/FYP/upload_cert/<?php echo htmlspecialchars($row['cert_file']); ?>"
                                            target="_blank"
                                            class="cert-link">
                                            <span class="material-icons-outlined">description</span>
                                            View Certificate
                                        </a>
                                    </td>
                                    <td><?php echo htmlspecialchars($row['description']); ?></td>
                                    <td><?php echo date('Y-m-d H:i', strtotime($row['created_time'])); ?></td>
                                    <td>
                                        <span class="status-badge status-<?php echo strtolower($row['status']); ?>">
                                            <?php
                                            switch ($row['status']) {
                                                case 'W':
                                                    echo 'Pending Review';
                                                    break;
                                                case 'A':
                                                    echo 'Approved';
                                                    break;
                                                case 'N':
                                                    echo 'Not Approved';
                                                    break;
                                                default:
                                                    echo 'Unknown Status';
                                            }
                                            ?>
                                        </span>
                                    </td>
                                    <td>
                                        <form method="POST" class="btn-group">
                                            <input type="hidden" name="cert_id" value="<?php echo $row['member_cert_id']; ?>">
                                            <button type="submit" name="new_status" value="A" class="btn btn-approve">
                                                <span class="material-icons-outlined">check_circle</span>
                                            </button>
                                            <button type="submit" name="new_status" value="N" class="btn btn-reject">
                                                <span class="material-icons-outlined">cancel</span>
                                            </button>
                                        </form>
                                    </td>
                                </tr>
                            <?php endwhile; ?>
                        </tbody>
                    </table>
                <?php else: ?>
                    <div class="empty-message">
                        <span class="material-icons-outlined">info</span>
                        <p>There are currently no certificates pending verification</p>
                    </div>
                <?php endif; ?>
            </div>

        </main>

        <!-- End Main -->

    </div>
</body>
<script src="js/scripts.js"></script>
</html>