<?php
session_start(); // Start session

include 'includes/session.php';

include 'includes/db_connect.php';

// Query for active members (distinct member_id in applications)
$sql_active_members = "SELECT COUNT(DISTINCT member_id) as active_members FROM application";
$result_active_members = $conn->query($sql_active_members);
$active_members = 0;

if ($result_active_members) {
  $row_active_members = $result_active_members->fetch_assoc();
  $active_members = $row_active_members['active_members'];
}

// Query for total applications
$sql_approved_applications = "SELECT COUNT(*) as approved_applications FROM application WHERE status = 'A'";
$result_approved_applications = $conn->query($sql_approved_applications);
$approved_applications = 0;

if ($result_approved_applications) {
  $row_approved_applications = $result_approved_applications->fetch_assoc();
  $approved_applications = $row_approved_applications['approved_applications'];
}

// Query for total members
$sql_total_members = "SELECT COUNT(*) as total_members FROM member";
$result_total_members = $conn->query($sql_total_members);
$total_members = 0;

if ($result_total_members) {
  $row_total_members = $result_total_members->fetch_assoc();
  $total_members = $row_total_members['total_members'];
}

// Query for total certificates
$sql_total_certs = "SELECT COUNT(*) as total_certs FROM member_cert";
$result_total_certs = $conn->query($sql_total_certs);
$total_certs = 0;

if ($result_total_certs) {
  $row_total_certs = $result_total_certs->fetch_assoc();
  $total_certs = $row_total_certs['total_certs'];
}

// Query for pending applications
$sql_pending_applications = "SELECT COUNT(*) as pending_applications FROM application WHERE status = 'P'";
$result_pending_applications = $conn->query($sql_pending_applications);
$pending_applications = 0;
if ($result_pending_applications) {
  $row_pending_applications = $result_pending_applications->fetch_assoc();
  $pending_applications = $row_pending_applications['pending_applications'];
}

// Query for active matches
$sql_active_matches = "SELECT COUNT(*) as active_matches FROM `match` WHERE status = 'A'"; // Use backticks for reserved keyword 'match'
$result_active_matches = $conn->query($sql_active_matches);
$active_matches = 0;
if ($result_active_matches) {
  $row_active_matches = $result_active_matches->fetch_assoc();
  $active_matches = $row_active_matches['active_matches'];
}

// Query for pending payments
$sql_pending_payments = "SELECT COUNT(*) as pending_payments FROM payment WHERE status = 'pending'";
$result_pending_payments = $conn->query($sql_pending_payments);
$pending_payments = 0;
if ($result_pending_payments) {
  $row_pending_payments = $result_pending_payments->fetch_assoc();
  $pending_payments = $row_pending_payments['pending_payments'];
}

// Query for active ads
$sql_active_ads = "SELECT COUNT(*) as active_ads FROM ads WHERE status = 'active'";
$result_active_ads = $conn->query($sql_active_ads);
$active_ads = 0;
if ($result_active_ads) {
  $row_active_ads = $result_active_ads->fetch_assoc();
  $active_ads = $row_active_ads['active_ads'];
}

// Query for pending CVs
$sql_pending_cvs = "SELECT COUNT(*) as pending_cvs FROM member_cv WHERE status = 'P'";
$result_pending_cvs = $conn->query($sql_pending_cvs);
$pending_cvs = 0;
if ($result_pending_cvs) {
  $row_pending_cvs = $result_pending_cvs->fetch_assoc();
  $pending_cvs = $row_pending_cvs['pending_cvs'];
}

// Query for pending bookings
$sql_pending_bookings = "SELECT COUNT(*) as pending_bookings FROM booking WHERE status = 'pending'";
$result_pending_bookings = $conn->query($sql_pending_bookings);
$pending_bookings = 0;
if ($result_pending_bookings) {
  $row_pending_bookings = $result_pending_bookings->fetch_assoc();
  $pending_bookings = $row_pending_bookings['pending_bookings'];
}

// Query for recent members
$sql_recent_members = "SELECT member_id, username, email FROM member ORDER BY member_id DESC LIMIT 5";
$result_recent_members = $conn->query($sql_recent_members);

// Query for recent applications
$sql_recent_applications = "SELECT a.app_id, m.username, cl.class_level_name, a.app_creator, a.status
                            FROM application a
                            JOIN member m ON a.member_id = m.member_id
                            JOIN class_level cl ON a.class_level_id = cl.class_level_id
                            ORDER BY a.app_id DESC LIMIT 5";
$result_recent_applications = $conn->query($sql_recent_applications);

// --- Chart Data Preparation ---

// 1. Application Status Distribution
$sql_app_status = "SELECT status, COUNT(*) as count FROM application GROUP BY status";
$result_app_status = $conn->query($sql_app_status);
$app_status_data = [];
$app_status_labels = [];
$status_map = [
    'P' => 'Pending',
    'A' => 'Approved',
    'R' => 'Rejected',
    'W' => 'Withdrawn', // Assuming 'W' might exist or other statuses
    // Add other statuses if needed
];
if ($result_app_status) {
    while($row = $result_app_status->fetch_assoc()) {
        $app_status_data[] = (int)$row['count'];
        // Use mapped label if exists, otherwise use the status code itself
        $app_status_labels[] = isset($status_map[$row['status']]) ? $status_map[$row['status']] : $row['status'];
    }
}

// 2. Recent Booking Trends (Last 30 Days)
$days_limit = 30; // Number of days for the trend
$sql_booking_trend = "SELECT DATE(created_at) as booking_date, COUNT(*) as count
                      FROM booking
                      WHERE created_at >= CURDATE() - INTERVAL ? DAY
                      GROUP BY DATE(created_at)
                      ORDER BY booking_date ASC";
$stmt_booking_trend = $conn->prepare($sql_booking_trend);
$stmt_booking_trend->bind_param("i", $days_limit);
$stmt_booking_trend->execute();
$result_booking_trend = $stmt_booking_trend->get_result();

$booking_trend_data = [];
$booking_trend_labels = [];
$booking_dates_counts = []; // Temporary array to hold counts by date

if ($result_booking_trend) {
    while($row = $result_booking_trend->fetch_assoc()) {
        $booking_dates_counts[$row['booking_date']] = (int)$row['count'];
    }
}

// Fill in missing dates with 0 count for the last 30 days
for ($i = $days_limit - 1; $i >= 0; $i--) {
    $date = date('Y-m-d', strtotime("-$i days"));
    $booking_trend_labels[] = $date;
    $booking_trend_data[] = isset($booking_dates_counts[$date]) ? $booking_dates_counts[$date] : 0;
}

$stmt_booking_trend->close();
// --- End Chart Data Preparation ---

$conn->close(); // Close connection
?>

<!DOCTYPE html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Admin Dashboard</title>

  <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
  <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600&display=swap" rel="stylesheet">
  <link href="https://fonts.googleapis.com/icon?family=Material+Icons+Outlined" rel="stylesheet">
  <link rel="stylesheet" href="css/styles.css">
  <link rel="stylesheet" href="css/sidebar.css">
  <link rel="stylesheet" href="css/header.css">
</head>

<body>
  <div class="grid-container">

    <?php
    // 設定當前頁面標題
    $page_title = "DASHBOARD";
    include 'includes/header.php';
    ?>
    <!-- Sidebar -->
    <?php include 'includes/sidebar.php'; ?>
    <!-- End Sidebar -->

    <!-- Main -->
    <main class="main-container">
      <div class="main-cards">

        <div class="card card-total-members">
          <div class="card-inner">
            <p class="text-primary"><?php echo $total_members; ?></p>
            <span class="material-icons-outlined text-green">person</span>
          </div>
          <span class="text-primary font-weight-bold">Total Members</span>
        </div>

        <div class="card card-approved-applications">
          <div class="card-inner">
            <p class="text-primary"><?php echo $approved_applications; ?></p>
            <span class="material-icons-outlined text-orange">assignment_turned_in</span>
          </div>
          <span class="text-primary font-weight-bold">Approved Applications</span>
        </div>

        <div class="card card-active-ads">
          <div class="card-inner">
            <p class="text-primary"><?php echo $active_ads; ?></p>
            <span class="material-icons-outlined text-cyan">campaign</span>
          </div>
          <span class="text-primary font-weight-bold">Active Ads</span>
        </div>

        <div class="card card-active-matches">
          <div class="card-inner">
            <p class="text-primary"><?php echo $active_matches; ?></p>
            <span class="material-icons-outlined text-purple">handshake</span>
          </div>
          <span class="text-primary font-weight-bold">Active Matches</span>
        </div>

        <div class="card card-pending-payment">
          <div class="card-inner">
            <p class="text-primary"><?php echo $pending_payments; ?></p>
            <span class="material-icons-outlined text-brown">payment</span>
          </div>
          <span class="text-primary font-weight-bold">Pending Payments</span>
        </div>

        <div class="card card-pending-application">
          <div class="card-inner">
             <p class="text-primary"><?php echo $pending_applications; ?></p>
             <span class="material-icons-outlined text-yellow">hourglass_top</span>
          </div>
          <span class="text-primary font-weight-bold">Pending Applications</span>
        </div>

        <div class="card card-pending-cv">
          <div class="card-inner">
            <p class="text-primary"><?php echo $pending_cvs; ?></p>
            <span class="material-icons-outlined text-indigo">assignment_ind</span>
          </div>
          <span class="text-primary font-weight-bold">Pending CVs</span>
        </div>

        <div class="card card-pending-booking">
          <div class="card-inner">
            <p class="text-primary"><?php echo $pending_bookings; ?></p>
            <span class="material-icons-outlined text-teal">pending_actions</span>
          </div>
          <span class="text-primary font-weight-bold">Pending Bookings</span>
        </div>

      </div>

      <!-- Recent Activities Section -->
      <div class="recent-activities row mt-4">
        <div class="col-md-6">
          <div class="card">
            <div class="card-header">
              <h5 class="card-title mb-0">Recent Members</h5>
            </div>
            <div class="card-body">
              <ul class="list-group list-group-flush">
                <?php
                if ($result_recent_members && $result_recent_members->num_rows > 0) {
                    while($row = $result_recent_members->fetch_assoc()) {
                        echo '<li class="list-group-item d-flex justify-content-between align-items-center">';
                        echo '<span><span class="material-icons-outlined align-middle mr-2">person</span>' . htmlspecialchars($row['username']) . ' (' . htmlspecialchars($row['email']) . ')</span>';
                        echo '<span class="badge badge-primary badge-pill">ID: ' . $row['member_id'] . '</span>';
                        echo '</li>';
                    }
                } else {
                    echo '<li class="list-group-item">No recent members found.</li>';
                }
                ?>
              </ul>
            </div>
          </div>
        </div>
        <div class="col-md-6">
          <div class="card">
            <div class="card-header">
              <h5 class="card-title mb-0">Recent Applications</h5>
            </div>
            <div class="card-body">
               <ul class="list-group list-group-flush">
                <?php
                if ($result_recent_applications && $result_recent_applications->num_rows > 0) {
                    while($row = $result_recent_applications->fetch_assoc()) {
                        $status_badge = '';
                        switch ($row['status']) {
                            case 'P': $status_badge = '<span class="badge badge-warning">Pending</span>'; break;
                            case 'A': $status_badge = '<span class="badge badge-success">Approved</span>'; break;
                            case 'R': $status_badge = '<span class="badge badge-danger">Rejected</span>'; break;
                            default: $status_badge = '<span class="badge badge-secondary">' . htmlspecialchars($row['status']) . '</span>'; break;
                        }
                        echo '<li class="list-group-item d-flex justify-content-between align-items-center">';
                        echo '<span>';
                        echo '<span class="material-icons-outlined align-middle mr-1">' . ($row['app_creator'] == 'T' ? 'school' : 'face') . '</span>'; // Icon based on creator type
                        echo 'App #' . $row['app_id'] . ' by ' . htmlspecialchars($row['username']) . ' (' . htmlspecialchars($row['class_level_name']) . ')';
                        echo '</span>';
                        echo $status_badge;
                        echo '</li>';
                    }
                } else {
                    echo '<li class="list-group-item">No recent applications found.</li>';
                }
                ?>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <!-- End Recent Activities Section -->

      <div class="charts">
        <div class="charts-card">
          <p class="chart-title">Application Status Distribution</p>
          <div id="status-pie-chart"></div>
        </div>

        <div class="charts-card">
          <p class="chart-title">Recent Booking Trends (Last <?php echo $days_limit; ?> Days)</p>
          <div id="booking-line-chart"></div>
        </div>
      </div>
    </main>
    <!-- End Main -->

  </div>

  <!-- Scripts -->
  <script src="https://cdnjs.cloudflare.com/ajax/libs/apexcharts/3.35.3/apexcharts.min.js"></script>
  <!-- Custom JS -->
  <script>
    // Convert PHP data to JavaScript
    const appStatusLabels = <?php echo json_encode($app_status_labels); ?>;
    const appStatusData = <?php echo json_encode($app_status_data); ?>;
    const bookingTrendLabels = <?php echo json_encode($booking_trend_labels); ?>;
    const bookingTrendData = <?php echo json_encode($booking_trend_data); ?>;

    // Application Status Pie Chart
    var statusPieChartOptions = {
      series: appStatusData,
      chart: {
        type: 'pie',
        height: 350
      },
      labels: appStatusLabels,
      title: {
        text: 'Application Status Distribution'
      },
      legend: {
        position: 'bottom'
      },
      responsive: [{
        breakpoint: 480,
        options: {
          chart: {
            width: 200
          },
          legend: {
            position: 'bottom'
          }
        }
      }]
    };

    var statusPieChart = new ApexCharts(document.querySelector("#status-pie-chart"), statusPieChartOptions);
    statusPieChart.render();

    // Recent Booking Trends Line Chart
    var bookingLineChartOptions = {
      series: [{
        name: 'New Bookings',
        data: bookingTrendData
      }],
      chart: {
        height: 350,
        type: 'line',
        zoom: {
          enabled: false
        }
      },
      dataLabels: {
        enabled: false
      },
      stroke: {
        curve: 'smooth'
      },
      title: {
        text: 'Daily New Bookings (Last <?php echo $days_limit; ?> Days)',
        align: 'left'
      },
      grid: {
        row: {
          colors: ['#f3f3f3', 'transparent'], // takes an array which will be repeated on columns
          opacity: 0.5
        },
      },
      xaxis: {
        categories: bookingTrendLabels,
        title: {
          text: 'Date'
        }
      },
      yaxis: {
        title: {
          text: 'Number of Bookings'
        },
        min: 0 // Ensure y-axis starts at 0
      }
    };

    var bookingLineChart = new ApexCharts(document.querySelector("#booking-line-chart"), bookingLineChartOptions);
    bookingLineChart.render();

  </script>
  <script src="js/scripts.js"></script>
</body>

</html>