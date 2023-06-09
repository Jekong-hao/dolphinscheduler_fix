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
            <el-popover trigger="hover" placement="top">
              <p>{{ scope.row.name }}</p>
              <div slot="reference" class="name-wrapper">
                <router-link :to="{ path: `/projects/${projectCode}/definition/list/${scope.row.code}` }" tag="a" class="links">
                  <span class="ellipsis">{{scope.row.name}}</span>
                </router-link>
              </div>
            </el-popover>
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
        <el-table-column :label="$t('Operation')" width="380" fixed="right">
          <template slot-scope="scope">
            <el-tooltip :content="$t('Edit')" placement="top" :enterable="false">
              <span><el-button type="primary" size="mini" icon="el-icon-edit-outline" :disabled="scope.row.perm != 7 || scope.row.releaseState === 'ONLINE'" @click="_edit(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('Start')" placement="top" :enterable="false">
              <span><el-button type="success" size="mini" :disabled="scope.row.perm != 7 || scope.row.releaseState !== 'ONLINE'"  icon="el-icon-video-play" @click="_start(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('Timing')" placement="top" :enterable="false">
              <span><el-button type="primary" size="mini" icon="el-icon-time" :disabled="scope.row.perm != 7 || scope.row.releaseState !== 'ONLINE' || scope.row.scheduleReleaseState !== null" @click="_timing(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('online')" placement="top" :enterable="false">
              <span><el-button type="warning" size="mini" v-if="scope.row.releaseState === 'OFFLINE'"  icon="el-icon-upload2" :disabled="scope.row.perm != 7" @click="_poponline(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('offline')" placement="top" :enterable="false">
              <span><el-button type="danger" size="mini" icon="el-icon-download" :disabled="scope.row.perm != 7" v-if="scope.row.releaseState === 'ONLINE'" @click="_downline(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('Copy Workflow')" placement="top" :enterable="false">
              <span><el-button type="primary" size="mini" :disabled="scope.row.perm != 7 || scope.row.releaseState === 'ONLINE'"  icon="el-icon-document-copy" @click="_copyProcess(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('Cron Manage')" placement="top" :enterable="false">
              <span><el-button type="primary" size="mini" icon="el-icon-date" :disabled="scope.row.perm != 7 || scope.row.releaseState !== 'ONLINE'" @click="_timingManage(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('Delete')" placement="top" :enterable="false">
              <el-popconfirm
                :confirmButtonText="$t('Confirm')"
                :cancelButtonText="$t('Cancel')"
                icon="el-icon-info"
                iconColor="red"
                :title="$t('Delete?')"
                @onConfirm="_delete(scope.row,scope.row.id)"
              >
                <el-button type="danger" size="mini" icon="el-icon-delete" :disabled="scope.row.perm != 7 || scope.row.releaseState === 'ONLINE'" circle slot="reference"></el-button>
              </el-popconfirm>
            </el-tooltip>
            <el-tooltip :content="$t('TreeView')" placement="top" :enterable="false">
              <span><el-button type="primary" size="mini" icon="el-icon-s-data" @click="_treeView(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('Process Instance')" placement="top" :enterable="false">
              <span><el-button type="primary" size="mini" icon="el-icon-s-help" @click="_showInstances(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('Export')" placement="top" :enterable="false">
              <span><el-button type="primary" size="mini" icon="el-icon-s-unfold" :disabled="scope.row.perm != 7" @click="_export(scope.row)" circle></el-button></span>
            </el-tooltip>
            <el-tooltip :content="$t('Version Info')" placement="top" :enterable="false">
              <span><el-button type="primary" size="mini" icon="el-icon-info" :disabled="scope.row.perm != 7" @click="_version(scope.row)" circle></el-button></span>
            </el-tooltip>
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
    <el-tooltip :content="$t('Delete')" placement="top">
      <el-popconfirm
        :confirmButtonText="$t('Confirm')"
        :cancelButtonText="$t('Cancel')"
        :title="$t('Delete?')"
        @onConfirm="_delete({},-1)"
      >
        <el-button style="position: absolute; bottom: -48px; left: 19px;"  type="primary" size="mini" :disabled="!strSelectCodes" slot="reference">{{$t('Delete')}}</el-button>
      </el-popconfirm>
    </el-tooltip>
    <el-button type="primary" size="mini" :disabled="!strSelectCodes" style="position: absolute; bottom: -48px; left: 80px;" @click="_batchExport(item)" >{{$t('Export')}}</el-button>
    <span><el-button type="primary" size="mini" :disabled="!strSelectCodes" style="position: absolute; bottom: -48px; left: 140px;" @click="_batchCopy(item)" >{{$t('Batch copy')}}</el-button></span>
<!--    <el-button type="primary" size="mini" :disabled="!strSelectCodes" style="position: absolute; bottom: -48px; left: 225px;" @click="_batchMove(item)" >{{$t('Batch move')}}</el-button>-->
    <el-drawer
      :visible.sync="drawer"
      size=""
      :with-header="false">
      <m-versions :versionData = versionData @mVersionSwitchProcessDefinitionVersion="mVersionSwitchProcessDefinitionVersion" @mVersionGetProcessDefinitionVersionsPage="mVersionGetProcessDefinitionVersionsPage" @mVersionDeleteProcessDefinitionVersion="mVersionDeleteProcessDefinitionVersion" @closeVersion="closeVersion"></m-versions>
    </el-drawer>
    <el-dialog
      :title="$t('Please set the parameters before starting')"
      v-if="startDialog"
      :visible.sync="startDialog"
      width="auto">
      <m-start :startData= "startData" @onUpdateStart="onUpdateStart" @closeStart="closeStart"></m-start>
    </el-dialog>
    <el-dialog
      :title="$t('Set parameters before timing')"
      :visible.sync="timingDialog"
      width="auto">
      <m-timing :timingData="timingData" @onUpdateTiming="onUpdateTiming" @closeTiming="closeTiming"></m-timing>
    </el-dialog>
    <el-dialog
      :title="$t('Info')"
      :visible.sync="relatedItemsDialog"
      width="auto">
      <m-related-items :tmp="tmp" @onBatchCopy="onBatchCopy" @onBatchMove="onBatchMove" @closeRelatedItems="closeRelatedItems"></m-related-items>
    </el-dialog>
    <el-dialog
      :title="$t('Process Instance')"
      :visible.sync="instanceDialog"
      width="auto"
    >
      <m-instance-list
        :processInstanceData="processInstanceData"
        @mInstanceGetProcessInstancesPage="getProcessInstances"
        @closeInstance="closeInstance"
      ></m-instance-list>
    </el-dialog>
  </div>
</template>
<script>
  import _ from 'lodash'
  import mStart from './start'
  import mTiming from './timing'
  import mRelatedItems from './relatedItems'
  import { mapActions, mapState } from 'vuex'
  import { publishStatus } from '@/conf/home/pages/dag/_source/config'
  import mInstanceList from './instanceList'
  import mVersions from './versions'

  export default {
    name: 'definition-list',
    data () {
      return {
        list: [],
        strSelectCodes: '',
        checkAll: false,
        drawer: false,
        versionData: {
          processDefinition: {},
          processDefinitionVersions: [],
          total: null,
          pageNo: null,
          pageSize: null
        },
        startDialog: false,
        startData: {},
        timingDialog: false,
        timingData: {
          item: {},
          type: ''
        },
        instanceDialog: false,
        processInstanceData: {
          processInstances: [],
          total: null,
          pageNo: null,
          pageSize: null
        },
        relatedItemsDialog: false,
        tmp: false
      }
    },
    props: {
      processList: Array,
      pageNo: Number,
      pageSize: Number
    },
    methods: {
      ...mapActions('dag', ['editProcessState', 'editGrayState', 'getStartCheck', 'deleteDefinition', 'batchDeleteDefinition', 'exportDefinition', 'getProcessDefinitionVersionsPage', 'copyProcess', 'switchProcessDefinitionVersion', 'deleteProcessDefinitionVersion', 'moveProcess', 'getProcessInstancesPageByCode']),
      ...mapActions('security', ['getWorkerGroupsAll']),

      selectable (row, index) {
        if (row.releaseState === 'ONLINE' || row.perm !== 7) {
          return false
        } else {
          return true
        }
      },
      _rtPublishStatus (code) {
        return _.filter(publishStatus, v => v.code === code)[0].desc
      },
      _treeView (item) {
        this.$router.push({ path: `/projects/${this.projectCode}/definition/tree/${item.code}` })
      },

      /**
       * View historical workflow instances
       */
      _showInstances (item) {
        this.getProcessInstancesPageByCode({
          pageNo: 1,
          pageSize: 10,
          code: item.code
        }).then(res => {
          let processInstances = res.data.totalList
          let total = res.data.total
          let pageSize = res.data.pageSize
          let pageNo = res.data.currentPage
          this.processInstanceData.processInstances =
            processInstances
          this.processInstanceData.processDefinitionCode = this.definitionCode
          this.processInstanceData.total = total
          this.processInstanceData.pageNo = pageNo
          this.processInstanceData.pageSize = pageSize
          this.instanceDialog = true
        }).catch((e) => {
          this.$message.error(e.msg || '')
        })
      },
      closeInstance () {
        this.instanceDialog = false
      },
      getProcessInstances ({ pageNo, pageSize, processDefinitionCode }) {
        this.getProcessInstancesPageByCode({
          pageNo: pageNo,
          pageSize: pageSize,
          code: processDefinitionCode
        })
          .then((res) => {
            this.processInstanceData.processInstances = res.data.totalList
            this.processInstanceData.total = res.data.total
            this.processInstanceData.pageSize = res.data.pageSize
            this.processInstanceData.pageNo = res.data.currentPage
          })
          .catch((e) => {
            this.$message.error(e.msg || '')
          })
      },

      /**
       * Start
       */
      _start (item) {
        this.getWorkerGroupsAll()
        this.getStartCheck({ processDefinitionCode: item.code }).then(res => {
          this.startData = item
          this.startDialog = true
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },
      onUpdateStart () {
        this._onUpdate()
        this.startDialog = false
      },
      closeStart () {
        this.startDialog = false
      },
      /**
       * timing
       */
      _timing (item) {
        this.timingData.item = item
        this.timingData.type = 'timing'
        this.timingDialog = true
      },
      onUpdateTiming () {
        this._onUpdate()
        this.timingDialog = false
      },
      closeTiming () {
        this.timingDialog = false
      },
      /**
       * Timing manage
       */
      _timingManage (item) {
        this.$router.push({ path: `/projects/${this.projectCode}/definition/list/timing/${item.code}` })
      },
      /**
       * delete
       */
      _delete (item, i) {
        // remove tow++
        if (i < 0) {
          this._batchDelete()
          return
        }
        // remove one
        this.deleteDefinition({
          code: item.code
        }).then(res => {
          this._onUpdate()
          this.$message.success(res.msg)
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },
      /**
       * edit
       */
      _edit (item) {
        this.$router.push({ path: `/projects/${this.projectCode}/definition/list/${item.code}` })
      },
      /**
       * Offline
       */
      _downline (item) {
        this._upProcessState({
          ...item,
          releaseState: 'OFFLINE'
        })
      },
      /**
       * online
       */
      _poponline (item) {
        this._upProcessState({
          ...item,
          releaseState: 'ONLINE'
        })
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
       * copy
       */
      _copyProcess (item) {
        this.copyProcess({
          codes: item.code,
          targetProjectCode: item.projectCode
        }).then(res => {
          this.strSelectCodes = ''
          this.$message.success(res.msg)
          // $('body').find('.tooltip.fade.top.in').remove()
          this._onUpdate()
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },

      /**
       * move
       */
      _moveProcess (item) {
        this.moveProcess({
          codes: item.code,
          targetProjectCode: item.projectCode
        }).then(res => {
          this.strSelectCodes = ''
          this.$message.success(res.msg)
          $('body').find('.tooltip.fade.top.in').remove()
          this._onUpdate()
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },

      _export (item) {
        this.exportDefinition({
          codes: item.code,
          fileName: item.name
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },
      /**
        * switch version in process definition version list
        *
        * @param version the version user want to change
        * @param processDefinitionCode the process definition code
        * @param fromThis fromThis
      */
      mVersionSwitchProcessDefinitionVersion ({ version, processDefinitionCode, fromThis }) {
        this.switchProcessDefinitionVersion({
          version: version,
          code: processDefinitionCode
        }).then(res => {
          this.$message.success($t('Switch Version Successfully'))
          this.$router.push({ path: `/projects/${this.projectCode}/definition/list/${processDefinitionCode}` })
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },
      /**
        * Paging event of process definition versions
        *
        * @param pageNo page number
        * @param pageSize page size
        * @param processDefinitionCode the process definition Code of page version
        * @param fromThis fromThis
      */
      mVersionGetProcessDefinitionVersionsPage ({ pageNo, pageSize, processDefinitionCode, fromThis }) {
        this.getProcessDefinitionVersionsPage({
          pageNo: pageNo,
          pageSize: pageSize,
          code: processDefinitionCode
        }).then(res => {
          this.versionData.processDefinitionVersions = res.data.totalList
          this.versionData.total = res.data.total
          this.versionData.pageSize = res.data.pageSize
          this.versionData.pageNo = res.data.currentPage
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },
      /**
        * delete one version of process definition
        *
        * @param version the version need to delete
        * @param processDefinitionCode the process definition code user want to delete
        * @param fromThis fromThis
      */
      mVersionDeleteProcessDefinitionVersion ({ version, processDefinitionCode, fromThis }) {
        this.deleteProcessDefinitionVersion({
          version: version,
          code: processDefinitionCode
        }).then(res => {
          this.$message.success(res.msg || '')
          this.mVersionGetProcessDefinitionVersionsPage({
            pageNo: 1,
            pageSize: 10,
            processDefinitionCode: processDefinitionCode,
            fromThis: fromThis
          })
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },
      _version (item) {
        this.getProcessDefinitionVersionsPage({
          pageNo: 1,
          pageSize: 10,
          code: item.code
        }).then(res => {
          let processDefinitionVersions = res.data.totalList
          let total = res.data.total
          let pageSize = res.data.pageSize
          let pageNo = res.data.currentPage

          this.versionData.processDefinition = item
          this.versionData.processDefinitionVersions = processDefinitionVersions
          this.versionData.total = total
          this.versionData.pageNo = pageNo
          this.versionData.pageSize = pageSize
          this.drawer = true
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },

      closeVersion () {
        this.drawer = false
      },

      _batchExport () {
        this.exportDefinition({
          codes: this.strSelectCodes,
          fileName: 'process_' + new Date().getTime()
        }).then(res => {
          this._onUpdate()
          this.checkAll = false
          this.strSelectCodes = ''
        }).catch(e => {
          this.strSelectCodes = ''
          this.checkAll = false
          this.$message.error(e.msg)
        })
      },
      /**
       * Batch Copy
       */
      _batchCopy () {
        this.relatedItemsDialog = true
        this.tmp = false
      },
      onBatchCopy (projectCode) {
        this._copyProcess({ code: this.strSelectCodes, projectCode: projectCode })
        this.relatedItemsDialog = false
      },
      closeRelatedItems () {
        this.relatedItemsDialog = false
      },
      /**
       * _batchMove
       */
      _batchMove () {
        this.tmp = true
        this.relatedItemsDialog = true
      },
      onBatchMove (projectCode) {
        this._moveProcess({ code: this.strSelectCodes, projectCode: projectCode })
        this.relatedItemsDialog = false
      },
      /**
       * Edit state
       */
      _upProcessState (o) {
        this.editProcessState(o).then(res => {
          this.$message.success(res.msg)
          $('body').find('.tooltip.fade.top.in').remove()
          this._onUpdate()
        }).catch(e => {
          this.$message.error(e.msg || '')
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
      },
      /**
       * batch delete
       */
      _batchDelete () {
        this.batchDeleteDefinition({
          codes: this.strSelectCodes
        }).then(res => {
          this._onUpdate()
          this.checkAll = false
          this.strSelectCodes = ''
          this.$message.success(res.msg)
        }).catch(e => {
          this.strSelectCodes = ''
          this.checkAll = false
          this.$message.error(e.msg || '')
        })
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
    components: { mVersions, mInstanceList, mStart, mTiming, mRelatedItems }
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
