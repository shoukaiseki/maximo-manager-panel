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
                <el-input v-model="imageFilter" placeholder="搜索图片名称" clearable class="mas-img-filter" />
                <div v-for="img of filteredImages" :key="img.path" class="mas-img-item">
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

        <SksElImageViewer v-if="currentImage" :visible.sync="showViewer" :url="currentImage.src" :title="currentImage.name || '图片预览'" :scale="3" />
    </div>
</template>

<script>
import svgIcons from './svg-icons'
import elementIcons from './element-icons'
import clipboard from '@/utils/clipboard'
import SksElImageViewer from '@/components/sks-el-image-viewer'

export default {
    name: 'Icons',
    components: { SksElImageViewer },

    data() {
        return {
            svgIcons,
            elementIcons,
            masImages: [],
            imageFilter: '',
            showViewer: false,
            previewSrcList: [],
            currentImage: null
        }
    },
    computed: {
        filteredImages() {
            if (!this.imageFilter) {
                return this.masImages
            }
            const keywords = this.imageFilter.toLowerCase().split(/\s+/).filter(k => k)
            return this.masImages.filter(img => {
                const name = img.name.toLowerCase()
                return keywords.every(keyword => name.includes(keyword))
            })
        }
    },
    created() {
        this.loadMasImages()
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
        },
        loadMasImages() {
            console.log('---------loadMasImages')
            const images = []
            try {
                const context = require.context('../../../assets/mas/images', true, /\.(png|jpg|jpeg|gif|svg)$/i)
                console.log('require.context keys:', context.keys())
                context.keys().forEach(path => {
                    try {
                        const name = path.replace('./', '')
                        const src = context(path)
                        const imgSrc = typeof src === 'string' ? src : (src.default || src)
                        images.push({
                            path: path,
                            name: name,
                            src: imgSrc
                        })
                    } catch (e) {
                        console.warn('Failed to load image:', path, e.message)
                    }
                })
                images.sort((a, b) => a.name.localeCompare(b.name))
                this.masImages = images
                console.log('Loaded masImages:', images.length)
            } catch (e) {
                console.error('loadMasImages error:', e)
            }
        },
        viewImage(img) {
            console.log('viewImage:', img.name, img.src)
            this.currentImage = img
            this.previewSrcList = [img.src]
            this.showViewer = true
        },
        handleCloseViewer() {
            this.showViewer = false
            this.previewSrcList = []
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

    .mas-img-filter {
        width: 300px;
        margin: 15px;
        float: right;
    }

    .mas-img-item {
        margin: 15px;
        text-align: center;
        float: left;
        cursor: pointer;
    }

    .mas-img-wrapper {
        padding: 8px;
        border: 1px solid #ebeef5;
        border-radius: 4px;
        transition: all 0.2s;

        &:hover {
            border-color: #409eff;
            box-shadow: 0 0 4px rgba(64, 158, 255, 0.3);
        }
    }

    .mas-img-preview {
        width: 64px;
        height: 64px;
        object-fit: contain;
        display: block;
        margin: 0 auto 8px;
        background: #fafafa;
        border-radius: 2px;
    }

    .mas-img-name {
        font-size: 12px;
        color: #606266;
        max-width: 80px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        display: block;
        margin-bottom: 4px;
    }
}
</style>
