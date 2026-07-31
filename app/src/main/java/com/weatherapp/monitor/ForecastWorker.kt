package com.weatherapp.monitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.weatherapp.MainActivity
import com.weatherapp.R

class ForecastWorker(context: Context,
                     params: WorkerParameters) : Worker(context, params) {
    companion object {
        private const val CHANNEL_ID: String = "WEATHER_APP"
    }
    override fun doWork(): Result {
        val cityName = inputData.getString("city") ?: return Result.failure()
        Log.d("ForecastWorker", "Iniciando trabalho para a cidade: $cityName")
        showNotification(cityName)
        return Result.success()
    }
    @SuppressLint("MissingPermission")
    private fun showNotification(cityName: String) {
        Log.d("ForecastWorker", "Exibindo notificação para: $cityName")
        val newIntent = Intent(
            this.applicationContext,
            MainActivity::class.java
        )
        newIntent.addFlags(
            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP)
        newIntent.putExtra("city", cityName)
        val pendingIntent = PendingIntent.getActivity(
            this.applicationContext, cityName.hashCode(), newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        
        val builder = NotificationCompat
            .Builder(this.applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(cityName)
            .setContentText("Clique para ver previsão do tempo atualizada.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        val notificationManager: NotificationManager =
            this.applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, 
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("ForecastWorker", "Permissão POST_NOTIFICATIONS não concedida")
                return
            }
        }

        // ID = hashCode: para substituir ou remover notificações
        notificationManager.notify(cityName.hashCode(), builder.build())
        Log.d("ForecastWorker", "Notificação enviada com sucesso")
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val name = "WeatherApp"
        val descriptionText = "WeatherApp Notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
            .apply { description = descriptionText }
        val notificationManager: NotificationManager = this.applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}