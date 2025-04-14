package com.example.lab5mobile

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.lab5mobile.databinding.ActivityMainBinding
import android.app.Notification
import android.app.NotificationChannel
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // launcher для запроса разрешения
    private val requestPostNotification = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::onPostNotificationGranted
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onSetupNotificationChannel()
        findViewById<Button>(R.id.notify).setOnClickListener {
            onSendNotification()
        }
        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !isPostNotificationGranted()) {
                requestPostNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                val intent = Intent(this, DownloadService::class.java)
                startForegroundService(intent)
            }
        }
    }
    private fun createNotificationForBob(): Notification {
        return NotificationCompat.Builder(this, CoolStoryBobNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_cookie_24)    // иконка
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()) // свой стиль
            .setCustomContentView(buildNotificationPanel()) // свой контент
            .setShowWhen(true) // время
            .setOngoing(true) // может ли пользователь его сам убрать
            .setContentText("Story from bob") // текст
            .build() }

    private fun buildNotificationPanel( title: String = "Current story",
                                        ): RemoteViews { // вся работа через API у RemoteViews
            return RemoteViews(this.packageName,
                R.layout.player_notification)
                .apply {
                    setTextViewText(R.id.notification_title, title)
                }
    }
    private fun onSetupNotificationChannel() {
        // проверяем текущие устройство
        // если Android ниже 8, то просто возвращаемся, ибо
        // channel не нужен
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        // создание канала
        val channel = NotificationChannel(
            CoolStoryBobNotification.CHANNEL_ID,
            getString(R.string.bob_notification),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        // установка его
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
    private val collStoryBobConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?)
        {
            val coolStoryBob = (service as CoolStoryBobService.LocalBinder).getBob()
            onProcessBobStories(coolStoryBob)
        }
        override fun onServiceDisconnected(name: ComponentName?) { }
    }
    private fun onProcessBobStories(coolStoryBob: CoolStoryBob) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                coolStoryBob.stories.collect {
                    story ->
                    Log.d("MainActivity", "onProcessBobStories: $story") }
            }
        }
    }

    // отправка уведомления
    private fun onSendNotification() {
        if (!isPostNotificationGranted()) {
            requestPostNotification()
        }
        val notification = createNotificationForBob()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(CoolStoryBobNotification.NOTIFICATION_ID, notification)
    }
    // запрос разрешения
    private fun requestPostNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPostNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // проверка есть ли разрешение
    private fun isPostNotificationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    // получение результата разрешения
    private fun onPostNotificationGranted(isGranted: Boolean) {
        if (!isGranted) {
            Toast.makeText(this,
                "Permission not granted",
                Toast.LENGTH_SHORT).show()
            return }
        onSendNotification()
    }




}