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
import org.apache.dolphinscheduler.common.enums.ElementType;
import org.apache.dolphinscheduler.dao.entity.GrayRelation;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;


/**
 * gray relation mapper interface
 */
public interface GrayRelationMapper extends BaseMapper<GrayRelation> {

    /**
     *
     * @param elementType elementType
     * @param elementId elementId
     * @param elementCode element_code
     * @return
     */
    GrayRelation queryByTypeAndIdAndCode(@Param("elementType") ElementType elementType, @Param("elementId") Integer elementId, @Param("elementCode") Long elementCode);


    /**
     *
     * @param elementType elementType
     * @param codes element codes
     * @return
     */
    List<GrayRelation> queryByDefinitionCodes(@Param("elementType") ElementType elementType, @Param("codes") Collection<Long> codes);

}
