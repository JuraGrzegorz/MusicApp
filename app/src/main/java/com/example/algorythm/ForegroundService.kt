package com.example.algorythm

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import android.util.Base64
import kotlinx.coroutines.delay
import kotlin.io.encoding.ExperimentalEncodingApi

class ForegroundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ForegroundService = this@ForegroundService
    }

    override fun onCreate() {
        super.onCreate()

        mediaPlayer = MediaPlayer()
        // Inicjalizacja MediaSession
        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaController = MediaControllerCompat(this, mediaSession.sessionToken)

        // Ustawienia MediaSession
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(MediaSessionCallback())

        // Utworzenie kanału powiadomień
        createNotificationChannel()

        // Inicjalizacja budowniczego powiadomień
        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_placeholder)
            .setContentTitle("AlgoRhythm")
            .setContentText("Playing music")
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this, PlaybackStateCompat.ACTION_STOP
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2) // Indeksy działań: pause, play, stop
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this, PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )

        // Zarządzanie stanem odtwarzania
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L)

        // Ustawienie sesji do obsługi powiadomień
        mediaSession.isActive = true
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        val action = intent?.action
        val url = intent?.getStringExtra(EXTRA_URL)
        when (action) {
            ACTION_START -> {
                url?.let { playMusic(it) }
            }
            ACTION_PAUSE -> {
                mediaPlayer?.pause()
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer?.currentPosition?.toLong() ?: 0L)
            }
            ACTION_RESUME -> {
                mediaPlayer?.start()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer?.currentPosition?.toLong() ?: 0L)
            }
            ACTION_STOP -> {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                updatePlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L)
                stopSelf()
            }
            ACTION_NEXT -> {
                nextTrack()
            }
            ACTION_SEEK->{
                val position = intent.getIntExtra(EXTRA_POSITION, 0)
                mediaPlayer?.seekTo(position)

            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        // Aktualizacja powiadomienia
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun updatePlaybackState(state: Int, position: Long) {
        // Ustawienie stanu odtwarzania w sesji i kontrolerze
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, position, 1.0f, SystemClock.elapsedRealtime())
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_STOP)
            .build()
        mediaSession.setPlaybackState(playbackState)

        // Aktualizacja metadanych
        val metadata = MediaMetadataCompat.Builder()
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer?.duration?.toLong() ?: 0L)
            .build()
        mediaSession.setMetadata(metadata)

        // Aktualizacja powiadomienia
        updateNotification()
    }

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            // Implementacja rozpoczęcia odtwarzania
            mediaSession.isActive = true
            mediaPlayer?.start()
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer?.currentPosition?.toLong() ?: 0L)
        }

        override fun onPause() {
            // Implementacja pauzy
            mediaPlayer?.pause()
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer?.currentPosition?.toLong() ?: 0L)
        }

        override fun onStop() {
            // Implementacja zatrzymania
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L)
            stopSelf()
        }
    }

    private fun playMusic(url: String) {
        // Implementacja odtwarzania muzyki
        mediaPlayer?.reset()
        mediaPlayer?.setDataSource(url)
        mediaPlayer?.setOnPreparedListener {
            mediaPlayer?.start()
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, 0L) // Update state immediately after starting
            startPositionUpdates()
        }
        mediaPlayer?.prepareAsync()
    }

    private fun sendPositionUpdate() {
        mediaPlayer?.let {
            val intent = Intent(ACTION_POSITION_UPDATE).apply {
                putExtra(EXTRA_POSITION, it.currentPosition)
                putExtra(EXTRA_DURATION, it.duration)
            }
            sendBroadcast(intent)
        }
    }

    private fun startPositionUpdates() {
        coroutineScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                sendPositionUpdate()
                delay(1000)
            }
        }
    }

    private fun nextTrack() {
        coroutineScope.launch {
            try {
                val sharedPref = getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
                val jwt = sharedPref.getString("JWT", "") ?: ""
                val data: String = API.getProposedMusic(1, jwt)
                val arr = JSONArray(data)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val nextId = obj.getString("id")
                    val nextTitle = obj.getString("title")
                    val nextAuthor = obj.getString("artistName")
                    val thumbnailData = obj.getString("thumbnailData")
                    val nextViews = obj.getString("views")
                    val nextLikes = obj.getString("likes")
                    val imageBytes = Base64.decode(thumbnailData, Base64.DEFAULT)
                    val nextBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                    // Zaktualizuj odtwarzacz z nowym utworem
                    playMusic(nextId) // assuming nextId is a URL or file path

                    // Aktualizacja powiadomienia z nowymi metadanymi
                    val metadata = MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, nextTitle)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, nextAuthor)
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer?.duration?.toLong() ?: 0L)
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, nextBitmap)
                        .build()
                    mediaSession.setMetadata(metadata)

                    // Ustaw dane do powiadomienia
                    notificationBuilder.setContentTitle(nextTitle)
                        .setContentText(nextAuthor)
                        .setLargeIcon(nextBitmap)

                    // Aktualizacja powiadomienia
                    updateNotification()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getMediaPlayer(): MediaPlayer? {
        return mediaPlayer
    }

    companion object {
        private const val CHANNEL_ID = "MusicServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val EXTRA_URL = "EXTRA_URL"

        const val ACTION_SEEK = "ACTION_SEEK"
        const val ACTION_POSITION_UPDATE = "ACTION_POSITION_UPDATE"
        const val EXTRA_POSITION = "EXTRA_POSITION"
        const val EXTRA_DURATION = "EXTRA_DURATION"
    }
}