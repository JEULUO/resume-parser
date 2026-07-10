import hashlib

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware

from .ai_extractor import extract_key_info
from .cache import CacheStore
from .config import get_settings
from .matcher import cache_key_for_match, match_resume
from .models import AnalyzeResponse, JobMatchRequest, JobMatchResponse, ResumeKeyInfo, ResumeParseResponse
from .pdf_parser import PdfParseError, extract_text_from_pdf
from .text_cleaner import clean_resume_text

settings = get_settings()
cache = CacheStore(settings.cache_db_path, settings.redis_url)

app = FastAPI(title=settings.app_name, version="1.0.0")
allow_credentials = "*" not in settings.cors_origin_list
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=allow_credentials,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/health")
def health() -> dict[str, str]:
    return {"status": "ok", "app": settings.app_name}


@app.post("/api/resumes/upload", response_model=ResumeParseResponse)
async def upload_resume(file: UploadFile = File(...)) -> ResumeParseResponse:
    if file.content_type not in {"application/pdf", "application/x-pdf"} and not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="仅支持上传 PDF 格式简历。")

    file_bytes = await file.read()
    if not file_bytes:
        raise HTTPException(status_code=400, detail="上传文件为空。")

    resume_id = hashlib.sha256(file_bytes).hexdigest()
    cache_key = f"resume:{resume_id}"
    cached = cache.get(cache_key)
    if cached:
        cached["cache_hit"] = True
        return ResumeParseResponse.model_validate(cached)

    try:
        raw_text, pages = extract_text_from_pdf(file_bytes)
    except PdfParseError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    cleaned_text, paragraphs = clean_resume_text(raw_text)
    key_info = extract_key_info(cleaned_text, settings)
    response = ResumeParseResponse(
        resume_id=resume_id,
        file_name=file.filename,
        pages=pages,
        cleaned_text=cleaned_text,
        paragraphs=paragraphs,
        key_info=key_info,
        cache_hit=False,
    )
    cache.set(cache_key, response.model_dump())
    return response


@app.post("/api/match", response_model=JobMatchResponse)
def match_job(payload: JobMatchRequest) -> JobMatchResponse:
    resume_text, key_info = resolve_resume(payload)
    cache_key = cache_key_for_match(payload.resume_id, resume_text, payload.job_description)
    cached = cache.get(cache_key)
    if cached:
        cached["cache_hit"] = True
        return JobMatchResponse.model_validate(cached)

    response = match_resume(payload.resume_id, resume_text, key_info, payload.job_description, settings)
    cache.set(cache_key, response.model_dump())
    return response


@app.post("/api/analyze", response_model=AnalyzeResponse)
async def analyze_resume(
    file: UploadFile = File(...),
    job_description: str | None = Form(default=None),
) -> AnalyzeResponse:
    resume = await upload_resume(file)
    if not job_description:
        return AnalyzeResponse(resume=resume)
    match = match_job(JobMatchRequest(resume_id=resume.resume_id, job_description=job_description))
    return AnalyzeResponse(resume=resume, match=match)


def resolve_resume(payload: JobMatchRequest) -> tuple[str, ResumeKeyInfo]:
    if payload.resume_id:
        cached = cache.get(f"resume:{payload.resume_id}")
        if cached:
            parsed = ResumeParseResponse.model_validate(cached)
            return parsed.cleaned_text, parsed.key_info

    if payload.resume_text:
        key_info = payload.key_info or extract_key_info(payload.resume_text, settings)
        return payload.resume_text, key_info

    raise HTTPException(status_code=400, detail="请提供 resume_id，或直接提供 resume_text。")
