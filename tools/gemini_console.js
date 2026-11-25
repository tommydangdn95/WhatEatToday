/*
 * Tạo ảnh món ăn hàng loạt trên web Gemini — dán vào Console của Chrome (F12 → Console), Enter.
 *
 * Chuẩn bị:
 *   - Mở một chat Gemini có quyền tạo ảnh (chat mới, tài khoản nào cũng được — mỗi câu lệnh đã đủ yêu cầu phong cách).
 *   - Chrome: Site settings của gemini.google.com → Automatic downloads = Allow;
 *     Settings → Downloads → thư mục lưu = D:\Project\App\images, tắt "Ask where to save".
 *   - Để tab Gemini hiện trên màn hình trong lúc chạy (tab ẩn bị trình duyệt làm chậm).
 *
 * Mỗi món: gõ câu lệnh → chờ ảnh → tải ảnh hiển thị (1200×896) với tên <mã_món>.<đuôi> vào thư mục tải.
 * Lỗi / quá thời gian → thử lại 1 lần; vẫn lỗi thì bỏ qua, cuối cùng in danh sách món chưa có ảnh.
 * Dừng giữa chừng: gõ  window.__stopDishes = true  trong Console.
 * Chạy lại một số món: sửa mảng DISHES (chỉ giữ các món cần làm) rồi dán lại.
 */
(async () => {
  // Câu lệnh đầy đủ trong mỗi lần gửi → chạy được ở chat mới / tài khoản khác mà ảnh vẫn cùng kiểu.
  const STYLE = 'Tạo một ảnh chụp thực tế, khung ngang tỉ lệ 4:3, độ phân giải cao. Chỉ một món, đựng trong bát/đĩa sứ gia đình Việt Nam (men trắng viền hoa lam), món chiếm khoảng 75% khung hình và đặt chính giữa, chụp gần góc nghiêng khoảng 45 độ, lấy nét sắc toàn bộ món. Ánh sáng ban ngày tự nhiên, sáng rõ, màu sắc tươi và đúng thực tế. Nền gọn: mặt bàn gỗ sáng. Không chữ, không logo, không người, không tay. Ảnh như chụp bằng máy ảnh thật, không phải tranh vẽ';
  // Chỉ giữ các món cần tạo (lại). Đã có đủ 80 món; đậu bắp lần trước ra nhầm rau xanh.
  const DISHES = [
    ['dau_bap_luoc', 'Đậu bắp luộc (okra): khoảng 15 QUẢ đậu bắp xanh nguyên quả, hình thuôn dài có cạnh khía, luộc chín xếp trên đĩa sứ, kèm bát nhỏ nước mắm tỏi ớt. Không phải rau lá.'],
  ];

  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const scroller = () => document.querySelector('infinite-scroller.chat-history');
  const images = () => [...(scroller() || document).querySelectorAll('img')].filter((i) => i.complete && i.naturalWidth > 300);
  const input = () => document.querySelector('rich-textarea div[contenteditable="true"], div[contenteditable="true"]');

  async function send(text) {
    const el = input();
    el.focus();
    document.execCommand('selectAll', false, null);
    document.execCommand('insertText', false, text);
    await sleep(500);
    const btn = [...document.querySelectorAll('button')].find((b) => /send|gửi/i.test(b.getAttribute('aria-label') || ''));
    if (btn && !btn.disabled) btn.click();
    else el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, bubbles: true }));
  }

  async function waitNewImage(before, timeoutMs = 180000) {
    const t0 = Date.now();
    while (Date.now() - t0 < timeoutMs) {
      if (window.__stopDishes) return null;
      const list = images();
      if (list.length > before) {
        await sleep(2500); // chờ ảnh hiện đủ nét
        const imgs = images();
        return imgs[imgs.length - 1];
      }
      await sleep(1000);
    }
    return null;
  }

  // Không dùng fetch(img.src): CSP "connect-src" của Gemini chặn đọc blob:. Vẽ ảnh lên canvas rồi xuất PNG.
  async function save(img, id) {
    const canvas = document.createElement('canvas');
    canvas.width = img.naturalWidth;
    canvas.height = img.naturalHeight;
    canvas.getContext('2d').drawImage(img, 0, 0);
    const blob = await new Promise((resolve, reject) =>
      canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('canvas.toBlob trả về rỗng'))), 'image/png'));
    const ext = 'png';
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `${id}.${ext}`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 30000);
    return `${id}.${ext} (${img.naturalWidth}×${img.naturalHeight}, ${Math.round(blob.size / 1024)} KB)`;
  }

  window.__stopDishes = false;
  const failed = [];
  for (let i = 0; i < DISHES.length; i++) {
    if (window.__stopDishes) { console.warn('Đã dừng theo yêu cầu.'); break; }
    const [id, desc] = DISHES[i];
    let ok = false;
    for (let attempt = 1; attempt <= 2 && !ok; attempt++) {
      const before = images().length;
      const prefix = attempt === 1 ? 'Ảnh tiếp theo' : 'Tạo lại ảnh';
      await send(`${prefix}. ${STYLE}. Món kiểu gia đình Hà Nội — ${desc}`);
      const img = await waitNewImage(before);
      if (img) {
        try {
          console.log(`[${i + 1}/${DISHES.length}] ✓ ${await save(img, id)}`);
          ok = true;
        } catch (e) {
          console.warn(`[${i + 1}/${DISHES.length}] ${id}: lỗi tải`, e);
        }
      } else {
        console.warn(`[${i + 1}/${DISHES.length}] ${id}: không thấy ảnh (lần ${attempt})`);
      }
      await sleep(3000);
    }
    if (!ok) failed.push(id);
  }
  console.log(failed.length ? `XONG. Chưa có ảnh: ${failed.join(', ')}` : 'XONG tất cả món.');
})();
