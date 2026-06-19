ALTER TABLE `vip_payment_order`
  ADD COLUMN `wechat_order_id` varchar(64) DEFAULT NULL COMMENT '微信虚拟支付内部单号' AFTER `open_id`,
  ADD COLUMN `provide_status` varchar(20) NOT NULL DEFAULT 'NONE' COMMENT '发货状态：NONE/PROCESSING/SUCCESS' AFTER `pay_channel`,
  ADD COLUMN `provided_at` datetime DEFAULT NULL COMMENT '发货完成时间' AFTER `paid_at`;
