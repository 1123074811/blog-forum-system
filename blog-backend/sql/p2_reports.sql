-- ============================================
-- P2: 内容举报系统
-- ============================================

CREATE TABLE IF NOT EXISTS `reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint NOT NULL COMMENT '举报人ID',
  `target_type` varchar(20) NOT NULL COMMENT '举报目标类型: article, comment',
  `target_id` bigint NOT NULL COMMENT '举报目标ID',
  `reason` varchar(50) NOT NULL COMMENT '举报原因分类',
  `description` text COMMENT '补充说明',
  `status` varchar(20) DEFAULT 'pending' COMMENT 'pending=待处理, resolved=已处理, dismissed=已驳回',
  `handled_by` bigint DEFAULT NULL COMMENT '处理人ID(管理员)',
  `handled_at` varchar(30) DEFAULT NULL COMMENT '处理时间',
  `handle_note` text COMMENT '处理备注',
  `created_at` varchar(30) NOT NULL COMMENT '举报时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_reporter` (`reporter_id`),
  INDEX `idx_target` (`target_type`, `target_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_created` (`created_at` DESC)
) ENGINE=InnoDB COMMENT='内容举报表';
