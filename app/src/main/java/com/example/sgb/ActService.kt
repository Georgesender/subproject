package com.example.sgb

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.ServiceRecord
import com.example.sgb.room.ServiceRecordDao
import com.example.sub.R
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class ActService : AppCompatActivity() {
    private var bikeId: Int = -1
    companion object {
        private val STEP_INTERVAL_MS = TimeUnit.DAYS.toMillis(61)
    }

    private var updateJob: Job? = null
    private lateinit var contentContainer: LinearLayout
    private var sus_50: TextView? = null
    private var sus_100: TextView? = null
    private var sus_year: TextView? = null
    private var valueElapsed: EditText? = null
    private var btnIncrement: Button? = null
    private lateinit var addNewCardButton: Button

    // State caches
    private var lastT50: String? = null
    private var lastT100: String? = null
    private var lastTYear: String? = null
    private var cachedStartTime: Long = 0L

    //ServiceCards
    private lateinit var serviceCardsContainer: LinearLayout
    private lateinit var serviceRecordDao: ServiceRecordDao

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_service)

        bikeId = intent.getIntExtra("bike_id" , -1)
        val database = BikeDatabase.getDatabase(this)
        serviceRecordDao = database.serviceRecordDao()

        val backButton = findViewById<Button>(R.id.back)

        backButton.setOnClickListener {
            val intent = Intent(this, ActBikeGarage::class.java).apply {
                putExtra("bike_id", bikeId)
            }
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this, R.anim.fade_in_faster, R.anim.fade_out_faster
            )
            startActivity(intent, options.toBundle())
            finish()
        }
        // Ініціалізація фіксованого стартового часу при першому запуску
        // Кешуємо start_time при ініціалізації
        val prefs = getSharedPreferences("maintenance", MODE_PRIVATE)
        cachedStartTime = prefs.getLong("start_time", System.currentTimeMillis()).also {
            if (!prefs.contains("start_time")) prefs.edit { putLong("start_time", it) }
        }





// testiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiing
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        contentContainer = findViewById(R.id.contentContainer)

        // Setup tabs
        tabLayout.addTab(tabLayout.newTab().setText("Сервіс"), true)
        tabLayout.addTab(tabLayout.newTab().setText("Сповіщення"))
        showServiceContent()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showServiceContent()
                    1 -> showNotificationSettings()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }

    private suspend fun updateRemainingTimes(now: Long) {
        val rem50Ms = calcRemMs(cachedStartTime, 1 * STEP_INTERVAL_MS, now)
        val rem100Ms = calcRemMs(cachedStartTime, 3 * STEP_INTERVAL_MS, now)
        val remYearMs = calcRemMs(cachedStartTime, 6 * STEP_INTERVAL_MS, now)

        // Форматуємо тільки при зміні значень
        val t50 = formatDuration(rem50Ms).takeIf { it != lastT50 }
        val t100 = formatDuration(rem100Ms).takeIf { it != lastT100 }
        val tYear = formatDuration(remYearMs).takeIf { it != lastTYear }

        withContext(Dispatchers.Main) {
            t50?.let {
                sus_50?.text = it
                lastT50 = it
            }
            t100?.let {
                sus_100?.text = it
                lastT100 = it
            }
            tYear?.let {
                sus_year?.text = it
                lastTYear = it
            }
        }
    }
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let { view ->
                if (view is EditText) {
                    val outRect = Rect().apply { view.getGlobalVisibleRect(this) }
                    // якщо торкнулися поза межами поточного EditText
                    if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                        view.clearFocus()
                        hideKeyboard(view)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Функція для відображення сервісного вмісту
    private fun showServiceContent() {
        contentContainer.removeAllViews()
        val serviceView = layoutInflater.inflate(R.layout.tab_service_main, contentContainer, false)
        contentContainer.addView(serviceView)

        // Ініціалізація елементів з поточної вкладки
        sus_50 = serviceView.findViewById(R.id.sus_date50)
        sus_100 = serviceView.findViewById(R.id.sus_date100)
        sus_year = serviceView.findViewById(R.id.sus_dateyear)
        btnIncrement = serviceView.findViewById(R.id.btn_increment_hours)
        valueElapsed = serviceView.findViewById(R.id.elapsed_hours_value)

        // Ініціалізація нових елементів
        serviceCardsContainer = serviceView.findViewById(R.id.serviceCardContainer)
        addNewCardButton = serviceView.findViewById(R.id.addNewCard)

        addNewCardButton.setOnClickListener { showDatePicker() }

        // Завантажити існуючі записи
        loadServiceRecords()

        // Додати слухачі подій для НОВИХ елементів
        btnIncrement?.setOnClickListener {
            val current = valueElapsed?.text.toString().toIntOrNull() ?: 0
            val incremented = current + 1
            valueElapsed?.setText(incremented.toString())
            saveHoursToDb(incremented)
        }

        valueElapsed?.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newValue = v.text.toString().toIntOrNull() ?: 0
                saveHoursToDb(newValue)
                true
            } else false
        }

        // Оновити години при відкритті вкладки
        loadHour()
        // запускаємо цикл тільки ПІСЛЯ того, як привʼязали вʼюхи
        updateJob?.cancel()  // на всяк випадок
        updateJob = lifecycleScope.launch {
            // одразу показуємо актуальний час
            updateRemainingTimes(System.currentTimeMillis())
            // потім оновлюємо щосекунди
            while (isActive) {
                delay(1000)
                updateRemainingTimes(System.currentTimeMillis())
            }
        }
    }
    private fun showNotificationSettings() {
        contentContainer.removeAllViews()
        val serviceView = layoutInflater.inflate(R.layout.tab_notifications_setting, contentContainer, false)
        contentContainer.addView(serviceView)

        // Ініціалізація елементів керування автоінкрементом
        val switchAutoInc = serviceView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_autoinc)
        val editAutoIncValue = serviceView.findViewById<EditText>(R.id.et_autoinc_days)

        // Завантаження збережених налаштувань
        val prefs = getSharedPreferences("bike_prefs", Context.MODE_PRIVATE)
        switchAutoInc.isChecked = prefs.getBoolean("auto_inc_enabled", false)
        editAutoIncValue.isEnabled = switchAutoInc.isChecked
        editAutoIncValue.setText(prefs.getInt("auto_inc_value", 7).toString())

                // Обробник змін стану перемикача
                switchAutoInc.setOnCheckedChangeListener { _, isChecked ->
            editAutoIncValue.isEnabled = isChecked
            prefs.edit { putBoolean("auto_inc_enabled", isChecked) }

            if (isChecked && editAutoIncValue.text.isNullOrEmpty()) {
                editAutoIncValue.setText("7")
                prefs.edit { putInt("auto_inc_value", 7) }
            }
        }

                // Обробник зміни значення інкременту
                editAutoIncValue.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.takeIf { it > 0 }?.let {
                    prefs.edit { putInt("auto_inc_value", it) }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
        // Світчер для сповіщень ТО
        val switchNotifications = serviceView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_receive_notifications)
        switchNotifications.isChecked = prefs.getBoolean("receive_notifications", true)

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit {
                putBoolean("receive_notifications", isChecked)
                apply()
            }

            // Синхронізація з ActBikeGarage через Broadcast
            sendNotificationSettingsUpdate()

            if (isChecked) {
                Toast.makeText(this, "Сповіщення увімкнено", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Сповіщення вимкнено", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun sendNotificationSettingsUpdate() {
        val intent = Intent("NOTIFICATION_SETTINGS_CHANGED")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    // Функція збереження в Room
    private fun saveHoursToDb(value: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            BikeDatabase
                .getDatabase(this@ActService)
                .bikeDao()
                .updateElapsedHours(bikeId, value)
        }
    }
    private fun loadHour() {
        lifecycleScope.launch {
            val bike = BikeDatabase.getDatabase(this@ActService)
                .bikeDao()
                .getBikeById(bikeId)
            val hoursText = "${bike?.elapsedHoursValue}"
            // Оновити valueElapsed поточної вкладки
            valueElapsed?.setText(hoursText)
        }
    }
    private fun calcRemMs(startTime: Long, offsetMs: Long, now: Long): Long {
        // цільовий час події
        val targetTime = startTime + offsetMs
        return maxOf(0L, targetTime - now)
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).let { if (ms % 1000 == 0L && it > 0) it - 1 else it }
        val days = totalSeconds / 86400
        val remaining = totalSeconds % 86400
        return String.format(
            Locale.getDefault(),
            "  %dдн\n %02d:%02d:%02d",
            days,
            remaining / 3600,
            (remaining % 3600) / 60,
            remaining % 60
        )
    }

    //ServiceCard func >>>>>>>>>>
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, day)
            }.timeInMillis

            createNewServiceCard(selectedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun createNewServiceCard(date: Long) {
        val cardView = layoutInflater.inflate(R.layout.blank_service_card,serviceCardsContainer, false)

        val dateText = cardView.findViewById<TextView>(R.id.dateText)
        val titleInput = cardView.findViewById<EditText>(R.id.titleInput)
        val notesInput = cardView.findViewById<EditText>(R.id.notesInput)
        val priceInput = cardView.findViewById<EditText>(R.id.priceInput)


        dateText.text = formatDate(date)

        setupTextWatchers(date, titleInput, notesInput, priceInput)

        serviceCardsContainer.addView(cardView, 0)
        saveNewRecord(date)
    }

    private fun setupTextWatchers(date: Long, vararg editTexts: EditText) {
        editTexts.forEach { editText ->


            // Дебаунс-слухач
            var updateJob: Job? = null
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateJob?.cancel()
                    updateJob = lifecycleScope.launch {
                        delay(300)                         // чекаємо 300 мс після останньої зміни
                        updateRecordInDatabase(date, editTexts)
                    }
                }
            })

            // Done → ховаємо клавіатуру + знімаємо фокус
            editText.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    v.clearFocus()
                    hideKeyboard(v)
                    true
                } else false
            }
        }
    }


    private fun updateRecordInDatabase(date: Long, inputs: Array<out EditText>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val record = serviceRecordDao.getRecordByDate(date, bikeId) ?: let {
                ServiceRecord(date = date, bikeId = bikeId).also {
                    serviceRecordDao.insert(it)
                }
            }

            withContext(Dispatchers.Main) {
                record.title = inputs[0].text.toString()
                record.notes = inputs[1].text.toString()
                record.price = inputs[2].text.toString()
            }

            serviceRecordDao.update(record)
        }
    }

    private fun saveNewRecord(date: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val newRecord = ServiceRecord(
                date = date,
                bikeId = bikeId,
                title = "",
                notes = "",
                price = ""
            )
            serviceRecordDao.insert(newRecord)
        }
    }

    private fun loadServiceRecords() {
        lifecycleScope.launch {
            var previousDates = emptyList<Long>()
            // getRecordsForBike() – дає Flow<List<ServiceRecord>>
            serviceRecordDao.getRecordsForBike(bikeId)
                .collect { records ->
                    // витягуємо тільки ключі (дати)
                    val dates = records.map { it.date }
                    if (dates != previousDates) {
                        previousDates = dates
                        // ми вже в Main dispatcher (lifecycleScope), тож можна оновлювати UI прямо
                        serviceCardsContainer.removeAllViews()
                        records.forEach { record ->
                            createCardFromRecord(record)
                        }
                    }
                    // якщо dates == previousDates, значить змінилися лише title/notes/price → нічого не перероюємо
                }
        }
    }



    private fun createCardFromRecord(record: ServiceRecord) {
        val cardView = layoutInflater.inflate(R.layout.blank_service_card, serviceCardsContainer, false)



        // Аналогічно до createNewServiceCard, але з заповненням даних з запису
        val dateText = cardView.findViewById<TextView>(R.id.dateText)
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        dateText.text = formatter.format(record.date)

        cardView.findViewById<EditText>(R.id.titleInput).setText(record.title)
        cardView.findViewById<EditText>(R.id.notesInput).setText(record.notes)
        cardView.findViewById<EditText>(R.id.priceInput).setText(record.price)

        setupTextWatchers(record.date,
            cardView.findViewById(R.id.titleInput),
            cardView.findViewById(R.id.notesInput),
            cardView.findViewById(R.id.priceInput)
        )

        serviceCardsContainer.addView(cardView)

    }
    private fun formatDate(date: Long): String {
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)
        // Або для API 26+:
        // return LocalDate.ofEpochDay(date).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }
}
