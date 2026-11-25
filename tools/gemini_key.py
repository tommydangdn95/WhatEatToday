"""Đọc khoá Gemini từ Secret.txt ở gốc dự án (file này bị .gitignore, KHÔNG commit)."""
import pathlib, re

ROOT = pathlib.Path(__file__).resolve().parent.parent


def load_key() -> str:
    text = (ROOT / "Secret.txt").read_text(encoding="utf-8")
    m = re.search(r"AIza[0-9A-Za-z_\-]{35}", text)
    if m:
        return m.group(0)
    # Không đúng dạng AIza… → lấy dòng không rỗng cuối cùng, bỏ "KEY=" nếu có.
    line = [l for l in text.splitlines() if l.strip()][-1]
    return line.split("=", 1)[-1].strip().strip('"\'')
