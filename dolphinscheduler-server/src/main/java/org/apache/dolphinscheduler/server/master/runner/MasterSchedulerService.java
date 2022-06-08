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
import org.apache.dolphinscheduler.common.enums.StateEvent;
import org.apache.dolphinscheduler.common.enums.StateEventType;
import org.apache.dolphinscheduler.common.enums.TaskType;
import org.apache.dolphinscheduler.common.thread.Stopper;
import org.apache.dolphinscheduler.common.thread.ThreadUtils;
import org.apache.dolphinscheduler.common.utils.NetUtils;
import org.apache.dolphinscheduler.common.utils.OSUtils;
import org.apache.dolphinscheduler.dao.entity.Command;
import org.apache.dolphinscheduler.dao.entity.ProcessDefinition;
import org.apache.dolphinscheduler.dao.entity.ProcessInstance;
import org.apache.dolphinscheduler.dao.entity.TaskInstance;
import org.apache.dolphinscheduler.remote.NettyRemotingClient;
import org.apache.dolphinscheduler.remote.config.NettyClientConfig;
import org.apache.dolphinscheduler.server.master.config.MasterConfig;
import org.apache.dolphinscheduler.server.master.dispatch.executor.NettyExecutorManager;
import org.apache.dolphinscheduler.server.master.processor.queue.TaskResponseService;
import org.apache.dolphinscheduler.server.master.registry.MasterRegistryClient;
import org.apache.dolphinscheduler.server.master.registry.ServerNodeManager;
import org.apache.dolphinscheduler.service.alert.ProcessAlertManager;
import org.apache.dolphinscheduler.service.process.ProcessService;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * master scheduler thread
 */
@Service
public class MasterSchedulerService extends Thread {

    /**
     * logger of MasterSchedulerService
     */
    private static final Logger logger = LoggerFactory.getLogger(MasterSchedulerService.class);

    /**
     * handle task event
     */
    @Autowired
    private TaskResponseService taskResponseService;

    /**
     * dolphinscheduler database interface
     */
    @Autowired
    private ProcessService processService;

    /**
     * zookeeper master client
     */
    @Autowired
    private MasterRegistryClient masterRegistryClient;

    /**
     * master config
     */
    @Autowired
    private MasterConfig masterConfig;

    /**
     * alert manager
     */
    @Autowired
    private ProcessAlertManager processAlertManager;

    /**
     * netty remoting client
     */
    private NettyRemotingClient nettyRemotingClient;

    @Autowired
    NettyExecutorManager nettyExecutorManager;

    @Value("${dolphinscheduler.server.gray-flag}")
    private String grayFlag;

    /**
     * master exec service
     */
    private MasterExecService masterExecService;

    /**
     * start process failed map
     */
    private final ConcurrentHashMap<Integer, WorkflowExecuteThread> startProcessFailedMap = new ConcurrentHashMap<>();

    /**
     * process instance execution list
     */
    private ConcurrentHashMap<Integer, WorkflowExecuteThread> processInstanceExecMaps;
    /**
     * process timeout check list
     */
    ConcurrentHashMap<Integer, ProcessInstance> processTimeoutCheckList = new ConcurrentHashMap<>();

    /**
     * task time out checkout list
     */
    ConcurrentHashMap<Integer, TaskInstance> taskTimeoutCheckList = new ConcurrentHashMap<>();

    /**
     * task retry check list
     */
    ConcurrentHashMap<Integer, TaskInstance> taskRetryCheckList = new ConcurrentHashMap<>();

//    private StateWheelExecuteThread stateWheelExecuteThread;

    private WorkflowStateWheelExecuteThread workflowStateWheelExecuteThread;

    /**
     * constructor of MasterSchedulerService
     */
    public void init(ConcurrentHashMap<Integer, WorkflowExecuteThread> processInstanceExecMaps) {
        this.processInstanceExecMaps = processInstanceExecMaps;
        this.masterExecService = new MasterExecService(this.startProcessFailedMap,
                (ThreadPoolExecutor) ThreadUtils.newDaemonFixedThreadExecutor("Master-Exec-Thread", masterConfig.getMasterExecThreads()));
        NettyClientConfig clientConfig = new NettyClientConfig();
        this.nettyRemotingClient = new NettyRemotingClient(clientConfig);

//        // TODO 应该每个workflow实例负责各自的调度

        workflowStateWheelExecuteThread = new WorkflowStateWheelExecuteThread(
                masterExecService,
                processAlertManager,
                processService,
                processTimeoutCheckList,
                processInstanceExecMaps,
                startProcessFailedMap,
                masterConfig.getStateWheelInterval() * Constants.SLEEP_TIME_MILLIS
        );
    }

    @Override
    public synchronized void start() {
        super.setName("MasterSchedulerService");
        super.start();
//        this.stateWheelExecuteThread.start();
        this.workflowStateWheelExecuteThread.start();
    }

    public void close() {
        masterExecService.shutdown();
        boolean terminated = false;
        try {
            terminated = masterExecService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
        if (!terminated) {
            logger.warn("masterExecService shutdown without terminated, increase await time");
        }
        nettyRemotingClient.close();
        logger.info("master schedule service stopped...");
    }

    /**
     * run of MasterSchedulerService
     */
    @Override
    public void run() {
        logger.info("master scheduler started");
        while (Stopper.isRunning()) {
            try {
                boolean runCheckFlag = OSUtils.checkResource(masterConfig.getMasterMaxCpuloadAvg(), masterConfig.getMasterReservedMemory());
                if (!runCheckFlag) {
                    Thread.sleep(Constants.SLEEP_TIME_MILLIS);
                    continue;
                }
                scheduleProcess();
                logger.debug("Scheduler running process instance nums {} , ids [{}].",
                        this.processInstanceExecMaps.size(),
                        this.processInstanceExecMaps.keySet()
                                .stream().map(String::valueOf).collect(Collectors.joining(",")));
//                checkFailedStartProcess();

//                // 检查workflow完整性
//                checkWorkflowCompleteness();
            } catch (Exception e) {
                logger.error("master scheduler thread error", e);
            }
        }
    }


    /**
     * 检查正在执行的工作的完整性
     */
    private void checkWorkflowCompleteness() {
        for (WorkflowExecuteThread workflowExecuteThread : this.processInstanceExecMaps.values()) {
            // TODO 轮训查询数据库，有性能压力
            List<TaskInstance> taskInstancesList = this.processService.findValidTaskListByProcessId(workflowExecuteThread.getProcessInstance().getId());
            for (TaskInstance taskInstance : taskInstancesList) {
                if (taskInstance.getTaskType() == TaskType.DEPENDENT.getDesc() || taskInstance.taskCanRetry()) {
                    if (!this.taskRetryCheckList.containsKey(taskInstance.getId())) {
                        logger.error("[taskRetryCheck] process instance : {} incompleteness. {} task instance : {} disappear.",
                                workflowExecuteThread.getProcessInstance().getId(), taskInstance.getTaskType(), taskInstance.getId());
                    }
                }
            }
        }
    }

//    /**
//     * check start failed process and cancel retry
//     * @deprecated
//     */
//    private void checkFailedStartProcess() {
//        if (!this.failedStartProcessMap.isEmpty()) {
//            for (ProcessInstance processInstance : this.failedStartProcessMap.values()) {
//                logger.info("Scheduler start process : {}, failed.", processInstance.getId());
//
////                // 取消重试
////                this.startProcessFailedMap.remove(processInstance.getId());
////                this.failedStartProcessMap.remove(processInstance.getId());
//            }
//        }
//    }

    /**
     * 1. get command by slot
     * 2. donot handle command if slot is empty
     */
    private void scheduleProcess() throws Exception {

        // make sure to scan and delete command  table in one transaction
        Command command = findOneCommand();

        if (command != null) {
            logger.info("find one command: id: {}, type: {}, for process [code : {}, version : {} process instance id : {}]",
                    command.getId(),
                    command.getCommandType(),
                    command.getProcessDefinitionCode(),
                    command.getProcessDefinitionVersion(),
                    command.getProcessInstanceId());
            try {
                ProcessInstance processInstance = processService.handleCommand(logger, getLocalAddress(), command);

                if (processInstance != null) {
                    logger.info("[process instance {}] master start at {} for schedule_time {}. with command type {}",
                            processInstance.getId(),
                            processInstance.getStartTime(),
                            processInstance.getScheduleTime(),
                            command.getCommandType());

                    WorkflowExecuteThread workflowExecuteThread = new WorkflowExecuteThread(
                            processInstance
                            , taskResponseService
                            , processService
                            , nettyExecutorManager
                            , processAlertManager
                            , masterConfig
                            , taskTimeoutCheckList
                            , taskRetryCheckList);

                    this.processInstanceExecMaps.put(processInstance.getId(), workflowExecuteThread);
                    if (processInstance.getTimeout() > 0) {
                        this.processTimeoutCheckList.put(processInstance.getId(), processInstance);
                    }
                    logger.info("[process instance {}] master handle command end, command {} process {} start...",
                            processInstance.getId(),
                            command.getId(), processInstance.getId());
                    masterExecService.execute(workflowExecuteThread);
                } else {
                    logger.error("master cannot init process instance from command : id : {} type : {}",
                            command.getId(), command.getCommandType());
                }
            } catch (Exception e) {
                logger.error("scan command error ", e);
                processService.moveToErrorCommand(command, e.toString());
            }
        } else {
            //indicate that no command ,sleep for 1s
            Thread.sleep(Constants.SLEEP_TIME_MILLIS);
        }
    }

    private Command findOneCommand() {
        int pageNumber = 0;
        Command result = null;
        while (Stopper.isRunning()) {
            if (ServerNodeManager.MASTER_SIZE == 0) {
                return null;
            }
            logger.debug("master size:{}",ServerNodeManager.MASTER_SIZE);
            List<Command> commandList = processService.findCommandPage(ServerNodeManager.MASTER_SIZE, pageNumber, grayFlag);
            if (commandList.size() == 0) {
                return null;
            }
            for (Command command : commandList) {
                int slot = ServerNodeManager.getSlot();
                if (ServerNodeManager.MASTER_SIZE != 0 && command.getId() % ServerNodeManager.MASTER_SIZE == slot) {
                    result = command;
                    break;
                }
            }
            if (result != null) {
                logger.info("find command {}, slot:{} :", result.getId(), ServerNodeManager.getSlot());
                break;
            }
            pageNumber += 1;
        }
        return result;
    }

    private String getLocalAddress() {
        return NetUtils.getAddr(masterConfig.getListenPort());
    }
}
