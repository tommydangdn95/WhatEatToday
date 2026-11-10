package com.family.homnayangi.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * F25 — gửi thực đơn sang Zalo. Bản này gửi chữ; ảnh thực đơn (MenuImageRenderer) sẽ thêm sau.
 * Có Zalo → mở thẳng Zalo; không có → hiện bảng chọn app của Android.
 */
object ZaloSharer {
    const val ZALO_PACKAGE = "com.zing.zalo"

    fun isZaloInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(ZALO_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun share(context: Context, text: String, imageUri: Uri? = null, chooserTitle: String = "Gửi cho cả nhà") {
        val send = Intent(Intent.ACTION_SEND).apply {
            if (imageUri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_TEXT, text)
        }
        if (isZaloInstalled(context)) {
            try {
                context.startActivity(Intent(send).setPackage(ZALO_PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
                // Zalo không nhận kiểu dữ liệu này → rơi xuống bảng chọn.
            }
        }
        context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
