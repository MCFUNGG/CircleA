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

// Prepare the SQL statement
$stmt = $conn->prepare("
    SELECT 
        a.app_id AS app_id,
        a.member_id AS member_id,
        a.app_creator AS app_creator,
        GROUP_CONCAT(DISTINCT s.subject_name ORDER BY s.subject_name ASC) AS subjects,
        cl.class_level_name AS level,
        GROUP_CONCAT(DISTINCT d.district_name ORDER BY d.district_name ASC) AS districts,
        a.description AS description,
        a.feePerHr AS feePerHr,
        a.status AS status
    FROM application a
    LEFT JOIN member m ON a.member_id = m.member_id
    LEFT JOIN application_subject aps ON a.app_id = aps.app_id
    LEFT JOIN subject s ON aps.subject_id = s.subject_id
    LEFT JOIN class_level cl ON a.class_level_id = cl.class_level_id
    LEFT JOIN application_district ad ON a.app_id = ad.app_id
    LEFT JOIN district d ON ad.district_id = d.district_id
    WHERE a.member_id = ?
    GROUP BY a.app_id
");
$stmt->bind_param("i", $member_id); // Bind the member_id as an integer
$stmt->execute();

// Get the result
$result = $stmt->get_result();
?>

<div class="tab-pane fade <?php echo ($current_page === 'memberApplications') ? 'show active' : ''; ?>" id="applications">
    
    <?php if ($result->num_rows > 0): ?>
        <table class="table table-bordered">
            <thead>
                <tr>
                    <th>Application ID</th>
                    <th>Member ID</th>
                    <th>Application Creator</th>
                    <th>Subjects</th>
                    <th>Level</th>
                    <th>Districts</th>
                    <th>Description</th>
                    <th>Fee Per Hour</th>
                </tr>
            </thead>
            <tbody>
                <?php while ($row = $result->fetch_assoc()): ?>
                    <tr>
                        <td><?php echo htmlspecialchars($row['app_id']); ?></td>
                        <td><?php echo htmlspecialchars($row['member_id']); ?></td>
                        <td>
                            <?php 
                            $creator = $row['app_creator'] === 'T' ? 'Tutor' : 'Parent/Student';
                            echo htmlspecialchars($creator); 
                            ?>
                        </td>
                        <td><?php echo htmlspecialchars($row['subjects']); ?></td>
                        <td><?php echo htmlspecialchars($row['level']); ?></td>
                        <td><?php echo htmlspecialchars($row['districts']); ?></td>
                        <td><?php echo htmlspecialchars($row['description']); ?></td>
                        <td><?php echo htmlspecialchars($row['feePerHr']); ?></td>
                    </tr>
                <?php endwhile; ?>
            </tbody>
        </table>
    <?php else: ?>
        <p>No applications found for member ID <?php echo htmlspecialchars($member_id); ?>.</p>
    <?php endif; ?>
</div>
