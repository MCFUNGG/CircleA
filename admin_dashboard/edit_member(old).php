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

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Get member_id from URL
if (!isset($_GET['id']) || empty($_GET['id'])) {
    header("Location: member.php"); // Redirect to member list if no ID is provided
    exit();
}

$member_id = $_GET['id'];

// Fetch member details
$sql = "SELECT member_id, email, phone, status, isAdmin, password FROM member WHERE member_id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $member_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    header("Location: member.php"); // Redirect to member list if member not found
    exit();
}

$member = $result->fetch_assoc();

// Update member details
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = $_POST['email'];
    $phone = $_POST['phone'];
    $status = $_POST['status'];
    $isAdmin = $_POST['isAdmin'];
    
    // Check if a new password is provided
    if (!empty($_POST['password'])) {
        // Hash the new password
        $password = password_hash($_POST['password'], PASSWORD_DEFAULT);
        // Update query including password
        $update_sql = "UPDATE member SET email = ?, phone = ?, status = ?, isAdmin = ?, password = ? WHERE member_id = ?";
        $update_stmt = $conn->prepare($update_sql);
        $update_stmt->bind_param("ssssss", $email, $phone, $status, $isAdmin, $password, $member_id);
    } else {
        // If no new password, update without password
        $update_sql = "UPDATE member SET email = ?, phone = ?, status = ?, isAdmin = ? WHERE member_id = ?";
        $update_stmt = $conn->prepare($update_sql);
        $update_stmt->bind_param("sssss", $email, $phone, $status, $isAdmin, $member_id);
    }

    if ($update_stmt->execute()) {
        header("Location: member.php?message=updated"); // Redirect to member list after success
        exit();
    } else {
        $error_message = "Error updating record: " . $conn->error;
    }
}
?>

<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Member</title>
    <link rel="stylesheet" href="css/styles.css">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons+Outlined" rel="stylesheet">
</head>

<body>
    <div class="grid-container">

        <!-- Header -->
        <header class="header">
            <div class="menu-icon" onclick="openSidebar()">
                <span class="material-icons-outlined">menu</span>
            </div>
            <h1>Edit Member</h1>
            <div class="header-right">
                <span class="material-icons-outlined">notifications</span>
                <span class="material-icons-outlined">email</span>
                <span class="material-icons-outlined">account_circle</span>
            </div>
        </header>
        <!-- End Header -->

        <!-- Sidebar -->
        <aside id="sidebar">
            <div class="sidebar-title">
                <div class="sidebar-brand">
                    <span class="material-icons-outlined">inventory</span> CircleA
                </div>
                <span class="material-icons-outlined" onclick="closeSidebar()">close</span>
            </div>

            <ul class="sidebar-list">
                <li class="sidebar-list-item">
                    <a href="index.php">
                        <span class="material-icons-outlined">dashboard</span> Dashboard
                    </a>
                </li>
                <li class="sidebar-list-item">
                    <a href="applications.php">
                        <span class="material-symbols-outlined">checklist_rtl</span> Applications
                    </a>
                </li>
                <li class="sidebar-list-item">
                    <a href="member.php">
                        <span class="material-icons-outlined">group</span> Members
                    </a>
                </li>
                <li class="sidebar-list-item">
                    <a href="reports.php">
                        <span class="material-icons-outlined">poll</span> Reports
                    </a>
                </li>
                <li class="sidebar-list-item">
                    <a href="settings.php">
                        <span class="material-icons-outlined">settings</span> Settings
                    </a>
                </li>
                <li class="sidebar-list-item">
                    <a href="logout.php">
                        <span class="material-icons-outlined">logout</span> Logout
                    </a>
                </li>
            </ul>
        </aside>
        <!-- End Sidebar -->

        <!-- Main -->
        <main class="main-container">
            <div class="form-container">
                <h2>Edit Member</h2>
                <?php if (isset($error_message)): ?>
                    <p class="error-message"><?php echo $error_message; ?></p>
                <?php endif; ?>
                <form method="post" action="">
                    <div class="form-group">
                        <label for="member_id">Member ID</label>
                        <input type="text" id="member_id" name="member_id" value="<?php echo htmlspecialchars($member['member_id']); ?>" disabled class="search-input">
                    </div>
                    <div class="form-group">
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email" value="<?php echo htmlspecialchars($member['email']); ?>" class="search-input" required>
                    </div>
                    <div class="form-group">
                        <label for="phone">Phone</label>
                        <input type="text" id="phone" name="phone" value="<?php echo htmlspecialchars($member['phone']); ?>" class="search-input">
                    </div>
                    <div class="form-group">
                        <label for="status">Status</label>
                        <select id="status" name="status" required class="search-input">
                            <option value="1" <?php echo $member['status'] == '1' ? 'selected' : ''; ?>>Active</option>
                            <option value="0" <?php echo $member['status'] == '0' ? 'selected' : ''; ?>>Inactive</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="isAdmin">Is Admin</label>
                        <select id="isAdmin" name="isAdmin" required class="search-input">
                            <option value="y" <?php echo $member['isAdmin'] == 'Y' ? 'selected' : ''; ?>>Yes</option>
                            <option value="n" <?php echo $member['isAdmin'] == 'N' ? 'selected' : ''; ?>>No</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="password">Password (leave blank to keep current)</label>
                        <input type="password" id="password" name="password" class="search-input" placeholder="Enter new password">
                    </div>
                    <div class="button-group">
                        <button type="submit" class="btn-save">Save</button>
                        <a href="member.php" class="btn-cancel">Cancel</a>
                    </div>
                </form>
            </div>
        </main>
        <!-- End Main -->

    </div>
</body>

</html>
