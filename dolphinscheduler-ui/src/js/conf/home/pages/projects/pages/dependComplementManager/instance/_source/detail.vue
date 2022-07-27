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
    <div class="table-box" v-if="dependComplementLogDetailData.dependComplementLogDetails.length > 0">
      <el-table :data="dependComplementLogDetailData.dependComplementLogDetails" size="mini" style="width: 100%" row-key="code" lazy :load="loadDependComplementDetailProcess"  :tree-props="{children: 'children', hasChildren: 'hasChildren'}">
        <el-table-column prop="code" :label="$t('#')" width="150"></el-table-column>
        <el-table-column :label="$t('Name')" min-width="200">
          <template slot-scope="scope">
            {{scope.row.processDefinitionName}}
          </template>
        </el-table-column>
        <el-table-column :label="$t('Level')" min-width="50">
          <template slot-scope="scope">
            <span v-if="scope.row.level">{{scope.row.level}}</span>
            <span v-if="!scope.row.level">-</span>
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
        <el-table-column :label="$t('Operation')" width="70" fixed="right">
          <template slot-scope="scope">
            <div v-show="scope.row.flag === 'depend-complement-detail'">
              <el-tooltip :content="$t('Stop')" placement="top" :enterable="false">
                <span>
                  <el-button type="danger" size="mini" icon="el-icon-close" :disabled="scope.row.perm !== 7 || (scope.row.state !== 'RUNNING_EXECUTION' && scope.row.state !== 'SUBMITTED_SUCCESS' && scope.row.state !== 'FAILURE')" @click="_stop(scope.row)" circle></el-button>
                </span>
              </el-tooltip>
            </div>
            <div v-show="scope.row.flag === 'depend-complement-detail-process'">
              <el-tooltip :content="$t('Edit')" placement="top" :enterable="false">
                <span>
                  <el-button type="primary" size="mini" icon="el-icon-edit-outline" :disabled="scope.row.perm !== 7" @click="_reEdit(scope.row)" circle></el-button>
                </span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-if="dependComplementLogDetailData.dependComplementLogDetails.length === 0">
      <m-no-data><!----></m-no-data>
    </div>

    <div v-if="dependComplementLogDetailData.dependComplementLogDetails.length > 0">
      <div class="bottom-box">
        <el-pagination
          style="float:right"
          background
          @current-change="_mGetDependComplementsPage"
          layout="prev, pager, next"
          :total="dependComplementLogDetailData.total">
        </el-pagination>
        <el-button type="primary" size="mini" @click="_refreshDetail()" style="float:right" round>{{$t('Refresh')}}</el-button>
        <el-button size="mini" @click="_close()" style="float:right" round>{{$t('Close')}}</el-button>
      </div>
    </div>
  </div>
</template>

<script>

  import _ from 'lodash'
  import mNoData from '@/module/components/noData/noData'
  import { tasksState } from '@/conf/home/pages/dag/_source/config'
  import { mapActions, mapState } from 'vuex'

  export default {
    name: 'depend-complement-instance-detail',
    data () {
      return {
        dependComplementExecuteTarget: 'DEPEND_COMPLEMENT_DETAIL',
        logDialog: false,
        // data
        list: Object
      }
    },
    props: {
      dependComplementLogDetailData: Object
    },
    methods: {
      ...mapActions('dag', ['editDependComplementState', 'getDependComplementDetailProcessListByTwoId']),

      _rtState (code) {
        let o = tasksState[code]
        return `<em class="fa ansfont ${o.icoUnicode} ${o.isSpin ? 'as as-spin' : ''}" style="color:${o.color}" data-toggle="tooltip" data-placement="left" title="${o.desc}"></em>`
      },
      _stop (item, index) {
        // 标签操作的是哪个目标
        if (item.flag === 'depend-complement-detail') {
          this.dependComplementExecuteTarget = 'DEPEND_COMPLEMENT_DETAIL'
        } else {
          this.dependComplementExecuteTarget = 'DEPEND_COMPLEMENT_DETAIL_PROCESS'
        }
        this._upDependComplementState({
          dependComplementId: item.dependComplementId,
          dependComplementDetailId: item.dependComplementDetailId,
          dependComplementExecuteTarget: this.dependComplementExecuteTarget,
          executeType: 'STOP'
        })
      },
      sleep (time) {
        return new Promise((resolve) => setTimeout(resolve, time))
      },
      _upDependComplementState (o) {
        this.editDependComplementState(o).then(res => {
          this.$message.success(res.msg)
          this._refreshDetail()
          this.sleep(5000).then(() => {
            this._refreshDetail()
          })
          // 暂时没起作用
          this._refreshDetail()
        }).catch(e => {
          this.$message.error(e.msg || '')
          this._refreshDetail()
        })
      },
      loadDependComplementDetailProcess (tree, treeNode, resolve) {
        this.getDependComplementDetailProcessListByTwoId({
          dependComplementId: tree.dependComplementId,
          dependComplementDetailId: tree.dependComplementDetailId
        }).then((res) => {
          resolve(res.data)
        }).catch((e) => {
          this.$message.error(e.msg || '')
        })
      },
      _mGetDependComplementsPage (val) {
        this.$emit('mGetDependComplementsPage', {
          pageNo: val,
          pageSize: this.dependComplementLogDetailData.pageSize,
          dependComplementId: this.dependComplementLogDetailData.dependComplementId
        })
      },
      _reEdit (item) {
        this.$router.push({ path: `/projects/${this.projectCode}/instance/list/${item.processInstanceId}`, query: { code: item.processDefinitionCode } })
      },
      _arrDelChange (v) {
        let arr = []
        arr = _.map(v, 'id')
        this.strDelete = _.join(arr, ',')
      },
      _close () {
        // flag Whether to delete a node this.$destroy()
        this.$emit('closeDetail')
      },
      _refreshDetail () {
        this.$emit('refreshDetail', {
          pageNo: this.dependComplementLogDetailData.pageNo,
          pageSize: this.dependComplementLogDetailData.pageSize,
          dependComplementId: this.dependComplementLogDetailData.dependComplementId
        })
      },
      _go (item) {
        this.$router.push({ path: `/projects/${this.projectCode}/instance/list/${item.processInstanceId}`, query: { taskName: item.name } })
      }
    },
    created () {
    },
    mounted () {
    },
    components: { mNoData },
    computed: {
      ...mapState('dag', ['projectCode'])
    },
    watch: {
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
