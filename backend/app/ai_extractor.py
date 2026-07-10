import json
import re

from openai import OpenAI

from .config import Settings
from .models import ResumeKeyInfo


EMAIL_RE = re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+")
PHONE_RE = re.compile(r"(?<!\d)(?:\+?86[- ]?)?1[3-9]\d{9}(?!\d)")
SALARY_RE = re.compile(r"(?:期望薪资|薪资要求|期望月薪)[:： ]*([0-9kK万千\-~至以上/每月]+)")
YEARS_RE = re.compile(r"(\d{1,2})\s*年(?:以上)?(?:工作|开发|项目|相关)?经验")

SECTION_HEADERS = {
    "education_background": ("教育经历", "教育背景", "学历", "Education"),
    "project_experiences": ("项目经历", "项目经验", "Projects"),
    "work_experiences": ("工作经历", "实习经历", "工作经验", "Experience"),
}

COMMON_SKILLS = [
    "Python",
    "Java",
    "JavaScript",
    "TypeScript",
    "React",
    "Vue",
    "FastAPI",
    "Django",
    "Flask",
    "Spring",
    "MySQL",
    "PostgreSQL",
    "Redis",
    "Docker",
    "Kubernetes",
    "Linux",
    "NLP",
    "机器学习",
    "深度学习",
    "大模型",
    "数据分析",
    "爬虫",
    "Git",
]


def extract_key_info(text: str, settings: Settings) -> ResumeKeyInfo:
    if settings.openai_api_key:
        ai_result = try_extract_with_ai(text, settings)
        if ai_result:
            return enrich_with_rules(ai_result, text)
    return extract_with_rules(text)


def try_extract_with_ai(text: str, settings: Settings) -> ResumeKeyInfo | None:
    client = OpenAI(api_key=settings.openai_api_key)
    prompt = f"""
你是严谨的简历解析助手。请从简历文本中提取结构化信息，只返回 JSON。
字段必须包含：
name, phone, email, address, job_intention, expected_salary, years_of_experience,
education_background, project_experiences, skills, work_experiences。
无法确定的字符串字段返回 null，列表字段返回 []。

简历文本：
{text[:12000]}
"""
    try:
        response = client.chat.completions.create(
            model=settings.openai_model,
            messages=[
                {"role": "system", "content": "你只输出可被 json.loads 解析的 JSON，不要输出 Markdown。"},
                {"role": "user", "content": prompt},
            ],
            temperature=0.1,
        )
        content = response.choices[0].message.content or "{}"
        data = json.loads(strip_json_fence(content))
        return ResumeKeyInfo.model_validate(data)
    except Exception:
        return None


def strip_json_fence(content: str) -> str:
    content = content.strip()
    if content.startswith("```"):
        content = re.sub(r"^```(?:json)?", "", content).strip()
        content = re.sub(r"```$", "", content).strip()
    return content


def extract_with_rules(text: str) -> ResumeKeyInfo:
    phone = first_match(PHONE_RE, text)
    email = first_match(EMAIL_RE, text)
    salary = first_group(SALARY_RE, text)
    years = first_group(YEARS_RE, text)
    sections = {key: find_sections(text, headers) for key, headers in SECTION_HEADERS.items()}

    return ResumeKeyInfo(
        name=guess_name(text),
        phone=phone,
        email=email,
        address=guess_address(text),
        job_intention=guess_job_intention(text),
        expected_salary=salary,
        years_of_experience=f"{years}年" if years else None,
        education_background=sections["education_background"],
        project_experiences=sections["project_experiences"],
        work_experiences=sections["work_experiences"],
        skills=extract_skills(text),
    )


def enrich_with_rules(info: ResumeKeyInfo, text: str) -> ResumeKeyInfo:
    fallback = extract_with_rules(text)
    data = info.model_dump()
    for key, value in fallback.model_dump().items():
        if not data.get(key):
            data[key] = value
    return ResumeKeyInfo.model_validate(data)


def first_match(pattern: re.Pattern[str], text: str) -> str | None:
    match = pattern.search(text)
    return match.group(0) if match else None


def first_group(pattern: re.Pattern[str], text: str) -> str | None:
    match = pattern.search(text)
    return match.group(1).strip() if match else None


def guess_name(text: str) -> str | None:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    for line in lines[:8]:
        compact = re.sub(r"\s+", "", line)
        compact = re.sub(r"(姓名|个人简历|简历|Resume|CV)[:：]?", "", compact, flags=re.IGNORECASE)
        if re.fullmatch(r"[\u3400-\u9fff]{2,4}", compact):
            return compact
    match = re.search(r"姓名[:： ]*([\u3400-\u9fff]{2,4})", text)
    return match.group(1) if match else None


def guess_address(text: str) -> str | None:
    match = re.search(r"(?:地址|现居地|所在地)[:： ]*([^\n，,;；]{2,40})", text)
    return match.group(1).strip() if match else None


def guess_job_intention(text: str) -> str | None:
    match = re.search(r"(?:求职意向|应聘岗位|目标岗位)[:： ]*([^\n，,;；]{2,50})", text)
    return match.group(1).strip() if match else None


def find_sections(text: str, headers: tuple[str, ...]) -> list[str]:
    results: list[str] = []
    header_pattern = "|".join(re.escape(header) for header in sorted(headers, key=len, reverse=True))
    pattern = rf"(?m)^\s*(?:{header_pattern})[:：]?\s*(.*?)(?=^\s*(?:教育经历|教育背景|工作经历|实习经历|项目经历|项目经验|技能|自我评价|个人信息|求职意向)[:：]?\s*|\Z)"
    for match in re.finditer(pattern, text, flags=re.IGNORECASE | re.DOTALL):
        segment = re.sub(r"\s+", " ", match.group(1)).strip(" -•")
        if segment and len(segment) > 8:
            results.append(segment[:500])
    return dedupe(results)[:5]


def extract_skills(text: str) -> list[str]:
    found = [skill for skill in COMMON_SKILLS if re.search(rf"(?<![A-Za-z]){re.escape(skill)}(?![A-Za-z])", text, re.IGNORECASE)]
    return dedupe(found)


def dedupe(items: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for item in items:
        normalized = item.lower()
        if normalized not in seen:
            seen.add(normalized)
            result.append(item)
    return result
