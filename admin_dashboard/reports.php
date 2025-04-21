<?php

include 'includes/session.php';

include 'includes/db_connect.php';

// --- Fetch Data for All Reports ---

// 1. User Management Report
$sql_users = "SELECT member_id, username, email, phone, isAdmin, status FROM member ORDER BY member_id ASC";
$result_users = $conn->query($sql_users);

// 2. Detailed Application List
$sql_applications = "SELECT a.app_id, m.username AS applicant_name, a.app_creator, cl.class_level_name, a.status AS app_status, a.feePerHr, a.lessonPerWeek 
                     FROM application a
                     JOIN member m ON a.member_id = m.member_id
                     JOIN class_level cl ON a.class_level_id = cl.class_level_id
                     ORDER BY a.app_id DESC";
$result_applications = $conn->query($sql_applications);

// 3. Payment Status Tracking Report
$sql_payments = "SELECT p.payment_id, p.match_id, m_student.username AS student_name, p.amount, p.status AS payment_status, p.receipt_path, p.submitted_at, p.verified_at
                 FROM payment p
                 LEFT JOIN member m_student ON p.student_id = m_student.member_id
                 ORDER BY p.payment_id DESC";
// Note: LEFT JOIN is used for student in case student_id is somehow null or points to a deleted member
$result_payments = $conn->query($sql_payments);

// 4. Tutor Rating & Performance Report
$sql_tutor_ratings = "SELECT 
                        m.username AS tutor_name,
                        m.member_id AS tutor_id, 
                        AVG(tr.rate_score) AS average_rating,
                        COUNT(tr.rate_id) AS rating_count
                      FROM 
                        tutor_rating tr
                      JOIN 
                        application a ON tr.application_id = a.app_id 
                      JOIN 
                        member m ON a.member_id = m.member_id -- Assuming member_id in application is the tutor when app_creator = 'T'
                      WHERE 
                        a.app_creator = 'T' -- Focus on ratings related to tutor applications
                        AND tr.role = 'parent' -- Consider only ratings given BY parents/students TO tutors
                      GROUP BY 
                        m.member_id, m.username
                      ORDER BY 
                        average_rating DESC";
$result_tutor_ratings = $conn->query($sql_tutor_ratings);

// --- Fetch Data for Chart Reports ---

// 1. Applications by Subject
$sql_subj_dist = "SELECT s.subject_name, COUNT(aps.application_subject_id) as count 
                  FROM application_subject aps
                  JOIN subject s ON aps.subject_id = s.subject_id
                  GROUP BY s.subject_name
                  ORDER BY count DESC";
$result_subj_dist = $conn->query($sql_subj_dist);
$subj_labels = [];
$subj_data = [];
if ($result_subj_dist) {
    while($row = $result_subj_dist->fetch_assoc()) {
        $subj_labels[] = $row['subject_name'];
        $subj_data[] = (int)$row['count'];
    }
}

// 2. Applications by Class Level
$sql_level_dist = "SELECT cl.class_level_name, COUNT(a.app_id) as count 
                   FROM application a
                   JOIN class_level cl ON a.class_level_id = cl.class_level_id
                   GROUP BY cl.class_level_name
                   ORDER BY cl.class_level_id ASC"; // Order by level ID for logical sequence
$result_level_dist = $conn->query($sql_level_dist);
$level_labels = [];
$level_data = [];
if ($result_level_dist) {
    while($row = $result_level_dist->fetch_assoc()) {
        $level_labels[] = $row['class_level_name'];
        $level_data[] = (int)$row['count'];
    }
}

// 3. Applications by District
$sql_district_dist = "SELECT d.district_name, COUNT(ad.application_district_id) as count
                     FROM application_district ad
                     JOIN district d ON ad.district_id = d.district_id
                     -- Optionally JOIN application if you only want to count active/pending applications
                     -- JOIN application a ON ad.app_id = a.app_id WHERE a.status IN ('A', 'P') 
                     GROUP BY d.district_name
                     ORDER BY count DESC";
$result_district_dist = $conn->query($sql_district_dist);
$district_labels = [];
$district_data = [];
if ($result_district_dist) {
    while($row = $result_district_dist->fetch_assoc()) {
        $district_labels[] = $row['district_name'];
        $district_data[] = (int)$row['count'];
    }
}

// 4. Revenue Trend (Monthly Confirmed Payments)
// Fetching for the last 12 months for example
$sql_revenue_trend = "SELECT DATE_FORMAT(verified_at, '%Y-%m') AS month, SUM(amount) AS total_amount
                      FROM payment
                      WHERE status = 'confirmed' AND verified_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
                      GROUP BY DATE_FORMAT(verified_at, '%Y-%m')
                      ORDER BY month ASC";
$result_revenue_trend = $conn->query($sql_revenue_trend);
$revenue_labels = [];
$revenue_data = [];
$revenue_monthly = []; // Temp array
if ($result_revenue_trend) {
    while($row = $result_revenue_trend->fetch_assoc()) {
        $revenue_monthly[$row['month']] = (float)$row['total_amount'];
    }
}
// Fill missing months with 0
for ($i = 11; $i >= 0; $i--) {
    $month = date('Y-m', strtotime("-$i months"));
    $revenue_labels[] = $month;
    $revenue_data[] = isset($revenue_monthly[$month]) ? $revenue_monthly[$month] : 0;
}


// 4. Tutor Rating Distribution (Parent/Student ratings for Tutors)
$sql_rating_dist = "SELECT 
                        CASE 
                          WHEN tr.rate_score >= 4.5 THEN '4.5 - 5.0'
                          WHEN tr.rate_score >= 4.0 THEN '4.0 - 4.4'
                          WHEN tr.rate_score >= 3.5 THEN '3.5 - 3.9'
                          WHEN tr.rate_score >= 3.0 THEN '3.0 - 3.4'
                          ELSE 'Below 3.0'
                        END AS rating_range,
                        COUNT(tr.rate_id) AS count
                      FROM 
                        tutor_rating tr
                      JOIN 
                        application a ON tr.application_id = a.app_id 
                      WHERE 
                        a.app_creator = 'T' AND tr.role = 'parent' 
                      GROUP BY 
                        rating_range
                      ORDER BY 
                        CASE rating_range -- Custom order for ranges
                          WHEN '4.5 - 5.0' THEN 1
                          WHEN '4.0 - 4.4' THEN 2
                          WHEN '3.5 - 3.9' THEN 3
                          WHEN '3.0 - 3.4' THEN 4
                          ELSE 5
                        END ASC";
$result_rating_dist = $conn->query($sql_rating_dist);
$rating_dist_labels = [];
$rating_dist_data = [];
if ($result_rating_dist) {
    while($row = $result_rating_dist->fetch_assoc()) {
        $rating_dist_labels[] = $row['rating_range'];
        $rating_dist_data[] = (int)$row['count'];
    }
}

$conn->close(); // Close connection after fetching all data
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Application Reports</title>
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
        $page_title = "Application Reports"; // Update page title
        include 'includes/header.php'; 
        ?>
        <?php include 'includes/sidebar.php'; ?>
        <main class="main-container">
            <h3 class="mb-4">Application Distribution Reports</h3>

            <div class="charts">
                 <!-- Row 1 -->
                <div class="charts-card">
                  <p class="chart-title">Applications by Subject</p>
                  <div id="subject-distribution-chart"></div>
                </div>

                <div class="charts-card">
                  <p class="chart-title">Applications by Class Level</p>
                  <div id="level-distribution-chart"></div>
                </div>
                
                <!-- Row 2 -->
                 <div class="charts-card">
                  <p class="chart-title">Applications by District</p>
                  <div id="district-distribution-chart"></div>
                </div>
                 <!-- Add another chart here later if needed -->
                 <div class="charts-card" style="visibility: hidden;"> <!-- Placeholder to maintain grid structure -->
                 </div>
            </div>
            
        </main>
    </div>

    <!-- ApexCharts -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/apexcharts/3.35.3/apexcharts.min.js"></script>
    
    <!-- Chart Initialization -->
    <script>
        // Pass PHP data to JavaScript
        const subjLabels = <?php echo json_encode($subj_labels); ?>;
        const subjData = <?php echo json_encode($subj_data); ?>;
        const levelLabels = <?php echo json_encode($level_labels); ?>;
        const levelData = <?php echo json_encode($level_data); ?>;
        const districtLabels = <?php echo json_encode($district_labels); ?>;
        const districtData = <?php echo json_encode($district_data); ?>;
      
        // 1. Subject Distribution Chart (Horizontal Bar)
        var subjDistOptions = {
          series: [{
            name: 'Applications',
            data: subjData
          }],
          chart: {
            type: 'bar',
            height: 450 // Increased height for potentially many subjects
          },
          plotOptions: {
            bar: {
              horizontal: true, // Make it horizontal for better label readability
            },
          },
          dataLabels: {
            enabled: true,
             style: {
                fontSize: '12px',
                colors: ["#304758"]
            },
            offsetX: 30
          },
          stroke: {
            show: true,
            width: 1,
            colors: ['#fff']
          },
          xaxis: {
            categories: subjLabels,
             title: {
                text: 'Number of Applications'
            }
          },
          yaxis: {
            // title: { text: 'Subject' }
          },
          fill: {
            opacity: 1
          },
          tooltip: {
             y: {
              formatter: function (val) {
                return val + " applications"
              }
            }
          },
           title: {
                text: 'Applications per Subject',
                align: 'center'
            }
        };
        var subjDistChart = new ApexCharts(document.querySelector("#subject-distribution-chart"), subjDistOptions);
        subjDistChart.render();

        // 2. Class Level Distribution Chart (Vertical Bar)
        var levelDistOptions = {
          series: [{
            name: 'Applications',
            data: levelData
          }],
          chart: {
            type: 'bar',
            height: 350
          },
          plotOptions: {
            bar: {
              horizontal: false,
              columnWidth: '60%',
              // endingShape: 'rounded'
            },
          },
          dataLabels: {
            enabled: false
          },
           stroke: {
            show: true,
            width: 2,
            colors: ['transparent']
          },
          xaxis: {
            categories: levelLabels,
            title: {
                text: 'Class Level'
            },
             labels: { rotate: -45, hideOverlappingLabels: false, trim: false, style: { fontSize: '10px' } } // Rotate labels if needed
          },
          yaxis: {
            title: {
              text: 'Number of Applications'
            }
          },
          fill: {
            opacity: 1
          },
          tooltip: {
            y: {
              formatter: function (val) {
                return val + " applications"
              }
            }
          },
           title: {
                text: 'Applications per Class Level',
                align: 'center'
            }
        };
        var levelDistChart = new ApexCharts(document.querySelector("#level-distribution-chart"), levelDistOptions);
        levelDistChart.render();

         // 3. District Distribution Chart (Horizontal Bar)
        var districtDistOptions = {
          series: [{
            name: 'Applications',
            data: districtData
          }],
          chart: {
            type: 'bar',
            height: 400 // Adjust height as needed
          },
          plotOptions: {
            bar: {
              horizontal: true,
            },
          },
          dataLabels: {
            enabled: true,
             style: {
                fontSize: '12px',
                colors: ["#304758"]
            },
             offsetX: 30
          },
           stroke: {
            show: true,
            width: 1,
            colors: ['#fff']
          },
          xaxis: {
            categories: districtLabels,
            title: {
                text: 'Number of Applications'
            }
          },
          yaxis: {
             // title: { text: 'District' }
          },
          fill: {
            opacity: 1
          },
          tooltip: {
            y: {
              formatter: function (val) {
                return val + " applications"
              }
            }
          },
           title: {
                text: 'Applications per District',
                align: 'center'
            }
        };
        var districtDistChart = new ApexCharts(document.querySelector("#district-distribution-chart"), districtDistOptions);
        districtDistChart.render();

    </script>

</body>
</html> 