# SPEC — App "Hôm nay ăn gì" (bản 1: Android cho Mẹ)

> Phiên bản spec: 1.2 · 2026-11-10 (1.1: thêm F10 công thức nấu · 1.2: món yêu thích chiếm 10% điểm, tab Món nhà có lọc Yêu thích)
> Nguồn: tài liệu requirements "App 'Hôm nay ăn gì' cho Mẹ — Requirements".
> File dữ liệu đi kèm: `seed_data.json` (80 món + nguyên liệu) và `recipes.json` (công thức 80 món) → đặt trong `app/src/main/assets/`.

---

## 1. Tổng quan

App Android giúp Mẹ trả lời câu "hôm nay ăn gì" trong dưới 30 giây: mở app → xem mâm 4 món gợi ý → đổi món nếu chưa ưng → chốt → gửi thực đơn vào nhóm Zalo gia đình.

| Thông tin | Giá trị |
| --- | --- |
| Người dùng chính | Mẹ, dùng điện thoại ở mức cơ bản (Zalo, Facebook, YouTube) |
| Thiết bị | Vivo 1935 (Vivo Y50), **chỉ chạy được Android 10 (API 29)**; app hỗ trợ từ Android 9 (API 28) |
| Gia đình | 7 người ăn, gốc Bắc, không ăn chay, không ai ăn kiêng |
| Số bữa | 2 bữa/ngày: trưa và tối |
| Mâm cơm | **Tối thiểu 4 món**: 1 mặn + 1 rau + 1 canh + 1 món thứ 4 (mặn hoặc xào) |
| Phân phối | App nội bộ gia đình, không quảng cáo, cài bằng APK đã ký |
| Mạng | Chạy offline; chỉ thời tiết và gửi Zalo cần mạng |

### 1.1 Phạm vi bản 1 (MVP)

F01–F13, F17, F19, F20, F25 (chi tiết ở mục 7).

### 1.2 Ngoài phạm vi bản 1 (để sau)

| ID | Nội dung |
| --- | --- |
| F14 | Thực đơn 7 ngày |
| F15 | Hồ sơ thành viên, món từng người không thích |
| F16, F22, F23 | Bản web cho cả nhà: đăng nhập Zalo, xem thực đơn, góp ý, nhận thông báo (cần host server; 3 người dùng iPhone) |
| F18 | Giọng nói (nhận dạng + đọc to) |
| F21 | Tự gạch nguyên liệu đã dùng |
| F24, F26 | Nhập lượng nguyên liệu, tính đủ cho 7 người, hỏi "Thiếu / Vừa / Dư" sau bữa |

Không làm: quảng cáo, thanh toán, đăng nhập, phát hành công khai trên Play Store.

---

## 2. Nguyên tắc UX (bắt buộc)

| Nguyên tắc | Quy định cụ thể |
| --- | --- |
| Chữ to | Nội dung ≥ 18sp, tiêu đề ≥ 24sp; dùng `sp` để theo cỡ chữ hệ thống |
| Nút to | Vùng bấm ≥ 56×56dp, cách nhau ≥ 12dp |
| Ít bước | Xem gợi ý → chốt món: tối đa 2 lần bấm |
| Ngôn ngữ | Tiếng Việt đời thường: "Đổi món khác", "Chọn mâm này", "Gửi cho cả nhà" |
| Có hình | Mỗi món có ảnh; nếu chưa có ảnh thì dùng ảnh theo loại món |
| Không đăng nhập | Mở là dùng được |
| Không sợ bấm nhầm | Mọi thao tác xoá có Snackbar "Hoàn tác" trong 5 giây |
| Tương phản | Đạt WCAG AA; hỗ trợ TalkBack (`contentDescription` cho icon) |
| Không quảng cáo | Không popup, không quảng cáo |

---

## 3. Tech stack

| Phần | Lựa chọn |
| --- | --- |
| IDE | Android Studio (bản ổn định mới nhất) |
| Ngôn ngữ | Kotlin |
| UI | Jetpack Compose + Material 3, Navigation Compose |
| Kiến trúc | MVVM: `ViewModel` + `StateFlow`, Repository; Hilt cho DI |
| CSDL | Room (SQLite), nạp dữ liệu ban đầu từ `assets/seed_data.json` lần chạy đầu |
| Cài đặt | DataStore (Preferences) |
| Nhắc giờ | `AlarmManager.setExactAndAllowWhileIdle` + `BroadcastReceiver` |
| Tác vụ nền | WorkManager (lấy thời tiết mỗi ngày) |
| Mạng | Retrofit + OkHttp + kotlinx.serialization (hoặc Ktor client) |
| Ảnh | Coil |
| Chọn / chụp ảnh | `ActivityResultContracts.PickVisualMedia`, `TakePicture` |
| Chia sẻ | `Intent.ACTION_SEND` + `FileProvider` |
| Báo lỗi (tuỳ chọn) | Firebase Crashlytics |
| Test | JUnit + Turbine cho ViewModel; unit test bắt buộc cho `SuggestionEngine` |

### 3.1 Cấu hình build

```kotlin
android {
    defaultConfig {
        applicationId = "com.family.homnayangi"   // đổi theo ý bạn
        minSdk = 28                               // Android 9 trở lên; máy của Mẹ chạy Android 10
        targetSdk = /* bản mới nhất Android Studio đề xuất */
    }
}
```

- Code dùng API mới hơn 28 phải bọc trong `if (Build.VERSION.SDK_INT >= ...)`.
- Emulator để test: **Android 9 (API 28)** và **Android 10 (API 29)**; máy thật chính là Vivo của Mẹ (Android 10).
- Build release: APK đã ký (`Build → Generate Signed App Bundle / APK → APK`).

---

## 4. Cấu trúc project đề xuất

```
app/src/main/java/com/family/homnayangi/
├── MainActivity.kt
├── HomNayAnGiApp.kt                 // @HiltAndroidApp
├── data/
│   ├── db/                          // AppDatabase, DAO, Entity, Converters
│   ├── seed/SeedLoader.kt           // đọc assets/seed_data.json lần đầu
│   ├── weather/                     // WeatherApi, WeatherRepository
│   ├── settings/SettingsRepository.kt   // DataStore
│   └── repo/                        // DishRepository, MealRepository, PantryRepository, ShoppingRepository
├── domain/
│   ├── SuggestionEngine.kt          // thuật toán gợi ý (thuần Kotlin, test được)
│   ├── MealSlotResolver.kt          // xác định bữa sắp tới
│   └── model/                       // Meal, MealSlot, Season, WeatherClass…
├── reminder/
│   ├── ReminderScheduler.kt
│   ├── ReminderReceiver.kt
│   └── BootReceiver.kt
├── share/
│   ├── MenuImageRenderer.kt         // vẽ ảnh thực đơn ra PNG
│   └── ZaloSharer.kt
├── work/WeatherWorker.kt
└── ui/
    ├── theme/                       // chữ to, màu tương phản cao
    ├── today/                       // Hôm nay (màn hình chính)
    ├── pantry/                      // Tủ lạnh có gì
    ├── shopping/                    // Đi chợ
    ├── dishes/                      // Món của nhà + thêm/sửa món
    ├── history/                     // Lịch sử
    └── settings/                    // Cài đặt
app/src/main/assets/seed_data.json
app/src/main/res/xml/file_paths.xml  // cho FileProvider
```

---

## 5. Mô hình dữ liệu (Room)

### 5.1 Enum

```kotlin
enum class DishCategory { MAN, XAO, RAU, CANH }        // XAO = xào có thịt/hải sản; RAU = rau luộc, rau xào chay, nộm
enum class Season { NONG, LANH, QUANH_NAM }
enum class Region { BAC, CHUNG, TRUNG, NAM }           // CHUNG = món cả nước đều nấu
enum class MealType { TRUA, TOI }
enum class IngredientGroup { THIT, CA_HAI_SAN, TRUNG_DAU, RAU_CU, KHAC }
enum class WeatherClass { NONG, MAT_LANH, BINH_THUONG }
```

### 5.2 Entity

```kotlin
@Entity data class Dish(
    @PrimaryKey val id: String,             // slug, vd "ca_kho_rieng"; món Mẹ thêm: "custom_<uuid>"
    val name: String,
    val nameNoAccent: String,               // để tìm kiếm không dấu
    val category: DishCategory,
    val seasons: List<Season>,              // TypeConverter
    val region: Region,
    val cookTimeMin: Int,
    val imageUri: String?,                  // asset hoặc ảnh Mẹ chụp
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isCustom: Boolean = false,
    val note: String? = null
)

@Entity data class Ingredient(
    @PrimaryKey val id: String,
    val name: String,
    val nameNoAccent: String,
    val group: IngredientGroup,
    val aliases: List<String>               // "ba rọi", "thịt xay"…
)

@Entity(primaryKeys = ["dishId", "ingredientId"])
data class DishIngredient(val dishId: String, val ingredientId: String, val isMain: Boolean = true)

@Entity data class PantryItem(              // "Tủ lạnh có gì" — chỉ tên, KHÔNG lượng
    @PrimaryKey val ingredientId: String,
    val addedAt: Long
)

@Entity data class MealLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,                    // TypeConverter
    val mealType: MealType,
    val dishIds: List<String>,              // ≥ 4 món
    val confirmedAt: Long
)   // unique (date, mealType): chốt lại thì ghi đè

@Entity data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ingredientId: String?,              // null nếu Mẹ gõ tay
    val freeText: String?,
    val isBought: Boolean = false,
    val createdFromMealLogId: Long?
)

@Entity data class Recipe(                  // F10 — bảng riêng để bản cập nhật thêm được công thức
    @PrimaryKey val dishId: String,
    val ingredients: List<String>,          // mỗi phần tử một dòng, vd "700g thịt ba chỉ"
    val steps: List<String>,
    val videoUrl: String?,                  // link Mẹ dán; null → trang tìm kiếm YouTube
    val isEdited: Boolean = false           // Mẹ đã sửa → seed không ghi đè
)

@Entity data class WeatherCache(
    @PrimaryKey val date: LocalDate,
    val maxTempC: Double,
    val rainMm: Double,
    val classification: WeatherClass,
    val fetchedAt: Long
)
```

### 5.3 Cài đặt (DataStore)

| Key | Mặc định | Ghi chú |
| --- | --- | --- |
| `servings` | 7 | Chưa dùng tính lượng ở bản 1, hiển thị trong ảnh chia sẻ |
| `minDishes` | 4 | Tối thiểu 4 |
| `repeatAvoidDays` | 4 | Không gợi ý lại món đã ăn trong N ngày |
| `hotThresholdC` | 32 | Nhiệt độ cao nhất ≥ ngưỡng → NONG |
| `coolThresholdC` | 25 | Nhiệt độ cao nhất ≤ ngưỡng → MAT_LANH |
| `rainThresholdMm` | 10 | Mưa ≥ ngưỡng → MAT_LANH |
| `reminderLunchPrevDay` | 20:00 | Nhắc chọn món trưa ngày mai |
| `reminderDinner` | 13:00 | Nhắc chọn món tối |
| `reminderLunchMorning` | 06:00 | Nhắc lại món trưa hôm nay |
| `remindersEnabled` | true | |
| `quickFilterUnder30` | false | Bộ lọc "Món nhanh < 30 phút" |

---

## 6. Dữ liệu ban đầu (`seed_data.json`)

```json
{
  "version": 1,
  "ingredients": [ { "id": "thit_ba_chi", "name": "Thịt ba chỉ", "group": "THIT", "aliases": ["ba chỉ", "ba rọi"] } ],
  "dishes": [ { "id": "ca_kho_rieng", "name": "Cá kho riềng", "category": "MAN",
                "seasons": ["LANH"], "region": "BAC", "cookTimeMin": 90,
                "mainIngredients": ["ca_tram"] } ]
}
```

- Lần chạy đầu: `SeedLoader` đọc file, tạo `nameNoAccent`, insert vào Room trong 1 transaction.
- Khi `version` trong file tăng: chỉ thêm món/nguyên liệu mới, **không ghi đè** món Mẹ đã sửa, đã thích hoặc đã ẩn.
- Mùa và thời gian nấu trong file là ước tính ban đầu, Mẹ sửa được trong app.

### 6.1 Công thức (`recipes.json`)

```json
{ "version": 1, "servings": 7,
  "recipes": [ { "dishId": "ca_kho_rieng",
                 "ingredients": ["1 con cá trắm (khoảng 1,2kg)", "1 củ riềng to"],
                 "steps": ["Cá làm sạch, khứa khúc…", "…"] } ] }
```

- Có `version` riêng (lưu `recipesVersion` trong DataStore). Tăng `version` → máy đã cài nhận công thức mới.
- Chỉ thêm công thức cho món đang có; insert IGNORE nên không ghi đè công thức Mẹ đã sửa.
- Bước nấu không tự ghi số thứ tự — màn hình tự đánh số.

---

## 7. Chức năng chi tiết

### F01 — Màn hình Hôm nay hiển thị bữa sắp tới

`MealSlotResolver` xác định bữa mặc định theo giờ hiện tại:

| Giờ hiện tại | Bữa mặc định |
| --- | --- |
| 00:00 – 10:59 | Trưa hôm nay |
| 11:00 – 19:59 | Tối hôm nay |
| 20:00 – 23:59 | Trưa ngày mai |

- Có công tắc "Trưa / Tối" và mũi tên đổi ngày để Mẹ xem bữa khác.
- Nếu bữa đó đã chốt: hiện mâm đã chốt + nút "Gửi cho cả nhà" + "Chọn lại".
- Nếu chưa chốt: hiện mâm gợi ý.

**Chấp nhận:** mở app lúc 20:30 → tiêu đề "Trưa mai ăn gì?"; mở lúc 12:00 → "Tối nay ăn gì?".

### F02 — Cấu trúc mâm tối thiểu 4 món

Mâm gồm 4 slot: `MAN`, `RAU`, `CANH`, `MON_4`.

- `MON_4` chọn từ món loại `MAN` hoặc `XAO`.
- Không có 2 món trùng nguyên liệu chính loại THIT / CA_HAI_SAN trong cùng mâm (vd không có cả "Cá kho riềng" và "Canh dưa chua nấu cá" dùng cá trắm).
- Cài đặt `minDishes` cho phép thêm slot (≥ 4); slot thêm lấy từ `MAN` hoặc `XAO`.

**Chấp nhận:** mọi mâm gợi ý đều có đủ 1 MAN, 1 RAU, 1 CANH và ít nhất 4 món.

### F03 — Đổi món

- Mỗi thẻ món có nút "Đổi" → thay đúng slot đó bằng món khác cùng loại, không lặp món vừa bị đổi trong phiên.
- Nút "Đổi cả mâm" → gợi ý lại toàn bộ.
- Nút "+ Thêm món" (khi chưa chốt) → bảng chọn món (tìm không dấu, lọc theo loại; không có món đã ẩn / đã trong mâm). Món thêm vào slot `THEM` ở cuối mâm, loại nào cũng được; Mẹ tự chọn nên không áp tránh lặp / trùng thịt cá. Món thêm khi "Đổi món" thì đổi sang món **cùng loại** với nó.
- Mọi thẻ món có "Đổi món" và "Bỏ món" (bỏ khỏi bữa này, có Hoàn tác, **không** ẩn món). Bỏ được cả 4 món chính → mâm Mẹ tự sửa có thể dưới 4 món (người dùng quyết định); gợi ý tự động vẫn luôn đủ 4. Ẩn món ("Nhà không ăn") chỉ còn ở tab Món nhà.
- Nút lớn "Chọn mâm này" → chốt (tạo / ghi đè `MealLog`) → sang F12, F25.

### F04 — Tránh lặp món

Loại món đã có trong `MealLog` trong `repeatAvoidDays` ngày gần nhất (tính cả bữa trưa lẫn tối). Nếu loại xong mà không còn món cho một slot thì nới dần (bỏ điều kiện này cho slot đó) chứ không để trống.

### F05 — Lọc nhanh

Chip "Món nhanh < 30 phút": chỉ lấy món có `cookTimeMin < 30`. Nới điều kiện như F04 nếu không đủ món.

### F06 + F20 — "Tủ lạnh có gì" (màn hình riêng, không bắt buộc)

- Lưới chip có hình theo nhóm (Thịt, Cá & hải sản, Trứng & đậu, Rau củ, Khác), có ô tìm kiếm không dấu.
- Mẹ chỉ chạm để bật/tắt **tên** nguyên liệu. **Không nhập lượng.**
- Nút "Xoá hết" (có Hoàn tác).
- Món có nguyên liệu chính đang có trong tủ lạnh được cộng điểm (xem mục 8) và có nhãn "Có sẵn đồ" trên thẻ món.
- Tủ lạnh trống thì app vẫn gợi ý bình thường.

### F07 — Kho món sẵn

Nạp 80 món từ `seed_data.json`, ưu tiên món miền Bắc. Mỗi món có loại, mùa, vùng miền, thời gian nấu, nguyên liệu chính.

### F08 — Mẹ thêm món riêng

Form: tên (bắt buộc), loại (4 nút lớn), ảnh (chụp hoặc chọn), nguyên liệu chính (chọn chip), mùa (Nóng / Lạnh / Quanh năm; mặc định Quanh năm), thời gian nấu (Nhanh < 30 phút / Vừa / Lâu). Vùng miền mặc định `BAC`. Sửa và xoá được (xoá có Hoàn tác).

### F09 — Yêu thích / Ẩn món

Trên thẻ món và trong Món của nhà: nút tim (yêu thích) và "Nhà không ăn món này" (ẩn). Món ẩn không bao giờ được gợi ý; có mục "Món đã ẩn" để bỏ ẩn.

- Tab Món nhà có 3 chip lọc kèm số món: **Tất cả** (không gồm món ẩn) · **Yêu thích** · **Đã ẩn**; ô tìm không dấu; danh sách nhóm theo loại (Mặn, Xào, Rau, Canh), tên A→Z.
- Bỏ yêu thích và ẩn món đều có Snackbar "Hoàn tác" 5 giây. Bấm vào món → màn hình Cách nấu (F10).
- Món yêu thích được cộng 10/100 điểm khi gợi ý (mục 8.2).

### F10 — Xem cách nấu

- Trên thẻ món (màn hình Hôm nay): bấm vào ảnh / tên món hoặc dòng "Xem cách nấu ›" → màn hình Cách nấu.
- Hiện: tên món, loại, thời gian nấu; "Nguyên liệu (cho 7 người)" dạng gạch đầu dòng; "Các bước nấu" đánh số to.
- Nút "Tìm video trên YouTube": mở trang tìm kiếm "cách nấu <tên món>" (không lưu sẵn link video cụ thể). Nếu Mẹ đã dán link thì nút là "Xem video cách nấu".
- "Sửa công thức" / "Thêm công thức" (món chưa có): 3 ô — nguyên liệu (mỗi dòng một thứ), các bước (mỗi dòng một bước, bắt buộc ≥ 1), link video (không bắt buộc). Số thứ tự / gạch đầu dòng Mẹ tự gõ được bỏ đi khi lưu.
- Có sẵn công thức cho 80 món trong `recipes.json`, kiểu miền Bắc, lượng cho 7 người.

**Chấp nhận:** bấm vào món trên mâm gợi ý → thấy nguyên liệu và các bước; sửa xong, cài APK mới đè lên vẫn giữ bản Mẹ đã sửa.

### F11 — Lịch sử

Danh sách theo tuần, mỗi ngày 2 dòng Trưa / Tối với tên các món đã chốt. Bấm vào một bữa → xem lại và "Nấu lại mâm này" (chép sang bữa đang chọn).

### F12 — Danh sách đi chợ

Khi chốt mâm: lấy nguyên liệu chính của các món, gộp trùng, **trừ đi nguyên liệu đang có trong tủ lạnh**, thêm vào danh sách đi chợ (không có số lượng ở bản 1). Mỗi dòng ghi món dùng nó: "Rau muống — cho Rau muống luộc".

### F13 — Đi chợ: thao tác

Tích đã mua (gạch ngang), thêm dòng gõ tay, "Xoá đã mua", "Gửi danh sách qua Zalo" (chia sẻ chữ, như F25).

### F17 — Nhắc nhở

| Giờ | Nội dung thông báo | Bấm vào mở |
| --- | --- | --- |
| 20:00 | "Mẹ ơi, trưa mai nấu gì nhỉ?" + tên 4 món gợi ý | Hôm nay → Trưa ngày mai |
| 13:00 | "Tối nay nấu gì nhỉ?" + gợi ý | Hôm nay → Tối hôm nay |
| 06:00 | Nếu trưa đã chốt: "Trưa nay: Cá kho riềng, Rau muống luộc, …". Nếu chưa: kèm gợi ý | Hôm nay → Trưa hôm nay |

Yêu cầu kỹ thuật:
- `setExactAndAllowWhileIdle(RTC_WAKEUP, …)` cho từng mốc; khi một báo thức chạy xong thì tự đặt lần kế tiếp (ngày hôm sau).
- `BootReceiver` nghe `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED` → đặt lại toàn bộ lịch.
- Notification channel "Nhắc nấu ăn", mức HIGH, hiện trên màn hình khoá; `PendingIntent` mở `MainActivity` với extra `date` + `mealType`.
- Trên API 28–29 không cần quyền `SCHEDULE_EXACT_ALARM` / `POST_NOTIFICATIONS`; vẫn viết code kiểm tra theo `SDK_INT` cho máy mới hơn.
- Màn hình Cài đặt có mục "Kiểm tra nhắc nhở" để gửi thử thông báo ngay.
- Hướng dẫn trong Cài đặt (có ảnh): bật **Tự khởi động** và tắt **tối ưu pin** cho app trên Vivo (Funtouch OS hay tắt app chạy nền).
  - Bản hiện tại: hướng dẫn bằng chữ (chưa có ảnh) + nút "Tắt tối ưu pin" (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, hiện "Đã tắt" khi xong) và nút "Mở Tự khởi động" (thử các màn của Vivo, không có thì mở trang thông tin app).
- Thông báo chưa chốt kèm mâm gợi ý; bấm vào thì app hiện **đúng mâm đó** (truyền `dishIds` trong `PendingIntent`), để Mẹ không thấy món khác với thông báo.
- Màn Cài đặt: bật/tắt nhắc, đổi giờ 3 mốc (đồng hồ 24 giờ), "Gửi thử thông báo" (gửi đúng loại nhắc của bữa sắp tới), cảnh báo khi thông báo bị tắt / không được đặt báo thức chính xác, số người ăn, số ngày tránh lặp món.

### F19 — Gợi ý theo thời tiết

- `WeatherWorker` (WorkManager, mỗi ngày, cần mạng) + gọi thêm khi mở app nếu chưa có dữ liệu hôm nay.
- API: Open-Meteo, toạ độ Đà Nẵng:
  `https://api.open-meteo.com/v1/forecast?latitude=16.0544&longitude=108.2022&daily=temperature_2m_max,precipitation_sum&timezone=Asia%2FHo_Chi_Minh&forecast_days=2`
- Phân loại theo ngày của bữa:
  - `maxTemp ≥ hotThresholdC` → `NONG`
  - `maxTemp ≤ coolThresholdC` hoặc `rain ≥ rainThresholdMm` → `MAT_LANH`
  - còn lại → `BINH_THUONG`
- Không có mạng: dùng `WeatherCache` gần nhất (≤ 3 ngày), quá hạn thì dùng mặc định theo tháng (tháng 4–8: NONG; tháng 11–2: MAT_LANH; còn lại: BINH_THUONG).
- Màn hình Hôm nay hiển thị dòng nhỏ: "Trời nóng 34°C — ưu tiên canh mát".

### F25 — Gửi thực đơn cho cả nhà qua Zalo

- Nút "Gửi cho cả nhà" trên mâm đã chốt.
- `MenuImageRenderer` vẽ ảnh PNG 1080×1350: tiêu đề "Trưa nay mẹ nấu" / "Tối nay mẹ nấu" + ngày, ảnh và tên từng món (không ghi số người — người dùng bỏ dòng "Cho 7 người").
- Lưu vào `cacheDir/share/menu_<date>_<meal>.png`, chia sẻ qua `FileProvider` (không cần quyền bộ nhớ).
- Intent: `ACTION_SEND`, `type = "image/png"`, `EXTRA_STREAM` (chỉ gửi ảnh, không kèm chữ; chỉ khi vẽ ảnh lỗi mới gửi `text/plain`), `FLAG_GRANT_READ_URI_PERMISSION`.
- Nếu đã cài Zalo (`com.zing.zalo`): `setPackage("com.zing.zalo")`; nếu không: `Intent.createChooser`.
- Mẫu chữ đi kèm:

```
Trưa nay mẹ nấu (T3, 10/11):
- Thịt ba chỉ rang cháy cạnh
- Rau muống luộc
- Canh cua rau đay mướp
- Đậu phụ sốt cà chua
```

---

## 8. Thuật toán gợi ý (`SuggestionEngine`)

Thuần Kotlin, không phụ thuộc Android, nhận vào toàn bộ dữ liệu cần thiết, trả về mâm. Có unit test.

### 8.1 Lọc

1. Bỏ món `isHidden`.
2. Bỏ món đã ăn trong `repeatAvoidDays` ngày (F04).
3. Áp bộ lọc nhanh nếu bật (F05).
4. Nếu slot nào hết món → nới lần lượt: bỏ bộ lọc nhanh → bỏ điều kiện tránh lặp.

### 8.2 Chấm điểm (thang 100)

| Tiêu chí | Điểm |
| --- | --- |
| Hợp thời tiết | 30 |
| Vùng miền | 20 |
| Lâu chưa ăn | 25 |
| Yêu thích | 10 |
| Có sẵn đồ trong tủ lạnh | 15 |

Chi tiết:

```
weatherScore =
  NONG:        món có NONG → 30 · QUANH_NAM → 20 · chỉ LANH → 0
  MAT_LANH:    món có LANH → 30 · QUANH_NAM → 20 · chỉ NONG → 0
  BINH_THUONG: QUANH_NAM → 25 · còn lại → 20

regionScore   = BAC → 20 · CHUNG → 10 · TRUNG/NAM → 0
recencyScore  = chưa từng ăn → 25 · ngược lại min(daysSinceLastEaten, 25)
favoriteScore = isFavorite ? 10 : 0
pantryScore   = 15 × (số nguyên liệu chính đang có / tổng số nguyên liệu chính)
```

Món yêu thích **chỉ được cộng điểm** (chiếm 10% thang điểm). Mọi điều kiện ở 8.1 và F02 vẫn áp dụng như món khác:
món yêu thích bị ẩn không bao giờ được gợi ý, vừa ăn trong `repeatAvoidDays` ngày thì bị loại (trừ khi phải nới điều kiện),
vẫn theo bộ lọc nhanh, đúng loại của slot, không trùng thịt/cá trong mâm.

### 8.3 Chọn món

- Với từng slot theo thứ tự `MAN → CANH → RAU → MON_4`: lấy top 6 theo điểm, chọn ngẫu nhiên có trọng số (xác suất ∝ điểm + 1).
- Sau khi chọn một món, loại khỏi các slot sau những món trùng nguyên liệu chính THIT / CA_HAI_SAN (F02).
- Cho phép truyền `Random(seed)` để test cho kết quả cố định.

### 8.4 Test tối thiểu

- Mâm luôn đủ 4 slot và đúng loại.
- Món ẩn không bao giờ xuất hiện.
- Món ăn hôm qua không xuất hiện khi `repeatAvoidDays = 4` (trừ khi phải nới điều kiện).
- Ngày NONG: món chỉ có mùa LANH không bao giờ đứng đầu.
- Không có 2 món trùng nguyên liệu thịt/cá chính.
- Tổng trọng số = 100, yêu thích = 10. Món yêu thích được gợi ý thường hơn, nhưng vẫn tuân thủ ẩn / tránh lặp / lọc nhanh / F02.

---

## 9. Màn hình và điều hướng

Thanh điều hướng dưới cùng, luôn hiện, chữ + icon to:

| Tab | Nội dung |
| --- | --- |
| Hôm nay | Bữa sắp tới, 4 thẻ món, đổi món, chốt, gửi Zalo; icon bánh răng → Cài đặt |
| Tủ lạnh | Lưới chip nguyên liệu (F06, F20) |
| Đi chợ | Checklist (F12, F13) |
| Món nhà | Danh sách theo loại, tìm không dấu, thêm / sửa / thích / ẩn (F07–F09) |
| Lịch sử | Theo tuần (F11) |

Luồng chính: Mở app → Hôm nay → (Đổi món)* → Chọn mâm này → Gửi cho cả nhà → Zalo.

Thẻ món: ảnh (tỉ lệ 4:3) · tên món (≥ 20sp, đậm) · nhãn loại (Mặn / Rau / Canh / Xào) · nhãn "Có sẵn đồ" nếu có · nút "Đổi" · nút tim.

---

## 10. Yêu cầu phi chức năng

| Nhóm | Yêu cầu |
| --- | --- |
| Hiệu năng | Mở app < 2 giây; đổi món < 300ms trên máy Vivo Y50 |
| Offline | Mọi chức năng chạy không cần mạng, trừ thời tiết (có cache) và gửi Zalo |
| Dung lượng | APK < 50MB; ảnh món WebP ~600px; ảnh Mẹ chụp resize ≤ 1080px |
| Ngôn ngữ | Chỉ tiếng Việt; tìm kiếm không dấu (`"ca kho"` → "Cá kho riềng") |
| Quyền | Camera (chỉ khi Mẹ chụp ảnh món), Internet. Không xin quyền vị trí hay bộ nhớ |
| Riêng tư | Không thu thập dữ liệu cá nhân; dữ liệu nằm trên máy |
| Sao lưu | Bật Android Auto Backup cho DB và DataStore (`allowBackup=true` + `dataExtractionRules`) |
| Ổn định | Crashlytics (tuỳ chọn); không crash khi mất mạng hoặc DB trống |
| Cập nhật | Cài APK mới đè lên không mất dữ liệu (Room migration có test) |

---

## 11. Thứ tự làm đề xuất

1. **Spike trên máy Vivo** (2–3 ngày): báo thức 3 mốc giờ + BootReceiver, gửi ảnh sang Zalo, gọi Open-Meteo. Ba phần này phụ thuộc thiết bị, cần thử trước.
2. Room + `SeedLoader` + `SuggestionEngine` (kèm unit test).
3. Màn hình Hôm nay (F01–F05) → chốt mâm → Lịch sử (F11).
4. Tủ lạnh (F06, F20) → Đi chợ (F12, F13).
5. Món nhà (F07–F09).
6. Nhắc nhở (F17) + Thời tiết (F19) + Gửi Zalo (F25).
7. Cài đặt, hướng dẫn Vivo, chỉnh UI chữ to → build APK → Mẹ dùng thử 2–3 tuần.

## 12. Definition of Done (bản 1)

- [ ] Mẹ tự mở app, chốt được mâm 4 món và gửi vào nhóm Zalo mà không cần hỏi con.
- [ ] Thông báo 06:00 / 13:00 / 20:00 đến đúng giờ 7 ngày liên tiếp trên máy Vivo, kể cả sau khi khởi động lại máy.
- [ ] Không gợi ý lặp món trong 4 ngày.
- [ ] Ngày nóng ưu tiên canh mát, ngày lạnh ưu tiên món nóng.
- [ ] Tắt mạng vẫn gợi ý và chốt mâm được.
- [ ] Unit test `SuggestionEngine` đều pass.
