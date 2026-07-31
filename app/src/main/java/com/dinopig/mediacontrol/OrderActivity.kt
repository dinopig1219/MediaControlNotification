package com.dinopig.mediacontrol

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

class OrderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiuixTheme(
                colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                Scaffold {
                    OrderScreen(onBack = { finish() })
                }
            }
        }
    }
}

private val SLOT4_KEYS = listOf("LEFT2", "LEFT1", "RIGHT1", "RIGHT2")
private val SLOT4_LABELS = listOf("左2", "左1", "右1", "右2")
private val SIDE2_KEYS = listOf("LEFT", "RIGHT")
private val SIDE2_LABELS = listOf("左", "右")

@Composable
private fun OrderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("debug_info", Context.MODE_PRIVATE) }

    var custom1Index by remember {
        mutableIntStateOf(SLOT4_KEYS.indexOf(prefs.getString("expanded_custom1_slot", "RIGHT1")).coerceAtLeast(0))
    }
    var custom2Index by remember {
        mutableIntStateOf(SLOT4_KEYS.indexOf(prefs.getString("expanded_custom2_slot", "RIGHT2")).coerceAtLeast(0))
    }
    var compactModeIndex by remember {
        mutableIntStateOf(if (prefs.getString("compact_mode", "STANDARD") == "CUSTOM") 1 else 0)
    }
    var compactSide1Index by remember {
        mutableIntStateOf(SIDE2_KEYS.indexOf(prefs.getString("compact_custom1_side", "LEFT")).coerceAtLeast(0))
    }
    var compactSide2Index by remember {
        mutableIntStateOf(SIDE2_KEYS.indexOf(prefs.getString("compact_custom2_side", "RIGHT")).coerceAtLeast(0))
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = "按钮排序",
                largeTitle = "按钮排序",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SmallTitle(text = "展开通知")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                Column {
                    OverlayDropdownPreference(
                        title = "自定义动作 1 位置",
                        items = SLOT4_LABELS,
                        selectedIndex = custom1Index,
                        onSelectedIndexChange = {
                            custom1Index = it
                            prefs.edit().putString("expanded_custom1_slot", SLOT4_KEYS[it]).apply()
                        }
                    )
                    OverlayDropdownPreference(
                        title = "自定义动作 2 位置",
                        items = SLOT4_LABELS,
                        selectedIndex = custom2Index,
                        onSelectedIndexChange = {
                            custom2Index = it
                            prefs.edit().putString("expanded_custom2_slot", SLOT4_KEYS[it]).apply()
                        }
                    )
                }
            }

            SmallTitle(text = "收起通知")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                Column {
                    OverlayDropdownPreference(
                        title = "显示内容",
                        items = listOf("播放器三宝", "自定义按钮"),
                        selectedIndex = compactModeIndex,
                        onSelectedIndexChange = {
                            compactModeIndex = it
                            prefs.edit().putString("compact_mode", if (it == 1) "CUSTOM" else "STANDARD").apply()
                        }
                    )

                    if (compactModeIndex == 1) {
                        OverlayDropdownPreference(
                            title = "自定义动作 1 位置",
                            items = SIDE2_LABELS,
                            selectedIndex = compactSide1Index,
                            onSelectedIndexChange = {
                                compactSide1Index = it
                                prefs.edit().putString("compact_custom1_side", SIDE2_KEYS[it]).apply()
                            }
                        )
                        OverlayDropdownPreference(
                            title = "自定义动作 2 位置",
                            items = SIDE2_LABELS,
                            selectedIndex = compactSide2Index,
                            onSelectedIndexChange = {
                                compactSide2Index = it
                                prefs.edit().putString("compact_custom2_side", SIDE2_KEYS[it]).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
