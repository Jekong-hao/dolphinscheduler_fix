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

package org.apache.dolphinscheduler.server.master.runner;

import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.enums.ExecutionStatus;
import org.apache.dolphinscheduler.common.thread.Stopper;
import org.apache.dolphinscheduler.common.thread.ThreadUtils;
import org.apache.dolphinscheduler.dao.entity.*;
import org.apache.dolphinscheduler.server.master.config.MasterConfig;
import org.apache.dolphinscheduler.server.master.registry.ServerNodeManager;
import org.apache.dolphinscheduler.service.process.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.dolphinscheduler.common.Constants.*;

@Service
public class DependComplementThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DependComplementThread.class);

    @Autowired
    private ProcessService processService;

    @Autowired
    private MasterConfig masterConfig;

    private DependComplementStateWheelThead dependComplementStateWheelThead;

    // 用于存储dependComplement的状态(只存储stop指令)
    private final ConcurrentHashMap<Integer, ExecutionStatus> dependComplementStateMaps = new ConcurrentHashMap<>();
    // 用于存储dependComplementDetail的状态(只存储stop指令)
    private final ConcurrentHashMap<Integer, ExecutionStatus> dependComplementDetailStateMaps = new ConcurrentHashMap<>();
    // 用于在内存中维持,哪些任务已经提交了
    private final List<Integer> dependComplementSubmitThreadList = new ArrayList<>();

    // 创建线程池
    ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    /**
     * 初始化依赖补数容错
     */
    public void init() {
        // 先进行一次detailprocess的日志初始化
        dependComplementStateWheelThead = new DependComplementStateWheelThead(
                processService,
                dependComplementStateMaps,
                dependComplementDetailStateMaps,
                masterConfig.getMasterDependComplementStateCheckInterval() * Constants.SLEEP_TIME_MILLIS
        );
    }

    @Override
    public synchronized void start() {
        super.setName("DependComplementThread");
        super.start();
        this.dependComplementStateWheelThead.start();
    }

    @Override
    public void run() {
        // 休眠两分钟后启动补数作业,等待master注册完成
        ThreadUtils.sleep((long) Constants.SLEEP_TIME_MILLIS * 60 * 2);
        while (Stopper.isRunning()) {
            logger.info("depend complement execute started");
            try {
                // 补数作业调度
                handleNormalExecution();
            } catch (Exception e) {
                logger.error("depend complement execute error", e);
            } finally {
                // 每过20秒进行一次依赖补数检验
                ThreadUtils.sleep((long) Constants.SLEEP_TIME_MILLIS  * masterConfig.getMasterDependComplementCheckInterval());
            }
        }
    }

    /**
     * 如果是线程启动完成,后面正常任务执行
     */
    private void handleNormalExecution() {
        // 查询需要进行依赖补数的任务,从主表中查状态为:提交成功和正在运行
        List<DependComplement> dependComplementList = getDependComplement();
        // 如果没有要处理的依赖补数,直接返回
        if (dependComplementList == null || dependComplementList.size() == 0) {
            return;
        }

        try {
            for (DependComplement dependComplement : dependComplementList) {
                if (!dependComplementSubmitThreadList.contains(dependComplement.getId())) {
                    dependComplementSubmitThreadList.add(dependComplement.getId());
                    cachedThreadPool.submit(new DependComplementRunnable(dependComplement));
                }
            }
        } catch (Exception e) {
            logger.error("scan depend complement error", e);
        }
    }

    private List<DependComplement> getDependComplement() {
        int pageNumber = 0;
        List<DependComplement> result = new ArrayList<>();
        while (Stopper.isRunning()) {
            if (ServerNodeManager.MASTER_SIZE == 0) {
                return null;
            }
            logger.info("master size:{}", ServerNodeManager.MASTER_SIZE);
            // 获取需要依赖补数的任务(这里是提交作业的地方,不涉及容错相关,所以只获取提交成功的任务,后面会对状态进行变更)
            final List<DependComplement> dependComplementList = processService.queryDependComplementWithState(ServerNodeManager.MASTER_SIZE, pageNumber);
            if (dependComplementList.size() == 0) {
                return null;
            }
            for (DependComplement dependComplement : dependComplementList) {
                final Integer slot = ServerNodeManager.getSlot();
                logger.info("master slot: {}", slot);
                if (ServerNodeManager.MASTER_SIZE != 0 && dependComplement.getId() % ServerNodeManager.MASTER_SIZE == slot) {
                    result.add(dependComplement);
                    break;
                }
            }
            if (result.size() > 0) {
                logger.info("find dependComplement task num: {}", result.size());
                for (DependComplement dependComplement : result) {
                    logger.info("find dependComplement task, id: {}", dependComplement.getId());
                }
                break;
            }
            pageNumber += 1;
        }
        return result;
    }

    private class DependComplementRunnable implements Runnable {

        private final DependComplement dependComplement;

        public DependComplementRunnable(DependComplement dependComplement) {
            this.dependComplement = dependComplement;
        }

        @Override
        public void run() {
            // 执行调度的参数
            Map<String, String> config = new HashMap<>();

            if (dependComplement.getState() == ExecutionStatus.SUBMITTED_SUCCESS) {
                // 正常任务
                config.put(DEPEND_COMPLEMENT_RUN_TYPE, DEPEND_COMPLEMENT_RUN_TYPE_NORMAL);
            } else if (dependComplement.getState() == ExecutionStatus.RUNNING_EXECUTION) {
                // 容错任务
                config.put(DEPEND_COMPLEMENT_RUN_TYPE, DEPEND_COMPLEMENT_RUN_TYPE_FAILOVER);
            }

            // 生成DependComplementDetailProcess到数据库中,写入到t_ds_depend_complement_detail_process表中
            processService.handleDependComplement(
                    logger,
                    dependComplement,
                    config,
                    dependComplementStateMaps,
                    dependComplementDetailStateMaps,
                    masterConfig.getMasterDependComplementCommandCheckInterval(),
                    masterConfig.getMasterDependComplementDetailCheckInterval(),
                    masterConfig.getMasterDependComplementCommandCheckTimeout());
        }
    }
}