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
import androidx.compose.ui.res.stringResource
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
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.basic.BasicComponent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.Box
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class DebugData(
    val logInfo: String,
    val song: String,
    val artist: String
)

class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            val view = LocalView.current

            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
                }
            }

            MiuixTheme(
                colors = if (isDarkTheme) darkColorScheme() else lightColorScheme()
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
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    var debugData by remember { mutableStateOf(loadDebugInfo(context)) }
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences("debug_info", Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "current_song" || key == "current_artist" || key == "last_debug_info") {
                debugData = loadDebugInfo(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            if (isTablet) {
                SmallTopAppBar(
                    title = stringResource(R.string.debug_title),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.content_desc_back)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = stringResource(R.string.debug_title),
                    largeTitle = stringResource(R.string.debug_title),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.content_desc_back)
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()) 
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            
            SmallTitle(text = stringResource(R.string.section_now_playing))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                BasicComponent(
                    title = debugData.song,
                    summary = debugData.artist
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                SmallTitle(text = stringResource(R.string.section_log_output))

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 28.dp)
                ) {
                    if (isRefreshing) {
                        InfiniteProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            text = stringResource(R.string.action_refresh),
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.primary,
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
            }

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                SelectionContainer {
                    Text(
                        text = debugData.logInfo,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

private fun loadDebugInfo(context: Context): DebugData {
    val prefs = context.getSharedPreferences("debug_info", Context.MODE_PRIVATE)
    val defaultNoData = context.getString(R.string.debug_no_data)
    val defaultSong = context.getString(R.string.info_no_song)
    val defaultArtist = context.getString(R.string.info_no_artist)
    val info = prefs.getString("last_debug_info", null) ?: defaultNoData
    val song = prefs.getString("current_song", defaultSong) ?: defaultSong
    val artist = prefs.getString("current_artist", defaultArtist) ?: defaultArtist

    return DebugData(logInfo = info, song = song, artist = artist)
}