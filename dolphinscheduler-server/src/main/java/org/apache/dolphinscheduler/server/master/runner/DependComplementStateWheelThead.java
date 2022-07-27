package org.apache.dolphinscheduler.server.master.runner;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.dolphinscheduler.common.enums.ExecutionStatus;
import org.apache.dolphinscheduler.common.thread.Stopper;
import org.apache.dolphinscheduler.common.thread.ThreadUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.DependComplement;
import org.apache.dolphinscheduler.dao.entity.DependComplementDetail;
import org.apache.dolphinscheduler.dao.entity.DependComplementDetailProcess;
import org.apache.dolphinscheduler.service.process.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于轮训修改依赖补数的状态
 */
public class DependComplementStateWheelThead extends Thread {

    private static final Logger logger =  LoggerFactory.getLogger(DependComplementStateWheelThead.class);

    private ProcessService processService;

    private final ConcurrentHashMap<Integer, ExecutionStatus> dependComplementStateMaps;

    private final ConcurrentHashMap<Integer, ExecutionStatus> dependComplementDetailStateMaps;

    private int stateCheckIntervalSecs;

    public DependComplementStateWheelThead(
            ProcessService processService,
            ConcurrentHashMap<Integer, ExecutionStatus> dependComplementStateMaps,
            ConcurrentHashMap<Integer, ExecutionStatus> dependComplementDetailStateMaps,
            int stateCheckIntervalSecs) {
        this.processService = processService;
        this.dependComplementStateMaps = dependComplementStateMaps;
        this.dependComplementDetailStateMaps = dependComplementDetailStateMaps;
        this.stateCheckIntervalSecs = stateCheckIntervalSecs;
    }

    @Override
    public synchronized void start() {
        super.setName("DependComplementStateWheelThead");
        super.start();
    }

    @Override
    public void run() {
        logger.info("depend complement state wheel thread start");
        while (Stopper.isRunning()) {
            try {
                // 查找正在运行和提交成功的所有dependComplement
                final List<DependComplement> dependComplementList = processService.queryNonfinalStateDependComplement();

                // 处理停止事件
                handleStopEvent(dependComplementList);
                // 处理正常运行中的状态检查
                handleNormalStateCheck(dependComplementList);

            } catch (Exception e) {
                logger.error("depend complement state wheel thread error:", e);
            }
            ThreadUtils.sleep(stateCheckIntervalSecs);
        }
    }

    /**
     * 处理停止事件
     * @param dependComplementList
     */
    private void handleStopEvent(List<DependComplement> dependComplementList) {
        // 处理停止事件
        for (DependComplement dependComplement : dependComplementList) {
            // 查询指定dependComplement下,所有detail清单
            final List<DependComplementDetail> dependComplementDetailAll
                    = processService.queryDependComplementDetailListByDependComplementId(dependComplement.getId());

            // 改变内存中任务的状态
            switch(dependComplement.getState()) {
                case SUBMITTED_SUCCESS:
                case RUNNING_EXECUTION:
                    // 查找detail层的任务状态,看是否有准备停止的任务,提交成功和提交失败相同的处理方法
                    for (DependComplementDetail dependComplementDetail : dependComplementDetailAll) {
                        if (dependComplementDetail.getState() == ExecutionStatus.READY_STOP
                                || dependComplementDetail.getState() == ExecutionStatus.STOP) {
                            dependComplementDetailStateMaps.put(dependComplementDetail.getId(), ExecutionStatus.STOP);
                        }
                    }
                    break;
                case READY_STOP:
                    dependComplementStateMaps.put(dependComplement.getId(), ExecutionStatus.STOP);
                    for (DependComplementDetail dependComplementDetail : dependComplementDetailAll) {
                        dependComplementDetailStateMaps.put(dependComplementDetail.getId(), ExecutionStatus.STOP);
                    }

                    break;
                default:
            }
        }
    }

    /**
     * 处理正常运行中的状态检查
     */
    private void handleNormalStateCheck(List<DependComplement> dependComplementList) {
        // 轮训遍历,从上到下查看每一个
        for (DependComplement dependComplement : dependComplementList) {
            final String dependComplementProcessJson = dependComplement.getDependComplementProcessJson();
            final ArrayNode jsonNodes = JSONUtils.parseArray(dependComplementProcessJson);


            // 获取执行dependComplement下的所有detail(状态为: 提交成功,正在运行,准备停止)
            final List<DependComplementDetail> dependComplementDetails
                    = processService.queryNonfinalStateDependComplementDetailByDependComplementId(dependComplement.getId());
            for (DependComplementDetail dependComplementDetail : dependComplementDetails) {
                // 查询指定detail下,所有process清单
                final List<DependComplementDetailProcess> dependComplementDetailProcessAll
                        = processService.queryDependComplementDetailProcessListByTwoId(dependComplementDetail.getDependComplementId(), dependComplementDetail.getId());
                // 用于保存各个运行状态的process的数量
                Map<ExecutionStatus, Integer> processStatusMap = new HashMap<>();
                // 查询detail中各个状态的数量
                for (DependComplementDetailProcess dependComplementDetailProcess : dependComplementDetailProcessAll) {
                    final ExecutionStatus state = dependComplementDetailProcess.getState();
                    Integer num = processStatusMap.get(dependComplementDetailProcess.getState());
                    if (num == null) {
                        num = 0;
                    }
                    num += 1;
                    processStatusMap.put(state, num);
                }

                if (processStatusMap.get(ExecutionStatus.SUCCESS) != null && processStatusMap.get(ExecutionStatus.SUCCESS) == jsonNodes.size()) {
                    // 表示detail下的所有process全部都执行成功了,直接设置detail为陈宫
                    dependComplementDetail.setState(ExecutionStatus.SUCCESS);
                    processService.updateDependComplementDetailStateById(dependComplementDetail.getId(), dependComplementDetail.getState());
                } else if (processStatusMap.get(ExecutionStatus.FAILURE) != null) {
                    // 表示detail下有失败的任务,设置detail失败
                    dependComplementDetail.setState(ExecutionStatus.FAILURE);
                    processService.updateDependComplementDetailStateById(dependComplementDetail.getId(), dependComplementDetail.getState());
                } else if (dependComplementDetail.getState() == ExecutionStatus.READY_STOP) {
                    // 表示detail为准备停止
                    dependComplementDetail.setState(ExecutionStatus.STOP);
                    processService.updateDependComplementDetailStateById(dependComplementDetail.getId(), dependComplementDetail.getState());
                } else if (processStatusMap.get(ExecutionStatus.STOP) != null) {
                    // 表示detail下有停止的任务,设置detail停止
                    dependComplementDetail.setState(ExecutionStatus.STOP);
                    processService.updateDependComplementDetailStateById(dependComplementDetail.getId(), dependComplementDetail.getState());
                }
            }

            // 查询指定dependComplement下,所有detail清单
            final List<DependComplementDetail> dependComplementDetailAll
                    = processService.queryDependComplementDetailListByDependComplementId(dependComplement.getId());
            // 用于保存各个运行状态的detail的数量
            Map<ExecutionStatus, Integer> detailStatusMap = new HashMap<>();
            for (DependComplementDetail dependComplementDetail : dependComplementDetailAll) {
                final ExecutionStatus state = dependComplementDetail.getState();
                Integer num = detailStatusMap.get(dependComplementDetail.getState());
                if (num == null) {
                    num = 0;
                }
                num += 1;
                detailStatusMap.put(state, num);
            }

            // 获取成功,失败,停止三个状态的和
            final Integer successStatusNum = detailStatusMap.getOrDefault(ExecutionStatus.SUCCESS, 0);
            final Integer failureStatusNum = detailStatusMap.getOrDefault(ExecutionStatus.FAILURE, 0);
            final Integer stopStatusNum = detailStatusMap.getOrDefault(ExecutionStatus.STOP, 0);
            final Integer sumNum = successStatusNum + failureStatusNum + stopStatusNum;

            if (detailStatusMap.get(ExecutionStatus.SUCCESS) != null && detailStatusMap.get(ExecutionStatus.SUCCESS) == dependComplementDetailAll.size()) {
                // 表示此dependComplement下的所有detail全部成功了
                dependComplement.setState(ExecutionStatus.SUCCESS);
                processService.updateDependComplementStateById(dependComplement.getId(), dependComplement.getState());
            } else if (detailStatusMap.get(ExecutionStatus.FAILURE) != null) {
                if (sumNum == dependComplementDetailAll.size()) {
                    // 表示此dependComplement下有失败的任务呢,设置dependComplement事变
                    dependComplement.setState(ExecutionStatus.FAILURE);
                    processService.updateDependComplementStateById(dependComplement.getId(), dependComplement.getState());
                }
            } else if (detailStatusMap.get(ExecutionStatus.STOP) != null) {
                if (sumNum == dependComplementDetailAll.size()) {
                    // 表示此dependComplement下有停止的任务,设置dependComplement为停止
                    dependComplement.setState(ExecutionStatus.STOP);
                    processService.updateDependComplementStateById(dependComplement.getId(), dependComplement.getState());
                }
            }
        }
    }

}
