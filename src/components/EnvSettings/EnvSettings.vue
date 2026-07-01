<template>
  <div>
    <el-dialog title="后端配置" :visible="dialogVisible" width="880px" @close="handleClose" @update:visible="handleClose">
      <el-form :model="formData" label-width="100px" class="settings-form">
        <el-form-item label="环境名称">
          <el-input v-model="formData.envName" placeholder="输入环境名称" style="width: 200px; margin-right: 8px;" />
          <el-button @click="showEnvList">切换环境</el-button>
          <el-button type="primary" @click="saveEnv">保存环境</el-button>
        </el-form-item>
        <el-form-item label="认证方式">
          <el-radio-group v-model="formData.authType">
            <el-radio label="maxauth">MAXAUTH</el-radio>
            <el-radio label="apikey">API Key</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formData.authType === 'maxauth'" label="MAXAUTH">
          <el-input v-model="formData.maxauth" placeholder="base64编码的用户名:密码" />
          <span class="form-tip">格式：base64(username:password)</span>
        </el-form-item>
        <el-form-item v-if="formData.authType === 'apikey'" label="API Key">
          <el-input v-model="formData.apiKey" placeholder="API Key" />
        </el-form-item>
        <el-form-item label="管理模式">
          <el-switch v-model="formData.isManageMode" active-text="开启" inactive-text="关闭" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button native-type="button" @click="handleClose">取消</el-button>
        <el-button native-type="button" type="primary" @click="handleSave">保存配置</el-button>
      </div>
    </el-dialog>

    <el-dialog title="环境列表" :visible="envListVisible" width="400px" @close="closeEnvList">
      <el-table :data="envList" border>
        <el-table-column prop="name" label="环境名称" />
        <el-table-column prop="authType" label="认证方式">
          <template slot-scope="scope">
            {{ scope.row.authType === 'apikey' ? 'API Key' : 'MAXAUTH' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template slot-scope="scope">
            <template v-if="scope.row.name !== formData.envName">
              <el-button type="text" @click="switchEnv(scope.row)">切换</el-button>
              <el-button type="text" @click="deleteEnv(scope.row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'EnvSettings',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dialogVisible: false,
      envListVisible: false,
      envList: [],
      formData: {
        envName: '',
        authType: 'apikey',
        maxauth: '',
        apiKey: '',
        isManageMode: true
      }
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        this.dialogVisible = val
        if (val) {
          this.loadSettings()
        }
      }
    }
  },
  created() {
    this.loadSettings();
  },
  activated() {
    this.loadSettings();
  },

  methods: {
    loadSettings() {
      const saved = localStorage.getItem('maximo-env-settings')
      if (saved) {
        try {
          const settings = JSON.parse(saved)
          this.envList = settings.envs || []
          this.formData = {
            envName: settings.currentEnv || '',
            authType: settings.authType || 'maxauth',
            maxauth: settings.maxauth || '',
            apiKey: settings.apiKey || '',
            isManageMode: settings.isManageMode !== undefined ? settings.isManageMode : true
          }
        } catch (e) {
          console.error('加载配置失败', e)
          this.resetSettings()
        }
      } else {
        this.resetSettings()
      }
    },
    resetSettings() {
      this.envList = []
      this.formData = {
        envName: '',
        authType: 'maxauth',
        maxauth: '',
        apiKey: '',
        isManageMode: true
      }
    },
    saveEnv() {
      if (!this.formData.envName.trim()) {
        this.$message.error('请输入环境名称')
        return
      }
      if (this.formData.authType === 'maxauth' && !this.formData.maxauth) {
        this.$message.error('请填写 MAXAUTH')
        return
      }
      if (this.formData.authType === 'apikey' && !this.formData.apiKey) {
        this.$message.error('请填写 API Key')
        return
      }

      const envName = this.formData.envName.trim()
      const envIndex = this.envList.findIndex(env => env.name === envName)

      if (envIndex >= 0) {
        this.envList[envIndex] = {
          name: envName,
          authType: this.formData.authType,
          maxauth: this.formData.maxauth,
          apiKey: this.formData.apiKey
        }
        this.$message.success('环境配置已更新')
      } else {
        this.envList.push({
          name: envName,
          authType: this.formData.authType,
          maxauth: this.formData.maxauth,
          apiKey: this.formData.apiKey
        })
        this.$message.success('环境配置已保存')
      }

      this.saveSettings()
    },
    showEnvList() {
      this.envListVisible = true
    },
    closeEnvList() {
      this.envListVisible = false
    },
    switchEnv(env) {
      this.formData.envName = env.name
      this.formData.authType = env.authType
      this.formData.maxauth = env.maxauth
      this.formData.apiKey = env.apiKey
      this.envListVisible = false
      this.saveSettings()
      this.$message.success('已切换到环境：' + env.name)
    },
    deleteEnv(env) {
      this.$confirm('确定要删除环境 "' + env.name + '" 吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.envList = this.envList.filter(e => e.name !== env.name)
        this.saveSettings()
        this.$message.success('删除成功')
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    handleSave() {
      if (this.formData.authType === 'maxauth' && !this.formData.maxauth) {
        this.$message.error('请填写 MAXAUTH')
        return
      }
      if (this.formData.authType === 'apikey' && !this.formData.apiKey) {
        this.$message.error('请填写 API Key')
        return
      }

      this.saveSettings()
      this.sksConfig.isManageMode = this.formData.isManageMode
      this.$message.success('配置保存成功')

      this.$emit('saved', {
        authType: this.formData.authType,
        maxauth: this.formData.maxauth,
        apiKey: this.formData.apiKey,
        useApiKey: this.formData.authType === 'apikey',
        isManageMode: this.formData.isManageMode
      })
      this.handleClose()
    },
    saveSettings() {
      const settings = {
        currentEnv: this.formData.envName,
        envs: this.envList,
        authType: this.formData.authType,
        maxauth: this.formData.maxauth,
        apiKey: this.formData.apiKey,
        useApiKey: this.formData.authType === 'apikey',
        isManageMode: this.formData.isManageMode
      }
      localStorage.setItem('maximo-env-settings', JSON.stringify(settings))
    },
    handleClose() {
      this.dialogVisible = false
      this.$emit('update:visible', false)
      console.log("EnvSettings handleClose")
    }
  }
}
</script>

<style lang="scss" scoped>
.settings-form {
  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-left: 8px;
  }
}
</style>