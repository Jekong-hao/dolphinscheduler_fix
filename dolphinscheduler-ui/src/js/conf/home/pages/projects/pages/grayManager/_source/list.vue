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
      <el-table :data="list" size="mini" style="width: 100%" @selection-change="_arrDelChange">
        <el-table-column type="selection" width="50" :selectable="selectable"></el-table-column>
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
            <el-tooltip :content="$t('to_gray')" placement="top" :enterable="false">
              <span><el-button type="warning" size="mini" v-if="scope.row.grayFlag === 'PROD'"  icon="el-icon-star-on" :disabled="scope.row.perm != 7" @click="_popgray(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('to_no_gray')" placement="top" :enterable="false">
              <span><el-button type="danger" size="mini" icon="el-icon-star-off" v-if="scope.row.grayFlag === 'GRAY'" :disabled="scope.row.perm != 7" @click="_cancelgray(scope.row)" circle></el-button></span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!--加入批量迁移标签-->
    <span v-if="$route.params.grayFlag === 'gray'"><el-button type="primary" size="mini" :disabled="!strSelectCodes" style="position: absolute; bottom: -48px; left: 19px;" @click="_batchUpdateToGray()" >{{$t('Batch update to prod')}}</el-button></span>
    <span v-if="$route.params.grayFlag === 'prod'"><el-button type="primary" size="mini" :disabled="!strSelectCodes" style="position: absolute; bottom: -48px; left: 19px;" @click="_batchUpdateToProd()" >{{$t('Batch update to gray')}}</el-button></span>

  </div>
</template>
<script>
  import _ from 'lodash'
  import { mapActions, mapState } from 'vuex'
  import { publishStatus } from '@/conf/home/pages/dag/_source/config'

  export default {
    name: 'gray-manager-list',
    data () {
      return {
        list: [],
        strSelectCodes: '',
        checkAll: false
      }
    },
    props: {
      processList: Array,
      pageNo: Number,
      pageSize: Number
    },
    methods: {
      ...mapActions('dag', ['editGrayState', 'batchUpdateGray']),

      selectable (row, index) {
        if (row.perm !== 7) {
          return false
        } else {
          return true
        }
      },
      _rtPublishStatus (code) {
        return _.filter(publishStatus, v => v.code === code)[0].desc
      },
      /**
       * NO_GRAY
       */
      _cancelgray (item) {
        this._upGrayState({
          ...item,
          grayFlag: 'PROD'
        })
      },
      /**
       * GRAY
       */
      _popgray (item) {
        this._upGrayState({
          ...item,
          grayFlag: 'GRAY'
        })
      },
      /**
       * Gray state
       */
      _upGrayState (o) {
        this.editGrayState(o).then(res => {
          this.$message.success(res.msg)
          $('body').find('.tooltip.fade.top.in').remove()
          this._onUpdate()
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },
      /**
       * batch update to gray
       */
      _batchUpdateToGray () {
        this._upBatchGrayState({
          codes: this.strSelectCodes,
          grayFlag: 'PROD'
        })
      },
      /**
       * batch update to gray
       */
      _batchUpdateToProd () {
        this._upBatchGrayState({
          codes: this.strSelectCodes,
          grayFlag: 'GRAY'
        })
      },
      _upBatchGrayState (o) {
        this.batchUpdateGray(o).then(res => {
          this.$message.success(res.msg)
          $('body').find('.tooltip.fade.top.in').remove()
          this._onUpdate()
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },
      _onUpdate () {
        this.$emit('on-update')
      },
      /**
       * the array that to be delete
       */
      _arrDelChange (v) {
        let arr = []
        arr = _.map(v, 'code')
        this.strSelectCodes = _.join(arr, ',')
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
      },
      pageNo () {
        this.strSelectCodes = ''
      }
    },
    created () {
    },
    mounted () {
    },
    computed: {
      ...mapState('dag', ['projectCode'])
    },
    components: { }
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
