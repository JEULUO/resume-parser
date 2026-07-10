import hashlib
import json
import math
import re

from openai import OpenAI

from .config import Settings
from .models import JobMatchResponse, MatchDetail, ResumeKeyInfo


STOP_WORDS = {
    "负责",
    "熟悉",
    "掌握",
    "具备",
    "相关",
    "经验",
    "能力",
    "优先",
    "以及",
    "进行",
    "完成",
    "岗位",
    "要求",
    "工作",
    "the",
    "and",
    "with",
    "for",
}


def match_resume(
    resume_id: str | None,
    resume_text: str,
    key_info: ResumeKeyInfo,
    job_description: str,
    settings: Settings,
) -> JobMatchResponse:
    job_keywords = extract_keywords(job_description)
    resume_keywords = set(extract_keywords(resume_text) + key_info.skills)
    matched = [keyword for keyword in job_keywords if keyword.lower() in {item.lower() for item in resume_keywords}]
    missing = [keyword for keyword in job_keywords if keyword not in matched]

    skill_match_rate = round(len(matched) / max(len(job_keywords), 1), 4)
    experience_relevance = calculate_experience_relevance(key_info, job_description)
    base_score = (skill_match_rate * 70) + (experience_relevance * 30)
    ai_comment = None

    if settings.openai_api_key:
        ai_score, ai_comment = try_ai_score(resume_text, key_info, job_description, settings)
        if ai_score is not None:
            base_score = (base_score * 0.45) + (ai_score * 0.55)

    score = round(min(max(base_score, 0), 100), 2)
    return JobMatchResponse(
        resume_id=resume_id,
        job_keywords=job_keywords,
        score=score,
        detail=MatchDetail(
            score=score,
            matched_keywords=matched,
            missing_keywords=missing[:20],
            skill_match_rate=skill_match_rate,
            experience_relevance=experience_relevance,
            ai_comment=ai_comment,
        ),
    )


def extract_keywords(text: str) -> list[str]:
    english = re.findall(r"[A-Za-z][A-Za-z0-9+#.\-]{1,}", text)
    chinese = re.findall(r"[\u4e00-\u9fa5]{2,8}", text)
    tokens = english + chinese

    scored: dict[str, float] = {}
    for token in tokens:
        normalized = token.strip().lower()
        if normalized in STOP_WORDS or len(normalized) < 2:
            continue
        score = 1 + math.log(len(token) + 1)
        if re.search(r"python|java|react|vue|redis|mysql|docker|linux|算法|后端|前端|大模型|机器学习|数据", normalized):
            score += 2
        scored[token] = scored.get(token, 0) + score

    return [item for item, _ in sorted(scored.items(), key=lambda pair: pair[1], reverse=True)[:30]]


def calculate_experience_relevance(key_info: ResumeKeyInfo, job_description: str) -> float:
    required_years = parse_required_years(job_description)
    actual_years = parse_actual_years(key_info.years_of_experience)
    if required_years == 0:
        return 0.75 if key_info.work_experiences or key_info.project_experiences else 0.45
    if actual_years == 0:
        return 0.35
    return round(min(actual_years / required_years, 1.2) / 1.2, 4)


def parse_required_years(text: str) -> int:
    values = [int(value) for value in re.findall(r"(\d{1,2})\s*年(?:以上)?", text)]
    return max(values) if values else 0


def parse_actual_years(value: str | None) -> int:
    if not value:
        return 0
    match = re.search(r"\d{1,2}", value)
    return int(match.group(0)) if match else 0


def try_ai_score(
    resume_text: str,
    key_info: ResumeKeyInfo,
    job_description: str,
    settings: Settings,
) -> tuple[float | None, str | None]:
    client = OpenAI(api_key=settings.openai_api_key)
    prompt = f"""
请评估候选人与岗位的匹配度。只返回 JSON：
{{"score": 0-100之间数字, "comment": "一句话说明主要匹配点和风险"}}

岗位需求：
{job_description[:5000]}

结构化简历：
{json.dumps(key_info.model_dump(), ensure_ascii=False)}

简历文本：
{resume_text[:9000]}
"""
    try:
        response = client.chat.completions.create(
            model=settings.openai_model,
            messages=[
                {"role": "system", "content": "你是招聘匹配评分助手，只输出 JSON。"},
                {"role": "user", "content": prompt},
            ],
            temperature=0.1,
        )
        payload = json.loads(strip_json_fence(response.choices[0].message.content or "{}"))
        return float(payload.get("score")), payload.get("comment")
    except Exception:
        return None, None


def strip_json_fence(content: str) -> str:
    content = content.strip()
    if content.startswith("```"):
        content = re.sub(r"^```(?:json)?", "", content).strip()
        content = re.sub(r"```$", "", content).strip()
    return content


def cache_key_for_match(resume_id: str | None, resume_text: str, job_description: str) -> str:
    digest = hashlib.sha256(f"{resume_id or resume_text}|{job_description}".encode("utf-8")).hexdigest()
    return f"match:{digest}"
