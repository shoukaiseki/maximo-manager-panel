<template>
    <div v-if="visible" class="sks-image-viewer-overlay" @click.self="handleClose">
        <div class="sks-image-viewer-container" :style="{ width: width, height: height }">
            <div class="sks-image-viewer-header">
                <span class="sks-image-viewer-title">{{ title }}</span>
                <div class="sks-image-viewer-toolbar">
                    <el-button @click="zoomOut" size="small" icon="el-icon-zoom-out">缩小</el-button>
                    <span style="margin:0 10px">{{ Math.round(currentScale * 100) }}%</span>
                    <el-button @click="zoomIn" size="small" icon="el-icon-zoom-in">放大</el-button>
                    <el-button @click="resetScale" size="small" icon="el-icon-refresh">重置</el-button>
                </div>
                <div class="sks-image-viewer-slider">
                    <el-slider v-model="currentScale" :min="0.25" :max="10" :step="0.25" :show-input="true" :input-size="'small'" />
                </div>
                <el-button type="text" icon="el-icon-close" @click="handleClose" class="sks-image-viewer-close"></el-button>
            </div>
            <div class="sks-image-viewer-body">
                <img :src="url" :alt="title" class="sks-image-viewer-img"
                     :style="{ transform: 'scale(' + currentScale + ')', maxHeight: imageHeight + 'px' }" />
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'SksElImageViewer',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        url: {
            type: String,
            default: ''
        },
        title: {
            type: String,
            default: '图片预览'
        },
        width: {
            type: String,
            default: '80%'
        },
        height: {
            type: String,
            default: '500px'
        },
        imageHeight: {
            type: Number,
            default: 440
        },
        scale: {
            type: Number,
            default: 1
        }
    },
    data() {
        return {
            currentScale: this.scale
        }
    },
    watch: {
        visible(val) {
            if (val) {
                this.currentScale = this.scale
                document.body.style.overflow = 'hidden'
            } else {
                document.body.style.overflow = ''
            }
        },
        scale(val) {
            this.currentScale = val
        }
    },
    methods: {
        zoomIn() {
            if (this.currentScale < 10) {
                this.currentScale += 0.25
            }
        },
        zoomOut() {
            if (this.currentScale > 0.25) {
                this.currentScale -= 0.25
            }
        },
        resetScale() {
            this.currentScale = this.scale
        },
        handleClose() {
            this.$emit('update:visible', false)
        }
    }
}
</script>

<style lang="scss" scoped>
.sks-image-viewer-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.7);
    z-index: 999999;
    display: flex;
    align-items: center;
    justify-content: center;
}

.sks-image-viewer-container {
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
    max-height: 90%;
    display: flex;
    flex-direction: column;
}

.sks-image-viewer-header {
    padding: 12px 20px;
    border-bottom: 1px solid #ebeef5;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
}

.sks-image-viewer-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    flex-shrink: 0;
}

.sks-image-viewer-toolbar {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 1;
}

.sks-image-viewer-slider {
    width: 100%;
    max-width: 300px;
}

.sks-image-viewer-close {
    font-size: 16px;
    color: #909399;
    flex-shrink: 0;

    &:hover {
        color: #409eff;
    }
}

.sks-image-viewer-body {
    padding: 20px;
    overflow: auto;
    flex: 1;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
}

.sks-image-viewer-img {
    max-width: 100%;
    transition: transform 0.2s ease;
    transform-origin: center center;
    object-fit: contain;
}
</style>