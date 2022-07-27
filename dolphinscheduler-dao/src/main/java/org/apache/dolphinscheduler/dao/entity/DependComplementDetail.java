package org.apache.dolphinscheduler.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.dolphinscheduler.common.enums.*;

import java.util.Date;

@TableName("t_ds_depend_complement_detail")
public class DependComplementDetail {

    @TableId(value = "id", type = IdType.AUTO)
    private int id;

    @TableField("depend_complement_id")
    private int dependComplementId;

    @TableField("schedule_start_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date scheduleStartDate;

    @TableField("schedule_end_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date scheduleEndDate;

    @TableField("state")
    private ExecutionStatus state;

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
