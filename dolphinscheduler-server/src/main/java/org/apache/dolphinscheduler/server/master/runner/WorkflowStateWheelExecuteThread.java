package org.apache.dolphinscheduler.server.master.runner;

import com.google.common.collect.Maps;
import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.enums.StateEvent;
import org.apache.dolphinscheduler.common.enums.StateEventType;
import org.apache.dolphinscheduler.common.thread.Stopper;
import org.apache.dolphinscheduler.common.utils.DateUtils;
import org.apache.dolphinscheduler.dao.entity.ProcessInstance;
import org.apache.hadoop.util.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private ConcurrentHashMap<Integer, ProcessInstance> failedStartProcessMap;

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

    public WorkflowStateWheelExecuteThread(ConcurrentHashMap<Integer, ProcessInstance> processInstanceTimeoutCheckList,
                                           ConcurrentHashMap<Integer, WorkflowExecuteThread> processInstanceExecMaps,
                                           ConcurrentHashMap<Integer, ProcessInstance> failedStartProcessMap,
                                           ConcurrentHashMap<Integer, WorkflowExecuteThread> startProcessFailedMap,
                                           int stateCheckIntervalSecs) {
        this.processInstanceTimeoutCheckList = processInstanceTimeoutCheckList;
        this.processInstanceExecMaps = processInstanceExecMaps;
        this.failedStartProcessMap = failedStartProcessMap;
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
            if (!this.startProcessFailedRetryCountMaps.containsKey(workflowExecuteThread.getProcessInstance().getId())) {
                this.startProcessFailedRetryCountMaps.put(workflowExecuteThread.getProcessInstance().getId(), 0);
            }
            int tryTimes = this.startProcessFailedRetryCountMaps.get(workflowExecuteThread.getProcessInstance().getId()) + 1;
            if (tryTimes > Constants.DEFAULT_RETRY_START_PROCESS_MAX_TIMES) {
//                // TODO 重试失败后剔除
//                // TODO 重试失败告警
//                ProcessInstance processInstance = workflowExecuteThread.getProcessInstance();
//                processInstance.setState(ExecutionStatus.FAILURE);
//                processInstance.setEndTime(new Date());
//
//                this.startProcessFailedMap.remove(processInstance.getId());
                if (this.failedStartProcessMap.containsKey(workflowExecuteThread.getProcessInstance().getId())) {
                    this.failedStartProcessMap.put(workflowExecuteThread.getProcessInstance().getId(), workflowExecuteThread.getProcessInstance());
//                    this.startProcessFailedMap.remove(workflowExecuteThread.getProcessInstance().getId());
                }
            } else {
                this.startProcessFailedRetryCountMaps.put(workflowExecuteThread.getProcessInstance().getId(), tryTimes);
                logger.info("Retry[{}/{}] to execute process instance {}", tryTimes, Constants.DEFAULT_RETRY_START_PROCESS_MAX_TIMES,
                        workflowExecuteThread.getProcessInstance().getId());
                masterExecService.execute(workflowExecuteThread);
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
