package com.dinopig.mediacontrol

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.basic.BasicComponent

data class DebugData(
    val logInfo: String,
    val song: String,
    val artist: String
)

class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiuixTheme(
                colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                DebugScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun DebugScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    
    var debugData by remember { mutableStateOf(loadDebugInfo(context)) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "调试信息",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding() - 4.dp) 
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            
            SmallTitle(text = "当前播放状态")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                BasicComponent(
                    title = debugData.song,
                    summary = debugData.artist
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallTitle(
                    text = "信息输出",
                    modifier = Modifier.padding(bottom = 0.dp)
                )

                if (isRefreshing) {
                    InfiniteProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.CenterVertically)
                    )
                } else {
                    Text(
                        text = "刷新",
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            coroutineScope.launch {
                                isRefreshing = true
                                delay(1500)
                                debugData = loadDebugInfo(context)
                                isRefreshing = false
                            }
                        }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                SelectionContainer {
                    Text(
                        text = debugData.logInfo,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

private fun loadDebugInfo(context: Context): DebugData {
    val prefs = context.getSharedPreferences("debug_info", Context.MODE_PRIVATE)
    val info = prefs.getString("last_debug_info", null) ?: "还没有数据。请先播放 Spotify，确保已授权通知使用权。"
    val song = prefs.getString("current_song", "未知") ?: "未知"
    val artist = prefs.getString("current_artist", "未知") ?: "未知"
    
    return DebugData(logInfo = info, song = song, artist = artist)
}
