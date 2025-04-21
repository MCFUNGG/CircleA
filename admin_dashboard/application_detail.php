<?php
require_once 'includes/session.php';

// Database connection
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "system001";


$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Get application ID from URL
if (!isset($_GET['id'])) {
    header("Location: applications.php");
    exit();
}

$app_id = $_GET['id'];

// Get application details
$sql = "SELECT a.*, m.username, m.email, m.phone, c.class_level_name 
        FROM application a 
        LEFT JOIN member m ON a.member_id = m.member_id
        LEFT JOIN class_level c ON a.class_level_id = c.class_level_id
        WHERE a.app_id = ?";

$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $app_id);
$stmt->execute();
$result = $stmt->get_result();
$app = $result->fetch_assoc();

// Get subjects
$subject_sql = "SELECT s.subject_name 
                FROM application_subject aps 
                JOIN subject s ON aps.subject_id = s.subject_id 
                WHERE aps.app_id = ?";
$stmt = $conn->prepare($subject_sql);
$stmt->bind_param("i", $app_id);
$stmt->execute();
$subjects_result = $stmt->get_result();
$subjects = [];
while ($subject = $subjects_result->fetch_assoc()) {
    $subjects[] = $subject['subject_name'];
}

// Get districts
$district_sql = "SELECT d.district_name, d.Latitude, d.Longitude 
                 FROM application_district ad 
                 JOIN district d ON ad.district_id = d.district_id 
                 WHERE ad.app_id = ?";
$stmt = $conn->prepare($district_sql);
$stmt->bind_param("i", $app_id);
$stmt->execute();
$districts_result = $stmt->get_result();
$districts = [];
while ($district = $districts_result->fetch_assoc()) {
    $districts[] = $district;
}

// Get available time
$time_sql = "SELECT * FROM application_date WHERE app_id = ?";
$stmt = $conn->prepare($time_sql);
$stmt->bind_param("i", $app_id);
$stmt->execute();
$time_result = $stmt->get_result();
$time_data = $time_result->fetch_assoc();
?>

<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Application Details</title>

    <!-- Montserrat Font -->
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@100;200;300;400;500;600;700;800;900&display=swap"
        rel="stylesheet">

    <!-- Material Icons -->
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons+Outlined" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200&icon_names=checklist_rtl"
        rel="stylesheet" />

    <!-- CSS Files -->
    <link rel="stylesheet" href="css/styles.css">    <!-- 基本樣式 -->
    <link rel="stylesheet" href="css/sidebar.css">   <!-- Sidebar 樣式 -->
    <link rel="stylesheet" href="css/header.css">    <!-- Header 樣式 -->
    
    <!-- Bootstrap & Leaflet -->
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css">
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
            <div class="container-fluid">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h2>Application Details #<?php echo $app_id; ?></h2>
                    <a href="applications.php" class="btn btn-secondary">
                        <span class="material-icons-outlined">arrow_back</span> Back
                    </a>
                </div>

                <div class="row">
                    <!-- Basic Information -->
                    <div class="col-md-6">
                        <div class="card mb-4">
                            <div class="card-header">
                                <h5 class="card-title mb-0">Basic Information</h5>
                            </div>
                            <div class="card-body">
                                <dl class="row">
                                    <dt class="col-sm-4">Status</dt>
                                    <dd class="col-sm-8">
                                        <?php
                                        $status_class = [
                                            'P' => 'badge-warning',
                                            'A' => 'badge-success',
                                            'R' => 'badge-danger'
                                        ];
                                        $status_text = [
                                            'P' => 'Pending',
                                            'A' => 'Approved',
                                            'R' => 'Rejected'
                                        ];
                                        echo "<span class='badge " . $status_class[$app['status']] . "'>"
                                            . $status_text[$app['status']] . "</span>";
                                        ?>
                                    </dd>

                                    <dt class="col-sm-4">Creator Type</dt>
                                    <dd class="col-sm-8">
                                        <?php echo $app['app_creator'] == 'T' ? 'Tutor' : 'Parent/Student'; ?>
                                    </dd>

                                    <dt class="col-sm-4">Member</dt>
                                    <dd class="col-sm-8">
                                        <?php echo $app['username'] ?? 'N/A'; ?><br>
                                        <small>
                                            <?php echo $app['email'] ?? ''; ?><br>
                                            <?php echo $app['phone'] ?? ''; ?>
                                        </small>
                                    </dd>

                                    <dt class="col-sm-4">Fee per Hour</dt>
                                    <dd class="col-sm-8">HKD $<?php echo $app['feePerHr']; ?></dd>

                                    <dt class="col-sm-4">Class Level</dt>
                                    <dd class="col-sm-8"><?php echo $app['class_level_name']; ?></dd>
                                </dl>
                            </div>
                        </div>
                    </div>

                    <!-- Subjects & Districts -->
                    <div class="col-md-6">
                        <div class="card mb-4">
                            <div class="card-header">
                                <h5 class="card-title mb-0">Subjects & Districts</h5>
                            </div>
                            <div class="card-body">
                                <h6>Subjects:</h6>
                                <ul class="list-unstyled">
                                    <?php foreach ($subjects as $subject): ?>
                                        <li class="mb-2">
                                            <span class="material-icons-outlined align-middle">school</span>
                                            <span class="align-middle"><?php echo $subject; ?></span>
                                        </li>
                                    <?php endforeach; ?>
                                </ul>

                                <h6 class="mt-4">Districts:</h6>
                                <ul class="list-unstyled">
                                    <?php foreach ($districts as $district): ?>
                                        <li class="mb-2">
                                            <span class="material-icons-outlined align-middle">location_on</span>
                                            <span class="align-middle"><?php echo $district['district_name']; ?></span>
                                        </li>
                                    <?php endforeach; ?>
                                </ul>
                            </div>
                        </div>
                    </div>

                    <!-- Available Time -->
                    <div class="col-md-6">
                        <div class="card mb-4">
                            <div class="card-header">
                                <h5 class="card-title mb-0">Available Time</h5>
                            </div>
                            <div class="card-body">
                                <div class="table-responsive">
                                    <table class="table table-sm">
                                        <thead>
                                            <tr>
                                                <th>Day</th>
                                                <th>Time</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <?php
                                            $days = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];
                                            foreach ($days as $day) {
                                                $time_key = $day . '_time';
                                                if (!empty($time_data[$time_key])) {
                                                    echo "<tr>";
                                                    echo "<td>" . ucfirst($day) . "</td>";
                                                    echo "<td>" . $time_data[$time_key] . "</td>";
                                                    echo "</tr>";
                                                }
                                            }
                                            ?>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Map -->
                    <div class="col-md-6">
                        <div class="card mb-4">
                            <div class="card-header">
                                <h5 class="card-title mb-0">District Locations</h5>
                            </div>
                            <div class="card-body">
                                <div id="map" style="height: 400px;"></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>

    <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
    <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
    <script src="js/scripts.js"></script>

    <!-- Map initialization -->
    <script>
        var map = L.map('map').setView([22.302711, 114.177216], 11);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(map);

        <?php foreach ($districts as $district): ?>
            L.marker([<?php echo $district['Latitude']; ?>, <?php echo $district['Longitude']; ?>])
                .bindPopup("<?php echo $district['district_name']; ?>")
                .addTo(map);
        <?php endforeach; ?>
    </script>
</body>

</html>

<?php
$conn->close();
?>