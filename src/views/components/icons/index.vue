<template>
    <div class="icons-container">
        <aside>
            <a href="https://gitee.com/shoukaiseki/vue-element-admin-site/blob/master/docs/guide/advanced/icon.md" target="_blank">
                添加方式
            </a>
        </aside>
        <el-tabs type="border-card">
            <el-tab-pane label="Icons">
                <div v-for="item of svgIcons" :key="item" @click="handleClipboard(generateIconCode(item),$event)">
                    <el-tooltip placement="top">
                        <div slot="content">
                            {{ generateIconCode(item) }}
                        </div>
                        <div class="icon-item">
                            <svg-icon :icon-class="item" class-name="disabled" />
                            <span>{{ item }}</span>
                        </div>
                    </el-tooltip>
                </div>
            </el-tab-pane>
            <el-tab-pane label="Element-UI Icons">
                <div v-for="item of elementIcons" :key="item" @click="handleClipboard(generateElementIconCode(item),$event)">
                    <el-tooltip placement="top">
                        <div slot="content">
                            {{ generateElementIconCode(item) }}
                        </div>
                        <div class="icon-item">
                            <i :class="'el-icon-' + item" />
                            <span>{{ item }}</span>
                        </div>
                    </el-tooltip>
                </div>
            </el-tab-pane>
            <el-tab-pane label="MAS Images">
                <div v-for="img of masImages" :key="img.path" class="mas-img-item">
                    <el-tooltip placement="top">
                        <div slot="content">
                            <span>点击复制: {{ img.name }}</span>
                            <br>
                            <span style="font-size:12px;color:#999">点击查看按钮预览</span>
                        </div>
                        <div class="mas-img-wrapper" @click="handleClipboard(img.name, $event)">
                            <img :src="img.src" :alt="img.name" class="mas-img-preview" />
                            <span class="mas-img-name">{{ img.name }}</span>
                            <el-button size="mini" icon="el-icon-view" @click.stop="viewImage(img)" type="text">查看</el-button>
                        </div>
                    </el-tooltip>
                </div>
            </el-tab-pane>
        </el-tabs>

        <el-dialog title="图片预览" :visible.sync="imagePreviewVisible" width="70%" append-to-body>
            <div class="image-preview-container">
                <div class="image-preview-controls">
                    <el-button @click="zoomOut" size="small" icon="el-icon-zoom-out">缩小</el-button>
                    <span style="margin:0 10px">{{ Math.round(imageScale * 100) }}%</span>
                    <el-button @click="zoomIn" size="small" icon="el-icon-zoom-in">放大</el-button>
                    <el-button @click="resetScale" size="small" icon="el-icon-refresh">重置</el-button>
                </div>
                <div class="image-preview-wrapper">
                    <img :src="currentImage?.src" :alt="currentImage?.name" 
                         class="preview-image" 
                         :style="{ transform: `scale(${imageScale})` }" />
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import svgIcons from './svg-icons'
import elementIcons from './element-icons'
import clipboard from '@/utils/clipboard'

export default {
    name: 'Icons',
    data() {
        return {
            svgIcons,
            elementIcons
        }
    },
    methods: {
        generateIconCode(symbol) {
            return `<svg-icon icon-class="${symbol}" />`
        },
        generateElementIconCode(symbol) {
            return `<i class="el-icon-${symbol}" />`
        },
        handleClipboard(text, event) {
            clipboard(text, event)
        }
    }
}
</script>

<style lang="scss" scoped>
.icons-container {
    margin: 10px 20px 0;
    overflow: hidden;

    .icon-item {
        margin: 20px;
        height: 85px;
        text-align: center;
        width: 100px;
        float: left;
        font-size: 30px;
        color: #24292e;
        cursor: pointer;
    }

    span {
        display: block;
        font-size: 16px;
        margin-top: 10px;
    }

    .disabled {
        pointer-events: none;
    }
}
</style>
