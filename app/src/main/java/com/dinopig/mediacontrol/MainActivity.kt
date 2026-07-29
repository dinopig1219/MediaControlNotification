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
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    
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
                RootScreen()
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
        val prefs = getSharedPreferences("debug_info", Context.MODE_PRIVATE)
        if (prefs.getBoolean("hide_recents_enabled", false)) {
            finishAndRemoveTask()
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
    var logoSpacerHeightPx by remember { mutableStateOf(0) }

    val scrollProgress by remember {
        derivedStateOf {
            if (logoSpacerHeightPx <= 0) return@derivedStateOf 0f
            val index = lazyListState.firstVisibleItemIndex
            val offset = lazyListState.firstVisibleItemScrollOffset
            if (index > 0) 1f else (offset.toFloat() / logoSpacerHeightPx).coerceIn(0f, 1f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BgEffectBackground(
            dynamicBackground = true,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - scrollProgress }
        ) { }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = "关于",
                    largeTitle = "",
                    scrollBehavior = scrollBehavior,
                    color = if (scrollProgress > 0.99f) MiuixTheme.colorScheme.surface else Color.Transparent,
                    titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = scrollProgress)
                )
            }
        ) { padding ->
            AboutScreen(
                scrollBehavior = scrollBehavior,
                padding = padding,
                lazyListState = lazyListState,
                scrollProgress = scrollProgress,
                onLogoSpacerHeightChanged = { logoSpacerHeightPx = it }
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

    var hideRecentsEnabled by remember {
        mutableStateOf(prefs.getBoolean("hide_recents_enabled", false))
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
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState()),
    ) {
        SmallTitle(text = "总开关")
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

        SmallTitle(text = "权限设置")

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

                SwitchPreference(
                    title = "隐藏后台窗口",
                    summary = "切换到后台时自动从最近任务中隐藏",
                    checked = hideRecentsEnabled,
                    onCheckedChange = { checked ->
                        hideRecentsEnabled = checked
                        prefs.edit().putBoolean("hide_recents_enabled", checked).apply()
                    }
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
    scrollProgress: Float,
    onLogoSpacerHeightChanged: (Int) -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding() + 40.dp)
            .onSizeChanged { size ->
                with(density) { logoHeightDp = size.height.toDp() }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    val iconProgress = ((scrollProgress - 0.35f) / 0.15f).coerceIn(0f, 1f)
                    
                    clip = true 
                    shape = RoundedCornerShape(24.dp)
                    
                    alpha = 1f - iconProgress
                    scaleX = 1f - (iconProgress * 0.05f)
                    scaleY = 1f - (iconProgress * 0.05f)
                }
                .background(Color.White)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(74.dp)
            )
        }


        Text(
            text = "媒体控制通知",
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.Bold,
            fontSize = 35.sp,
            modifier = Modifier
                .padding(top = 12.dp, bottom = 5.dp)
                .graphicsLayer {
                    val projectNameProgress = ((scrollProgress - 0.20f) / 0.15f).coerceIn(0f, 1f)
                    alpha = 1f - projectNameProgress
                    scaleX = 1f - (projectNameProgress * 0.05f)
                    scaleY = 1f - (projectNameProgress * 0.05f)
                }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val versionProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
                    alpha = 1f - versionProgress
                    scaleX = 1f - (versionProgress * 0.05f)
                    scaleY = 1f - (versionProgress * 0.05f)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$versionName ($versionCode)",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp
            )
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 16.dp)
    ) {
        item(key = "logoSpacer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(logoHeightDp + 52.dp + 40.dp + 82.dp)
                    .onSizeChanged { size -> onLogoSpacerHeightChanged(size.height) }
            )
        }

        item(key = "about_content") {
            Column(
                modifier = Modifier
                    .fillParentMaxHeight()
                    .padding(bottom = 16.dp)
            ) {
                SmallTitle(text = "链接")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    Column {
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
                        
                        ArrowPreference(
                            title = "检查更新",
                            summary = "检查软件版本更新和新功能",
                            onClick = {
                                uriHandler.openUri("https://github.com/dinopig1219/MediaControlNotification/releases")
                            }
                        )
                    }
                }
            }
        }
    }
}
