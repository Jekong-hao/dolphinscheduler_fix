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

package org.apache.dolphinscheduler.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.dolphinscheduler.common.enums.ElementType;
import org.apache.dolphinscheduler.common.enums.GrayFlag;

import java.util.Date;

/**
 * gray relation
 */
@TableName("t_ds_relation_gray")
public class GrayRelation {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private int id;

    /**
     * 0 process definition
     * 1 process instance
     * 2 task instance
     */
    private ElementType elementType;

    /**
     * process definition, process instance, task definition, command 的id字段
     */
    private Integer elementId;

    /**
     * process definition, process instance, task definition, command 的code字段
     */
    private Long elementCode;

    /**
     * gray flag
     */
    private GrayFlag grayFlag;

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

    public GrayRelation() {
    }

    public GrayRelation(
            ElementType elementType,
            Integer elementId,
            Long elementCode) {
        this.setElementType(elementType);
        this.setElementId(elementId);
        this.setElementCode(elementCode);
        Date date = new Date();
        this.createTime = date;
        this.updateTime = date;
    }

    public GrayRelation(int id,
                        ElementType elementType,
                        Integer elementId,
                        Long elementCode,
                        GrayFlag grayFlag,
                        Date createTime,
                        Date updateTime) {
        this.id = id;
        this.elementType = elementType;
        this.elementId = elementId;
        this.elementCode = elementCode;
        this.grayFlag = grayFlag;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public void setElementType(ElementType elementType) {
        this.elementType = elementType;
    }

    public Integer getElementId() {
        return elementId;
    }

    public void setElementId(Integer elementId) {
        this.elementId = elementId;
    }

    public Long getElementCode() {
        return elementCode;
    }

    public void setElementCode(Long elementCode) {
        this.elementCode = elementCode;
    }

    public GrayFlag getGrayFlag() {
        return grayFlag;
    }

    public void setGrayFlag(GrayFlag grayFlag) {
        this.grayFlag = grayFlag;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GrayRelation that = (GrayRelation) o;

        if (id != that.id) {
            return false;
        }
        if (elementId != that.elementId) {
            return false;
        }
        if (elementCode != that.elementCode) {
            return false;
        }
        if (elementType != that.elementType) {
            return false;
        }
        if (grayFlag != that.grayFlag) {
            return false;
        }
        if (!createTime.equals(that.createTime)) {
            return false;
        }
        return updateTime.equals(that.updateTime);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + elementType.hashCode();
        result = 31 * result + elementId;
        result = 31 * result + (int) (elementCode ^ (elementCode >>> 32));
        result = 31 * result + grayFlag.hashCode();
        result = 31 * result + createTime.hashCode();
        result = 31 * result + updateTime.hashCode();
        return result;
    }
}
