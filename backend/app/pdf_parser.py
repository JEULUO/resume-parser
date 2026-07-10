from io import BytesIO

from pypdf import PdfReader


class PdfParseError(ValueError):
    pass


def extract_text_from_pdf(file_bytes: bytes) -> tuple[str, int]:
    try:
        reader = PdfReader(BytesIO(file_bytes))
    except Exception as exc:
        raise PdfParseError("无法读取 PDF 文件，请确认文件未损坏且不是加密 PDF。") from exc

    if reader.is_encrypted:
        raise PdfParseError("暂不支持加密 PDF，请先移除密码后再上传。")

    page_texts: list[str] = []
    for index, page in enumerate(reader.pages, start=1):
        try:
            text = page.extract_text() or ""
        except Exception:
            text = ""
        page_texts.append(f"\n\n--- page {index} ---\n{text.strip()}")

    raw_text = "\n".join(page_texts).strip()
    if not raw_text:
        raise PdfParseError("未能从 PDF 中提取文本，扫描件请先进行 OCR。")
    return raw_text, len(reader.pages)
