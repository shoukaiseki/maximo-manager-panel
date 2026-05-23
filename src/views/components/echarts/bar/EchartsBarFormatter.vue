<template>
  <div class="count-chart-wrapper">
    <h3>ApacheEcharts</h3>
    <div class="echart-bar" ref="chartRef"></div>
  </div>

</template>

<script>
import echarts from "echarts";
require('echarts/theme/macarons')

import '@/components/ApacheEcharts/customed.js'
import '@/components/ApacheEcharts/dark.js'

export default {
  name: 'EchartsBarFormatter',
  components: {
  },
  data() {
    return {
      chart3:{},
      chartdata3:{
        title: {
          text: '各模具制造耗时',
          subtext: '预计耗时/实际耗时',
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross',
            crossStyle: {
              color: '#999'
            }
          },
          formatter:function(datas){
            console.log("tooltipFormatter",datas)
            let str=[];
            str.push(datas[0].name);
            for (let i = 0; i < datas.length; i++) {
              const item=datas[i];
              let items = `${item.marker}${item.seriesName}:${item.value}(天)`
              str.push(items)
              if(item.seriesName=="预计耗时"){
                items = `${item.marker}计划开始时间:${item.data.data.plannedStartTime}`
                str.push(items)
                if(item.data.data.plannedEndTime==undefined){
                  items = `${item.marker}计划结束时间:未完成}`
                  str.push(items)
                }else{
                  items = `${item.marker}计划结束时间:${item.data.data.plannedEndTime}`
                  str.push(items)
                }
              }else{
                items = `${item.marker}实际开始时间:${item.data.data.actualStartTime}`
                str.push(items)
                if(item.data.data.actualEndTime==undefined){
                  items = `${item.marker}实际结束时间:未完成}`
                  str.push(items)
                }else{
                  items = `${item.marker}实际结束时间:${item.data.data.actualEndTime}`
                  str.push(items)
                }
              }
            }
            return str.join("<br>")
          }
        },
        legend: {
          data: ['预计耗时', '实际耗时']
        },
        xAxis: [
          {
            "type": "category",
            "data": [
              "喷臂",
              "齿轮",
              "架子"
            ],
            "axisPointer": {
              "type": "shadow"
            }
          }
        ],
        yAxis: [
          {
            type: 'value',
            // name: '耗时',
            min: 0,
            max: 30,
            interval: 10,
            axisLabel: {
              formatter: '{value} 天'
            }
          }
        ],
        series: [
          {
            "name": "预计耗时",
            "type": "bar",
            "data": [
              {
                "value": "27.06",
                "data": {
                  "plannedStartTime": "2021-02-24 02:10:30",
                  "plannedEndTime": "2021-03-23 03:40:10",
                  "plannedTimeConsuming": 0,
                  "actualStartTime": "2021-02-22 11:19:21",
                  "actualEndTime": null,
                  "actualTimeConsuming": 0,
                }
              },
              {
                "value": "27.17",
                "data": {
                  "plannedStartTime": "2021-02-22 11:05:04",
                  "plannedEndTime": "2021-03-21 15:04:53",
                  "plannedTimeConsuming": 0,
                  "actualStartTime": "2021-02-22 13:38:15",
                  "actualEndTime": "2021-03-19 16:25:14",
                  "actualTimeConsuming": 0,
                }
              },
              {
                "value": "7.17",
                "data": {
                  "plannedStartTime": "2021-03-21 11:05:04",
                  "plannedEndTime": "2021-03-28 15:05:04",
                  "plannedTimeConsuming": 0,
                  "actualStartTime": "2021-02-17 14:12:08",
                  "actualEndTime": null,
                  "actualTimeConsuming": 0,
                }
              }
            ]
          },
          {
            "name": "实际耗时",
            "type": "bar",
            "data": [
              {
                "value": "7.23",
                "data": {
                  "plannedStartTime": "2021-02-24 02:10:30",
                  "plannedEndTime": "2021-03-23 03:40:10",
                  "plannedTimeConsuming": 0,
                  "actualStartTime": "2021-02-22 11:19:21",
                  "actualEndTime": null,
                  "actualTimeConsuming": 0,
                }
              },
              {
                "value": "25.12",
                "data": {
                  "plannedStartTime": "2021-02-22 11:05:04",
                  "plannedEndTime": "2021-03-21 15:04:53",
                  "actualStartTime": "2021-02-22 13:38:15",
                  "actualEndTime": "2021-03-19 16:25:14",
                  "description": ""
                }
              },
              {
                "value": "12.11",
                "data": {
                  "plannedStartTime": "2021-03-21 11:05:04",
                  "plannedEndTime": "2021-03-28 15:05:04",
                  "plannedTimeConsuming": 0,
                  "actualStartTime": "2021-02-17 14:12:08",
                  "actualEndTime": null,
                  "actualTimeConsuming": 0,
                }
              }
            ]
          }
        ]
      },
    };
  },
  mounted() {
    //created 初始化 echarts 会出错
    console.log("EchartsBarFormatter.mounted")
    this.initPage();
  },
  methods: {
    initPage(){
      this.chart3 = echarts.init(this.$refs.chartRef,'dark',{ renderer: 'svg' });
      this.chart3.setOption(this.chartdata3);
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
  min-width: 500px;
  width: 100%;
  height: 400px;
}

</style>
