<template>

  <div class="app-container">
    <div>
      <pre>
        主题配色方案自定义/下载 https://echarts.apache.org/zh/theme-builder.html
        //设置主题,
        import '@/components/ApacheEcharts/dark.js'
        this.chart3 = echarts.init(this.$refs.chartRef,'dark',{ renderer: 'svg' });
      </pre>
    </div>
    <el-tabs v-model="defaultTabName">
      <el-tab-pane  label="formatter" name="1" :key="1" >
        <EchartsBarFormatter  v-if="'1'===defaultTabName"/>
      </el-tab-pane>
      <el-tab-pane label="background" name="2" :key="2" >
        <EchartsBarBackground  v-if="'2'===defaultTabName" />
      </el-tab-pane>
    </el-tabs>
  </div>

</template>

<script>
import EchartsBarFormatter from './EchartsBarFormatter'
import EchartsBarBackground from './EchartsBarBackground'

export default {
  name: 'EchartsBarIndex',

  components: {
    EchartsBarFormatter,
    EchartsBarBackground,
  },
  data(){
    return {
      defaultTabName: "1",
    }
  },
  methods:{
  }
}
</script>

<style scoped>

</style>
