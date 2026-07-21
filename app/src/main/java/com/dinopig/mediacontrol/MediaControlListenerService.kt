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
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle

class MediaControlListenerService : NotificationListenerService() {

    companion object {
        const val CHANNEL_ID = "media_control_patch"
        const val NOTIFICATION_ID = 9001
        const val DEBUG_NOTIFICATION_ID = 9002
        const val DEBUG_MODE = true // 先开着看原始数据，确认 Smart Shuffle 怎么暴露之后可以关掉
        val TARGET_PACKAGES = setOf("com.spotify.music")
    }

    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeController: MediaControllerCompat? = null

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) = updateNotification()
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) = updateNotification()
        override fun onRepeatModeChanged(repeatMode: Int) = updateNotification()
        override fun onShuffleModeChanged(shuffleMode: Int) = updateNotification()
        override fun onSessionDestroyed() {
            activeController = null
            cancelNotification()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers -> pickController(controllers) }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
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
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun pickController(controllers: List<android.media.session.MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        val target = controllers?.firstOrNull { it.packageName in TARGET_PACKAGES }
        activeController = target?.let {
            MediaControllerCompat(this, MediaSessionCompat.Token.fromToken(it.sessionToken))
        }
        activeController?.registerCallback(controllerCallback)
        updateNotification()
    }

    private fun updateNotification() {
        val controller = activeController
        val state = controller?.playbackState

        if (controller == null || state == null || state.state == PlaybackStateCompat.STATE_NONE) {
            cancelNotification()
            return
        }

        val metadata = controller.metadata
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
        val actionsBitmask = state.actions

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "正在播放")
            .setContentText(metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "")
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

        metadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)?.let { builder.setLargeIcon(it) }

        builder.addAction(standardAction(android.R.drawable.ic_media_previous, "上一首", MediaActionReceiver.ACTION_SKIP_PREV))
        builder.addAction(
            if (isPlaying) standardAction(android.R.drawable.ic_media_pause, "暂停", MediaActionReceiver.ACTION_PAUSE)
            else standardAction(android.R.drawable.ic_media_play, "播放", MediaActionReceiver.ACTION_PLAY)
        )
        builder.addAction(standardAction(android.R.drawable.ic_media_next, "下一首", MediaActionReceiver.ACTION_SKIP_NEXT))

        if (actionsBitmask and PlaybackStateCompat.ACTION_SET_REPEAT_MODE != 0L) {
            val label = when (controller.repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> "循环: 单曲"
                PlaybackStateCompat.REPEAT_MODE_ALL, PlaybackStateCompat.REPEAT_MODE_GROUP -> "循环: 全部"
                else -> "循环: 关闭"
            }
            builder.addAction(standardAction(R.drawable.ic_repeat, label, MediaActionReceiver.ACTION_TOGGLE_REPEAT))
        }

        if (actionsBitmask and PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE != 0L) {
            val shuffleOn = controller.shuffleMode != PlaybackStateCompat.SHUFFLE_MODE_NONE
            builder.addAction(
                standardAction(
                    R.drawable.ic_shuffle,
                    if (shuffleOn) "随机: 开" else "随机: 关",
                    MediaActionReceiver.ACTION_TOGGLE_SHUFFLE
                )
            )
        }

        if (actionsBitmask and PlaybackStateCompat.ACTION_SET_RATING != 0L) {
            val currentRating = metadata?.getRating(MediaMetadataCompat.METADATA_KEY_USER_RATING)
            val loved = when (controller.ratingType) {
                RatingCompat.RATING_HEART -> currentRating?.hasHeart() == true
                RatingCompat.RATING_THUMB_UP_DOWN -> currentRating?.isRated == true && currentRating.isThumbUp
                else -> false
            }
            builder.addAction(
                standardAction(
                    if (loved) R.drawable.ic_like_filled else R.drawable.ic_like_outline,
                    if (loved) "已喜欢" else "喜欢",
                    MediaActionReceiver.ACTION_TOGGLE_LIKE
                )
            )
        }

        state.customActions?.forEach { builder.addAction(customAction(it)) }

        builder.setStyle(MediaStyle().setShowActionsInCompactView(0, 1, 2))

        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, builder.build())

        if (DEBUG_MODE) showDebugNotification(state, controller)
    }

    // 只是给你看原始数据用的，跟功能无关，之后确定 Smart Shuffle 的字段后可以整段删掉
    private fun showDebugNotification(state: PlaybackStateCompat, controller: MediaControllerCompat) {
        val sb = StringBuilder()
        sb.append("actions bitmask: ${state.actions}\n")
        sb.append("repeatMode: ${controller.repeatMode}\n")
        sb.append("shuffleMode: ${controller.shuffleMode}\n")
        sb.append("ratingType: ${controller.ratingType}\n")
        sb.append("customActions:\n")
        if (state.customActions.isNullOrEmpty()) {
            sb.append("  (无)\n")
        } else {
            state.customActions.forEach {
                sb.append("  name=${it.name} action=${it.action} icon=${it.icon}\n")
            }
        }

        val debugBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("调试信息（点开看完整）")
            .setStyle(NotificationCompat.BigTextStyle().bigText(sb.toString()))
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
        return NotificationCompat.Action.Builder(customAction.icon, customAction.name.toString(), pi).build()
    }

    private fun cancelNotification() {
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        getSystemService(NotificationManager::class.java).cancel(DEBUG_NOTIFICATION_ID)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "媒体控制补丁", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
