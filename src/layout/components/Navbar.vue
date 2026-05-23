<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" />

    <div class="right-menu">
      <template v-if="device!=='mobile'">
        <div class="layout-actions">
          <el-select :value="selectedEnvKey" placeholder="选择环境" size="small" class="env-select" @change="handleEnvChange">
            <el-option v-for="env in envs" :key="env.key" :label="env.name" :value="env.key" />
          </el-select>
          <el-tooltip content="源码地址" effect="dark" placement="bottom">
            <el-button type="text" class="repo-link right-menu-item hover-effect" @click.native="openRepo">
              <svg-icon icon-class="github" class="header-link-icon" />
            </el-button>
          </el-tooltip>
        </div>

        <search id="header-search" class="right-menu-item" />

<!--          <TaskWebSocket class="right-menu-item hover-effect" >-->
<!--          </TaskWebSocket>-->
<!--         <el-tooltip content="源码地址" effect="dark" placement="bottom">-->
<!--          <ruo-yi-git id="ruoyi-git" class="right-menu-item hover-effect" />-->
<!--        </el-tooltip>-->

<!--        <el-tooltip content="文档地址" effect="dark" placement="bottom">-->
<!--          <ruo-yi-doc id="ruoyi-doc" class="right-menu-item hover-effect" />-->
<!--        </el-tooltip>-->

        <screenfull id="screenfull" class="right-menu-item hover-effect" />

        <el-tooltip content="布局大小" effect="dark" placement="bottom">
          <size-select id="size-select" class="right-menu-item hover-effect" />
        </el-tooltip>
      </template>
      <div class="username right-menu-item">
        {{username}}
      </div>
      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="click">
        <div class="avatar-wrapper">
          <img :src="avatar" class="user-avatar">
          <i class="el-icon-caret-bottom" />
        </div>
        <el-dropdown-menu slot="dropdown">
          <router-link to="/user/profile">
            <el-dropdown-item>个人中心</el-dropdown-item>
          </router-link>
          <el-dropdown-item @click.native="setting = true">
            <span>布局设置</span>
          </el-dropdown-item>
          <el-dropdown-item divided @click.native="logout">
            <span>退出登录</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '/src/components/Breadcrumb'
import Hamburger from '/src/components/Hamburger'
import Screenfull from '/src/components/Screenfull'
import SizeSelect from '/src/components/SizeSelect'
import Search from '/src/components/HeaderSearch'
import RuoYiGit from '/src/components/RuoYi/Git'
import RuoYiDoc from '/src/components/RuoYi/Doc'
import SvgIcon from '/src/components/SvgIcon'
// import { getInfo } from "/src/api/login";
// import TaskWebSocket from '/src/components/TaskWebSocket/TaskWebSocket'

export default {
  components: {
    Breadcrumb,
    Hamburger,
    Screenfull,
    SizeSelect,
    Search,
    RuoYiGit,
    RuoYiDoc,
    SvgIcon,
      // TaskWebSocket,
  },
  data(){
    return{
      username:'用户身份'
    }
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'device',
      'envs',
      'selectedEnvKey'
    ]),
    setting: {
      get() {
        return this.$store.state.settings.showSettings
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'showSettings',
          value: val
        })
      }
    }
  },
  created(){
    // getInfo().then(res=>{
    //   console.log(res.user.remark)
    //     if(res.user.remark&&res.user.remark!=''){
    //         this.username=res.user.nickName+'('+res.user.remark+')';
    //     }else{
    //         this.username=res.user.nickName;
    //     }
    // })
  },
  methods: {
    openRepo() {
      window.open('https://gitee.com/shoukaiseki/maximo-manager-panel', '_blank')
    },
    handleEnvChange(key) {
      this.$store.dispatch('env/setSelectedEnv', key)
    },
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    async logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
          this.$router.push({ path: "/login" });

        // this.$store.dispatch('LogOut').then(() => {
        //   location.href = '/index';
        // })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  .username{
    font-size: 0.85rem!important;
  }
  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color:transparent;

    &:hover {
      background: rgba(0, 0, 0, .025)
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background .3s;

        &:hover {
          background: rgba(0, 0, 0, .025)
        }
      }
    }

    .layout-actions {
      display: flex;
      align-items: center;
      gap: 14px;
    }

    .env-select {
      min-width: 120px;
    }

    .repo-link {
      height: 36px;
      display: flex;
      align-items: center;
      padding: 0;
    }

    .header-link-icon {
      width: 20px;
      height: 20px;
    }

    .avatar-container {
      margin-right: 30px;

      .avatar-wrapper {
        margin-top: 5px;
        position: relative;

        .user-avatar {
          cursor: pointer;
          width: 40px;
          height: 40px;
          border-radius: 10px;
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          position: absolute;
          right: -20px;
          top: 25px;
          font-size: 12px;
        }
      }
    }
  }
}
</style>