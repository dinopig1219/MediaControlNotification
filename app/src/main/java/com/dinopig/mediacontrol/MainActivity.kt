package com.dinopig.mediacontrol

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dinopig.mediacontrol.effect.BgEffectBackground
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiuixTheme(
                colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                RootScreen()
            }
        }
    }
}

private fun isNotificationPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

private fun openAppDetailsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

private fun openBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

@Composable
private fun RootScreen() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    icon = MiuixIcons.Home,
                    label = "主页"
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    icon = MiuixIcons.Demibold.Info,
                    label = "关于"
                )
            }
        }
    ) { outerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = outerPadding.calculateBottomPadding())
        ) { page ->
            if (page == 0) HomePage() else AboutPage()
        }
    }
}

@Composable
private fun HomePage() {
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        topBar = { TopAppBar(title = "媒体控制通知", scrollBehavior = scrollBehavior) }
    ) { padding ->
        HomeScreen(scrollBehavior, padding)
    }
}

@Composable
private fun AboutPage() {
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    
    val scrollProgress by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) return@derivedStateOf 1f
            val spacer = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "logoSpacer" }
            if (spacer != null && spacer.size > 0) {
                (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface) 
    ) {
        BgEffectBackground(
            dynamicBackground = true,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 1f - scrollProgress
                }
        ) { }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = "关于",
                    largeTitle = "",
                    scrollBehavior = scrollBehavior,
                    color = Color.Transparent, 
                    titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = scrollProgress)
                )
            }
        ) { padding ->
            AboutScreen(
                scrollBehavior = scrollBehavior, 
                padding = padding,
                lazyListState = lazyListState,
                scrollProgress = scrollProgress
            )
        }
    }
}

@Composable
private fun HomeScreen(scrollBehavior: ScrollBehavior, padding: PaddingValues) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("debug_info", Context.MODE_PRIVATE) }

    var notificationGranted by remember { mutableStateOf(isNotificationPermissionGranted(context)) }
    var listenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var notificationAskedBefore by remember {
        mutableStateOf(prefs.getBoolean("notification_permission_asked", false))
    }
    var masterEnabled by remember {
        mutableStateOf(
            prefs.getBoolean("master_enabled", false) &&
                isNotificationPermissionGranted(context) &&
                isNotificationListenerEnabled(context)
        )
    }
    var debugNotificationsOn by remember {
        mutableStateOf(prefs.getBoolean("debug_notifications_enabled", false))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationGranted = granted }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationGranted = isNotificationPermissionGranted(context)
                listenerEnabled = isNotificationListenerEnabled(context)
                if ((!notificationGranted || !listenerEnabled) && masterEnabled) {
                    masterEnabled = false
                    prefs.edit().putBoolean("master_enabled", false).apply()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState()),
    ) {
        SmallTitle(text = "开关")
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            SwitchPreference(
                title = "启用服务",
                summary = when {
                    !notificationGranted || !listenerEnabled -> "需要先同时开启通知权限和通知使用权才能启用"
                    masterEnabled -> "正在运行，Spotify 播放音乐时会生成通知"
                    else -> "服务已关闭"
                },
                checked = masterEnabled,
                onCheckedChange = { checked ->
                    if (checked && (!notificationGranted || !listenerEnabled)) {
                        when {
                            !notificationGranted -> {
                                notificationAskedBefore = true
                                prefs.edit().putBoolean("notification_permission_asked", true).apply()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            !listenerEnabled -> {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        }
                    } else {
                        masterEnabled = checked
                        prefs.edit().putBoolean("master_enabled", checked).apply()
                    }
                }
            )
        }

        SmallTitle(text = "通知")

        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            Column {
                SwitchPreference(
                    title = "通知权限",
                    summary = if (notificationGranted) "已授权" else "未授权，用于本 App 生成通知",
                    checked = notificationGranted,
                    onCheckedChange = {
                        if (!notificationAskedBefore) {
                            notificationAskedBefore = true
                            prefs.edit().putBoolean("notification_permission_asked", true).apply()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationGranted = true
                            }
                        } else {
                            openAppDetailsSettings(context)
                        }
                    }
                )

                SwitchPreference(
                    title = "通知使用权",
                    summary = if (listenerEnabled) "已授权" else "未授权，用于读取 Spotify 播放状态",
                    checked = listenerEnabled,
                    onCheckedChange = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )

                ArrowPreference(
                    title = "省电策略（可选）",
                    summary = "允许后台运行以保持服务更新，避免服务被系统杀掉",
                    onClick = { openBatteryOptimizationSettings(context) }
                )
            }
        }

        SmallTitle(text = "日志")
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            Column {
                SwitchPreference(
                    title = "显示调试通知",
                    summary = "开启后本应用通知会多一条调试信息",
                    checked = debugNotificationsOn,
                    onCheckedChange = { checked ->
                        debugNotificationsOn = checked
                        prefs.edit().putBoolean("debug_notifications_enabled", checked).apply()
                    }
                )

                ArrowPreference(
                    title = "查看调试信息",
                    onClick = { context.startActivity(Intent(context, DebugActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
private fun AboutScreen(
    scrollBehavior: ScrollBehavior, 
    padding: PaddingValues,
    lazyListState: LazyListState,
    scrollProgress: Float
) {
    val context = LocalContext.current
@Composable
private fun AboutScreen(
    scrollBehavior: ScrollBehavior, 
    padding: PaddingValues,
    lazyListState: LazyListState,
    scrollProgress: Float
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionName = packageInfo.versionName ?: "未知"
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)

    val density = LocalDensity.current
    var logoHeightDp by remember { mutableStateOf(0.dp) }

    // ✨ 完美對齊 InstallerX 的 Logo 區塊留白
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding() + 64.dp) 
            .onSizeChanged { size -> 
                with(density) { logoHeightDp = size.height.toDp() }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp) 
                .graphicsLayer {
                    val iconProgress = ((scrollProgress - 0.35f) / 0.15f).coerceIn(0f, 1f)
                    alpha = 1f - iconProgress
                    scaleX = 1f - (iconProgress * 0.05f)
                    scaleY = 1f - (iconProgress * 0.05f)
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp) 
                    .clip(RoundedCornerShape(24.dp))
            )
        }

        Text(
            text = "媒体控制通知",
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp, // 採用 InstallerX 具備份量感的大字體
            modifier = Modifier
                .padding(top = 10.dp, bottom = 4.dp)
                .graphicsLayer {
                    val projectNameProgress = ((scrollProgress - 0.20f) / 0.15f).coerceIn(0f, 1f)
                    alpha = 1f - projectNameProgress
                    scaleX = 1f - (projectNameProgress * 0.05f)
                    scaleY = 1f - (projectNameProgress * 0.05f)
                }
        )
        Text(
            text = "v$versionName ($versionCode)",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 14.sp,
            modifier = Modifier.graphicsLayer {
                val versionCodeProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
                alpha = 1f - versionCodeProgress
                scaleX = 1f - (versionCodeProgress * 0.05f)
                scaleY = 1f - (versionCodeProgress * 0.05f)
            }
        )
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 16.dp)
    ) {
        
        // ✨ 關鍵調整：採用 InstallerX 的高度公式，讓卡片往上拉高，身型跟右邊完全一致
        item(key = "logoSpacer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(logoHeightDp + 130.dp) 
            )
        }

        item(key = "about") {
            Box {
                Spacer(Modifier.fillParentMaxHeight()) 
                Column(modifier = Modifier.fillMaxWidth()) {
                    SmallTitle(text = "关于")
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        Text(
                            text = "用一条独立的通知，把被 HyperOS 在媒体通知卡片隐藏掉的 Spotify 播放控件（智能随机播放 / 随机播放 / 收藏等）重新显示出来，点击后直接转发给 Spotify 本体。",
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    SmallTitle(text = "链接")
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        ArrowPreference(
                            title = "查看源码",
                            summary = "项目主页与更新日志",
                            endActions = {
                                Text(
                                    text = "GitHub",
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                )
                            },
                            onClick = {
                                uriHandler.openUri("https://github.com/dinopig1219/MediaControlNotification")
                            }
                        )
                    }
                }
            }
        }
    }
}
