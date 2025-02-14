package com.example.sgb

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BPSetupDao
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.BikeParkSetupData
import com.example.sgb.room.BpMarksSuspenshion
import com.example.sgb.room.Component
import com.example.sgb.room.ComponentsDao
import com.example.sub.R
import com.example.sub.R.id.shock_seg_units
import kotlinx.coroutines.launch

class MaketSetup : AppCompatActivity() {
    // View змінні
    private lateinit var fork: TextView
    private lateinit var shock: TextView
    private lateinit var fTyre: TextView
    private lateinit var rTyre: TextView

    private lateinit var forkHSR: EditText
    private lateinit var forkLSR: EditText
    private lateinit var forkHSC: EditText
    private lateinit var forkLSC: EditText
    private lateinit var forkNotes: EditText
    private lateinit var shockHSR: EditText
    private lateinit var shockLSR: EditText
    private lateinit var shockHSC: EditText
    private lateinit var shockLSC: EditText
    private lateinit var shockNotes: EditText
    private lateinit var fTyrePressure: EditText
    private lateinit var rTyrePressure: EditText
    private lateinit var tyreNotes: EditText
    private lateinit var forkSag: EditText
    private lateinit var shockSag: EditText
    private lateinit var forkPressure: EditText
    private lateinit var shockPressure: EditText

    private lateinit var forkSegUnits: TextView
    private lateinit var forkPressureUnits: TextView
    private lateinit var shockSegUnits: TextView
    private lateinit var shockPressureUnits: TextView
    private lateinit var tyresPressureUnits: TextView

    // Збережені посилання на вью
    private lateinit var marksHandle: Button

    // Збереження стану
    private var isExpanded = false

    // Кешовані анімації
    private lateinit var expandHeightAnimation: ValueAnimator
    private lateinit var collapseHeightAnimation: ValueAnimator
    private lateinit var expandWidthAnimation: ValueAnimator
    private lateinit var collapseWidthAnimation: ValueAnimator


    // Зберігаємо посилання на вью, що постійно є у layout
    private lateinit var marksOverlay: FrameLayout
    private lateinit var gOut: EditText
    private lateinit var numbHands: EditText
    private lateinit var squareEdgedHits: EditText
    private lateinit var riderShifts: EditText
    private lateinit var bottomOutSus: EditText
    private lateinit var susSwinging: EditText
    private lateinit var stability: EditText
    private lateinit var btnCancel: Button

    // Зберігаємо DAO, оскільки база даних – сінглтон
    private val bpMarksSusDao by lazy { BikeDatabase.getDatabase(this).bpMarksSusDao() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_maket_setup)

        // Отримання даних з Intent
        val bikeId = intent.getIntExtra("bike_id", -1)
        val setupName = intent.getStringExtra("setup_name")
        val checkedText = intent.getStringExtra("BikePark")
        val setupId = intent.getIntExtra("setup_id", -1)

        // Ініціалізація View
        initView()

        // Ініціалізація вью
        marksHandle = findViewById(R.id.marks_handle)
        marksOverlay = findViewById(R.id.marks_overlay)

        // Ініціалізація кешованих анімацій
        expandHeightAnimation = ValueAnimator.ofInt(110, 950).apply {
            duration = 1000
            addUpdateListener { animation ->
                val params = marksHandle.layoutParams
                params.height = animation.animatedValue as Int
                marksHandle.layoutParams = params
            }
        }
        collapseHeightAnimation = ValueAnimator.ofInt(950, 110).apply {
            duration = 1000
            addUpdateListener { animation ->
                val params = marksHandle.layoutParams
                params.height = animation.animatedValue as Int
                marksHandle.layoutParams = params
            }
        }
        expandWidthAnimation = ValueAnimator.ofInt(19, 22).apply {
            duration = 1000
            addUpdateListener { animation ->
                val params = marksHandle.layoutParams
                params.width = animation.animatedValue as Int
                marksHandle.layoutParams = params
            }
        }
        collapseWidthAnimation = ValueAnimator.ofInt(22, 19).apply {
            duration = 1000
            addUpdateListener { animation ->
                val params = marksHandle.layoutParams
                params.width = animation.animatedValue as Int
                marksHandle.layoutParams = params
            }
        }

        // Обробка натискання на marksHandle
        marksHandle.setOnClickListener {
//            hideKeyboardAndClearFocus()
            if (isExpanded) {
                // Закриття: запускаємо collapse-анімації та анімацію overlay
                marksOverlay.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.slide_out_right)
                )
                marksOverlay.postDelayed({ marksOverlay.visibility = View.GONE }, 400)
                collapseWidthAnimation.start()
                collapseHeightAnimation.start()
            } else {
                // Відкриття: викликаємо діалог, запускаємо expand-анімації та анімацію overlay
                dialogForMarks(bikeId) { isExpanded = false } // callback для встановлення isExpanded = false при закритті діалогу
                marksOverlay.visibility = View.VISIBLE
                marksOverlay.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
                )
                expandWidthAnimation.start()
                expandHeightAnimation.start()
            }
            isExpanded = !isExpanded
        }

        // Інший ваш код (ініціалізація інших вью, завантаження даних, діалоги тощо)



        // Ініціалізація вибору одиниць для Sag
        shockSegUnits.setOnClickListener {
            showUnitSelectionDialogForShock()
        }

        forkSegUnits.setOnClickListener {
            showUnitSelectionDialogForFork()
        }

        // Init choosing Pressure
        forkPressureUnits.setOnClickListener {
            showUnitPressureDialogForFork()
        }
        shockPressureUnits.setOnClickListener {
            showUnitPressureDialogForShock()
        }
        tyresPressureUnits.setOnClickListener {
            showUnitPressureDialogForTyres()
        }

        // Налаштування кнопки "Назад"
        findViewById<Button>(R.id.back).setOnClickListener {
            navigateBackToActSetups(bikeId)
        }

        // Встановлення заголовка
        setHeaderText(setupName, checkedText)

        // Ініціалізація бази даних DAO
        val bikeDatabase = BikeDatabase.getDatabase(this)
        val componentsDao = bikeDatabase.componentsDao()
        val bpSetupDao = bikeDatabase.bpSetupDao()

        // Завантаження даних
        if (bikeId != -1) {
            loadBikeData(bikeId, componentsDao)
            loadSetupData(bikeId, bpSetupDao)
        }

        if (setupId != -1) {
            loadSetupById(setupId)
        }


    }
    // functions for hiding focus and keyboard

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        fun Activity.closeKeyboard() {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val view = currentFocus ?: View(this) // Якщо немає фокусу, створюємо новий View
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
        // Якщо це клік поза EditText
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is EditText) {
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)
                // Якщо натискання було поза EditText
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    closeKeyboard()  // Закриваємо клавіатуру перед скиданням фокусу
                    view.clearFocus() // Прибираємо фокус
                }
            }
        }


        return super.dispatchTouchEvent(event)
    }






    private fun initView() {
        // Ініціалізація текстових полів
        fork = findViewById(R.id.fork)
        shock = findViewById(R.id.shock)
        fTyre = findViewById(R.id.front_tyre)
        rTyre = findViewById(R.id.rear_tyre)

        forkHSR = findViewById(R.id.fork_hsr)
        forkLSR = findViewById(R.id.fork_lsr)
        forkHSC = findViewById(R.id.fork_hsc)
        forkLSC = findViewById(R.id.fork_lsc)
        forkNotes = findViewById(R.id.fork_notes)
        shockHSR = findViewById(R.id.shock_hsr)
        shockLSR = findViewById(R.id.shock_lsr)
        shockHSC = findViewById(R.id.shock_hsc)
        shockLSC = findViewById(R.id.shock_lsc)
        shockNotes = findViewById(R.id.shock_notes)
        fTyrePressure = findViewById(R.id.front_tyre_pressure)
        rTyrePressure = findViewById(R.id.rear_tyre_pressure)
        tyreNotes = findViewById(R.id.tyre_notes)
        forkSag = findViewById(R.id.fork_seg_value)
        shockSag = findViewById(R.id.shock_seg_value)
        forkPressure = findViewById(R.id.fork_pressure_value)
        shockPressure = findViewById(R.id.shock_pressure_value)



        forkSegUnits = findViewById(R.id.fork_seg_units)
        forkPressureUnits = findViewById(R.id.fork_pressure_units)
        shockSegUnits = findViewById(shock_seg_units)
        shockPressureUnits = findViewById(R.id.shock_pressure_units)
        tyresPressureUnits = findViewById(R.id.tyres_pressure_units)

        // Завантаження збережених одиниць вимірювання
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val shockUnit = sharedPreferences.getString("shockSegUnit", "")
        val forkUnit = sharedPreferences.getString("forkSegUnit", "")
        val forkPressureUnitShared = sharedPreferences.getString("forkPressureShared", "")
        val shockPressureUnitShared = sharedPreferences.getString("shockPressureShared", "")
        val tyresPressureUnitShared = sharedPreferences.getString("tyresPressureUnit", "")
        shockSegUnits.text = shockUnit
        forkSegUnits.text = forkUnit
        forkPressureUnits.text = forkPressureUnitShared
        shockPressureUnits.text = shockPressureUnitShared
        tyresPressureUnits.text = tyresPressureUnitShared

        // Ініціалізація вью
        gOut = findViewById(R.id.g_out)
        numbHands = findViewById(R.id.numb_hands)
        squareEdgedHits = findViewById(R.id.square_edged_hits)
        riderShifts = findViewById(R.id.rider_shifts)
        bottomOutSus = findViewById(R.id.bottom_out_sus)
        susSwinging = findViewById(R.id.sus_swinging)
        stability = findViewById(R.id.stability)
        btnCancel = findViewById(R.id.btn_cancel)

    }

    private fun navigateBackToActSetups(bikeId: Int) {
        val intent = Intent(this, ActSetups::class.java).apply {
            putExtra("bike_id", bikeId)
        }
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.fade_in_faster,
            R.anim.fade_out_faster
        )
        startActivity(intent, options.toBundle())
        finish()
    }

    private fun setHeaderText(setupName: String?, checkedText: String?) {
        val textViewHeader: TextView = findViewById(R.id.setup_name)
        textViewHeader.text = checkedText ?: setupName ?: getString(R.string.test)
    }

    @SuppressLint("SetTextI18n")
    private fun loadSetupData(bikeId: Int, bpSetupDao: BPSetupDao) {
        lifecycleScope.launch {
            val bpSetups = bpSetupDao.getBikeParkSetupById(bikeId)
                ?: BikeParkSetupData(bikeId = bikeId).also { bpSetupDao.insertBikeParkSetup(it) }

            mapOf(
                forkHSR to "forkHSR",
                forkLSR to "forkLSR",
                forkHSC to "forkHSC",
                forkLSC to "forkLSC",
                forkNotes to "forkNotes",
                shockHSR to "shockHSR",
                shockLSR to "shockLSR",
                shockHSC to "shockHSC",
                shockLSC to "shockLSC",
                shockNotes to "shockNotes",
                fTyrePressure to "frontTyrePressure",
                rTyrePressure to "rearTyrePressure",
                tyreNotes to "tyreNotes",
                forkSag to "forkSag",
                shockSag to "shockSag",
                forkPressure to "forkPressure",
                shockPressure to "shockPressure"
            ).forEach { (editText, field) ->
                editText.setText(bpSetups.getFieldValue(field))
                setupEditTextListener(editText, bikeId, field, bpSetupDao)
            }
        }
    }

    private fun loadBikeData(bikeId: Int, componentsDao: ComponentsDao) {
        lifecycleScope.launch {
            val components = componentsDao.getComponentsByBikeId(bikeId)
                ?: Component(bikeId = bikeId).also { componentsDao.insertComponent(it) }

            fork.text = getString(R.string.two_strings, components.forkBrand, components.forkSeries)
            shock.text =
                getString(R.string.two_strings, components.shockBrand, components.shockSeries)
            fTyre.text = getString(
                R.string.two_strings,
                components.frontTyreBrand,
                components.frontTyreSeries
            )
            rTyre.text =
                getString(R.string.two_strings, components.rearTyreBrand, components.rearTyreSeries)
        }
    }

    private fun loadSetupById(setupId: Int) {
        lifecycleScope.launch {
            val setupDao = BikeDatabase.getDatabase(this@MaketSetup).setupDao()
            val setup = setupDao.getSetupById(setupId)
            setup?.let {
                findViewById<TextView>(R.id.setup_name).append("\nДані: ${it.setupName}")
            }
        }
    }

    private fun setupEditTextListener(
        editText: EditText,
        bikeId: Int,
        field: String,
        bpSetupDao: BPSetupDao
    ) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    val bpSetups = bpSetupDao.getBikeParkSetupById(bikeId)
                    bpSetups?.let {
                        it.setFieldValue(field, s.toString())
                        bpSetupDao.updateBikeParkSetup(it)
                    }
                }
            }
        })
    }

    // Допоміжні методи для доступу до полів BikeParkSetupData
    private fun BikeParkSetupData.getFieldValue(field: String): String {
        return when (field) {
            "forkHSR" -> forkHSR
            "forkLSR" -> forkLSR
            "forkHSC" -> forkHSC
            "forkLSC" -> forkLSC
            "forkNotes" -> forkNotes
            "shockHSR" -> shockHSR
            "shockLSR" -> shockLSR
            "shockHSC" -> shockHSC
            "shockLSC" -> shockLSC
            "shockNotes" -> shockNotes
            "frontTyrePressure" -> frontTyrePressure
            "rearTyrePressure" -> rearTyrePressure
            "tyreNotes" -> tyreNotes
            "forkSag" -> forkSag
            "shockSag" -> shockSag
            "forkPressure" -> forkPressure
            "shockPressure" -> shockPressure
            else -> ""
        }
    }

    private fun BikeParkSetupData.setFieldValue(field: String, value: String) {
        when (field) {
            "forkHSR" -> forkHSR = value
            "forkLSR" -> forkLSR = value
            "forkHSC" -> forkHSC = value
            "forkLSC" -> forkLSC = value
            "forkNotes" -> forkNotes = value
            "shockHSR" -> shockHSR = value
            "shockLSR" -> shockLSR = value
            "shockHSC" -> shockHSC = value
            "shockLSC" -> shockLSC = value
            "shockNotes" -> shockNotes = value
            "frontTyrePressure" -> frontTyrePressure = value
            "rearTyrePressure" -> rearTyrePressure = value
            "tyreNotes" -> tyreNotes = value
            "forkSag" -> forkSag = value
            "shockSag" -> shockSag = value
            "forkPressure" -> forkPressure = value
            "shockPressure" -> shockPressure = value
        }
    }


    private fun showUnitSelectionDialogForShock() {
        // Надування кастомного макета
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_percent_or_mm, null)

        // Створення діалогового вікна
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(transparentColor))

        // Налаштування кнопок
        val btnPercent = dialogView.findViewById<Button>(R.id.btn_percent)
        val btnMm = dialogView.findViewById<Button>(R.id.btn_mm)

        btnPercent.setOnClickListener {
            saveSelectedUnit("%")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        btnMm.setOnClickListener {
            saveSelectedUnit("mm")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        // Показ діалогу
        dialogBuilder.show()
    }

    private fun showUnitSelectionDialogForFork() {
        // Надування кастомного макета
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_percent_or_mm, null)

        // Створення діалогового вікна
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(transparentColor))

        // Налаштування кнопок
        val btnPercent = dialogView.findViewById<Button>(R.id.btn_percent)
        val btnMm = dialogView.findViewById<Button>(R.id.btn_mm)

        btnPercent.setOnClickListener {
            saveSelectedUnitForFork("%")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        btnMm.setOnClickListener {
            saveSelectedUnitForFork("mm")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        // Показ діалогу
        dialogBuilder.show()
    }

    // Метод для збереження вибраної одиниці для Fork
    private fun saveSelectedUnitForFork(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("forkSegUnit", unit).apply()

        // Оновлення TextView
        forkSegUnits.text = unit
    }

    // Метод для збереження вибраної одиниці
    private fun saveSelectedUnit(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("shockSegUnit", unit).apply()

        // Оновлення TextView
        shockSegUnits.text = unit
    }

    private fun showUnitPressureDialogForFork() {
        // Надування кастомного макета
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_psi_or_bar, null)

        // Створення діалогового вікна
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(transparentColor))

        // Налаштування кнопок
        val btnPsi = dialogView.findViewById<Button>(R.id.btn_psi)
        val btnBar = dialogView.findViewById<Button>(R.id.btn_bar)

        btnPsi.setOnClickListener {
            savePressureUnitForFork("psi")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        btnBar.setOnClickListener {
            savePressureUnitForFork("bar")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        // Показ діалогу
        dialogBuilder.show()
    }

    private fun showUnitPressureDialogForShock() {
        // Надування кастомного макета
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_psi_or_bar, null)

        // Створення діалогового вікна
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(transparentColor))

        // Налаштування кнопок
        val btnPsi = dialogView.findViewById<Button>(R.id.btn_psi)
        val btnBar = dialogView.findViewById<Button>(R.id.btn_bar)

        btnPsi.setOnClickListener {
            savePressureUnitForShock("psi")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        btnBar.setOnClickListener {
            savePressureUnitForShock("bar")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        // Показ діалогу
        dialogBuilder.show()
    }

    // Метод для збереження вибраної одиниці для Fork
    private fun savePressureUnitForFork(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("forkPressureShared", unit).apply()

        // Оновлення TextView
        forkPressureUnits.text = unit
    }

    // Метод для збереження вибраної одиниці
    private fun savePressureUnitForShock(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("shockPressureShared", unit).apply()

        // Оновлення TextView
        shockPressureUnits.text = unit
    }

    private fun showUnitPressureDialogForTyres() {
        // Надування кастомного макета
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_psi_or_bar, null)

        // Створення діалогового вікна
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(transparentColor))

        // Налаштування кнопок
        val btnPsi = dialogView.findViewById<Button>(R.id.btn_psi)
        val btnBar = dialogView.findViewById<Button>(R.id.btn_bar)

        btnPsi.setOnClickListener {
            savePressureUnitForTyres("psi")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        btnBar.setOnClickListener {
            savePressureUnitForTyres("bar")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        // Показ діалогу
        dialogBuilder.show()
    }

    // Метод для збереження вибраної одиниці для Fork
    private fun savePressureUnitForTyres(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("tyresPressureUnit", unit).apply()

        // Оновлення TextView
        tyresPressureUnits.text = unit
    }

    private fun dialogForMarks(bikeId: Int, onExpandChange: (Boolean) -> Unit) {
        // Знаходимо overlay із розміткою діалогу, який вже включено в DrawerLayout
        val marksOverlay = findViewById<FrameLayout>(R.id.marks_overlay)


        // Завантаження даних із бази
        lifecycleScope.launch {
            val existingMarks = bpMarksSusDao.getBpMarksSusByBikeId(bikeId)
            existingMarks?.let {
                gOut.setText(it.gOut)
                numbHands.setText(it.numbHands)
                squareEdgedHits.setText(it.squareEdgedHits)
                riderShifts.setText(it.riderShifts)
                bottomOutSus.setText(it.bottomOutSus)
                susSwinging.setText(it.susSwinging)
                stability.setText(it.stability)
            }
        }
        fun updateMarks() {
            lifecycleScope.launch {
                val newMarks = BpMarksSuspenshion(
                    bikeId = bikeId,
                    gOut = gOut.text.toString(),
                    numbHands = numbHands.text.toString(),
                    squareEdgedHits = squareEdgedHits.text.toString(),
                    riderShifts = riderShifts.text.toString(),
                    bottomOutSus = bottomOutSus.text.toString(),
                    susSwinging = susSwinging.text.toString(),
                    stability = stability.text.toString()
                )
                val existingMarks = bpMarksSusDao.getBpMarksSusByBikeId(bikeId)
                if (existingMarks == null) {
                    bpMarksSusDao.insertBpMarksSus(newMarks)
                } else {
                    bpMarksSusDao.updateBpMarksSus(newMarks.copy(id = existingMarks.id))
                }
            }
        }
        // Налаштовуємо кожен EditText із callback-ом, який зберігає дані після зміни тексту
        setupEditTextWithLimit(gOut, onValidTextChanged = { updateMarks() })
        setupEditTextWithLimit(numbHands, onValidTextChanged = { updateMarks() })
        setupEditTextWithLimit(squareEdgedHits, onValidTextChanged = { updateMarks() })
        setupEditTextWithLimit(riderShifts, onValidTextChanged = { updateMarks() })
        setupEditTextWithLimit(bottomOutSus, onValidTextChanged = { updateMarks() })
        setupEditTextWithLimit(susSwinging, onValidTextChanged = { updateMarks() })
        setupEditTextWithLimit(stability, onValidTextChanged = { updateMarks() })
        // Спільна функція для закриття overlay з анімацією і анімацією кнопки marks_handle
        fun closeOverlayWithAnimation() {
            // Анімація для кнопки marks_handle (закриття)
            val marksHandle: Button = findViewById(R.id.marks_handle)

            // Створюємо collapse-анімації для висоти та ширини
            val collapseHeightAnimation =
                ValueAnimator.ofInt(950, 110) // Наприклад: від 950 до 110px
            val collapseWidthAnimation = ValueAnimator.ofInt(22, 19)    // Наприклад: від 22 до 19px

            collapseHeightAnimation.duration = 1000
            collapseWidthAnimation.duration = 1000

            collapseWidthAnimation.addUpdateListener { animation ->
                val params = marksHandle.layoutParams
                params.width = animation.animatedValue as Int
                marksHandle.layoutParams = params
            }
            collapseHeightAnimation.addUpdateListener { animation ->
                val params = marksHandle.layoutParams
                params.height = animation.animatedValue as Int
                marksHandle.layoutParams = params
            }

            collapseWidthAnimation.start()
            collapseHeightAnimation.start()

            // Анімація закриття overlay (slide_out_right)
            marksOverlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right))
            marksOverlay.postDelayed({ marksOverlay.visibility = View.GONE }, 400)
// Передаємо isExpanded = false у зовнішню функцію
            onExpandChange(false)
        }

        // Обробка кнопок: і btnCancel, і btnOk використовують ту саму функцію для закриття з анімацією
        btnCancel.setOnClickListener {
            closeOverlayWithAnimation()
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    fun setupEditTextWithLimit(editText: EditText, onValidTextChanged: (() -> Unit)? = null) {
        val suffix = "/24"

        // Якщо поле порожнє, встановлюємо лише суфікс і курсор на початку
        if (editText.text.isEmpty()) {
            editText.setText(suffix)
            editText.setSelection(0)
        }
        // Збереження останнього валідного значення (тільки числова частина)
        var previousNumeric = ""
        editText.addTextChangedListener(object : TextWatcher {
            var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true

                // Отримуємо поточний текст та видаляємо суфікс (якщо він є)
                var fullText = s.toString()
                if (fullText.endsWith(suffix)) {
                    fullText = fullText.substring(0, fullText.length - suffix.length)
                } else {
                    // Якщо випадково з'явився символ '/', обрізаємо все після нього
                    val slashIndex = fullText.indexOf('/')
                    if (slashIndex != -1) {
                        fullText = fullText.substring(0, slashIndex)
                    }
                }

                // Перевіряємо, чи введене число не перевищує 24
                val number = fullText.toIntOrNull()
                if (number != null && number > 24) {
                    // Якщо число перевищує 24 – повертаємо попереднє валідне значення
                    Toast.makeText(
                        editText.context,
                        "Максимальний бал дорівнює 24!",
                        Toast.LENGTH_SHORT
                    ).show()
                    fullText = previousNumeric
                }
                // Зберігаємо поточне валідне значення
                previousNumeric = fullText

                // Встановлюємо текст знову: числова частина + незмінний суфікс
                editText.setText(fullText + suffix)
                // Курсор розташовується на кінці числової частини (тобто перед суфіксом)
                editText.setSelection(fullText.length)

                isEditing = false

                // Викликаємо callback для збереження даних
                onValidTextChanged?.invoke()
            }
        })
        // Забороняємо переміщення курсора у зону суфікса
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val allowedPosition = editText.text.toString().indexOf(suffix)
                if (editText.selectionStart > allowedPosition) {
                    editText.setSelection(allowedPosition)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}


