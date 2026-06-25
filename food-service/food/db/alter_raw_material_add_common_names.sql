ALTER TABLE `raw_material`
  ADD COLUMN `common_names` varchar(500) NOT NULL DEFAULT '' COMMENT '常见名/学名/缩写，英文逗号分隔' AFTER `name`;
