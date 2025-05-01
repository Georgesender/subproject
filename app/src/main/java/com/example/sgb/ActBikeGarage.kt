package com.example.sgb

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.sgb.room.Bike
import com.example.sgb.room.BikeDao
import com.example.sgb.room.BikeDatabase
import com.example.sub.R
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

class ActBikeGarage : AppCompatActivity() {
    private var bikeId: Int = -1
    private lateinit var bikeDao: BikeDao
    private lateinit var bikeNameTextView: TextView
    private lateinit var bikeSubmodelTextView: TextView
    private lateinit var bikeYearTextView: TextView
    private lateinit var bikeImageView: ImageView
    private val REQUEST_NOTIFICATION_PERMISSION = 1001
    private lateinit var receiver: BroadcastReceiver

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this ,
                "Сповіщення вимкнено - автоматичні оновлення не будуть показуватись" ,
                Toast.LENGTH_LONG
            ).show()
        }
    }


    @SuppressLint("DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_bikegarage_active)
        bikeDao = BikeDatabase.getDatabase(this).bikeDao()
        bikeId = intent.getIntExtra("bike_id" , -1)


        // Перевіряємо, чи bikeId дійсне
        if (bikeId != -1) {
            lifecycleScope.launch {
                val bikeDao = BikeDatabase.getDatabase(this@ActBikeGarage).bikeDao()
                val bike = bikeDao.getBikeById(bikeId)

                // Якщо байк знайдений
                if (bike != null) {
                    saveSelectedBikeId(bikeId)
                    bikeNameTextView.text =
                        getString(R.string.two_strings , bike.brand , bike.modelsJson.keys.first())
                    bikeSubmodelTextView.text =
                        bike.modelsJson.values.first().submodels.keys.first()
                    // Отримуємо рік зі структури BikeSubmodel
                    val years = bike.modelsJson.values.first().submodels.values.first().years
                    val year = years.keys.first() // Отримуємо перший рік з карти
                    bikeYearTextView.text = year

                    // Завантажуємо зображення байка
                    if(bike.addedImgBikeUri == null) {
                        val imageRes = bike.modelsJson
                            .values.first()
                            .submodels
                            .values.first()
                            .imageRes
                        if (imageRes != null) {
                            bikeImageView.setImageResource(imageRes)
                        }
                    } else {
                        loadPhotoIntoPlaceholder(bike.addedImgBikeUri.toUri())
                    }

                }
            }
        }


        // Buttons >>>>
        val service = findViewById<ConstraintLayout>(R.id.service)
        service.setOnClickListener{
            val intent = Intent(this , ActService::class.java)
            intent.putExtra("bike_id" , bikeId)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            startActivity(intent , options.toBundle())
        }
        val clearButton = findViewById<ImageButton>(R.id.right_button_2)
        clearButton.setOnClickListener {
            val sharedPreferences: SharedPreferences =
                getSharedPreferences("bike_prefs" , MODE_PRIVATE)
            val bikeId = sharedPreferences.getInt("selected_bike_id" , -1)
            if (bikeId != -1) {
                showDeleteConfirmation(bikeId)
            }
        }

        val compGeometry = findViewById<ConstraintLayout>(R.id.componentsGeometry)
        compGeometry.setOnClickListener {
            val intent = Intent(this , ActComponentsGeometry::class.java)
            intent.putExtra("bike_id" , bikeId)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            startActivity(intent , options.toBundle())
        }

        val frameGeometry = findViewById<ConstraintLayout>(R.id.frameGeometry)
        frameGeometry.setOnClickListener {
            val intent = Intent(this , ActBikeGeometry::class.java)
            intent.putExtra("bike_id" , bikeId) // Передаємо bikeId
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            startActivity(intent , options.toBundle())
        }

        val setupBike = findViewById<ConstraintLayout>(R.id.setups)
        setupBike.setOnClickListener {
            val intent = Intent(this , ActSetups::class.java)
            intent.putExtra("bike_id" , bikeId)
            val option = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            startActivity(intent , option.toBundle())
        }

        val testing = findViewById<Button>(R.id.left_button)
        testing.setOnClickListener{
            val intent = Intent(this , GarageActivity::class.java)
            val option = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            startActivity(intent , option.toBundle())
        }
        // Buttons <<<<

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                checkNotificationSettings()
            }
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter("NOTIFICATION_SETTINGS_CHANGED"))

        // Functions initialisation >>>>
        checkFirstLaunch()
        checkAndRequestNotificationPermission()
        initViews()
        checkNotificationSettings()
        setupBottomNavigation()
        // Functions initialisation <<<<
    }
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    private fun initViews(){
        bikeNameTextView = findViewById(R.id.bike_name)
        bikeSubmodelTextView = findViewById(R.id.bike_submodel)
        bikeYearTextView = findViewById(R.id.bike_year)
        bikeImageView = findViewById(R.id.bike_image)
    }

    private fun saveSelectedBikeId(bikeId: Int) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs" , MODE_PRIVATE)
        sharedPreferences.edit {
            putInt("selected_bike_id" , bikeId)
        }
        startObservingBike()
    }
    private fun startObservingBike() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bikeDao.getBikeByIdFlow(bikeId).collect { bike ->
                    bike?.let { updateUI(it) }
                }
            }
        }
    }
    private fun updateUI(bike: Bike) {
        bikeNameTextView.text =
            getString(R.string.two_strings , bike.brand , bike.modelsJson.keys.first())
        bikeYearTextView.text =
            bike.modelsJson.values.first().submodels.values.first().years.keys.first()
        bikeSubmodelTextView.text = bike.modelsJson.values.first().submodels.keys.first()

        // Додаємо оновлення лічильника годин
        val hoursText = "${bike.elapsedHoursValue} год."
        findViewById<TextView>(R.id.elapsed_hours_value).text = hoursText
    }


    private fun setupBottomNavigation() {
        val navHome = findViewById<TextView>(R.id.nav_home)
        val navCompCheck = findViewById<TextView>(R.id.nav_setups)
        val navDiscover = findViewById<TextView>(R.id.nav_etc)

        navCompCheck.setOnClickListener {
            val intent = Intent(this , ComponentsCheker::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            startActivity(intent , options.toBundle())
            finish()
        }
// garage test
        navDiscover.setOnClickListener {
            val intent = Intent(this , GarageActivity::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            startActivity(intent , options.toBundle())
            finish()
        }

        navHome.setTypeface(null , Typeface.BOLD)
        navHome.textSize = navHome.textSize / resources.displayMetrics.density + 10
    }
    //========== Delete function ==========
    private fun showDeleteConfirmation(bikeId: Int) {
        // inflate custom view
        val dialogView = layoutInflater.inflate(R.layout.di_confirm_delete, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialog.window?.setBackgroundDrawable(transparentColor.toDrawable())
        // кнопка "Ні" — просто закрити
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        // кнопка "Так" — виконати видалення
        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            dialog.dismiss()
            performDeleteBike(bikeId)
        }

        dialog.show()
    }
    // Перейменували: тепер це внутрішня логіка видалення
    private fun performDeleteBike(bikeId: Int) {
        lifecycleScope.launch {
            val db = BikeDatabase.getDatabase(this@ActBikeGarage)
            with(db) {
                geometryDao().deleteGeometryByBikeId(bikeId)
                bikeDao().deleteBikeById(bikeId)
                bpSetupDao().deleteBikeParkSetupByBikeId(bikeId)
                serviceRecordDao().deleteRecordsByBikeId(bikeId)
                setupDao().deleteSetupsByBikeId(bikeId)
                componentsDao().deleteComponentsByBikeId(bikeId)
            }

            // Очищаємо shared prefs
            getSharedPreferences("bike_prefs", MODE_PRIVATE).edit {
                remove("selected_bike_id")
            }

            // Переходимо до PreAddBikeActivity
            startActivity(Intent(this@ActBikeGarage, MainBikeGarage::class.java))
            finish()
        }
    }
    //========== Delete function ==========

// Notifications functions >>>>
    private fun checkFirstLaunch() {
        val prefs = getSharedPreferences("app_prefs" , MODE_PRIVATE)
        if (!prefs.getBoolean("has_seen_warning" , false)) {
            showWarningDialog()
        }
    }

    private fun showWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle("Попередження")
            .setMessage("Запущений лічильник часу для ТО. Вимкнути його можна у вкладці \"Технічне обслуговування\".")
            .setPositiveButton("OK") { dialog , _ ->
                dialog.dismiss()
                getSharedPreferences("app_prefs" , MODE_PRIVATE).edit {
                    putBoolean("has_seen_warning" , true)
                }
                scheduleWeeklyUpdate()
            }
            .setCancelable(false)
            .show()
    }

    private fun scheduleWeeklyUpdate() {
        val prefs = getSharedPreferences("app_prefs" , MODE_PRIVATE)
        if (prefs.getBoolean("work_scheduled" , false)) return

        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .build()

        // Мінімальний інтервал 15 хвилин
        val workRequest = PeriodicWorkRequestBuilder<WorkerHourIncrement>(
            7 , // Кількість одиниць
            TimeUnit.DAYS // Мінімальний інтервал
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "hour_increment_work" ,
            ExistingPeriodicWorkPolicy.KEEP ,
            workRequest
        )

        prefs.edit { putBoolean("work_scheduled" , true) }
    }
    private fun checkNotificationSettings() {
        val prefs = getSharedPreferences("bike_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("receive_notifications", true)) {
            initNotifications()
        } else {
            cancelScheduledNotifications()
        }
    }

    private fun cancelScheduledNotifications() {
        WorkManager.getInstance(this).cancelUniqueWork("maintenance_cycle")
        WorkManager.getInstance(this).cancelUniqueWork("hour_increment_work")
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this ,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Дозвіл надано
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {

                }

                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }


    private fun initNotifications() {
        val prefs = getSharedPreferences("maintenance", MODE_PRIVATE)
        if (!prefs.contains("cycle_step")) {
            prefs.edit {
                putInt("cycle_step", -1)               // було 0 → стало -1
                putLong("last_step_time", System.currentTimeMillis())
            }
            scheduleNextCycle(61)
        }
    }



    private fun scheduleNextCycle(delayDays: Long) {
        val workRequest = OneTimeWorkRequestBuilder<CycleWorker>()
            .setInitialDelay(delayDays, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork("maintenance_cycle", ExistingWorkPolicy.REPLACE, workRequest)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int ,
        permissions: Array<out String> ,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults)

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Дозвіл на сповіщення надано — відправляємо всі відкладені сповіщення
                scheduleNextCycle(61)
            } else {
                // Дозвіл не надано — показуємо користувачу коротке повідомлення
                Toast.makeText(
                    this , "Потрібен дозвіл на сповіщення, щоб отримувати нагадування" ,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    class CycleWorker(
        context: Context ,
        params: WorkerParameters
    ) : CoroutineWorker(context , params) {


        override suspend fun doWork(): Result {
            val prefs = applicationContext.getSharedPreferences("bike_prefs", Context.MODE_PRIVATE)

            // Перевірка, чи дозволені сповіщення
            if (!prefs.getBoolean("receive_notifications", true)) {
                Log.d(TAG, "Notifications are disabled, skipping work")
                return Result.success()
            }

            // Отримуємо поточний стан
            var currentStep = prefs.getInt("cycle_step", 0)
            prefs.getLong("last_step_time", 0L)

            // Відправляємо сповіщення на основі ПОТОЧНОГО кроку
            when (currentStep) {
                0, 1, 3, 4 -> sendNotification("50h", "50", "50")
                2 -> sendNotification("100h", "100", "100")
                5 -> sendNotification("year", "100", "100")
            }

            // Інкрементуємо крок (0-5 циклічно)
            currentStep = (currentStep + 1) % 6

            // Зберігаємо новий стан
            prefs.edit {
                putInt("cycle_step", currentStep)
                putLong("last_step_time", System.currentTimeMillis())
            }

            // Плануємо наступний запуск через 61 день
            scheduleNextCycle(61)

            return Result.success()
        }

        private fun scheduleNextCycle(delayDays: Long) {
            val workRequest = OneTimeWorkRequestBuilder<CycleWorker>()
                .setInitialDelay(delayDays, TimeUnit.DAYS)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "maintenance_cycle",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        private fun sendNotification(type: String , title: String , message: String) {
            val context = applicationContext

            // Перевіряємо, чи глобально увімкнені сповіщення в налаштуваннях додатка
            if (!areNotificationsEnabled(context)) return

            // На Android 13+ потрібний runtime-дозвіл POST_NOTIFICATIONS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context ,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    // Якщо дозвіл не надано — краще або попросити його в Activity,
                    // або просто повернутися, щоб уникнути Crash
                    Log.w(TAG , "Notification permission not granted")
                    return
                }
            }

            val channelId = when (type) {
                "year" -> "year_channel"
                else -> "maintenance_channel"
            }

            createNotificationChannel(channelId , "Сповіщення ТО")

            val notification = NotificationCompat.Builder(context , channelId)
                .setSmallIcon(R.drawable.svg_error)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            try {
                NotificationManagerCompat.from(context)
                    .notify(type.hashCode() , notification)
            } catch (e: SecurityException) {
                // Логування або інша обробка, якщо користувач відкликав дозвіл вручну
                Log.e(TAG , "Cannot send notification — permission denied" , e)
            }
        }

        private fun areNotificationsEnabled(context: Context): Boolean {
            return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context ,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED)
        }

        private fun createNotificationChannel(channelId: String , name: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId ,
                    name ,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Сповіщення про технічне обслуговування" }

                val manager = applicationContext.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }
    }
    private fun loadPhotoIntoPlaceholder(uri: Uri) {
        findViewById<ImageView>(R.id.bike_image).also { btn ->
            Glide.with(this)
                .load(uri)
                .override(500, 500)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.img_fork)
                .error(R.drawable.img_fork)
                .into(btn)
        }
    }

}

