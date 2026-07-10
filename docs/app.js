const apiBaseInput = document.querySelector("#apiBase");
const fileInput = document.querySelector("#resumeFile");
const fileName = document.querySelector("#fileName");
const jobDescription = document.querySelector("#jobDescription");
const analyzeBtn = document.querySelector("#analyzeBtn");
const statusText = document.querySelector("#status");
const scoreValue = document.querySelector("#scoreValue");
const scoreBar = document.querySelector("#scoreBar");
const keyInfo = document.querySelector("#keyInfo");
const keywords = document.querySelector("#keywords");
const matchDetail = document.querySelector("#matchDetail");
const jsonOutput = document.querySelector("#jsonOutput");
const copyJsonBtn = document.querySelector("#copyJsonBtn");

const savedApiBase = localStorage.getItem("resumeMatcherApiBase");
const isLocalFrontend = ["localhost", "127.0.0.1"].includes(window.location.hostname);
const isLocalApi = savedApiBase && /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?\/?$/i.test(savedApiBase);

if (savedApiBase && (isLocalFrontend || !isLocalApi)) {
  apiBaseInput.value = savedApiBase;
} else {
  localStorage.setItem("resumeMatcherApiBase", apiBaseInput.value);
}

window.addEventListener("DOMContentLoaded", () => {
  if (window.lucide) window.lucide.createIcons();
});

apiBaseInput.addEventListener("change", () => {
  localStorage.setItem("resumeMatcherApiBase", apiBaseInput.value.trim());
});

fileInput.addEventListener("change", () => {
  const file = fileInput.files?.[0];
  fileName.textContent = file ? file.name : "选择 PDF 简历";
});

analyzeBtn.addEventListener("click", async () => {
  const file = fileInput.files?.[0];
  const jd = jobDescription.value.trim();
  const apiBase = apiBaseInput.value.trim().replace(/\/$/, "");

  if (!file) return setStatus("请先选择 PDF 简历", true);
  if (!jd || jd.length < 10) return setStatus("请填写不少于 10 个字符的岗位需求", true);
  if (!apiBase) return setStatus("请填写后端 API 地址", true);

  const formData = new FormData();
  formData.append("file", file);
  formData.append("job_description", jd);

  setLoading(true);
  setStatus("分析中");

  try {
    const response = await fetch(`${apiBase}/api/analyze`, {
      method: "POST",
      body: formData,
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.detail || "分析失败");

    renderResult(payload);
    setStatus(payload.resume.cache_hit ? "命中简历缓存" : "分析完成");
  } catch (error) {
    setStatus(error.message, true);
  } finally {
    setLoading(false);
  }
});

copyJsonBtn.addEventListener("click", async () => {
  await navigator.clipboard.writeText(jsonOutput.textContent);
  setStatus("JSON 已复制");
});

function setLoading(isLoading) {
  analyzeBtn.disabled = isLoading;
  analyzeBtn.querySelector("span").textContent = isLoading ? "分析中" : "开始分析";
}

function setStatus(message, isError = false) {
  statusText.textContent = message;
  statusText.style.color = isError ? "var(--rose)" : "var(--muted)";
}

function renderResult(payload) {
  const info = payload.resume?.key_info || {};
  const match = payload.match;
  const score = Number(match?.score || 0);

  scoreValue.textContent = match ? `${score}` : "--";
  scoreBar.style.width = `${Math.max(0, Math.min(score, 100))}%`;
  renderKeyInfo(info);
  renderKeywords(match?.job_keywords || []);
  renderMatchDetail(match);
  jsonOutput.textContent = JSON.stringify(payload, null, 2);
}

function renderKeyInfo(info) {
  const rows = [
    ["姓名", info.name],
    ["电话", info.phone],
    ["邮箱", info.email],
    ["地址", info.address],
    ["求职意向", info.job_intention],
    ["期望薪资", info.expected_salary],
    ["工作年限", info.years_of_experience],
    ["技能", (info.skills || []).join("、")],
  ];

  keyInfo.innerHTML = rows
    .map(([label, value]) => `<dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value || "未识别")}</dd>`)
    .join("");
}

function renderKeywords(items) {
  keywords.innerHTML = items.length
    ? items.map((item) => `<span class="chip">${escapeHtml(item)}</span>`).join("")
    : `<span class="detail-empty">暂无关键词</span>`;
}

function renderMatchDetail(match) {
  if (!match) {
    matchDetail.className = "detail-empty";
    matchDetail.textContent = "暂无结果";
    return;
  }

  const detail = match.detail || {};
  matchDetail.className = "detail-list";
  matchDetail.innerHTML = `
    <div><strong>技能匹配率：</strong>${toPercent(detail.skill_match_rate)}</div>
    <div><strong>经验相关性：</strong>${toPercent(detail.experience_relevance)}</div>
    <div><strong>已匹配：</strong>${renderChips(detail.matched_keywords || [])}</div>
    <div><strong>待补足：</strong>${renderChips(detail.missing_keywords || [], "missing")}</div>
    ${detail.ai_comment ? `<div><strong>AI 评语：</strong>${escapeHtml(detail.ai_comment)}</div>` : ""}
  `;
}

function renderChips(items, className = "") {
  if (!items.length) return "无";
  return items.map((item) => `<span class="chip ${className}">${escapeHtml(item)}</span>`).join(" ");
}

function toPercent(value) {
  return `${Math.round(Number(value || 0) * 100)}%`;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
