package org.apache.dolphinscheduler.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 类型当前标签类型
 */
public enum ElementType {

    /**
     * 0 process definition
     * 1 process instance
     * 2 task definition
     * 3 task instance
     * 4 schedules
     * 5 command
     */
    PROCESSDEFINITION(0, "process_definition"),
    PROCESSINSTANCE(1, "process_instance"),
    TASKDEFINITION(2, "task_definition"),
//    TASKINSTANCE(3, "task_instance"),  // 取消,直接PROCESSINSTANCE的灰度状态即可
//    SCHEDULES(4, "schedules"),  // 取消,直接取PROCESSDEFINITION的灰度状态即可
    COMMAND(5, "command");

    ElementType(int code, String descp) {
        this.code = code;
        this.descp = descp;
    }

    @EnumValue
    private final int code;
    private final String descp;

    public int getCode() {
        return code;
    }

    public String getDescp() {
        return descp;
    }

}
