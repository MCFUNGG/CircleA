<?php
session_start(); // Start session

// Check if user is logged in
if (!isset($_SESSION['loggedin']) || $_SESSION['loggedin'] !== true) {
    header("Location: login.html");
    exit();
}

// Database connection
$servername = "localhost";
$username = "root"; // Database username
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

// Validate member_id
$member_id = filter_var($_GET['id'], FILTER_VALIDATE_INT);
if ($member_id === false) {
    header("Location: member.php"); // Redirect to member list if ID is invalid
    exit();
}

// Set current page
$current_page = isset($_GET['page']) ? $_GET['page'] : 'editMember'; // Initialize current_page

?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit Member</title>
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
            <div class="container mt-4">
                <div class="d-flex align-items-center mt-4">
                    <!-- Back button -->
                    <a href="member.php" class="btn btn-secondary me-3">
                        <span class="material-icons-outlined" style="vertical-align: middle;">arrow_back</span>
                        Back to Members
                    </a>
                    
                    <!-- Function buttons group -->
                    <div class="btn-group" role="group">
                        <a class="btn btn-info <?php echo ($current_page == 'memberInfo') ? 'active' : ''; ?>" 
                           href="?id=<?php echo $member_id; ?>&page=memberInfo">
                           Member Info
                        </a>
                        <a class="btn btn-info <?php echo ($current_page == 'memberDetail') ? 'active' : ''; ?>" 
                           href="?id=<?php echo $member_id; ?>&page=memberDetail">
                           Member Details
                        </a>
                        <a class="btn btn-info <?php echo ($current_page == 'memberCert') ? 'active' : ''; ?>" 
                           href="?id=<?php echo $member_id; ?>&page=memberCert">
                           Member Certifications
                        </a>
                        <a class="btn btn-info <?php echo ($current_page == 'memberApplications') ? 'active' : ''; ?>" 
                           href="?id=<?php echo $member_id; ?>&page=memberApplications">
                           Member Applications
                        </a>
                    </div>
                </div>

                <div class="tab-content mt-4">
                    <?php
                    if ($current_page === 'memberInfo' && file_exists('member_info.php')) {
                        include 'member_info.php';
                    } elseif ($current_page === 'memberDetail' && file_exists('member_detail.php')) {
                        include 'member_detail.php';
                    } elseif ($current_page === 'memberCert' && file_exists('member_cert.php')) {
                        include 'member_cert.php';
                    } elseif ($current_page === 'memberApplications' && file_exists('member_applications.php')) {
                        include 'member_applications.php';
                    } else {
                        echo "<div class='alert alert-danger'>Requested page not found.</div>";
                    }
                    ?>
                </div>
            </div>
        </main>

        <!-- End Main -->

    </div>
</body>
<script src="https://code.jquery.com/jquery-3.5.1.slim.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.9.2/dist/umd/popper.min.js"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
<script src="js/scripts.js"></script>
</html>

<?php
$conn->close(); // Close the database connection
?>
