package com.family.homnayangi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.graphics.Color
import com.family.homnayangi.ui.AppRoot
import com.family.homnayangi.ui.theme.HomNayAnGiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App chỉ dùng nền sáng → icon thanh trạng thái / điều hướng luôn màu tối cho dễ nhìn.
        val paper = Color.parseColor("#FFFBF5")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(paper, paper),
            navigationBarStyle = SystemBarStyle.light(paper, paper),
        )
        setContent {
            HomNayAnGiTheme {
                AppRoot()
            }
        }
    }
}
