<template>
  <div class="code-editor">
    <div v-if="!isMonacoLoaded" class="loading-placeholder">
      <a-spin size="large" tip="加载编辑器..." />
    </div>
    <div
      v-else
      ref="editorRef"
      :style="{ height: height + 'px' }"
      class="editor-container"
    ></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'javascript'
  },
  height: {
    type: [Number, String],
    default: 400
  },
  readOnly: {
    type: Boolean,
    default: false
  },
  theme: {
    type: String,
    default: 'vs-dark'
  },
  options: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'ready'])

const editorRef = ref(null)
const isMonacoLoaded = ref(false)
let editor = null
let monaco = null

// 默认配置
const defaultOptions = {
  fontSize: 14,
  fontFamily: 'Consolas, Monaco, "Courier New", monospace',
  lineNumbers: 'on',
  roundedSelection: false,
  cursorStyle: 'line',
  wordWrap: 'on',
  automaticLayout: true,
  minimap: { enabled: false },
  scrollBeyondLastLine: false,
  renderWhitespace: 'selection',
  renderControlCharacters: false,
  renderIndentGuides: true,
  folding: true,
  lineNumbersMinChars: 3,
  showFoldingControls: 'always',
  smoothScrolling: true,
  bracketPairColorization: {
    enabled: true
  }
}

const initMonaco = async () => {
  try {
    // 动态导入Monaco Editor
    monaco = await import('monaco-editor')

    // 配置Monaco
    configureMonaco()

    isMonacoLoaded.value = true

    await nextTick()
    initEditor()
  } catch (error) {
    console.error('加载Monaco Editor失败:', error)
  }
}

const configureMonaco = () => {
  // 配置TypeScript
  monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
    noImplicitAny: false,
    noUnusedLocals: false,
    noUnusedParameters: false
  })

  // 自定义主题
  monaco.editor.defineTheme('custom-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [
      { token: 'comment', foreground: '6A9955' },
      { token: 'keyword', foreground: '569CD6' },
      { token: 'string', foreground: 'CE9178' },
      { token: 'number', foreground: 'B5CEA8' }
    ],
    colors: {
      'editor.background': '#1E1E1E',
      'editor.foreground': '#D4D4D4',
      'editor.lineHighlightBackground': '#2D2D30',
      'editorCursor.foreground': '#AEAFAD',
      'editorWhitespace.foreground': '#404040'
    }
  })
}

const initEditor = () => {
  if (!editorRef.value) return

  const finalOptions = {
    ...defaultOptions,
    ...props.options,
    value: props.modelValue,
    language: getMonacoLanguage(props.language),
    theme: props.theme,
    readOnly: props.readOnly
  }

  editor = monaco.editor.create(editorRef.value, finalOptions)

  // 监听内容变化
  editor.onDidChangeModelContent(() => {
    const value = editor.getValue()
    emit('update:modelValue', value)
    emit('change', value)
  })

  // 监听光标位置变化
  editor.onDidChangeCursorPosition((e) => {
    const position = e.position
    emit('cursor-change', position)
  })

  // 监听选择变化
  editor.onDidChangeCursorSelection((e) => {
    const selection = e.selection
    emit('selection-change', selection)
  })

  emit('ready', editor)
}

const getMonacoLanguage = (language) => {
  const languageMap = {
    javascript: 'javascript',
    typescript: 'typescript',
    java: 'java',
    python: 'python',
    cpp: 'cpp',
    c: 'c',
    cs: 'csharp',
    php: 'php',
    go: 'go',
    rust: 'rust',
    html: 'html',
    css: 'css',
    scss: 'scss',
    less: 'less',
    json: 'json',
    xml: 'xml',
    yaml: 'yaml',
    yml: 'yaml',
    sql: 'sql',
    markdown: 'markdown',
    vue: 'html' // Vue文件使用HTML语法高亮
  }
  return languageMap[language] || 'plaintext'
}

const updateValue = (newValue) => {
  if (editor && newValue !== editor.getValue()) {
    const model = editor.getModel()
    if (model) {
      model.setValue(newValue)
    }
  }
}

const updateLanguage = (newLanguage) => {
  if (editor && monaco) {
    const monacoLang = getMonacoLanguage(newLanguage)
    const model = editor.getModel()
    if (model) {
      monaco.editor.setModelLanguage(model, monacoLang)
    }
  }
}

const updateTheme = (newTheme) => {
  if (editor && monaco) {
    monaco.editor.setTheme(newTheme)
  }
}

const updateReadOnly = (readOnly) => {
  if (editor) {
    editor.updateOptions({ readOnly })
  }
}

const updateOptions = (newOptions) => {
  if (editor) {
    editor.updateOptions(newOptions)
  }
}

const getEditor = () => editor

const dispose = () => {
  if (editor) {
    editor.dispose()
    editor = null
  }
}

const setCursorPosition = (lineNumber, column = 1) => {
  if (editor) {
    const position = {
      lineNumber: lineNumber,
      column: column
    }
    editor.setPosition(position)
    editor.revealPosition(position)
  }
}

const focus = () => {
  if (editor) {
    editor.focus()
  }
}

const getValue = () => {
  return editor ? editor.getValue() : ''
}

const getSelection = () => {
  return editor ? editor.getSelection() : null
}

const setSelectedText = (text) => {
  if (editor && text) {
    const selection = editor.getSelection()
    const model = editor.getModel()
    if (model && selection) {
      model.pushEditOperations([], [{
        range: selection,
        text: text
      }])
    }
  }
}

// 监听props变化
watch(() => props.modelValue, updateValue, { deep: true })
watch(() => props.language, updateLanguage)
watch(() => props.theme, updateTheme)
watch(() => props.readOnly, updateReadOnly)
watch(() => props.options, updateOptions, { deep: true })

onMounted(() => {
  initMonaco()
})

onUnmounted(() => {
  dispose()
})

// 暴露方法给父组件
defineExpose({
  getEditor,
  setCursorPosition,
  focus,
  getValue,
  getSelection,
  setSelectedText,
  dispose
})
</script>

<style scoped>
.code-editor {
  width: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
  background: #1e1e1e;
}

.loading-placeholder {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1e1e1e;
}

.editor-container {
  width: 100%;
  min-height: 200px;
}

/* 自定义滚动条样式 */
.code-editor :deep(.monaco-scrollable-element) {
  scrollbar-width: thin;
}

.code-editor :deep(.monaco-scrollable-element::-webkit-scrollbar) {
  width: 8px;
  height: 8px;
}

.code-editor :deep(.monaco-scrollable-element::-webkit-scrollbar-track) {
  background: #1e1e1e;
}

.code-editor :deep(.monaco-scrollable-element::-webkit-scrollbar-thumb) {
  background: #424242;
  border-radius: 4px;
}

.code-editor :deep(.monaco-scrollable-element::-webkit-scrollbar-thumb:hover) {
  background: #4a4a4a;
}
</style>