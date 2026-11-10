# CLAUDE.md — App "Hôm nay ăn gì"

App Android nội bộ gia đình giúp Mẹ chọn mâm cơm 4 món rồi gửi thực đơn vào nhóm Zalo.
**Đặc tả đầy đủ nằm trong [SPEC.md](SPEC.md) — đọc trước khi làm tính năng mới.** File này chỉ ghi những gì SPEC không có: cách build, quyết định kỹ thuật, tiến độ.

Trao đổi với người dùng bằng **tiếng Việt**. Chữ trên giao diện và chú thích trong code cũng viết tiếng Việt.

## Build & test

```bash
./gradlew :app:assembleDebug          # build APK debug
./gradlew :app:testDebugUnitTest      # unit test (JVM), phải luôn pass
```

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Kết quả test: `app/build/test-results/testDebugUnitTest/*.xml`
- Test đọc thẳng `src/main/assets/seed_data.json` (dữ liệu thật), không cần thiết bị.

## Toolchain — những điểm dễ vướng

| Mục | Giá trị | Ghi chú |
| --- | --- | --- |
| AGP | 9.2.1 | Kotlin tích hợp sẵn; **không** apply plugin `kotlin-android` trong `app/` |
| Kotlin | 2.4.21 | Nâng bằng cách khai báo `kotlin-android` **`apply false`** ở `build.gradle.kts` gốc |
| compileSdk | 37 | Bắt buộc: Compose 1.12, core 1.19, navigation 2.10… đòi API 37. Không ảnh hưởng máy chạy |
| minSdk / targetSdk | 28 / 36 | Máy của Mẹ: Vivo Y50, Android 10 (API 29) |
| KSP | 2.3.13 | Cho Room + Hilt |
| Java target | 17 | |

- **R8 đang tắt** (`optimization { enable = false }`). Bật lên cần thêm cờ `android.r8.gradual.support` trong `gradle.properties` — làm cùng lúc cấu hình ký APK release.
- Phiên bản thư viện quản lý tập trung ở `gradle/libs.versions.toml`.
- Không dùng `material-icons-extended` (APK quá nặng khi chưa bật R8). Chỉ dùng `material-icons-core`; icon thiếu thì vẽ vector trong `res/drawable/` (vd `ic_fridge.xml`).

## Kiến trúc

Package: `com.family.homnayangi` (applicationId — **không được đổi** sau khi đã cài lên máy Mẹ, sẽ mất dữ liệu).

```
domain/      Kotlin thuần, không import Android → unit test được
  SuggestionEngine   thuật toán gợi ý (SPEC §8): suggest / swap / arrange / rank
  MealSlotResolver   bữa mặc định theo giờ (F01)
  WeatherClassifier  phân loại thời tiết + mặc định theo tháng (F19)
  MenuText           mọi câu chữ: tiêu đề, ngày, mẫu tin nhắn Zalo
  TextNormalizer     bỏ dấu để tìm kiếm không dấu
  RecipeText         tách dòng khi sửa công thức, link tìm kiếm YouTube, nhãn thời gian nấu
  model/             enum + DishInfo, MealRecord, MealTarget, MealSlot
data/
  db/        Room: Entities.kt, Daos.kt, Converters.kt, AppDatabase.kt
  seed/      SeedParser, RecipeParser (thuần, test được) + SeedLoader (Android)
  settings/  SettingsRepository (DataStore, SPEC §5.3)
  repo/      DishRepository, MealRepository, PantryRepository
di/          AppModule (Hilt)
share/       ZaloSharer
ui/          AppRoot (thanh điều hướng 5 tab), MealSelection, theme/, today/, history/, recipe/, dishes/
```

- MVVM: `@HiltViewModel` + `StateFlow`; màn hình dùng `hiltViewModel()` từ `androidx.hilt.lifecycle.viewmodel.compose`.
- `MealSelection` (@Singleton) giữ **bữa đang chọn** dùng chung giữa các tab (Lịch sử "Nấu lại" chép mâm sang; thông báo nhắc giờ sau này sẽ đặt bữa ở đây).
- `TodayViewModel` cache mâm gợi ý theo từng `MealTarget`; chốt mâm thì bỏ cache các bữa khác có món trùng (F04).

## Quy ước bắt buộc

- **UX (SPEC §2):** chữ nội dung ≥ 18sp, tiêu đề ≥ 24sp (đã đặt sẵn trong `ui/theme/Type.kt` — dùng `MaterialTheme.typography`, đừng tự đặt `fontSize` nhỏ hơn). Vùng bấm ≥ 56dp. Icon có `contentDescription` tiếng Việt, trừ khi đã có nhãn chữ bên cạnh.
- **Xoá/ẩn phải có "Hoàn tác" 5 giây:** dùng `SnackbarDuration.Indefinite` bọc trong `withTimeoutOrNull(5_000)` (xem `TodayScreen.kt`).
- **Chỉ giao diện sáng**, màu trong `ui/theme/Theme.kt` đã kiểm tương phản WCAG AA. Thêm màu mới thì kiểm lại.
- Kiểm tra chữ trên nút **không bị xuống dòng** ở màn hình 360dp (đã gặp với "Đổi món khác", "Chọn mâm này").
- **Room:** không dùng `fallbackToDestructiveMigration`. Đổi schema → tăng `version`, thêm `AutoMigration` (hoặc `Migration` viết tay) + test trong `androidTest/MigrationTest.kt`. Schema export ở `app/schemas/` (commit vào git). DB hiện ở **version 2** (v2 thêm bảng `Recipe`).
- **Seed:** `SeedLoader` chỉ insert IGNORE — không bao giờ ghi đè món Mẹ đã sửa/thích/ẩn. Thêm món mới vào `seed_data.json` thì tăng `version`.
- **Công thức** (`assets/recipes.json`): version riêng (`recipesVersion`). Thêm món mới vào seed thì phải thêm công thức và tăng version của **cả hai** file — `RecipeFileTest` sẽ báo lỗi nếu có món thiếu công thức. Bước nấu không ghi số thứ tự ở đầu.
- **MealLog:** mỗi (date, mealType) một bản ghi; chốt lại dùng `MealLogDao.upsertByMeal` (không dùng `REPLACE`/`@Upsert` vì unique index ngoài khoá chính).
- **Quyền:** không khai báo `CAMERA` (chụp ảnh qua `TakePicture` không cần quyền; khai báo mà chưa cấp sẽ crash). Không xin vị trí, bộ nhớ.
- Code dùng API > 28 phải bọc `if (Build.VERSION.SDK_INT >= …)`.
- `SuggestionEngine` thay đổi gì cũng phải thêm/cập nhật test trong `SuggestionEngineTest`.
- **Trọng số điểm** nằm ở hằng số trong `SuggestionEngine.Companion` (thời tiết 30, vùng 20, lâu chưa ăn 25, **yêu thích 10** theo yêu cầu người dùng, tủ lạnh 15; tổng phải = 100, có test kiểm). Món yêu thích chỉ được cộng điểm, không bao giờ được bỏ qua điều kiện lọc (ẩn, tránh lặp, lọc nhanh, đúng slot, trùng thịt/cá).

## Thử trên máy thật

Máy test đang dùng: **Samsung SM-G885F, Android 10 (API 29)** — cùng phiên bản Android với máy Vivo của Mẹ.

```bash
ADB="/c/Users/TOMMY/AppData/Local/Android/Sdk/platform-tools/adb.exe"
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk
MSYS_NO_PATHCONV=1 "$ADB" shell am start -W -n com.family.homnayangi/.MainActivity
"$ADB" exec-out screencap -p > <scratchpad>/shot.png
```

- Trong Git Bash phải đặt `MSYS_NO_PATHCONV=1`, nếu không `/sdcard/...` bị đổi thành đường dẫn Windows.
- Tìm nút để bấm: `uiautomator dump /sdcard/ui.xml` rồi đọc `bounds` theo `text`/`content-desc`. Python in tiếng Việt cần `python -X utf8` (console mặc định cp1252).
- **Chạy test migration mà không gỡ app khỏi máy:** `./gradlew connectedDebugAndroidTest` sẽ gỡ app sau khi chạy (mất dữ liệu thử). Thay vào đó build `:app:assembleDebugAndroidTest`, cài `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` rồi chạy
  `adb shell am instrument -w -e class com.family.homnayangi.MigrationTest com.family.homnayangi.test/androidx.test.runner.AndroidJUnitRunner`.
- Logcat lọc theo `SeedLoader` để xem dữ liệu ban đầu đã nạp chưa. Crash của `com.google.android.gms` trên máy này là của Google Play Services, không phải app.
- Máy test **chưa cài Zalo** → nút "Gửi cho cả nhà" mở bảng chọn app (đúng SPEC).

## Tiến độ (cập nhật 10/11/2026, sau F10 + yêu thích)

Đã xong (đều đã chạy thử trên máy):
- Khung project, Room, SeedLoader, SuggestionEngine + 28 unit test
- **Hôm nay** (F01–F05, tim/ẩn của F09 trên thẻ): chọn bữa/ngày, đổi món, đổi cả mâm, lọc món nhanh, chốt, chọn lại, gửi Zalo **dạng chữ**
- **Lịch sử** (F11): theo tuần, "Nấu lại mâm này"
- **Món nhà** (F07, F09): lọc Tất cả / Yêu thích / Đã ẩn, tìm không dấu, tim, ẩn / bỏ ẩn có Hoàn tác, bấm món → Cách nấu. Món yêu thích = 10/100 điểm khi gợi ý
- **Cách nấu** (F10): công thức 80 món (`recipes.json`), xem / sửa / thêm, nút tìm video YouTube; DB v2 + test migration chạy trên máy

Chưa làm (theo thứ tự SPEC §11):
1. Tủ lạnh (F06, F20) → Đi chợ (F12, F13; chốt mâm tự thêm nguyên liệu, trừ đồ có trong tủ lạnh)
2. Thêm / sửa / xoá món riêng của Mẹ (F08) — nút "Thêm món" trong tab Món nhà
3. Nhắc nhở (F17), thời tiết Open-Meteo (F19 — hiện đang dùng mặc định theo tháng), ảnh thực đơn PNG cho Zalo (F25)
4. Cài đặt (nút bánh răng đang mở trang trống), hướng dẫn Vivo, ký APK release + bật R8

Vấn đề đang mở:
- Mở app lần đầu ~4 giây trên bản debug (SPEC yêu cầu < 2 giây) → đo lại trên bản release có R8.
- Chưa có ảnh món; đang dùng ô màu theo loại món (`CategoryBadge`). Thẻ món khá cao, một màn hình chỉ thấy ~2 thẻ — xem lại bố cục khi thêm ảnh.
- `canh_chua_ca` trong seed có mùa `["NONG","LANH"]` (được 30 điểm cả ngày nóng lẫn lạnh); đã đề xuất đổi thành `QUANH_NAM`, người dùng chưa quyết.
- Máy test còn dữ liệu thử: bữa tối 10/11 đã chốt, "Cá kho riềng" và "Cá chép om dưa" đang yêu thích, công thức "Giả cầy" đã bị sửa thử (thêm dòng "La lot thai chi"). Đã bỏ ẩn hết món.
- Khi thử bằng script: bấm "Hoàn tác" phải ngay sau khi ẩn (thanh chỉ hiện 5 giây); lần mở app đầu tiên ngay sau `adb install` đôi khi bị Android bỏ qua → kiểm tra `mResumedActivity` rồi mở lại.
- Ô sửa "Các bước nấu" là một ô nhiều dòng: bước dài tự xuống dòng trông giống 2 bước. Nếu Mẹ thấy khó dùng thì đổi sang mỗi bước một ô riêng.
- Màn hình Lịch sử chưa có lối vào Cách nấu (đã có ở thẻ món tab Hôm nay và tab Món nhà).
