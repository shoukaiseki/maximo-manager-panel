<template>
  <div class="count-chart-wrapper">
    <h3>ApacheEcharts</h3>
    <div class="echart-bar" ref="chartRef"></div>
  </div>

</template>

<script>
import echarts from 'echarts';
require('echarts/theme/macarons') // echarts theme
import '@/components/ApacheEcharts/customed.js'
import '@/components/ApacheEcharts/dark.js'
import '@/components/ApacheEcharts/macarons.js'

export default {
  name: 'EchartsBarBackground',
  components: {
  },
  data() {
    return {

    }
  },
  mounted() {
    //created 初始化 echarts 会出错
    this.initPage();
  },
  methods: {
    initPage(){
      console.log("dom.",this.$refs.chartRef)
      var myChart = echarts.init(this.$refs.chartRef,'customed',{ renderer: 'svg' });
      var option;

      option = {
        tmpId: new Date(),
        xAxis: {
          type: 'category',
          data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          data: [120, 200, 150, 80, 70, 110, 130],
          type: 'bar',
          showBackground: true,
          backgroundStyle: {
            color: 'rgba(180, 180, 180, 0.2)'
          }
        }]
      };

      myChart.setOption(option);
      myChart.resize();
      console.log("myChart.resize()")
    },

  },
}
</script>

<style scoped>
.count-chart-wrapper {
  width: 100%;
  margin: 0 auto;
}
.echart-bar{
  position: relative;
  margin: 0 auto;
  width: 100%;
  height: 800px;
}
</style>
