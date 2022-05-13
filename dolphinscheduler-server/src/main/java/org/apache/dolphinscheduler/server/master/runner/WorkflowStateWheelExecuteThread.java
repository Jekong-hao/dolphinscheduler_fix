package org.apache.dolphinscheduler.server.master.runner;

import com.google.common.collect.Maps;
import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.enums.ExecutionStatus;
import org.apache.dolphinscheduler.common.enums.StateEvent;
import org.apache.dolphinscheduler.common.enums.StateEventType;
import org.apache.dolphinscheduler.common.thread.Stopper;
import org.apache.dolphinscheduler.common.utils.DateUtils;
import org.apache.dolphinscheduler.dao.entity.ProcessInstance;
import org.apache.dolphinscheduler.service.alert.ProcessAlertManager;
import org.apache.dolphinscheduler.service.process.ProcessService;
import org.apache.hadoop.util.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * workflow 状态检测
 *
 * @author 林振杰
 * @email linzhenjie@aulton.com
 * @date 2022/4/22 2:24 下午
 * @Copyright Copyright (c)  aulton Inc. All Rights Reserved.
 **/
public class WorkflowStateWheelExecuteThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStateWheelExecuteThread.class);

    private ConcurrentHashMap<Integer, ProcessInstance> processInstanceTimeoutCheckList;
    private ConcurrentHashMap<Integer, WorkflowExecuteThread> processInstanceExecMaps;

    private Map<Integer, Integer> startProcessFailedRetryCountMaps = Maps.newHashMap();

    /**
     * start process failed map
     */
    private final ConcurrentHashMap<Integer, WorkflowExecuteThread> startProcessFailedMap;

    private int stateCheckIntervalSecs;

    /**
     * master exec service
     */
    private MasterExecService masterExecService;

    private ProcessAlertManager processAlertManager;
    private ProcessService processService;

    public WorkflowStateWheelExecuteThread(MasterExecService masterExecService,
                                           ProcessAlertManager processAlertManager,
                                           ProcessService processService,
                                           ConcurrentHashMap<Integer, ProcessInstance> processInstanceTimeoutCheckList,
                                           ConcurrentHashMap<Integer, WorkflowExecuteThread> processInstanceExecMaps,
                                           ConcurrentHashMap<Integer, WorkflowExecuteThread> startProcessFailedMap,
                                           int stateCheckIntervalSecs) {
        this.masterExecService = masterExecService;
        this.processAlertManager = processAlertManager;
        this.processService = processService;
        this.processInstanceTimeoutCheckList = processInstanceTimeoutCheckList;
        this.processInstanceExecMaps = processInstanceExecMaps;
        this.startProcessFailedMap = startProcessFailedMap;
        this.stateCheckIntervalSecs = stateCheckIntervalSecs;

    }


    @Override
    public void run() {
        logger.info("workflow state wheel thread start");
        while (Stopper.isRunning()) {
            try {
                check4StartProcessFailed();
                checkProcess4Timeout();
            } catch (Exception e) {
                logger.error("workflow state wheel thread check error:", e);
            }
            ThreadUtil.sleepAtLeastIgnoreInterrupts(stateCheckIntervalSecs);
        }
    }


    private void checkProcess4Timeout() {
        if (processInstanceTimeoutCheckList.isEmpty()) {
            return;
        }
        for (ProcessInstance processInstance : this.processInstanceTimeoutCheckList.values()) {

            long timeRemain = DateUtils.getRemainTime(processInstance.getStartTime(), processInstance.getTimeout() * Constants.SEC_2_MINUTES_TIME_UNIT);
            if (timeRemain < 0) {
                addProcessTimeoutEvent(processInstance);
                processInstanceTimeoutCheckList.remove(processInstance.getId());
            }
        }
    }

    private void check4StartProcessFailed() {
        if (startProcessFailedMap.isEmpty()) {
            return;
        }
        for (WorkflowExecuteThread workflowExecuteThread : this.startProcessFailedMap.values()) {
            this.startProcessFailedMap.remove(workflowExecuteThread.getProcessInstance().getId());

            if (!this.startProcessFailedRetryCountMaps.containsKey(workflowExecuteThread.getProcessInstance().getId())) {
                this.startProcessFailedRetryCountMaps.put(workflowExecuteThread.getProcessInstance().getId(), 0);
            }
            int tryTimes = this.startProcessFailedRetryCountMaps.get(workflowExecuteThread.getProcessInstance().getId()) + 1;
            // 超过最大重试次数
            if (tryTimes > Constants.DEFAULT_RETRY_START_PROCESS_MAX_TIMES) {

                this.startProcessFailedRetryCountMaps.remove(workflowExecuteThread.getProcessInstance().getId());

                ProcessInstance processInstance = workflowExecuteThread.getProcessInstance();
                if (null != workflowExecuteThread && workflowExecuteThread.isStart()) {
                    processInstance.setState(ExecutionStatus.READY_STOP);
                    processInstance.setEndTime(new Date());
                    this.processService.updateProcessInstance(processInstance);

                    StateEvent stateEvent = new StateEvent();
                    stateEvent.setType(StateEventType.PROCESS_STATE_CHANGE);
                    stateEvent.setProcessInstanceId(processInstance.getId());
                    stateEvent.setExecutionStatus(ExecutionStatus.READY_STOP);
                    workflowExecuteThread.addStateEvent(stateEvent);
                } else {
                    processInstance.setState(ExecutionStatus.FAILURE);
                    processInstance.setEndTime(new Date());
                    this.processService.updateProcessInstance(processInstance);
                    this.processService.updatePendingTaskInstance2Kill(processInstance);
                }

                String title = "工作流启动失败";
                this.processAlertManager.sendAlertProcessMessage(processInstance, title, null);
            } else {
                this.startProcessFailedRetryCountMaps.put(workflowExecuteThread.getProcessInstance().getId(), tryTimes);
                logger.info("Retry[{}/{}] to execute process instance {}", tryTimes, Constants.DEFAULT_RETRY_START_PROCESS_MAX_TIMES,
                        workflowExecuteThread.getProcessInstance().getId());
                masterExecService.execute(workflowExecuteThread);
//                this.failedStartProcessMap.put(workflowExecuteThread.getProcessInstance().getId(), workflowExecuteThread.getProcessInstance());
            }

        }
    }

    private boolean addProcessTimeoutEvent(ProcessInstance processInstance) {
        StateEvent stateEvent = new StateEvent();
        stateEvent.setType(StateEventType.PROCESS_TIMEOUT);
        stateEvent.setProcessInstanceId(processInstance.getId());
        addEvent(stateEvent);
        return true;
    }

    private void addEvent(StateEvent stateEvent) {
        if (!processInstanceExecMaps.containsKey(stateEvent.getProcessInstanceId())) {
            logger.error("Event [{}] cannot be handle, because cannot found process instance : {}", stateEvent.toString(), stateEvent.getProcessInstanceId());
            return;
        }
        WorkflowExecuteThread workflowExecuteThread = this.processInstanceExecMaps.get(stateEvent.getProcessInstanceId());
        workflowExecuteThread.addStateEvent(stateEvent);
    }
}
