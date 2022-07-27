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

package org.apache.dolphinscheduler.api.controller;

import io.swagger.annotations.*;
import org.apache.dolphinscheduler.api.aspect.AccessLogAnnotation;
import org.apache.dolphinscheduler.api.enums.ExecuteType;
import org.apache.dolphinscheduler.api.exceptions.ApiException;
import org.apache.dolphinscheduler.api.service.DependComplementService;
import org.apache.dolphinscheduler.api.service.WorkFlowLineageService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.enums.*;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.common.utils.ParameterUtils;
import org.apache.dolphinscheduler.dao.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

import static org.apache.dolphinscheduler.api.enums.Status.*;
import static org.apache.dolphinscheduler.common.Constants.SESSION_USER;

/**
 * depend complement controller
 */
@Api(tags = "DEPEND COMPLEMENT TAG")
@RestController
@RequestMapping("projects/{projectCode}/depend-complement")
public class DependComplementController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(DependComplementController.class);

    @Autowired
    private WorkFlowLineageService workFlowLineageService;

    @Autowired
    private DependComplementService dependComplementService;

    @ApiOperation(value = "queryDependComplementLineageByCode", notes = "QUERY_DEPEND_COMPLEMENT_LINEAGE_BY_CODES_NOTES")
    @GetMapping(value = "/lineage/{workFlowCode}")
    @ResponseStatus(HttpStatus.OK)
    @AccessLogAnnotation(ignoreRequestArgs = "loginUser")
    public Result<Map<String, Object>> queryDependComplementLineageByCode(
            @ApiIgnore @RequestAttribute(value = SESSION_USER) User loginUser,
            @ApiParam(name = "projectCode", value = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable(value = "workFlowCode", required = true) long workFlowCode) {
        try {
            Map<String, Object> result = workFlowLineageService.queryDependComplementLineageByCode(loginUser, projectCode, workFlowCode);
            return returnDataList(result);
        } catch (Exception e) {
            logger.error(QUERY_WORKFLOW_LINEAGE_ERROR.getMsg(), e);
            return error(QUERY_WORKFLOW_LINEAGE_ERROR.getCode(), QUERY_WORKFLOW_LINEAGE_ERROR.getMsg());
        }
    }

    @PostMapping(value = "/start-depend-complement")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(START_DEPEND_COMPLEMENT_ERROR)
    @AccessLogAnnotation(ignoreRequestArgs = "loginUser")
    public Result startDependComplement(@ApiIgnore @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                        @ApiParam(name = "projectCode", value = "PROJECT_CODE", required = true) @PathVariable long projectCode,
                                        @RequestParam(value = "startProcessDefinitionCode", required = true) Long startProcessDefinitionCode,
                                        @RequestParam(value = "startProcessDefinitionName", required = true) String startProcessDefinitionName,
                                        @RequestParam(value = "scheduleTime", required = false) String scheduleTime,
                                        @RequestParam(value = "execType", required = false) CommandType execType,
                                        @RequestParam(value = "dependComplementProcessJson", required = false) String dependComplementProcessJson,
                                        @RequestParam(value = "startParams", required = false) String startParams,
                                        @RequestParam(value = "parallism", required = false, defaultValue = "1") int parallism,
                                        @RequestParam(value = "failureStrategy", required = true) FailureStrategy failureStrategy,
                                        @RequestParam(value = "workerGroup", required = false, defaultValue = "default") String workerGroup,
                                        @RequestParam(value = "environmentCode", required = false, defaultValue = "-1") Long environmentCode,
                                        @RequestParam(value = "warningType", required = true) WarningType warningType,
                                        @RequestParam(value = "warningGroupId", required = false) int warningGroupId) {

        Map<String, String> startParamMap = null;
        if (startParams != null) {
            startParamMap = JSONUtils.toMap(startParams);
        }
        Map<String, Object> result = dependComplementService.startDependComplement(loginUser, projectCode,
                startProcessDefinitionCode, startProcessDefinitionName,
                scheduleTime, execType, dependComplementProcessJson, startParamMap, failureStrategy,
                workerGroup, environmentCode, warningType, warningGroupId);
        return returnDataList(result);
    }

    @PostMapping(value = "/execute")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(EXECUTE_DEPEND_COMPLEMENT_ERROR)
    @AccessLogAnnotation(ignoreRequestArgs = "loginUser")
    public Result execute(@ApiIgnore @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                          @ApiParam(name = "projectCode", value = "PROJECT_CODE", required = true) @PathVariable long projectCode,
                          @RequestParam(value = "dependComplementId") Integer dependComplementId,
                          @RequestParam(value = "dependComplementDetailId", required = false) Integer dependComplementDetailId,
                          @RequestParam(value = "processInstanceId", required = false) Integer processInstanceId,
                          @RequestParam(value = "dependComplementExecuteTarget") DependComplementExecuteTarget dependComplementExecuteTarget,
                          @RequestParam(value = "executeType") ExecuteType executeType) {

        Map<String, Object> result = dependComplementService.execute(loginUser, projectCode,
                dependComplementId, dependComplementDetailId, processInstanceId,
                dependComplementExecuteTarget, executeType);
        return returnDataList(result);
    }

    @ApiOperation(value = "queryDependComplementListPaging", notes = "QUERY_DEPEND_COMPLEMENT_LIST_PAGING_NOTES")
    @GetMapping(value = "")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_DEPEMD_COMPLEMENT_LIST_PAGING_ERROR)
    @AccessLogAnnotation(ignoreRequestArgs = "loginUser")
    public Result queryDependComplementListPaging(
            @ApiIgnore @RequestAttribute(value = SESSION_USER) User loginUser,
            @ApiParam(name = "projectCode", value = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @RequestParam(value = "searchVal", required = false) String searchVal,
            @RequestParam(value = "userId", required = false, defaultValue = "0") Integer userId,
            @RequestParam("pageNo") Integer pageNo,
            @RequestParam("pageSize") Integer pageSize) {

        Result result = checkPageParams(pageNo, pageSize);
        if (!result.checkResult()) {
            return result;
        }
        searchVal = ParameterUtils.handleEscapes(searchVal);

        return dependComplementService.queryDependComplementListPaging(loginUser, projectCode, searchVal, userId, pageNo, pageSize);
    }

    @ApiOperation(value = "queryDependComplementDetailListByDependComplementIdPaging", notes = "QUERY_DEPEND_COMPLEMENT_DETAIL_LIST_BY_DEPEDN_COMPLEMENT_ID_PAGING_NOTES")
    @GetMapping(value = "/detail")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_DEPEMD_COMPLEMENT_DETAIL_LIST_PAGING_ERROR)
    @AccessLogAnnotation(ignoreRequestArgs = "loginUser")
    public Result queryDependComplementDetailListByDependComplementIdPaging(@ApiIgnore @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                                                            @ApiParam(name = "projectCode", value = "PROJECT_CODE", required = true) @PathVariable long projectCode,
                                                                            @RequestParam(value = "pageNo") int pageNo,
                                                                            @RequestParam(value = "pageSize") int pageSize,
                                                                            @RequestParam(value = "dependComplementId") int dependComplementId) {

        Result result = checkPageParams(pageNo, pageSize);
        if (!result.checkResult()) {
            return result;
        }
        result = dependComplementService.queryDependComplementDetailListByDependComplementIdPaging(loginUser, projectCode, pageNo, pageSize, dependComplementId);

        return result;
    }

    @ApiOperation(value = "queryDependComplementDetailProcessListByTwoId", notes = "QUERY_DEPEND_COMPLEMENT_DETAIL_PROCESS_LIST_BY_TWO_ID_NOTES")
    @GetMapping(value = "/detail/process")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_DEPEMD_COMPLEMENT_DETAIL_LIST_PAGING_ERROR)
    @AccessLogAnnotation(ignoreRequestArgs = "loginUser")
    public Result queryDependComplementDetailProcessListByTwoId(@ApiIgnore @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                                                @ApiParam(name = "projectCode", value = "PROJECT_CODE", required = true) @PathVariable long projectCode,
                                                                @RequestParam(value = "dependComplementId") int dependComplementId,
                                                                @RequestParam(value = "dependComplementDetailId") int dependComplementDetailId) {

        Map<String, Object> result = dependComplementService.queryDependComplementDetailProcessListByTwoId(loginUser, projectCode, dependComplementId, dependComplementDetailId);
        return returnDataList(result);
    }
}
