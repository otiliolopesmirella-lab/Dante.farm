package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FarmPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground Service (specialUse) that elevates process priority and holds a partial CPU wake lock
 * so WebView idle-game sessions remain alive and responsive while the user watches YouTube or uses
 * other apps.
 */
class FarmKeepAliveService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var heartbeatJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var activeSlotCount: Int = 1

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                serviceScope.launch {
                    runCatching { FarmPreferencesRepository(applicationContext).updateBackgroundFarmEnabled(false) }
                    stopServiceCleanly()
                }
                return START_NOT_STICKY
            }
            ACTION_UPDATE_STATUS -> {
                activeSlotCount = intent.getIntExtra(EXTRA_ACTIVE_SLOTS, activeSlotCount).coerceIn(1, 4)
            }
            else -> {
                activeSlotCount = intent?.getIntExtra(EXTRA_ACTIVE_SLOTS, activeSlotCount)?.coerceIn(1, 4)
                    ?: activeSlotCount
            }
        }

        promoteToForeground(activeSlotCount)
        acquirePartialWakeLock()
        _isRunning.value = true
        startHeartbeatLoop()
        return START_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopServiceCleanly()
        super.onTimeout(startId, fgsType)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // User explicitly swiped the app away from Recents: stop cleanly
        stopServiceCleanly()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        _isRunning.value = false
        heartbeatJob?.cancel()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promoteToForeground(slotsCount: Int) {
        val notification = buildNotification(slotsCount)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    private fun buildNotification(slotsCount: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, FarmKeepAliveService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentText = if (slotsCount > 1) {
            "$slotsCount contas farmando em 2º plano • Toque para voltar"
        } else {
            "Conta 1 farmando em 2º plano • Toque para voltar"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.fgs_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.fgs_action_stop),
                stopPendingIntent,
            )
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.fgs_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.fgs_channel_desc)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun startHeartbeatLoop() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = serviceScope.launch {
            while (true) {
                delay(15_000L)
                _backgroundTickCount.value = _backgroundTickCount.value + 1
            }
        }
    }

    private fun acquirePartialWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DanteFarm::KeepAliveLock").apply {
            setReferenceCounted(false)
            // Renewed cleanly while service is running; capped at 12h per acquire for safety
            acquire(12 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        wakeLock = null
    }

    private fun stopServiceCleanly() {
        _isRunning.value = false
        heartbeatJob?.cancel()
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val CHANNEL_ID = "dante_farm_keepalive"
        private const val NOTIFICATION_ID = 4041
        private const val ACTION_START = "com.example.action.START_FARM_SERVICE"
        private const val ACTION_UPDATE_STATUS = "com.example.action.UPDATE_FARM_SERVICE"
        private const val ACTION_STOP = "com.example.action.STOP_FARM_SERVICE"
        private const val EXTRA_ACTIVE_SLOTS = "extra_active_slots"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _backgroundTickCount = MutableStateFlow(0L)
        val backgroundTickCount: StateFlow<Long> = _backgroundTickCount.asStateFlow()

        @Volatile
        private var lastNotifiedSlots: Int = -1

        fun startOrUpdate(context: Context, activeSlots: Int) {
            val clamped = activeSlots.coerceIn(1, 4)
            if (_isRunning.value && lastNotifiedSlots == clamped) return
            lastNotifiedSlots = clamped
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, FarmKeepAliveService::class.java).apply {
                action = if (_isRunning.value) ACTION_UPDATE_STATUS else ACTION_START
                putExtra(EXTRA_ACTIVE_SLOTS, clamped)
            }
            runCatching {
                ContextCompat.startForegroundService(appCtx, intent)
            }
        }

        fun stop(context: Context) {
            lastNotifiedSlots = -1
            val appCtx = context.applicationContext
            if (!_isRunning.value) return
            val intent = Intent(appCtx, FarmKeepAliveService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching {
                appCtx.stopService(intent)
            }
            _isRunning.value = false
        }
    }
}
