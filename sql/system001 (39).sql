-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- 主機： 127.0.0.1
-- 產生時間： 2025-04-24 18:38:32
-- 伺服器版本： 10.4.27-MariaDB
-- PHP 版本： 8.2.0

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 資料庫： `system001`
--

-- --------------------------------------------------------

--
-- 資料表結構 `ads`
--

CREATE TABLE `ads` (
  `ad_id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `image_url` varchar(255) NOT NULL,
  `link_url` varchar(255) DEFAULT NULL,
  `status` enum('active','inactive') NOT NULL DEFAULT 'active',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `sort_order` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `ads`
--

INSERT INTO `ads` (`ad_id`, `title`, `description`, `image_url`, `link_url`, `status`, `created_at`, `updated_at`, `sort_order`) VALUES
(7, 'm', '', '/FYP/ads/1744341636_67f88a843f1cf.jpg', NULL, 'active', '2025-04-11 03:20:36', '2025-04-11 03:20:36', 0),
(8, '222', 'gdgd', '/FYP/ads/1744352822_67f8b63651b5a.jpg', 'https://www.google.com/search?q=news&rlz=1C1CHZN_enHK1128HK1128&oq=news&gs_lcrp=EgZjaHJvbWUyDAgAEEUYORjJAxiABDINCAEQABiDARixAxiABDINCAIQABiSAxiABBiKBTINCAMQABiDARixAxiABDIHCAQQABiABDIHCAUQABiABDINCAYQABiDARixAxiABDINCAcQABiDARixAxiABDIHCAgQABiABDIGCAkQABg', 'inactive', '2025-04-11 06:27:02', '2025-04-11 06:27:02', 0),
(9, '3', 'grfrg', '/FYP/ads/1744352877_67f8b66dc0405.png', NULL, 'active', '2025-04-11 06:27:57', '2025-04-11 06:27:57', 0);

-- --------------------------------------------------------

--
-- 資料表結構 `application`
--

CREATE TABLE `application` (
  `app_id` int(10) NOT NULL,
  `member_id` int(10) NOT NULL,
  `app_creator` varchar(2) NOT NULL,
  `class_level_id` int(10) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `feePerHr` float NOT NULL,
  `lessonPerWeek` int(2) DEFAULT NULL,
  `status` varchar(1) NOT NULL DEFAULT 'P'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `application`
--

INSERT INTO `application` (`app_id`, `member_id`, `app_creator`, `class_level_id`, `description`, `feePerHr`, `lessonPerWeek`, `status`) VALUES
(77, 1, 'T', 1, '', 222, NULL, 'A'),
(80, 3, 'T', 19, 'fhc', 500, NULL, 'A'),
(81, 1, 'T', 6, '/', 250, NULL, 'A'),
(98, 1, 'PS', 3, '1', 444, NULL, 'A'),
(108, 1, 'PS', 1, '', 222, NULL, 'A'),
(109, 5, 'T', 9, 'Ma Wan', 250, NULL, 'A'),
(110, 3, 'PS', 9, '', 251, NULL, 'A'),
(111, 3, 'PS', 15, '', 250, NULL, 'A'),
(112, 1, 'PS', 11, '', 250, NULL, 'P'),
(113, 1, 'PS', 5, '', 320, NULL, 'P'),
(115, 1, 'PS', 1, '', 250, 1, 'R'),
(120, 6, 'PS', 15, '', 200, 1, 'A'),
(121, 5, 'T', 1, '', 200, NULL, 'A'),
(123, 3, 'PS', 9, '', 250, 1, 'A'),
(124, 5, 'T', 9, '', 200, NULL, 'A'),
(125, 3, 'PS', 15, '', 200, 1, 'A'),
(126, 5, 'PS', 1, '', 205, 1, 'A'),
(127, 3, 'PS', 1, '', 410, 2, 'A'),
(132, 5, 'T', 15, '', 250, NULL, 'A');

-- --------------------------------------------------------

--
-- 資料表結構 `application_date`
--

CREATE TABLE `application_date` (
  `app_Date_id` int(10) NOT NULL,
  `app_id` int(10) NOT NULL,
  `monday_time` varchar(11) DEFAULT NULL,
  `tuesday_time` varchar(11) DEFAULT NULL,
  `wednesday_time` varchar(11) DEFAULT NULL,
  `thursday_time` varchar(11) DEFAULT NULL,
  `friday_time` varchar(11) DEFAULT NULL,
  `saturday_time` varchar(11) DEFAULT NULL,
  `sunday_time` varchar(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `application_date`
--

INSERT INTO `application_date` (`app_Date_id`, `app_id`, `monday_time`, `tuesday_time`, `wednesday_time`, `thursday_time`, `friday_time`, `saturday_time`, `sunday_time`) VALUES
(1, 81, '1200-1300', '', '1600-1900', '', '', '', ''),
(2, 98, '', '', '1400-1400', '', '', '', ''),
(11, 108, '1400-1600', '', '', '', '', '', ''),
(12, 109, '1400-1630', '', '', '', '', '', ''),
(13, 110, '1400-1600', '', '', '', '', '', ''),
(14, 111, '1400-1600', '', '', '', '', '', ''),
(15, 112, '', '', '', '1200-1630', '', '', ''),
(16, 113, '1400-1600', '', '', '', '', '', ''),
(18, 115, '1200-1600', '', '', '', '', '', ''),
(23, 120, '1200-1600', '', '', '', '', '', ''),
(24, 121, '1200-1600', '', '', '', '', '', ''),
(26, 123, '1200-1600', '', '', '', '', '', ''),
(27, 124, '1400-1600', '', '', '', '', '', ''),
(28, 125, '1400-1530', '', '', '', '', '', ''),
(29, 126, '1400 - 1600', '', '', '', '', '', ''),
(30, 127, '1200-1600', '', '', '', '', '', ''),
(35, 132, '1500-1600', '', '', '', '', '', '');

-- --------------------------------------------------------

--
-- 資料表結構 `application_district`
--

CREATE TABLE `application_district` (
  `application_district_id` int(10) NOT NULL,
  `app_id` int(10) NOT NULL,
  `district_id` int(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `application_district`
--

INSERT INTO `application_district` (`application_district_id`, `app_id`, `district_id`) VALUES
(1, 77, 1),
(8, 80, 7),
(9, 80, 13),
(10, 80, 14),
(11, 81, 2),
(12, 81, 4),
(29, 98, 1),
(30, 98, 2),
(46, 108, 1),
(49, 110, 2),
(50, 111, 2),
(51, 111, 3),
(52, 111, 4),
(53, 112, 1),
(54, 112, 4),
(55, 113, 2),
(56, 113, 3),
(59, 115, 5),
(68, 120, 2),
(69, 120, 3),
(70, 121, 5),
(73, 123, 8),
(74, 123, 9),
(75, 124, 10),
(76, 124, 11),
(77, 125, 10),
(78, 125, 11),
(84, 127, 6),
(97, 126, 1),
(98, 126, 5),
(99, 126, 9),
(100, 126, 12),
(101, 126, 14),
(110, 109, 4),
(111, 109, 5),
(116, 132, 1),
(117, 132, 2);

-- --------------------------------------------------------

--
-- 資料表結構 `application_subject`
--

CREATE TABLE `application_subject` (
  `application_subject_id` int(10) NOT NULL,
  `app_id` int(10) NOT NULL,
  `subject_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- 傾印資料表的資料 `application_subject`
--

INSERT INTO `application_subject` (`application_subject_id`, `app_id`, `subject_id`) VALUES
(1, 77, '1'),
(2, 77, '10'),
(3, 77, '11'),
(10, 80, '12'),
(11, 80, '15'),
(12, 80, '7'),
(13, 81, '2'),
(14, 81, '3'),
(15, 81, '4'),
(77, 108, '2'),
(80, 110, '1'),
(81, 110, '3'),
(82, 110, '5'),
(83, 111, '1'),
(84, 111, '3'),
(85, 111, '5'),
(86, 112, '1'),
(87, 113, '1'),
(88, 113, '3'),
(89, 113, '5'),
(92, 115, '3'),
(100, 120, '11'),
(101, 120, '12'),
(102, 121, '1'),
(103, 121, '3'),
(104, 121, '5'),
(107, 123, '1'),
(108, 123, '2'),
(109, 124, '1'),
(110, 124, '3'),
(111, 125, '1'),
(112, 125, '2'),
(116, 127, '1'),
(117, 127, '3'),
(129, 126, '1'),
(130, 126, '2'),
(131, 126, '3'),
(132, 126, '5'),
(141, 109, '5'),
(142, 109, '12'),
(146, 132, '1'),
(147, 132, '3');

-- --------------------------------------------------------

--
-- 資料表結構 `booking`
--

CREATE TABLE `booking` (
  `booking_id` int(11) NOT NULL,
  `match_id` int(10) DEFAULT NULL,
  `tutor_id` int(10) NOT NULL,
  `student_id` int(10) DEFAULT NULL,
  `date` date NOT NULL,
  `start_time` time DEFAULT NULL,
  `end_time` time DEFAULT NULL,
  `status` enum('available','pending','confirmed','expired','completed','conflict') DEFAULT 'available',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `booking`
--

INSERT INTO `booking` (`booking_id`, `match_id`, `tutor_id`, `student_id`, `date`, `start_time`, `end_time`, `status`, `created_at`, `updated_at`) VALUES
(9, 69, 5, 3, '2025-03-31', '09:00:00', '10:08:00', '', '2025-03-16 07:21:13', '2025-03-19 13:38:34'),
(18, 69, 5, NULL, '2025-04-17', '09:00:00', '10:00:00', 'available', '2025-04-11 03:46:39', '2025-04-11 04:42:48'),
(21, 69, 5, 3, '2025-04-23', '09:00:00', '10:00:00', 'available', '2025-04-11 06:10:04', '2025-04-23 03:38:09'),
(22, 104, 5, 3, '2025-04-24', '09:00:00', '10:00:00', '', '2025-04-21 10:25:22', '2025-04-22 09:00:00'),
(23, 104, 5, NULL, '2025-04-25', '09:00:00', '10:00:00', 'available', '2025-04-21 10:35:00', '2025-04-22 20:11:06'),
(24, 113, 3, 5, '2025-04-25', '09:00:00', '10:00:00', 'available', '2025-04-22 10:10:27', '2025-04-22 12:58:58'),
(27, 114, 5, NULL, '2025-04-30', '09:00:00', '10:00:00', 'available', '2025-04-22 12:59:28', '2025-04-22 20:11:43'),
(28, 114, 5, NULL, '2025-04-24', '09:00:00', '10:00:00', '', '2025-04-22 20:10:53', '2025-04-22 20:22:10'),
(38, 116, 5, NULL, '2025-04-26', '09:00:00', '10:00:00', '', '2025-04-23 18:19:39', '2025-04-23 18:24:29'),
(40, 116, 5, NULL, '2025-04-30', '09:00:00', '10:00:00', '', '2025-04-23 18:58:55', '2025-04-23 19:14:41'),
(42, 116, 5, 3, '2025-04-30', '09:00:00', '10:01:00', 'completed', '2025-04-23 19:20:15', '2025-04-24 16:28:44');

-- --------------------------------------------------------

--
-- 資料表結構 `class_level`
--

CREATE TABLE `class_level` (
  `class_level_id` int(10) NOT NULL,
  `class_level_name` varchar(50) NOT NULL,
  `applicationapp_id` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `class_level`
--

INSERT INTO `class_level` (`class_level_id`, `class_level_name`, `applicationapp_id`) VALUES
(1, 'Kindergarten - K.1', ''),
(2, 'Kindergarten - K.2', ''),
(3, 'Kindergarten - K.3', ''),
(4, 'Primary school - P.1', ''),
(5, 'Primary school - P.2', ''),
(6, 'Primary school - P.3', ''),
(7, 'Primary school - P.4', ''),
(8, 'Primary school - P.5', ''),
(9, 'Primary school - P.6', ''),
(10, 'Secondary School - F.1', ''),
(11, 'Secondary School - F.2', ''),
(12, 'Secondary School - F.3', ''),
(13, 'Secondary School - F.4', ''),
(14, 'Secondary School - F.5', ''),
(15, 'Secondary School - F.6', ''),
(16, 'University - College freshman', ''),
(17, 'University - Sophomore', ''),
(18, 'University - Third year', ''),
(19, 'University - Senior year', '');

-- --------------------------------------------------------

--
-- 資料表結構 `cv_data`
--

CREATE TABLE `cv_data` (
  `cv_id` int(11) NOT NULL,
  `member_id` int(11) NOT NULL,
  `contact` text DEFAULT NULL,
  `skills` text DEFAULT NULL,
  `education` text DEFAULT NULL,
  `language` text DEFAULT NULL,
  `other` text DEFAULT NULL,
  `cv_score` float DEFAULT 0,
  `cv_path` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp(),
  `Score` int(3) DEFAULT NULL,
  `status` varchar(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `cv_data`
--

INSERT INTO `cv_data` (`cv_id`, `member_id`, `contact`, `skills`, `education`, `language`, `other`, `cv_score`, `cv_path`, `created_at`, `last_modified`, `Score`, `status`) VALUES
(1, 3, 'CONTACT\nlaukwantingabc1 23@gmal. com', 'Acquired web design skills using\nHTML, Css, and JavaScript.\nLearned basic operations of the Linux\nsystem.', 'Education (IVE)\n2023-2025\nStudied various programming', 'LANGUAGES\nEnglish\nCantonese\nMandarin', 'Engineered a generative A-driven marketplace as part of a school hackathon.\nEmployed Python and JavaScript to build a platform capable of understanding and\nresponding to natural language queries. Integrated OpenAl API to classify user\ninputs (e.g. product search, recipe request) and generate tailored responses.\nImplemented a question-answering systern for recipes, enabling users to ask follow-\nup questions.', 0, '/storage/emulated/0/Android/data/com.example.circlea/files/Pictures/CVs/CV_20250201_175409.jpg', '2025-02-01 17:54:09', '2025-04-20 19:21:50', NULL, 'A'),
(2, 3, 'CONTACT\nlaukwantingabc1 23@gmal. com', 'Acquired web design skills using\nHTML, Css, and JavaScript.\nLearned basic operations of the Linux\nsystem.', 'Education (IVE)\n2023-2025\nStudied various programming香港教育大學', 'LANGUAGES\nEnglish\nCantonese\nMandarin', 'Engineered a generative A-driven marketplace as part of a school hackathon.\nEmployed Python and JavaScript to build a platform capable of understanding and\nresponding to natural language queries. Integrated OpenAl API to classify user\ninputs (e.g. product search, recipe request) and generate tailored responses.\nImplemented a question-answering systern for recipes, enabling users to ask follow-\nup questions.', 0, 'uploads/CV_3_1738432816.jpg', '2025-02-01 18:00:16', '2025-04-20 19:21:50', NULL, 'A'),
(3, 1, 'Email :\nWardiere Inc. / CEO\n123-456-7890\nhello@reallygreatsite.com', 'SKILLS\n• Project Management\n• Public Relations\n• Teamwork\nTime Management\n• Leadership\n• Effective Communication\n• Critical Thinking', 'WARDIERE UNIVERSITY\nBachelor of Business\n• GPA: 3.8 / 4.0', 'LANGUAGES\n• English (Fluent)\n• French (Fluent)\nGerman (Basics)\n• Spanish (Intermediate)\nRICHARD SANCHEZ\nMARKETING MANAGER\nPROFILE\nLorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor\nincididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam quis\nnostrud exercitation. Lorenm ipsum dolor sit amet, consectetur adipiscing elit,\nsed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad\nminim veniam quis nostrud exercitation. Ut enim ad minim veniam quis nostrud\nexercitation.\nWORK EXPERIENCE\nBorcelle Studio\nMarketing Manager & Specialist\n• Develop and execute comprehensive marketing strategies and\ncampaigns that align with the company\'s goals and objectives.\nLead, mentor, and manage a high-performing marketing team,\nfosteringa collaborative and results-driven work environment.\n• Monitor brand consistency across marketing channels and materials.\nFauget Studio\nMarketing Manager & Specialist\n• Create and manage the marketing budget, ensuring efficient\nallocation of resources and optimizing ROI.\n• Oversee market research to identify emerging trends, customer needs,\nand competitor strategies.\nStudio Shodwe\n• Monitor brand consistency across marketing channels and materials.\nMarketing Manager & Specialist\n2030 - PRESENT\nREFERENCE\n• Develop and maintain strong relationships with partners, agencies,\nand vendors to support marketing initiatives.\nEstelle Darcy\nMonitor and maintain brand consistency across all marketing\nchannels and materials.\nWardiere Inc. / CTO', '', 0, 'uploads/CV_1_1738433277.jpg', '2025-02-01 18:07:57', '2025-04-20 19:21:50', NULL, ''),
(4, 5, 'cONTACT\nsuCATIoN\n30\nLANG ULOIS\nRICHARD SANCHEZ\nARRETNG wGER\nPROFLE\nwoEK IENCE\ni', 'c', 'HKBU', 'g', 'g', 0, 'uploads/CV_5_1738821808.jpg', '2025-02-06 06:03:28', '2025-04-20 19:21:50', NULL, 'N');

-- --------------------------------------------------------

--
-- 資料表結構 `district`
--

CREATE TABLE `district` (
  `district_id` int(10) NOT NULL,
  `district_name` varchar(50) NOT NULL,
  `Latitude` double NOT NULL,
  `Longitude` double NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `district`
--

INSERT INTO `district` (`district_id`, `district_name`, `Latitude`, `Longitude`) VALUES
(1, 'Central and Western', 22.279565569909433, 114.16407358389291),
(2, 'Eastern', 22.279269868019835, 114.21894075280422),
(3, 'Southern', 22.258529793908025, 114.13689450149039),
(4, 'Wan Chai', 22.2776039116902, 114.17279331884414),
(5, 'Kowloon City', 22.323665350861198, 114.18739464031789),
(6, 'Yau Tsim Mong', 22.312784874967875, 114.1704315223154),
(7, 'Sham Shui Po', 22.328822189875652, 114.15792063882897),
(8, 'Wong Tai Sin', 22.341653432077443, 114.1934271085896),
(9, 'Kwun Tong', 22.312200636106557, 114.22616230306382),
(10, 'Tai Po', 22.451055182349638, 114.16141597200658),
(11, 'Yuen Long', 22.442937791774103, 114.02347339515119),
(12, 'Tuen Mun', 22.390305130968237, 113.9734616928739),
(13, 'North', 22.288067186347416, 114.1943171590562),
(14, 'Sai Kung', 22.383819114695196, 114.27085505912702),
(15, 'Sha Tin', 22.38504245434105, 114.19821922753633),
(16, 'Tsuen Wan', 22.368338682220394, 114.10954504714795),
(17, 'Kwai Tsing', 22.348248479593146, 114.12604400243906),
(18, 'Islands', 22.22546337446653, 114.11223391652673);

-- --------------------------------------------------------

--
-- 資料表結構 `fcm_tokens`
--

CREATE TABLE `fcm_tokens` (
  `id` int(11) NOT NULL,
  `member_id` varchar(50) NOT NULL,
  `token` text NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `fcm_tokens`
--

INSERT INTO `fcm_tokens` (`id`, `member_id`, `token`, `created_at`, `updated_at`) VALUES
(1, '5', 'd4lbRsz-S0yozkuxmi_HFx:APA91bEqq8iiHoecZXPSKWpFMxb_iOqE3zCbTDyyV1yp5l94-eDEhAfDu5VddUCXvbLo254358KZ5NaYknAKXrkKDK7Z_M90iGeczZFf8QK3s4OOF49fj4k', '2025-04-15 13:31:11', '2025-04-21 04:39:10'),
(2, '3', 'dFUDwy5hSNu0tUGRfsmI-X:APA91bHxl1y-lT3vSuBUOMG3gJ5DbXBU3WgLMQtOjoP-ukrJs-oqP4QlJs8c2oQj1SRcPHvvjUFAnUJQ2iOTZ99mWQmXilWTYl4BB_DxJMTsYSxvL-NETSI', '2025-04-15 13:31:50', '2025-04-21 08:03:24');

-- --------------------------------------------------------

--
-- 資料表結構 `first_lesson`
--

CREATE TABLE `first_lesson` (
  `first_lesson_id` int(11) NOT NULL,
  `booking_id` int(11) DEFAULT NULL,
  `next_action` varchar(20) DEFAULT NULL,
  `t_reason` varchar(255) DEFAULT NULL,
  `ps_reason` varchar(255) DEFAULT NULL,
  `ps_response` enum('completed','incomplete') DEFAULT NULL,
  `t_response` enum('completed','incomplete') DEFAULT NULL,
  `ps_response_time` timestamp NULL DEFAULT NULL,
  `t_response_time` timestamp NULL DEFAULT NULL,
  `response_deadline` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `first_lesson`
--

INSERT INTO `first_lesson` (`first_lesson_id`, `booking_id`, `next_action`, `t_reason`, `ps_reason`, `ps_response`, `t_response`, `ps_response_time`, `t_response_time`, `response_deadline`) VALUES
(31, 21, NULL, NULL, NULL, 'completed', 'completed', '2025-04-11 06:15:36', '2025-04-11 06:19:25', NULL),
(32, 22, NULL, NULL, NULL, NULL, NULL, '2025-04-22 08:48:38', '2025-04-22 09:00:00', '2025-04-25 08:12:58'),
(33, 23, NULL, NULL, NULL, 'completed', 'completed', '2025-04-22 09:48:25', '2025-04-22 09:48:45', '2025-04-25 09:48:25'),
(50, 42, NULL, NULL, NULL, 'completed', 'completed', '2025-04-24 16:24:29', '2025-04-24 16:28:44', '2025-04-27 15:22:27');

-- --------------------------------------------------------

--
-- 資料表結構 `match`
--

CREATE TABLE `match` (
  `match_id` int(10) NOT NULL,
  `tutor_id` varchar(10) NOT NULL,
  `tutor_app_id` varchar(10) NOT NULL,
  `ps_id` varchar(10) NOT NULL,
  `ps_app_id` varchar(10) NOT NULL,
  `match_mark` varchar(7) NOT NULL,
  `status` varchar(3) NOT NULL,
  `admin_approval_status` enum('pending','approved','rejected') NOT NULL DEFAULT 'pending',
  `admin_approved_by` int(11) DEFAULT NULL,
  `admin_approved_at` timestamp NULL DEFAULT NULL,
  `video_file` varchar(255) DEFAULT NULL,
  `video_mark` float DEFAULT NULL,
  `video_datetime` date DEFAULT NULL,
  `app_id` int(10) DEFAULT NULL,
  `member_id` int(10) DEFAULT NULL,
  `match_creator` varchar(2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `match`
--

INSERT INTO `match` (`match_id`, `tutor_id`, `tutor_app_id`, `ps_id`, `ps_app_id`, `match_mark`, `status`, `admin_approval_status`, `admin_approved_by`, `admin_approved_at`, `video_file`, `video_mark`, `video_datetime`, `app_id`, `member_id`, `match_creator`) VALUES
(49, '1', '81', '3', '110', '50.00%', 'WT', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PS'),
(52, '1', '77', '3', '110', '50.00%', 'WPS', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'T'),
(58, '1', '112', '3', '110', '50.00%', 'WPS', 'rejected', 2, '2025-04-11 06:25:00', NULL, NULL, NULL, NULL, NULL, 'T'),
(60, '1', '81', '3', '111', '50.00%', 'WPS', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'T'),
(68, '1', '77', '3', '123', '68%', 'WT', 'approved', 2, '2025-04-11 06:24:58', NULL, NULL, NULL, NULL, NULL, 'PS'),
(114, '5', '121', '3', '111', '71.28%', 'ope', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PS'),
(116, '5', '124', '3', '111', '71.28%', 'A', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PS');

-- --------------------------------------------------------

--
-- 資料表結構 `member`
--

CREATE TABLE `member` (
  `member_id` int(10) NOT NULL,
  `isAdmin` varchar(1) NOT NULL,
  `status` varchar(1) NOT NULL,
  `email` varchar(50) NOT NULL,
  `phone` varchar(10) NOT NULL,
  `password` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `member`
--

INSERT INTO `member` (`member_id`, `isAdmin`, `status`, `email`, `phone`, `password`, `username`) VALUES
(1, 'N', 'I', 'a654166303@gmail.com', '56440566', '$2y$10$RYOnwlsrFYcHb1bDa.e73OdvvbaWFZwDljRuR7xqL5e9qENCyV2cu', 'kenny'),
(2, 'Y', '', 'admin', '', '$2y$10$C3S7lh0vBWW.x1QVut81GemZuDMo8iv6rW9IZiBaRDb41AmgaSUHW', ''),
(3, 'Y', '', 'hh29499392@gmail.com', '56440566', '$2y$10$nx063Q2nC2QjP1uf4puUbu2hF8E5XsN2yBe67MPAJo5uuxHHbiAfe', 'tsui'),
(5, 'N', 'I', 'b654166303@gmail.com', '1234 5678', '$2y$10$PDiycF61LGXMWTQft028h.DW11g4iTHsJ/XHhS8YoCXGYeY4SCGKm', 'Mr Chan'),
(6, '0', '1', 'c654166303@gmail.com', '123456', '$2y$10$pxxvOqcMK1jGHn1Hm/F08unoApZww8syp3JOI69WfBVZ32LeMDs7m', 'hang'),
(7, 'N', 'I', 'gooddaybonmay2312@gmail.com', 'qwert', '$2y$10$N9s11lxIbhtbzvDH02XqLOyNaCCaRoZl8//1tosRRsw8X4QHeEjo2', 'Loh');

-- --------------------------------------------------------

--
-- 資料表結構 `member_cert`
--

CREATE TABLE `member_cert` (
  `member_cert_id` int(10) NOT NULL,
  `member_id` int(10) NOT NULL,
  `cert_file` varchar(50) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `status` varchar(1) NOT NULL,
  `created_time` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `member_cert`
--

INSERT INTO `member_cert` (`member_cert_id`, `member_id`, `cert_file`, `description`, `status`, `created_time`) VALUES
(48, 2, '2_20250123_102236.jpg', 'n', 'W', '2025-01-23 10:22:39'),
(49, 2, '2_20250123_102315.jpg', 'n', 'W', '2025-01-23 10:23:17'),
(50, 2, '2_20250123_102315.jpg', 'n', 'W', '2025-01-23 10:23:18'),
(51, 2, '2_20250123_102316.jpg', 'n', 'N', '2025-01-23 10:23:19'),
(52, 2, '2_20250123_102316.jpg', 'n', 'A', '2025-01-23 10:23:19'),
(53, 2, '2_20250206_132541.jpg', 'n', 'W', '2025-02-06 13:25:41'),
(54, 2, '2_20250206_152347.jpeg', 'n', 'W', '2025-02-06 15:23:47'),
(55, 2, '2_20250206_152408.jpg', 'n', 'W', '2025-02-06 15:24:07'),
(56, 1, 'CERT_1_20250210064133_1.jpeg', '', 'P', '2025-02-10 13:41:33'),
(62, 6, 'CERT_6_20250213082018_1.jpeg', '', 'P', '2025-02-13 15:20:18'),
(63, 6, 'CERT_6_20250213082018_2.jpeg', '', 'P', '2025-02-13 15:20:18'),
(68, 5, 'CERT_5_20250227085238_1.jpeg', '', 'P', '2025-02-27 15:52:38'),
(69, 5, 'CERT_5_20250227085238_2.jpeg', '', 'P', '2025-02-27 15:52:38');

-- --------------------------------------------------------

--
-- 資料表結構 `member_detail`
--

CREATE TABLE `member_detail` (
  `member_detail_id` int(10) NOT NULL,
  `member_id` int(10) NOT NULL,
  `Gender` varchar(1) NOT NULL,
  `Address` varchar(255) NOT NULL,
  `Address_District_id` varchar(50) NOT NULL,
  `DOB` date NOT NULL,
  `profile` varchar(50) DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `version` int(10) NOT NULL,
  `status` varchar(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `member_detail`
--

INSERT INTO `member_detail` (`member_detail_id`, `member_id`, `Gender`, `Address`, `Address_District_id`, `DOB`, `profile`, `description`, `version`, `status`) VALUES
(1, 1, 'M', 'tt', '1', '0000-00-00', 'D:/xampp/htdocs/FYP/images/image.jpg', 'www', 8, ''),
(2, 1, 'F', 'tt', '2', '0000-00-00', 'D:/xampp/htdocs/FYP/images/image.jpg', 'www', 8, ''),
(3, 1, 'F', 'tt', '3', '0000-00-00', 'D:/xampp/htdocs/FYP/images/image.jpg', 'www', 8, ''),
(4, 1, 'F', 'tm', '1', '0000-00-00', 'D:/xampp/htdocs/FYP/images/image.jpg', 'www', 8, ''),
(5, 1, 'F', 'TM', '12', '2000-01-02', 'D:/xampp/htdocs/FYP/images/image.jpg', 'bb', 8, ''),
(6, 1, 'F', 'abc', '12', '2025-01-01', 'D:/xampp/htdocs/FYP/images/image.jpg', 'ww', 8, ''),
(7, 1, 'F', '12', '12', '1212-12-12', 'D:/xampp/htdocs/FYP/images/image.jpg', '12', 8, ''),
(9, 1, 'M', 'tt', '1', '0000-00-00', 'D:/xampp/htdocs/FYP/images/image.jpg', 'www', 9, ''),
(10, 1, 'M', 'tt', '1', '0000-00-00', 'D:/xampp/htdocs/FYP/images/image.null', 'www', 10, ''),
(11, 1, 'M', 'tt', '1', '0000-00-00', 'D:/xampp/htdocs/FYP/images/image.jpg', 'www', 11, ''),
(12, 1, 'F', 'tt2', '12', '0000-00-00', 'D:/xampp/htdocs/FYP/images/image.jpg', 'www2', 12, ''),
(13, 1, 'F', 'tt2', '12', '2000-01-01', 'D:/xampp/htdocs/FYP/images/image.jpg', 'www2', 13, ''),
(14, 1, 'F', 'tt2', '12', '2000-01-01', 'D:/xampp/htdocs/FYP/images/image.null', 'www2', 14, ''),
(15, 1, 'F', 'tt2', '12', '2000-01-01', 'D:/xampp/htdocs/FYP/images/image.jpg', 'www2', 15, ''),
(16, 1, 'F', 'tt2', '12', '2000-01-01', 'D:/xampp/htdocs/FYP/images/icon_678bc5782f8fd7.939', 'www2', 16, ''),
(17, 1, 'F', 'tt2', '12', '2000-01-01', 'D:/xampp/htdocs/FYP/images/icon_678bc58199ccf6.202', 'www2', 17, ''),
(18, 1, 'F', 'tt2', '12', '2000-01-01', 'D:/xampp/htdocs/FYP/images/icon_18.jpg', 'www2', 18, ''),
(19, 1, 'F', 'tt2', '12', '2000-01-01', 'D:/xampp/htdocs/FYP/images/icon_19.jpg', 'www2', 19, ''),
(20, 1, 'F', '45655', '13', '2000-01-02', 'D:/xampp/htdocs/FYP/images/icon_19.jpg', 'eebr', 20, ''),
(21, 1, 'F', '45655', '13', '2000-01-02', 'D:/xampp/htdocs/FYP/images/icon_21.jpg', 'eebr', 21, ''),
(22, 1, 'F', '45655', '13', '2000-01-02', 'D:/xampp/htdocs/FYP/images/icon_22.jpg', 'eebr', 22, ''),
(23, 1, 'F', '45655', '13', '2000-01-02', 'D:/xampp/htdocs/FYP/images/icon_23.jpg', 'eebr', 23, ''),
(24, 1, 'F', '45655', '13', '2000-01-02', 'D:/xampp/htdocs/FYP/images/icon_24.jpg', 'eebr', 24, ''),
(25, 1, 'F', '45655', '13', '2000-01-02', 'D:/xampp/htdocs/FYP/images/icon_25.jpg', 'eebr', 25, ''),
(27, 1, 'F', '45655', '13', '2000-01-02', 'D:/xampp/htdocs/FYP/images/icon_27.jpg', 'eebr', 26, ''),
(28, 1, 'F', '45655', '13', '2000-01-02', 'D:/xampp/htdocs/FYP/images/icon_28.jpg', 'eebr', 27, ''),
(29, 1, 'F', '45655888', '13', '2000-01-02', 'D:/xampp/htdocs/FYP/images/icon_28.jpg', 'eebr', 28, ''),
(30, 1, 'F', '45655888', '13', '2000-01-02', NULL, 'eebr', 29, ''),
(31, 1, 'F', '45655888', '13', '2000-01-02', '/FYP/images/icon_31.jpg', 'eebr', 30, ''),
(32, 1, 'F', '45655888', '13', '2000-01-02', '/FYP/images/icon_32.jpg', 'eebr', 31, ''),
(33, 1, 'F', '45655888', '13', '2000-01-02', '/FYP/images/icon_33.jpg', 'eebr', 32, ''),
(34, 1, 'F', '45655888', '13', '2000-01-02', '/FYP/images/icon_34.jpg', 'eebr', 33, ''),
(35, 3, '', '', '', '0000-00-00', '/FYP/images/icon_35.jpg', '', 1, ''),
(36, 5, '', '', '', '0000-00-00', NULL, '', 1, ''),
(37, 3, '', '', '', '0000-00-00', '/FYP/images/icon_37.jpg', '', 2, ''),
(38, 3, '', '', '', '0000-00-00', '/FYP/images/icon_38.jpg', '', 3, ''),
(39, 3, '', '', '', '0000-00-00', '/FYP/images/icon_39.jpg', '', 4, ''),
(40, 5, '', '', '', '0000-00-00', '/FYP/images/icon_40.jpg', '', 2, ''),
(41, 5, '', '', '', '0000-00-00', '/FYP/images/icon_41.jpg', '', 3, ''),
(42, 3, '', '', '', '0000-00-00', '', '', 5, ''),
(43, 3, '', '', '', '0000-00-00', '', '', 6, '');

-- --------------------------------------------------------

--
-- 資料表結構 `notifications`
--

CREATE TABLE `notifications` (
  `id` int(11) NOT NULL,
  `member_id` varchar(50) NOT NULL,
  `title` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `type` varchar(50) NOT NULL,
  `related_id` int(11) DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `notifications`
--

INSERT INTO `notifications` (`id`, `member_id`, `title`, `message`, `type`, `related_id`, `is_read`, `created_at`) VALUES
(282, '5', '申請已通過審核', '您的申請已通過管理員審核，現在可以開始使用配對功能。', 'application_approved', 132, 0, '2025-04-21 04:36:59'),
(283, '5', '申請已通過審核', '您的申請已通過管理員審核，現在可以開始使用配對功能。', 'application_approved', 132, 0, '2025-04-21 04:37:42'),
(284, '5', '申請已通過審核', '您的申請已通過管理員審核，現在可以開始使用配對功能。', 'application_approved', 132, 0, '2025-04-21 04:39:21'),
(285, '3', '申請已通過審核', '您的申請已通過管理員審核，現在可以開始使用配對功能。', 'application_approved', 127, 0, '2025-04-21 04:40:16'),
(286, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 100, 0, '2025-04-21 04:47:33'),
(287, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 101, 0, '2025-04-21 05:02:45'),
(288, '5', '申請已通過審核', '您的申請已通過管理員審核，現在可以開始使用配對功能。', 'application_approved', 132, 0, '2025-04-21 05:03:36'),
(289, '5', '申請未通過審核', '很抱歉，您的申請未通過管理員審核。請檢查您的資料是否完整，或聯繫客服了解詳情。', 'application_rejected', 132, 0, '2025-04-21 05:03:41'),
(290, '5', '申請已通過審核', '您的申請已通過管理員審核，現在可以開始使用配對功能。', 'application_approved', 132, 0, '2025-04-21 05:03:45'),
(291, '5', 'CV已通過審核', '您的CV已通過管理員審核，現在可以開始使用配對功能。', 'cv_approved', 4, 0, '2025-04-21 05:16:34'),
(292, '5', 'CV已通過審核', '您的CV已通過管理員審核，現在可以開始使用配對功能。', 'cv_approved', 4, 0, '2025-04-21 05:20:48'),
(293, '5', 'CV未通過審核', '很抱歉，您的CV未通過管理員審核。請檢查您的CV是否完整，或聯繫客服了解詳情。', 'cv_rejected', 4, 0, '2025-04-21 05:20:52'),
(294, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 102, 0, '2025-04-21 05:41:35'),
(295, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 103, 0, '2025-04-21 05:49:12'),
(296, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 104, 0, '2025-04-21 06:19:03'),
(297, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 105, 0, '2025-04-21 06:42:29'),
(298, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 105, 0, '2025-04-21 07:30:25'),
(299, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 106, 0, '2025-04-21 07:33:00'),
(300, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 07:33:06'),
(301, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 07:49:46'),
(302, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 07:54:10'),
(303, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 07:55:09'),
(304, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:02:37'),
(305, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:03:30'),
(306, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:06:43'),
(307, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:08:00'),
(308, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:10:19'),
(309, '3', '新補習請求', 'Mr Chan 向您發送了補習請求', 'new_request', 107, 0, '2025-04-21 08:12:24'),
(310, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:13:14'),
(311, '3', '配對狀態更新', '導師已接受您的配對請求', 'match_accepted', 106, 1, '2025-04-21 08:19:56'),
(312, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:24:49'),
(313, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:31:11'),
(314, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:32:34'),
(315, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:38:40'),
(316, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:41:20'),
(317, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:45:45'),
(318, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'match_accepted', 106, 0, '2025-04-21 08:52:16'),
(319, '3', '新補習請求', 'Mr Chan 向您發送了補習請求', 'new_request', 108, 0, '2025-04-21 08:54:20'),
(320, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'new_request', 106, 0, '2025-04-21 09:08:10'),
(321, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 109, 1, '2025-04-21 09:18:59'),
(322, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'new_request', 109, 1, '2025-04-21 09:19:12'),
(323, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 110, 0, '2025-04-21 09:20:38'),
(324, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 111, 0, '2025-04-21 09:28:57'),
(325, '3', '配對狀態更新', '導師 Mr Chan 已拒絕您的配對請求', 'new_request', 111, 0, '2025-04-21 09:29:02'),
(326, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 112, 0, '2025-04-21 09:32:23'),
(327, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'new_request', 112, 0, '2025-04-21 09:32:27'),
(328, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'new_request', 104, 0, '2025-04-21 09:33:07'),
(329, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'new_request', 104, 0, '2025-04-21 09:58:23'),
(330, '3', '配對狀態更新', '導師 Mr Chan 已拒絕您的配對請求', 'new_request', 104, 0, '2025-04-21 09:58:39'),
(331, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'new_request', 104, 0, '2025-04-21 10:11:00'),
(332, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'new_request', 104, 0, '2025-04-21 10:17:09'),
(333, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 104, 1, '2025-04-21 10:35:00'),
(334, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-24 09:00 - 10:00，請查看並確認。', 'new_request', 104, 0, '2025-04-21 10:50:44'),
(335, '3', '預約請求已接受', '導師 Mr Chan 已接受您 2025-04-24 09:00 - 10:00 的預約請求。', 'booking_accepted', 104, 0, '2025-04-21 10:55:08'),
(336, '5', '學生已付款', 'tsui 已為 2025-04-24 09:00 - 10:00 的課程付款 $351.00，確認後將解鎖聯絡方式。', 'payment_submitted', 104, 0, '2025-04-21 11:08:59'),
(337, '3', '付款已確認', '您為 2025-04-24 09:00 - 10:00 課程的 351 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 104, 0, '2025-04-21 11:14:26'),
(338, '5', '學生付款已確認', 'tsui 為 2025-04-24 09:00 - 10:00 課程的 351 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 104, 0, '2025-04-21 11:14:26'),
(339, '3', '付款已確認', '您為  01:00 - 01:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 69, 0, '2025-04-21 11:14:28'),
(340, '5', '學生付款已確認', 'tsui 為  01:00 - 01:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 69, 1, '2025-04-21 11:14:28'),
(341, '5', '請回應課程狀態', '學生 tsui 已更新第一堂課狀態為 已完成，請您在3天內回應，謝謝。', 'lesson_status_request', 22, 0, '2025-04-22 08:25:06'),
(342, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 22, 0, '2025-04-22 08:30:33'),
(343, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 22, 0, '2025-04-22 08:48:38'),
(344, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 22, 0, '2025-04-22 09:00:00'),
(345, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-25 09:00 - 10:00，請查看並確認。', 'new_request', 104, 0, '2025-04-22 09:15:07'),
(346, '3', '預約請求已接受', '導師 Mr Chan 已接受您 2025-04-25 09:00 - 10:00 的預約請求。', 'booking_accepted', 104, 0, '2025-04-22 09:15:18'),
(347, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-25 09:00 - 10:00，請查看並確認。', 'new_request', 104, 0, '2025-04-22 09:21:29'),
(348, '3', '預約請求已接受', '導師 Mr Chan 已接受您 2025-04-25 09:00 - 10:00 的預約請求。', 'booking_accepted', 104, 0, '2025-04-22 09:21:58'),
(349, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-25 09:00 - 10:00，請查看並確認。', 'new_request', 104, 0, '2025-04-22 09:39:39'),
(350, '3', '預約請求已接受', '導師 Mr Chan 已接受您 2025-04-25 09:00 - 10:00 的預約請求。', 'booking_accepted', 104, 0, '2025-04-22 09:39:50'),
(351, '5', '學生已付款', 'tsui 已為 2025-04-25 09:00 - 10:00 的課程付款 $351.00，確認後將解鎖聯絡方式。', 'payment_submitted', 104, 0, '2025-04-22 09:40:26'),
(352, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 23, 0, '2025-04-22 09:48:25'),
(353, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 23, 0, '2025-04-22 09:48:45'),
(354, '3', '新補習請求', 'Mr Chan 向您發送了補習請求', 'new_request', 113, 0, '2025-04-22 10:08:42'),
(355, '5', '配對狀態更新', '導師 tsui 已接受您的配對請求', 'new_request', 113, 0, '2025-04-22 10:09:14'),
(356, '5', '導師已更新可用時間', '導師 tsui 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 113, 0, '2025-04-22 10:10:27'),
(357, '5', '導師已更新可用時間', '導師 tsui 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 113, 0, '2025-04-22 10:11:17'),
(358, '3', '新的預約時間請求', 'Mr Chan 向您請求預約時間 2025-04-25 09:00 - 10:00，請查看並確認。', 'new_request', 113, 0, '2025-04-22 10:16:01'),
(359, '5', '預約請求已接受', '導師 tsui 已接受您 2025-04-25 09:00 - 10:00 的預約請求。', 'booking_accepted', 113, 1, '2025-04-22 10:19:30'),
(360, '3', '學生已付款', 'Mr Chan 已為 2025-04-25 09:00 - 10:00 的課程付款 $305.00，確認後將解鎖聯絡方式。', 'payment_submitted', 113, 0, '2025-04-22 10:23:45'),
(361, '5', '付款已確認', '您為 2025-04-25 09:00 - 10:00 課程的 305 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 113, 0, '2025-04-22 10:24:10'),
(362, '3', '學生付款已確認', 'Mr Chan 為 2025-04-25 09:00 - 10:00 課程的 305 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 113, 0, '2025-04-22 10:24:10'),
(363, '5', '請回應課程狀態', '導師 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 24, 0, '2025-04-22 10:32:05'),
(364, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 114, 0, '2025-04-22 10:37:49'),
(365, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'new_request', 114, 0, '2025-04-22 10:50:17'),
(366, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 114, 0, '2025-04-22 10:51:12'),
(367, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-26 09:00 - 10:00，請查看並確認。', 'new_request', 114, 0, '2025-04-22 10:51:21'),
(368, '3', '預約請求已接受', '導師 Mr Chan 已接受您 2025-04-26 09:00 - 10:00 的預約請求。', 'booking_accepted', 114, 0, '2025-04-22 10:51:30'),
(369, '5', '學生已付款', 'tsui 已為 2025-04-26 09:00 - 10:00 的課程付款 $350.00，確認後將解鎖聯絡方式。', 'payment_submitted', 114, 0, '2025-04-22 10:52:30'),
(370, '3', '付款已確認', '您為 2025-04-26 09:00 - 10:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 10:52:58'),
(371, '5', '學生付款已確認', 'tsui 為 2025-04-26 09:00 - 10:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 10:52:58'),
(372, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 114, 0, '2025-04-22 12:59:28'),
(373, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-30 09:00 - 10:00，請查看並確認。', 'new_request', 114, 0, '2025-04-22 13:00:22'),
(374, '3', '預約請求已接受', '導師 Mr Chan 已接受您 2025-04-30 09:00 - 10:00 的預約請求。', 'booking_accepted', 114, 0, '2025-04-22 13:01:20'),
(375, '5', '學生已付款', 'tsui 已為 2025-04-30 09:00 - 10:00 的課程付款 $350.00，確認後將解鎖聯絡方式。', 'payment_submitted', 114, 0, '2025-04-22 13:07:28'),
(376, '3', '付款已確認', '您為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:08:44'),
(377, '5', '學生付款已確認', 'tsui 為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:08:44'),
(378, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 27, 0, '2025-04-22 13:11:48'),
(379, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 27, 0, '2025-04-22 13:11:56'),
(380, '3', '付款已確認', '您為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:20:08'),
(381, '5', '學生付款已確認', 'tsui 為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:20:08'),
(382, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 27, 0, '2025-04-22 13:22:55'),
(383, '3', '付款已確認', '您為  01:00 - 01:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:23:25'),
(384, '5', '學生付款已確認', 'tsui 為  01:00 - 01:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:23:25'),
(385, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 27, 0, '2025-04-22 13:24:59'),
(386, '3', '付款已確認', '您為  01:00 - 01:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:25:08'),
(387, '5', '學生付款已確認', 'tsui 為  01:00 - 01:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:25:08'),
(388, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 27, 0, '2025-04-22 13:26:53'),
(389, '3', '付款已確認', '您為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:27:56'),
(390, '5', '學生付款已確認', 'tsui 為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:27:56'),
(391, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 27, 0, '2025-04-22 13:28:03'),
(392, '5', '課程狀態已更新', '學生 tsui 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 27, 0, '2025-04-22 13:28:11'),
(393, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 27, 0, '2025-04-22 13:29:03'),
(394, '5', '課程狀態已更新', '學生 tsui 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 27, 0, '2025-04-22 13:29:07'),
(395, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 27, 0, '2025-04-22 13:29:37'),
(396, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 27, 0, '2025-04-22 13:29:44'),
(397, '3', '付款已確認', '您為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:31:15'),
(398, '5', '學生付款已確認', 'tsui 為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 13:31:15'),
(399, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 27, 0, '2025-04-22 13:31:22'),
(400, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 27, 0, '2025-04-22 13:31:28'),
(401, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 27, 0, '2025-04-22 13:32:52'),
(402, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 27, 0, '2025-04-22 13:39:20'),
(403, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 27, 0, '2025-04-22 14:04:29'),
(404, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 27, 0, '2025-04-22 19:13:02'),
(405, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 27, 0, '2025-04-22 19:13:08'),
(406, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 27, 0, '2025-04-22 19:14:28'),
(407, '3', '付款已確認', '您為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 19:14:39'),
(408, '5', '學生付款已確認', 'tsui 為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 114, 0, '2025-04-22 19:14:39'),
(409, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 27, 0, '2025-04-22 19:14:54'),
(410, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 27, 0, '2025-04-22 19:26:24'),
(411, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 27, 0, '2025-04-22 19:45:04'),
(412, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 27, 0, '2025-04-22 19:48:47'),
(413, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 27, 0, '2025-04-22 19:51:38'),
(414, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 27, 0, '2025-04-22 19:51:42'),
(415, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 114, 0, '2025-04-22 20:10:53'),
(416, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 27, 0, '2025-04-22 20:11:37'),
(417, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 27, 0, '2025-04-22 20:11:43'),
(418, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-24 09:00 - 10:00，請查看並確認。', 'new_request', 114, 0, '2025-04-22 20:12:15'),
(419, '3', '預約請求已接受', '導師 Mr Chan 已接受您 2025-04-24 09:00 - 10:00 的預約請求。', 'booking_accepted', 114, 0, '2025-04-22 20:12:27'),
(420, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 28, 0, '2025-04-22 20:12:43'),
(421, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:12:54'),
(422, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:12:56'),
(423, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:13:07'),
(424, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:18:59'),
(425, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:19:00'),
(426, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:19:27'),
(427, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:19:28'),
(428, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:20:07'),
(429, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:20:08'),
(430, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:22:08'),
(431, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 28, 0, '2025-04-22 20:22:10'),
(432, '5', '課程取消通知', '學生 tsui 已取消課程並選擇尋找其他導師。', 'booking_cancelled', 28, 0, '2025-04-22 20:22:10'),
(433, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 115, 0, '2025-04-23 00:06:28'),
(434, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'new_request', 115, 0, '2025-04-23 00:06:36'),
(435, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 115, 0, '2025-04-23 00:07:21'),
(436, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-30 09:00 - 10:00，請查看並確認。', 'new_request', 115, 0, '2025-04-23 00:07:54'),
(437, '3', '預約請求已接受', '導師 Mr Chan 已接受您 2025-04-30 09:00 - 10:00 的預約請求。', 'booking_accepted', 115, 0, '2025-04-23 00:08:07'),
(438, '5', '學生已付款', 'tsui 已為 2025-04-30 09:00 - 10:00 的課程付款 $350.00，確認後將解鎖聯絡方式。', 'payment_submitted', 115, 0, '2025-04-23 00:28:01'),
(439, '3', '付款已確認', '您為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 115, 0, '2025-04-23 00:29:56'),
(440, '5', '學生付款已確認', 'tsui 為 2025-04-30 09:00 - 10:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 115, 0, '2025-04-23 00:29:56'),
(441, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 29, 0, '2025-04-23 00:30:23'),
(442, '5', '課程狀態已更新', '學生 tsui 已回應第一堂課狀態，請查看詳情。', 'lesson_conflict', 29, 0, '2025-04-23 00:30:30'),
(443, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_conflict', 29, 0, '2025-04-23 00:52:51'),
(444, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_conflict', 29, 0, '2025-04-23 01:03:35'),
(445, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 29, 0, '2025-04-23 01:15:06'),
(446, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 115, 0, '2025-04-23 02:36:32'),
(447, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 115, 0, '2025-04-23 02:37:38'),
(448, '5', '新補習請求', 'tsui 向您發送了補習請求', 'new_request', 116, 0, '2025-04-23 02:56:36'),
(449, '3', '配對狀態更新', '導師 Mr Chan 已接受您的配對請求', 'new_request', 116, 0, '2025-04-23 02:56:43'),
(450, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 116, 0, '2025-04-23 02:57:13'),
(451, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-23 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 03:07:30'),
(452, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-26 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 03:10:06'),
(453, '5', '學生已付款', 'tsui 已為 2025-04-26 09:00 - 10:00 的課程付款 $350.00，確認後將解鎖聯絡方式。', 'payment_submitted', 116, 0, '2025-04-23 07:43:30'),
(454, '3', '付款已確認', '您為 2025-04-26 09:00 - 10:00 課程的 350 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 07:44:15'),
(455, '5', '學生付款已確認', 'tsui 為 2025-04-26 09:00 - 10:00 課程的 350 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 07:44:15'),
(456, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 33, 0, '2025-04-23 07:48:07'),
(457, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_conflict', 33, 0, '2025-04-23 07:48:15'),
(458, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 33, 0, '2025-04-23 08:36:22'),
(459, '5', '課程狀態已更新', '學生 tsui 已回應第一堂課狀態，請查看詳情。', 'lesson_conflict', 33, 0, '2025-04-23 08:36:30'),
(460, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 33, 0, '2025-04-23 08:53:30'),
(461, '5', '課程狀態已更新', '學生 tsui 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 33, 0, '2025-04-23 08:53:35'),
(462, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 116, 0, '2025-04-23 13:17:58'),
(463, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-25 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 13:18:06'),
(464, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 34, 0, '2025-04-23 13:20:01'),
(465, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 34, 0, '2025-04-23 13:20:05'),
(466, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 116, 0, '2025-04-23 14:09:32'),
(467, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-25 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 14:09:38'),
(468, '5', '學生已付款', 'tsui 已為 2025-04-25 09:00 - 10:00 的課程付款 $100.00，確認後將解鎖聯絡方式。', 'payment_submitted', 116, 0, '2025-04-23 14:09:57'),
(469, '3', '付款已確認', '您為 2025-04-25 09:00 - 10:00 課程的 100 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 14:10:17'),
(470, '5', '學生付款已確認', 'tsui 為 2025-04-25 09:00 - 10:00 課程的 100 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 14:10:17'),
(471, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 116, 0, '2025-04-23 14:55:26'),
(472, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-30 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 14:55:33'),
(473, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 116, 0, '2025-04-23 15:05:25'),
(474, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-29 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 15:09:07'),
(475, '5', '學生已付款', 'tsui 已為 2025-04-29 09:00 - 10:00 的課程付款 $100.00，確認後將解鎖聯絡方式。', 'payment_submitted', 116, 0, '2025-04-23 15:14:02'),
(476, '3', '付款已確認', '您為 2025-04-29 09:00 - 10:00 課程的 100 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 15:14:08'),
(477, '5', '學生付款已確認', 'tsui 為 2025-04-29 09:00 - 10:00 課程的 100 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 15:14:08'),
(478, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 37, 0, '2025-04-23 15:48:46'),
(479, '5', '課程狀態已更新', '學生 tsui 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 37, 0, '2025-04-23 15:48:51'),
(480, '5', '新的預約時間請求', 'tsui 向您請求預約時間 2025-04-29 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 17:59:23'),
(481, '5', '新的預約時間已被選擇', 'tsui 向選擇預約時間 2025-04-29 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 18:12:09'),
(482, '5', '學生已付款', 'tsui 已為 2025-04-29 09:00 - 10:00 的課程付款 $100.00，確認後將解鎖聯絡方式。', 'payment_submitted', 116, 0, '2025-04-23 18:12:45'),
(483, '3', '付款已確認', '您為 2025-04-29 09:00 - 10:00 課程的 100 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 18:12:58'),
(484, '5', '學生付款已確認', 'tsui 為 2025-04-29 09:00 - 10:00 課程的 100 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 18:12:58'),
(485, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 37, 0, '2025-04-23 18:13:33'),
(486, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 37, 0, '2025-04-23 18:13:51'),
(487, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 116, 0, '2025-04-23 18:19:39'),
(488, '5', '新的預約時間已被選擇', 'tsui 向選擇預約時間 2025-04-26 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 18:19:47'),
(489, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 38, 0, '2025-04-23 18:19:59'),
(490, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 38, 0, '2025-04-23 18:20:20'),
(491, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 38, 0, '2025-04-23 18:20:30'),
(492, '5', '新的預約時間已被選擇', 'tsui 向選擇預約時間 2025-04-26 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 18:20:45'),
(493, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 38, 0, '2025-04-23 18:21:18'),
(494, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 38, 0, '2025-04-23 18:21:39'),
(495, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 116, 0, '2025-04-23 18:24:49'),
(496, '5', '新的預約時間已被選擇', 'tsui 向選擇預約時間 2025-04-26 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 18:24:58'),
(497, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 116, 0, '2025-04-23 18:58:55'),
(498, '5', '新的預約時間已被選擇', 'tsui 向選擇預約時間 2025-04-30 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 18:59:04'),
(499, '5', '學生已付款', 'tsui 已為 2025-04-30 09:00 - 10:00 的課程付款 $100.00，確認後將解鎖聯絡方式。', 'payment_submitted', 116, 0, '2025-04-23 19:00:04'),
(500, '3', '付款已確認', '您為 2025-04-30 09:00 - 10:00 課程的 100 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 19:00:55'),
(501, '5', '學生付款已確認', 'tsui 為 2025-04-30 09:00 - 10:00 課程的 100 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 19:00:55'),
(502, '3', '付款已確認', '您為 2025-04-30 09:00 - 10:00 課程的 100 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 19:01:02'),
(503, '5', '學生付款已確認', 'tsui 為 2025-04-30 09:00 - 10:00 課程的 100 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 19:01:02'),
(504, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 40, 0, '2025-04-23 19:01:22'),
(505, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 40, 0, '2025-04-23 19:13:53'),
(506, '5', '新的預約時間已被選擇', 'tsui 向選擇預約時間 0000-00-00 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 19:15:10'),
(507, '5', '學生已付款', 'tsui 已為 0000-00-00 09:00 - 10:00 的課程付款 $100.00，確認後將解鎖聯絡方式。', 'payment_submitted', 116, 0, '2025-04-23 19:15:28'),
(508, '3', '付款已確認', '您為 0000-00-00 09:00 - 10:00 課程的 100 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 19:15:41'),
(509, '5', '學生付款已確認', 'tsui 為 0000-00-00 09:00 - 10:00 課程的 100 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 19:15:41'),
(510, '3', '付款已確認', '您為 0000-00-00 09:00 - 10:00 課程的 100 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 19:17:37'),
(511, '5', '學生付款已確認', 'tsui 為 0000-00-00 09:00 - 10:00 課程的 100 付款已確認。學生現在可以查看您的聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-23 19:17:37'),
(512, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 41, 0, '2025-04-23 19:18:25'),
(513, '5', '課程狀態已更新', '學生 tsui 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 41, 0, '2025-04-23 19:18:34'),
(514, '3', '導師已更新可用時間', '導師 Mr Chan 已經更新了可預約的時間段，請前往查看並選擇合適的時間。', 'new_request', 116, 0, '2025-04-23 19:20:15'),
(515, '5', '新的預約時間已被選擇', 'tsui 向選擇預約時間 2025-04-30 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 19:20:37'),
(516, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 42, 0, '2025-04-23 19:21:19'),
(517, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 42, 0, '2025-04-23 19:21:30'),
(518, '5', '課程狀態已更新', '學生 tsui.已回應第一堂課狀態，請查看詳情。', 'lesson_incomplete', 42, 0, '2025-04-23 19:21:39'),
(519, '5', '新的預約時間已被選擇', 'tsui 向選擇預約時間 2025-04-30 09:00 - 10:00，請查看並確認。', 'new_request', 116, 0, '2025-04-23 19:22:03'),
(520, '3', '請回應課程狀態', '導師 Mr Chan 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 42, 0, '2025-04-23 19:22:24'),
(521, '5', '課程狀態已更新', '學生 tsui 已回應第一堂課狀態，請查看詳情。', 'lesson_conflict', 42, 0, '2025-04-23 19:22:34'),
(522, '5', '學生已付款', 'tsui 已為 2025-04-30 09:00 - 10:01 的課程付款 $475.00，確認後將解鎖聯絡方式。', 'payment_submitted', 116, 0, '2025-04-24 14:27:09'),
(523, '3', '付款已確認', '您為 2025-04-30 09:00 - 10:01 課程的 475 付款已確認。您現在可以查看導師聯絡資訊。', 'payment_confirmed', 116, 0, '2025-04-24 14:27:43'),
(524, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 42, 0, '2025-04-24 15:22:27'),
(525, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 42, 0, '2025-04-24 15:27:38'),
(526, '5', '請回應課程狀態', '學生 tsui 已回應第一堂課狀態，請您在3天內回應，謝謝。', 'lesson_status_request', 42, 0, '2025-04-24 16:24:29'),
(527, '3', '課程狀態已更新', '導師 Mr Chan 已回應第一堂課狀態，請查看詳情。', 'lesson_completed', 42, 0, '2025-04-24 16:28:44');

-- --------------------------------------------------------

--
-- 資料表結構 `payment`
--

CREATE TABLE `payment` (
  `payment_id` int(10) NOT NULL,
  `match_id` int(10) NOT NULL,
  `student_id` int(10) NOT NULL,
  `amount` int(10) NOT NULL,
  `status` enum('not_submitted','pending','confirmed','rejected') NOT NULL DEFAULT 'not_submitted',
  `receipt_path` longtext DEFAULT NULL,
  `submitted_at` timestamp NULL DEFAULT NULL,
  `verified_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `payment`
--

INSERT INTO `payment` (`payment_id`, `match_id`, `student_id`, `amount`, `status`, `receipt_path`, `submitted_at`, `verified_at`) VALUES
(36, 116, 3, 475, 'confirmed', 'uploads/receipts/RECEIPT_680a4a3d68399_RECEIPT_1745504827126.jpeg', '2025-04-24 14:27:09', '2025-04-24 14:27:43');

-- --------------------------------------------------------

--
-- 資料表結構 `subject`
--

CREATE TABLE `subject` (
  `subject_id` int(10) NOT NULL,
  `subject_name` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `subject`
--

INSERT INTO `subject` (`subject_id`, `subject_name`) VALUES
(1, 'Chinese Language'),
(2, 'English Language'),
(3, 'Mathematics'),
(4, 'Citizenship and Social Development'),
(5, 'Biology'),
(6, 'Chemistry'),
(7, 'Physics'),
(8, 'Chinese History'),
(9, 'History'),
(10, 'Economics'),
(11, 'Business, Accounting and Financial Studies'),
(12, 'Geography'),
(13, 'Tourism and Hospitality Studies'),
(14, 'Chinese Literature'),
(15, 'Literature in English'),
(16, 'Ethics and Religious Studies'),
(17, 'Design and Applied Technology'),
(18, 'Health Management and Social Care'),
(19, 'Information and Communication Technology'),
(20, 'Technology and Living'),
(21, 'Music'),
(22, 'Visual Arts'),
(23, 'Physical Education'),
(24, 'General Studies');

-- --------------------------------------------------------

--
-- 資料表結構 `tutor_rating`
--

CREATE TABLE `tutor_rating` (
  `rate_id` int(11) NOT NULL,
  `member_id` int(11) NOT NULL,
  `application_id` int(11) NOT NULL,
  `role` enum('tutor','parent') NOT NULL,
  `rate_times` datetime NOT NULL,
  `rate_score` decimal(2,1) NOT NULL,
  `comment` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `tutor_rating`
--

INSERT INTO `tutor_rating` (`rate_id`, `member_id`, `application_id`, `role`, `rate_times`, `rate_score`, `comment`) VALUES
(1, 5, 109, 'tutor', '2025-03-15 14:30:00', '4.5', '学生学习态度认真，很有上进心'),
(2, 3, 109, 'parent', '2025-03-15 15:00:00', '5.0', '老师教学方法很好，孩子进步明显'),
(3, 3, 80, 'tutor', '2025-03-16 09:15:00', '4.0', '地理科目学习进展良好，继续保持'),
(4, 1, 80, 'parent', '2025-03-16 10:30:00', '3.5', '教学质量不错，但有时候会迟到'),
(5, 1, 81, 'tutor', '2025-03-16 13:20:00', '5.0', '对学生的学习进度非常满意'),
(6, 3, 81, 'parent', '2025-03-16 14:00:00', '4.8', '老师非常专业，对孩子很有耐心'),
(7, 6, 120, 'parent', '2025-03-17 11:00:00', '4.2', '教学风格很好，讲解清晰明了'),
(8, 5, 120, 'tutor', '2025-03-17 11:30:00', '4.5', '企会财科目进步显著，理解能力提高'),
(9, 3, 123, 'parent', '2025-03-18 09:00:00', '4.7', '对教学质量非常满意，孩子很喜欢上课'),
(10, 5, 123, 'tutor', '2025-03-18 10:00:00', '4.3', '英文科目表现出色，具有很大的发展潜力'),
(11, 3, 124, 'parent', '2025-04-25 00:28:17', '3.0', 'www');

--
-- 已傾印資料表的索引
--

--
-- 資料表索引 `ads`
--
ALTER TABLE `ads`
  ADD PRIMARY KEY (`ad_id`);

--
-- 資料表索引 `application`
--
ALTER TABLE `application`
  ADD PRIMARY KEY (`app_id`),
  ADD KEY `idx_class_level_id` (`class_level_id`),
  ADD KEY `fk_member_id` (`member_id`);

--
-- 資料表索引 `application_date`
--
ALTER TABLE `application_date`
  ADD PRIMARY KEY (`app_Date_id`),
  ADD KEY `idx_app_id_date` (`app_id`);

--
-- 資料表索引 `application_district`
--
ALTER TABLE `application_district`
  ADD PRIMARY KEY (`application_district_id`),
  ADD KEY `application_id` (`app_id`),
  ADD KEY `district_id` (`district_id`);

--
-- 資料表索引 `application_subject`
--
ALTER TABLE `application_subject`
  ADD PRIMARY KEY (`application_subject_id`),
  ADD KEY `idx_application_id` (`app_id`);

--
-- 資料表索引 `booking`
--
ALTER TABLE `booking`
  ADD PRIMARY KEY (`booking_id`),
  ADD KEY `tutor_id` (`tutor_id`),
  ADD KEY `student_id` (`student_id`);

--
-- 資料表索引 `class_level`
--
ALTER TABLE `class_level`
  ADD PRIMARY KEY (`class_level_id`);

--
-- 資料表索引 `cv_data`
--
ALTER TABLE `cv_data`
  ADD PRIMARY KEY (`cv_id`),
  ADD KEY `member_id` (`member_id`);

--
-- 資料表索引 `district`
--
ALTER TABLE `district`
  ADD PRIMARY KEY (`district_id`);

--
-- 資料表索引 `fcm_tokens`
--
ALTER TABLE `fcm_tokens`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_member_id` (`member_id`);

--
-- 資料表索引 `first_lesson`
--
ALTER TABLE `first_lesson`
  ADD PRIMARY KEY (`first_lesson_id`),
  ADD UNIQUE KEY `booking_id` (`booking_id`);

--
-- 資料表索引 `match`
--
ALTER TABLE `match`
  ADD PRIMARY KEY (`match_id`),
  ADD KEY `idx_applicationapp_id` (`app_id`),
  ADD KEY `idx_membermember_id` (`member_id`);

--
-- 資料表索引 `member`
--
ALTER TABLE `member`
  ADD PRIMARY KEY (`member_id`);

--
-- 資料表索引 `member_cert`
--
ALTER TABLE `member_cert`
  ADD PRIMARY KEY (`member_cert_id`),
  ADD KEY `idx_member_id` (`member_id`);

--
-- 資料表索引 `member_detail`
--
ALTER TABLE `member_detail`
  ADD PRIMARY KEY (`member_detail_id`),
  ADD KEY `idx_member_id_detail` (`member_id`);

--
-- 資料表索引 `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_member_id` (`member_id`),
  ADD KEY `idx_is_read` (`is_read`);

--
-- 資料表索引 `payment`
--
ALTER TABLE `payment`
  ADD PRIMARY KEY (`payment_id`),
  ADD KEY `idx_match_id` (`match_id`),
  ADD KEY `idx_student_id` (`student_id`);

--
-- 資料表索引 `subject`
--
ALTER TABLE `subject`
  ADD PRIMARY KEY (`subject_id`);

--
-- 資料表索引 `tutor_rating`
--
ALTER TABLE `tutor_rating`
  ADD PRIMARY KEY (`rate_id`),
  ADD KEY `member_id` (`member_id`),
  ADD KEY `application_id` (`application_id`);

--
-- 在傾印的資料表使用自動遞增(AUTO_INCREMENT)
--

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `ads`
--
ALTER TABLE `ads`
  MODIFY `ad_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `application`
--
ALTER TABLE `application`
  MODIFY `app_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=133;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `application_date`
--
ALTER TABLE `application_date`
  MODIFY `app_Date_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=36;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `application_district`
--
ALTER TABLE `application_district`
  MODIFY `application_district_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=118;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `application_subject`
--
ALTER TABLE `application_subject`
  MODIFY `application_subject_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=148;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `booking`
--
ALTER TABLE `booking`
  MODIFY `booking_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=44;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `class_level`
--
ALTER TABLE `class_level`
  MODIFY `class_level_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `cv_data`
--
ALTER TABLE `cv_data`
  MODIFY `cv_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `district`
--
ALTER TABLE `district`
  MODIFY `district_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `fcm_tokens`
--
ALTER TABLE `fcm_tokens`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `first_lesson`
--
ALTER TABLE `first_lesson`
  MODIFY `first_lesson_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=51;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `match`
--
ALTER TABLE `match`
  MODIFY `match_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=117;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `member`
--
ALTER TABLE `member`
  MODIFY `member_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `member_cert`
--
ALTER TABLE `member_cert`
  MODIFY `member_cert_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=70;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `member_detail`
--
ALTER TABLE `member_detail`
  MODIFY `member_detail_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=44;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=528;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `payment`
--
ALTER TABLE `payment`
  MODIFY `payment_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=37;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `subject`
--
ALTER TABLE `subject`
  MODIFY `subject_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=25;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `tutor_rating`
--
ALTER TABLE `tutor_rating`
  MODIFY `rate_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- 已傾印資料表的限制式
--

--
-- 資料表的限制式 `application_date`
--
ALTER TABLE `application_date`
  ADD CONSTRAINT `fk_application_date_application` FOREIGN KEY (`app_id`) REFERENCES `application` (`app_id`);

--
-- 資料表的限制式 `application_district`
--
ALTER TABLE `application_district`
  ADD CONSTRAINT `application_district_ibfk_1` FOREIGN KEY (`app_id`) REFERENCES `application` (`app_id`),
  ADD CONSTRAINT `application_district_ibfk_2` FOREIGN KEY (`district_id`) REFERENCES `district` (`district_id`);

--
-- 資料表的限制式 `booking`
--
ALTER TABLE `booking`
  ADD CONSTRAINT `booking_ibfk_1` FOREIGN KEY (`tutor_id`) REFERENCES `member` (`member_id`),
  ADD CONSTRAINT `booking_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `member` (`member_id`);

--
-- 資料表的限制式 `cv_data`
--
ALTER TABLE `cv_data`
  ADD CONSTRAINT `cv_data_ibfk_1` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE;

--
-- 資料表的限制式 `first_lesson`
--
ALTER TABLE `first_lesson`
  ADD CONSTRAINT `first_lesson_ibfk_1` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`booking_id`);

--
-- 資料表的限制式 `payment`
--
ALTER TABLE `payment`
  ADD CONSTRAINT `fk_payment_match` FOREIGN KEY (`match_id`) REFERENCES `match` (`match_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_payment_student` FOREIGN KEY (`student_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- 資料表的限制式 `tutor_rating`
--
ALTER TABLE `tutor_rating`
  ADD CONSTRAINT `tutor_rating_ibfk_1` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  ADD CONSTRAINT `tutor_rating_ibfk_2` FOREIGN KEY (`application_id`) REFERENCES `application` (`app_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
