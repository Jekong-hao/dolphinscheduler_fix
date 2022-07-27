package org.apache.dolphinscheduler.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.dolphinscheduler.common.enums.GrayFlag;
import org.apache.dolphinscheduler.common.enums.ReleaseState;

import java.util.Date;

/**
 * ProcessDependenceDetailVO
 */
public class DependComplementLineageVO {

    /**
     * 序号
     */
    private int level;

    /**
     * 工作流名称
     */
    private String processName;

    /**
     * 工作流编码
     */
    private long processCode;

    /**
     * release state : online/offline
     */
    private ReleaseState releaseState;

    /**
     * gray flag : gray/no_gray
     */
    private GrayFlag grayFlag;

    /**
     * permission
     */
    private int perm;

    /**
     * create time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * update time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * user name/所属用户
     */
    private String userName;

    /**
     * project name/项目名称
     */
    private String projectName;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public long getProcessCode() {
        return processCode;
    }

    public void setProcessCode(long processCode) {
        this.processCode = processCode;
    }

    public ReleaseState getReleaseState() {
        return releaseState;
    }

    public void setReleaseState(ReleaseState releaseState) {
        this.releaseState = releaseState;
    }

    public GrayFlag getGrayFlag() {
        return grayFlag;
    }

    public void setGrayFlag(GrayFlag grayFlag) {
        this.grayFlag = grayFlag;
    }

    public int getPerm() {
        return perm;
    }

    public void setPerm(int perm) {
        this.perm = perm;
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

}
