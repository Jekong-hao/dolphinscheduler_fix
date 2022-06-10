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

package org.apache.dolphinscheduler.api.service;

import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.enums.ReleaseState;
import org.apache.dolphinscheduler.dao.entity.ProcessDefinition;
import org.apache.dolphinscheduler.dao.entity.Project;
import org.apache.dolphinscheduler.dao.entity.TaskDefinitionLog;
import org.apache.dolphinscheduler.dao.entity.User;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartFile;

/**
 * process authority service
 */
public interface ProcessAuthorityService {

    Result queryProcessListPaging(User loginUser,
                                   long projectCode,
                                   String searchVal,
                                   Integer userId,
                                   Integer pageNo,
                                   Integer pageSize);
    /**
     * query authorized user
     *
     * @param loginUser     login user
     * @param code   process code
     * @return users    who have permission for the specified process
     */
    Map<String, Object> queryAuthorizedUser(User loginUser, Long code);

    /**
     * query unauthorized user
     *
     * @param loginUser     login user
     * @param code   process code
     * @return users    who not have permission for the specified process
     */
    Map<String, Object> queryUnauthorizedUser(User loginUser, Long code);

    boolean hasProcessAndPerm(User loginUser, ProcessDefinition processDefinition, Map<String, Object> result);

    Result queryProcessListPaging(User loginUser, String searchVal, Integer pageNo, Integer pageSize);
}

