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
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource

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

private val SLOT4_KEYS = listOf("LEFT1", "LEFT2", "RIGHT1", "RIGHT2")
private val SLOT4_LABELS = listOf("左 1", "左 2", "右 1", "右 2")
private val SIDE2_KEYS = listOf("LEFT", "RIGHT")
private val SIDE2_LABELS = listOf("左", "右")

@Composable
private fun OrderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("debug_info", Context.MODE_PRIVATE) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

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
            if (isTablet) {
                SmallTopAppBar(
                    title = "自定义通知动作排序",
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = "自定义通知动作排序",
                    largeTitle = "自定义通知动作排序",
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                        }
                    }
                )
            }
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
                )
        ) {
            SmallTitle(text = "展开状态排序")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewIcons = mutableListOf<Int>()
                    
                    if (custom1Index == 0) previewIcons.add(R.drawable.ic_custom_1)
                    if (custom2Index == 0) previewIcons.add(R.drawable.ic_custom_2)

                    if (custom1Index == 1) previewIcons.add(R.drawable.ic_custom_1)
                    if (custom2Index == 1) previewIcons.add(R.drawable.ic_custom_2)

                    previewIcons.add(R.drawable.ic_thin_previous)
                    previewIcons.add(R.drawable.ic_thin_pause) 
                    previewIcons.add(R.drawable.ic_thin_next)

                    if (custom1Index == 2) previewIcons.add(R.drawable.ic_custom_1)
                    if (custom2Index == 2) previewIcons.add(R.drawable.ic_custom_2)
                    
                    if (custom1Index == 3) previewIcons.add(R.drawable.ic_custom_1)
                    if (custom2Index == 3) previewIcons.add(R.drawable.ic_custom_2)

                    previewIcons.forEach { iconResId ->
                        val isCustom = iconResId == R.drawable.ic_custom_1 || iconResId == R.drawable.ic_custom_2
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MiuixTheme.colorScheme.surfaceVariant, 
                                    shape = RoundedCornerShape(50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = iconResId),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp), 
                                colorFilter = ColorFilter.tint(
                                    if (isCustom) 
                                        MiuixTheme.colorScheme.primary 
                                    else 
                                        MiuixTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
            
            SmallTitle(text = "展开通知")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                Column {
                    OverlayDropdownPreference(
                        title = "自定义动作 1 位置",
                        collapseOnSelection = true,
                        entries = listOf(
                            DropdownEntry(
                                items = listOf(0, 1).map { index ->
                                    DropdownItem(
                                        text = SLOT4_LABELS[index],
                                        selected = custom1Index == index,
                                        onClick = {
                                            val oldIndex = custom1Index
                                            custom1Index = index
                                            prefs.edit().putString("expanded_custom1_slot", SLOT4_KEYS[index]).apply()
                                            
                                            if (custom2Index == index) {
                                                custom2Index = oldIndex
                                                prefs.edit().putString("expanded_custom2_slot", SLOT4_KEYS[oldIndex]).apply()
                                            }
                                        }
                                    )
                                }
                            ),
                            DropdownEntry(
                                items = listOf(2, 3).map { index ->
                                    DropdownItem(
                                        text = SLOT4_LABELS[index],
                                        selected = custom1Index == index,
                                        onClick = {
                                            val oldIndex = custom1Index
                                            custom1Index = index
                                            prefs.edit().putString("expanded_custom1_slot", SLOT4_KEYS[index]).apply()
                                            
                                            if (custom2Index == index) {
                                                custom2Index = oldIndex
                                                prefs.edit().putString("expanded_custom2_slot", SLOT4_KEYS[oldIndex]).apply()
                                            }
                                        }
                                    )
                                }
                            )
                        )
                    )
                    
                    OverlayDropdownPreference(
                        title = "自定义动作 2 位置",
                        collapseOnSelection = true,
                        entries = listOf(
                            DropdownEntry(
                                items = listOf(0, 1).map { index ->
                                    DropdownItem(
                                        text = SLOT4_LABELS[index],
                                        selected = custom2Index == index,
                                        onClick = {
                                            val oldIndex = custom2Index
                                            custom2Index = index
                                            prefs.edit().putString("expanded_custom2_slot", SLOT4_KEYS[index]).apply()
                                            
                                            if (custom1Index == index) {
                                                custom1Index = oldIndex
                                                prefs.edit().putString("expanded_custom1_slot", SLOT4_KEYS[oldIndex]).apply()
                                            }
                                        }
                                    )
                                }
                            ),
                            DropdownEntry(
                                items = listOf(2, 3).map { index ->
                                    DropdownItem(
                                        text = SLOT4_LABELS[index],
                                        selected = custom2Index == index,
                                        onClick = {
                                            val oldIndex = custom2Index
                                            custom2Index = index
                                            prefs.edit().putString("expanded_custom2_slot", SLOT4_KEYS[index]).apply()
                                            
                                            if (custom1Index == index) {
                                                custom1Index = oldIndex
                                                prefs.edit().putString("expanded_custom1_slot", SLOT4_KEYS[oldIndex]).apply()
                                            }
                                        }
                                    )
                                }
                            )
                        )
                    )
                }
            }

            SmallTitle(text = "收起状态排序")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val compactPreviewIcons = mutableListOf<Int>()

                    if (compactModeIndex == 0) {
                        compactPreviewIcons.add(R.drawable.ic_thin_previous)
                        compactPreviewIcons.add(R.drawable.ic_thin_play)
                        compactPreviewIcons.add(R.drawable.ic_thin_next)
                    } else {
                        val itemsWithSide = listOf(
                            R.drawable.ic_custom_1 to compactSide1Index,
                            R.drawable.ic_custom_2 to compactSide2Index
                        ).sortedBy { it.second }

                        itemsWithSide.forEach { (icon, _) ->
                            compactPreviewIcons.add(icon)
                        }
                    }

                    compactPreviewIcons.forEach { iconResId ->
                        val isCustom = iconResId == R.drawable.ic_custom_1 || iconResId == R.drawable.ic_custom_2
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MiuixTheme.colorScheme.surfaceVariant, 
                                    shape = RoundedCornerShape(50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = iconResId),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp), 
                                colorFilter = ColorFilter.tint(
                                    if (isCustom) 
                                        MiuixTheme.colorScheme.primary 
                                    else 
                                        MiuixTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            SmallTitle(text = "收起通知")
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                Column {
                    OverlayDropdownPreference(
                        title = "显示动作",
                        items = listOf("音乐控制动作", "自定义动作"),
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
                            onSelectedIndexChange = { newIndex ->
                                val oldIndex = compactSide1Index
                                compactSide1Index = newIndex
                                prefs.edit().putString("compact_custom1_side", SIDE2_KEYS[newIndex]).apply()
                                if (compactSide2Index == newIndex) {
                                    compactSide2Index = oldIndex
                                    prefs.edit().putString("compact_custom2_side", SIDE2_KEYS[oldIndex]).apply()
                                }
                            }
                        )
                        OverlayDropdownPreference(
                            title = "自定义动作 2 位置",
                            items = SIDE2_LABELS,
                            selectedIndex = compactSide2Index,
                            onSelectedIndexChange = { newIndex ->
                                val oldIndex = compactSide2Index
                                compactSide2Index = newIndex
                                prefs.edit().putString("compact_custom2_side", SIDE2_KEYS[newIndex]).apply()
                                if (compactSide1Index == newIndex) {
                                    compactSide1Index = oldIndex
                                    prefs.edit().putString("compact_custom1_side", SIDE2_KEYS[oldIndex]).apply()
                                }
                            }
                        )
                    }
                }
            }
            Text(
                text = "更改将会在音乐状态有变动时生效。",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }
}