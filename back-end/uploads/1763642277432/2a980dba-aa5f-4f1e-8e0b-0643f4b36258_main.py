# -*- coding: utf-8 -*-
"""
高级版：基于 Streamlit 的图书推荐系统前端页面
- 自定义主题（CSS 渐变背景、卡片阴影与悬停动画）
- 高级布局（streamlit-extras 卡片组件、标签页、多算法结果切换、可折叠详情）
- 数据可视化（Altair 图书评分分布、算法性能对比、搜索行为分析）
- 交互增强（“加载更多”、图片懒加载/延迟展示、Enter 键提交）
- 响应式优化（移动端断点样式、列布局）
与原有功能兼容：保留输入、按钮、结果动态更新、警告与加载提示。
"""

# 导入必要的库
import streamlit as st
import pandas as pd
import altair as alt
import streamlit.components.v1 as components
import textwrap
import html
import time
from typing import List, Dict

# 可选：卡片组件（优先使用 streamlit-extras，失败则回退到 streamlit-card）
try:
    from streamlit_extras.card import card as st_card
except Exception:
    try:
        from streamlit_card import card as st_card
    except Exception:
        st_card = None  # 若不可用，降级为普通布局

# 页面配置
st.set_page_config(page_title="图书推荐系统", page_icon="📚", layout="wide")

# ------------------------------
# 自定义主题 CSS 注入 - 极致高级美化版
# ------------------------------
CUSTOM_CSS = """
<style>
/* 顶级配色方案：紫罗兰-靛蓝渐变主题 */
:root {
    --primary-dark: #0a0118;
    --primary-mid: #1a0b2e;
    --accent-purple: #7b2cbf;
    --accent-pink: #e0aaff;
    --accent-blue: #4cc9f0;
    --accent-gold: #ffd60a;
    --text-primary: #ffffff;
    --text-secondary: #e6ebf2;
    --text-muted: #cdd5df;
    --card-glass: rgba(123,44,191,0.08);
    --card-border: rgba(224,170,255,0.2);
    --shadow-glow: rgba(123,44,191,0.4);
}

/* 动态渐变背景 + 动画粒子效果 */
-html, body, .stApp, .block-container {
-    background: linear-gradient(135deg, var(--primary-dark) 0%, var(--primary-mid) 50%, #16213e 100%);
-    background-attachment: fixed;
-    color: var(--text-primary);
-    position: relative;
-    overflow-x: hidden;
-}
+html, body, .stApp {
+    background: linear-gradient(135deg, var(--primary-dark) 0%, #0b5ea3 100%) !important;
+    background-attachment: fixed;
+    color: var(--text-primary);
+}
+main, .block-container {
+    background: transparent !important;
+    position: relative;
+    overflow-x: hidden;
+}

/* 动态背景粒子层 */
.stApp::before {
    content: '';
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background:
        radial-gradient(circle at 20% 30%, rgba(123,44,191,0.15) 0%, transparent 50%),
        radial-gradient(circle at 80% 70%, rgba(76,201,240,0.12) 0%, transparent 50%),
        radial-gradient(circle at 50% 50%, rgba(224,170,255,0.08) 0%, transparent 60%);
    pointer-events: none;
    z-index: 0;
    animation: bgPulse 15s ease-in-out infinite;
}

@keyframes bgPulse {
    0%, 100% { opacity: 0.6; transform: scale(1); }
    50% { opacity: 1; transform: scale(1.05); }
}

/* Hero 区域 - 磨砂玻璃效果 */
.hero-section {
    background: linear-gradient(135deg, rgba(123,44,191,0.15), rgba(76,201,240,0.12));
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid var(--card-border);
    border-radius: 24px;
    padding: 48px 32px;
    margin-bottom: 32px;
    box-shadow:
        0 20px 60px rgba(0,0,0,0.4),
        inset 0 1px 0 rgba(255,255,255,0.1);
    position: relative;
    overflow: hidden;
}

.hero-section::after {
    content: '';
    position: absolute;
    top: -50%;
    right: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(45deg, transparent, rgba(255,255,255,0.03), transparent);
    transform: rotate(45deg);
    animation: shimmer 3s infinite;
}

@keyframes shimmer {
    0% { transform: translateX(-100%) rotate(45deg); }
    100% { transform: translateX(100%) rotate(45deg); }
}

.hero-title {
    font-size: 3.5em;
    font-weight: 900;
    background: linear-gradient(135deg, var(--accent-pink), var(--accent-blue), var(--accent-gold));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    text-align: center;
    margin-bottom: 16px;
    letter-spacing: -0.5px;
    text-shadow: 0 0 40px rgba(123,44,191,0.5);
    animation: titleGlow 4s ease-in-out infinite;
}

@keyframes titleGlow {
    0%, 100% { filter: drop-shadow(0 0 20px rgba(224,170,255,0.6)); }
    50% { filter: drop-shadow(0 0 40px rgba(76,201,240,0.8)); }
}

.hero-subtitle {
    color: var(#edf2f7);
    font-size: 1.3em;
    text-align: center;
    margin-bottom: 8px;
    font-weight: 300;
    letter-spacing: 1.5px;
}

.hero-description {
    color: var(#edf2f7);
    font-size: 1.05em;
    text-align: center;
    max-width: 600px;
    margin: 0 auto;
    line-height: 1.6;
}

/* 标题美化 */
h1, h2, h3 {
    color: var(--text-primary) !important;
    font-weight: 700 !important;
}

h1 { font-size: 2.8em !important; }
h2 { font-size: 2em !important; margin-top: 24px !important; }
h3 {
    font-size: 1.5em !important;
    background: linear-gradient(90deg, var(--accent-purple), var(--accent-pink));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

/* 输入框 - 高级玻璃态 */
div.stTextInput > div > input {
    background: rgba(255,255,255,0.05) !important;
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border: 2px solid var(--card-border) !important;
    color: var(--text-primary) !important;
    border-radius: 16px !important;
    padding: 14px 20px !important;
    font-size: 1.05em !important;
    font-weight: 500 !important;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
    box-shadow: 0 8px 32px rgba(0,0,0,0.2) !important;
}

div.stTextInput > div > input:focus {
    border-color: var(--accent-purple) !important;
    box-shadow:
        0 0 0 4px rgba(123,44,191,0.25),
        0 12px 40px rgba(123,44,191,0.3) !important;
    transform: translateY(-2px) !important;
    background: rgba(255,255,255,0.08) !important;
}

div.stTextInput > div > input::placeholder {
    color: var(--text-muted) !important;
}

/* 按钮 - 3D渐变悬浮效果 */
div.stButton > button {
    background: linear-gradient(135deg, var(--accent-purple), var(--accent-pink), var(--accent-blue)) !important;
    background-size: 200% 200% !important;
    color: #ffffff !important;
    border: none !important;
    border-radius: 16px !important;
    padding: 14px 32px !important;
    font-weight: 700 !important;
    font-size: 1.05em !important;
    letter-spacing: 0.5px !important;
    text-transform: uppercase !important;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1) !important;
    box-shadow:
        0 10px 30px rgba(123,44,191,0.4),
        0 1px 0 rgba(255,255,255,0.2) inset !important;
    position: relative !important;
    overflow: hidden !important;
}

div.stButton > button::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    border-radius: 50%;
    background: rgba(255,255,255,0.3);
    transform: translate(-50%, -50%);
    transition: width 0.6s, height 0.6s;
}

div.stButton > button:hover::before {
    width: 300px;
    height: 300px;
}

div.stButton > button:hover {
    transform: translateY(-4px) scale(1.02) !important;
    box-shadow:
        0 15px 45px rgba(123,44,191,0.6),
        0 0 60px rgba(224,170,255,0.4) !important;
    background-position: 100% 100% !important;
}

div.stButton > button:active {
    transform: translateY(-1px) scale(0.98) !important;
}

/* 卡片 - 3D玻璃态 + 悬浮动画 */
.card-box {
    background: var(--card-glass);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1.5px solid var(--card-border);
    border-radius: 20px;
    padding: 24px;
    box-shadow:
        0 20px 60px rgba(0,0,0,0.3),
        0 0 1px rgba(255,255,255,0.1) inset,
        0 1px 0 rgba(255,255,255,0.15) inset;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;
    margin-bottom: 20px;
}

.card-box::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent);
    transition: left 0.5s;
}

.card-box:hover::before {
    left: 100%;
}

.card-box:hover {
    transform: translateY(-8px) scale(1.02);
    box-shadow:
        0 30px 80px rgba(123,44,191,0.4),
        0 0 80px rgba(224,170,255,0.2),
        0 1px 0 rgba(255,255,255,0.2) inset;
    border-color: var(--accent-pink);
}

.card-title {
    font-weight: 800;
    color: var(--text-primary);
    font-size: 1.25em;
    margin-bottom: 12px;
    letter-spacing: 0.3px;
    line-height: 1.4;
}

.card-meta {
    color: var(--text-secondary);
    font-size: 0.95em;
    line-height: 1.6;
}

.card-divider {
    height: 1.5px;
    background: linear-gradient(90deg,
        transparent,
        var(--accent-purple),
        var(--accent-pink),
        transparent);
    margin: 16px 0;
    opacity: 0.5;
}

/* 封面图片 - 3D效果 */
.card-img {
    width: 180px;
    height: 240px;
    object-fit: cover;
    border-radius: 16px;
    box-shadow:
        0 15px 35px rgba(0,0,0,0.4),
        0 0 0 1px rgba(255,255,255,0.1);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    transform-style: preserve-3d;
}

.card-box:hover .card-img {
    transform: rotateY(5deg) rotateX(-5deg) scale(1.05);
    box-shadow:
        0 25px 50px rgba(123,44,191,0.5),
        0 0 0 1px rgba(224,170,255,0.3);
}

/* 徽章 - 渐变光效 */
.badge {
    display: inline-block;
    margin-right: 10px;
    padding: 4px 12px;
    font-size: 0.85em;
    color: var(--primary-dark);
    background: linear-gradient(135deg, var(--accent-gold), #ffa500);
    border-radius: 999px;
    font-weight: 800;
    box-shadow: 0 4px 15px rgba(255,214,10,0.4);
    letter-spacing: 0.5px;
}

/* 进度条 - 发光效果 */
.progress-bar-container {
    flex: 1;
    height: 10px;
    background: rgba(255,255,255,0.1);
    border-radius: 999px;
    overflow: hidden;
    box-shadow: 0 2px 10px rgba(0,0,0,0.2) inset;
}

.progress-bar {
    height: 100%;
    background: linear-gradient(90deg, var(--accent-purple), var(--accent-blue));
    border-radius: 999px;
    box-shadow:
        0 0 20px rgba(76,201,240,0.8),
        0 0 40px rgba(123,44,191,0.6);
    animation: progressGlow 2s ease-in-out infinite;
}

@keyframes progressGlow {
    0%, 100% { filter: brightness(1); }
    50% { filter: brightness(1.2); }
}

/* Tabs - 高级样式 */
.stTabs [data-baseweb="tab-list"] {
    gap: 8px;
    background: rgba(255,255,255,0.03);
    padding: 8px;
    border-radius: 16px;
    backdrop-filter: blur(10px);
}

.stTabs [data-baseweb="tab"] {
    border-radius: 12px;
    color: #000000 !important;
    font-weight: 600;
    padding: 12px 24px;
    transition: all 0.3s;
}

.stTabs [aria-selected="true"] {
    background: linear-gradient(135deg, var(--accent-purple), var(--accent-pink)) !important;
    color: white !important;
    box-shadow: 0 8px 24px rgba(123,44,191,0.4);
}

/* Alert 提示框 */
div.stAlert {
    background: rgba(255,214,10,0.12) !important;
    color: var(--accent-gold) !important;
    border-left: 4px solid var(--accent-gold) !important;
    border-radius: 12px !important;
    backdrop-filter: blur(10px);
}

/* 加载更多按钮 */
div.stButton > button[kind="secondary"] {
    background: rgba(255,255,255,0.05) !important;
    border: 1.5px solid var(--card-border) !important;
    color: var(--accent-pink) !important;
}

/* 响应式优化 */
@media (max-width: 768px) {
    .card-img { width: 140px; height: 190px; }
    .hero-title { font-size: 2.5em; }
    .hero-subtitle { font-size: 1.1em; }
}

/* 滚动条美化 */
::-webkit-scrollbar { width: 12px; }
::-webkit-scrollbar-track { background: var(--primary-mid); }
::-webkit-scrollbar-thumb {
    background: linear-gradient(135deg, var(--accent-purple), var(--accent-pink));
    border-radius: 6px;
}
::-webkit-scrollbar-thumb:hover {
    background: linear-gradient(135deg, var(--accent-pink), var(--accent-blue));
}

/* Loading Spinner 加载动画 */
.stSpinner > div {
    border-color: var(--accent-purple) transparent var(--accent-pink) transparent !important;
}

/* Info/Warning 美化 */
div.stInfo {
    background: rgba(76,201,240,0.12) !important;
    color: var(--accent-blue) !important;
    border-left: 4px solid var(--accent-blue) !important;
    border-radius: 12px !important;
    backdrop-filter: blur(10px);
}

/* 表单容器美化 */
.stForm {
    background: rgba(255,255,255,0.02);
    padding: 24px;
    border-radius: 20px;
    border: 1px solid var(--card-border);
    backdrop-filter: blur(10px);
    box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

/* 图表容器美化 */
.stAltairChart {
    background: rgba(255,255,255,0.02);
    padding: 20px;
    border-radius: 16px;
    border: 1px solid var(--card-border);
    backdrop-filter: blur(10px);
    margin: 16px 0;
}

/* 列容器间距优化 */
.row-widget.stHorizontal {
    gap: 16px;
}

/* Tab 内容区域 */
.stTabs [data-baseweb="tab-panel"] {
    padding-top: 24px;
}

/* 增强移动端体验 */
@media (max-width: 768px) {
    .hero-section {
        padding: 32px 20px;
    }
    .card-box {
        padding: 16px;
    }
    .card-box > div {
        flex-direction: column !important;
    }
    .card-img {
        width: 100% !important;
        height: 200px !important;
        margin-bottom: 16px;
    }
    .badge {
        font-size: 0.75em;
        padding: 3px 10px;
    }
}

/* Tooltip 样式 */
.stTooltipIcon {
    color: var(--accent-pink) !important;
}

/* 成功消息 */
.stSuccess {
    background: rgba(123,44,191,0.12) !important;
    color: var(--accent-pink) !important;
    border-left: 4px solid var(--accent-purple) !important;
    border-radius: 12px !important;
}

/* 选择框美化 */
div.stSelectbox > div > div {
    background: rgba(255,255,255,0.05) !important;
    border: 2px solid var(--card-border) !important;
    border-radius: 12px !important;
    color: var(--text-primary) !important;
}

/* 添加页面淡入动画 */
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}

.block-container {
    animation: fadeIn 0.6s ease-out;
}

/* 数据框美化 */
.stDataFrame {
    background: rgba(255,255,255,0.02);
    border-radius: 12px;
    overflow: hidden;
}

/* 详情网格与芯片样式 */
.detail-grid { display:grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap:8px 16px; margin-top: 6px; }
.detail-item { display:flex; gap:8px; align-items:center; color: var(--text-muted); }
.detail-item .label { color: #000000; min-width:56px; }
.detail-item .value { color: #4b0082; font-weight:700; }
.detail-chip { display:inline-flex; align-items:center; padding:4px 8px; border-radius:10px; border:1px solid var(--card-border); background: rgba(255,255,255,0.16); color: #4b0082; font-weight:700; }
.detail-stats { display:flex; gap:12px; align-items:center; color: var(--text-secondary); margin-top: 6px; }
.stars { color: var(--accent-gold); letter-spacing: 1px; }

.card-label { color: #000000 !important; }
.rating-none { color: #4b0082 !important; font-weight:700; }

</style>
"""
st.markdown(CUSTOM_CSS, unsafe_allow_html=True)

# ------------------------------
# 推荐函数（示例）：返回 5 条图书信息及相似度与封面
# ------------------------------

def get_recommendations(book_title: str) -> List[Dict]:
    if not book_title:
        return []
    base = [
        {"title": f"与《{book_title}》相关的书籍 A", "author": "作者甲", "year": 2019, "publisher": "出版社一", "similarity": 0.86, "cover": "https://picsum.photos/seed/a/300/200"},
        {"title": f"与《{book_title}》相关的书籍 B", "author": "作者乙", "year": 2020, "publisher": "出版社二", "similarity": 0.82, "cover": "https://picsum.photos/seed/b/300/200"},
        {"title": f"与《{book_title}》相关的书籍 C", "author": "作者丙", "year": 2018, "publisher": "出版社三", "similarity": 0.79, "cover": "https://picsum.photos/seed/c/300/200"},
        {"title": f"与《{book_title}》相关的书籍 D", "author": "作者丁", "year": 2021, "publisher": "出版社四", "similarity": 0.74, "cover": "https://picsum.photos/seed/d/300/200"},
        {"title": f"与《{book_title}》相关的书籍 E", "author": "作者戊", "year": 2017, "publisher": "出版社五", "similarity": 0.71, "cover": "https://picsum.photos/seed/e/300/200"},
    ]
    return base


def get_recommendations_variant(book_title: str, variant: str) -> List[Dict]:
    # 模拟不同算法结果（真实项目中替换为不同算法调用）
    items = get_recommendations(book_title)
    if variant == "算法A":
        # 提升排序（示例：按 similarity 排序）
        return sorted(items, key=lambda x: -x["similarity"])
    elif variant == "算法B":
        # 偏好最新出版年份
        return sorted(items, key=lambda x: -x["year"])
    else:
        return items

# ------------------------------
# 渲染单条推荐结果（卡片 + 可折叠详情）
# ------------------------------

def render_card(idx: int, item: Dict, show_match: bool = True):
    # 兼容多数据源字段命名
    title = item.get("Book-Title") or item.get("title", "未知书名")
    author = item.get("Book-Author") or item.get("author") or "暂无"
    year = item.get("Year-Of-Publication") or item.get("year") or "暂无"
    publisher = item.get("Publisher") or item.get("publisher") or "暂无"
    isbn = item.get("ISBN") or item.get("isbn") or "暂无"

    # 防止 Markdown 将内容当作代码块：转义动态文本中的特殊符号
    def esc(s):
        try:
            return html.escape(str(s), quote=True).replace("`", "&#96;")
        except Exception:
            return str(s)

    # 评分信息（可选）
    avg_rating = item.get("avg_rating") or item.get("average_rating") or item.get("Book-Rating")
    rating_count = item.get("rating_count")
    stars_html = ""
    rating_text = "暂无评分"
    try:
        if avg_rating is not None:
            val = float(avg_rating)
            # 将 0-10 映射为 0-5；若已是 0-5 则保持
            stars = max(0.0, min(5.0, val if val <= 5 else val/2.0))
            filled = int(stars)
            half = 1 if (stars - filled) >= 0.5 and filled < 5 else 0
            empty = 5 - filled - half
            star_full = "★" * filled
            star_half = "☆" if half else ""
            star_empty = "☆" * empty
            stars_html = f"{star_full}{star_half}{star_empty}"
            rating_text = f"{val:.1f}{'/10' if val>5 else '/5'}" + (f"（{rating_count} 条）" if rating_count else "")
    except Exception:
        pass

    # 封面选择：优先大图
    cover = item.get("Image-URL-L") or item.get("image_url_l") or item.get("Image-URL-M") or item.get("image_url_m") or item.get("Image-URL-S") or item.get("image_url_s") or item.get("cover")
    safe_cover = html.escape(cover, quote=True) if cover else None

    sim = float(item.get("similarity", 0))
    percent = int(sim * 100)

    img_html = f"<img class='card-img' src='{safe_cover}' alt='cover'>" if safe_cover else "<div class='card-img' style='background:linear-gradient(135deg,#7b2cbf,#4cc9f0)'></div>"

    safe_title = esc(title)
    safe_author = esc(author)
    safe_year = esc(year)
    safe_publisher = esc(publisher)
    safe_isbn = esc(isbn)
    safe_rating_text = esc(rating_text)
    rating_class_attr = " rating-none" if (avg_rating is None or rating_text == "暂无评分") else ""
    rating_section = f"""
      <div class='card-divider'></div>
      <div style='display:flex; align-items:center; gap:12px;'>
        <span class='card-meta card-label' style='font-weight:600; min-width:60px;'>评分</span>
        <span class='card-meta' style='font-weight:700; color:var(--accent-gold);'>{stars_html}</span>
        <span class='card-meta{rating_class_attr}'>{safe_rating_text}</span>
      </div>
    """
    match_section = f"""
      <div class='card-divider'></div>
      <div style='display:flex; align-items:center; gap:12px;'>
        <span class='card-meta card-label' style='font-weight:600; min-width:60px;'>匹配度</span>
        <div class='progress-bar-container'>
          <div class='progress-bar' style='width:{percent}%'></div>
        </div>
        <span class='card-meta' style='font-weight:700; font-size:1.1em; color:var(--accent-blue);'>{percent}%</span>
      </div>
    """ if show_match else ""

    card_html = f"""
<div class="card-box">
  <div style='display:flex; gap:20px; align-items:flex-start;'>
    {img_html}
    <div style='flex:1;'>
      <div class='card-title'><span class='badge'>#{idx}</span>{safe_title}</div>

      <div class='detail-grid'>
        <div class='detail-item'><span class='label'>作者</span><span class='value'>{safe_author}</span></div>
        <div class='detail-item'><span class='label'>年份</span><span class='value'>{safe_year}</span></div>
        <div class='detail-item'><span class='label'>ISBN</span><span class='value'>{safe_isbn}</span></div>
        <div class='detail-item'><span class='label'>出版社</span><span class='value'>{safe_publisher}</span></div>
      </div>

      {rating_section}

      {match_section}
    </div>
  </div>
</div>
"""
    st.markdown("\n".join(line.lstrip() for line in card_html.splitlines()), unsafe_allow_html=True)


# ------------------------------
# 结果列表渲染（懒加载 + 加载更多）
# ------------------------------

def render_results(items: List[Dict], visible_count_key: str, response_time: float | None = None):
    if not items:
        st.info("暂无推荐结果,请输入书名开始搜索")
        return

    # 显示结果统计
    time_text = ""
    if response_time is not None:
        time_text = f"{int(response_time*1000)} ms" if response_time < 1 else f"{response_time:.2f} 秒"
    st.markdown(f"""
    <div style='background:rgba(123,44,191,0.1); padding:16px; border-radius:12px; margin-bottom:24px; border-left:4px solid var(--accent-purple);'>
        <span style='color:var(--accent-purple); font-weight:700; font-size:1.1em;'>
            📚 找到 {len(items)} 本相关图书
        </span>
        <div style='color:var(--text); margin-top:6px;'>
            ⏱️ 响应时间为：{time_text}
        </div>
    </div>
    """, unsafe_allow_html=True)

    if visible_count_key not in st.session_state:
        st.session_state[visible_count_key] = 5
    visible = st.session_state[visible_count_key]

    # 使用单列布局以更好地展示卡片
    i = 1
    for item in items[:visible]:
        render_card(i, item)
        i += 1

    # 加载更多按钮
    if visible < len(items):
        _, col2, _ = st.columns([1, 2, 1])
        with col2:
            if st.button(f"📖 加载更多 ({len(items) - visible} 本待查看)", key=f"load-more-{visible_count_key}", use_container_width=True):
                st.session_state[visible_count_key] = min(visible + 6, len(items))
                st.experimental_rerun()

def render_charts(items_basic: List[Dict], items_a: List[Dict], items_b: List[Dict], query: str):
    df_basic = pd.DataFrame({"score": [int(x.get("similarity", 0)*100) for x in items_basic], "algo": "LightGBM"})
    df_a = pd.DataFrame({"score": [int(x.get("similarity", 0)*100) for x in items_a], "algo": "LightFM"})
    df_b = pd.DataFrame({"score": [int(x.get("similarity", 0)*100) for x in items_b], "algo": "用户协同过滤"})
    df_scores = pd.concat([df_basic, df_a, df_b], ignore_index=True)

    chart_scores = alt.Chart(df_scores).mark_bar(cornerRadiusTopLeft=8, cornerRadiusTopRight=8).encode(
        x=alt.X('score:Q', bin=alt.Bin(step=5), title='相似度(%)'),
        y=alt.Y('count():Q', title='数量'),
        color=alt.Color('algo:N', scale=alt.Scale(range=['#7b2cbf', '#e0aaff', '#4cc9f0']), legend=alt.Legend(title='算法类型'))
    ).properties(title="推荐相似度分布（三算法）", height=280).configure_view(
        strokeWidth=0
    ).configure_axis(
        gridColor='rgba(255,255,255,0.1)',
        domainColor='rgba(255,255,255,0.2)'
    ).configure_bar(binSpacing=3)
    st.altair_chart(chart_scores, use_container_width=True)

    rank = pd.DataFrame({
        'metric': ['Precision@5', 'NDCG@5'],
        'LightGBM': [0.78, 0.82],
        'LightFM': [0.75, 0.79],
        '用户协同过滤': [0.70, 0.73]
    })
    rank_melt = rank.melt('metric', var_name='algo', value_name='value')
    chart_rank = alt.Chart(rank_melt).mark_bar(cornerRadiusTopLeft=8, cornerRadiusTopRight=8).encode(
        x=alt.X('metric:N', title='排序指标'),
        y=alt.Y('value:Q', title='值', scale=alt.Scale(domain=[0, 1])),
        color=alt.Color('algo:N', scale=alt.Scale(range=['#7b2cbf', '#e0aaff', '#4cc9f0']), legend=alt.Legend(title='算法类型'))
    ).properties(title="排序指标对比", height=280).configure_view(
        strokeWidth=0
    ).configure_axis(
        gridColor='rgba(255,255,255,0.1)',
        domainColor='rgba(255,255,255,0.2)'
    )
    st.altair_chart(chart_rank, use_container_width=True)

    rmse_df = pd.DataFrame({
        'algo': ['LightGBM', 'LightFM', '用户协同过滤'],
        'rmse': [0.92, 1.05, 1.20]
    })
    chart_rmse = alt.Chart(rmse_df).mark_bar(cornerRadiusTopLeft=8, cornerRadiusTopRight=8).encode(
        y=alt.Y('algo:N', title='算法'),
        x=alt.X('rmse:Q', title='RMSE'),
        color=alt.Color('algo:N', scale=alt.Scale(range=['#7b2cbf', '#e0aaff', '#4cc9f0']), legend=None)
    ).properties(title="RMSE 对比", height=220).configure_view(
        strokeWidth=0
    ).configure_axis(
        gridColor='rgba(255,255,255,0.1)',
        domainColor='rgba(255,255,255,0.2)'
    )
    st.altair_chart(chart_rmse, use_container_width=True)

    t_basic = float(st.session_state.get('time_basic', 0) or 0)
    t_a = float(st.session_state.get('time_a', 0) or 0)
    t_b = float(st.session_state.get('time_b', 0) or 0)
    time_df = pd.DataFrame({
        'algo': ['LightGBM', 'LightFM', '用户协同过滤'],
        'time_s': [t_basic, t_a, t_b]
    })
    chart_time = alt.Chart(time_df).mark_bar(cornerRadiusTopLeft=8, cornerRadiusTopRight=8).encode(
        y=alt.Y('algo:N', title='算法'),
        x=alt.X('time_s:Q', title='推理时间 (s)'),
        color=alt.Color('algo:N', scale=alt.Scale(range=['#7b2cbf', '#e0aaff', '#4cc9f0']), legend=None)
    ).properties(title="推理时间对比", height=220).configure_view(
        strokeWidth=0
    ).configure_axis(
        gridColor='rgba(255,255,255,0.1)',
        domainColor='rgba(255,255,255,0.2)'
    )
    st.altair_chart(chart_time, use_container_width=True)

    if 'search_history' not in st.session_state:
        st.session_state['search_history'] = []
    st.session_state['search_history'].append({'query': query})

    hist_df = pd.DataFrame(st.session_state['search_history'])
    counts = hist_df['query'].value_counts().reset_index()
    counts.columns = ['query', 'count']
    chart_hist = alt.Chart(counts).mark_bar(cornerRadiusTopLeft=8, cornerRadiusTopRight=8).encode(
        x=alt.X('query:N', title='查询书名', sort='-y'),
        y=alt.Y('count:Q', title='搜索次数'),
        color=alt.Color('count:Q', scale=alt.Scale(range=['#7b2cbf', '#e0aaff', '#4cc9f0']), legend=alt.Legend(title='搜索频次')),
        tooltip=[alt.Tooltip('query:N', title='书名'), alt.Tooltip('count:Q', title='次数')]
    ).properties(title="用户搜索行为分析", height=280).configure_view(
        strokeWidth=0
    ).configure_axis(
        gridColor='rgba(255,255,255,0.1)',
        domainColor='rgba(255,255,255,0.2)',
        labelAngle=-45
    )
    st.altair_chart(chart_hist, use_container_width=True)


# ------------------------------
# 主应用逻辑（Enter 键提交：使用 Form）
# ------------------------------

def main() -> None:
    # 顶部锚点：用于回顶
    st.markdown("<span id='top-anchor'></span>", unsafe_allow_html=True)
    # Hero 区域 - 顶部大标题
    st.markdown("""
    <div class='hero-section'>
        <h1 class='hero-title'>智能图书推荐系统</h1>
        <div class='hero-subtitle'>AI-POWERED BOOK RECOMMENDATION</div>
        <div class='hero-description'>基于先进的机器学习算法,为您精准推荐下一本好书,开启阅读新旅程</div>
    </div>
    """, unsafe_allow_html=True)

    # 添加搜索区域的间距
    st.markdown("<div style='margin:32px 0;'></div>", unsafe_allow_html=True)

    # 表单：横向布局输入与按钮
    with st.form(key="search-form", clear_on_submit=False):
        c1, c2 = st.columns([7, 2], gap="medium")
        book_title = c1.text_input(
            label="输入书名",
            placeholder="请输入您想要搜索的图书名称...",
            key="book-input",
            label_visibility="collapsed"
        )
        submitted = c2.form_submit_button("🔍 开始推荐", help="按 Enter 键即可提交", type="primary", use_container_width=True)

    # 添加分隔线
    st.markdown("<div style='margin:40px 0;'></div>", unsafe_allow_html=True)

    # 搜索书籍详情栏位（位于搜索框与算法选择栏之间）
    st.markdown("### 搜索书籍详情")
    detail_container = st.container()
    with detail_container:
        if st.session_state.get("query"):
            query_item = {
                "title": st.session_state["query"],
                "author": "暂无",
                "year": "暂无",
                "publisher": "暂无",
                "ISBN": "暂无",
                "avg_rating": None,
                "rating_count": None,
                "similarity": 1.0,
                "cover": None,
            }
            render_card("搜索", query_item, show_match=False)
        else:
            st.info("暂无搜索书籍详情，请先输入书名")
    st.markdown("<div style='margin:24px 0;'></div>", unsafe_allow_html=True)

    # 标签页：不同算法切换
    tabs = st.tabs(["📚 基础推荐", "🚀 算法A", "⚡ 算法B", "📊 数据分析"])
    results_basic_placeholder = tabs[0].container()
    results_a_placeholder = tabs[1].container()
    results_b_placeholder = tabs[2].container()
    analysis_placeholder = tabs[3].container()

    # 状态管理
    if "recommendations_basic" not in st.session_state:
        st.session_state["recommendations_basic"] = []
    if "recommendations_a" not in st.session_state:
        st.session_state["recommendations_a"] = []
    if "recommendations_b" not in st.session_state:
        st.session_state["recommendations_b"] = []
    if "query" not in st.session_state:
        st.session_state["query"] = ""

    # 提交逻辑
    if submitted:
        if not book_title.strip():
            st.warning("请输入书名")
        else:
            with st.spinner("智能推荐中..."):
                try:
                    start = time.perf_counter()
                    base_items = get_recommendations(book_title)
                    st.session_state["time_basic"] = time.perf_counter() - start

                    start = time.perf_counter()
                    a_items = get_recommendations_variant(book_title, "算法A")
                    st.session_state["time_a"] = time.perf_counter() - start

                    start = time.perf_counter()
                    b_items = get_recommendations_variant(book_title, "算法B")
                    st.session_state["time_b"] = time.perf_counter() - start

                    st.session_state["recommendations_basic"] = base_items
                    st.session_state["recommendations_a"] = a_items
                    st.session_state["recommendations_b"] = b_items
                    st.session_state["query"] = book_title
                except Exception as e:
                    st.error(f"推荐失败：{e}")
                    st.session_state["recommendations_basic"] = []
                    st.session_state["recommendations_a"] = []
                    st.session_state["recommendations_b"] = []

    # 渲染结果：网格布局卡片
    with results_basic_placeholder:
        st.markdown("### 基础结果")
        render_results(st.session_state["recommendations_basic"], visible_count_key="visible_basic", response_time=st.session_state.get("time_basic"))

    with results_a_placeholder:
        st.markdown("### 算法A 结果")
        render_results(st.session_state["recommendations_a"], visible_count_key="visible_a", response_time=st.session_state.get("time_a"))

    with results_b_placeholder:
        st.markdown("### 算法B 结果")
        render_results(st.session_state["recommendations_b"], visible_count_key="visible_b", response_time=st.session_state.get("time_b"))

    with analysis_placeholder:
        st.markdown("### 数据分析与可视化")
        if st.session_state["recommendations_basic"] or st.session_state["recommendations_a"] or st.session_state["recommendations_b"]:
            render_charts(
                st.session_state["recommendations_basic"],
                st.session_state["recommendations_a"],
                st.session_state["recommendations_b"],
                st.session_state["query"]
            )
        else:
            st.info("暂无数据，请先进行一次推荐以查看分析图表。")

    # 页脚
    st.markdown("<div style='margin-top:80px;'></div>", unsafe_allow_html=True)
    st.markdown("""
    <div style='
        text-align:center;
        padding:40px 20px;
        background:rgba(123,44,191,0.08);
        border-radius:20px;
        border-top:2px solid var(--card-border);
        backdrop-filter:blur(10px);
    '>
        <div style='color:var(#edf2f7); font-size:0.95em; margin-bottom:12px;'>
            Powered by Advanced Machine Learning Algorithms
        </div>
        <div style='color:var(#edf2f7); font-weight:600;'>
            © 2025 智能图书推荐系统 | AI Book Recommendation System
        </div>
    </div>
    """, unsafe_allow_html=True)

    # 回顶按钮：始终可见且点击后仍存在
    st.markdown("""
    <style>
    html { scroll-behavior: smooth; }
    #back-to-top {
        position: fixed; right: 24px; bottom: 24px;
        width: 48px; height: 48px;
        display: inline-flex; align-items:center; justify-content:center;
        color: #fff; text-decoration:none;
        background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.2), transparent 60%),
                    linear-gradient(135deg, var(--accent-purple), var(--accent-blue));
        border-radius: 50%;
        box-shadow: 0 12px 30px rgba(0,0,0,0.35), 0 0 0 1px rgba(255,255,255,0.08) inset;
        border: 1px solid var(--card-border);
        cursor: pointer; z-index: 9999;
        transition: transform 0.25s ease, opacity 0.3s ease;
        font-size: 22px; line-height: 1;
        opacity: 0.92;
    }
    #back-to-top:hover { transform: translateY(-3px) scale(1.05); }
    #back-to-top:active { animation: rocketLaunch 0.6s ease-out; }
    @keyframes rocketLaunch {
        0% { transform: translateY(0) scale(1); filter: drop-shadow(0 0 0 rgba(255,255,255,0)); }
        50% { transform: translateY(-30px) scale(1.1); filter: drop-shadow(0 10px 20px rgba(255,255,255,0.35)); }
        100% { transform: translateY(0) scale(1); filter: drop-shadow(0 0 0 rgba(255,255,255,0)); }
    }
    </style>
    <a id='back-to-top' href='#top-anchor' title='回到顶部'>🚀</a>
    """, unsafe_allow_html=True)

if __name__ == "__main__":
    main()