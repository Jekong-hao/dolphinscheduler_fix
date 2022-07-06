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
    <div class="table-box" v-if="processInstanceData.processInstances.length > 0">
      <el-table :data="processInstanceData.processInstances" size="mini" style="width: 100%" row-key="name" lazy :load="loadTask"  :tree-props="{hasChildren: 'hasChildren'}">
        <el-table-column prop="id" :label="$t('#')" width="100"></el-table-column>
        <el-table-column :label="$t('Name')" min-width="200">
          <template slot-scope="scope">
            <el-popover v-if="scope.row.taskType" trigger="hover" placement="top">
              <p>{{ scope.row.name }}</p>
              <div slot="reference" class="name-wrapper">
                <a href="javascript:" class="links" @click="_go(scope.row)"><span class="ellipsis">{{scope.row.name}}</span></a>
              </div>
            </el-popover>
            <el-popover v-if="scope.row.commandType" trigger="hover" placement="top">
              <p>{{ scope.row.name }}</p>
              <div slot="reference" class="name-wrapper">
                <router-link :to="{ path: `/projects/${projectCode}/instance/list/${scope.row.id}` , query:{code: scope.row.processDefinitionCode}}" tag="a" class="links"><span class="ellipsis">{{ scope.row.name }}</span></router-link>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column prop="executorName" :label="$t('Executor')"></el-table-column>
        <el-table-column :label="$t('Run Type')">
          <template slot-scope="scope">
            <span v-if="scope.row.commandType">{{_rtRunningType(scope.row.commandType)}}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Node Type')">
          <template slot-scope="scope">
            <span v-if="scope.row.commandType">-</span>
            <span v-else>{{scope.row.taskType}}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('State')" width="50">
          <template slot-scope="scope">
            <span v-html="_rtState(scope.row.state)" style="cursor: pointer;"></span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Scheduling Time')" width="135">
          <template slot-scope="scope">
            <span v-if="scope.row.scheduleTime">{{scope.row.scheduleTime | formatDate}}</span>
            <span v-if="scope.row.submitTime">{{scope.row.submitTime | formatDate}}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Start Time')" width="135">
          <template slot-scope="scope">
            <span>{{scope.row.startTime | formatDate}}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('End Time')" width="135">
          <template slot-scope="scope">
            <span>{{scope.row.endTime | formatDate}}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Duration')">
          <template slot-scope="scope">
            <span>{{scope.row.duration | filterNull}}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Operation')" width="120" fixed="right">
          <template slot-scope="scope">
            <div v-show="scope.row.commandType">
              <el-tooltip :content="$t('Edit')" placement="top" :enterable="false">
                <span>
                  <el-button type="primary" size="mini" icon="el-icon-edit-outline" :disabled="scope.row.state !== 'SUCCESS' && scope.row.state !== 'PAUSE' && scope.row.state !== 'FAILURE' && scope.row.state !== 'STOP'" @click="_reEdit(scope.row)" circle></el-button>
                </span>
              </el-tooltip>
              <el-tooltip :content="$t('Gantt')" placement="top" :enterable="false">
                <span><el-button type="primary" size="mini" icon="el-icon-s-operation" @click="_gantt(scope.row)" circle></el-button></span>
              </el-tooltip>
            </div>

            <div v-show="scope.row.taskType">
              <el-tooltip :content="$t('View log')" placement="top" :enterable="false">
                <span><el-button type="primary" size="mini" :disabled="scope.row.taskType==='SUB_PROCESS'? true: false"  icon="el-icon-tickets" @click="_refreshLog(scope.row)" circle></el-button></span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-if="processInstanceData.processInstances.length === 0">
      <m-no-data><!----></m-no-data>
    </div>

    <div v-if="processInstanceData.processInstances.length > 0">
      <div class="bottom-box">
        <el-pagination
          style="float:right"
          background
          @current-change="_mInstanceGetProcessInstancesPage"
          layout="prev, pager, next"
          :total="processInstanceData.total">
        </el-pagination>
        <el-button type="text" size="mini" @click="_close()" style="float:right">{{$t('Close')}}</el-button>
      </div>
    </div>
    <el-dialog
      :show-close="false"
      :visible.sync="logDialog"
      width="auto"
      append-to-body>
      <m-log :key="taskInstanceId" :item="item" :source="source" :taskInstanceId="taskInstanceId" @close="_closeLog"></m-log>
    </el-dialog>
  </div>
</template>

<script>
  import mNoData from '@/module/components/noData/noData'
  import mLog from '@/conf/home/pages/dag/_source/formModel/log'
  import _ from 'lodash'
  import { runningType, tasksState } from '../../../../../../dag/_source/config'
  import { mapActions, mapState } from 'vuex'

  export default {
    name: 'instanceList',
    data () {
      return {
        logDialog: false,
        taskInstances: [],
        // data
        drawer: false,
        list: Object,
        tableHeaders: [
          {
            label: 'version',
            prop: 'version'
          },
          {
            label: 'createTime',
            prop: 'createTime'
          }
        ]
      }
    },
    props: {
      isInstance: Boolean,
      processInstanceData: Object
    },
    methods: {
      ...mapActions('dag', ['editExecutorsState', 'deleteInstance', 'batchDeleteInstance', 'getTaskInstanceListByProcessInstanceId']),

      /**
       * Return status
       */
      _rtState (code) {
        let o = tasksState[code]
        return `<em class="fa ansfont ${o.icoUnicode} ${o.isSpin ? 'as as-spin' : ''}" style="color:${o.color}" data-toggle="tooltip" data-container="body" title="${o.desc}"></em>`
      },

      /**
       * Return run type
       */
      _rtRunningType (code) {
        return _.filter(runningType, v => v.code === code)[0].desc
      },

      /**
       * getting a Task instance
       */
      loadTask (tree, treeNode, resolve) {
        this.getTaskInstanceListByProcessInstanceId(tree.id)
          .then((res) => {
            const { taskList } = res.data
            resolve(taskList)
          })
          .catch((e) => {
            this.$message.error(e.msg || '')
          })
      },

      /**
       * Countdown method refresh
       */
      _countDownFn (param) {
        this.buttonType = param.buttonType
        this.editExecutorsState({
          processInstanceId: param.id,
          executeType: param.executeType
        }).then(res => {
          this.list[param.index].disabled = false
          $('body').find('.tooltip.fade.top.in').remove()
          this.$forceUpdate()
          this.$message.success(res.msg)
          // Countdown
          this._countDown(() => {
            this._onUpdate()
          }, param.index)
        }).catch(e => {
          this.$message.error(e.msg || '')
          this._onUpdate()
        })
      },

      /**
       * delete one version of process definition
       */
      _mVersionDeleteProcessDefinitionVersion (item) {
        this.$emit('mVersionDeleteProcessDefinitionVersion', {
          version: item.version,
          processDefinitionCode: this.versionData.processDefinition.code,
          fromThis: this
        })
      },

      /**
       * Paging event of process instance
       */
      _mInstanceGetProcessInstancesPage (val) {
        this.$emit('mInstanceGetProcessInstancesPage', {
          pageNo: val,
          pageSize: this.processInstanceData.pageSize,
          processDefinitionCode: this.processInstanceData.processDefinitionCode,
          fromThis: this
        })
      },

      /**
       * edit
       */
      _reEdit (item) {
        this.$router.push({ path: `/projects/${this.projectCode}/instance/list/${item.id}`, query: { code: item.processDefinitionCode } })
      },

      _gantt (item) {
        this.$router.push({ path: `/projects/${this.projectCode}/instance/gantt/${item.id}` })
      },

      _arrDelChange (v) {
        let arr = []
        arr = _.map(v, 'id')
        this.strDelete = _.join(arr, ',')
      },
      /**
       * Close and destroy component and component internal events
       */
      _close () {
        // flag Whether to delete a node this.$destroy()
        this.$emit('closeInstance')
      },
      _closeLog () {
        this.logDialog = false
      },
      _refreshLog (item) {
        this.item = item
        this.source = 'list'
        this.taskInstanceId = item.id
        this.logDialog = true
      },

      _go (item) {
        this.$router.push({ path: `/projects/${this.projectCode}/instance/list/${item.processInstanceId}`, query: { taskName: item.name } })
      }
    },
    created () {
    },
    mounted () {
    },
    components: { mNoData, mLog },
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
