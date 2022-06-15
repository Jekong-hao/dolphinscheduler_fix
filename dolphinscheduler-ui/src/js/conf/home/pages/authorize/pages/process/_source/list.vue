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
  <div class="list-model process-list-model">
    <div class="table-box">
      <el-table :data="list" size="mini" style="width: 100%">
        <el-table-column type="index" :label="$t('#')" width="100"></el-table-column>
        <el-table-column prop="name" :label="$t('Process Name')"></el-table-column>
        <el-table-column prop="projectName" :label="$t('Project Name')"></el-table-column>
        <el-table-column prop="description" :label="$t('Description')" width="200"></el-table-column>
        <el-table-column :label="$t('Create Time')" width="200">
          <template slot-scope="scope">
            <span>{{scope.row.createTime | formatDate}}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Update Time')" width="200">
          <template slot-scope="scope">
            <span>{{scope.row.updateTime | formatDate}}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Operation')" width="100" fixed="right">
          <template slot-scope="scope">
            <el-tooltip :content="$t('Authorize')" placement="top">
              <el-dropdown trigger="click">
                <el-button type="warning" size="mini" icon="el-icon-user" circle></el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item @click.native="_authProcess(scope.row,scope.$index)">{{$t('Process')}}</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog
      v-if="authProcessDialog"
      :visible.sync="authProcessDialog"
      width="auto">
      <m-transfer :transferData="transferData" @onUpdateAuthProcessPermissions="onUpdateAuthProcessPermissions" @closeAuthProcessPermissions="closeAuthProcessPermissions"></m-transfer>
    </el-dialog>
  </div>
</template>
<script>
  import _ from 'lodash'
  import i18n from '@/module/i18n'
  import { mapActions } from 'vuex'
  import mTransfer from '@/module/components/transfer/transfer'

  export default {
    name: 'process-list',
    data () {
      return {
        list: [],
        authProcessDialog: false,
        transferData: {
          sourceListPrs: [],
          targetListPrs: [],
          type: {
            name: ''
          }
        },
        item: {},
        authDataSourceDialog: false,
        authUdfFuncDialog: false,
        resourceData: {
          fileSourceList: [],
          udfSourceList: [],
          fileTargetList: [],
          udfTargetList: [],
          type: {
            name: ''
          }
        },
        resourceDialog: false
      }
    },
    props: {
      processList: Array,
      pageNo: Number,
      pageSize: Number
    },
    methods: {
      ...mapActions('security', ['getAuthProcessList', 'grantAuthorization']),
      _edit (item) {
        this.$emit('on-edit', item)
      },
      _authProcess (item, i) {
        this.getAuthProcessList({
          code: item.code,
          type: 'user',
          category: 'process-authority'
        }).then(data => {
          let sourceListPrs = _.map(data[0], v => {
            return {
              id: v.id,
              name: v.userName
            }
          })
          console.log('____________' + sourceListPrs.name)
          let targetListPrs = _.map(data[1], v => {
            return {
              id: v.id,
              name: v.userName
            }
          })
          this.item = item
          this.transferData.sourceListPrs = sourceListPrs
          this.transferData.targetListPrs = targetListPrs
          this.transferData.type.name = `${i18n.$t('Process Perm')}`
          this.authProcessDialog = true
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },
      onUpdateAuthProcessPermissions (userIds) {
        this._grantAuthorization('users/grant-process-to-user', {
          userIds: userIds,
          processId: this.item.id
        })
        this.authProcessDialog = false
      },

      closeAuthProjectPermissions () {
        this.authProcessDialog = false
      },

      /*
        getAllLeaf
       */
      getAllLeaf (data) {
        let result = []
        let getLeaf = (data) => {
          data.forEach(item => {
            if (item.children.length === 0) {
              result.push(item)
            } else {
              getLeaf(item.children)
            }
          })
        }
        getLeaf(data)
        return result
      },

      _grantAuthorization (api, param) {
        this.grantAuthorization({
          api: api,
          param: param
        }).then(res => {
          this.$message.success(res.msg)
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      }
    },
    watch: {
      processList (a) {
        this.list = []
        setTimeout(() => {
          this.list = a
        })
      }
    },
    created () {
      this.list = this.processList
    },
    mounted () {
    },
    components: { mTransfer }
  }
</script>

<style lang="scss" rel="stylesheet/scss">
  .process-list-model {
    .process-list-poptip {
      min-width: 90px !important;
      .auth-select-box {
        a {
          font-size: 14px;
          height: 28px;
          line-height: 28px;
          display: block;
        }
      }
    }
  }
</style>
