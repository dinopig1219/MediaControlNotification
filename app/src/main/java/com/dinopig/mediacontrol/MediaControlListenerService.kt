package com.dinopig.mediacontrol

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.content.SharedPreferences

class MediaControlListenerService : NotificationListenerService() {

    companion object {
        const val CHANNEL_ID = "media_control_notification"
        const val NOTIFICATION_ID = 9001
        const val DEBUG_NOTIFICATION_ID = 9002
        val TARGET_PACKAGES = setOf("com.spotify.music")
    }

    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeController: MediaControllerCompat? = null
    private var activePackageName: String = ""

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) = updateNotification()
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) = updateNotification()
        override fun onSessionDestroyed() {
            activeController = null
            cancelNotification()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers -> pickController(controllers) }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "master_enabled") {
            updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    
        getSharedPreferences("debug_info", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        
        getSharedPreferences("debug_info", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val component = ComponentName(this, MediaControlListenerService::class.java)
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            pickController(mediaSessionManager.getActiveSessions(component))
        } catch (e: SecurityException) {
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {}
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        
        if (sbn?.packageName in TARGET_PACKAGES) {
            
            getSharedPreferences("debug_info", Context.MODE_PRIVATE)
                .edit()
                .putString("current_song", "暂未获取到歌名")
                .putString("current_artist", "暂未获取到歌手")
                .putString("last_debug_info", "还没有数据。请先播放 Spotify，确保已授权通知使用权。")
                .apply()
                
            cancelNotification()
            activeController = null
        }
    }

    private fun pickController(controllers: List<android.media.session.MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        val target = controllers?.firstOrNull { it.packageName in TARGET_PACKAGES }
        activePackageName = target?.packageName ?: ""
        activeController = target?.let {
            MediaControllerCompat(this, MediaSessionCompat.Token.fromToken(it.sessionToken))
        }
        activeController?.registerCallback(controllerCallback)
        updateNotification()
    }

    private fun updateNotification() {
        val masterEnabled = getSharedPreferences("debug_info", Context.MODE_PRIVATE)
            .getBoolean("master_enabled", false)
        if (!masterEnabled) {
            cancelNotification()
            return
        }

        val controller = activeController
        val state = controller?.playbackState

        if (controller == null || state == null || state.state == PlaybackStateCompat.STATE_NONE) {
            cancelNotification()
            
            getSharedPreferences("debug_info", Context.MODE_PRIVATE)
                .edit()
                .remove("current_song")
                .remove("current_artist")
                .remove("last_debug_info")
                .apply()
                
            return
        }
        
        val metadata = controller.metadata
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "正在播放")
            .setContentText(metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "")
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

        metadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)?.let { builder.setLargeIcon(it) }

        val prefs = getSharedPreferences("debug_info", Context.MODE_PRIVATE)

        val prevAction = standardAction(R.drawable.ic_thin_previous, "上一首", MediaActionReceiver.ACTION_SKIP_PREV)
        val playPauseAction = if (isPlaying) standardAction(R.drawable.ic_thin_pause, "暂停", MediaActionReceiver.ACTION_PAUSE)
            else standardAction(R.drawable.ic_thin_play, "播放", MediaActionReceiver.ACTION_PLAY)
        val nextAction = standardAction(R.drawable.ic_thin_next, "下一首", MediaActionReceiver.ACTION_SKIP_NEXT)
        val customActionsList = state.customActions?.map { customAction(it) } ?: emptyList()
        val custom1 = customActionsList.getOrNull(0)
        val custom2 = customActionsList.getOrNull(1)

        val left2 = mutableListOf<NotificationCompat.Action>()
        val left1 = mutableListOf<NotificationCompat.Action>()
        val right1 = mutableListOf<NotificationCompat.Action>()
        val right2 = mutableListOf<NotificationCompat.Action>()

        fun place(slot: String, action: NotificationCompat.Action?) {
            if (action == null) return
            when (slot) {
                "LEFT2" -> left2.add(action)
                "LEFT1" -> left1.add(action)
                "RIGHT1" -> right1.add(action)
                "RIGHT2" -> right2.add(action)
            }
        }
        place(prefs.getString("expanded_custom1_slot", "RIGHT1") ?: "RIGHT1", custom1)
        place(prefs.getString("expanded_custom2_slot", "RIGHT2") ?: "RIGHT2", custom2)

        val orderedActions = mutableListOf<NotificationCompat.Action>()
        orderedActions.addAll(left2)
        orderedActions.addAll(left1)
        orderedActions.add(prevAction)
        orderedActions.add(playPauseAction)
        orderedActions.add(nextAction)
        orderedActions.addAll(right1)
        orderedActions.addAll(right2)
        customActionsList.drop(2).forEach { orderedActions.add(it) }

        orderedActions.forEach { builder.addAction(it) }

        val compactMode = prefs.getString("compact_mode", "STANDARD") ?: "STANDARD"
        val compactIndices: IntArray = if (compactMode == "CUSTOM" && custom1 != null && custom2 != null) {
            val side1 = prefs.getString("compact_custom1_side", "LEFT") ?: "LEFT"
            val side2 = prefs.getString("compact_custom2_side", "RIGHT") ?: "RIGHT"
            listOf(custom1 to side1, custom2 to side2)
                .sortedBy { if (it.second == "LEFT") 0 else 1 }
                .map { orderedActions.indexOf(it.first) }
                .filter { it >= 0 }
        } else {
            listOf(prevAction, playPauseAction, nextAction).map { orderedActions.indexOf(it) }
        }.distinct().toIntArray()

        builder.setStyle(MediaStyle().setShowActionsInCompactView(*compactIndices))

        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, builder.build())

        // 完整调试信息一直存起来，App 里的 DebugActivity 随时能看到最新的
        saveDebugInfo(state, metadata)

        // 调试通知本身要不要发，看用户在 App 里那个开关
        val debugNotificationsOn = getSharedPreferences("debug_info", Context.MODE_PRIVATE)
            .getBoolean("debug_notifications_enabled", false)
        if (debugNotificationsOn) {
            showDebugNotification(state)
        } else {
            getSystemService(NotificationManager::class.java).cancel(DEBUG_NOTIFICATION_ID)
        }
    }

    private fun buildDebugText(state: PlaybackStateCompat): String {
        val sb = StringBuilder()
        sb.append("更新时间: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}\n\n")
        sb.append("包名: $activePackageName\n")
        sb.append("actions bitmask: ${state.actions}\n")
        sb.append("customActions:\n")
        if (state.customActions.isNullOrEmpty()) {
            sb.append("  (无)\n")
        } else {
            state.customActions.forEach {
                sb.append("  name=${it.name}\n  action=${it.action}\n  icon=${it.icon}\n\n")
            }
        }
        return sb.toString()
    }

    private fun saveDebugInfo(state: PlaybackStateCompat, metadata: MediaMetadataCompat?) {
    val title = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "暂未获取到歌名"
    val artist = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "暂未获取到歌手"
    
    getSharedPreferences("debug_info", Context.MODE_PRIVATE)
        .edit()
        .putString("last_debug_info", buildDebugText(state))
        .putString("current_song", title)
        .putString("current_artist", artist)
        .apply()
}

    private fun showDebugNotification(state: PlaybackStateCompat) {
        val debugBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("调试信息")
            .setStyle(NotificationCompat.BigTextStyle().bigText(buildDebugText(state)))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

        getSystemService(NotificationManager::class.java).notify(DEBUG_NOTIFICATION_ID, debugBuilder.build())
    }

    private fun standardAction(icon: Int, title: String, action: String): NotificationCompat.Action {
        val intent = Intent(this, MediaActionReceiver::class.java).apply { this.action = action }
        val pi = PendingIntent.getBroadcast(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(icon, title, pi).build()
    }

    private fun customAction(customAction: PlaybackStateCompat.CustomAction): NotificationCompat.Action {
        val intent = Intent(this, MediaActionReceiver::class.java).apply {
            action = MediaActionReceiver.ACTION_CUSTOM
            putExtra(MediaActionReceiver.EXTRA_CUSTOM_ACTION, customAction.action)
        }
        val pi = PendingIntent.getBroadcast(
            this, customAction.action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = try {
            val remoteResources = packageManager.getResourcesForApplication(activePackageName)
            IconCompat.createWithResource(remoteResources, activePackageName, customAction.icon)
        } catch (e: Exception) {
            IconCompat.createWithResource(resources, packageName, android.R.drawable.ic_menu_help)
        }
        return NotificationCompat.Action.Builder(icon, customAction.name.toString(), pi).build()
    }

    private fun cancelNotification() {
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        getSystemService(NotificationManager::class.java).cancel(DEBUG_NOTIFICATION_ID)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "媒体控制通知", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
