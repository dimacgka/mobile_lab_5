package com.example.lab5mobile

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

private const val MY_CUSTOM_ACTION = "my_custom_action"
private const val NOTIFICATION_REQUEST_CODE = 1

// Интерфейс, чтобы ограничить возможности из вне
interface CoolStoryBob {
    // будет поток историй
    val stories: SharedFlow<String>
}

class CoolStoryBobService : Service(), CoolStoryBob {
    // Создание Binder
    // inner - значит имеет ссылку на родителя, где определен
    // т.е. на CoolStoryBobService
    inner class LocalBinder : Binder() {
        fun getBob(): CoolStoryBob = this@CoolStoryBobService
    }

    private val binder = LocalBinder()
    private val _stories = MutableSharedFlow<String>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val stories: SharedFlow<String> = _stories.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    // Изменяемое значение. Корутина в другом потоке, поэтому нужен
    // метод синхронизации (сейчас AtomicReference) для предотвращения
    // race condition
    private var template: AtomicReference<String> = AtomicReference("Story")


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (val data = intent?.getStringExtra(MY_CUSTOM_ACTION)) {
            is String -> {
                template.set(data)
                // обновляем notification
                val notification = createNotification()
                startForeground(1, notification)
            }
            // без action
            else -> {
                val notification = createNotification()
                startForeground(1, notification)
                scope.launch {
                    repeat(100) { data ->
                        _stories.tryEmit("${template.get()} $data")
                        delay(1000L)
                    }
                    Log.d("CoolStoryBobService", "onStartCommand: complete!")
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        // изначальный интент
        val intent = Intent(applicationContext, CoolStoryBobService::class.java).apply {
            putExtra(MY_CUSTOM_ACTION, "somedata")
        }
        // делаем PendingIntent
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CoolStoryBobNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_cookie_24)
            .setShowWhen(true)
            .setOngoing(true)
            .setContentTitle("CoolStoryBobService started")
            .setContentText("${template.get()} from bob")
            .addAction(R.drawable.ic_play_arrow, "Change text", pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

