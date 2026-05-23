<template>
  <el-dialog title="后端配置" :visible.sync="visible" width="480px" @close="handleClose">
    <el-form :model="formData" label-width="100px" class="settings-form">
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
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSave">保存配置</el-button>
    </div>
  </el-dialog>
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
      formData: {
        authType: 'maxauth',
        maxauth: '',
        apiKey: ''
      }
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.loadSettings()
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
          this.formData = {
            authType: settings.authType || 'maxauth',
            maxauth: settings.maxauth || '',
            apiKey: settings.apiKey || ''
          }
        } catch (e) {
          console.error('加载配置失败', e)
        }
      } else {
        this.formData = {
          authType: 'maxauth',
          maxauth: '',
          apiKey: ''
        }
      }
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

      const settings = {
        authType: this.formData.authType,
        maxauth: this.formData.maxauth,
        apiKey: this.formData.apiKey,
        useApiKey: this.formData.authType === 'apikey'
      }

      localStorage.setItem('maximo-env-settings', JSON.stringify(settings))
      this.$message.success('配置保存成功')
      
      this.$emit('saved', settings)
      this.handleClose()
    },
    handleClose() {
      this.$emit('update:visible', false)
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