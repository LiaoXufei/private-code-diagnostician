<template>
  <div class="virtual-file-tree">
    <!-- 搜索框 -->
    <div class="tree-header">
      <a-input
        v-model:value="searchText"
        placeholder="搜索文件..."
        :prefix="h(SearchOutlined)"
        allowClear
        @input="handleSearch"
      />
      <div class="tree-stats">
        <span class="file-count">{{ filteredFiles.length }} 个文件</span>
        <span class="folder-count">{{ folderCount }} 个文件夹</span>
      </div>
    </div>

    <!-- 虚拟滚动列表 -->
    <div class="tree-container" ref="containerRef">
      <div class="tree-viewport" ref="viewportRef" @scroll="handleScroll">
        <div
          class="tree-content"
          :style="{
            height: `${totalHeight}px`,
            transform: `translateY(${offsetY}px)`
          }"
        >
          <div
            v-for="item in visibleItems"
            :key="item.id"
            class="tree-item"
            :class="{
              'tree-item-selected': selectedKey === item.id,
              'tree-item-folder': item.type === 'folder',
              'tree-item-file': item.type === 'file'
            }"
            :style="{
              paddingLeft: `${item.level * 20 + 12}px`
            }"
            @click="handleClick(item)"
            @dblclick="handleDoubleClick(item)"
            @contextmenu="handleContextMenu(item, $event)"
          >
            <!-- 展开/折叠图标 -->
            <span
              v-if="item.type === 'folder'"
              class="tree-expand-icon"
              @click.stop="handleExpand(item)"
            >
              <RightOutlined v-if="!item.expanded" />
              <DownOutlined v-else />
            </span>
            <span v-else class="tree-expand-placeholder"></span>

            <!-- 文件图标 -->
            <span class="tree-icon">
              <component
                :is="getFileIcon(item)"
                :style="{ color: getFileIconColor(item) }"
              />
            </span>

            <!-- 文件名 -->
            <span class="tree-label" :title="item.name">
              {{ highlightMatch(item.name) }}
            </span>

            <!-- 文件大小 -->
            <span v-if="item.type === 'file'" class="tree-size">
              {{ formatFileSize(item.size || 0) }}
            </span>

            <!-- 右侧操作按钮 -->
            <span class="tree-actions">
              <a-dropdown
                v-if="item.type === 'file'"
                trigger="click"
                position="bottomRight"
              >
                <MoreOutlined class="action-icon" />
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handlePreview(item)">
                      <EyeOutlined />
                      预览
                    </a-menu-item>
                    <a-menu-item @click="handleDownload(item)">
                      <DownloadOutlined />
                      下载
                    </a-menu-item>
                    <a-menu-item @click="handleDelete(item)">
                      <DeleteOutlined />
                      删除
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{
        left: `${contextMenu.x}px`,
        top: `${contextMenu.y}px`
      }"
      @click="hideContextMenu"
    >
      <div class="menu-content">
        <div class="menu-item" @click="handlePreview(contextMenu.item)">
          <EyeOutlined />
          预览
        </div>
        <div class="menu-item" @click="handleDownload(contextMenu.item)">
          <DownloadOutlined />
          下载
        </div>
        <div class="menu-item danger" @click="handleDelete(contextMenu.item)">
          <DeleteOutlined />
          删除
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  RightOutlined,
  DownOutlined,
  EyeOutlined,
  DownloadOutlined,
  DeleteOutlined,
  MoreOutlined,
  FolderOutlined,
  FolderOpenOutlined,
  FileOutlined,
  CodeOutlined,
  FileTextOutlined,
  FileMarkdownOutlined
} from '@ant-design/icons-vue'

const props = defineProps({
  files: {
    type: Array,
    default: () => []
  },
  height: {
    type: Number,
    default: 400
  },
  itemHeight: {
    type: Number,
    default: 32
  }
})

const emit = defineEmits(['select', 'expand', 'preview', 'download', 'delete'])

// 响应式数据
const containerRef = ref(null)
const viewportRef = ref(null)
const searchText = ref('')
const selectedKey = ref(null)
const expandedKeys = ref(new Set())

// 虚拟滚动相关
const scrollTop = ref(0)
const viewportHeight = ref(400)
const containerWidth = ref(0)

// 右键菜单
const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  item: null
})

// 处理后的文件数据
const processedFiles = ref([])
const flatFiles = ref([])

// 计算属性
const filteredFiles = computed(() => {
  if (!searchText.value) {
    return processedFiles.value
  }

  const search = searchText.value.toLowerCase()
  return processedFiles.value.filter(file =>
    file.name.toLowerCase().includes(search) ||
    file.path.toLowerCase().includes(search)
  )
})

const folderCount = computed(() => {
  return filteredFiles.value.filter(file => file.type === 'folder').length
})

// 扁平化文件列表（用于虚拟滚动）
const flattenedFiles = computed(() => {
  const result = []
  const flatten = (items, level = 0) => {
    items.forEach(item => {
      result.push({
        ...item,
        level,
        visible: item.level === 0 || isParentExpanded(item.parentId)
      })

      if (item.type === 'folder' && item.children && expandedKeys.value.has(item.id)) {
        flatten(item.children, level + 1)
      }
    })
  }

  flatten(filteredFiles.value.filter(item => !item.parentId))
  return result
})

// 虚拟滚动计算
const totalHeight = computed(() => {
  return flattenedFiles.value.length * props.itemHeight
})

const startIndex = computed(() => {
  return Math.floor(scrollTop.value / props.itemHeight)
})

const endIndex = computed(() => {
  return Math.min(
    startIndex.value + Math.ceil(viewportHeight.value / props.itemHeight) + 1,
    flattenedFiles.value.length
  )
})

const visibleItems = computed(() => {
  return flattenedFiles.value.slice(startIndex.value, endIndex.value)
})

const offsetY = computed(() => {
  return startIndex.value * props.itemHeight
})

// 方法
const buildFileTree = (files) => {
  const map = new Map()
  const root = []

  // 创建映射
  files.forEach(file => {
    map.set(file.id, {
      ...file,
      children: [],
      expanded: expandedKeys.value.has(file.id)
    })
  })

  // 构建树结构
  files.forEach(file => {
    const node = map.get(file.id)
    if (file.parentId && map.has(file.parentId)) {
      const parent = map.get(file.parentId)
      parent.children.push(node)
    } else {
      root.push(node)
    }
  })

  return root
}

const isParentExpanded = (parentId) => {
  let current = parentId
  while (current) {
    if (expandedKeys.value.has(current)) {
      return true
    }
    const parent = map.get(current)?.parentId
    if (!parent) break
    current = parent
  }
  return false
}

const getFileIcon = (item) => {
  if (item.type === 'folder') {
    return item.expanded ? FolderOpenOutlined : FolderOutlined
  }

  const ext = item.name.split('.').pop()?.toLowerCase()
  const iconMap = {
    html: FileTextOutlined,
    htm: FileTextOutlined,
    css: FileTextOutlined,
    scss: FileTextOutlined,
    sass: FileTextOutlined,
    less: FileTextOutlined,
    js: CodeOutlined,
    jsx: CodeOutlined,
    ts: CodeOutlined,
    tsx: CodeOutlined,
    json: FileTextOutlined,
    md: FileMarkdownOutlined,
    markdown: FileMarkdownOutlined
  }

  return iconMap[ext] || FileOutlined
}

const getFileIconColor = (item) => {
  if (item.type === 'folder') {
    return '#1890ff'
  }

  const ext = item.name.split('.').pop()?.toLowerCase()
  const colorMap = {
    html: '#E34C26',
    htm: '#E34C26',
    css: '#1572B6',
    scss: '#CF649A',
    sass: '#CF649A',
    less: '#1D365D',
    js: '#F7DF1E',
    jsx: '#F7DF1E',
    ts: '#3178C6',
    tsx: '#3178C6',
    json: '#000000',
    md: '#083FA1',
    markdown: '#083FA1'
  }

  return colorMap[ext] || '#666666'
}

const highlightMatch = (text) => {
  if (!searchText.value) return text

  const search = searchText.value
  const reg = new RegExp(`(${search})`, 'gi')
  return text.replace(reg, '<mark>$1</mark>')
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const handleScroll = (e) => {
  scrollTop.value = e.target.scrollTop
}

const handleClick = (item) => {
  selectedKey.value = item.id
  emit('select', item)
}

const handleDoubleClick = (item) => {
  if (item.type === 'folder') {
    handleExpand(item)
  } else {
    emit('preview', item)
  }
}

const handleExpand = (item) => {
  if (item.type === 'folder') {
    if (expandedKeys.value.has(item.id)) {
      expandedKeys.value.delete(item.id)
    } else {
      expandedKeys.value.add(item.id)
    }
    emit('expand', item.id, expandedKeys.value.has(item.id))
  }
}

const handleSearch = () => {
  // 搜索逻辑在computed中处理
}

const handleContextMenu = (item, event) => {
  if (item.type === 'file') {
    contextMenu.value = {
      visible: true,
      x: event.clientX,
      y: event.clientY,
      item
    }
  }
}

const hideContextMenu = () => {
  contextMenu.value.visible = false
}

const handlePreview = (item) => {
  emit('preview', item)
  hideContextMenu()
}

const handleDownload = (item) => {
  emit('download', item)
  hideContextMenu()
}

const handleDelete = (item) => {
  emit('delete', item)
  hideContextMenu()
}

// 监听器
const updateViewportSize = () => {
  if (viewportRef.value) {
    viewportHeight.value = viewportRef.value.clientHeight
    containerWidth.value = viewportRef.value.clientWidth
  }
}

const handleClickOutside = (event) => {
  if (contextMenu.value.visible) {
    hideContextMenu()
  }
}

// 监听files变化
watch(() => props.files, (newFiles) => {
  processedFiles.value = buildFileTree(newFiles)
}, { immediate: true, deep: true })

// 监听高度变化
watch(() => props.height, (newHeight) => {
  viewportHeight.value = newHeight
})

onMounted(() => {
  nextTick(() => {
    updateViewportSize()
  })

  window.addEventListener('resize', updateViewportSize)
  window.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewportSize)
  window.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.virtual-file-tree {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
}

.tree-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  display: flex;
  align-items: center;
  gap: 12px;
}

.tree-stats {
  margin-left: auto;
  font-size: 12px;
  color: #666;
  display: flex;
  gap: 12px;
}

.tree-container {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.tree-viewport {
  height: 100%;
  overflow-y: auto;
  position: relative;
}

.tree-content {
  position: relative;
  will-change: transform;
}

.tree-item {
  display: flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: all 0.2s ease;
  position: relative;
}

.tree-item:hover {
  background: #f5f5f5;
}

.tree-item-selected {
  background: #e6f7ff;
}

.tree-item-file {
  color: #262626;
}

.tree-item-folder {
  font-weight: 500;
  color: #1890ff;
}

.tree-expand-icon {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 4px;
  font-size: 12px;
  color: #666;
}

.tree-expand-placeholder {
  width: 16px;
  height: 16px;
  margin-right: 4px;
}

.tree-icon {
  margin-right: 8px;
  font-size: 14px;
}

.tree-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #262626;
}

.tree-label :deep(mark) {
  background: #ffeb3b;
  color: #000;
  padding: 1px 2px;
  border-radius: 2px;
}

.tree-size {
  font-size: 11px;
  color: #999;
  margin-left: 8px;
}

.tree-actions {
  opacity: 0;
  transition: opacity 0.2s ease;
  margin-left: 8px;
}

.tree-item:hover .tree-actions {
  opacity: 1;
}

.action-icon {
  cursor: pointer;
  color: #666;
  font-size: 12px;
  padding: 4px;
  border-radius: 2px;
}

.action-icon:hover {
  background: #f0f0f0;
  color: #1890ff;
}

.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 120px;
  padding: 4px 0;
}

.menu-content {
  width: 100%;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 12px;
  color: #262626;
  transition: all 0.2s ease;
}

.menu-item:hover {
  background: #f5f5f5;
}

.menu-item.danger {
  color: #ff4d4f;
}

.menu-item.danger:hover {
  background: #fff2f0;
}

/* 滚动条样式 */
.tree-viewport::-webkit-scrollbar {
  width: 8px;
}

.tree-viewport::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.tree-viewport::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.tree-viewport::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>