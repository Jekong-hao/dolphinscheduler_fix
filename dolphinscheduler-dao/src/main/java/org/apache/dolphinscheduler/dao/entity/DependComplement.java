package org.apache.dolphinscheduler.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.dolphinscheduler.common.enums.*;
import com.fasterxml.jackson.annotation.JsonFormat;


import java.util.Date;

@TableName("t_ds_depend_complement")
public class DependComplement {

    @TableId(value = "id", type = IdType.AUTO)
    private int id;

    @TableField("project_code")
    private Long projectCode;

    @TableField("start_process_definition_code")
    private Long startProcessDefinitionCode;

    @TableField("start_process_definition_name")
    private String startProcessDefinitionName;

    @TableField("schedule_start_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date scheduleStartDate;

    @TableField("schedule_end_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date scheduleEndDate;

    @TableField("command_type")
    private CommandType commandType;

    @TableField("depend_complement_process_json")
    private String dependComplementProcessJson;

    @TableField("depend_complement_param")
    private String dependComplementParam;

    @TableField("executor_id")
    private int executorId;

    @TableField("state")
    private ExecutionStatus state;

    @TableField("failure_strategy")
    private FailureStrategy failureStrategy;

    @TableField("worker_group")
    private String workerGroup;

    @TableField("environment_code")
    private Long environmentCode;

    @TableField("warning_type")
    private WarningType warningType;

    @TableField("warning_group_id")
    private int warningGroupId;

    @TableField("active")
    private DependComplementActiveType active;

    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private int perm;;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(Long projectCode) {
        this.projectCode = projectCode;
    }

    public Long getStartProcessDefinitionCode() {
        return startProcessDefinitionCode;
    }

    public void setStartProcessDefinitionCode(Long startProcessDefinitionCode) {
        this.startProcessDefinitionCode = startProcessDefinitionCode;
    }

    public String getStartProcessDefinitionName() {
        return startProcessDefinitionName;
    }

    public void setStartProcessDefinitionName(String startProcessDefinitionName) {
        this.startProcessDefinitionName = startProcessDefinitionName;
    }

    public Date getScheduleStartDate() {
        return scheduleStartDate;
    }

    public void setScheduleStartDate(Date scheduleStartDate) {
        this.scheduleStartDate = scheduleStartDate;
    }

    public Date getScheduleEndDate() {
        return scheduleEndDate;
    }

    public void setScheduleEndDate(Date scheduleEndDate) {
        this.scheduleEndDate = scheduleEndDate;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }

    public String getDependComplementProcessJson() {
        return dependComplementProcessJson;
    }

    public void setDependComplementProcessJson(String dependComplementProcessJson) {
        this.dependComplementProcessJson = dependComplementProcessJson;
    }

    public String getDependComplementParam() {
        return dependComplementParam;
    }

    public void setDependComplementParam(String dependComplementParam) {
        this.dependComplementParam = dependComplementParam;
    }

    public int getExecutorId() {
        return executorId;
    }

    public void setExecutorId(int executorId) {
        this.executorId = executorId;
    }

    public ExecutionStatus getState() {
        return state;
    }

    public void setState(ExecutionStatus state) {
        this.state = state;
    }

    public FailureStrategy getFailureStrategy() {
        return failureStrategy;
    }

    public void setFailureStrategy(FailureStrategy failureStrategy) {
        this.failureStrategy = failureStrategy;
    }

    public String getWorkerGroup() {
        return workerGroup;
    }

    public void setWorkerGroup(String workerGroup) {
        this.workerGroup = workerGroup;
    }

    public Long getEnvironmentCode() {
        return environmentCode;
    }

    public void setEnvironmentCode(Long environmentCode) {
        this.environmentCode = environmentCode;
    }

    public WarningType getWarningType() {
        return warningType;
    }

    public void setWarningType(WarningType warningType) {
        this.warningType = warningType;
    }

    public int getWarningGroupId() {
        return warningGroupId;
    }

    public void setWarningGroupId(int warningGroupId) {
        this.warningGroupId = warningGroupId;
    }

    public DependComplementActiveType getActive() {
        return active;
    }

    public void setActive(DependComplementActiveType active) {
        this.active = active;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getPerm() {
        return perm;
    }

    public void setPerm(int perm) {
        this.perm = perm;
    }
}
