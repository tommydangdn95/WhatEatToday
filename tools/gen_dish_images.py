"""
Tạo ảnh món ăn bằng Gemini cho tất cả món trong seed_data.json.

  python -X utf8 tools/gen_dish_images.py                  # tạo ảnh còn thiếu
  python -X utf8 tools/gen_dish_images.py --only gia_cay   # chỉ vài món (cách nhau dấu phẩy)
  python -X utf8 tools/gen_dish_images.py --redo gia_cay   # tạo lại món đã có ảnh
  python -X utf8 tools/gen_dish_images.py --model gemini-3.1-flash-image --out art/test

Ảnh gốc lưu ở art/dish_images_original/<mã_món>.png (commit vào git).
Bước tối ưu cho app: tools/optimize_dish_images.py.
Khoá API đọc từ Secret.txt (bị .gitignore).
"""
import argparse
import base64
import json
import pathlib
import sys
import time
import urllib.error
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from gemini_key import ROOT, load_key  # noqa: E402

DEFAULT_MODEL = "gemini-3-pro-image"
SEED = ROOT / "app/src/main/assets/seed_data.json"
RECIPES = ROOT / "app/src/main/assets/recipes.json"

CATEGORY_HINT = {
    "MAN": "món mặn ăn với cơm",
    "XAO": "món xào",
    "RAU": "món rau",
    "CANH": "món canh, đựng trong bát canh/tô",
}

PROMPT = """Ảnh chụp thực tế món ăn gia đình miền Bắc Việt Nam: "{name}" ({category}).
Nguyên liệu chính: {ingredients}.

Yêu cầu ảnh (dùng trong app cho người lớn tuổi, phải nhìn rõ trên điện thoại):
- Đúng món "{name}" như cách nấu trong bữa cơm gia đình Hà Nội, phần ăn cho cả nhà, trông ngon và chân thực.
- Chỉ một món, đựng trong bát/đĩa sứ gia đình Việt Nam, món chiếm khoảng 75% khung hình, đặt chính giữa.
- Chụp gần, góc nghiêng khoảng 45 độ, lấy nét sắc toàn bộ món, không làm mờ hậu cảnh quá mức.
- Ánh sáng ban ngày tự nhiên, sáng rõ, màu sắc tươi và đúng thực tế.
- Nền gọn: mặt bàn gỗ sáng hoặc khăn trải bàn màu nhạt, không đồ vật thừa.
- Không có chữ, không logo, không hình mờ, không người, không tay.
Ảnh chụp như máy ảnh thật, không phải tranh vẽ."""


def load_dishes():
    seed = json.loads(SEED.read_text(encoding="utf-8"))
    ingredients = {i["id"]: i["name"] for i in seed["ingredients"]}
    recipes = {}
    if RECIPES.exists():
        recipes = {r["dishId"]: r for r in json.loads(RECIPES.read_text(encoding="utf-8"))["recipes"]}
    out = []
    for d in seed["dishes"]:
        names = [ingredients.get(x if isinstance(x, str) else x.get("id"), "") for x in d.get("mainIngredients", [])]
        names = [n for n in names if n]
        if not names and d["id"] in recipes:
            # Lấy vài dòng đầu của phần nguyên liệu công thức, bỏ định lượng.
            names = [x.split(":")[0].split("(")[0].strip() for x in recipes[d["id"]]["ingredients"][:5]]
        out.append({"id": d["id"], "name": d["name"], "category": d["category"], "ingredients": names})
    return out


def generate(key: str, model: str, prompt: str, size: str) -> bytes:
    body = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "responseModalities": ["IMAGE"],
            "imageConfig": {"aspectRatio": "4:3", "imageSize": size},
        },
    }
    req = urllib.request.Request(
        f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
        data=json.dumps(body).encode("utf-8"),
        headers={"x-goog-api-key": key, "Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=300) as resp:
        data = json.load(resp)
    for cand in data.get("candidates", []):
        for part in cand.get("content", {}).get("parts", []):
            inline = part.get("inlineData") or part.get("inline_data")
            if inline and inline.get("data"):
                return base64.b64decode(inline["data"])
    raise RuntimeError("Không có ảnh trong phản hồi: " + json.dumps(data, ensure_ascii=False)[:500])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default=DEFAULT_MODEL)
    ap.add_argument("--size", default="2K")
    ap.add_argument("--out", default="art/dish_images_original")
    ap.add_argument("--only", default="")
    ap.add_argument("--redo", default="")
    args = ap.parse_args()

    key = load_key()
    out_dir = ROOT / args.out
    out_dir.mkdir(parents=True, exist_ok=True)
    only = {x for x in (args.only + "," + args.redo).split(",") if x}
    redo = {x for x in args.redo.split(",") if x}

    dishes = [d for d in load_dishes() if not only or d["id"] in only]
    failed = []
    for i, d in enumerate(dishes, 1):
        path = out_dir / f"{d['id']}.png"
        if path.exists() and d["id"] not in redo:
            continue
        prompt = PROMPT.format(
            name=d["name"],
            category=CATEGORY_HINT.get(d["category"], ""),
            ingredients=", ".join(d["ingredients"]) or "theo đúng món",
        )
        for attempt in range(3):
            try:
                t = time.time()
                img = generate(key, args.model, prompt, args.size)
                path.write_bytes(img)
                print(f"[{i}/{len(dishes)}] {d['id']} ✓ {len(img) // 1024} KB, {time.time() - t:.0f}s", flush=True)
                break
            except urllib.error.HTTPError as e:
                msg = e.read().decode("utf-8", "replace")[:300]
                print(f"[{i}/{len(dishes)}] {d['id']} lỗi HTTP {e.code}: {msg}", flush=True)
                time.sleep(20 * (attempt + 1))
            except Exception as e:  # noqa: BLE001
                print(f"[{i}/{len(dishes)}] {d['id']} lỗi: {e}", flush=True)
                time.sleep(10)
        else:
            failed.append(d["id"])
    if failed:
        print("CHƯA TẠO ĐƯỢC:", ",".join(failed))


if __name__ == "__main__":
    main()
