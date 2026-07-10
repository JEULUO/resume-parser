import re


NOISE_PATTERNS = [
    r"\x00",
    r"第\s*\d+\s*/\s*\d+\s*页",
    r"Page\s+\d+\s+of\s+\d+",
]


def clean_resume_text(raw_text: str) -> tuple[str, list[str]]:
    text = raw_text.replace("\r", "\n")
    for pattern in NOISE_PATTERNS:
        text = re.sub(pattern, " ", text, flags=re.IGNORECASE)

    text = re.sub(r"[ \t\f\v]+", " ", text)
    text = re.sub(r" *\n *", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    text = normalize_broken_lines(text)
    paragraphs = [segment.strip(" -•\t") for segment in re.split(r"\n{2,}", text) if segment.strip(" -•\t")]
    return "\n\n".join(paragraphs).strip(), paragraphs


def normalize_broken_lines(text: str) -> str:
    lines = [line.strip() for line in text.splitlines()]
    merged: list[str] = []
    buffer = ""

    for line in lines:
        if not line:
            if buffer:
                merged.append(buffer.strip())
                buffer = ""
            merged.append("")
            continue

        if buffer and should_merge_line(buffer, line):
            buffer = f"{buffer} {line}"
        else:
            if buffer:
                merged.append(buffer.strip())
            buffer = line

    if buffer:
        merged.append(buffer.strip())

    return "\n".join(merged)


def should_merge_line(previous: str, current: str) -> bool:
    if re.search(r"[:：]$", previous):
        return False
    if re.match(r"^(教育经历|工作经历|项目经历|技能|求职意向|自我评价|个人信息)", current):
        return False
    if len(previous) < 18:
        return False
    return not re.search(r"[。.!?；;]$", previous)
