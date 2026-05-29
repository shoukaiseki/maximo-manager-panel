<template>
  <div class="app-container">
    <el-button type="cyan" @click="jump('/sks-components/icons')">图标库</el-button>
    <!--            <div class="icon-list">-->
    <!--                <div v-for="(item, index) in iconList" :key="index" @click="selectedIcon(item)">-->
    <!--                    <svg-icon :icon-class="item" style="height: 30px;width: 16px;" />-->
    <!--                    <span>{{ item }}</span>-->
    <!--                </div>-->
    <!--            </div>-->
    <svg-icon icon-class="404" style="height: 30px;width: 16px;" />
    <i class="el-icon-success" />

    <h1>Apache Echarts</h1>
    <el-button type="cyan" @click="jump('/sks-components/echarts/bar')">formatter</el-button>
  </div>

</template>

<script>
import icons from '@/components/IconSelect/requireIcons'
export default {
  name: 'sksComponentIcons',
  data() {
    return {
      //是否为超管
      isAdmin: false,
      //是否为管理员
      isSystem: false,
      iconList: icons,
    };
  },
  created() {
    this.isAdmin=this.checkRole(['admin']);
    this.isSystem=this.checkRole(['system'])||this.isAdmin;
  },
  methods: {
    goTarget(href) {
      window.open(href, "_blank");
    },
    jump(path){
      this.$router.push({
        path: path
      })
    }
  },
}
</script>

<style scoped>

</style>
