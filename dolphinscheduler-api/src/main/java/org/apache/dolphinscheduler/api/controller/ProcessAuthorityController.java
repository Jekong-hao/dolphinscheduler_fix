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
import org.apache.dolphinscheduler.api.exceptions.ApiException;
import org.apache.dolphinscheduler.api.service.ProcessAuthorityService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.utils.ParameterUtils;
import org.apache.dolphinscheduler.dao.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

import static org.apache.dolphinscheduler.api.enums.Status.QUERY_AUTHORIZED_USER;
import static org.apache.dolphinscheduler.api.enums.Status.QUERY_PROCESS_DEFINITION_LIST_PAGING_ERROR;

/**
 * process definition controller
 */
@Api(tags = "PROCESS_AUTHORITY_TAG")
@RestController
@RequestMapping("process-authority")
public class ProcessAuthorityController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessDefinitionController.class);

    @Autowired
    private ProcessAuthorityService processAuthorityService;

    /**
     * query process list paging used for authorization
     *
     * @param loginUser login user
     * @param searchVal search value
     * @param pageNo page number
     * @param pageSize page size
     * @return process definition page
     */
    @ApiOperation(value = "queryListPaging", notes = "QUERY_PROCESS_LIST_PAGING_NOTES")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "searchVal", value = "SEARCH_VAL", required = false, type = "String"),
        @ApiImplicitParam(name = "pageNo", value = "PAGE_NO", required = true, dataType = "Int", example = "1"),
        @ApiImplicitParam(name = "pageSize", value = "PAGE_SIZE", required = true, dataType = "Int", example = "10")
    })
    @GetMapping("/list-paging")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROCESS_DEFINITION_LIST_PAGING_ERROR)
    @AccessLogAnnotation(ignoreRequestArgs = "loginUser")
    public Result queryProcessListPaging(@ApiIgnore @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                         @RequestParam(value = "searchVal", required = false) String searchVal,
                                         @RequestParam("pageNo") Integer pageNo,
                                         @RequestParam("pageSize") Integer pageSize) {
        Result result = checkPageParams(pageNo, pageSize);
        if (!result.checkResult()) {
            return result;
        }
        searchVal = ParameterUtils.handleEscapes(searchVal);

        return processAuthorityService.queryProcessListPaging(loginUser, searchVal, pageNo, pageSize);
    }

    /**
     * query process list paging used for authorization
     *
     * @param loginUser login user
     * @param projectCode project code
     * @param searchVal search value
     * @param pageNo page number
     * @param pageSize page size
     * @param userId user id
     * @return process definition page
     */
    @ApiOperation(value = "queryListPaging", notes = "QUERY_PROCESS_LIST_PAGING_NOTES")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "searchVal", value = "SEARCH_VAL", required = false, type = "String"),
        @ApiImplicitParam(name = "userId", value = "USER_ID", required = false, dataType = "Int", example = "100"),
        @ApiImplicitParam(name = "pageNo", value = "PAGE_NO", required = true, dataType = "Int", example = "1"),
        @ApiImplicitParam(name = "pageSize", value = "PAGE_SIZE", required = true, dataType = "Int", example = "10")
    })
    @GetMapping("/project-list-paging")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROCESS_DEFINITION_LIST_PAGING_ERROR)
    @AccessLogAnnotation(ignoreRequestArgs = "loginUser")
    public Result queryProcessListPaging(@ApiIgnore @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
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

        return processAuthorityService.queryProcessListPaging(loginUser, projectCode, searchVal, userId, pageNo, pageSize);
    }

    /**
     * query authorized user
     *
     * @param loginUser     login user
     * @param code   process code
     * @return users        who have permission for the specified project
     */
    @ApiOperation(value = "queryAuthorizedUser", notes = "QUERY_AUTHORIZED_USER_NOTES")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "code", value = "PROCESS_DEFINITION_CODE", required = true, dataType = "Long", example = "100")
    })
    @GetMapping(value = "/authed-user")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_AUTHORIZED_USER)
    @AccessLogAnnotation(ignoreRequestArgs = "loginUser")
    public Result queryAuthorizedUser(@ApiIgnore @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                      @RequestParam("code") Long code) {
        Map<String, Object> result = this.processAuthorityService.queryAuthorizedUser(loginUser, code);
        return this.returnDataList(result);
    }

    /**
     * query unauthorized user
     *
     * @param loginUser     login user
     * @param code   process code
     * @return users        who not have permission for the specified project
     */
    @ApiOperation(value = "queryUnauthorizedUser", notes = "QUERY_UNAUTHORIZED_USER_NOTES")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "code", value = "PROCESS_DEFINITION_CODE", required = true, dataType = "Long", example = "100")
    })
    @GetMapping(value = "/unauth-user")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_AUTHORIZED_USER)
    @AccessLogAnnotation(ignoreRequestArgs = "loginUser")
    public Result queryUnauthorizedUser(@ApiIgnore @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
                                        @RequestParam("code") Long code) {
        Map<String, Object> result = this.processAuthorityService.queryUnauthorizedUser(loginUser, code);
        return this.returnDataList(result);
    }

}
