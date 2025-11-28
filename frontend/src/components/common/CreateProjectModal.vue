<template>
  <a-modal
    v-model:open="modalVisible"
    title="创建新项目"
    width="600px"
    :confirm-loading="loading"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      layout="vertical"
    >
      <a-form-item label="项目名称" name="name" required>
        <a-input
          v-model:value="formData.name"
          placeholder="请输入项目名称"
          :maxlength="50"
          show-count
        />
      </a-form-item>

      <a-form-item label="项目描述" name="description">
        <a-textarea
          v-model:value="formData.description"
          placeholder="请输入项目描述"
          :rows="3"
          :maxlength="200"
          show-count
        />
      </a-form-item>

      <a-form-item label="编程语言" name="language" required>
        <a-select
          v-model:value="formData.language"
          placeholder="请选择主要编程语言"
          style="width: 100%"
        >
          <a-select-option value="javascript">
            <span class="language-option">
              <span class="language-icon">JS</span>
              JavaScript
            </span>
          </a-select-option>
          <a-select-option value="typescript">
            <span class="language-option">
              <span class="language-icon">TS</span>
              TypeScript
            </span>
          </a-select-option>
          <a-select-option value="java">
            <span class="language-option">
              <span class="language-icon">Java</span>
              Java
            </span>
          </a-select-option>
          <a-select-option value="python">
            <span class="language-option">
              <span class="language-icon">Py</span>
              Python
            </span>
          </a-select-option>
          <a-select-option value="cpp">
            <span class="language-option">
              <span class="language-icon">C++</span>
              C++
            </span>
          </a-select-option>
          <a-select-option value="c">
            <span class="language-option">
              <span class="language-icon">C</span>
              C
            </span>
          </a-select-option>
          <a-select-option value="cs">
            <span class="language-option">
              <span class="language-icon">C#</span>
              C#
            </span>
          </a-select-option>
          <a-select-option value="php">
            <span class="language-option">
              <span class="language-icon">PHP</span>
              PHP
            </span>
          </a-select-option>
          <a-select-option value="go">
            <span class="language-option">
              <span class="language-icon">Go</span>
              Go
            </span>
          </a-select-option>
          <a-select-option value="rust">
            <span class="language-option">
              <span class="language-icon">Rs</span>
              Rust
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-divider>高级配置（可选）</a-divider>

      <a-form-item label="排除规则" name="excludeRules">
        <a-select
          v-model:value="formData.config.excludeRules"
          mode="tags"
          placeholder="添加要排除的文件或文件夹模式"
          style="width: 100%"
          :token-separators="[',']"
        >
          <a-select-option value="node_modules/**">node_modules/**</a-select-option>
          <a-select-option value="dist/**">dist/**</a-select-option>
          <a-select-option value="build/**">build/**</a-select-option>
          <a-select-option value="*.min.js">*.min.js</a-select-option>
          <a-select-option value="*.test.js">*.test.js</a-select-option>
          <a-select-option value="*.spec.js">*.spec.js</a-select-option>
        </a-select>
        <div class="form-help-text">
          支持通配符，如 *.min.js、node_modules/**
        </div>
      </a-form-item>

<!--      <a-row :gutter="16">-->
<!--        <a-col :span="8">-->
<!--          <a-form-item label="复杂度阈值" name="complexityThreshold">-->
<!--            <a-input-number-->
<!--              v-model:value="formData.config.thresholds.complexity"-->
<!--              :min="1"-->
<!--              :max="50"-->
<!--              style="width: 100%"-->
<!--            />-->
<!--          </a-form-item>-->
<!--        </a-col>-->
<!--        <a-col :span="8">-->
<!--          <a-form-item label="可维护性指数" name="maintainabilityThreshold">-->
<!--            <a-input-number-->
<!--              v-model:value="formData.config.thresholds.maintainability"-->
<!--              :min="0"-->
<!--              :max="100"-->
<!--              style="width: 100%"-->
<!--            />-->
<!--          </a-form-item>-->
<!--        </a-col>-->
<!--        <a-col :span="8">-->
<!--          <a-form-item label="注释率" name="commentRateThreshold">-->
<!--            <a-input-number-->
<!--              v-model:value="formData.config.thresholds.c ommentRate"-->
<!--              :min="0"-->
<!--              :max="1"-->
<!--              :step="0.1"-->
<!--              :precision="2"-->
<!--              style="width: 100%"-->
<!--            />-->
<!--          </a-form-item>-->
<!--        </a-col>-->
<!--      </a-row>-->
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useProjectStore } from '@/stores/project'
import { useRouter } from 'vue-router'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'success'])

const router = useRouter()
const projectStore = useProjectStore()
const loading = ref(false)
const formRef = ref()

// 使用computed处理双向绑定
const modalVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const formData = reactive({
  name: '',
  description: '',
  language: undefined,
  config: {
    excludeRules: [],
    thresholds: {
      complexity: 10,
      maintainability: 65,
      commentRate: 0.1
    }
  }
})

const formRules = {
  name: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { min: 2, max: 50, message: '项目名称长度为2-50个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_\-\u4e00-\u9fa5]+$/, message: '项目名称只能包含中文、英文、数字、下划线和短横线', trigger: 'blur' }
  ],
  language: [
    { required: true, message: '请选择编程语言', trigger: 'change' }
  ],
  description: [
    { max: 200, message: '项目描述不能超过200个字符', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    const projectData = {
      name: formData.name,
      description: formData.description || '',
      language: formData.language,
      status: 'active'
    }

    console.log('发送创建项目请求:', projectData)

    try {
      // 调用后端API创建项目
      const newProject = await projectStore.createProject(projectData)

      emit('success', newProject)
      modalVisible.value = false
      resetForm()

      message.success('项目创建成功')
    } catch (apiError) {
      console.error('API创建项目失败:', apiError)

      // 显示详细错误信息
      const errorMessage = apiError.response?.data?.error || apiError.message || '创建项目失败'
      message.error(errorMessage)
      throw apiError
    }
  } catch (error) {
    console.error('创建项目失败:', error)
    if (error.errorFields) {
      message.error('请检查表单填写是否正确')
    } else {
      // 不重复显示错误信息，因为API调用已经显示了
    }
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  modalVisible.value = false
  resetForm()
}

const resetForm = () => {
  Object.assign(formData, {
    name: '',
    description: '',
    language: undefined,
    config: {
      excludeRules: [],
      thresholds: {
        complexity: 10,
        maintainability: 65,
        commentRate: 0.1
      }
    }
  })

  // 清除表单验证
  formRef.value?.clearValidate()
}

</script>

<style scoped>
.language-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.language-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #1890ff;
  color: white;
  border-radius: 4px;
  font-size: 10px;
  font-weight: bold;
}

.form-help-text {
  color: #8c8c8c;
  font-size: 12px;
  margin-top: 4px;
}

:deep(.ant-divider-horizontal.ant-divider-with-text) {
  margin: 20px 0 16px 0;
}

:deep(.ant-divider-horizontal.ant-divider-with-text-left) {
  margin: 20px 0 16px 0;
}

:deep(.ant-form-item-label > label) {
  font-weight: 500;
}

:deep(.ant-input-number) {
  width: 100%;
}
</style>