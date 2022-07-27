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
import org.apache.dolphinscheduler.dao.entity.DependComplementDetail;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * depend complement detail mapper interface
 */
public interface DependComplementDetailMapper extends BaseMapper<DependComplementDetail> {

    DependComplementDetail queryDetailById(@Param("dependComplementDetailId") int dependComplementDetailId);

    IPage<DependComplementDetail> queryDependComplementDetailListByDependComplementIdPaging(IPage<DependComplementDetail> page,
                                                                                            @Param("projectCode") long projectCode,
                                                                                            @Param("dependComplementId") int dependComplementId);

    List<DependComplementDetail> queryDependComplementDetailByDependComplementId(@Param("dependComplementId") int dependComplementId);

    List<DependComplementDetail> queryStateDependComplementDetailByDependComplementId(@Param("dependComplementId") int dependComplementId, @Param("states") int[] stateArray);

    int updateStateById(@Param("dependComplementDetailId") int dependComplementId, @Param("state") ExecutionStatus state);

    List<DependComplementDetail> queryAllStateDependComplementDetailByDependComplementId(@Param("dependComplementId") int dependComplementId);

}
