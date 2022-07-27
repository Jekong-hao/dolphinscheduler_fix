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

package org.apache.dolphinscheduler.api.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.dolphinscheduler.api.enums.ExecuteType;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.service.DependComplementService;
import org.apache.dolphinscheduler.api.service.MonitorService;
import org.apache.dolphinscheduler.api.service.ProjectService;
import org.apache.dolphinscheduler.api.utils.PageInfo;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.api.vo.DependComplementInstanceVO;
import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.enums.*;
import org.apache.dolphinscheduler.common.model.Server;
import org.apache.dolphinscheduler.common.utils.DateUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.*;
import org.apache.dolphinscheduler.dao.mapper.*;
import org.apache.dolphinscheduler.service.process.ProcessService;
import org.apache.dolphinscheduler.service.quartz.cron.CronUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.apache.dolphinscheduler.api.enums.Status.DEPEND_COMPLEMENT_JSON_PARAMS_ERROR;
import static org.apache.dolphinscheduler.common.Constants.*;

/**
 * depend complement impl
 */
@Service
public class DependComplementServiceImpl extends BaseServiceImpl implements DependComplementService {

    private static final Logger logger = LoggerFactory.getLogger(DependComplementServiceImpl.class);

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DependComplementMapper dependComplementMapper;

    @Autowired
    private DependComplementDetailMapper dependComplementDetailMapper;

    @Autowired
    private DependComplementDetailProcessMapper dependComplementDetailProcessMapper;

    @Autowired
    private ProcessUserMapper processUserMapper;

    @Autowired
    private MonitorService monitorService;

    @Autowired
    private ProcessService processService;

    @Override
    public Map<String, Object> startDependComplement(User loginUser, long projectCode,
                                                    Long startProcessDefinitionCode, String startProcessDefinitionName,
                                                    String cronTime, CommandType commandType, String dependComplementProcessJson,
                                                    Map<String, String> startParams, FailureStrategy failureStrategy,
                                                    String workerGroup, Long environmentCode,
                                                    WarningType warningType, int warningGroupId) {
        Project project = projectMapper.queryByCode(projectCode);
        //check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode);
        if (result.get(Constants.STATUS) != Status.SUCCESS) {
            return result;
        }
        // check master exists
        if (!checkMasterExists(result)) {
            return result;
        }
        // 依赖补数任务下,依赖补数参数不能为空字符串或者不能为空数组
        final ArrayNode jsonNodes;
        try {
            jsonNodes = JSONUtils.parseArray(dependComplementProcessJson);
        } catch (Exception e) {
            // json有问题
            putMsg(result, DEPEND_COMPLEMENT_JSON_PARAMS_ERROR);
            return result;
        }
        if ("".equals(dependComplementProcessJson) || jsonNodes.size() == 0) {
            putMsg(result, Status.DEPEND_COMPLEMENT_PARAMS_EMPTY_ERROR);
            return result;
        }
        if ("".equals(cronTime) || cronTime.split(",").length != 2) {
            putMsg(result, Status.DEPEND_COMPLEMENT_PARAMS_SCHEDULE_EMPTY_ERROR);
            return result;
        }

        int createDetail = this.createDependComplement(projectCode, startProcessDefinitionCode, startProcessDefinitionName,
                cronTime, commandType, dependComplementProcessJson,
                startParams, loginUser.getId(), failureStrategy, workerGroup, environmentCode,
                warningType, warningGroupId);

        if (createDetail > 0) {
            putMsg(result, Status.SUCCESS);
        } else {
            putMsg(result, Status.START_DEPEND_COMPLEMENT_ERROR);
        }
        return result;
    }

    @Override
    public Result queryDependComplementListPaging(User loginUser, long projectCode, String searchVal, Integer userId, Integer pageNo, Integer pageSize) {
        Result result = new Result();
        Project project = projectMapper.queryByCode(projectCode);
        //check user access for project
        Map<String, Object> checkResult = projectService.checkProjectAndAuth(loginUser, project, projectCode);
        Status resultStatus = (Status) checkResult.get(Constants.STATUS);
        if (resultStatus != Status.SUCCESS) {
            putMsg(result, resultStatus);
            return result;
        }

        final Page<DependComplement> page = new Page<>(pageNo, pageSize);
        final IPage<DependComplement> dependComplementIPage = dependComplementMapper.queryDependComplementListPaging(
                page, searchVal, userId, project.getCode(), isAdmin(loginUser));

        final List<DependComplement> dependComplements = dependComplementIPage.getRecords();
        for (DependComplement dependComplement : dependComplements) {
            final String dependComplementProcessJson = dependComplement.getDependComplementProcessJson();
            final String processDefinitionName = JSONUtils.parseArray(dependComplementProcessJson).get(0).get(PROCESS_NAME).asText();
            dependComplement.setStartProcessDefinitionName(processDefinitionName);

            // 用于对于工作流的操作权限设置
            final ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(dependComplement.getStartProcessDefinitionCode());
            ProcessUser processUser = processUserMapper.queryProcessRelation(processDefinition.getId(), loginUser.getId());
            if (project.getUserId() == loginUser.getId() || processDefinition.getUserId() == loginUser.getId() || loginUser.getUserType() == UserType.ADMIN_USER || processUser != null) {
                dependComplement.setPerm(Constants.ALL_PERMISSIONS);
            } else {
                dependComplement.setPerm(Constants.READ_PERMISSION);
            }
        }

        dependComplementIPage.setRecords(dependComplements);
        PageInfo<DependComplement> pageInfo = new PageInfo<>(pageNo, pageSize);
        pageInfo.setTotal((int) dependComplementIPage.getTotal());
        pageInfo.setTotalList(dependComplementIPage.getRecords());
        result.setData(pageInfo);
        putMsg(result, Status.SUCCESS);

        return result;
    }

    @Override
    public Result queryDependComplementDetailListByDependComplementIdPaging(User loginUser, long projectCode, int pageNo, int pageSize, int dependComplementId) {
        Result result = new Result();
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> checkResult = projectService.checkProjectAndAuth(loginUser, project, projectCode);
        Status resultStatus = (Status) checkResult.get(Constants.STATUS);
        if (resultStatus != Status.SUCCESS) {
            putMsg(result, resultStatus);
            return result;
        }

        Page<DependComplementDetail> page = new Page<>(pageNo, pageSize);
        final IPage<DependComplementDetail> dependComplementDetailIPage = dependComplementDetailMapper.queryDependComplementDetailListByDependComplementIdPaging(page, projectCode, dependComplementId);
        final List<DependComplementDetail> dependComplementDetails = dependComplementDetailIPage.getRecords();
        PageInfo<DependComplementInstanceVO> pageInfo = new PageInfo<>(pageNo, pageSize);

        // 封装输出
        final List<DependComplementInstanceVO> dependComplementInstanceVOS = new ArrayList<>();
        for (DependComplementDetail dependComplementDetail : dependComplementDetails) {
            final DependComplement dependComplement = dependComplementMapper.selectById(dependComplementDetail.getDependComplementId());
            // 创建输出元素
            final DependComplementInstanceVO dependComplementInstanceVO = new DependComplementInstanceVO();
            // 数据赋值
            dependComplementInstanceVO.setId(dependComplementDetail.getId());
            dependComplementInstanceVO.setCode(String.format("%s-%d", DEPEND, dependComplementDetail.getId()));
            dependComplementInstanceVO.setProcessDefinitionCode(dependComplement.getStartProcessDefinitionCode());
            dependComplementInstanceVO.setProcessDefinitionName(dependComplement.getStartProcessDefinitionName());
            dependComplementInstanceVO.setScheduleStartDate(dependComplementDetail.getScheduleStartDate());
            dependComplementInstanceVO.setScheduleEndDate(dependComplementDetail.getScheduleEndDate());
            dependComplementInstanceVO.setState(dependComplementDetail.getState());
            dependComplementInstanceVO.setCreateTime(dependComplementDetail.getCreateTime());
            dependComplementInstanceVO.setUpdateTime(dependComplementDetail.getUpdateTime());
            dependComplementInstanceVO.setDependComplementId(dependComplementDetail.getDependComplementId());
            dependComplementInstanceVO.setDependComplementDetailId(dependComplementDetail.getId());
            dependComplementInstanceVO.setHasChildren(true);
            dependComplementInstanceVO.setFlag(DEPEND_DETAIL);

            // 用于对于工作流的操作权限设置
            final ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(dependComplement.getStartProcessDefinitionCode());
            ProcessUser processUser = processUserMapper.queryProcessRelation(processDefinition.getId(), loginUser.getId());
            if (project.getUserId() == loginUser.getId() || processDefinition.getUserId() == loginUser.getId() || loginUser.getUserType() == UserType.ADMIN_USER || processUser != null) {
                dependComplementInstanceVO.setPerm(Constants.ALL_PERMISSIONS);
            } else {
                dependComplementInstanceVO.setPerm(Constants.READ_PERMISSION);
            }

            dependComplementInstanceVOS.add(dependComplementInstanceVO);
        }
        pageInfo.setTotalList(dependComplementInstanceVOS);
        pageInfo.setTotal((int) dependComplementDetailIPage.getTotal());
        result.setData(pageInfo);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Map<String, Object> queryDependComplementDetailProcessListByTwoId(User loginUser,
                                                                             long projectCode,
                                                                             int dependComplementId,
                                                                             int dependComplementDetailId) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }

        final List<DependComplementDetailProcess> dependComplementDetailProcesses = dependComplementDetailProcessMapper.queryDependComplementDetailProcessListByTwoId(dependComplementId, dependComplementDetailId);

        // 封装输出
        final List<DependComplementInstanceVO> dependComplementInstanceVOS = new ArrayList<>();
        for (DependComplementDetailProcess dependComplementDetailProcess : dependComplementDetailProcesses) {
            final DependComplementDetail dependComplementDetail = dependComplementDetailMapper.selectById(dependComplementDetailProcess.getDependComplementDetailId());
            // 创建输出元素
            final DependComplementInstanceVO dependComplementInstanceVO = new DependComplementInstanceVO();
            // 数据赋值
            dependComplementInstanceVO.setId(dependComplementDetailProcess.getId());
            dependComplementInstanceVO.setCode(String.format("%s-%d", PROCESS, dependComplementDetailProcess.getId()));
            dependComplementInstanceVO.setLevel(dependComplementDetailProcess.getLevel());
            dependComplementInstanceVO.setProcessDefinitionCode(dependComplementDetailProcess.getProcessDefinitionCode());
            dependComplementInstanceVO.setProcessDefinitionName(dependComplementDetailProcess.getProcessDefinitionName());
            dependComplementInstanceVO.setScheduleStartDate(dependComplementDetail.getScheduleStartDate());
            dependComplementInstanceVO.setScheduleEndDate(dependComplementDetail.getScheduleEndDate());
            dependComplementInstanceVO.setState(dependComplementDetailProcess.getState());
            dependComplementInstanceVO.setCreateTime(dependComplementDetailProcess.getCreateTime());
            dependComplementInstanceVO.setUpdateTime(dependComplementDetailProcess.getUpdateTime());
            dependComplementInstanceVO.setDependComplementId(dependComplementDetailProcess.getDependComplementId());
            dependComplementInstanceVO.setDependComplementDetailId(dependComplementDetailProcess.getDependComplementDetailId());
            dependComplementInstanceVO.setHasChildren(false);
            dependComplementInstanceVO.setFlag(DEPEND_DETAIL_PROCESS);

            // 用于对于工作流的操作权限设置
            final ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(dependComplementDetailProcess.getProcessDefinitionCode());
            ProcessUser processUser = processUserMapper.queryProcessRelation(processDefinition.getId(), loginUser.getId());
            if (project.getUserId() == loginUser.getId() || processDefinition.getUserId() == loginUser.getId() || loginUser.getUserType() == UserType.ADMIN_USER || processUser != null) {
                dependComplementInstanceVO.setPerm(Constants.ALL_PERMISSIONS);
            } else {
                dependComplementInstanceVO.setPerm(Constants.READ_PERMISSION);
            }

            dependComplementInstanceVO.setProcessInstanceId(dependComplementDetailProcess.getProcessInstanceId());

            dependComplementInstanceVOS.add(dependComplementInstanceVO);
        }
        result.put(DATA_LIST, dependComplementInstanceVOS);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Map<String, Object> execute(User loginUser,
                                       long projectCode,
                                       Integer dependComplementId,
                                       Integer dependComplementDetailId,
                                       Integer processInstanceId,
                                       DependComplementExecuteTarget dependComplementExecuteTarget,
                                       ExecuteType executeType) {
        Project project = projectMapper.queryByCode(projectCode);
        // check user access for project
        Map<String, Object> result = projectService.checkProjectAndAuth(loginUser, project, projectCode);
        if (result.get(STATUS) != Status.SUCCESS) {
            return result;
        }

        // check master exists
        if (!checkMasterExists(result)) {
            return result;
        }

        DependComplement dependComplement = processService.findDependComplementById(dependComplementId);
        if (dependComplement == null) {
            putMsg(result, Status.DEPEND_COMPLEMENT_NOT_EXIST, dependComplementId);
            return result;
        }

        switch (dependComplementExecuteTarget) {
            case DEPEND_COMPLEMENT:
                // 对依赖补数作业,执行指定指令, 目前只有停止指令
                if (executeType == ExecuteType.STOP) {
                    // 变更dependComplement的状态
                    dependComplement.setState(ExecutionStatus.READY_STOP);
                    processService.updateDependComplementStateById(dependComplement.getId(), dependComplement.getState());

                    // 查询此dependComplement下所有的detail
                    final List<DependComplementDetail> dependComplementDetails
                            = processService.queryDependComplementDetailListByDependComplementId(dependComplementId);

                    // 标记detail为ready_stop, 并停止dependComplement下所有detail的中包含的process
                    for (DependComplementDetail dependComplementDetail : dependComplementDetails) {
                        // 标记detail为停止状态
                        dependComplementDetail.setState(ExecutionStatus.READY_STOP);
                        processService.updateDependComplementDetailStateById(dependComplementDetail.getId(), dependComplementDetail.getState());
                    }
                }
                break;
            case DEPEND_COMPLEMENT_DETAIL:
                // 对依赖补数作业的详情作业,执行指定指令
                DependComplementDetail dependComplementDetail = processService.findDependComplementDetailById(dependComplementDetailId);
                if(dependComplementDetail == null) {
                    putMsg(result, Status.DEPEND_COMPLEMENT_DETAIL_NOT_EXIST, dependComplementDetailId);
                    return result;
                }
                if (executeType == ExecuteType.STOP) {
                    // 标记detail为ready_stop, 并停止detail的中包含的process
                    // 标记detail为停止状态
                    dependComplementDetail.setState(ExecutionStatus.READY_STOP);
                    processService.updateDependComplementDetailStateById(dependComplementDetail.getId(), dependComplementDetail.getState());
                }
                break;
            default:
                // 不可能发生此种情况
                putMsg(result, Status.EXECUTE_DEPEND_COMPLEMENT_ERROR);
                break;
        }
        return result;
    }

    private int createDependComplement(long projectCode, Long startProcessDefinitionCode, String startProcessDefinitionName,
                                       String schedule, CommandType commandType, String dependComplementProcessJson,
                                       Map<String, String> startParams, int executorId,
                                       FailureStrategy failureStrategy, String workerGroup, Long environmentCode,
                                       WarningType warningType, int warningGroupId) {

        DependComplement dependComplement = new DependComplement();

        dependComplement.setProjectCode(projectCode);
        dependComplement.setStartProcessDefinitionCode(startProcessDefinitionCode);
        dependComplement.setStartProcessDefinitionName(startProcessDefinitionName);

        Date scheduleStartDate = null;
        Date scheduleEndDate = null;
        if (!StringUtils.isEmpty(schedule)) {
            String[] interval = schedule.split(",");
            if (interval.length == 2) {
                scheduleStartDate = DateUtils.getScheduleDate(interval[0]);
                scheduleEndDate = DateUtils.getScheduleDate(interval[1]);
                if (scheduleStartDate == null || scheduleEndDate == null || scheduleStartDate.after(scheduleEndDate)) {
                    logger.info("complement data error, wrong date start:{} and end date:{} ",
                            scheduleStartDate, scheduleEndDate);
                    return 0;
                }
            }
        }
        dependComplement.setScheduleStartDate(scheduleStartDate);
        dependComplement.setScheduleEndDate(scheduleEndDate);
        dependComplement.setCommandType(commandType);
        dependComplement.setDependComplementProcessJson(dependComplementProcessJson);

        Map<String, String> cmdParam = new HashMap<>();
        if (startParams != null && startParams.size() > 0) {
            cmdParam.put(CMD_PARAM_START_PARAMS, JSONUtils.toJsonString(startParams));
        }
        dependComplement.setDependComplementParam(JSONUtils.toJsonString(cmdParam));

        dependComplement.setExecutorId(executorId);
        dependComplement.setState(ExecutionStatus.SUBMITTED_SUCCESS);

        if (failureStrategy != null) {
            dependComplement.setFailureStrategy(failureStrategy);
        }

        dependComplement.setWorkerGroup(workerGroup);
        dependComplement.setEnvironmentCode(environmentCode);

        if (warningType != null) {
            dependComplement.setWarningType(warningType);
        }

        dependComplement.setWarningGroupId(warningGroupId);

        // 创建depend Complement 信息
        final int dependComplementId = createDependComplement(dependComplement);
        // 创建depend Complement 详情信息
        final int createDependComplementDetail = createDependComplementDetail(dependComplement.getId(), dependComplement);

        if (dependComplementId > 0 && createDependComplementDetail > 0) {
            return 1;
        }
        return 0;
    }

    private int createDependComplement(DependComplement dependComplement) {
        return dependComplementMapper.insertAndReturnId(dependComplement);
    }

    private int createDependComplementDetail(int dependComplementId, DependComplement dependComplement) {

        final Date scheduleStartDate = dependComplement.getScheduleStartDate();
        final Date scheduleEndDate = dependComplement.getScheduleEndDate();

        LinkedList<Date> listDate = new LinkedList<>();
        if (scheduleStartDate.equals(scheduleEndDate)) {
            listDate.add(scheduleStartDate);
        } else {
            listDate.add(scheduleStartDate);
            listDate.addAll(CronUtils.getSelfFireDateList(scheduleStartDate, scheduleEndDate, Constants.DEFAULT_CRON_STRING));
            listDate.add(scheduleEndDate);
        }

        int createCount = listDate.size();
        if (!CollectionUtils.isEmpty(listDate)) {
            for (int i = 0; i < createCount; i++) {
                final DependComplementDetail dependComplementDetail = new DependComplementDetail();
                dependComplementDetail.setDependComplementId(dependComplementId);
                dependComplementDetail.setScheduleStartDate(listDate.get(i));
                dependComplementDetail.setScheduleEndDate(listDate.get(i));
                dependComplementDetail.setState(ExecutionStatus.SUBMITTED_SUCCESS);
                dependComplementDetailMapper.insert(dependComplementDetail);
            }
        }
        logger.info("create complement command count: {}", createCount);
        return createCount;
    }

    /**
     * check whether master exists
     *
     * @param result result
     * @return master exists return true , otherwise return false
     */
    private boolean checkMasterExists(Map<String, Object> result) {
        // check master server exists
        List<Server> masterServers = monitorService.getServerListFromRegistry(true);

        // no master
        if (masterServers.isEmpty()) {
            putMsg(result, Status.MASTER_NOT_EXISTS);
            return false;
        }
        return true;
    }

}
