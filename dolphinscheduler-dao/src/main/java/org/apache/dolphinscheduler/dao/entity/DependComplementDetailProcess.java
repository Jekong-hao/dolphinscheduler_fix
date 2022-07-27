package org.apache.dolphinscheduler.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.dolphinscheduler.common.enums.DependComplementActiveType;
import org.apache.dolphinscheduler.common.enums.ExecutionStatus;
import org.apache.dolphinscheduler.common.enums.FailureStrategy;
import org.apache.dolphinscheduler.common.enums.WarningType;

import java.util.Date;

@TableName("t_ds_depend_complement_detail_process")
public class DependComplementDetailProcess {

    @TableId(value = "id", type = IdType.AUTO)
    private int id;

    @TableField("depend_complement_id")
    private int dependComplementId;

    @TableField("depend_complement_detail_id")
    private int dependComplementDetailId;

    @TableField("process_definition_code")
    private Long processDefinitionCode;

    @TableField("process_definition_version")
    private int processDefinitionVersion;

    @TableField("process_definition_name")
    private String processDefinitionName;

    @TableField("process_instance_id")
    private int processInstanceId;

    @TableField("level")
    private int level;

    @TableField("failure_strategy")
    private FailureStrategy failureStrategy;

    @TableField("warning_type")
    private WarningType warningType;

    @TableField("warning_group_id")
    private int warningGroupId;

    @TableField(exist = false)
    private ExecutionStatus state;

    @TableField("worker_group")
    private String workerGroup;

    @TableField("environment_code")
    private long environmentCode;

    @TableField("active")
    private DependComplementActiveType active;

    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @TableField(exist = false)
    private String startProcessDefinitionName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDependComplementId() {
        return dependComplementId;
    }

    public void setDependComplementId(int dependComplementId) {
        this.dependComplementId = dependComplementId;
    }

    public int getDependComplementDetailId() {
        return dependComplementDetailId;
    }

    public void setDependComplementDetailId(int dependComplementDetailId) {
        this.dependComplementDetailId = dependComplementDetailId;
    }

    public Long getProcessDefinitionCode() {
        return processDefinitionCode;
    }

    public void setProcessDefinitionCode(Long processDefinitionCode) {
        this.processDefinitionCode = processDefinitionCode;
    }

    public int getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(int processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    public int getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(int processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public FailureStrategy getFailureStrategy() {
        return failureStrategy;
    }

    public void setFailureStrategy(FailureStrategy failureStrategy) {
        this.failureStrategy = failureStrategy;
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

    public ExecutionStatus getState() {
        return state;
    }

    public void setState(ExecutionStatus state) {
        this.state = state;
    }

    public String getWorkerGroup() {
        return workerGroup;
    }

    public void setWorkerGroup(String workerGroup) {
        this.workerGroup = workerGroup;
    }

    public long getEnvironmentCode() {
        return environmentCode;
    }

    public void setEnvironmentCode(long environmentCode) {
        this.environmentCode = environmentCode;
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

    public String getStartProcessDefinitionName() {
        return startProcessDefinitionName;
    }

    public void setStartProcessDefinitionName(String startProcessDefinitionName) {
        this.startProcessDefinitionName = startProcessDefinitionName;
    }
}
