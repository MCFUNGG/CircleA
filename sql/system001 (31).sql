-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- 主機： 127.0.0.1
-- 產生時間： 2025-04-14 03:56:44
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
(109, 5, 'T', 9, '', 250, NULL, 'A'),
(110, 3, 'PS', 9, '', 251, NULL, 'A'),
(111, 3, 'PS', 15, '', 250, NULL, 'A'),
(112, 1, 'PS', 11, '', 250, NULL, 'P'),
(113, 1, 'PS', 5, '', 320, NULL, 'P'),
(115, 1, 'PS', 1, '', 250, 1, 'R'),
(120, 6, 'PS', 15, '', 200, 1, 'A'),
(121, 5, 'T', 1, '', 200, NULL, 'A'),
(122, 5, 'T', 15, '', 200, NULL, 'A'),
(123, 3, 'PS', 9, '', 250, 1, 'A'),
(124, 5, 'T', 9, '', 200, NULL, 'A'),
(125, 3, 'PS', 15, '', 200, 1, 'A'),
(126, 5, 'PS', 1, '', 200, 1, 'P'),
(127, 3, 'PS', 1, '', 410, 2, 'P');

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
(25, 122, '1400-1600', '', '', '', '', '', ''),
(26, 123, '1200-1600', '', '', '', '', '', ''),
(27, 124, '1400-1600', '', '', '', '', '', ''),
(28, 125, '1400-1530', '', '', '', '', '', ''),
(29, 126, '1400 - 1600', '', '', '', '', '', ''),
(30, 127, '1200-1600', '', '', '', '', '', '');

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
(47, 109, 4),
(48, 109, 5),
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
(71, 122, 2),
(72, 122, 8),
(73, 123, 8),
(74, 123, 9),
(75, 124, 10),
(76, 124, 11),
(77, 125, 10),
(78, 125, 11),
(79, 126, 1),
(80, 126, 5),
(81, 126, 9),
(82, 126, 12),
(83, 126, 14),
(84, 127, 6);

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
(78, 109, '12'),
(79, 109, '11'),
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
(105, 122, '1'),
(106, 122, '2'),
(107, 123, '1'),
(108, 123, '2'),
(109, 124, '1'),
(110, 124, '3'),
(111, 125, '1'),
(112, 125, '2'),
(113, 126, '1'),
(114, 126, '3'),
(115, 126, '5'),
(116, 127, '1'),
(117, 127, '3');

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
  `status` enum('available','pending','confirmed','expired') DEFAULT 'available',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `next_action` varchar(50) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `booking`
--

INSERT INTO `booking` (`booking_id`, `match_id`, `tutor_id`, `student_id`, `date`, `start_time`, `end_time`, `status`, `created_at`, `updated_at`, `next_action`, `reason`) VALUES
(9, 69, 5, 3, '2025-03-31', '09:00:00', '10:08:00', '', '2025-03-16 07:21:13', '2025-03-19 13:38:34', 'rebook', 'Emergency situation'),
(18, 69, 5, NULL, '2025-04-17', '09:00:00', '10:00:00', 'available', '2025-04-11 03:46:39', '2025-04-11 04:42:48', NULL, NULL),
(21, 69, 5, 3, '2025-04-23', '09:00:00', '10:00:00', '', '2025-04-11 06:10:04', '2025-04-11 06:19:25', NULL, NULL);

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
  `Score` int(3) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `cv_data`
--

INSERT INTO `cv_data` (`cv_id`, `member_id`, `contact`, `skills`, `education`, `language`, `other`, `cv_score`, `cv_path`, `created_at`, `Score`) VALUES
(1, 3, 'CONTACT\nlaukwantingabc1 23@gmal. com', 'Acquired web design skills using\nHTML, Css, and JavaScript.\nLearned basic operations of the Linux\nsystem.', 'Education (IVE)\n2023-2025\nStudied various programming', 'LANGUAGES\nEnglish\nCantonese\nMandarin', 'Engineered a generative A-driven marketplace as part of a school hackathon.\nEmployed Python and JavaScript to build a platform capable of understanding and\nresponding to natural language queries. Integrated OpenAl API to classify user\ninputs (e.g. product search, recipe request) and generate tailored responses.\nImplemented a question-answering systern for recipes, enabling users to ask follow-\nup questions.', 0, '/storage/emulated/0/Android/data/com.example.circlea/files/Pictures/CVs/CV_20250201_175409.jpg', '2025-02-01 17:54:09', NULL),
(2, 3, 'CONTACT\nlaukwantingabc1 23@gmal. com', 'Acquired web design skills using\nHTML, Css, and JavaScript.\nLearned basic operations of the Linux\nsystem.', 'Education (IVE)\n2023-2025\nStudied various programming', 'LANGUAGES\nEnglish\nCantonese\nMandarin', 'Engineered a generative A-driven marketplace as part of a school hackathon.\nEmployed Python and JavaScript to build a platform capable of understanding and\nresponding to natural language queries. Integrated OpenAl API to classify user\ninputs (e.g. product search, recipe request) and generate tailored responses.\nImplemented a question-answering systern for recipes, enabling users to ask follow-\nup questions.', 0, 'uploads/CV_3_1738432816.jpg', '2025-02-01 18:00:16', NULL),
(3, 1, 'Email :\nWardiere Inc. / CEO\n123-456-7890\nhello@reallygreatsite.com', 'SKILLS\n• Project Management\n• Public Relations\n• Teamwork\nTime Management\n• Leadership\n• Effective Communication\n• Critical Thinking', 'WARDIERE UNIVERSITY\nBachelor of Business\n• GPA: 3.8 / 4.0', 'LANGUAGES\n• English (Fluent)\n• French (Fluent)\nGerman (Basics)\n• Spanish (Intermediate)\nRICHARD SANCHEZ\nMARKETING MANAGER\nPROFILE\nLorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor\nincididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam quis\nnostrud exercitation. Lorenm ipsum dolor sit amet, consectetur adipiscing elit,\nsed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad\nminim veniam quis nostrud exercitation. Ut enim ad minim veniam quis nostrud\nexercitation.\nWORK EXPERIENCE\nBorcelle Studio\nMarketing Manager & Specialist\n• Develop and execute comprehensive marketing strategies and\ncampaigns that align with the company\'s goals and objectives.\nLead, mentor, and manage a high-performing marketing team,\nfosteringa collaborative and results-driven work environment.\n• Monitor brand consistency across marketing channels and materials.\nFauget Studio\nMarketing Manager & Specialist\n• Create and manage the marketing budget, ensuring efficient\nallocation of resources and optimizing ROI.\n• Oversee market research to identify emerging trends, customer needs,\nand competitor strategies.\nStudio Shodwe\n• Monitor brand consistency across marketing channels and materials.\nMarketing Manager & Specialist\n2030 - PRESENT\nREFERENCE\n• Develop and maintain strong relationships with partners, agencies,\nand vendors to support marketing initiatives.\nEstelle Darcy\nMonitor and maintain brand consistency across all marketing\nchannels and materials.\nWardiere Inc. / CTO', '', 0, 'uploads/CV_1_1738433277.jpg', '2025-02-01 18:07:57', NULL),
(4, 5, 'cONTACT\nsuCATIoN\n30\nLANG ULOIS\nRICHARD SANCHEZ\nARRETNG wGER\nPROFLE\nwoEK IENCE\ni', 'c', 'g', 'g', 'g', 0, 'uploads/CV_5_1738821808.jpg', '2025-02-06 06:03:28', NULL);

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
(31, 21, NULL, NULL, NULL, 'completed', 'completed', '2025-04-11 06:15:36', '2025-04-11 06:19:25', NULL);

-- --------------------------------------------------------

--
-- 資料表結構 `first_lesson_responses`
--

CREATE TABLE `first_lesson_responses` (
  `response_id` int(11) NOT NULL,
  `booking_id` int(11) NOT NULL,
  `user_type` enum('student','tutor') NOT NULL,
  `status` enum('completed','incomplete') NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `response_time` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
(48, '3', '80', '1', '113', '50.00%', 'WT', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PS'),
(49, '1', '81', '3', '110', '50.00%', 'WT', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PS'),
(52, '1', '77', '3', '110', '50.00%', 'WPS', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'T'),
(55, '5', '109', '1', '108', '50.00%', 'WPS', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'T'),
(56, '5', '114', '3', '110', '50.00%', 'WPS', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'T'),
(58, '1', '112', '3', '110', '50.00%', 'WPS', 'rejected', 2, '2025-04-11 06:25:00', NULL, NULL, NULL, NULL, NULL, 'T'),
(59, '5', '', '1', '98', '50.00%', 'WPS', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'T'),
(60, '1', '81', '3', '111', '50.00%', 'WPS', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'T'),
(65, '5', '109', '6', '120', '68%', 'P', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PS'),
(66, '5', '109', '1', '98', '68%', 'WPS', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'T'),
(67, '5', '122', '1', '108', '68%', 'WPS', 'rejected', 2, '2025-04-11 06:24:54', NULL, NULL, NULL, NULL, NULL, 'T'),
(68, '1', '77', '3', '123', '68%', 'WT', 'approved', 2, '2025-04-11 06:24:58', NULL, NULL, NULL, NULL, NULL, 'PS'),
(69, '5', '109', '3', '123', '68%', 'A', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PS'),
(70, '1', '77', '5', '', '68%', 'WT', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PS'),
(71, '5', '122', '3', '125', '86%', 'P', 'pending', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'PS'),
(72, '1', '77', '5', '126', '72%', 'WT', 'approved', 2, '2025-04-11 06:24:53', NULL, NULL, NULL, NULL, NULL, 'PS');

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
-- 資料表結構 `member_cv`
--

CREATE TABLE `member_cv` (
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
  `last_modified` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `status` varchar(1) NOT NULL DEFAULT 'P'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 傾印資料表的資料 `member_cv`
--

INSERT INTO `member_cv` (`cv_id`, `member_id`, `contact`, `skills`, `education`, `language`, `other`, `cv_score`, `cv_path`, `created_at`, `last_modified`, `status`) VALUES
(1, 1, 'w', 'w', 'w', 'w', 'CERTIICATE\nOF ACHIEVEMENT\nTHIS CERTIFICATE IS PRESEND TO:\nRufs Stewatt\nIsabel Mercado\nHopefully this achievement will be the first step towards bigger success.\nkeep trying and give your best\nConnor Hamilton', 0, '', '2025-02-10 05:41:33', '2025-02-11 03:19:04', 'N'),
(2, 5, 'CONTACT\nlaukwantingabc123@gmail com', 'Acquired web design skills using\nHTML, CSs, and JavaScript.\n•Learned basic operations of the Linux\nsystem,', 'Education (IVE)\nKWAN TING LAU\n2023-2025\nStudied various programming', 'LANGUAGES\nEnglish\nCantonese\nMandarin', 'Engineered a generative A-driven marketplace as part of a school hackathon.\nEmployed Python and JavaScript to build a platform capable of understanding and\nresponding to natural language queries. Integrated OpenAl API to classify user\ninputs (e.g, product search, recipe request) and generate tailored responses.\nImplemented a question-answering system for recipes, enabling users to ask follow-\nup questions.', 0, '', '2025-02-11 03:15:18', '2025-02-27 07:53:41', 'A'),
(3, 6, 'CONTACT\nlaukwantingabc123@gmal. com', 'Acquired web design skills using\nHTML, CSs, and JavaScript.\nLearned basic operations of the Linux\nsystem,', 'Education (IVE)\n2023-2025\nKWAN TING LAU\nStudied various programming\nJanguages, Including Java, C#, PHP,\nand Python.', 'LANGUAGES\nEnglish\nCantonese\nMandarin', 'SCHOOL HACKATHON\nAl-Powered E-commerce Platform\nEngineered a generative A-driven marketplace as part of a school hackatihon.\nEmployed Python and JavaScript to build a platform capable of understanding and\nresponding to natural language queries. Integrated OpenAl API to classify user\ninputs (e.g. product search, recipe request) and generate tailored responses.\nImplemented a question-answering systern for recipes, enabling users to ask follow-\nup questions', 0, '', '2025-02-12 11:11:18', '2025-02-12 11:15:06', 'A');

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
(41, 5, '', '', '', '0000-00-00', '/FYP/images/icon_41.jpg', '', 3, '');

-- --------------------------------------------------------

--
-- 資料表結構 `notification`
--

CREATE TABLE `notification` (
  `id` int(11) NOT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `message` text DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
(23, 69, 3, 350, 'pending', 'payment/receipts/RECEIPT_23_1744351947.jpeg', '2025-04-11 06:12:27', NULL);

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
(10, 5, 123, 'tutor', '2025-03-18 10:00:00', '4.3', '英文科目表现出色，具有很大的发展潜力');

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
-- 資料表索引 `first_lesson`
--
ALTER TABLE `first_lesson`
  ADD PRIMARY KEY (`first_lesson_id`),
  ADD UNIQUE KEY `booking_id` (`booking_id`);

--
-- 資料表索引 `first_lesson_responses`
--
ALTER TABLE `first_lesson_responses`
  ADD PRIMARY KEY (`response_id`),
  ADD KEY `fk_first_lesson_responses_booking` (`booking_id`);

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
-- 資料表索引 `member_cv`
--
ALTER TABLE `member_cv`
  ADD PRIMARY KEY (`cv_id`),
  ADD UNIQUE KEY `unique_member_cv` (`member_id`),
  ADD KEY `member_id` (`member_id`);

--
-- 資料表索引 `member_detail`
--
ALTER TABLE `member_detail`
  ADD PRIMARY KEY (`member_detail_id`),
  ADD KEY `idx_member_id_detail` (`member_id`);

--
-- 資料表索引 `notification`
--
ALTER TABLE `notification`
  ADD PRIMARY KEY (`id`);

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
  MODIFY `app_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=128;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `application_date`
--
ALTER TABLE `application_date`
  MODIFY `app_Date_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=31;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `application_district`
--
ALTER TABLE `application_district`
  MODIFY `application_district_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=85;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `application_subject`
--
ALTER TABLE `application_subject`
  MODIFY `application_subject_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=118;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `booking`
--
ALTER TABLE `booking`
  MODIFY `booking_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=22;

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
-- 使用資料表自動遞增(AUTO_INCREMENT) `first_lesson`
--
ALTER TABLE `first_lesson`
  MODIFY `first_lesson_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=32;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `first_lesson_responses`
--
ALTER TABLE `first_lesson_responses`
  MODIFY `response_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `match`
--
ALTER TABLE `match`
  MODIFY `match_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=73;

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
-- 使用資料表自動遞增(AUTO_INCREMENT) `member_cv`
--
ALTER TABLE `member_cv`
  MODIFY `cv_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `member_detail`
--
ALTER TABLE `member_detail`
  MODIFY `member_detail_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=42;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `notification`
--
ALTER TABLE `notification`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `payment`
--
ALTER TABLE `payment`
  MODIFY `payment_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `subject`
--
ALTER TABLE `subject`
  MODIFY `subject_id` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=25;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `tutor_rating`
--
ALTER TABLE `tutor_rating`
  MODIFY `rate_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

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
-- 資料表的限制式 `first_lesson_responses`
--
ALTER TABLE `first_lesson_responses`
  ADD CONSTRAINT `fk_first_lesson_responses_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`booking_id`) ON DELETE CASCADE;

--
-- 資料表的限制式 `member_cv`
--
ALTER TABLE `member_cv`
  ADD CONSTRAINT `member_cv_ibfk_1` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`) ON DELETE CASCADE;

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
