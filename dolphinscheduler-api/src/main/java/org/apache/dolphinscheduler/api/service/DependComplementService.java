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

import org.apache.dolphinscheduler.api.enums.ExecuteType;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.enums.*;
import org.apache.dolphinscheduler.dao.entity.User;

import java.util.Map;

public interface DependComplementService {

    Map<String, Object> startDependComplement(User loginUser, long projectCode,
                                             Long startProcessDefinitionCode, String startProcessDefinitionName,
                                             String cronTime, CommandType commandType, String dependComplementProcessJson,
                                             Map<String, String> startParams, FailureStrategy failureStrategy,
                                             String workerGroup, Long environmentCode,
                                             WarningType warningType, int warningGroupId);

    Result queryDependComplementListPaging(User loginUser,
                                           long projectCode,
                                           String searchVal,
                                           Integer userId,
                                           Integer pageNo,
                                           Integer pageSize);

    Result queryDependComplementDetailListByDependComplementIdPaging(User loginUser,
                                                                    long projectCode,
                                                                    int pageNo,
                                                                    int pageSize,
                                                                    int dependComplementId);

    Map<String, Object> queryDependComplementDetailProcessListByTwoId(User loginUser,
                                                                      long projectCode,
                                                                      int dependComplementId,
                                                                      int dependComplementDetailId);

    Map<String, Object> execute(User loginUser,
                                long projectCode,
                                Integer dependComplementId,
                                Integer dependComplementDetailId,
                                Integer processInstanceId,
                                DependComplementExecuteTarget dependComplementExecuteTarget,
                                ExecuteType executeType);

}
