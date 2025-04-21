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
$sql_total_applications = "SELECT COUNT(*) as total_applications FROM application";
$result_total_applications = $conn->query($sql_total_applications);
$total_applications = 0;

if ($result_total_applications) {
  $row_total_applications = $result_total_applications->fetch_assoc();
  $total_applications = $row_total_applications['total_applications'];
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
    $page_title = "Payment";
    include 'includes/header.php';
    ?>
    <!-- Sidebar -->
    <?php include 'includes/sidebar.php'; ?>
    <!-- End Sidebar -->

    <!-- Main -->
    <main class="main-container">
      <div class="container-fluid">

        <?php
        // Reconnect to the database
        include 'includes/db_connect.php';

        // Fetch payment data with student names
        $sql_payments = "SELECT p.payment_id, p.match_id, m.username as student_name, p.amount, p.status, p.receipt_path, p.submitted_at, p.verified_at
                         FROM payment p
                         JOIN member m ON p.student_id = m.member_id
                         ORDER BY p.submitted_at DESC, p.payment_id DESC";
        $result_payments = $conn->query($sql_payments);
        ?>

        <div class="table-responsive">
          <table class="table table-striped table-bordered">
            <thead class="thead-dark">
              <tr>
                <th>Payment ID</th>
                <th>Match ID</th>
                <th>Student Name</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Submitted At</th>
                <th>Receipt</th>
                <th>Verified At</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <?php
              if ($result_payments && $result_payments->num_rows > 0) {
                  while ($row = $result_payments->fetch_assoc()) {
                      echo "<tr>";
                      echo "<td>" . htmlspecialchars($row['payment_id']) . "</td>";
                      echo "<td>" . htmlspecialchars($row['match_id']) . "</td>";
                      echo "<td>" . htmlspecialchars($row['student_name']) . "</td>";
                      echo "<td>$" . htmlspecialchars(number_format($row['amount'], 2)) . "</td>";
                      echo "<td>";
                      // Display status badge based on the status value
                      $status_class = '';
                      switch ($row['status']) {
                          case 'pending': $status_class = 'badge badge-warning'; break;
                          case 'confirmed': $status_class = 'badge badge-success'; break;
                          case 'rejected': $status_class = 'badge badge-danger'; break;
                          case 'not_submitted': $status_class = 'badge badge-secondary'; break;
                          default: $status_class = 'badge badge-light'; break;
                      }
                      // Just display the status, remove AJAX related classes/attributes
                      echo "<span class='" . $status_class . "'>" . ucfirst(htmlspecialchars($row['status'])) . "</span>";
                      echo "</td>";
                      echo "<td>" . ($row['submitted_at'] ? htmlspecialchars($row['submitted_at']) : '-') . "</td>";
                      echo "<td>";
                      if ($row['receipt_path']) {
                          // Prepend /FYP/ to the receipt path
                          echo "<a href='/FYP/" . htmlspecialchars($row['receipt_path']) . "' target='_blank'>View Receipt</a>";
                      } else {
                          echo "-";
                      }
                      echo "</td>";
                      // Remove data-id from verified-at cell
                      echo "<td>" . ($row['verified_at'] ? htmlspecialchars($row['verified_at']) : '-') . "</td>";
                      echo "<td>";
                      // Add buttons for changing status
                      echo "<form action='update_payment_status.php' method='post' style='display: inline-block; margin-right: 3px;'>";
                      echo "<input type='hidden' name='payment_id' value='" . htmlspecialchars($row['payment_id']) . "'>";
                      echo "<input type='hidden' name='new_status' value='pending'>";
                      echo "<button type='submit' class='btn btn-warning btn-sm'" . ($row['status'] == 'pending' ? ' disabled' : '') . ">Pending</button>";
                      echo "</form>";

                      echo "<form action='update_payment_status.php' method='post' style='display: inline-block; margin-right: 3px;'>";
                      echo "<input type='hidden' name='payment_id' value='" . htmlspecialchars($row['payment_id']) . "'>";
                      echo "<input type='hidden' name='new_status' value='confirmed'>";
                      echo "<button type='submit' class='btn btn-success btn-sm'" . ($row['status'] == 'confirmed' ? ' disabled' : '') . ">Confirm</button>";
                      echo "</form>";

                      echo "<form action='update_payment_status.php' method='post' style='display: inline-block;'>";
                      echo "<input type='hidden' name='payment_id' value='" . htmlspecialchars($row['payment_id']) . "'>";
                      echo "<input type='hidden' name='new_status' value='rejected'>";
                      echo "<button type='submit' class='btn btn-danger btn-sm'" . ($row['status'] == 'rejected' ? ' disabled' : '') . ">Reject</button>";
                      echo "</form>";
                      // We could add a button for 'not_submitted' if needed
                      echo "</td>";
                      echo "</tr>";
                  }
              } else {
                  echo "<tr><td colspan='9' class='text-center'>No payment records found.</td></tr>";
              }
              $conn->close(); // Close connection
              ?>
            </tbody>
          </table>
        </div>
      </div>
    </main>
    <!-- End Main -->

  </div>

  <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.5.3/dist/umd/popper.min.js"></script>
  <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>

</body>

</html>