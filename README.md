# AI Resume Parser & Matcher

一个面向招聘场景的简历解析与岗位匹配项目，包含 PDF 简历上传、文本解析清洗、关键信息提取、岗位关键词分析、匹配度评分、缓存和可部署前端页面。

## 功能

- 上传单个 PDF 简历，支持多页文本型 PDF。
- 使用 `pypdf` 提取 PDF 文本，并进行去噪、换行合并和段落整理。
- 优先调用 OpenAI 模型提取姓名、电话、邮箱、地址、求职意向、期望薪资、工作年限、学历背景、项目经历、技能等字段。
- 未配置 `OPENAI_API_KEY` 时自动使用规则解析兜底，保证基础演示可运行。
- 接收岗位需求文本，提取岗位关键词，计算技能匹配率、经验相关性和综合匹配分。
- 使用 SQLite 文件缓存简历解析结果和岗位匹配结果，避免重复计算。
- 前端为纯静态页面，可直接部署到 GitHub Pages；后端可部署到 Render、Railway 或任意 Docker 平台。

## 技术选型

- 后端：FastAPI、Pydantic、pypdf、OpenAI SDK、Redis/SQLite Cache
- 前端：原生 HTML/CSS/JavaScript、Fetch API、Lucide Icons
- 部署：GitHub Pages 静态前端 + Render Docker 后端

## 项目结构

```text
.
├── backend
│   ├── app
│   │   ├── ai_extractor.py    # AI/规则关键信息提取
│   │   ├── cache.py           # SQLite 缓存
│   │   ├── config.py          # 环境变量配置
│   │   ├── main.py            # FastAPI 路由
│   │   ├── matcher.py         # 岗位关键词和匹配评分
│   │   ├── models.py          # 响应/请求模型
│   │   ├── pdf_parser.py      # PDF 文本提取
│   │   └── text_cleaner.py    # 文本清洗与分段
│   ├── Dockerfile
│   └── requirements.txt
├── frontend
│   ├── app.js
│   ├── index.html
│   └── styles.css
├── render.yaml
└── README.md
```

## 本地运行

### 1. 启动后端

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

如需启用 AI 提取与 AI 评分，在 `backend/.env` 中填写：

```env
OPENAI_API_KEY=你的 Key
OPENAI_MODEL=gpt-4.1-mini
```

后端健康检查：

```text
http://localhost:8000/api/health
```

接口文档：

```text
http://localhost:8000/docs
```

### 2. 打开前端

直接用浏览器打开：

```text
frontend/index.html
```

页面里的 API 地址默认是：

```text
http://localhost:8000
```

## API 说明

### 上传并解析简历

```http
POST /api/resumes/upload
Content-Type: multipart/form-data

file=<PDF 文件>
```

返回字段包含：

- `resume_id`：基于 PDF 内容生成的 SHA-256 ID
- `cleaned_text`：清洗后的简历文本
- `paragraphs`：分段文本
- `key_info`：结构化关键信息
- `cache_hit`：是否命中缓存

### 岗位匹配评分

```http
POST /api/match
Content-Type: application/json

{
  "resume_id": "上传接口返回的 resume_id",
  "job_description": "岗位需求文本"
}
```

返回字段包含：

- `job_keywords`：岗位关键词
- `score`：综合匹配分，满分 100
- `detail.skill_match_rate`：技能匹配率
- `detail.experience_relevance`：经验相关性
- `detail.matched_keywords`：已匹配关键词
- `detail.missing_keywords`：待补足关键词

### 一步完成解析和匹配

```http
POST /api/analyze
Content-Type: multipart/form-data

file=<PDF 文件>
job_description=<岗位需求文本>
```

## 部署

### 后端部署到 Render

1. 将项目推送到公开 GitHub 仓库。
2. 在 Render 新建 Web Service，选择该仓库。
3. 使用仓库中的 `render.yaml` 或选择 Docker 部署。
4. 配置环境变量：

```env
OPENAI_API_KEY=你的 Key，可选
OPENAI_MODEL=gpt-4.1-mini
CORS_ORIGINS=*
```

部署完成后得到后端地址，例如：

```text
https://ai-resume-parser-api.onrender.com
```

### 前端部署到 GitHub Pages

1. 进入 GitHub 仓库的 `Settings`。
2. 打开 `Pages`。
3. Source 选择 `Deploy from a branch`。
4. Branch 选择 `main`，目录选择 `/frontend`。
5. 部署完成后打开 GitHub Pages 地址。
6. 在页面 `API 地址` 输入后端线上地址。

## 缓存设计

当前实现使用 Redis 优先、SQLite 兜底的缓存策略：

- 简历缓存 Key：`resume:{pdf_sha256}`
- 匹配缓存 Key：`match:{resume_id_or_text_and_job_description_sha256}`

这样同一份简历重复上传时不会重复解析；同一份简历和同一段岗位需求重复评分时会直接返回缓存结果。

本地未配置 Redis 时自动使用 SQLite。线上如果配置 `REDIS_URL`，缓存会优先写入 Redis，并保留 SQLite 兜底。

## 提交信息模板

```text
GitHub 仓库地址：<你的公开仓库地址>
线上演示地址：<GitHub Pages 或其他公开访问地址>
姓名：毛骄
联系方式：15523063767
```
