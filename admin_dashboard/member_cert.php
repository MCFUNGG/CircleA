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

// Update status if form is submitted
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['cert_id'], $_POST['new_status'])) {
    $cert_id = filter_var($_POST['cert_id'], FILTER_VALIDATE_INT);
    $new_status = $_POST['new_status'];

    $update_sql = "UPDATE member_cert SET status = ? WHERE member_cert_id = ?";
    $update_stmt = $conn->prepare($update_sql);
    $update_stmt->bind_param("si", $new_status, $cert_id);
    $update_stmt->execute();
    $update_stmt->close();
}

// Fetch all certifications for the specified member_id, ordered by created_time descending
$sql = "SELECT mc.*, m.username, m.email 
        FROM member_cert mc 
        JOIN member m ON mc.member_id = m.member_id 
        WHERE mc.member_id = ? 
        ORDER BY mc.created_time DESC";
$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $member_id);
$stmt->execute();
$result = $stmt->get_result(); // Store the result
?>

<div class="tab-pane fade <?php echo ($current_page === 'memberCert') ? 'show active' : ''; ?>" id="memberCert">
    
    <?php if ($result->num_rows > 0): ?>
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
        <p>No certifications found for member ID <?php echo htmlspecialchars($member_id); ?>.</p>
    <?php endif; ?>
</div>