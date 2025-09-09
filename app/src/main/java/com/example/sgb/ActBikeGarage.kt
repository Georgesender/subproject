package com.example.sgb

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.RectEvaluator
import android.animation.ValueAnimator
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
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import games.GamesMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit


class ActBikeGarage : AppCompatActivity() {
    private lateinit var bikeNameContainer: ViewGroup
    private lateinit var rootBikeInfo: ConstraintLayout
    private lateinit var rootScroll: ViewGroup
    private lateinit var bottomNav: ViewGroup
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
        window.setBackgroundDrawableResource(R.drawable.bg_bikegarageact)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_bikegarage_active)
        bikeDao = BikeDatabase.getDatabase(this).bikeDao()
        bikeId = intent.getIntExtra("bike_id" , -1)
        val session = intent.getStringExtra("Taked focus")
        // Знайдемо контейнери
        bikeNameContainer = findViewById(R.id.bike_name_container)
        rootBikeInfo       = findViewById(R.id.rootBikeinf)
        rootScroll = findViewById(R.id.rootScroll)
        bottomNav =findViewById(R.id.bottom_navigation)
        // ВСІ фони вже є — вони малюються автоматично.
        // А ВСІ внутрішні вьюшки поки що приховаємо через alpha=0:
        fadeOutChildren(rootScroll)
        fadeOutChildren(bikeNameContainer)
        fadeOutChildren(rootBikeInfo)
        fadeOutChildren(bottomNav)
        val testing = findViewById<Button>(R.id.left_button)

        //test section for setting start
// Функція для застосування режиму центрованого підмоделю
        fun applyCenterMode(enabled: Boolean) {
            val cs = ConstraintSet()
            cs.clone(rootBikeInfo)

            val sub = bikeSubmodelTextView
            val topMargin = (6 * resources.displayMetrics.density).toInt()
            val startMargin = (8 * resources.displayMetrics.density).toInt()

            if (enabled) {
                // центр по горизонталі — START/END до parent, але TOP залишається зверху з margin
                cs.clear(sub.id, ConstraintSet.START)
                cs.clear(sub.id, ConstraintSet.END)
                cs.connect(sub.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                cs.connect(sub.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                cs.connect(sub.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin)

                cs.setHorizontalBias(sub.id, 0.5f)
                // залишаємо вертикальний bias зверху (0f) — підмодель не буде зміщуватися по Y
                cs.setVerticalBias(sub.id, 0f)
            } else {
                // відновлюємо початкову позицію (start/top з margin)
                cs.clear(sub.id, ConstraintSet.START)
                cs.clear(sub.id, ConstraintSet.END)
                cs.clear(sub.id, ConstraintSet.TOP)
                cs.connect(sub.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, startMargin)
                cs.connect(sub.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin)
                cs.setHorizontalBias(sub.id, 0f)
                cs.setVerticalBias(sub.id, 0f)
            }

            cs.applyTo(rootBikeInfo)
        }


        val settingsButton = findViewById<ImageButton>(R.id.settings)
        settingsButton.setOnClickListener {
            val prefs = getSharedPreferences("bike_prefs", MODE_PRIVATE)
            val hiddenKey = "bike_${bikeId}_image_hidden"
            val centerKey = "bike_${bikeId}_center_submodel"
            val hideAllKey = "bike_${bikeId}_hide_bikeinf"
            val popup = PopupMenu(this, settingsButton)
            popup.menuInflater.inflate(R.menu.bike_settings_menu, popup.menu)

            // ініціалізація станів пунктів меню
            val isHidden = prefs.getBoolean(hiddenKey, false)
            val hideItem = popup.menu.findItem(R.id.menu_hide_image)
            hideItem.isChecked = isHidden
            hideItem.title = if (isHidden) "Show bike image" else "Hide bike image"

            val isCenter = prefs.getBoolean(centerKey, false)
            val centerItem = popup.menu.findItem(R.id.menu_centerize_no_size)
            centerItem.isChecked = isCenter
            centerItem.title = if (isCenter) "Show year" else "Hide year"

            val isHiddenBikeinf = prefs.getBoolean(hideAllKey, false)
            val hideAllItem = popup.menu.findItem(R.id.menu_hide_bikeinfo)
            hideAllItem.isChecked = isHiddenBikeinf
            hideAllItem.title = if (isHiddenBikeinf) "Show all bike sub info" else "Hide all bike sub info"


            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_delete_bike -> {
                        val sharedPreferences: SharedPreferences =
                            getSharedPreferences("bike_prefs", MODE_PRIVATE)
                        val bikeIdLocal = sharedPreferences.getInt("selected_bike_id", -1)
                        if (bikeIdLocal != -1) {
                            showDeleteConfirmation(bikeIdLocal)
                        }
                        true
                    }
                    R.id.menu_hide_image -> {
                        val newValue = !item.isChecked
                        item.isChecked = newValue
                        item.title = if (newValue) "Hide bike image ✓" else "Hide bike image"
                        prefs.edit { putBoolean(hiddenKey, newValue) }
                        bikeImageView.visibility = if (newValue) View.GONE else View.VISIBLE
                        true
                    }
                    R.id.menu_centerize_no_size -> {
                        val newValue = !item.isChecked
                        item.isChecked = newValue
                        item.title = if (newValue) "View only Submodel(series) ✓" else "View only Submodel(series)"
                        prefs.edit { putBoolean(centerKey, newValue) }

                        // застосовуємо зміни UI
                        applyCenterMode(newValue)

                        bikeYearTextView.visibility = if (newValue) View.GONE else View.VISIBLE
                        true
                    }
                    R.id.menu_hide_bikeinfo -> {
                        val newValue = !item.isChecked
                        item.isChecked = newValue
                        item.title = if (newValue) "Show all bike sub info" else "Hide all bike sub info"
                        prefs.edit { putBoolean(hideAllKey, newValue) }
                        rootBikeInfo.visibility = if (newValue) View.GONE else View.VISIBLE
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }


        //test section end
        // Перевіряємо, чи bikeId дійсне
        if (bikeId != -1) {
            lifecycleScope.launch {
                val bike = bikeDao.getBikeById(bikeId)
// після того, як встановили картинку (або завантажили)
                val prefs = getSharedPreferences("bike_prefs", MODE_PRIVATE)
                val hiddenKey = "bike_${bikeId}_image_hidden"
                val isHidden = prefs.getBoolean(hiddenKey, false)
// ДОБАВТЕ ОЦЕ: (для center-mode)
                val centerKey = "bike_${bikeId}_center_submodel"
                val isCenter = prefs.getBoolean(centerKey, false)
                applyCenterMode(isCenter)
                // NEXT
                val hideAllKey = "bike_${bikeId}_hide_bikeinf"
                val isHiddenBikeinf = prefs.getBoolean(hideAllKey, false)
                rootBikeInfo.visibility = if (isHiddenBikeinf) View.GONE else View.VISIBLE

                // Якщо байк знайдений
                if (bike != null) {
                    saveSelectedBikeId(bikeId)
                    val getBikeName =
                        getString(R.string.two_strings , bike.brand , bike.modelsJson.keys.first())


                    // Отримуємо рік зі структури BikeSubmodel
                    val years = bike.modelsJson.values.first().submodels.values.first().years
                    val year = years.keys.first() // Отримуємо перший рік з карти
                        bikeYearTextView.text = year
                    bikeYearTextView.visibility = if (isCenter) View.GONE else View.VISIBLE



                    // Завантажуємо зображення байка
                    if (bike.addedImgBikeUri == null) {
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

                    bikeImageView.visibility = if (isHidden) View.GONE else View.VISIBLE
                    if (session == "Yes") {
                        bikeNameTextView.text = getBikeName
                    } else {
                        startTypewriterAnimation(bikeNameTextView , getBikeName , 50)
                    }
                }
            }
        }


        // Buttons >>>>
        val service = findViewById<ConstraintLayout>(R.id.service)
        service.setOnClickListener {

            val intent = Intent(this , ActService::class.java)
            intent.putExtra("bike_id" , bikeId)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            startActivity(intent , options.toBundle())
        }

        val compGeometry = findViewById<ConstraintLayout>(R.id.componentsGeometry)
        compGeometry.setOnClickListener {

            val intent = Intent(this , ActComponentsGeometry::class.java)
            intent.putExtra("bike_id" , bikeId)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_outnormal
            )

            startActivity(intent , options.toBundle())
        }

        val frameGeometry = findViewById<ConstraintLayout>(R.id.frameGeometry)
        frameGeometry.setOnClickListener {
            // 1) Перші дві fadeOut-анімації,
            //    вони мають тривалість ~400ms кожна
            animationFrameGeometryBike()
            animationFrameGeometry()

            // 2) Одразу ховаємо bottom_navigation
            bottomNav.visibility = View.GONE

            // 3) Через delay чекаємо 800ms (кінець двох fadeOut’ів)
            Handler(Looper.getMainLooper()).postDelayed({
                // отримаємо аніматор
                val shrinkAnimator = animateShrinkRoot()

                // 4) Коли shrink-анімація закінчиться — стартуємо новий Activity
                shrinkAnimator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        val intent = Intent(this@ActBikeGarage, ActBikeGeometry::class.java)

                        intent.putExtra("bike_id", bikeId)

                        val options = ActivityOptionsCompat.makeCustomAnimation(
                            this@ActBikeGarage , 0, R.anim.fade_outnormal
                        )

                        startActivity(intent , options.toBundle())
                        finish()
                    }
                })

                // 5) І запускаємо shrink-анімацію
                shrinkAnimator.start()

            }, /* delayMillis = */ 800L)
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


        testing.setOnClickListener {
            val intent = Intent(this , GarageActivity::class.java)
            val option = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            startActivity(intent , option.toBundle())
        }
        // Buttons <<<<

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context? , intent: Intent?) {
                checkNotificationSettings()
            }
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver , IntentFilter("NOTIFICATION_SETTINGS_CHANGED"))

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
    // Коли система завершить анімацію самого переходу
    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        // Запускаємо свій “fade-in” лише для внутрішнього контенту:
        fadeInChildren(bikeNameContainer, duration = 400L)
        fadeInChildren(rootBikeInfo,       duration = 700L, startDelay = 400L)
        fadeInChildren(rootScroll, duration = 900L, startDelay =400L)
        fadeInChildren(bottomNav, duration = 200L)
    }

    // Робимо alpha=0 для всіх прямих дітей (вмісту) контейнера
    private fun fadeOutChildren(container: ViewGroup) {
        for (i in 0 until container.childCount) {
            container.getChildAt(i).alpha = 0f
        }
    }

    // Анімація alpha від 0 до 1 для дітей
    private fun fadeInChildren(
        container: ViewGroup,
        duration: Long,
        startDelay: Long = 0L
    ) {
        for (i in 0 until container.childCount) {
            container.getChildAt(i)
                .animate()
                .alpha(1f)
                .setStartDelay(startDelay)
                .setDuration(duration)
                .start()
        }
    }
    private fun initViews() {
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

        bikeSubmodelTextView.text = bike.modelsJson.values.first().submodels.keys.first()

    }


    private fun setupBottomNavigation() {
        val navHome = findViewById<TextView>(R.id.current_bike)
        val navGames = findViewById<TextView>(R.id.games)

// garage test
        navGames.setOnClickListener {
            val intent = Intent(this , GamesMenu::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            intent.putExtra("bike_id", bikeId)
            startActivity(intent , options.toBundle())
            finish()
        }

        navHome.setTypeface(null , Typeface.BOLD)
        navHome.textSize = navHome.textSize / resources.displayMetrics.density + 10
    }

    //========== Delete function ==========
    private fun showDeleteConfirmation(bikeId: Int) {
        // inflate custom view
        val dialogView = layoutInflater.inflate(R.layout.di_confirm_delete , null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent , theme)
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
            getSharedPreferences("bike_prefs" , MODE_PRIVATE).edit {
                remove("selected_bike_id")
            }

            // Переходимо до PreAddBikeActivity
            startActivity(Intent(this@ActBikeGarage , SplashActivity::class.java))
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
        val prefs = getSharedPreferences("bike_prefs" , MODE_PRIVATE)
        if (prefs.getBoolean("receive_notifications" , true)) {
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
        val prefs = getSharedPreferences("maintenance" , MODE_PRIVATE)
        if (!prefs.contains("cycle_step")) {
            prefs.edit {
                putInt("cycle_step" , -1)               // було 0 → стало -1
                putLong("last_step_time" , System.currentTimeMillis())
            }
            scheduleNextCycle(61)
        }
    }


    private fun scheduleNextCycle(delayDays: Long) {
        val workRequest = OneTimeWorkRequestBuilder<CycleWorker>()
            .setInitialDelay(delayDays , TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork("maintenance_cycle" , ExistingWorkPolicy.REPLACE , workRequest)
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
            val prefs = applicationContext.getSharedPreferences("bike_prefs" , MODE_PRIVATE)

            // Перевірка, чи дозволені сповіщення
            if (!prefs.getBoolean("receive_notifications" , true)) {
                Log.d(TAG , "Notifications are disabled, skipping work")
                return Result.success()
            }

            // Отримуємо поточний стан
            var currentStep = prefs.getInt("cycle_step" , 0)
            prefs.getLong("last_step_time" , 0L)

            // Відправляємо сповіщення на основі ПОТОЧНОГО кроку
            when (currentStep) {
                0 , 1 , 3 , 4 -> sendNotification("50h" , "50" , "50")
                2 -> sendNotification("100h" , "100" , "100")
                5 -> sendNotification("year" , "100" , "100")
            }

            // Інкрементуємо крок (0-5 циклічно)
            currentStep = (currentStep + 1) % 6

            // Зберігаємо новий стан
            prefs.edit {
                putInt("cycle_step" , currentStep)
                putLong("last_step_time" , System.currentTimeMillis())
            }

            // Плануємо наступний запуск через 61 день
            scheduleNextCycle(61)

            return Result.success()
        }

        private fun scheduleNextCycle(delayDays: Long) {
            val workRequest = OneTimeWorkRequestBuilder<CycleWorker>()
                .setInitialDelay(delayDays , TimeUnit.DAYS)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "maintenance_cycle" ,
                ExistingWorkPolicy.REPLACE ,
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
                .override(500 , 500)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.img_bike_placeholder)
                .error(R.drawable.img_bike_placeholder)
                .into(btn)
        }
    }

    private fun startTypewriterAnimation(
        view: TextView ,
        text: String ,
        delay: Long = 50 ,
        onEnd: (() -> Unit)? = null
    ) {
        view.text = "" // Очищаємо TextView перед початком
        lifecycleScope.launch {
            text.forEachIndexed { _ , char ->
                withContext(Dispatchers.Main) {
                    view.append(char.toString())
                }
                delay(delay)
            }
            onEnd?.invoke()
        }
    }



    private fun animationFrameGeometry() {
        val root = findViewById<ConstraintLayout>(R.id.rootBikeinf)

        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            // кожного разу новий екземпляр анімації
            val fadeOut = AnimationUtils.loadAnimation(this , R.anim.fade_outnormal)

            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationRepeat(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    child.visibility = View.INVISIBLE
                }
            })

            child.startAnimation(fadeOut)
        }
    }

    private fun animationFrameGeometryBike() {
        val scrollRoot = findViewById<LinearLayout>(R.id.rootScroll)

        for (i in 0 until scrollRoot.childCount) {
            val child = scrollRoot.getChildAt(i)
            // кожного разу новий екземпляр анімації
            val fadeOut = AnimationUtils.loadAnimation(this , R.anim.fade_outnormal)

            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationRepeat(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    child.visibility = View.INVISIBLE
                }
            })

            child.startAnimation(fadeOut)
        }
    }
    private fun animateShrinkRoot(): Animator {
        // 1. Знаходимо в’юшки
        val root     = findViewById<ConstraintLayout>(R.id.rootBikeinf)
        val leftBtn  = findViewById<View>(R.id.left_button)
        val rightBtn = findViewById<View>(R.id.settings)

        // 2. Конвертуємо 300dp → px для контейнера
        val targetPx   = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 300f, resources.displayMetrics
        ).toInt()
        val startH     = root.height

        // 3. Height-анімація контейнера
        val heightAnim = ValueAnimator.ofInt(startH, targetPx).apply {
            duration       = 750L
            interpolator   = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val newH = anim.animatedValue as Int
                root.layoutParams = root.layoutParams.apply { height = newH }
                root.requestLayout()
            }
        }

        // 4. Підготувати Rect для clipBounds
        val lw = leftBtn.width
        val lh = leftBtn.height
        val rw = rightBtn.width
        val rh = rightBtn.height

        // Ліва кнопка: clip справа → затираємо з правого краю
        val leftStartRect = Rect(0, 0, lw, lh)
        val leftEndRect   =  Rect(0, 0, 0, lh)  // ширина 0, “справа” → “зліва”
        val leftClipAnim = ObjectAnimator.ofObject(
            leftBtn, "clipBounds",
            RectEvaluator() ,
            leftStartRect,
            leftEndRect
        ).apply {
            duration     = 750L
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Права кнопка: clip зліва → затираємо з лівого краю
        val rightStartRect = Rect(0, 0, rw, rh)
        val rightEndRect   = Rect(rw, 0, rw, rh)    // ширина 0, “зліва” → “справа”
        val rightClipAnim = ObjectAnimator.ofObject(
            rightBtn, "clipBounds",
            RectEvaluator(),
            rightStartRect,
            rightEndRect
        ).apply {
            duration     = 750L
            interpolator = AccelerateDecelerateInterpolator()
        }

        // 5. Запускаємо всі разом
        return AnimatorSet().apply {
            playTogether(heightAnim, leftClipAnim, rightClipAnim)
        }
    }
}