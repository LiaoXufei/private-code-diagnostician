<template>
  <div class="file-tree">
    <a-tree
      v-model:expandedKeys="expandedKeys"
      v-model:selectedKeys="selectedKeys"
      :tree-data="treeData"
      :show-icon="true"
      @select="handleSelect"
      @expand="handleExpand"
    >
      <template #icon="{ dataRef }">
        <component :is="getFileIcon(dataRef)" />
      </template>
      <template #title="{ dataRef }">
        <span class="tree-node-title">{{ dataRef.title }}</span>
      </template>
    </a-tree>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  FolderOutlined,
  FolderOpenOutlined,
  FileOutlined,
  CodeOutlined
} from '@ant-design/icons-vue'

const props = defineProps({
  files: {
    type: Array,
    default: () => []
  },
  expanded: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['select', 'expand'])

const expandedKeys = ref([])
const selectedKeys = ref([])

const treeData = computed(() => {
  return buildTreeData(props.files)
})

const buildTreeData = (files) => {
  const tree = []
  const map = new Map()

  // 创建节点映射
  files.forEach(file => {
    const node = {
      key: file.id || file.path,
      title: file.name,
      isLeaf: file.type === 'file',
      icon: file.type === 'folder' ? 'folder' : 'file',
      children: [],
      ...file
    }
    map.set(node.key, node)
  })

  // 构建树结构
  files.forEach(file => {
    const node = map.get(file.id || file.path)
    if (file.parentId && map.has(file.parentId)) {
      const parent = map.get(file.parentId)
      parent.children.push(node)
    } else {
      tree.push(node)
    }
  })

  return tree
}

const getFileIcon = (node) => {
  if (node.type === 'folder') {
    return expandedKeys.value.includes(node.key) ? FolderOpenOutlined : FolderOutlined
  }
  return CodeOutlined
}

const handleSelect = (keys, { selected, selectedNodes }) => {
  if (selected && selectedNodes.length > 0) {
    emit('select', selectedNodes[0])
  }
}

const handleExpand = (keys) => {
  expandedKeys.value = keys
  emit('expand', keys)
}

// 监听外部expanded属性变化
watch(() => props.expanded, (newVal) => {
  expandedKeys.value = newVal
}, { immediate: true })
</script>

<style scoped>
.file-tree {
  max-height: 400px;
  overflow-y: auto;
}

.tree-node-title {
  font-size: 14px;
  color: #262626;
}

:deep(.ant-tree-node-content-wrapper) {
  padding: 4px 8px;
  border-radius: 4px;
}

:deep(.ant-tree-node-content-wrapper:hover) {
  background-color: #f5f5f5;
}

:deep(.ant-tree-node-selected .ant-tree-node-content-wrapper) {
  background-color: #e6f7ff;
}

:deep(.ant-tree-switcher) {
  width: 20px;
}

:deep(.ant-tree-iconEle) {
  margin-right: 4px;
}
</style>