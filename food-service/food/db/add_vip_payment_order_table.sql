CREATE TABLE `vip_payment_order` (
  `id` varchar(36) NOT NULL COMMENT 'VIP支付订单ID(UUID)',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  `out_trade_no` varchar(64) NOT NULL COMMENT '商户订单号',
  `transaction_id` varchar(64) DEFAULT NULL COMMENT '微信支付订单号',
  `plan_code` varchar(40) NOT NULL COMMENT '套餐编码',
  `amount_fen` int NOT NULL COMMENT '支付金额，单位分',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING/PAID/FAILED/CLOSED',
  `prepay_id` varchar(128) DEFAULT NULL COMMENT '微信预支付交易会话ID',
  `paid_at` datetime DEFAULT NULL COMMENT '支付完成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vip_payment_order_out_trade_no` (`out_trade_no`),
  KEY `idx_vip_payment_order_user` (`user_id`),
  KEY `idx_vip_payment_order_status` (`status`),
  CONSTRAINT `fk_vip_payment_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='VIP支付订单表';
