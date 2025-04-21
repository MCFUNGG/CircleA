<?php
define('DB_HOST', 'localhost');
define('DB_USER', 'root');
define('DB_PASS', '');
define('DB_NAME', 'system001');

if (basename($_SERVER['PHP_SELF']) == 'config.php') {
    exit('Access denied');
} 