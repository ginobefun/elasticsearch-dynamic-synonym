
CREATE TABLE `dynamic_synonym_rule` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `rule` varchar(255) NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '1: available, 0:unavailable',
  `version` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX_DYNAMIC_SYNONYM_VERSION` (`version`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Sample Records of dynamic_synonym_rule
-- ----------------------------
INSERT INTO `dynamic_synonym_rule` VALUES ('1', 'i-pod, i pod => ipod', '1', '1');
INSERT INTO `dynamic_synonym_rule` VALUES ('2', 'foozball, foosball', '1', '1');
