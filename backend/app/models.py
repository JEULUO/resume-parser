from typing import Any

from pydantic import BaseModel, Field


class ResumeKeyInfo(BaseModel):
    name: str | None = Field(default=None, description="候选人姓名")
    phone: str | None = Field(default=None, description="手机号码")
    email: str | None = Field(default=None, description="邮箱")
    address: str | None = Field(default=None, description="地址")
    job_intention: str | None = Field(default=None, description="求职意向")
    expected_salary: str | None = Field(default=None, description="期望薪资")
    years_of_experience: str | None = Field(default=None, description="工作年限")
    education_background: list[str] = Field(default_factory=list, description="学历背景")
    project_experiences: list[str] = Field(default_factory=list, description="项目经历")
    skills: list[str] = Field(default_factory=list, description="技能关键词")
    work_experiences: list[str] = Field(default_factory=list, description="工作经历")


class ResumeParseResponse(BaseModel):
    resume_id: str
    file_name: str
    pages: int
    cleaned_text: str
    paragraphs: list[str]
    key_info: ResumeKeyInfo
    cache_hit: bool = False


class JobMatchRequest(BaseModel):
    job_description: str = Field(min_length=10)
    resume_id: str | None = None
    resume_text: str | None = None
    key_info: ResumeKeyInfo | None = None


class MatchDetail(BaseModel):
    score: float
    matched_keywords: list[str]
    missing_keywords: list[str]
    skill_match_rate: float
    experience_relevance: float
    ai_comment: str | None = None


class JobMatchResponse(BaseModel):
    resume_id: str | None = None
    job_keywords: list[str]
    score: float
    detail: MatchDetail
    cache_hit: bool = False


class AnalyzeResponse(BaseModel):
    resume: ResumeParseResponse
    match: JobMatchResponse | None = None


class ErrorResponse(BaseModel):
    detail: str
    context: dict[str, Any] | None = None
