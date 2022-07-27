/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
-- 灰度测试
-- ----------------------------
-- Table structure for t_ds_relation_gray
-- ----------------------------
CREATE TABLE `t_ds_relation_gray` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT 'self_increasing id',
    `element_type` varchar(255) NOT NULL,
    `element_id` int NOT NULL,
    `element_code` bigint DEFAULT NULL,
    `gray_flag` tinyint NOT NULL DEFAULT '1' COMMENT 'gray flag 此值默认为1,理论上这个值应该全部为1,即出现在此表中的都应该是灰度测试相关的',
    `create_time` datetime NOT NULL COMMENT 'create time',
    `update_time` datetime NOT NULL COMMENT 'update time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_ds_relation_gray_instance_log
-- ----------------------------
CREATE TABLE `t_ds_relation_gray_instance_log` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT 'self_increasing id',
    `element_type` varchar(255) NOT NULL,
    `element_id` int NOT NULL,
    `element_code` bigint DEFAULT NULL,
    `gray_flag` tinyint NOT NULL DEFAULT '1' COMMENT 'gray flag 此值默认为1,理论上这个值应该全部为1,即出现在此表中的都应该是灰度测试相关的',
    `create_time` datetime NOT NULL COMMENT 'create time',
    `update_time` datetime NOT NULL COMMENT 'update time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- 依赖补数相关
-- ----------------------------
-- Table structure for t_ds_depend_complement
-- ----------------------------
CREATE TABLE `t_ds_depend_complement` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT 'key',
    `project_code` bigint DEFAULT NULL COMMENT '项目code',
    `start_process_definition_code` bigint NOT NULL COMMENT '起始的工作流编码',
    `start_process_definition_name` varchar(255) DEFAULT NULL COMMENT '起始的工作流名称',
    `schedule_start_date` date DEFAULT NULL COMMENT '依赖补数调度任务开始日期',
    `schedule_end_date` date DEFAULT NULL COMMENT '依赖补数调度任务结束日志',
    `command_type` tinyint DEFAULT NULL COMMENT '11 depend complement',
    `depend_complement_process_json` text NOT NULL COMMENT '依赖补数任务工作流列表',
    `depend_complement_param` text COMMENT '参数',
    `executor_id` int DEFAULT NULL COMMENT '执行者id',
    `state` tinyint DEFAULT NULL COMMENT '状态',
    `failure_strategy` tinyint DEFAULT '0' COMMENT 'Failed policy: 0 end, 1 continue',
    `worker_group` varchar(64) DEFAULT NULL COMMENT '工作组',
    `environment_code` bigint DEFAULT '-1' COMMENT '环境编码',
    `warning_type` tinyint DEFAULT '0' COMMENT 'Alarm type: 0 is not sent, 1 process is sent successfully, 2 process is sent failed, 3 process is sent successfully and all failures are sent',
    `warning_group_id` int DEFAULT NULL COMMENT 'warning group',
    `active` tinyint DEFAULT '1' COMMENT '此条数据是否生效',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_ds_depend_complement_detail
-- ----------------------------
CREATE TABLE `t_ds_depend_complement_detail` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT 'key',
    `depend_complement_id` int NOT NULL COMMENT '批次id',
    `schedule_start_date` date DEFAULT NULL COMMENT '依赖补数调度任务开始日期',
    `schedule_end_date` date DEFAULT NULL,
    `state` tinyint DEFAULT NULL COMMENT '执行者',
    `active` tinyint DEFAULT '1' COMMENT '此条数据是否生效',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_ds_depend_complement_detail_process
-- ----------------------------
CREATE TABLE `t_ds_depend_complement_detail_process` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT 'key',
    `depend_complement_id` int NOT NULL COMMENT '批次id',
    `depend_complement_detail_id` int NOT NULL COMMENT '明细id',
    `process_definition_code` bigint DEFAULT NULL COMMENT 'process definition code',
    `process_definition_version` int DEFAULT '0' COMMENT 'process definition version',
    `process_definition_name` varchar(255) DEFAULT NULL COMMENT 'process defin name',
    `process_instance_id` int DEFAULT '0' COMMENT '工作流实例id',
    `level` int NOT NULL COMMENT '层级',
    `failure_strategy` tinyint DEFAULT '0' COMMENT 'Failed policy: 0 end, 1 continue',
    `warning_type` tinyint DEFAULT '0' COMMENT 'Alarm type: 0 is not sent, 1 process is sent successfully, 2 process is sent failed, 3 process is sent successfully and all failures are sent',
    `warning_group_id` int DEFAULT NULL COMMENT 'warning group',
    `worker_group` varchar(64) DEFAULT NULL COMMENT 'worker group',
    `environment_code` bigint DEFAULT '-1' COMMENT 'environment code',
    `active` tinyint DEFAULT '1' COMMENT '此条数据是否生效',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;