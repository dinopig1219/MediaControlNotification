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
            
            val defaultNoData = getString(R.string.debug_no_data)
            val defaultSong = getString(R.string.info_no_song)
            val defaultArtist = getString(R.string.info_no_artist)
            getSharedPreferences("debug_info", Context.MODE_PRIVATE)
                .edit()
                .putString("current_song", defaultSong)
                .putString("current_artist", defaultArtist)
                .putString("last_debug_info", defaultNoData)
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
            .setContentTitle(metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: getString(R.string.notification_title_now_playing))
            .setContentText(metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "")
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

        metadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)?.let { builder.setLargeIcon(it) }

        val prefs = getSharedPreferences("debug_info", Context.MODE_PRIVATE)

        val prevAction = standardAction(R.drawable.ic_thin_previous, getString(R.string.notification_action_previous), MediaActionReceiver.ACTION_SKIP_PREV)
        val playPauseAction = if (isPlaying) standardAction(R.drawable.ic_thin_pause, getString(R.string.notification_action_pause), MediaActionReceiver.ACTION_PAUSE)
            else standardAction(R.drawable.ic_thin_play, getString(R.string.notification_action_play), MediaActionReceiver.ACTION_PLAY)
        val nextAction = standardAction(R.drawable.ic_thin_next, getString(R.string.notification_action_next), MediaActionReceiver.ACTION_SKIP_NEXT)
        val customActionsList = state.customActions?.mapIndexed { index, action -> customAction(action, index) } ?: emptyList()
        val custom1 = customActionsList.getOrNull(0)
        val custom2 = customActionsList.getOrNull(1)

        val left1 = mutableListOf<NotificationCompat.Action>()
        val left2 = mutableListOf<NotificationCompat.Action>()
        val right1 = mutableListOf<NotificationCompat.Action>()
        val right2 = mutableListOf<NotificationCompat.Action>()

        fun place(slot: String, action: NotificationCompat.Action?) {
            if (action == null) return
            when (slot) {
                "LEFT1" -> left1.add(action)
                "LEFT2" -> left2.add(action)
                "RIGHT1" -> right1.add(action)
                "RIGHT2" -> right2.add(action)
            }
        }
        place(prefs.getString("expanded_custom1_slot", "RIGHT1") ?: "RIGHT1", custom1)
        place(prefs.getString("expanded_custom2_slot", "RIGHT2") ?: "RIGHT2", custom2)

        val orderedActions = mutableListOf<NotificationCompat.Action>()
        orderedActions.addAll(left1)
        orderedActions.addAll(left2)
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

        saveDebugInfo(state, metadata)

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
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        sb.append(getString(R.string.debug_text_updated_at, $time) + "\n\n")
        sb.append(getString(R.string.debug_text_package_name, $activePackageName) + "\n")
        sb.append("actions bitmask: ${state.actions}\n")
        sb.append("customActions:\n")
        if (state.customActions.isNullOrEmpty()) {
            sb.append("  " + getString(R.string.debug_text_none) + "\n")
        } else {
            state.customActions.forEach {
                sb.append("  name=${it.name}\n")
                sb.append("  action=${it.action}\n")
                sb.append("  icon=${it.icon}\n\n")
            }
        }
        return sb.toString()
    }

    private fun saveDebugInfo(state: PlaybackStateCompat, metadata: MediaMetadataCompat?) {
    val title = metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: getString(R.string.info_no_song)
    val artist = metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: getString(R.string.info_no_artist)
    
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
            .setContentTitle(getString(R.string.debug_notification_title))
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

    private fun customAction(customAction: PlaybackStateCompat.CustomAction, index: Int): NotificationCompat.Action {
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
            val fallbackIcon = if (index == 0) R.drawable.ic_custom_1 else R.drawable.ic_custom_2
            IconCompat.createWithResource(resources, packageName, fallbackIcon)
        }
        return NotificationCompat.Action.Builder(icon, customAction.name.toString(), pi).build()
    }

    private fun cancelNotification() {
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        getSystemService(NotificationManager::class.java).cancel(DEBUG_NOTIFICATION_ID)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
