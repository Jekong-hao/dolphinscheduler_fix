package org.apache.dolphinscheduler.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.dolphinscheduler.common.enums.*;

import java.util.Date;

public class DependComplementInstanceVO {

    /**
     * 用户传参,不显示
     */
    private Integer id;

    /**
     * depend-编号
     * process-编号
     */
    private String code;

    /**
     * 工作流的层级
     */
    private Integer level;

    /**
     * 传参用,不显示
     * 起始工作流编码
     * 工作流编码
     */
    private long processDefinitionCode;

    /**
     * 起始工作流名称
     * 工作流名称
     */
    private String processDefinitionName;

    /**
     * 调度开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date scheduleStartDate;

    /**
     * 调度结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date scheduleEndDate;

    /**
     * 执行状态
     */
    private ExecutionStatus state;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 传参用,不显示
     */
    private int dependComplementId;

    /**
     * 传参用,不显示
     */
    private int dependComplementDetailId;

    /**
     * 传参用,不显示
     */
    private boolean hasChildren;

    /**
     * 传参用,不显示
     * 用于区分是显示depend-complement-instance-detail的还是depend-complement-instance-detail-process的
     */
    private String flag;

    /**
     * 传参用,不显示
     * 权限标签
     */
    private int perm;

    /**
     * 传参用,不显示
     * 工作流实例id
     */
    private int processInstanceId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public long getProcessDefinitionCode() {
        return processDefinitionCode;
    }

    public void setProcessDefinitionCode(long processDefinitionCode) {
        this.processDefinitionCode = processDefinitionCode;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
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

    public ExecutionStatus getState() {
        return state;
    }

    public void setState(ExecutionStatus state) {
        this.state = state;
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

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public int getPerm() {
        return perm;
    }

    public void setPerm(int perm) {
        this.perm = perm;
    }

    public int getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(int processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
}
