package com.example.lab5mobile


import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat

class DownloadService () : Service() {

    private val CHANNEL_ID = "DownloadChannel"
    private val NOTIFICATION_ID = 1
    private var progress = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("DownloadService", "onCreate called")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("DownloadService", "onStartCommand called")
        startForeground(NOTIFICATION_ID, createInitialNotification())
        startDownloadSimulation()
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.name_dl_serv),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createInitialNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.dl_serv_init))
            .setContentText(getString(R.string.prog_dl_serv_init_0))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, 0, false)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(progress: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.file_dl_serv))
            .setContentText(getString(R.string.progress_with_prc_in_ser) + " $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showDownloadCompleteNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.dl_comp_serv))
            .setContentText(getString(R.string.dl_comp_serv))
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2, notification)
    }

    private fun startDownloadSimulation() {
        handler.post(object : Runnable {
            override fun run() {
                if (progress < 100) {
                    progress += 10
                    updateNotification(progress)
                    handler.postDelayed(this, 2000)
                } else {
                    showDownloadCompleteNotification()
                    stopForeground(true)
                    stopSelf()
                }
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}