ALTER TABLE `vip_payment_order`
  ADD COLUMN `product_id` varchar(64) DEFAULT NULL COMMENT '微信虚拟支付道具ID' AFTER `plan_code`,
  ADD COLUMN `env` int NOT NULL DEFAULT 0 COMMENT '虚拟支付环境：0-现网 1-沙箱' AFTER `amount_fen`,
  ADD COLUMN `open_id` varchar(64) DEFAULT NULL COMMENT '微信openid' AFTER `env`,
  ADD COLUMN `wechat_pay_mch_order_no` varchar(64) DEFAULT NULL COMMENT '微信虚拟支付商户单号' AFTER `open_id`,
  ADD COLUMN `attach` varchar(255) DEFAULT NULL COMMENT '虚拟支付透传字段' AFTER `wechat_pay_mch_order_no`,
  ADD COLUMN `pay_channel` varchar(20) DEFAULT NULL COMMENT '支付渠道：WECHAT/APPLE' AFTER `attach`;
