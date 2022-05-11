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

package org.apache.dolphinscheduler.server.master.processor;

import org.apache.dolphinscheduler.common.enums.ExecutionStatus;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.remote.command.Command;
import org.apache.dolphinscheduler.remote.command.CommandType;
import org.apache.dolphinscheduler.remote.command.TaskKillResponseCommand;
import org.apache.dolphinscheduler.remote.processor.NettyRequestProcessor;
import org.apache.dolphinscheduler.server.master.processor.queue.TaskResponseEvent;
import org.apache.dolphinscheduler.server.master.processor.queue.TaskResponseService;
import org.apache.dolphinscheduler.server.master.runner.WorkflowExecuteThread;
import org.apache.dolphinscheduler.service.bean.SpringApplicationContext;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.netty.channel.Channel;

/**
 *  task response processor
 */
public class TaskKillResponseProcessor implements NettyRequestProcessor {

    private final Logger logger = LoggerFactory.getLogger(TaskKillResponseProcessor.class);

    /**
     * process service
     */
    private final TaskResponseService taskResponseService;

    public TaskKillResponseProcessor() {
        this.taskResponseService = SpringApplicationContext.getBean(TaskResponseService.class);
    }

    public void init(ConcurrentHashMap<Integer, WorkflowExecuteThread> processInstanceExecMaps) {
        this.taskResponseService.init(processInstanceExecMaps);
    }

    /**
     * task final result response
     * need master process , state persistence
     *
     * @param channel channel
     * @param command command
     */
    @Override
    public void process(Channel channel, Command command) {
        Preconditions.checkArgument(CommandType.TASK_KILL_RESPONSE == command.getType(), String.format("invalid command type : %s", command.getType()));

        TaskKillResponseCommand responseCommand = JSONUtils.parseObject(command.getBody(), TaskKillResponseCommand.class);
//        logger.info("received task kill response command : {}", responseCommand);
        logger.info("[process instance {}] master received kill task {} response, with command {}",
                responseCommand.getProcessInstanceId(),
                responseCommand.getTaskInstanceId(),
                responseCommand);
        // TaskResponseEvent
        TaskResponseEvent taskResponseEvent = TaskResponseEvent.newActionStop(ExecutionStatus.of(responseCommand.getStatus()),
                responseCommand.getTaskInstanceId(),
                responseCommand.getProcessInstanceId()
        );
        taskResponseService.addResponse(taskResponseEvent);
    }

}
