package org.apache.dolphinscheduler.server.master.runner;

import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.enums.ExecutionStatus;
import org.apache.dolphinscheduler.common.enums.StateEvent;
import org.apache.dolphinscheduler.common.enums.StateEventType;
import org.apache.dolphinscheduler.common.enums.TimeoutFlag;
import org.apache.dolphinscheduler.common.thread.Stopper;
import org.apache.dolphinscheduler.common.utils.DateUtils;
import org.apache.dolphinscheduler.dao.entity.ProcessInstance;
import org.apache.dolphinscheduler.dao.entity.TaskInstance;
import org.apache.dolphinscheduler.service.process.ProcessService;
import org.apache.hadoop.util.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * workflow task 状态检查
 *
 * @author 林振杰
 * @email linzhenjie@aulton.com
 * @date 2022/4/22 1:57 下午
 * @Copyright Copyright (c)  aulton Inc. All Rights Reserved.
 **/
public class TaskStateWheelExecuteThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(TaskStateWheelExecuteThread.class);

    private ProcessService processService;
    private ConcurrentHashMap<Integer, TaskInstance> taskInstanceTimeoutCheckList;
    private ConcurrentHashMap<Integer, TaskInstance> taskInstanceRetryCheckList;
    private WorkflowExecuteThread workflowExecuteThread;

    private int stateCheckIntervalSecs;

    public TaskStateWheelExecuteThread(ProcessService processService,
                                       ConcurrentHashMap<Integer, TaskInstance> taskInstanceTimeoutCheckList,
                                       ConcurrentHashMap<Integer, TaskInstance> taskInstanceRetryCheckList,
                                       WorkflowExecuteThread workflowExecuteThread,
                                       int stateCheckIntervalSecs) {
        this.processService = processService;
        this.workflowExecuteThread = workflowExecuteThread;
        this.taskInstanceTimeoutCheckList = taskInstanceTimeoutCheckList;
        this.taskInstanceRetryCheckList = taskInstanceRetryCheckList;
        this.stateCheckIntervalSecs = stateCheckIntervalSecs;
    }

    @Override
    public void run() {
        logger.info("[process instance {}] master task state wheel thread start.",
                this.workflowExecuteThread.getProcessInstance().getId());
        while (Stopper.isRunning()) {
            try {
                checkTask4Timeout();
                checkTask4Retry();
            } catch (Exception e) {
                logger.error("[process instance {}] master task state wheel thread check error: {}",
                        this.workflowExecuteThread.getProcessInstance().getId(),
                        e);
            }
            ThreadUtil.sleepAtLeastIgnoreInterrupts(stateCheckIntervalSecs);
        }
    }

    public void checkTask4Timeout() {
        if (taskInstanceTimeoutCheckList.isEmpty()) {
            return;
        }
        for (TaskInstance taskInstance : taskInstanceTimeoutCheckList.values()) {
            if (TimeoutFlag.OPEN == taskInstance.getTaskDefine().getTimeoutFlag()) {
                if (taskInstance.getStartTime() == null) {
                    TaskInstance newTaskInstance = processService.findTaskInstanceById(taskInstance.getId());
                    taskInstance.setStartTime(newTaskInstance.getStartTime());
                }
                long timeRemain = DateUtils.getRemainTime(taskInstance.getStartTime(), taskInstance.getTaskDefine().getTimeout() * Constants.SEC_2_MINUTES_TIME_UNIT);
                if (timeRemain < 0) {
                    addTaskTimeoutEvent(taskInstance);
                    taskInstanceTimeoutCheckList.remove(taskInstance.getId());
                }
            }
        }
    }

    private void checkTask4Retry() {
        if (taskInstanceRetryCheckList.isEmpty()) {
            return;
        }

        for (TaskInstance taskInstance : this.taskInstanceRetryCheckList.values()) {
            if (!taskInstance.getState().typeIsFinished() && (taskInstance.isSubProcess() || taskInstance.isDependTask())) {
                addTaskStateChangeEvent(taskInstance);
            } else if (taskInstance.taskCanRetry() && taskInstance.retryTaskIntervalOverTime()) {
                addTaskStateChangeEvent(taskInstance);
                taskInstanceRetryCheckList.remove(taskInstance.getId());
            }
        }
    }

    private boolean addTaskStateChangeEvent(TaskInstance taskInstance) {
        StateEvent stateEvent = new StateEvent();
        stateEvent.setType(StateEventType.TASK_STATE_CHANGE);
        stateEvent.setProcessInstanceId(taskInstance.getProcessInstanceId());
        stateEvent.setTaskInstanceId(taskInstance.getId());
        stateEvent.setExecutionStatus(ExecutionStatus.RUNNING_EXECUTION);
        addEvent(stateEvent);
        return true;
    }

    private boolean addTaskTimeoutEvent(TaskInstance taskInstance) {
        StateEvent stateEvent = new StateEvent();
        stateEvent.setType(StateEventType.TASK_TIMEOUT);
        stateEvent.setProcessInstanceId(taskInstance.getProcessInstanceId());
        stateEvent.setTaskInstanceId(taskInstance.getId());
        addEvent(stateEvent);
        return true;
    }

    private void addEvent(StateEvent stateEvent) {
        if (this.workflowExecuteThread.getProcessInstance().getId() != stateEvent.getProcessInstanceId()) {
            logger.error("Event [{}] cannot be handle, because cannot found process instance : {}", stateEvent.toString(), stateEvent.getProcessInstanceId());
            return;
        }

        this.workflowExecuteThread.addStateEvent(stateEvent);
    }
}
