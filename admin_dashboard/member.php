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

// Initialize search variables
$member_id = isset($_POST['member_id']) ? $_POST['member_id'] : '';
$email = isset($_POST['email']) ? $_POST['email'] : '';
$phone = isset($_POST['phone']) ? $_POST['phone'] : '';
$status = isset($_POST['status']) ? $_POST['status'] : '';
$isAdmin = isset($_POST['isAdmin']) ? $_POST['isAdmin'] : '';

// Build dynamic SQL query
$sql = "SELECT member_id, email, phone, status, isAdmin FROM member WHERE 1=1";
$params = [];
$types = "";

if (!empty($member_id)) {
    $sql .= " AND member_id LIKE ?";
    $params[] = "%" . $member_id . "%";
    $types .= "s";
}
if (!empty($email)) {
    $sql .= " AND email LIKE ?";
    $params[] = "%" . $email . "%";
    $types .= "s";
}
if (!empty($phone)) {
    $sql .= " AND phone LIKE ?";
    $params[] = "%" . $phone . "%";
    $types .= "s";
}
if (!empty($status)) {
    $sql .= " AND status = ?";
    $params[] = $status;
    $types .= "s";
}
if (!empty($isAdmin)) {
    $sql .= " AND isAdmin = ?";
    $params[] = $isAdmin;
    $types .= "s";
}

// Prepare and execute query
$stmt = $conn->prepare($sql);

if (!empty($params)) {
    $stmt->bind_param($types, ...$params);
}

$stmt->execute();
$result = $stmt->get_result();
$row_count = $result->num_rows;
?>

<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Members List</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons+Outlined" rel="stylesheet">
    <link rel="stylesheet" href="css/styles.css">
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/header.css">
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
            <div class="container">
                <form method="post" action="" class="search-form form-inline mb-3">
                    <input type="text" name="member_id" value="<?php echo htmlspecialchars($member_id); ?>" placeholder="Member ID" class="form-control form-control-sm mr-2 mb-2">
                    <input type="text" name="email" value="<?php echo htmlspecialchars($email); ?>" placeholder="Email" class="form-control form-control-sm mr-2 mb-2">
                    <input type="text" name="phone" value="<?php echo htmlspecialchars($phone); ?>" placeholder="Phone" class="form-control form-control-sm mr-2 mb-2">
                    <select name="status" class="form-control form-control-sm mr-2 mb-2">
                        <option value="">-- Status --</option>
                        <option value="1" <?php echo $status === '1' ? 'selected' : ''; ?>>Active</option>
                        <option value="0" <?php echo $status === '0' ? 'selected' : ''; ?>>Inactive</option>
                    </select>
                    <select name="isAdmin" class="form-control form-control-sm mr-2 mb-2">
                        <option value="">-- Admin? --</option>
                        <option value="Y" <?php echo $isAdmin === 'Y' ? 'selected' : ''; ?>>Yes</option>
                        <option value="N" <?php echo $isAdmin === 'N'  ? 'selected' : ''; ?>>No</option>
                    </select>
                    <button type="submit" class="btn btn-primary btn-sm mr-2 mb-2">Search</button>
                    <a href="add_member.php" class="btn btn-success btn-sm mr-2 mb-2">Add Member</a>
                    <button type="button" class="btn btn-info btn-sm mb-2" onclick="window.location.href='member.php'">Refresh</button>
                </form>

                <!-- 顯示搜索結果行數 -->
                <p>Total Results Found: <strong><?php echo $row_count; ?></strong></p>

                <div class="table-responsive">
                    <table class="table table-striped table-bordered table-hover">
                        <thead class="thead-dark">
                            <tr>
                                <th>Member ID</th>
                                <th>Email</th>
                                <th>Phone</th>
                                <th>Status</th>
                                <th>Is Admin</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php
                            if ($row_count > 0) {
                                while ($row = $result->fetch_assoc()) {
                                    // Use htmlspecialchars for security
                                    $memberId = htmlspecialchars($row['member_id']);
                                    $emailVal = htmlspecialchars($row['email']);
                                    $phoneVal = htmlspecialchars($row['phone']);
                                    $statusText = ($row['status'] == 1 ? '<span class="badge badge-success">Active</span>' : '<span class="badge badge-secondary">Inactive</span>');
                                    $isAdminText = ($row['isAdmin'] == 'Y' ? '<span class="badge badge-info">Yes</span>' : '<span class="badge badge-light text-dark">No</span>');

                                    echo "<tr>
                                        <td>{$memberId}</td>
                                        <td>{$emailVal}</td>
                                        <td>{$phoneVal}</td>
                                        <td>{$statusText}</td>
                                        <td>{$isAdminText}</td>
                                        <td><a href='member_main.php?id={$memberId}&page=memberInfo' class='btn btn-primary btn-sm'>Edit</a></td>";
                                }
                            } else {
                                echo "<tr><td colspan='6' class='text-center'>No members found</td></tr>";
                            }
                            ?>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>

        <!-- End Main -->

    </div>
</body>
<script src="js/scripts.js"></script>
</html>
