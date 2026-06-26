ALTER TABLE `raw_material`
  ADD COLUMN `calorie_estimate` varchar(100) NOT NULL DEFAULT '' COMMENT '热量预估' AFTER `allergen_flag`;
