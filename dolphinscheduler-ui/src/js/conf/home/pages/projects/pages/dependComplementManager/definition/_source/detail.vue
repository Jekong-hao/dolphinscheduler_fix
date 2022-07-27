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
<template>

  <div class="instance-container">
    <div class="table-box" v-if="dependenceComplementDetailData.processes.length > 0">
      <el-table :data="dependenceComplementDetailData.processes" size="mini" style="width: 100%" row-key="name" @selection-change="_arrDelChange">
        <el-table-column type="selection" width="50" :selectable="selectable"></el-table-column>
        <el-table-column prop="level" :label="$t('Level')" width="100"></el-table-column>
        <el-table-column :label="$t('Process Name')" min-width="200">
          <template slot-scope="scope">
            <el-popover trigger="hover" placement="top">
              <p>{{ scope.row.processName }}</p>
              <div slot="reference" class="name-wrapper">
                <a href="javascript:" class="links" @click="_go(scope.row)"><span class="ellipsis">{{scope.row.processName}}</span></a>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column prop="userName" :label="$t('Create User')"></el-table-column>
        <el-table-column :label="$t('State')">
          <template slot-scope="scope">
            {{_rtPublishStatus(scope.row.releaseState)}}
          </template>
        </el-table-column>
        <el-table-column :label="$t('Gray Flag')">
          <template slot-scope="scope">
            <span v-if="scope.row.grayFlag === 'GRAY'" class="time_offline">{{$t('onGray')}}</span>
            <span v-if="scope.row.grayFlag === 'PROD'" class="time_online">{{$t('offGray')}}</span>
            <span v-if="!scope.row.grayFlag">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Create Time')" width="135">
          <template slot-scope="scope">
            <span v-if="scope.row.createTime">{{scope.row.createTime | formatDate}}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Update Time')" width="135">
          <template slot-scope="scope">
            <span>{{scope.row.updateTime | formatDate}}</span>
          </template>
        </el-table-column>

        <el-table-column :label="$t('Operation')" width="120" fixed="right">
          <template slot-scope="scope">
            <div v-show="true">
              <el-tooltip :content="$t('Edit')" placement="top" :enterable="false">
                <span>
                  <el-button type="primary" size="mini" icon="el-icon-edit-outline" @click="_reEdit(scope.row)" circle></el-button>
                </span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-if="dependenceComplementDetailData.processes.length === 0">
      <m-no-data><!----></m-no-data>
    </div>

    <div v-if="dependenceComplementDetailData.processes.length > 0">
      <div class="bottom-box">
        <el-button type="text" size="small" @click="_close()"> {{$t('Cancel')}} </el-button>
        <el-button type="primary" size="small" round @click="dependenceComplementSet()">{{$t('Complement settings')}} </el-button>
      </div>
    </div>
  </div>
</template>

<script>
  import _ from 'lodash'
  import mNoData from '@/module/components/noData/noData'
  import { mapActions, mapState } from 'vuex'
  import { publishStatus } from '@/conf/home/pages/dag/_source/config'

  export default {
    name: 'depend-complement-definition-detail',
    data () {
      return {
        strSelectCodes: ''
      }
    },
    props: {
      dependenceComplementDetailData: Object
    },
    methods: {
      ...mapActions('dag', ['getTaskInstanceListByProcessInstanceId']),

      dependenceComplementSet () {
        this.$emit('dependenceComplementSet', this.strSelectCodes)
      },
      _arrDelChange (v) {
        let arr = []
        arr = _.map(v, 'processCode')
        this.strSelectCodes = _.join(arr, ',')
      },
      _rtPublishStatus (code) {
        return _.filter(publishStatus, v => v.code === code)[0].desc
      },
      selectable (row, index) {
        if (row.releaseState === 'OFFLINE' || row.perm !== 7 || row.grayFlag !== 'PROD') {
          return false
        } else {
          return true
        }
      },
      _reEdit (item) {
        this.$router.push({ path: `/projects/${this.projectCode}/definition/list/${item.processCode}` })
      },
      _go (item) {
        this.$router.push({ path: `/projects/${this.projectCode}/definition/list/${item.processCode}` })
      },
      _close () {
        this.$emit('closeDetail')
      }
    },
    created () {
    },
    mounted () {
    },
    watch: {
    },
    components: { mNoData },
    computed: {
      ...mapState('dag', ['projectCode'])
    }
  }
</script>

<style lang="scss" rel="stylesheet/scss">
  .instance-container {
    width: 1200px;
    position: relative;
    .title-box {
      height: 61px;
      border-bottom: 1px solid #DCDEDC;
      position: relative;
      .name {
        position: absolute;
        left: 24px;
        top: 18px;
        font-size: 16px;
      }
    }
    .bottom-box {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      text-align: right;
      height: 60px;
      line-height: 60px;
      border-top: 1px solid #DCDEDC;
      background: #fff;
      .ans-page {
        display: inline-block;
      }
    }
    .table-box {
      overflow-y: scroll;
      padding-bottom: 60px;
    }
  }
</style>
