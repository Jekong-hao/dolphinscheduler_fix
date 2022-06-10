package org.apache.dolphinscheduler.api.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.service.ProcessAuthorityService;
import org.apache.dolphinscheduler.api.service.ProcessInstanceService;
import org.apache.dolphinscheduler.api.service.ProjectService;
import org.apache.dolphinscheduler.api.service.SchedulerService;
import org.apache.dolphinscheduler.api.utils.PageInfo;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.Constants;
import org.apache.dolphinscheduler.common.enums.UserType;
import org.apache.dolphinscheduler.dao.entity.ProcessDefinition;
import org.apache.dolphinscheduler.dao.entity.ProcessUser;
import org.apache.dolphinscheduler.dao.entity.Project;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.*;
import org.apache.dolphinscheduler.service.process.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProcessAuthorityServiceImpl extends BaseServiceImpl implements ProcessAuthorityService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessDefinitionServiceImpl.class);

    private static final String RELEASESTATE = "releaseState";

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProcessDefinitionLogMapper processDefinitionLogMapper;

    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;

    @Autowired
    private ProcessUserMapper processUserMapper;

    /**
     * query process grant paging used for authorization
     *
     * @param loginUser login user
     * @param projectCode project code
     * @param searchVal search value
     * @param userId user id
     * @param pageNo page number
     * @param pageSize page size
     * @return process definition page
     */

    @Override
    public Result queryProcessListPaging(User loginUser, long projectCode, String searchVal, Integer userId, Integer pageNo, Integer pageSize) {
        Result result = new Result();
        Project project = projectMapper.queryByCode(projectCode);
        //check user access for project
        Map<String, Object> checkResult = projectService.checkProjectAndAuth(loginUser, project, projectCode);
        Status resultStatus = (Status) checkResult.get(Constants.STATUS);
        if (resultStatus != Status.SUCCESS) {
            putMsg(result, resultStatus);
            return result;
        }

        Page<ProcessDefinition> page = new Page<>(pageNo, pageSize);
        IPage<ProcessDefinition> processDefinitionIPage = processDefinitionMapper.queryDefineListPaging(
            page, searchVal, userId, project.getCode(), isAdmin(loginUser));

        PageInfo<ProcessDefinition> pageInfo = new PageInfo<>(pageNo, pageSize);
        pageInfo.setTotal((int) processDefinitionIPage.getTotal());
        pageInfo.setTotalList(processDefinitionIPage.getRecords());
        result.setData(pageInfo);
        putMsg(result, Status.SUCCESS);

        return result;
    }

    @Override
    public Result queryProcessListPaging(User loginUser, String searchVal, Integer pageNo, Integer pageSize) {
        Result result = new Result();
//        Project project = projectMapper.queryByCode(projectCode);
        //check user access for project
//        Map<String, Object> checkResult = projectService.checkProjectAndAuth(loginUser, project, projectCode);
//        Status resultStatus = (Status) checkResult.get(Constants.STATUS);
//        if (resultStatus != Status.SUCCESS) {
//            putMsg(result, resultStatus);
//            return result;
//        }

        Page<ProcessDefinition> page = new Page<>(pageNo, pageSize);
        IPage<ProcessDefinition> processDefinitionIPage = processDefinitionMapper.queryAllDefinitionListPagingByUser(page, searchVal, loginUser.getId(), isAdmin(loginUser));

        PageInfo<ProcessDefinition> pageInfo = new PageInfo<>(pageNo, pageSize);
        pageInfo.setTotal((int) processDefinitionIPage.getTotal());
        pageInfo.setTotalList(processDefinitionIPage.getRecords());
        result.setData(pageInfo);
        putMsg(result, Status.SUCCESS);

        return result;
    }

    /**
     * query authorized user
     *
     * @param loginUser     login user
     * @param code   process code
     * @return users        who have permission for the specified process
     */
    @Override
    public Map<String, Object> queryAuthorizedUser(User loginUser, Long code) {
        Map<String, Object> result = new HashMap<>();

        // 1. check read permission
        ProcessDefinition processDefinition = this.processDefinitionMapper.queryByCode(code);
        boolean hasProcessAndPerm = this.hasProcessAndPerm(loginUser, processDefinition, result);
        if (!hasProcessAndPerm) {
            return result;
        }

        // 2. query authorized user list
        List<User> users = this.userMapper.queryAuthedUserListByProcessDefinitionId(processDefinition.getId());
        result.put(Constants.DATA_LIST, users);
        this.putMsg(result, Status.SUCCESS);
        return result;
    }

    /**
     * query unauthorized user
     *
     * @param loginUser     login user
     * @param code   process code
     * @return users        who not have permission for the specified process
     */
    @Override
    public Map<String, Object> queryUnauthorizedUser(User loginUser, Long code) {
        Map<String, Object> result = new HashMap<>();

        ProcessDefinition processDefinition = this.processDefinitionMapper.queryByCode(code);

        // query all user list except specified userId
        List<User> userList = userMapper.queryAllGeneralUser()
            .stream().filter(user -> user.getId() != loginUser.getId()).collect(Collectors.toList());

        List<User> unauthedUserList = new ArrayList<>();
        if (!userList.isEmpty()) {
            List<User> authedUserList = userMapper.queryAuthedUserListByProcessDefinitionId(processDefinition.getId());
            if (authedUserList != null && !authedUserList.isEmpty()) {
                unauthedUserList = userList.stream().filter(user -> !authedUserList.contains(user)).collect(Collectors.toList());
            } else {
                unauthedUserList = userList;
            }
        }

        result.put(Constants.DATA_LIST, unauthedUserList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public boolean hasProcessAndPerm(User loginUser, ProcessDefinition processDefinition, Map<String, Object> result) {
        boolean checkResult = false;
        if (processDefinition == null) {
            putMsg(result, Status.PROCESS_DEFINE_NOT_EXIST, "");
        } else if (!checkGrantPermission(loginUser, processDefinition)) {
            putMsg(result, Status.USER_NO_OPERATION_PROCESS_PERM, loginUser.getUserName(), processDefinition.getCode());
        } else {
            checkResult = true;
        }
        return checkResult;
    }

    private boolean checkGrantPermission(User user, ProcessDefinition processDefinition) {
        int permissionId = queryPermission(user, processDefinition);
        return (permissionId & Constants.READ_PERMISSION) != 0;
    }

    /**
     * query permission id
     *
     * @param user user
     * @param processDefinition processDefinition
     * @return permission
     */
    private int queryPermission(User user, ProcessDefinition processDefinition) {
        if (user.getUserType() == UserType.ADMIN_USER) {
            return Constants.READ_PERMISSION;
        }

        if (processDefinition.getUserId() == user.getId()) {
            return Constants.ALL_PERMISSIONS;
        }
        ProcessUser processUser = processUserMapper.queryProcessRelation(processDefinition.getId(), user.getId());

        if (processUser == null) {
            return 0;
        }

        return processUser.getPerm();

    }
}
