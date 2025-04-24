package com.example.sgb

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sub.R
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // State caches
    private var lastT50: String? = null
    private var lastT100: String? = null
    private var lastTYear: String? = null
    private var cachedStartTime: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_service)
        bikeId = intent.getIntExtra("bike_id" , -1)
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
        tabLayout.addTab(tabLayout.newTab().setText("Інше 1"))
        tabLayout.addTab(tabLayout.newTab().setText("Інше 2"))
        showServiceContent()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showServiceContent()
                    1 -> showServiceContent1()
                    2 -> showServiceContent2()
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
                delay(10)
                updateRemainingTimes(System.currentTimeMillis())
            }
        }
    }
    private fun showServiceContent1() {
        //testing tab
        contentContainer.removeAllViews()
        val serviceView = layoutInflater.inflate(R.layout.kt_discover, contentContainer, false)
        contentContainer.addView(serviceView)
    }

    private fun showServiceContent2() {
        // testing tab
        contentContainer.removeAllViews()
        val serviceView = layoutInflater.inflate(R.layout.kt_setups, contentContainer, false)
        contentContainer.addView(serviceView)
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
}
