-- Flyway baseline: Module 04까지 ddl-auto(update)로 만들어진 현재 프로덕션 스키마 스냅샷.
-- 프로덕션 RDS는 baseline-on-migrate로 이 V1을 건너뛰고(이미 존재), CI 빈 DB는 V1+V2를 적용해 전체 스키마를 재현한다.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `cart_items` (
  `cart_item_id` bigint NOT NULL AUTO_INCREMENT,
  `added_at` datetime(6) NOT NULL,
  `course_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  PRIMARY KEY (`cart_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comments` (
  `comment_id` bigint NOT NULL AUTO_INCREMENT,
  `accept_count` int NOT NULL,
  `author_id` bigint NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_accepted` bit(1) NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `status` enum('ACTIVE','ADMIN_DELETED','DELETED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`comment_id`),
  KEY `idx_comments_post_id` (`post_id`),
  KEY `idx_comments_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `course` (
  `course_id` bigint NOT NULL AUTO_INCREMENT,
  `author_id` bigint NOT NULL,
  `price` int NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `learning_objectives` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price_type` enum('FREE','PAID') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('DELETED','DRAFT','PUBLISHED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_audience` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `tech_tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `thumbnail_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`course_id`),
  KEY `idx_course_status_created_at` (`status`,`created_at`),
  KEY `idx_course_author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `course_curriculum` (
  `curriculum_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `order_index` int NOT NULL,
  PRIMARY KEY (`curriculum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `course_section` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_index` int NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKp078rbq5ufse4i11ut9vraklr` (`course_id`),
  CONSTRAINT `FKp078rbq5ufse4i11ut9vraklr` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `daily_study_stats` (
  `daily_study_stat_id` bigint NOT NULL AUTO_INCREMENT,
  `completed_lesson_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `member_id` bigint NOT NULL,
  `stat_date` date NOT NULL,
  `study_seconds` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `watched_lesson_count` int NOT NULL,
  PRIMARY KEY (`daily_study_stat_id`),
  UNIQUE KEY `uk_daily_study_stats_member_id_stat_date` (`member_id`,`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `enrollment` (
  `enrollment_id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `enrolled_at` datetime(6) NOT NULL,
  `expired_at` datetime(6) DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `status` enum('COMPLETED','ENROLLED','EXPIRED','IN_PROGRESS','REFUNDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`enrollment_id`),
  UNIQUE KEY `uk_enrollment_member_course` (`member_id`,`course_id`),
  KEY `idx_enrollment_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `id_sequences` (
  `seq_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `seq_value` bigint DEFAULT NULL,
  PRIMARY KEY (`seq_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `lesson` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `duration_seconds` int DEFAULT NULL,
  `file_processing_status` enum('COMPLETED','FAILED','PENDING','PROCESSING') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_index` int NOT NULL,
  `s3_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `video_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `section_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKagc6yodpv36jku31ug2x939b4` (`section_id`),
  CONSTRAINT `FKagc6yodpv36jku31ug2x939b4` FOREIGN KEY (`section_id`) REFERENCES `course_section` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `members` (
  `member_id` bigint NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `birth_date` date DEFAULT NULL,
  `career` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `is_locked` bit(1) NOT NULL,
  `is_password_change_required` bit(1) NOT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `locked_at` datetime(6) DEFAULT NULL,
  `login_fail_count` int NOT NULL,
  `one_line_intro` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `optional_terms_agreed` bit(1) NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profile_image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` enum('ADMIN','INSTRUCTOR','STUDENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','SUSPENDED','WITHDRAWN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_member_username` (`username`),
  UNIQUE KEY `uk_member_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notices` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT,
  `author_id` bigint NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_pinned` bit(1) NOT NULL,
  `status` enum('DRAFT','PUBLISHED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`notice_id`),
  KEY `idx_notices_dashboard` (`type`,`status`,`created_at`,`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notification` (
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `is_read` bit(1) NOT NULL,
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `receiver_id` bigint NOT NULL,
  `redirect_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` enum('COMMENT_ACCEPTED','COMMENT_REPLY','COURSE_REGISTER','NOTICE','POST_COMMENT','REPORT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `idx_notification_receiver_read_url` (`receiver_id`,`is_read`,`redirect_url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `order_items` (
  `order_item_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `price` int NOT NULL,
  `refunded` bit(1) NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`order_item_id`),
  KEY `idx_order_items_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `orders` (
  `order_id` bigint NOT NULL,
  `final_amount` int NOT NULL,
  `member_id` bigint NOT NULL,
  `order_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ordered_at` datetime(6) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `payment_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_amount` int NOT NULL,
  `payment_type` enum('COURSE','SUBSCRIPTION') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_orders_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `payments` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint DEFAULT NULL,
  `idempotency_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `member_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `paid_amount` int NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `pg_transaction_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `UKj4pdqn1207fuhx2gybp1ue799` (`idempotency_key`),
  KEY `idx_payments_member_id` (`member_id`),
  KEY `idx_payments_member_id_paid_at_id` (`member_id`,`paid_at` DESC,`payment_id` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post_files` (
  `file_id` bigint NOT NULL AUTO_INCREMENT,
  `file_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `post_id` bigint NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `posts` (
  `post_id` bigint NOT NULL AUTO_INCREMENT,
  `author_id` bigint NOT NULL,
  `board_type` enum('FREE','QUESTION') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `comment_count` int NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_accepted` bit(1) NOT NULL,
  `status` enum('ACTIVE','ADMIN_DELETED','DELETED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `view_count` int NOT NULL,
  PRIMARY KEY (`post_id`),
  KEY `idx_posts_board_status_created` (`board_type`,`status`,`created_at`),
  KEY `idx_posts_board_status_count` (`board_type`,`status`,`comment_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reports` (
  `report_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `memo` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `report_types` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `reported_member_id` bigint NOT NULL,
  `reporter_id` bigint NOT NULL,
  `status` enum('PENDING','REJECTED','RESOLVED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `target_type` enum('COMMENT','POST','REVIEW') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`report_id`),
  KEY `idx_reports_reported_member_id` (`reported_member_id`),
  KEY `idx_reports_target_latest` (`target_type`,`target_id`,`created_at`,`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reviews` (
  `review_id` bigint NOT NULL AUTO_INCREMENT,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `member_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `status` enum('ACTIVE','ADMIN_DELETED','DELETED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`review_id`),
  KEY `idx_reviews_course_id` (`course_id`),
  KEY `idx_reviews_course_rating` (`course_id`,`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `study_timer_sessions` (
  `study_timer_session_id` bigint NOT NULL AUTO_INCREMENT,
  `accumulated_study_seconds` int NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  `lesson_id` bigint DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `paused_at` datetime(6) DEFAULT NULL,
  `started_at` datetime(6) NOT NULL,
  `status` enum('CANCELED','ENDED','PAUSED','RUNNING') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`study_timer_session_id`),
  KEY `idx_study_timer_sessions_member_id_status` (`member_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `subjects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `subscription_plans` (
  `plan_id` bigint NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `subscriptions` (
  `subscription_id` bigint NOT NULL,
  `expired_at` datetime(6) NOT NULL,
  `member_id` bigint NOT NULL,
  `status` enum('ACTIVE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `plan_id` bigint NOT NULL,
  `active_member_id` bigint DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `paid_amount` int NOT NULL,
  `payment_method` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `started_at` datetime(6) NOT NULL,
  PRIMARY KEY (`subscription_id`),
  UNIQUE KEY `uk_subscription_active_member` (`active_member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `uploaded_files` (
  `file_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `file_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `uploader_id` bigint NOT NULL,
  PRIMARY KEY (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `video` (
  `video_id` bigint NOT NULL,
  `curriculum_id` bigint NOT NULL,
  `s3_key` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `duration_seconds` int DEFAULT NULL,
  `sort_order` int NOT NULL,
  `is_preview` tinyint(1) NOT NULL,
  PRIMARY KEY (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `video_progress` (
  `progress_id` bigint NOT NULL,
  `is_completed` bit(1) NOT NULL,
  `course_id` bigint NOT NULL,
  `last_position_sec` int NOT NULL,
  `member_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `video_id` bigint NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `watch_time_sec` int NOT NULL,
  PRIMARY KEY (`progress_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `view_logs` (
  `view_log_id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `post_id` bigint NOT NULL,
  `viewed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`view_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `wishlist_items` (
  `wishlist_item_id` bigint NOT NULL AUTO_INCREMENT,
  `added_at` datetime(6) NOT NULL,
  `course_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  PRIMARY KEY (`wishlist_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
