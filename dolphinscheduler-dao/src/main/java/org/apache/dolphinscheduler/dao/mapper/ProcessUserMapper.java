package org.apache.dolphinscheduler.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.dolphinscheduler.dao.entity.ProcessUser;
import org.apache.ibatis.annotations.Param;

/**
 * process user mapper interface
 */
public interface ProcessUserMapper extends BaseMapper<ProcessUser> {

    /**
     * delte process user relation
     *
     * @param processId processId
     * @param userId userId
     * @return delete result
     */
    void deleteProcessUserRelation(@Param("processId") int processId,
                                   @Param("userId") int userId);

    /**
     * query process relation
     *
     * @param processId processId
     * @param userId userId
     * @return project user relation
     */
    ProcessUser queryProcessRelation(@Param("processId") int processId,
                                     @Param("userId") int userId);


}
