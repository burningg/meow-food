CREATE TABLE `knowledge_article` (
  `id` varchar(36) NOT NULL COMMENT '知识ID(UUID)',
  `title` varchar(120) NOT NULL COMMENT '题目',
  `category` varchar(50) NOT NULL COMMENT '分类',
  `image_url` varchar(500) DEFAULT NULL COMMENT '封面图URL',
  `body_nodes_json` text NOT NULL COMMENT '正文rich-text nodes JSON，使用通用HTML标签兼容小程序和其他端',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-下架，1-发布',
  `published_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_status_published` (`status`,`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食知识表';

INSERT INTO `knowledge_article` (`id`, `title`, `category`, `image_url`, `body_nodes_json`, `status`, `published_at`) VALUES
('e8f0d8b6-4f9b-46f8-9b6f-1a2b3c4d5e03', '大蒜：厨房里的含硫小烟花', '调料', NULL, '[{"name":"p","children":[{"type":"text","text":"大蒜的风味不是天生就“冲”，而是被切开的那一刻才开机：蒜氨酸和蒜氨酸酶相遇，生成大蒜素，辛辣、硫香、微甜的开场白就亮起来了。"}]},{"name":"p","children":[{"type":"text","text":"生蒜像一盏很有主见的小灯泡，辛辣、清脆、存在感强；小火慢炒后，硫香变柔，冒出坚果、焦糖和一点奶香似的甜。烤整头蒜则更像可抹面包的温柔蒜酱。"}]},{"name":"ul","children":[{"name":"li","children":[{"type":"text","text":"风味：切得越细、静置几分钟，大蒜素越活跃，适合凉拌、蘸料和蒜泥白肉；想温和一点，就切片或整瓣下锅。"}]},{"name":"li","children":[{"type":"text","text":"烹饪技巧：蒜末用中小火，微微金黄就离火，焦黑会发苦；炖煮用拍裂整瓣，出锅前补蒜蓉，可以让香气从后台走到聚光灯下。"}]},{"name":"li","children":[{"type":"text","text":"常见品种：紫皮蒜辛辣更尖，适合爆炒和腌制；白皮蒜味道圆、水分足，适合炖煮；独头蒜瓣少味浓，泡醋或烤着吃很会表现。"}]},{"name":"li","children":[{"type":"text","text":"营养功效：大蒜提供含硫化合物、多酚和少量矿物质，能帮菜肴在少放盐时依然有满足感。它是好调料，不是药丸，胃敏感、术前或服用抗凝药的人要少量并听医嘱。"}]}]},{"name":"p","children":[{"type":"text","text":"记住一个小口诀：生蒜负责“醒脑”，熟蒜负责“圆场”，焦蒜负责提醒你下次火小一点。"}]}]', 1, CURRENT_TIMESTAMP),
('e8f0d8b6-4f9b-46f8-9b6f-1a2b3c4d5e02', '肉蔻是什么，适合放在哪些菜里', '香料', 'https://images.unsplash.com/photo-1596040033229-a9821ebd058d?auto=format&fit=crop&w=900&q=80', '[{"name":"p","children":[{"type":"text","text":"肉蔻通常指肉豆蔻，是肉豆蔻树种仁制成的香料。它的气味温暖，带一点木质、坚果和甜香，适合给厚重菜肴收一层柔和的尾味。"}]},{"name":"p","children":[{"type":"text","text":"用它调味时不需要多，一点点现磨粉末就能让炖肉、奶油汤、土豆泥、南瓜、肉酱和烘焙甜点更圆润。中式炖煮里，也可以和桂皮、丁香少量搭配。"}]},{"name":"ul","children":[{"name":"li","children":[{"type":"text","text":"少量现磨：香气更清楚，也更容易控制味道。"}]},{"name":"li","children":[{"type":"text","text":"晚些加入：长时间高温会让香气变钝，出锅前少量补一点更稳。"}]},{"name":"li","children":[{"type":"text","text":"只作调味：不要把肉蔻当作保健品大量食用，特殊身体情况先遵医嘱。"}]}]},{"name":"p","children":[{"type":"text","text":"如果第一次尝试，可以从一小撮开始。闻起来刚刚有暖香，但吃不出明显药味，就是比较舒服的量。"}]}]', 1, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 HOUR))
