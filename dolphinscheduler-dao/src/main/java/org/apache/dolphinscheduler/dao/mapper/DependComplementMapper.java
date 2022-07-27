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
package org.apache.dolphinscheduler.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.dolphinscheduler.common.enums.ExecutionStatus;
import org.apache.dolphinscheduler.dao.entity.DependComplement;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * depend complement mapper interface
 */
public interface DependComplementMapper extends BaseMapper<DependComplement> {

    DependComplement queryDetailById(@Param("dependComplementId") int dependComplementId);

    IPage<DependComplement> queryDependComplementListPaging(IPage<DependComplement> page,
                                                            @Param("searchVal") String searchVal,
                                                            @Param("userId") int userId,
                                                            @Param("projectCode") long projectCode,
                                                            @Param("isAdmin") boolean isAdmin);

    /**
     * 插入数据,返回自增id
     * @param dependComplement
     * @return
     */
    int insertAndReturnId(@Param("dependComplement") DependComplement dependComplement);

    List<DependComplement> queryDependComplementWithState(@Param("limit") int limit, @Param("offset") int offset, @Param("states") int[] stateArray);

    List<DependComplement> queryNonfinalStateDependComplement(@Param("states") int[] stateArray);

    int updateStateById(@Param("dependComplementId") int dependComplementId, @Param("state") ExecutionStatus state);

}
