package com.dinopig.mediacontrol

import android.Manifest
import android.app.Activity
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.WindowCompat
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
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.basic.BasicComponent
import androidx.compose.ui.graphics.BlendMode
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import android.content.ComponentName
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import androidx.compose.foundation.layout.Row
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import androidx.compose.ui.res.stringResource

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

private fun openAutoStartSettings(context: Context) {
    try {
        val intent = Intent().apply {
            component = ComponentName(
                "com.miui.securitycenter", 
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}

@Composable
private fun RootScreen() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    
    val railState = rememberNavigationRailState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                state = railState
            ) {
                NavigationRailItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    icon = MiuixIcons.Home,
                    label = stringResource(R.string.nav_home)
                )
                NavigationRailItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    icon = MiuixIcons.Demibold.Info,
                    label = stringResource(R.string.nav_about)
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) HomePage() else AboutPage()
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        icon = MiuixIcons.Home,
                        label = stringResource(R.string.nav_home)
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        icon = MiuixIcons.Demibold.Info,
                        label = stringResource(R.string.nav_about)
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
}

@Composable
private fun HomePage() {
    val scrollBehavior = MiuixScrollBehavior()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        topBar = { 
            if (isTablet) {
                SmallTopAppBar(
                    title = stringResource(R.string.app_name),
                    scrollBehavior = scrollBehavior
                )
            } else {
                TopAppBar(
                    title = stringResource(R.string.app_name),
                    largeTitle = stringResource(R.string.app_name),
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { padding ->
        HomeScreen(scrollBehavior, padding)
    }
}

@Composable
private fun AboutPage() {
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
    var logoSpacerHeightPx by remember { mutableStateOf(0) }
    val backdrop = rememberLayerBackdrop()
    val blurSupported = remember { isRuntimeShaderSupported() }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val scrollProgress by remember {
        derivedStateOf {
            if (logoSpacerHeightPx <= 0) return@derivedStateOf 0f
            val index = lazyListState.firstVisibleItemIndex
            val offset = lazyListState.firstVisibleItemScrollOffset
            if (index > 0) 1f else (offset.toFloat() / logoSpacerHeightPx).coerceIn(0f, 1f)
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
                .graphicsLayer { alpha = 1f - scrollProgress }
                .layerBackdrop(backdrop)
        ) { }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (isTablet) {
                    SmallTopAppBar(
                        title = stringResource(R.string.nav_about),
                        scrollBehavior = scrollBehavior,
                        color = if (scrollProgress > 0.99f) MiuixTheme.colorScheme.surface else Color.Transparent,
                        titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = scrollProgress)
                    )
                } else {
                    TopAppBar(
                        title = stringResource(R.string.nav_about),
                        largeTitle = "",
                        scrollBehavior = scrollBehavior,
                        color = if (scrollProgress > 0.99f) MiuixTheme.colorScheme.surface else Color.Transparent,
                        titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = scrollProgress)
                    )
                }
            }
        ) { padding ->
            AboutScreen(
                scrollBehavior = scrollBehavior,
                padding = padding,
                lazyListState = lazyListState,
                scrollProgress = scrollProgress,
                onLogoSpacerHeightChanged = { logoSpacerHeightPx = it },
                backdrop = backdrop,
                blurSupported = blurSupported
            )
        }
    }
}

@OptIn(ExperimentalScrollBarApi::class)
@Composable
private fun HomeScreen(scrollBehavior: ScrollBehavior, padding: PaddingValues) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("debug_info", Context.MODE_PRIVATE) }
    val defaultSongText = stringResource(R.string.info_no_song)
    val defaultArtistText = stringResource(R.string.info_no_artist)
    val defaultInfoText = stringResource(R.string.info_no_data)

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

    var currentSong by remember { mutableStateOf(prefs.getString("current_song", defaultSongText) ?: defaultSongText) }
    var currentArtist by remember { mutableStateOf(prefs.getString("current_artist", defaultArtistText) ?: defaultArtistText) }
    var debugInfo by remember { mutableStateOf(prefs.getString("last_debug_info", "") ?: "") }

    val currentPackage by remember {
        derivedStateOf {
            if (debugInfo.contains("Package name: ")) {
                debugInfo.substringAfter("Package name: ").substringBefore("\n").trim()
            } else {
                defaultInfoText
            }
        }
    }

    val customAction1 by remember {
        derivedStateOf {
            if (debugInfo.contains("name=")) {
                debugInfo.substringAfter("name=").substringBefore("\n").trim()
            } else defaultInfoText
        }
    }

    val customAction2 by remember {
        derivedStateOf {
            val firstIndex = debugInfo.indexOf("name=")
            if (firstIndex != -1) {
                val afterFirst = debugInfo.substring(firstIndex + 5)
                if (afterFirst.contains("name=")) {
                    afterFirst.substringAfter("name=").substringBefore("\n").trim()
                } else defaultInfoText
            } else defaultInfoText
        }
    }

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "current_song") currentSong = sharedPreferences.getString("current_song", defaultSongText) ?: defaultSongText
            if (key == "current_artist") currentArtist = sharedPreferences.getString("current_artist", defaultArtistText) ?: defaultArtistText
            if (key == "last_debug_info") debugInfo = sharedPreferences.getString("last_debug_info", "") ?: ""
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
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

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState),
        ) {
            SmallTitle(text = stringResource(R.string.section_master_switch))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                SwitchPreference(
                    title = stringResource(R.string.pref_enable_service_title),
                    summary = when {
                        !notificationGranted || !listenerEnabled -> stringResource(R.string.pref_enable_service_summary_need_perms)
                        masterEnabled -> stringResource(R.string.pref_enable_service_summary_running)
                        else -> stringResource(R.string.pref_enable_service_summary_off)
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

            if (masterEnabled) {
                SmallTitle(text = stringResource(R.string.section_info))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    Column {
                        BasicComponent(
                            title = currentSong,
                            summary = currentArtist
                        )
                        BasicComponent(
                            title = stringResource(R.string.info_package_title),
                            summary = currentPackage
                        )
                        BasicComponent(
                            title = stringResource(R.string.info_custom_action1_title),
                            summary = customAction1
                        )
                        BasicComponent(
                            title = stringResource(R.string.info_custom_action2_title),
                            summary = customAction2
                        )
                    }
                }
            }

            SmallTitle(text = stringResource(R.string.section_notification_settings))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                ArrowPreference(
                    title = stringResource(R.string.pref_notification_order_title),
                    summary = stringResource(R.string.pref_notification_order_summary),
                    onClick = { context.startActivity(Intent(context, OrderActivity::class.java)) }
                )
            }

            SmallTitle(text = stringResource(R.string.section_permission_settings))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                Column {
                    SwitchPreference(
                        title = stringResource(R.string.pref_notification_permission_title),
                        summary = if (notificationGranted) stringResource(R.string.state_granted) else stringResource(R.string.pref_notification_permission_ungranted),
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
                        title = stringResource(R.string.pref_listener_permission_title),
                        summary = if (listenerEnabled) stringResource(R.string.state_granted) else stringResource(R.string.pref_listener_permission_ungranted),
                        checked = listenerEnabled,
                        onCheckedChange = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )

                    ArrowPreference(
                        title = stringResource(R.string.pref_autostart_title),
                        summary = stringResource(R.string.pref_autostart_summary),
                        onClick = { openAutoStartSettings(context) }
                    )

                    ArrowPreference(
                        title = stringResource(R.string.pref_battery_title),
                        summary = stringResource(R.string.pref_battery_summary),
                        onClick = { openBatteryOptimizationSettings(context) }
                    )

                    SwitchPreference(
                        title = stringResource(R.string.pref_hide_recents_title),
                        summary = stringResource(R.string.pref_hide_recents_summary),
                        checked = hideRecentsEnabled,
                        onCheckedChange = { checked ->
                            hideRecentsEnabled = checked
                            prefs.edit().putBoolean("hide_recents_enabled", checked).apply()
                        }
                    )
                }
            }

            SmallTitle(text = stringResource(R.string.section_log))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                Column {
                    SwitchPreference(
                        title = stringResource(R.string.pref_debug_notification_title),
                        summary = stringResource(R.string.pref_debug_notification_summary),
                        checked = debugNotificationsOn,
                        onCheckedChange = { checked ->
                            debugNotificationsOn = checked
                            prefs.edit().putBoolean("debug_notifications_enabled", checked).apply()
                        }
                    )

                    ArrowPreference(
                        title = stringResource(R.string.pref_view_debug_info_title),
                        onClick = { context.startActivity(Intent(context, DebugActivity::class.java)) }
                    )
                }
            }
        }

        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(scrollState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}

@OptIn(ExperimentalScrollBarApi::class)
@Composable
private fun AboutScreen(
    scrollBehavior: ScrollBehavior,
    padding: PaddingValues,
    lazyListState: LazyListState,
    scrollProgress: Float,
    onLogoSpacerHeightChanged: (Int) -> Unit,
    backdrop: LayerBackdrop,
    blurSupported: Boolean
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionName = packageInfo.versionName ?: stringResource(R.string.about_version_unknown)
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)

    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val logoBlend = remember(isDark) {
        if (isDark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
            )
        }
    }
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
            text = stringResource(R.string.app_name),
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
                .then(
                    if (blurSupported) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(16.dp),
                            blurRadius = 150f,
                            noiseCoefficient = BlurDefaults.NoiseCoefficient,
                            colors = BlurColors(blendColors = logoBlend),
                            contentBlendMode = BlendMode.DstIn,
                        )
                    } else Modifier
                )
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                    SmallTitle(text = stringResource(R.string.section_links))
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        Column {
                            ArrowPreference(
                                title = stringResource(R.string.pref_view_source_title),
                                summary = stringResource(R.string.pref_view_source_summary),
                                endActions = {
                                    Text(
                                        text = stringResource(R.string.label_github),
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                },
                                onClick = {
                                    uriHandler.openUri("https://github.com/dinopig1219/MediaControlNotification")
                                }
                            )
                            
                            ArrowPreference(
                                title = stringResource(R.string.pref_check_update_title),
                                summary = stringResource(R.string.pref_check_update_summary),
                                onClick = {
                                    uriHandler.openUri("https://github.com/dinopig1219/MediaControlNotification/releases")
                                }
                            )
                        }
                    }
                }
            }
        }
        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(lazyListState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}