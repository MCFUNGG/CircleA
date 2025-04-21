<?php
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

// Fetch member details
$sql = "SELECT member_id, username, email, phone, status, isAdmin FROM member WHERE member_id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $member_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    header("Location: member.php"); // Redirect if the member is not found
    exit();
}

$member = $result->fetch_assoc();

// Process form submission
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // Get POST data and sanitize
    $email = filter_var(trim($_POST['email']), FILTER_SANITIZE_EMAIL);
    $phone = filter_var(trim($_POST['phone']), FILTER_SANITIZE_STRING);
    $status = isset($_POST['status']) ? intval($_POST['status']) : 0;
    $isAdmin = isset($_POST['isAdmin']) ? $_POST['isAdmin'] : 'N';
    $password = trim($_POST['password']);

    // Prepare an update query
    if ($password !== "") {
        // If a new password is provided, hash it
        $hashed_password = password_hash($password, PASSWORD_DEFAULT);
        $update_sql = "UPDATE member SET email = ?, phone = ?, status = ?, isAdmin = ?, password = ? WHERE member_id = ?";
        $stmt_update = $conn->prepare($update_sql);
        $stmt_update->bind_param("ssissi", $email, $phone, $status, $isAdmin, $hashed_password, $member_id);
    } else {
        // If no new password is provided, ignore the password field in the update
        $update_sql = "UPDATE member SET email = ?, phone = ?, status = ?, isAdmin = ? WHERE member_id = ?";
        $stmt_update = $conn->prepare($update_sql);
        $stmt_update->bind_param("ssisi", $email, $phone, $status, $isAdmin, $member_id);
    }

    // Execute the update
    if ($stmt_update->execute()) {
        // Redirect or show success message
        echo "<div class='alert alert-success'>Member updated successfully!</div>";
        // Optionally, redirect to the member list after a few seconds
        header("refresh:2; url=member_main.php?id=" . urlencode($member_id) . "&page=memberInfo");
    } else {
        echo "<div class='alert alert-danger'>Error updating member: " . $stmt_update->error . "</div>";
    }

    $stmt_update->close();
}
?>

<div class="tab-pane fade show active" id="editMember">
    <form method="post" action="">
        <div class="form-group">
            <label for="member_id">Member ID</label>
            <input type="text" id="member_id" name="member_id" value="<?php echo htmlspecialchars($member['member_id']); ?>" disabled class="form-control">
        </div>
        <div class="form-group">
            <label for="username">User Name</label>
            <input type="text" id="username" name="username" value="<?php echo htmlspecialchars($member['username']); ?>" class="form-control" required>
        </div>
        <div class="form-group">
            <label for="email">Email</label>
            <input type="email" id="email" name="email" value="<?php echo htmlspecialchars($member['email']); ?>" class="form-control" required>
        </div>
        <div class="form-group">
            <label for="phone">Phone</label>
            <input type="text" id="phone" name="phone" value="<?php echo htmlspecialchars($member['phone']); ?>" class="form-control">
        </div>
        <div class="form-group">
            <label for="status">Status</label>
            <select id="status" name="status" required class="form-control">
                <option value="1" <?php echo $member['status'] == '1' ? 'selected' : ''; ?>>Active</option>
                <option value="0" <?php echo $member['status'] == '0' ? 'selected' : ''; ?>>Inactive</option>
            </select>
        </div>
        <div class="form-group">
            <label for="isAdmin">Is Admin</label>
            <select id="isAdmin" name="isAdmin" required class="form-control">
                <option value="Y" <?php echo $member['isAdmin'] == 'Y' ? 'selected' : ''; ?>>Yes</option>
                <option value="N" <?php echo $member['isAdmin'] == 'N' ? 'selected' : ''; ?>>No</option>
            </select>
        </div>
        <div class="form-group">
            <label for="password">Password (leave blank to keep current)</label>
            <input type="password" id="password" name="password" class="form-control" placeholder="Enter new password">
        </div>
        <div class="button-group">
            <button type="submit" class="btn btn-primary">Save</button>
            <a href="member.php" class="btn btn-secondary">Cancel</a>
        </div>
    </form>
</div>
