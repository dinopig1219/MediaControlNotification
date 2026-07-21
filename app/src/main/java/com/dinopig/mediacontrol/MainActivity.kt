package com.dinopig.mediacontrol

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 200, 64, 64)
        }

        val infoText = TextView(this).apply {
            text = "第 1 步：授予「通知使用權」\n\n" +
                "授權後，本 App 會讀取 Spotify 當前的播放狀態，" +
                "並在通知欄裡重新生成一條帶完整按鈕（包含 repeat / like）的通知。\n\n" +
                "第 2 步：把本 App 加入省電策略白名單 / 允許自啟動，" +
                "否則 HyperOS 可能會在後台把它殺掉。"
            textSize = 15f
        }

        val button = Button(this).apply {
            text = "打開通知使用權設定"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        layout.addView(infoText)
        layout.addView(button)
        setContentView(layout)
    }
}
