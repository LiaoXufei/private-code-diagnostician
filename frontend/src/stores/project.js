import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { projectApi } from '@/api/project'

export const useProjectStore = defineStore('project', () => {
  // State
  const projects = ref([])
  const currentProject = ref(null)
  const loading = ref(false)
  const error = ref(null)

  // Getters
  const activeProjects = computed(() =>
    projects.value.filter(project => project.status === 'active')
  )

  const projectById = computed(() => (id) =>
    projects.value.find(project => project.id === id)
  )

  const hasProjects = computed(() => projects.value.length > 0)

  const isCurrentProjectSet = computed(() => currentProject.value !== null)

  // Actions
  const fetchProjects = async () => {
    loading.value = true
    error.value = null

    try {
      const response = await projectApi.getProjects()
      projects.value = response.data || []

      if (import.meta.env.DEV) {
        console.log('Projects fetched from API:', projects.value.length)
      }

      return projects.value
    } catch (err) {
      error.value = err.message || '获取项目列表失败'
      message.error(error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  const createProject = async (projectData) => {
    loading.value = true
    error.value = null

    try {
      const response = await projectApi.createProject(projectData)
      const newProject = response.data

      projects.value.push(newProject)
      currentProject.value = newProject

      // 移除重复的成功消息，让组件处理
      return newProject
    } catch (err) {
      error.value = err.response?.data?.error || err.message || '项目创建失败'
      // 移除重复的错误消息，让组件处理
      throw err
    } finally {
      loading.value = false
    }
  }

  const getProject = async (id) => {
    // 先检查本地是否存在
    const existingProject = projectById.value(id)
    if (existingProject) {
      currentProject.value = existingProject
      return existingProject
    }

    loading.value = true
    error.value = null

    try {
      const response = await projectApi.getProject(id)
      const project = response.data

      currentProject.value = project

      // 更新本地缓存
      const index = projects.value.findIndex(p => p.id === id)
      if (index >= 0) {
        projects.value[index] = project
      } else {
        projects.value.push(project)
      }

      return project
    } catch (err) {
      error.value = err.message || '获取项目详情失败'
      message.error(error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateProject = async (id, updateData) => {
    loading.value = true
    error.value = null

    try {
      const response = await projectApi.updateProject(id, updateData)
      const updatedProject = response.data

      // 更新本地状态
      const index = projects.value.findIndex(p => p.id === id)
      if (index >= 0) {
        projects.value[index] = updatedProject
      }

      if (currentProject.value?.id === id) {
        currentProject.value = updatedProject
      }

      message.success('项目更新成功')
      return updatedProject
    } catch (err) {
      error.value = err.message || '项目更新失败'
      message.error(error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  const deleteProject = async (id) => {
    loading.value = true
    error.value = null

    try {
      // 对于示例数据，直接删除，不调用API
      if (id.startsWith('demo_')) {
        // 从本地状态中移除
        projects.value = projects.value.filter(p => p.id !== id)

        if (currentProject.value?.id === id) {
          currentProject.value = null
        }

        message.success('项目删除成功')
        return true
      }

      // 对于真实项目，调用API
      await projectApi.deleteProject(id)

      // 从本地状态中移除
      projects.value = projects.value.filter(p => p.id !== id)

      if (currentProject.value?.id === id) {
        currentProject.value = null
      }

      message.success('项目删除成功')
      return true
    } catch (err) {
      error.value = err.message || '项目删除失败'
      message.error(error.value)
      throw err
    } finally {
      loading.value = false
    }
  }

  const setCurrentProject = (project) => {
    currentProject.value = project
  }

  const clearCurrentProject = () => {
    currentProject.value = null
  }

  const addLocalProject = (projectData) => {
    const newProject = {
      id: 'local_' + Date.now().toString(),
      ...projectData,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
      file_count: 0,
      lastAnalysis: null
    }

    projects.value.push(newProject)
    return newProject
  }

  const clearError = () => {
    error.value = null
  }

  return {
    // State
    projects,
    currentProject,
    loading,
    error,

    // Getters
    activeProjects,
    projectById,
    hasProjects,
    isCurrentProjectSet,

    // Actions
    fetchProjects,
    createProject,
    getProject,
    updateProject,
    deleteProject,
    addLocalProject,
    setCurrentProject,
    clearCurrentProject,
    clearError
  }
})