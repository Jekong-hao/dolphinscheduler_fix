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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.dolphinscheduler.api.dto.ProcessDefinitionNode;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.service.WorkFlowLineageService;
import org.apache.dolphinscheduler.api.vo.DependComplementLineageVO;
import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.enums.GrayFlag;
import org.apache.dolphinscheduler.common.enums.TaskType;
import org.apache.dolphinscheduler.common.enums.UserType;
import org.apache.dolphinscheduler.common.model.DependentItem;
import org.apache.dolphinscheduler.common.model.DependentTaskModel;
import org.apache.dolphinscheduler.common.task.dependent.DependentParameters;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.*;
import org.apache.dolphinscheduler.dao.mapper.*;
import org.apache.dolphinscheduler.spi.utils.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.apache.dolphinscheduler.common.enums.ElementType.PROCESSDEFINITION;

/**
 * work flow lineage service impl
 */
@Service
public class WorkFlowLineageServiceImpl extends BaseServiceImpl implements WorkFlowLineageService {
    @Autowired
    private WorkFlowLineageMapper workFlowLineageMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private TaskDefinitionLogMapper taskDefinitionLogMapper;

    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;

    @Autowired
    private GrayRelationMapper grayRelationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProcessUserMapper processUserMapper;

    @Override
    public Map<String, Object> queryWorkFlowLineageByName(long projectCode, String workFlowName) {
        Map<String, Object> result = new HashMap<>();
        Project project = projectMapper.queryByCode(projectCode);
        if (project == null) {
            putMsg(result, Status.PROJECT_NOT_FOUNT, projectCode);
            return result;
        }
        List<WorkFlowLineage> workFlowLineageList = workFlowLineageMapper.queryWorkFlowLineageByName(projectCode, workFlowName);
        result.put(Constants.DATA_LIST, workFlowLineageList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Map<String, Object> queryWorkFlowLineageByCode(long projectCode, long workFlowCode) {
        Map<String, Object> result = new HashMap<>();
        Project project = projectMapper.queryByCode(projectCode);
        if (project == null) {
            putMsg(result, Status.PROJECT_NOT_FOUNT, projectCode);
            return result;
        }
        Map<Long, WorkFlowLineage> workFlowLineagesMap = new HashMap<>();
        Set<WorkFlowRelation> workFlowRelations = new HashSet<>();
        Set<Long> sourceWorkFlowCodes = Sets.newHashSet(workFlowCode);
        recursiveWorkFlow(projectCode, workFlowLineagesMap, workFlowRelations, sourceWorkFlowCodes);
        Map<String, Object> workFlowLists = new HashMap<>();
        workFlowLists.put(Constants.WORKFLOW_LIST, workFlowLineagesMap.values());
        workFlowLists.put(Constants.WORKFLOW_RELATION_LIST, workFlowRelations);
        result.put(Constants.DATA_LIST, workFlowLists);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    private void recursiveWorkFlow(long projectCode,
                                   Map<Long, WorkFlowLineage> workFlowLineagesMap,
                                   Set<WorkFlowRelation> workFlowRelations,
                                   Set<Long> sourceWorkFlowCodes) {
        for (Long workFlowCode : sourceWorkFlowCodes) {
            WorkFlowLineage workFlowLineage = workFlowLineageMapper.queryWorkFlowLineageByCode(projectCode, workFlowCode);
            workFlowLineagesMap.put(workFlowCode, workFlowLineage);
            List<ProcessLineage> processLineages = workFlowLineageMapper.queryProcessLineageByCode(projectCode, workFlowCode);
            List<TaskDefinition> taskDefinitionList = new ArrayList<>();
            for (ProcessLineage processLineage : processLineages) {
                if (processLineage.getPreTaskCode() > 0) {
                    taskDefinitionList.add(new TaskDefinition(processLineage.getPreTaskCode(), processLineage.getPreTaskVersion()));
                }
                if (processLineage.getPostTaskCode() > 0) {
                    taskDefinitionList.add(new TaskDefinition(processLineage.getPostTaskCode(), processLineage.getPostTaskVersion()));
                }
            }
            sourceWorkFlowCodes = querySourceWorkFlowCodes(projectCode, workFlowCode, taskDefinitionList);
            if (sourceWorkFlowCodes.isEmpty()) {
                workFlowRelations.add(new WorkFlowRelation(0L, workFlowCode));
                return;
            } else {
                workFlowLineagesMap.get(workFlowCode).setSourceWorkFlowCode(StringUtils.join(sourceWorkFlowCodes, Constants.COMMA));
                sourceWorkFlowCodes.forEach(code -> workFlowRelations.add(new WorkFlowRelation(code, workFlowCode)));
                recursiveWorkFlow(projectCode, workFlowLineagesMap, workFlowRelations, sourceWorkFlowCodes);
            }
        }
    }

    @Override
    public Map<String, Object> queryWorkFlowLineage(long projectCode) {
        Map<String, Object> result = new HashMap<>();
        Project project = projectMapper.queryByCode(projectCode);
        if (project == null) {
            putMsg(result, Status.PROJECT_NOT_FOUNT, projectCode);
            return result;
        }
        Map<Long, WorkFlowLineage> workFlowLineagesMap = new HashMap<>();
        Set<WorkFlowRelation> workFlowRelations = new HashSet<>();
        // 查询项目中所有的边和节点
        queryProjectWorkFlowLineage(projectCode, workFlowLineagesMap, workFlowRelations);

        Map<String, Object> workFlowLists = new HashMap<>();
        workFlowLists.put(Constants.WORKFLOW_LIST, workFlowLineagesMap.values());
        workFlowLists.put(Constants.WORKFLOW_RELATION_LIST, workFlowRelations);
        result.put(Constants.DATA_LIST, workFlowLists);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Map<String, Object> queryWorkFlowLineageBeyondByCode(long projectCode, long workFlowCode) {
        // 因为无法通过依赖链条向后找到整个依赖,所以先获得整个项目的依赖关系图
        Map<String, Object> result = new HashMap<>();
        Project project = projectMapper.queryByCode(projectCode);
        if (project == null) {
            putMsg(result, Status.PROJECT_NOT_FOUNT, projectCode);
            return result;
        }
        Map<Long, WorkFlowLineage> workFlowLineagesMap = new HashMap<>();
        Set<WorkFlowRelation> workFlowRelations = new HashSet<>();
        // 查询项目中所有的边和节点
        queryProjectWorkFlowLineage(projectCode, workFlowLineagesMap, workFlowRelations);

        // 循环查找所有跟入参workFlowCode向后相关的Code
        Set<Long> sourceWorkFlowCodes = Sets.newHashSet(workFlowCode);
        final Set<Long> allWorkFlowCodes = Sets.newHashSet();
        final Set<WorkFlowRelation> workFlowRelationHashSets = Sets.newHashSet();

        while (sourceWorkFlowCodes.size() > 0) {
            final Set<Long> tmpSet = Sets.newHashSet();
            for (Long sourceWorkFlowCode : sourceWorkFlowCodes) {
                allWorkFlowCodes.add(sourceWorkFlowCode);
                final Map<Long, WorkFlowRelation> targetWorkFlowCodeAndRelationMap = workFlowRelations.stream().filter(item -> sourceWorkFlowCode == item.getSourceWorkFlowCode()).collect(Collectors.toMap(WorkFlowRelation::getTargetWorkFlowCode, Function.identity()));
                if (targetWorkFlowCodeAndRelationMap.size() > 0) {
                    tmpSet.addAll(targetWorkFlowCodeAndRelationMap.keySet());
                    workFlowRelationHashSets.addAll(targetWorkFlowCodeAndRelationMap.values());
                }
            }
            sourceWorkFlowCodes = tmpSet;
        }

        // 查找所有涉及到的工作流
        final Set<WorkFlowLineage> workFlowLineagesMapValues = workFlowLineagesMap.values().stream().filter(item -> allWorkFlowCodes.contains(item.getWorkFlowCode())).collect(Collectors.toSet());

        Map<String, Object> workFlowLists = new HashMap<>();
        workFlowLists.put(Constants.WORKFLOW_LIST, workFlowLineagesMapValues);
        workFlowLists.put(Constants.WORKFLOW_RELATION_LIST, workFlowRelationHashSets);
        result.put(Constants.DATA_LIST, workFlowLists);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Map<String, Object> queryDependComplementLineageByCode(User loginUser, long projectCode, long workFlowCode) {
        // 因为无法通过依赖链条向后找到整个依赖,所以先获得整个项目的依赖关系图
        Map<String, Object> result = new HashMap<>();
        Project project = projectMapper.queryByCode(projectCode);
        if (project == null) {
            putMsg(result, Status.PROJECT_NOT_FOUNT, projectCode);
            return result;
        }
        Map<Long, WorkFlowLineage> workFlowLineagesMap = new HashMap<>();
        Set<WorkFlowRelation> workFlowRelations = new HashSet<>();

        // 查询项目中所有的边和节点
        queryProjectWorkFlowLineage(projectCode, workFlowLineagesMap, workFlowRelations);
        // 在内存中创建图
        Map<Long, ProcessDefinitionNode> workFlowCodeAndNode = Maps.newHashMap();
        createGraph(workFlowCodeAndNode, workFlowLineagesMap, workFlowRelations);
        // 递归获取各个节点的串行关系
        List<HashMap<ProcessDefinitionNode, Integer>> serialRelations = new ArrayList<>();
        recursiveGetDependList(serialRelations, workFlowCodeAndNode.get(workFlowCode), new HashMap<>(), 1);
        // 串行关系转化成层次-set关系
        final Map<Integer, Set<Long>> levelSourceWorkFlowCodes = Maps.newHashMap();
        createLevelRelation(levelSourceWorkFlowCodes, serialRelations);
        // 删掉前面重复项,使依赖尽量后置,并调整层级断档
        deleteRepeatProcess(levelSourceWorkFlowCodes);

        // 查询所有工作流的数据,封装成vo返回
        final List<DependComplementLineageVO> levelSourceWorkFlowObjects = Lists.newArrayList();
        for (Integer levelTmp : levelSourceWorkFlowCodes.keySet()) {
            final Set<Long> processDefineCodes = levelSourceWorkFlowCodes.get(levelTmp);
            for (Long processDefineCode : processDefineCodes) {
                final DependComplementLineageVO dependComplementLineageVO = new DependComplementLineageVO();
                ProcessDefinition processDefinition = processDefinitionMapper.queryByCode(processDefineCode);
                if (processDefinition != null) {
                    dependComplementLineageVO.setProcessName(processDefinition.getName());
                    dependComplementLineageVO.setProcessCode(processDefinition.getCode());
                    dependComplementLineageVO.setCreateTime(processDefinition.getCreateTime());
                    dependComplementLineageVO.setUpdateTime(processDefinition.getUpdateTime());
                    dependComplementLineageVO.setReleaseState(processDefinition.getReleaseState());
                    dependComplementLineageVO.setCreateTime(processDefinition.getCreateTime());
                    dependComplementLineageVO.setUpdateTime(processDefinition.getUpdateTime());
                    dependComplementLineageVO.setProjectName(project.getName());

                    // 所属用户
                    final User user = userMapper.selectById(processDefinition.getUserId());
                    if (user != null) {
                        dependComplementLineageVO.setUserName(user.getUserName());
                    }

                    // 用于对于工作流的操作权限设置
                    ProcessUser processUser = processUserMapper.queryProcessRelation(processDefinition.getId(), loginUser.getId());
                    if (project.getUserId() == loginUser.getId() || processDefinition.getUserId() == loginUser.getId() || loginUser.getUserType() == UserType.ADMIN_USER || processUser != null) {
                        dependComplementLineageVO.setPerm(Constants.ALL_PERMISSIONS);
                    } else {
                        dependComplementLineageVO.setPerm(Constants.READ_PERMISSION);
                    }

                    // 灰度标记(生产/灰度)
                    GrayRelation grayRelationProcessDefinition = grayRelationMapper.queryByTypeAndIdAndCode(PROCESSDEFINITION, processDefinition.getId(), processDefinition.getCode());
                    if(grayRelationProcessDefinition != null && grayRelationProcessDefinition.getGrayFlag() == GrayFlag.GRAY) {
                        dependComplementLineageVO.setGrayFlag(GrayFlag.GRAY);
                    } else {
                        dependComplementLineageVO.setGrayFlag(GrayFlag.PROD);
                    }
                } else {
                    putMsg(result, Status.PROCESS_DEFINE_NOT_EXIST, processDefineCode);
                }

                dependComplementLineageVO.setLevel(levelTmp);
                levelSourceWorkFlowObjects.add(dependComplementLineageVO);
            }
        }

        result.put(Constants.DATA_LIST, levelSourceWorkFlowObjects);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    private void createGraph(Map<Long, ProcessDefinitionNode> workFlowCodeAndNode,
                             Map<Long, WorkFlowLineage> workFlowLineagesMap,
                             Set<WorkFlowRelation> workFlowRelations) {
        for (Long workFlowCode : workFlowLineagesMap.keySet()) {
            workFlowCodeAndNode.put(workFlowCode, new ProcessDefinitionNode(workFlowCode));
        }
        for (Long processDefinitionCode : workFlowCodeAndNode.keySet()) {
            // 查找指定工作流code后面的工作流code
            final List<ProcessDefinitionNode> targetWorkflowNodes = workFlowRelations
                    .stream()
                    .filter(item -> processDefinitionCode == item.getSourceWorkFlowCode())
                    .map(WorkFlowRelation::getTargetWorkFlowCode)
                    .map(workFlowCodeAndNode::get)
                    .collect(Collectors.toList());
            if (targetWorkflowNodes.size() > 0) {
                workFlowCodeAndNode.get(processDefinitionCode).setNextNodeList(targetWorkflowNodes);
            }
        }
    }

    private void recursiveGetDependList(List<HashMap<ProcessDefinitionNode, Integer>> serialRelations,
                                        ProcessDefinitionNode processDefinitionNode,
                                        HashMap<ProcessDefinitionNode, Integer> nodeLevelMap,
                                        int level) {
        nodeLevelMap.put(processDefinitionNode, level);

        boolean hasNext = false;
        level++;
        for (ProcessDefinitionNode processDefinitionNextNode : processDefinitionNode.getNextNodeList()) {
            if (!nodeLevelMap.containsKey(processDefinitionNextNode)) {
                recursiveGetDependList(serialRelations, processDefinitionNextNode, nodeLevelMap, level);
                hasNext = true;
            }
        }
        if (!hasNext) {
            serialRelations.add((HashMap<ProcessDefinitionNode, Integer>)nodeLevelMap.clone());
        }

        nodeLevelMap.remove(processDefinitionNode);
    }

    private void createLevelRelation(Map<Integer, Set<Long>> levelSourceWorkFlowCodes,
                                     List<HashMap<ProcessDefinitionNode, Integer>> serialRelations) {
        for (HashMap<ProcessDefinitionNode, Integer> serialRelation : serialRelations) {
            for (ProcessDefinitionNode processDefinitionNode : serialRelation.keySet()) {
                Set<Long> processDefinitionCodes = levelSourceWorkFlowCodes.get(serialRelation.get(processDefinitionNode));
                if (processDefinitionCodes == null) {
                    processDefinitionCodes = Sets.newHashSet();
                }
                processDefinitionCodes.add(processDefinitionNode.getWorkFlowCode());
                levelSourceWorkFlowCodes.put(serialRelation.get(processDefinitionNode), processDefinitionCodes);
            }
        }
    }

    private void deleteRepeatProcess(Map<Integer, Set<Long>> levelSourceWorkFlowCodes) {
        for (Integer key : levelSourceWorkFlowCodes.keySet()) {
            final Set<Long> longs = levelSourceWorkFlowCodes.get(key);
            while (key > 1) {
                key--;
                for (Long aLong : longs) {
                    levelSourceWorkFlowCodes.get(key).remove(aLong);
                }
            }
        }

        Map<Integer, Set<Long>> tmpMap = Maps.newHashMap(levelSourceWorkFlowCodes);
        levelSourceWorkFlowCodes.clear();

        System.out.println("tmpMap: " + tmpMap.size());
        System.out.println("levelSourceWorkFlowCodes: " + levelSourceWorkFlowCodes.size());
        int level = 1;
        for (Integer key : tmpMap.keySet()) {
            if (tmpMap.get(key) == null || tmpMap.get(key).size() == 0) {
                continue;
            }
            levelSourceWorkFlowCodes.put(level++, tmpMap.get(key));
        }
    }

    /**
     * 查询项目中所有的边和节点
     * @param projectCode
     * @param workFlowLineagesMap
     * @param workFlowRelations
     */
    private void queryProjectWorkFlowLineage(long projectCode, Map<Long, WorkFlowLineage> workFlowLineagesMap, Set<WorkFlowRelation> workFlowRelations) {
        List<ProcessLineage> processLineages = workFlowLineageMapper.queryProcessLineage(projectCode);
        if (!processLineages.isEmpty()) {
            List<WorkFlowLineage> workFlowLineages = workFlowLineageMapper.queryWorkFlowLineageByLineage(processLineages);
            workFlowLineagesMap.putAll(workFlowLineages.stream().collect(Collectors.toMap(WorkFlowLineage::getWorkFlowCode, workFlowLineage -> workFlowLineage)));
            Map<Long, List<TaskDefinition>> workFlowMap = new HashMap<>();
            for (ProcessLineage processLineage : processLineages) {
                workFlowMap.compute(processLineage.getProcessDefinitionCode(), (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    if (processLineage.getPreTaskCode() > 0) {
                        v.add(new TaskDefinition(processLineage.getPreTaskCode(), processLineage.getPreTaskVersion()));
                    }
                    if (processLineage.getPostTaskCode() > 0) {
                        v.add(new TaskDefinition(processLineage.getPostTaskCode(), processLineage.getPostTaskVersion()));
                    }
                    return v;
                });
            }
            for (Entry<Long, List<TaskDefinition>> workFlow : workFlowMap.entrySet()) {
                Set<Long> sourceWorkFlowCodes = querySourceWorkFlowCodes(projectCode, workFlow.getKey(), workFlow.getValue());
                if (sourceWorkFlowCodes.isEmpty()) {
                    workFlowRelations.add(new WorkFlowRelation(0L, workFlow.getKey()));
                } else {
                    workFlowLineagesMap.get(workFlow.getKey()).setSourceWorkFlowCode(StringUtils.join(sourceWorkFlowCodes, Constants.COMMA));
                    sourceWorkFlowCodes.forEach(code -> workFlowRelations.add(new WorkFlowRelation(code, workFlow.getKey())));
                }
            }
        }
    }

    private Set<Long> querySourceWorkFlowCodes(long projectCode, long workFlowCode, List<TaskDefinition> taskDefinitionList) {
        Set<Long> sourceWorkFlowCodes = new HashSet<>();
        if (taskDefinitionList == null || taskDefinitionList.isEmpty()) {
            return sourceWorkFlowCodes;
        }
        List<TaskDefinitionLog> taskDefinitionLogs = taskDefinitionLogMapper.queryByTaskDefinitions(taskDefinitionList);
        for (TaskDefinitionLog taskDefinitionLog : taskDefinitionLogs) {
            if (taskDefinitionLog.getProjectCode() == projectCode) {
                if (taskDefinitionLog.getTaskType().equals(TaskType.DEPENDENT.getDesc())) {
                    DependentParameters dependentParameters = JSONUtils.parseObject(taskDefinitionLog.getDependence(), DependentParameters.class);
                    if (dependentParameters != null) {
                        List<DependentTaskModel> dependTaskList = dependentParameters.getDependTaskList();
                        for (DependentTaskModel taskModel : dependTaskList) {
                            List<DependentItem> dependItemList = taskModel.getDependItemList();
                            for (DependentItem dependentItem : dependItemList) {
                                if (dependentItem.getProjectCode() == projectCode && dependentItem.getDefinitionCode() != workFlowCode) {
                                    sourceWorkFlowCodes.add(dependentItem.getDefinitionCode());
                                }
                            }
                        }
                    }
                }
            }
        }
        return sourceWorkFlowCodes;
    }
}
