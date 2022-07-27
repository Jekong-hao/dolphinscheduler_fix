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
  <div class="list-model" style="position: relative;">
    <div class="table-box">
      <el-table :data="list" size="mini" style="width: 100%">
        <el-table-column prop="id" :label="$t('#')" width="50"></el-table-column>
        <el-table-column :label="$t('Process Name')" min-width="200">
          <template slot-scope="scope">
            {{scope.row.name}}
          </template>
        </el-table-column>
        <el-table-column :label="$t('State')">
          <template slot-scope="scope">
            {{_rtPublishStatus(scope.row.releaseState)}}
          </template>
        </el-table-column>
        <el-table-column :label="$t('Create Time')" width="135">
          <template slot-scope="scope">
            <span>{{scope.row.createTime | formatDate}}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Update Time')" width="135">
          <template slot-scope="scope">
            <span>{{scope.row.updateTime | formatDate}}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Description')">
          <template slot-scope="scope">
            <span>{{scope.row.description | filterNull}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userName" :label="$t('Create User')"></el-table-column>
        <el-table-column prop="modifyBy" :label="$t('Modify User')"></el-table-column>
        <el-table-column :label="$t('Timing state')">
          <template slot-scope="scope">
            <span v-if="scope.row.scheduleReleaseState === 'OFFLINE'" class="time_offline">{{$t('offline')}}</span>
            <span v-if="scope.row.scheduleReleaseState === 'ONLINE'" class="time_online">{{$t('online')}}</span>
            <span v-if="!scope.row.scheduleReleaseState">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Gray Flag')">
          <template slot-scope="scope">
            <span v-if="scope.row.grayFlag === 'GRAY'" class="time_offline">{{$t('onGray')}}</span>
            <span v-if="scope.row.grayFlag === 'PROD'" class="time_online">{{$t('offGray')}}</span>
            <span v-if="!scope.row.grayFlag">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Operation')" width="50" fixed="right">
          <template slot-scope="scope">
            <el-tooltip :content="$t('Depend Complement Operation')" placement="top" :enterable="false">
              <span><el-button type="primary" size="mini" icon="el-icon-s-operation" :disabled="scope.row.perm != 7" @click="_showDetail(scope.row)" circle></el-button></span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog
      :title="$t('Depend Complement Operation')"
      :visible.sync="dependenceComplementDetailDialog"
      width="auto"
    >
      <m-detail
        :dependenceComplementDetailData="dependenceComplementDetailData"
        @dependenceComplementSet="dependenceComplementSet"
        @closeDetail="closeDetail"
      ></m-detail>
    </el-dialog>
    <el-dialog
      :title="$t('Please set the parameters before starting')"
      :visible.sync="dependenceComplementStartDialog"
      width="auto"
    >
      <m-start
        :dependenceComplementStartData = "dependenceComplementStartData"
        @onUpdateStart="onUpdateStart"
        @closeStart="closeStart"
      ></m-start>
    </el-dialog>
  </div>
</template>
<script>
  import _ from 'lodash'
  import { mapActions, mapState } from 'vuex'
  import { publishStatus } from '@/conf/home/pages/dag/_source/config'
  import mDetail from './detail.vue'
  import mStart from './start.vue'

  export default {
    name: 'depend-complement-definition-list',
    data () {
      return {
        list: [],
        // 显示detail组件弹窗
        dependenceComplementDetailDialog: false,
        dependenceComplementDetailData: {
          processes: []
        },
        // 显示start组件弹窗
        dependenceComplementStartDialog: false,
        dependenceComplementStartData: {
          processes: []
        },
        checkAll: false,
        detailDialog: false
      }
    },
    props: {
      processList: Array,
      pageNo: Number,
      pageSize: Number
    },
    methods: {
      ...mapActions('dag', ['queryDependComplementLineageByCode']),

      /**
       * 展示补数依赖链条
       */
      _showDetail (item) {
        this.queryDependComplementLineageByCode({
          pageNo: 1,
          pageSize: 10,
          code: item.code
        }).then(res => {
          this.dependenceComplementDetailData.processes = res.data
          this.dependenceComplementDetailData.processDefinitionCode = item.code
          this.dependenceComplementDetailData.processDefinitionName = item.name
          this.dependenceComplementDetailData.grayFlag = item.grayFlag
          this.dependenceComplementDetailDialog = true
        }).catch((e) => {
          this.$message.error(e.msg || '')
        })
      },
      closeDetail () {
        this.dependenceComplementDetailDialog = false
      },
      dependenceComplementSet (item) {
        this.dependenceComplementDetailDialog = false
        this.dependenceComplementStartDialog = true
        let processes = item.split(',')
        this.dependenceComplementStartData.processes =
          this.dependenceComplementDetailData.processes.filter(
            function (element, index, self) {
              return processes.includes(element.processCode + '')
            }
          )
        this.dependenceComplementStartData.processDefinitionCode = this.dependenceComplementDetailData.processDefinitionCode
        this.dependenceComplementStartData.processDefinitionName = this.dependenceComplementDetailData.processDefinitionName
        this.dependenceComplementStartData.grayFlag = this.dependenceComplementDetailData.grayFlag
      },
      closeStart () {
        this.dependenceComplementStartDialog = false
      },
      _rtPublishStatus (code) {
        return _.filter(publishStatus, v => v.code === code)[0].desc
      },
      onUpdateStart () {
        this._onUpdate()
        this.dependenceComplementStartDialog = false
      },
      /**
       * update
       */
      _onUpdate () {
        this.$emit('on-update')
      }
    },
    watch: {
      processList: {
        handler (a) {
          this.checkAll = false
          this.list = []
          setTimeout(() => {
            this.list = _.cloneDeep(a)
          })
        },
        immediate: true,
        deep: true
      }
    },
    created () {
    },
    mounted () {
    },
    computed: {
      ...mapState('dag', ['projectCode'])
    },
    components: { mDetail, mStart }
  }
</script>

<style lang="scss" rel="stylesheet/scss">

  .time_online {
    background-color: #5cb85c;
    color: #fff;
    padding: 3px;
  }
  .time_offline {
    background-color: #ffc107;
    color: #fff;
    padding: 3px;
  }
</style>
