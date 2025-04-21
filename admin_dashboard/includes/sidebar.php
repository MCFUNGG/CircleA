<?php
// Unified sidebar for all pages
?>
<aside id="sidebar">
    <div class="sidebar-title">
        <div class="sidebar-brand">
            <span class="material-icons-outlined">inventory</span> CircleA
        </div>
        <span class="material-icons-outlined" onclick="closeSidebar()">close</span>
    </div>

    <ul class="sidebar-list">
        <li class="sidebar-list-item">
            <a href="index.php" <?php echo basename($_SERVER['PHP_SELF']) == 'index.php' ? 'class="active"' : ''; ?>>
                <span class="material-icons-outlined">dashboard</span> Dashboard
            </a>
        </li>
        <li class="sidebar-list-item">
            <a href="matching.php" <?php echo basename($_SERVER['PHP_SELF']) == 'matching.php' ? 'class="active"' : ''; ?>>
                <span class="material-icons-outlined">swap_horiz</span> Matching
            </a>
        </li>
        <li class="sidebar-list-item">
            <a href="applications.php" <?php echo basename($_SERVER['PHP_SELF']) == 'applications.php' ? 'class="active"' : ''; ?>>
                <span class="material-icons-outlined">assignment</span> Applications
            </a>
        </li>
        <li class="sidebar-list-item">
            <a href="payment.php" <?php echo basename($_SERVER['PHP_SELF']) == 'payment.php' ? 'class="active"' : ''; ?>>
                <span class="material-icons-outlined">payment</span> Payment
            </a>
        </li>
        <li class="sidebar-list-item">
            <a href="verify_cv.php" <?php echo basename($_SERVER['PHP_SELF']) == 'verify_cv.php' ? 'class="active"' : ''; ?>>
                <span class="material-icons-outlined">verified</span> Verify CV
            </a>
        </li>
        <li class="sidebar-list-item">
            <a href="ads.php" <?php echo basename($_SERVER['PHP_SELF']) == 'ads.php' ? 'class="active"' : ''; ?>>
                <span class="material-icons-outlined">campaign</span> Advertisements
            </a>
        </li>
        <li class="sidebar-list-item">
            <a href="member.php" <?php echo basename($_SERVER['PHP_SELF']) == 'member.php' ? 'class="active"' : ''; ?>>
                <span class="material-icons-outlined">group</span> Members
            </a>
        </li>
        <li class="sidebar-list-item">
            <a href="add_member.php" <?php echo basename($_SERVER['PHP_SELF']) == 'add_member.php' ? 'class="active"' : ''; ?>>
                <span class="material-icons-outlined">person_add</span> Add Member
            </a>
        </li>
        <li class="sidebar-list-item">
            <a href="reports.php" <?php echo basename($_SERVER['PHP_SELF']) == 'reports.php' ? 'class="active"' : ''; ?>>
                <span class="material-icons-outlined">poll</span> Reports
            </a>
        </li>
        <li class="sidebar-list-item">
            <a href="settings.php" <?php echo basename($_SERVER['PHP_SELF']) == 'settings.php' ? 'class="active"' : ''; ?>>
                <span class="material-icons-outlined">settings</span> Settings
            </a>
        </li>
        <li class="sidebar-list-item">
            <a href="logout.php">
                <span class="material-icons-outlined">logout</span> Logout
            </a>
        </li>
    </ul>
</aside> 