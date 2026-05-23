<template>
    <span>
<!--        DEFAULT(0,"未开始",""),-->
<!--        WS1(1,"暂停/待命","工序:待命(工序未轮到,待命状态)"),-->
<!--        WS2(2,"生产完成",""),-->
<!--        WS4(4,"生产中","已接收材料"),-->

        <el-row v-if="value==0" :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-tag
                    type="danger">
                    未开始
                </el-tag>
            </el-col>
        </el-row>
        <el-row v-else-if="value==0x10000" :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-tag
                    type="info">
                    已接收
                </el-tag>
            </el-col>
        </el-row>
        <el-row v-else-if="value==5" :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-tag
                    type="info">
                    等待中
                </el-tag>
            </el-col>
        </el-row>
        <el-row v-else :gutter="10" class="mb8">
            <el-col
                v-if="value&2"
                :span="1.5">
                <el-tag
                    type="success" >
                    已完成
                </el-tag>
            </el-col>
            <el-col
                v-if="value&4"
                :span="1.5">
                <el-tag
                    type="warning">
                    生产中
                </el-tag>
            </el-col>
        </el-row>

    </span>
</template>
<script>


import { findRowInListFromAttrName } from '/src/utils/wangbao'

export default {
    name: "ProTaskWorkStatusLabel",
    components: {
    },
    props: {
        value: [String, Number],
    },
    data() {
        return {
        }
    },
    computed: {
        size() {
            return 0
        }
    },
    created() {
        this.initComponents();
    },
    mounted() {
        this.initComponents();
    },
    watch: {
        value: {
            handler(val) {
            },
            immediate: true,
        },
    },
    methods: {
        initComponents(){
        },
    }

}
</script>


<style lange='less' scoped>
</style>
