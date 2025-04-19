package com.example.sgb

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sgb.room.BikeDatabase
import com.example.sub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkerHourIncrement(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("Worker", "Starting work...")
                val dao = BikeDatabase.getDatabase(applicationContext).bikeDao()
                val prefs = applicationContext.getSharedPreferences("bike_prefs", Context.MODE_PRIVATE)
                val bikeId = prefs.getInt("selected_bike_id", -1)
                Log.d("Worker", "Bike ID: $bikeId")

                if (bikeId != -1) {
                    val bike = dao.getBikeById(bikeId)
                    bike?.let {
                        val newHours = it.elapsedHoursValue + 1
                        Log.d("Worker", "Updating hours to $newHours")
                        dao.updateElapsedHours(bikeId, newHours)
                        sendNotification(newHours)
                    }
                }
                Result.success()
            } catch (e: Exception) {
                Log.e("Worker", "Error in worker", e)
                Result.failure()
            }
        }
    }

    private fun sendNotification(newValue: Int) {
        val context = applicationContext
        val channelId = "to_channel"

        // Перевірка чи дозволені сповіщення
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }

        // Для Android 13+ перевіряємо дозвіл явно
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        try {
            // Створення каналу (якщо потрібно)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Bike service",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "adding hours service notification"
                }

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            // Побудова сповіщення
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.svg_error)
                .setContentTitle("Updating time of ride")
                .setContentText("Was added automatically to the time of the hours driven -> $newValue")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            // Відправка сповіщення з обробкою винятків
            NotificationManagerCompat.from(context)
                .notify(newValue, notification)

        } catch (e: SecurityException) {
            // Логування помилки або інша обробка
            Log.e("Notification", "Permission denied for notifications", e)
        }
    }

}