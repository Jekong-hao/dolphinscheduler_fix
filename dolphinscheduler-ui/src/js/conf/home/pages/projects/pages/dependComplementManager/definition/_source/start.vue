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
  <div class="start-process-model">
    <div class="clearfix list">
      <div class="text">
        {{$t('Process Name')}}
      </div>
      <div style="line-height: 32px;">{{workflowName}}</div>
    </div>
    <div class="clearfix list">
      <div class="text">
        {{$t('Failure Strategy')}}
      </div>
      <div class="cont">
        <el-radio-group v-model="failureStrategy" style="margin-top: 7px;" size="small">
          <el-radio :label="'CONTINUE'">{{$t('Continue')}}</el-radio>
          <el-radio :label="'END'">{{$t('End')}}</el-radio>
        </el-radio-group>
      </div>
    </div>
    <div class="clearfix list">
      <div class="text">
        {{$t('Notification strategy')}}
      </div>
      <div class="cont">
        <el-select style="width: 200px;" v-model="warningType" size="small">
          <el-option
                  v-for="city in warningTypeList"
                  :key="city.id"
                  :value="city.id"
                  :label="city.code">
          </el-option>
        </el-select>
      </div>
    </div>
    <div class="clearfix list">
      <div class="text">
        {{$t('Worker group')}}
      </div>
      <div class="cont">
        <m-worker-groups v-model="workerGroup"></m-worker-groups>
      </div>
    </div>
    <div class="clearfix list">
      <div class="text">
        {{$t('Environment Name')}}
      </div>
      <div class="cont">
        <m-related-environment v-model="environmentCode" :workerGroup="workerGroup" v-on:environmentCodeEvent="_onUpdateEnvironmentCode"></m-related-environment>
      </div>
    </div>
    <div class="clearfix list">
      <div class="text">
        {{$t('Alarm group')}}
      </div>
      <div class="cont">
        <el-select
           style="width: 200px;"
           clearable
           size="small"
           v-model="warningGroupId"
          :disabled="!notifyGroupList.length">
          <el-input slot="trigger" slot-scope="{ selectedModel }" readonly size="small" :value="selectedModel ? selectedModel.label : ''" style="width: 200px;" @on-click-icon.stop="warningGroupId = ''">
            <em slot="suffix" class="el-icon-error" style="font-size: 15px;cursor: pointer;" v-show="warningGroupId"></em>
            <em slot="suffix" class="el-icon-bottom" style="font-size: 12px;" v-show="!warningGroupId"></em>
          </el-input>
          <el-option
                  v-for="city in notifyGroupList"
                  :key="city.id"
                  :value="city.id"
                  :label="city.code">
          </el-option>
        </el-select>
      </div>
    </div>
    <div class="clearfix list">
      <div class="text">
        {{$t('Schedule date')}}
      </div>
      <div class="cont">
        <el-date-picker
          style="width: 280px"
          v-model="scheduleTime"
          size="small"
          @change="_datepicker"
          type="daterange"
          range-separator="-"
          :start-placeholder="$t('startDate')"
          :end-placeholder="$t('endDate')"
          value-format="yyyy-MM-dd HH:mm:ss">
        </el-date-picker>
      </div>
    </div>
    <div class="clearfix list">
      <div class="text">
        <span>{{$t('Startup parameter')}}</span>
      </div>
      <div class="cont" style="width: 688px;">
        <div style="padding-top: 6px;">
          <m-local-params
            ref="refLocalParams"
            @on-local-params="_onLocalParams"
            :udp-list="udpList"
            :hide="false">
          </m-local-params>
        </div>
      </div>
    </div>
    <div class="submit">
      <el-button type="text" size="small" @click="close()"> {{$t('Cancel')}} </el-button>
      <el-button type="primary" size="small" round :loading="spinnerLoading" @click="ok()">{{spinnerLoading ? $t('Loading...') : $t('Start')}} </el-button>
    </div>
  </div>
</template>
<script>
  import _ from 'lodash'
  import dayjs from 'dayjs'
  import store from '@/conf/home/store'
  import { warningTypeList } from './util'
  import mWorkerGroups from '@/conf/home/pages/dag/_source/formModel/_source/workerGroups'
  import mRelatedEnvironment from '@/conf/home/pages/dag/_source/formModel/_source/relatedEnvironment'
  import mLocalParams from '@/conf/home/pages/dag/_source/formModel/tasks/_source/localParams'
  import disabledState from '@/module/mixin/disabledState'
  import { mapMutations } from 'vuex'

  export default {
    name: 'depend-complement-definition-start',
    data () {
      return {
        store,
        failureStrategy: 'CONTINUE',
        warningTypeList: warningTypeList,
        workFlowCode: '',
        workflowName: '',
        notifyGroupList: [],
        warningType: '',
        warningGroupId: '',
        scheduleTime: '',
        spinnerLoading: false,
        execType: 'DEPEND_COMPLEMENT_DATA',
        workerGroup: 'default',
        environmentCode: '',
        // Global custom parameters
        definitionGlobalParams: [],
        udpList: []
      }
    },
    mixins: [disabledState],
    props: {
      dependenceComplementStartData: Object
    },
    methods: {
      ...mapMutations('dag', ['setIsDetails', 'resetParams']),

      _onLocalParams (a) {
        this.udpList = a
      },
      _datepicker (val) {
        this.scheduleTime = val
      },
      _onUpdateEnvironmentCode (o) {
        this.environmentCode = o
      },
      _start () {
        this.spinnerLoading = true
        let startParams = {}
        for (const item of this.udpList) {
          if (item.value !== '') {
            startParams[item.prop] = item.value
          }
        }
        let param = {
          startProcessDefinitionCode: this.workFlowCode,
          startProcessDefinitionName: this.workflowName,
          scheduleTime: this.scheduleTime.length && this.scheduleTime.join(',') || '',
          execType: this.execType,
          dependComplementProcessJson: JSON.stringify(this.dependenceComplementStartData.processes) || '',
          startParams: !_.isEmpty(startParams) ? JSON.stringify(startParams) : '',
          parallism: this.parallism,
          failureStrategy: this.failureStrategy,
          workerGroup: this.workerGroup,
          environmentCode: this.environmentCode,
          warningType: this.warningType,
          warningGroupId: this.warningGroupId === '' ? 0 : this.warningGroupId
        }
        this.store.dispatch('dag/startDependComplement', param).then(res => {
          this.$message.success(res.msg)
          this.$emit('onUpdateStart')
          // recovery
          this.udpList = _.cloneDeep(this.definitionGlobalParams)
          setTimeout(() => {
            this.spinnerLoading = false
            this.close()
          }, 500)
        }).catch(e => {
          this.$message.error(e.msg || '')
          this.spinnerLoading = false
        })
      },
      _getNotifyGroupList () {
        return new Promise((resolve, reject) => {
          this.store.dispatch('dag/getNotifyGroupList').then(res => {
            this.notifyGroupList = res
            resolve()
          })
        })
      },
      _getGlobalParams () {
        this.store.dispatch('dag/getProcessDetails', this.dependenceComplementStartData.processDefinitionCode).then(res => {
          this.definitionGlobalParams = _.cloneDeep(this.store.state.dag.globalParams)
          this.udpList = _.cloneDeep(this.store.state.dag.globalParams)
        })
      },
      ok () {
        this._start()
      },
      close () {
        this.$emit('closeStart')
      }
    },
    watch: {
      execType (a) {
        this.scheduleTime = a ? [dayjs().format('YYYY-MM-DD 00:00:00'), dayjs().format('YYYY-MM-DD 00:00:00')] : ''
      }
    },
    created () {
      this.warningType = this.warningTypeList[0].id
      this.workFlowCode = this.dependenceComplementStartData.processDefinitionCode
      this.workflowName = this.dependenceComplementStartData.processDefinitionName
      this.grayFlag = this.dependenceComplementStartData.grayFlag
      this._getGlobalParams()
      let stateWorkerGroupsList = this.store.state.security.workerGroupsListAll || []
      if (stateWorkerGroupsList.length) {
        this.workerGroup = stateWorkerGroupsList[0].id
      } else {
        this.store.dispatch('security/getWorkerGroupsAll').then(res => {
          this.$nextTick(() => {
            if (res.length > 0) {
              this.workerGroup = res[0].id
            }
          })
        })
      }
    },
    mounted () {
      this._getNotifyGroupList().then(() => {
        this.$nextTick(() => {
          this.warningGroupId = ''
        })
      })
      this.workFlowCode = this.dependenceComplementStartData.processDefinitionCode
      this.workflowName = this.dependenceComplementStartData.processDefinitionName
    },
    computed: {},
    components: { mWorkerGroups, mLocalParams, mRelatedEnvironment }
  }
</script>

<style lang="scss" rel="stylesheet/scss">
  .start-process-model {
    width: 860px;
    min-height: 300px;
    background: #fff;
    border-radius: 3px;
    .title-box {
      margin-bottom: 18px;
      span {
        padding-left: 30px;
        font-size: 16px;
        padding-top: 29px;
        display: block;
      }
    }
    .ans {
      color: #0097e0;
      font-size: 14px;
      vertical-align: middle;
      cursor: pointer;
    }
    .list {
      margin-bottom: 14px;
      .text {
        width: 140px;
        float: left;
        text-align: right;
        line-height: 32px;
        padding-right: 8px;
      }
      .cont {
        width: 350px;
        float: left;
        .add-email-model {
          padding: 20px;
        }

      }
    }
    .submit {
      text-align: right;
      padding-right: 30px;
      padding-top: 10px;
      padding-bottom: 30px;
    }
  }
</style>
