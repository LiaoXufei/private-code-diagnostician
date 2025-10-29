<template>
  <div class="score-card">
    <div class="score-display">
      <div class="score-circle" :class="gradeClass">
        <div class="score-number">{{ score }}</div>
        <div class="score-grade">{{ gradeText }}</div>
      </div>
    </div>
    <div class="score-info">
      <h3 class="score-title">总体评分</h3>
      <div class="score-desc">
        <div class="grade-badge" :class="gradeClass">
          {{ gradeText }}
        </div>
        <div class="score-text">{{ score }}分</div>
      </div>
      <div class="progress-container">
        <a-progress
          :percent="score"
          :stroke-color="progressColor"
          :show-info="false"
          size="small"
          :trail-color="'#f0f0f0'"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  score: {
    type: Number,
    required: true,
    default: 0
  },
  grade: {
    type: String,
    default: 'average'
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const gradeInfo = computed(() => {
  const gradeMap = {
    excellent: { text: '优秀', color: '#52c41a', bgClass: 'excellent' },
    good: { text: '良好', color: '#1890ff', bgClass: 'good' },
    average: { text: '及格', color: '#faad14', bgClass: 'average' },
    poor: { text: '不及格', color: '#f5222d', bgClass: 'poor' }
  }
  return gradeMap[props.grade] || gradeMap.average
})

const gradeText = computed(() => gradeInfo.value.text)
const progressColor = computed(() => gradeInfo.value.color)
const gradeClass = computed(() => gradeInfo.value.bgClass)
</script>

<style scoped>
.score-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.score-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.score-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.3s ease;
}

.score-circle.excellent {
  background: linear-gradient(135deg, #52c41a, #73d13d);
  box-shadow: 0 0 20px rgba(82, 196, 26, 0.3);
}

.score-circle.good {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  box-shadow: 0 0 20px rgba(24, 144, 255, 0.3);
}

.score-circle.average {
  background: linear-gradient(135deg, #faad14, #ffc53d);
  box-shadow: 0 0 20px rgba(250, 173, 20, 0.3);
}

.score-circle.poor {
  background: linear-gradient(135deg, #f5222d, #ff7875);
  box-shadow: 0 0 20px rgba(245, 34, 45, 0.3);
}

.score-number {
  font-size: 36px;
  font-weight: bold;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  margin-bottom: 4px;
}

.score-grade {
  font-size: 14px;
  color: #fff;
  font-weight: 500;
  opacity: 0.9;
}

.score-info {
  width: 100%;
}

.score-title {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 16px 0;
}

.score-desc {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.grade-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.grade-badge.excellent {
  background: #52c41a;
}

.grade-badge.good {
  background: #1890ff;
}

.grade-badge.average {
  background: #faad14;
}

.grade-badge.poor {
  background: #f5222d;
}

.score-text {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.progress-container {
  width: 100%;
  max-width: 200px;
  margin: 0 auto;
}

/* 加载状态 */
.score-card.loading {
  opacity: 0.6;
  pointer-events: none;
}

.score-card.loading .score-number::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: none;
  border-left: none;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: translate(-50%, -50%) rotate(0deg);
  }
  100% {
    transform: translate(-50%, -50%) rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .score-card {
    padding: 16px;
  }

  .score-circle {
    width: 100px;
    height: 100px;
  }

  .score-number {
    font-size: 28px;
  }

  .score-grade {
    font-size: 12px;
  }

  .score-title {
    font-size: 16px;
  }

  .grade-badge {
    padding: 4px 8px;
    font-size: 12px;
  }

  .score-text {
    font-size: 14px;
  }
}
</style>