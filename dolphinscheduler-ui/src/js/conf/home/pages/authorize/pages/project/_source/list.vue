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
  <div class="list-model project-list-model">
    <div class="table-box">
      <el-table :data="list" size="mini" style="width: 100%">
        <el-table-column type="index" :label="$t('#')" width="150"></el-table-column>
        <el-table-column prop="name" :label="$t('Project Name')" width="250"></el-table-column>
        <el-table-column prop="description" :label="$t('Description')" width="250"></el-table-column>
        <el-table-column :label="$t('Create Time')" width="250">
          <template slot-scope="scope">
            <span>{{scope.row.createTime | formatDate}}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('Update Time')" width="250">
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
                  <el-dropdown-item @click.native="_authProject(scope.row,scope.$index)">{{$t('Project')}}</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog
      v-if="authProjectDialog"
      :visible.sync="authProjectDialog"
      width="auto">
      <m-transfer :transferData="transferData" @onUpdateAuthProjectPermissions="onUpdateAuthProjectPermissions" @closeAuthProjectPermissions="closeAuthProjectPermissions"></m-transfer>
    </el-dialog>
  </div>
</template>
<script>
  import _ from 'lodash'
  import i18n from '@/module/i18n'
  import { mapActions } from 'vuex'
  import mTransfer from '@/module/components/transfer/transfer'

  export default {
    name: 'project-list',
    data () {
      return {
        list: [],
        authProjectDialog: false,
        transferData: {
          sourceListPrs: [],
          targetListPrs: [],
          type: {
            name: ''
          }
        },
        item: {}
      }
    },
    props: {
      projectList: Array,
      pageNo: Number,
      pageSize: Number
    },
    methods: {
      ...mapActions('security', ['getAuthProjectList', 'grantAuthorization']),
      _edit (item) {
        this.$emit('on-edit', item)
      },
      _authProject (item, i) {
        this.getAuthProjectList({
          code: item.code,
          type: 'user',
          category: 'projects'
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
          this.transferData.type.name = `${i18n.$t('Project Perm')}`
          this.authProjectDialog = true
        }).catch(e => {
          this.$message.error(e.msg || '')
        })
      },
      onUpdateAuthProjectPermissions (userIds) {
        this._grantAuthorization('users/grant-project-to-user', {
          userIds: userIds,
          projectId: this.item.id
        })
        this.authProjectDialog = false
      },

      closeAuthProjectPermissions () {
        this.authProjectDialog = false
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
      projectList (a) {
        this.list = []
        setTimeout(() => {
          this.list = a
        })
      }
    },
    created () {
      this.list = this.projectList
    },
    mounted () {
    },
    components: { mTransfer }
  }
</script>

<style lang="scss" rel="stylesheet/scss">
  .project-list-model {
    .project-list-poptip {
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
