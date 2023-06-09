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

package org.apache.dolphinscheduler.service.alert;

import org.apache.commons.lang.StringUtils;
import org.apache.dolphinscheduler.common.enums.CommandType;
import org.apache.dolphinscheduler.common.enums.Flag;
import org.apache.dolphinscheduler.common.enums.WarningType;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.AlertDao;
import org.apache.dolphinscheduler.dao.entity.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.dolphinscheduler.dao.mapper.UserMapper;
import org.apache.dolphinscheduler.service.process.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * process alert manager
 */
@Component
public class ProcessAlertManager {

    /**
     * logger of AlertManager
     */
    private static final Logger logger = LoggerFactory.getLogger(ProcessAlertManager.class);

    /**
     * alert dao
     */
    @Autowired
    private AlertDao alertDao;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProcessService processService;


    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String DEFAULT_NEW_LINE_CHAR = "\n";


    /**
     * command type convert chinese
     *
     * @param commandType command type
     * @return command name
     */
    private String getCommandCnName(CommandType commandType) {
        switch (commandType) {
            case RECOVER_TOLERANCE_FAULT_PROCESS:
                return "recover tolerance fault process";
            case RECOVER_SUSPENDED_PROCESS:
                return "recover suspended process";
            case START_CURRENT_TASK_PROCESS:
                return "start current task process";
            case START_FAILURE_TASK_PROCESS:
                return "start failure task process";
            case START_PROCESS:
                return "【手动调度】";
            case REPEAT_RUNNING:
                return "repeat running";
            case SCHEDULER:
                return "【定时调度】";
            case COMPLEMENT_DATA:
                return "【补数调度】";
            case PAUSE:
                return "pause";
            case STOP:
                return "stop";
            default:
                return "unknown type";
        }
    }

    /**
     * get process instance content
     *
     * @param processInstance process instance
     * @param taskInstances task instance list
     * @return process instance format content
     */
    public String getContentProcessInstance(ProcessInstance processInstance,
                                            List<TaskInstance> taskInstances,
                                            ProjectUser projectUser) {

        StringBuffer content = processInstanceAlertContent(processInstance, projectUser);
        content.append("失败任务列表: [").append(taskInstances.stream().filter(t -> t.getState().typeIsFailure()).map(t -> t.getName()).collect(Collectors.joining("; "))).append("]").append(DEFAULT_NEW_LINE_CHAR);

        return content.toString();

        /**
         * 【手动调度】失败
         * 项目：xxx
         * 工作流：xxxx
         * 工作流实例： xxxx
         * 调度日期： xxxx
         * 开始时间： xxx
         * 结束时间： xxx
         * 失败任务列表： [xxxx,xxx]
         * 查询详情：http://xxxxx
         */


    }

    /**
     * getting worker fault tolerant content
     *
     * @param processInstance process instance
     * @param toleranceTaskList tolerance task list
     * @return worker tolerance content
     */
    private String getWorkerToleranceContent(ProcessInstance processInstance, List<TaskInstance> toleranceTaskList) {

        List<MyProcessAlertContent> toleranceTaskInstanceList = new ArrayList<>();

        for (TaskInstance taskInstance : toleranceTaskList) {
            MyProcessAlertContent processAlertContent = MyProcessAlertContent.newBuilder()
                    .processId(processInstance.getId())
                    .processDefinitionCode(processInstance.getProcessDefinitionCode())
                    .processName(processInstance.getName())
                    .taskCode(taskInstance.getTaskCode())
                    .taskName(taskInstance.getName())
                    .taskHost(taskInstance.getHost())
                    .retryTimes(taskInstance.getRetryTimes())
                    .build();
            toleranceTaskInstanceList.add(processAlertContent);
        }
        return  JSONUtils.formatJson(JSONUtils.toJsonString(toleranceTaskInstanceList));
    }

    /**
     * send worker alert fault tolerance
     *
     * @param processInstance process instance
     * @param toleranceTaskList tolerance task list
     */
    public void sendAlertWorkerToleranceFault(ProcessInstance processInstance, List<TaskInstance> toleranceTaskList) {
        try {
            Alert alert = new Alert();
            alert.setTitle("worker fault tolerance");
            String content = getWorkerToleranceContent(processInstance, toleranceTaskList);
            alert.setContent(content);
            alert.setCreateTime(new Date());
            alert.setAlertGroupId(processInstance.getWarningGroupId() == null ? 1 : processInstance.getWarningGroupId());
            alertDao.addAlert(alert);
            logger.info("add alert to db , alert : {}", alert);

        } catch (Exception e) {
            logger.error("send alert failed:{} ", e.getMessage());
        }

    }

    /**
     * send process instance alert
     *
     * @param processInstance process instance
     * @param taskInstances task instance list
     */
    public void sendAlertProcessInstance(ProcessInstance processInstance,
                                         List<TaskInstance> taskInstances,
                                         ProjectUser projectUser) {

        if (!isNeedToSendWarning(processInstance)) {
            return;
        }

        int alertGroupId = processInstance.getWarningGroupId();
        String cmdName = getCommandCnName(processInstance.getCommandType());
        String success = processInstance.getState().typeIsSuccess() ? "成功" : "失败";
        String title = cmdName + " " + success;
        String content = getContentProcessInstance(processInstance, taskInstances,projectUser);

        saveMessage2DB(title, content, alertGroupId);
    }

    /**
     * 发送指定消息
     * @param processInstance
     * @param title
     */
    public void sendAlertProcessMessage(ProcessInstance processInstance, String title) {
        sendAlertProcessMessage(processInstance, title, null);
    }

    /**
     * 发送指定消息
     * @param processInstance
     * @param title
     * @param errorMessage
     */
    public void sendAlertProcessMessage(ProcessInstance processInstance, String title, String errorMessage) {
        int alertGroupId = (processInstance.getWarningGroupId() == null || processInstance.getWarningGroupId() == 0) ? 1 : processInstance.getWarningGroupId();
        StringBuffer content = processInstanceAlertContent(processInstance);

        if (StringUtils.isNotEmpty(errorMessage)) {
            content.append("ERROR MESSAGE : ").append(errorMessage).append(DEFAULT_NEW_LINE_CHAR);
        }

        saveMessage2DB(title, content.toString(), alertGroupId);
    }

    /**
     * check if need to be send warning
     *
     * @param processInstance
     * @return
     */
    public boolean isNeedToSendWarning(ProcessInstance processInstance) {
        if (Flag.YES == processInstance.getIsSubProcess()) {
            return false;
        }
        boolean sendWarning = false;
        WarningType warningType = processInstance.getWarningType();
        switch (warningType) {
            case ALL:
                if (processInstance.getState().typeIsFinished()) {
                    sendWarning = true;
                }
                break;
            case SUCCESS:
                if (processInstance.getState().typeIsSuccess()) {
                    sendWarning = true;
                }
                break;
            case FAILURE:
                if (processInstance.getState().typeIsFailure()) {
                    sendWarning = true;
                }
                break;
            default:
        }
        return sendWarning;
    }

    /**
     * send process timeout alert
     *
     * @param processInstance process instance
     * @param processDefinition process definition
     */
    public void sendProcessTimeoutAlert(ProcessInstance processInstance, ProcessDefinition processDefinition) {
//        alertDao.sendProcessTimeoutAlert(processInstance, processDefinition);

        String title = "工作流超时告警";
        StringBuffer content = processInstanceAlertContent(processInstance);
        saveMessage2DB(title, content.toString(), processInstance.getWarningGroupId());
    }

    /**
     * 任务超时告警
     * @param processInstance
     * @param taskInstance
     */
    public void sendTaskTimeoutAlert(ProcessInstance processInstance, TaskInstance taskInstance) {
        try {
            String title = "任务超时告警";
            StringBuffer content = processInstanceAlertContent(processInstance);
            TaskDefinition taskDefinition =  taskInstance.getTaskDefine();
            if (null == taskDefinition) {
                taskDefinition = processService.findTaskDefinition(taskInstance.getTaskCode(), taskInstance.getTaskDefinitionVersion());
            }
            content.append("超时任务：").append(taskDefinition.getName()).append(DEFAULT_NEW_LINE_CHAR);
            content.append("任务开始时间：").append(taskInstance.getStartTime()==null?"":df.format(taskInstance.getStartTime())).append(DEFAULT_NEW_LINE_CHAR);
            content.append("任务结算时间：").append(taskInstance.getEndTime()==null?"":df.format(taskInstance.getEndTime())).append(DEFAULT_NEW_LINE_CHAR);
            content.append("任务运行状态：").append(taskInstance.getState().getDescp()).append(DEFAULT_NEW_LINE_CHAR);

            saveMessage2DB(title, content.toString(), processInstance.getWarningGroupId());
        } catch (Exception e) {
            logger.warn("Send alert message failure.", e);
        }

    }

    /**
     * 任务失败告警
     * @param processInstance
     * @param taskInstance
     */
    public void sendTaskFailureAlert(ProcessInstance processInstance, TaskInstance taskInstance) {
        try {
            String title = "任务失败告警";
            StringBuffer content = processInstanceAlertContent(processInstance);
            TaskDefinition taskDefinition =  taskInstance.getTaskDefine();
            if (null == taskDefinition) {
                taskDefinition = processService.findTaskDefinition(taskInstance.getTaskCode(), taskInstance.getTaskDefinitionVersion());
            }
            content.append("失败任务：").append(taskDefinition.getName()).append(DEFAULT_NEW_LINE_CHAR);
            content.append("任务开始时间：").append(taskInstance.getStartTime()==null?"":df.format(taskInstance.getStartTime())).append(DEFAULT_NEW_LINE_CHAR);
            content.append("任务结算时间：").append(taskInstance.getEndTime()==null?"":df.format(taskInstance.getEndTime())).append(DEFAULT_NEW_LINE_CHAR);
            content.append("任务运行状态：").append(taskInstance.getState().getDescp()).append(DEFAULT_NEW_LINE_CHAR);

            saveMessage2DB(title, content.toString(), processInstance.getWarningGroupId());
        } catch (Exception e) {
            logger.warn("Send alert message failure.", e);
        }

    }

    private StringBuffer processInstanceAlertContent(ProcessInstance processInstance){
        ProjectUser projectUser = processService.queryProjectWithUserByProcessInstanceId(processInstance.getId());
        return processInstanceAlertContent(processInstance, projectUser);
    }

    private StringBuffer processInstanceAlertContent(ProcessInstance processInstance, ProjectUser projectUser) {
        StringBuffer content = new StringBuffer();
        if (null == processInstance) {
            content.append("UNEXCEPT：工作流实例为空!");
        } else {
            User user = userMapper.queryByProcessDefinitionCode(processInstance.getProcessDefinitionCode());
            ProcessDefinition processDefinition = processService.findProcessDefinition(processInstance.getProcessDefinitionCode(), processInstance.getProcessDefinitionVersion());

            if (null != projectUser) {
                content.append("项目：").append(projectUser.getProjectName()).append(DEFAULT_NEW_LINE_CHAR);
            }
            content.append("工作流：").append(processDefinition.getName()).append(DEFAULT_NEW_LINE_CHAR)
                    .append("所有者：").append(user.getUserName()).append(DEFAULT_NEW_LINE_CHAR)
                    .append("工作流实例：").append(processInstance.getName()).append(DEFAULT_NEW_LINE_CHAR)
                    .append("调度执行命令：").append(getCommandCnName(processInstance.getCommandType())).append(DEFAULT_NEW_LINE_CHAR)
                    .append("运行状态：").append(processInstance.getState().getDescp()).append(DEFAULT_NEW_LINE_CHAR)
                    .append("调度日期：").append(processInstance.getScheduleTime()==null?"":df.format(processInstance.getScheduleTime())).append(DEFAULT_NEW_LINE_CHAR)
                    .append("开始时间：").append(processInstance.getStartTime()==null?"":df.format(processInstance.getStartTime())).append(DEFAULT_NEW_LINE_CHAR)
                    .append("结束时间：").append(processInstance.getEndTime()==null?"":df.format(processInstance.getEndTime())).append(DEFAULT_NEW_LINE_CHAR);

        }
        return content;

    }

    private void saveMessage2DB(String title, String content, int alertGroupId) {
        Alert alert = new Alert();
        alert.setTitle(title);
        alert.setContent(content);
        alert.setAlertGroupId(alertGroupId);
        alert.setCreateTime(new Date());
        alertDao.addAlert(alert);
        logger.info("add alert to db , alert: {}", alert);
    }
}
