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

// Fetch the record with the maximum version for the specified member_id
$sql = "SELECT * FROM member_detail WHERE member_id = ? ORDER BY version DESC LIMIT 1";
$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $member_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    // Get the row with the maximum version
    $latest_record = $result->fetch_assoc();
} else {
    echo "No details found for member ID " . htmlspecialchars($member_id);
    exit();
}

// Process form submission for updating details
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // Get POST data and sanitize
    $gender = $_POST['Gender'];
    $address = filter_var(trim($_POST['Address']), FILTER_SANITIZE_STRING);
    $address_district_id = filter_var(trim($_POST['Address_District_id']), FILTER_SANITIZE_STRING);
    $dob = $_POST['DOB'];
    $profile = filter_var(trim($_POST['profile']), FILTER_SANITIZE_STRING);
    $description = filter_var(trim($_POST['description']), FILTER_SANITIZE_STRING);
    $status = $_POST['status'];

    // Prepare the insert query
    $insert_sql = "INSERT INTO member_detail (member_id, Gender, Address, Address_District_id, DOB, profile, description, status, version) 
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    $new_version = $latest_record['version'] + 1; // Increment version when inserting

    // Execute the insert
    $stmt_insert = $conn->prepare($insert_sql);
    $stmt_insert->bind_param("issssssis", $member_id, $gender, $address, $address_district_id, $dob, $profile, $description, $status, $new_version); // Assuming member_id is an integer

    if ($stmt_insert->execute()) {
        echo "<div class='alert alert-success'>Member details inserted successfully! You will be redirected shortly...</div>";
        header("refresh:2; url=member_main.php?id=" . urlencode($member_id) . "&page=memberDetail");
        exit();
    } else {
        echo "<div class='alert alert-danger'>Error inserting member details: " . $stmt_insert->error . "</div>";
    }
    

    $stmt_insert->close();

}
?>

<div class="tab-pane fade show active" id="memberDetail">
    <form method="post" action="">
        <input type="hidden" name="member_id" value="<?php echo htmlspecialchars($member_id); ?>">

        <div class="form-group">
            <label for="latest_version">Latest Version</label>
            <input type="text" id="latest_version" value="<?php echo htmlspecialchars($latest_record['version']); ?>" class="form-control" readonly>
        </div>
        
        <div class="form-group">
            <label for="gender">Gender</label>
            <select id="gender" name="Gender" class="form-control" required>
                <option value="M" <?php echo ($latest_record['Gender'] === 'M') ? 'selected' : ''; ?>>Male</option>
                <option value="F" <?php echo ($latest_record['Gender'] === 'F') ? 'selected' : ''; ?>>Female</option>
            </select>
        </div>
        
        <div class="form-group">
            <label for="address">Address</label>
            <input type="text" id="address" name="Address" value="<?php echo htmlspecialchars($latest_record['Address']); ?>" class="form-control" >
        </div>
        
        <div class="form-group">
            <label for="address_district">Address District ID</label>
            <input type="text" id="address_district" name="Address_District_id" value="<?php echo htmlspecialchars($latest_record['Address_District_id']); ?>" class="form-control" >
        </div>
        
        <div class="form-group">
            <label for="dob">Date of Birth</label>
            <input type="date" id="dob" name="DOB" value="<?php echo htmlspecialchars($latest_record['DOB']); ?>" class="form-control" >
        </div>
        
        <div class="form-group">
            <label for="profile">Profile</label>
            <img src="http://localhost/<?php echo htmlspecialchars($latest_record['profile']); ?>" alt="Profile Image" class="img-fluid">
        </div>
        
        <div class="form-group">
            <label for="description">Description</label>
            <textarea id="description" name="description" class="form-control" rows="3"><?php echo htmlspecialchars($latest_record['description']); ?></textarea>
        </div>
        
        <div class="form-group">
            <label for="status">Status</label>
            <select id="status" name="status" class="form-control" required>
                <option value="A" <?php echo ($latest_record['status'] === 'A') ? 'selected' : ''; ?>>Active</option>
                <option value="I" <?php echo ($latest_record['status'] === 'I') ? 'selected' : ''; ?>>Inactive</option>
            </select>
        </div>
        
        <div class="button-group">
            <button type="submit" class="btn btn-primary">Save Changes</button>
            <a href="member.php" class="btn btn-secondary">Cancel</a>
        </div>
    </form>
</div>
