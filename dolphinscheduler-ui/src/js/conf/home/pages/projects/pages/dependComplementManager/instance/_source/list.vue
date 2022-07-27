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
        <el-table-column prop="id" :label="$t('#')" width="100"></el-table-column>
        <el-table-column :label="$t('Start Process Name')" min-width="200">
          <template slot-scope="scope">
            {{scope.row.startProcessDefinitionName}}
          </template>
        </el-table-column>
        <el-table-column :label="$t('Schedule Start Date')" width="135">
          <template slot-scope="scope">
            <span>{{scope.row.scheduleStartDate}}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Schedule End Date')" width="135">
          <template slot-scope="scope">
            <span>{{scope.row.scheduleEndDate}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userName" :label="$t('Execution User')"></el-table-column>
        <el-table-column :label="$t('Run Type')">{{$t('Depend complement')}}</el-table-column>
        <el-table-column :label="$t('State')" width="100">
          <template slot-scope="scope">
            <span v-html="_rtState(scope.row.state)" style="cursor: pointer;"></span>
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
        <el-table-column :label="$t('Operation')" width="100" fixed="right">
          <template slot-scope="scope">
            <el-tooltip :content="$t('Stop')" placement="top" :enterable="false">
              <span><el-button type="danger" size="mini" icon="el-icon-close" :disabled="scope.row.perm !== 7 || (scope.row.state !== 'RUNNING_EXECUTION' && scope.row.state !== 'SUBMITTED_SUCCESS' && scope.row.state !== 'FAILURE')" @click="_stop(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('Depend Complement Instance Detail')" placement="top" :enterable="false">
              <span><el-button type="primary" size="mini" icon="el-icon-info" :disabled="false" @click="_showInstanceDetail(scope.row)" circle></el-button></span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog
      :title="$t('Depend Complement Instance Detail')"
      :visible.sync="dependComplementLogDetailDialog"
      width="auto"
    >
      <m-detail
        :dependComplementLogDetailData="dependComplementLogDetailData"
        @closeDetail="closeDetail"
        @refreshDetail="refreshDetail"
        @mGetDependComplementsPage="getDependComplements"
      ></m-detail>
    </el-dialog>
  </div>
</template>
<script>
  import _ from 'lodash'
  import { mapActions, mapState } from 'vuex'
  import { tasksState } from '@/conf/home/pages/dag/_source/config'
  import mDetail from './detail'

  export default {
    name: 'depend-complement-instance-list',
    data () {
      return {
        dependComplementExecuteTarget: 'DEPEND_COMPLEMENT',
        list: [],
        checkAll: false,
        dependComplementLogDetailDialog: false,
        dependComplementLogDetailData: {
          dependComplementLogDetails: [],
          total: null,
          pageNo: null,
          pageSize: null
        },
        detailDialog: false
      }
    },
    props: {
      dependComplementList: Array,
      pageNo: Number,
      pageSize: Number
    },
    methods: {
      ...mapActions('dag', ['editDependComplementState', 'getDependComplementDetailListByDependComplementIdP']),

      _rtState (code) {
        let o = tasksState[code]
        return `<em class="fa ansfont ${o.icoUnicode} ${o.isSpin ? 'as as-spin' : ''}" style="color:${o.color}" data-toggle="tooltip" data-container="body" title="${o.desc}"></em>`
      },
      _stop (item, index) {
        this._upDependComplementState({
          dependComplementId: item.id,
          dependComplementExecuteTarget: this.dependComplementExecuteTarget,
          executeType: 'STOP'
        })
      },
      _upDependComplementState (o) {
        this.editDependComplementState(o).then(res => {
          this.$message.success(res.msg)
          this._onUpdate()
        }).catch(e => {
          this.$message.error(e.msg || '')
          this._onUpdate()
        })
      },

      _showInstanceDetail (item) {
        this.getDependComplementDetailListByDependComplementIdP({
          pageNo: 1,
          pageSize: 10,
          dependComplementId: item.id
        }).then(res => {
          this.dependComplementLogDetailData.dependComplementLogDetails = res.data.totalList
          this.dependComplementLogDetailData.dependComplementId = item.id
          this.dependComplementLogDetailData.total = res.data.total
          this.dependComplementLogDetailData.pageNo = res.data.currentPage
          this.dependComplementLogDetailData.pageSize = res.data.pageSize
          this.dependComplementLogDetailDialog = true
        }).catch((e) => {
          this.$message.error(e.msg || '')
        })
      },
      getDependComplements ({ pageNo, pageSize, dependComplementId }) {
        this.getDependComplementDetailListByDependComplementIdP({
          pageNo: pageNo,
          pageSize: pageSize,
          dependComplementId: dependComplementId
        }).then((res) => {
          this.dependComplementLogDetailData.dependComplementLogDetails = res.data.totalList
          this.dependComplementLogDetailData.total = res.data.total
          this.dependComplementLogDetailData.pageNo = res.data.currentPage
          this.dependComplementLogDetailData.pageSize = res.data.pageSize
        })
          .catch((e) => {
            this.$message.error(e.msg || '')
          })
      },

      closeDetail () {
        this.dependComplementLogDetailDialog = false
      },
      refreshDetail ({ pageNo, pageSize, dependComplementId }) {
        this.getDependComplementDetailListByDependComplementIdP({
          pageNo: pageNo,
          pageSize: pageSize,
          dependComplementId: dependComplementId
        }).then(res => {
          this.dependComplementLogDetailData.dependComplementLogDetails = res.data.totalList
          this.dependComplementLogDetailData.total = res.data.total
          this.dependComplementLogDetailData.pageNo = res.data.currentPage
          this.dependComplementLogDetailData.pageSize = res.data.pageSize
        })
          .catch((e) => {
            this.$message.error(e.msg || '')
          })
      },
      _onUpdate () {
        this.$emit('on-update')
      }
    },
    watch: {
      dependComplementList: {
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
    components: { mDetail }
  }
</script>

<style lang="scss" rel="stylesheet/scss">

</style>
