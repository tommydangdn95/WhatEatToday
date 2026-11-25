"""
Gom ảnh món ăn → ảnh gốc (art/) + ảnh cho app (assets/) + trang xem trước.

  python -X utf8 tools/optimize_dish_images.py

Nguồn ảnh (theo thứ tự ưu tiên cho mỗi món):
  1. art/dish_images_original/<mã>.jpg          (đã có)
  2. images/<mã>.png                            (script Console tải về, đặt tên sẵn)
  3. images/<file> theo tools/dish_image_map.txt (ảnh tải/copy lúc đầu; dòng "<file> SKIP" bị bỏ qua)
  images/ không commit (PNG nặng); bản gốc giữ ở art/ dạng JPEG chất lượng 92.

Ảnh cho app: cắt 4:3 bỏ hình mờ ngôi sao Gemini ở góc dưới phải, thu về 960×720, WebP chất lượng 80
→ app/src/main/assets/dish_images/<mã>.webp
Trang xem trước: art/xem_anh_mon.html (mở bằng trình duyệt).
"""
import html
import json
import pathlib

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
SEED = ROOT / "app/src/main/assets/seed_data.json"
ORIGINALS = ROOT / "art/dish_images_original"
DOWNLOADS = ROOT / "images"
MAP = ROOT / "tools/dish_image_map.txt"
APP_OUT = ROOT / "app/src/main/assets/dish_images"
PREVIEW = ROOT / "art/xem_anh_mon.html"

APP_SIZE = (960, 720)


def crop_without_watermark(im: Image.Image) -> Image.Image:
    """
    Hình mờ ngôi sao Gemini ở góc dưới phải, kích thước cố định theo pixel nên vị trí tương đối khác nhau:
    ảnh gốc 2400px: ~91–97% chiều cao; ảnh hiển thị 1200px: ~84–89% chiều cao.
    → giữ phần phía trên hình mờ, cắt khung 4:3 ở giữa.
    """
    w, h = im.size
    top, bottom = round(h * 0.01), round(h * (0.90 if w >= 2000 else 0.83))
    ch = bottom - top
    cw = min(w, round(ch * 4 / 3))
    left = (w - cw) // 2
    return im.crop((left, top, left + cw, bottom))


def collect_sources(dish_ids):
    mapped = {}
    if MAP.exists():
        for line in MAP.read_text(encoding="utf-8").splitlines():
            parts = line.split()
            if len(parts) == 2 and parts[1] != "SKIP":
                mapped[parts[1]] = DOWNLOADS / parts[0]
    sources = {}
    for d in dish_ids:
        for cand in (ORIGINALS / f"{d}.jpg", DOWNLOADS / f"{d}.png", mapped.get(d)):
            if cand is not None and cand.exists():
                sources[d] = cand
                break
    return sources


def main():
    seed = json.loads(SEED.read_text(encoding="utf-8"))
    dishes = seed["dishes"]
    ids = [d["id"] for d in dishes]
    sources = collect_sources(ids)

    ORIGINALS.mkdir(parents=True, exist_ok=True)
    APP_OUT.mkdir(parents=True, exist_ok=True)
    total_app = 0
    rows = []
    for d in dishes:
        src = sources.get(d["id"])
        if src is None:
            rows.append((d, None))
            continue
        im = Image.open(src).convert("RGB")
        orig = ORIGINALS / f"{d['id']}.jpg"
        if src != orig:
            im.save(orig, quality=92, optimize=True)
        app = APP_OUT / f"{d['id']}.webp"
        crop_without_watermark(im).resize(APP_SIZE, Image.LANCZOS).save(app, "WEBP", quality=80, method=6)
        total_app += app.stat().st_size
        rows.append((d, im.size))

    missing = [d["id"] for d, size in rows if size is None]
    print(f"Ảnh cho app: {len(rows) - len(missing)}/{len(rows)} món, tổng {total_app / 1024 / 1024:.1f} MB → {APP_OUT.relative_to(ROOT)}")
    if missing:
        print("THIẾU ẢNH:", ", ".join(missing))

    cards = []
    for d, size in rows:
        name = html.escape(d["name"])
        if size is None:
            cards.append(f'<figure class="miss"><div>THIẾU ẢNH</div><figcaption>{name}<br><code>{d["id"]}</code></figcaption></figure>')
        else:
            note = "gốc 2400px" if size[0] >= 2000 else f"gốc {size[0]}×{size[1]}"
            cards.append(
                f'<figure><img loading="lazy" src="../app/src/main/assets/dish_images/{d["id"]}.webp" alt="{name}">'
                f'<figcaption>{name}<br><code>{d["id"]}</code> · {d["category"]} · {note}</figcaption></figure>'
            )
    PREVIEW.write_text(
        "<!DOCTYPE html><html lang='vi'><meta charset='utf-8'><title>Ảnh món ăn — xem trước</title>"
        "<style>body{font:16px system-ui;margin:16px;background:#FFFBF5}"
        "main{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:16px}"
        "figure{margin:0;background:#fff;border:1px solid #e8ded2;border-radius:12px;overflow:hidden}"
        "img{width:100%;display:block;aspect-ratio:4/3}figcaption{padding:8px 12px;font-size:20px;font-weight:600}"
        "code{font-size:13px;font-weight:400;color:#5a5148}.miss div{aspect-ratio:4/3;display:grid;place-items:center;"
        "background:#fde;color:#a0153e;font-weight:700}</style>"
        f"<h1>Ảnh món ăn ({len(rows) - len(missing)}/{len(rows)})</h1><main>{''.join(cards)}</main></html>",
        encoding="utf-8",
    )
    print("Trang xem trước:", PREVIEW.relative_to(ROOT))


if __name__ == "__main__":
    main()
