package com.example.sgb

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BPSetupDao
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.BikeParkSetupData
import com.example.sgb.room.BpMarksSuspenshion
import com.example.sgb.room.Component
import com.example.sgb.room.ComponentsDao
import com.example.sub.R
import com.example.sub.R.id.average_mark
import com.example.sub.R.id.shock_seg_units
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
    private lateinit var marksHandleCon: FrameLayout

    // Збереження стану
    private var isExpanded = false


    // Зберігаємо посилання на вью, що постійно є у layout
    private lateinit var marksOverlay: FrameLayout
    private lateinit var gOut: EditText
    private lateinit var numbHands: EditText
    private lateinit var squareEdgedHits: EditText
    private lateinit var riderShifts: EditText
    private lateinit var bottomOutSus: EditText
    private lateinit var susSwinging: EditText
    private lateinit var stability: EditText
    private lateinit var tyrePlussiness: EditText
    private lateinit var btnCancel: Button
    private lateinit var pulling: EditText
    private lateinit var cornersEdit: EditText
    private lateinit var feetTired: EditText

    private lateinit var gestureDetector: GestureDetector


//test
private val initialValues = mutableMapOf<Int, Int>()

    private lateinit var inForkHsr: ImageButton
    private lateinit var deForkHsr: ImageButton
    private lateinit var inForkLsr: ImageButton
    private lateinit var deForkLsr: ImageButton
    private lateinit var inForkHsc: ImageButton
    private lateinit var deForkHsc: ImageButton
    private lateinit var inForkLsc: ImageButton
    private lateinit var deForkLsc: ImageButton

    private lateinit var inShockHsr: ImageButton
    private lateinit var deShockHsr: ImageButton
    private lateinit var inShockLsr: ImageButton
    private lateinit var deShockLsr: ImageButton
    private lateinit var inShockHsc: ImageButton
    private lateinit var deShockHsc: ImageButton
    private lateinit var inShockLsc: ImageButton
    private lateinit var deShockLsc: ImageButton



    // Delta TextViews for each EditText (ensure these IDs match your XML)
    private lateinit var forkHSRDelta: TextView
    private lateinit var forkLSRDelta: TextView
    private lateinit var forkHSCDelta: TextView
    private lateinit var forkLSCDelta: TextView
    private lateinit var shockHSRDelta: TextView
    private lateinit var shockLSRDelta: TextView
    private lateinit var shockHSCDelta: TextView
    private lateinit var shockLSCDelta: TextView


    // Зберігаємо DAO, оскільки база даних – сінглтон
    private val bpMarksSusDao by lazy { BikeDatabase.getDatabase(this).bpMarksSusDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_maket_setup)
        fun closeKeyboard() {
            val inputMethodManager =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val view = currentFocus ?: View(this)
            inputMethodManager.hideSoftInputFromWindow(
                view.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
// In your onCreate or similar initialization code:
        val nestedScrollView = findViewById<NestedScrollView>(R.id.scroll_view)

        nestedScrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            nestedScrollView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = nestedScrollView.rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            // If the keyboard occupies more than 30% of the screen...
            if (keyboardHeight > screenHeight * 0.3) {
                val focusedView = currentFocus
                if (focusedView is EditText) {
                    // Get the visible rectangle of the focused view.
                    val focusedRect = Rect()
                    focusedView.getGlobalVisibleRect(focusedRect)

                    // Only scroll if the bottom of the focused view is below the visible area.
                    if (focusedRect.bottom > rect.bottom) {
                        nestedScrollView.post {
                            // Calculate the scroll amount:
                            // Scroll just enough so that the bottom of the view moves within the visible area,
                            // optionally add a small extra offset if desired.
                            val extraOffset = 20  // you can adjust this value
                            val scrollDelta = (focusedRect.bottom - rect.bottom) + extraOffset
                            nestedScrollView.smoothScrollBy(0, scrollDelta)
                        }
                    }
                }
            }
        }
        // Ініціалізація GestureDetector
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Перевіряємо, чи натискання відбулося поза EditText
                val view = currentFocus
                if (view is EditText) {
                    val outRect = Rect()
                    view.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(e.rawX.toInt(), e.rawY.toInt())) {
                        closeKeyboard()
                        view.clearFocus()
                    }
                }
                return super.onSingleTapUp(e)
            }
        })


        // Отримання даних з Intent
        val bikeId = intent.getIntExtra("bike_id", -1)
        val setupName = intent.getStringExtra("setup_name")
        val checkedText = intent.getStringExtra("BikePark")
       // val setupId = intent.getIntExtra("setup_id", -1)

        // Ініціалізація View
        initView()


        val handleNormal = ContextCompat.getDrawable(this, R.drawable.btn_right_handle)
        val handleActive = ContextCompat.getDrawable(this, R.drawable.btn_right_handle_activated)

        val fadeDuration = 800 // duration in milliseconds

        marksHandleCon.setOnClickListener {
            marksHandle.isEnabled = false // disable during transition

            if (isExpanded) {
                // Create a TransitionDrawable to crossfade from active to normal.
                val transition = TransitionDrawable(arrayOf(handleActive, handleNormal))
                transition.isCrossFadeEnabled = true
                marksHandle.background = transition
                transition.startTransition(fadeDuration)

                // Animate overlay out as before.
                marksOverlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right))
                marksOverlay.postDelayed({ marksOverlay.visibility = View.GONE }, 400)

                // Once the transition is complete, force the final drawable.
                marksHandle.postDelayed({
                    marksHandle.background = handleNormal
                }, fadeDuration.toLong())
            } else {
                // Create a TransitionDrawable to crossfade from normal to active.
                val transition = TransitionDrawable(arrayOf(handleNormal, handleActive))
                transition.isCrossFadeEnabled = true
                marksHandle.background = transition
                transition.startTransition(fadeDuration)

                // Show overlay and open marks dialog as before.
                dialogForMarks(bikeId) { isExpanded = false }
                marksOverlay.visibility = View.VISIBLE
                marksOverlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right))

                // Force the final drawable after the transition.
                marksHandle.postDelayed({
                    marksHandle.background = handleActive
                }, fadeDuration.toLong())
            }

            // Re-enable the button after a delay a bit longer than the fade.
            marksHandle.postDelayed({
                marksHandle.isEnabled = true
            }, 400)

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


        loadBikeData(bikeId, componentsDao)
        loadSetupData(bikeId, bpSetupDao)
        loadSetupById(bikeId)
        loadMarksData(bikeId)
            // Load and display all saved delta values for 10 seconds.
        loadDeltaValues(bikeId, bpSetupDao)






// test
        inForkHsr.setOnClickListener {
            val currentValue = forkHSR.text.toString().toIntOrNull() ?: 0
            val newValue = currentValue + 1
            forkHSR.setText(newValue.toString())
            updateFieldInDb("forkHSR", newValue, bikeId, bpSetupDao)

        }


        deForkHsr.setOnClickListener {
            val currentValue = forkHSR.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            forkHSR.setText(newValue.toString())
            updateFieldInDb("forkHSR", newValue, bikeId, bpSetupDao)
        }

        inForkLsr.setOnClickListener {
            val currentValue = forkLSR.text.toString().toIntOrNull() ?: 0
            val newValue = currentValue + 1
            forkLSR.setText(newValue.toString())
            updateFieldInDb("forkLSR", newValue, bikeId, bpSetupDao)
        }

        deForkLsr.setOnClickListener {
            val currentValue = forkLSR.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            forkLSR.setText(newValue.toString())
            updateFieldInDb("forkLSR", newValue, bikeId, bpSetupDao)
        }

        inForkHsc.setOnClickListener {
            val currentValue = forkHSC.text.toString().toIntOrNull() ?: 0
            val newValue = currentValue + 1
            forkHSC.setText(newValue.toString())
            updateFieldInDb("forkHSC", newValue, bikeId, bpSetupDao)
        }

        deForkHsc.setOnClickListener {
            val currentValue = forkHSC.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            forkHSC.setText(newValue.toString())
            updateFieldInDb("forkHSC", newValue, bikeId, bpSetupDao)
        }

        inForkLsc.setOnClickListener {
            val currentValue = forkLSC.text.toString().toIntOrNull() ?: 0
            val newValue = currentValue + 1
            forkLSC.setText(newValue.toString())
            updateFieldInDb("forkLSC", newValue, bikeId, bpSetupDao)
        }

        deForkLsc.setOnClickListener {
            val currentValue = forkLSC.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            forkLSC.setText(newValue.toString())
            updateFieldInDb("forkLSC", newValue, bikeId, bpSetupDao)
        }


        inShockHsr.setOnClickListener {
            val currentValue = shockHSR.text.toString().toIntOrNull() ?: 0
            val newValue = currentValue + 1
            shockHSR.setText(newValue.toString())
            updateFieldInDb("shockHSR", newValue, bikeId, bpSetupDao)

        }

        deShockHsr.setOnClickListener {
            val currentValue = shockHSR.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            shockHSR.setText(newValue.toString())
            updateFieldInDb("shockHSR", newValue, bikeId, bpSetupDao)
        }

        inShockLsr.setOnClickListener {
            val currentValue = shockLSR.text.toString().toIntOrNull() ?: 0
            val newValue = currentValue + 1
            shockLSR.setText(newValue.toString())
            updateFieldInDb("shockLSR", newValue, bikeId, bpSetupDao)
        }

        deShockLsr.setOnClickListener {
            val currentValue = shockLSR.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            shockLSR.setText(newValue.toString())
            updateFieldInDb("shockLSR", newValue, bikeId, bpSetupDao)
        }

        inShockHsc.setOnClickListener {
            val currentValue = shockHSC.text.toString().toIntOrNull() ?: 0
            val newValue = currentValue + 1
            shockHSC.setText(newValue.toString())
            updateFieldInDb("shockHSC", newValue, bikeId, bpSetupDao)
        }

        deShockHsc.setOnClickListener {
            val currentValue = shockHSC.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            shockHSC.setText(newValue.toString())
            updateFieldInDb("shockHSC", newValue, bikeId, bpSetupDao)
        }

        inShockLsc.setOnClickListener {
            val currentValue = shockLSC.text.toString().toIntOrNull() ?: 0
            val newValue = currentValue + 1
            shockLSC.setText(newValue.toString())
            updateFieldInDb("shockLSC", newValue, bikeId, bpSetupDao)
        }

        deShockLsc.setOnClickListener {
            val currentValue = shockLSC.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            shockLSC.setText(newValue.toString())
            updateFieldInDb("shockLSC", newValue, bikeId, bpSetupDao)
        }

    }
    // functions for hiding focus and keyboard

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
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
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
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
        tyrePlussiness = findViewById(R.id.tyres_plussiness)
        btnCancel = findViewById(R.id.btn_cancel)
        pulling = findViewById(R.id.pulling)
        cornersEdit = findViewById(R.id.corers)
        feetTired = findViewById(R.id.feet_tired)
        marksHandle = findViewById(R.id.marks_handle)
        marksHandleCon = findViewById(R.id.marks_handle_container)
        marksOverlay = findViewById(R.id.marks_overlay)


        // testing
        // Initialize delta TextViews for the composite cells.
        forkHSRDelta = findViewById(R.id.fork_hsr_delta)
        forkLSRDelta = findViewById(R.id.fork_lsr_delta)
        forkHSCDelta = findViewById(R.id.fork_hsc_delta)
        forkLSCDelta = findViewById(R.id.fork_lsc_delta)
        inForkHsr = findViewById(R.id.increment_fhsr)
        deForkHsr = findViewById(R.id.decrement_fhsr)
        inForkLsr = findViewById(R.id.increment_flsr)
        deForkLsr = findViewById(R.id.decrement_flsr)
        inForkHsc = findViewById(R.id.increment_fhsc)
        deForkHsc = findViewById(R.id.decrement_fhsc)
        inForkLsc = findViewById(R.id.increment_flsc)
        deForkLsc = findViewById(R.id.decrement_flsc)

        shockHSRDelta = findViewById(R.id.shock_hsr_delta)
        shockLSRDelta = findViewById(R.id.shock_lsr_delta)
        shockHSCDelta = findViewById(R.id.shock_hsc_delta)
        shockLSCDelta = findViewById(R.id.shock_lsc_delta)
        inShockHsr = findViewById(R.id.increment_shsr)
        deShockHsr = findViewById(R.id.decrement_shsr)
        inShockLsr = findViewById(R.id.increment_slsr)
        deShockLsr = findViewById(R.id.decrement_slsr)
        inShockHsc = findViewById(R.id.increment_shsc)
        deShockHsc = findViewById(R.id.decrement_shsc)
        inShockLsc = findViewById(R.id.increment_slsc)
        deShockLsc = findViewById(R.id.decrement_slsc)


    }

    private fun updateFieldInDb(
        fieldName: String,
        newValue: Int,
        bikeId: Int,
        bpSetupDao: BPSetupDao
    ) {
        // Використовуємо lifecycleScope для контролю життєвого циклу корутин
        lifecycleScope.launch {
            // Працюємо з БД на IO-потоці
            withContext(Dispatchers.IO) {
                val setup = bpSetupDao.getBikeParkSetupById(bikeId)
                if (setup != null) {
                    when (fieldName) {
                        "forkHSR" -> setup.forkHSR = newValue
                        "forkLSR" -> setup.forkLSR = newValue
                        "forkHSC" -> setup.forkHSC = newValue
                        "forkLSC" -> setup.forkLSC = newValue
                        "shockHSR" -> setup.shockHSR = newValue
                        "shockLSR" -> setup.shockLSR = newValue
                        "shockHSC" -> setup.shockHSC = newValue
                        "shockLSC" -> setup.shockLSC = newValue
                    }
                    bpSetupDao.updateBikeParkSetup(setup)
                }
            }
            // Перемикаємося на головний потік для оновлення UI
            when (fieldName) {
                "forkHSR" -> updateDeltaForField(
                    forkHSR,
                    forkHSRDelta,
                    "forkHSR",
                    bikeId,
                    bpSetupDao
                )
                "forkLSR" -> updateDeltaForField(
                    forkLSR,
                    forkLSRDelta,
                    "forkLSR",
                    bikeId,
                    bpSetupDao
                )
                "forkHSC" -> updateDeltaForField(
                    forkHSC,
                    forkHSCDelta,
                    "forkHSC",
                    bikeId,
                    bpSetupDao
                )
                "forkLSC" -> updateDeltaForField(
                    forkLSC,
                    forkLSCDelta,
                    "forkLSC",
                    bikeId,
                    bpSetupDao
                )
                "shockHSR" -> updateDeltaForField(
                    shockHSR,
                    shockHSRDelta,
                    "shockHSR",
                    bikeId,
                    bpSetupDao
                )
                "shockLSR" -> updateDeltaForField(
                    shockLSR,
                    shockLSRDelta,
                    "shockLSR",
                    bikeId,
                    bpSetupDao
                )
                "shockHSC" -> updateDeltaForField(
                    shockHSC,
                    shockHSCDelta,
                    "shockHSC",
                    bikeId,
                    bpSetupDao
                )
                "shockLSC" -> updateDeltaForField(
                    shockLSC,
                    shockLSCDelta,
                    "shockLSC",
                    bikeId,
                    bpSetupDao
                )
            }
        }
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


    private fun updateDeltaForField(
        editText: EditText,
        deltaTextView: TextView,
        field: String,
        bikeId: Int,
        bpSetupDao: BPSetupDao,
        delayMillis: Long = 2000
    ) {
        val baseline = initialValues[editText.id] ?: 0
        val currentValue = editText.text.toString().toIntOrNull() ?: 0
        val diff = currentValue - baseline

        // Перемикаємося на головний потік для роботи з UI
        lifecycleScope.launch(Dispatchers.Main) {
            if (diff == 0) {
                // Якщо різниця нуль — fade out та приховування
                deltaTextView.animate().alpha(0f).setDuration(500).withEndAction {
                    deltaTextView.visibility = View.GONE
                }
            } else {
                val deltaStr = if (diff > 0) "+$diff" else diff.toString()
                deltaTextView.text = deltaStr

                // Встановлюємо потрібний колір: зелений для позитивного, червоний для негативного
                val colorRes = if (diff > 0) R.color.green else R.color.red_dark
                deltaTextView.setTextColor(ContextCompat.getColor(deltaTextView.context, colorRes))

                // Анімація fade in
                deltaTextView.alpha = 0f
                deltaTextView.visibility = View.VISIBLE
                deltaTextView.animate().alpha(1f).setDuration(500).start()

                // Оновлюємо дельту в БД (цей метод можна залишити в IO, якщо він не змінює UI)
                updateDeltaFieldInDb(bikeId, bpSetupDao, field, deltaStr)

                // Плануємо fade out після затримки
                scheduleHideDelta(deltaTextView, delayMillis)
            }
        }
    }

    private fun scheduleHideDelta(deltaTextView: TextView, delayMillis: Long = 2000) {
        // Використовуємо postDelayed, який гарантує виконання на UI-потоці
        deltaTextView.postDelayed({
            deltaTextView.animate().alpha(0f).setDuration(500).withEndAction {
                deltaTextView.visibility = View.GONE
            }
        }, delayMillis)
    }



    // --- When loading data from Room, load baselines and attach delta listeners ---
    @SuppressLint("SetTextI18n")
    private fun loadSetupData(bikeId: Int, bpSetupDao: BPSetupDao) {
        lifecycleScope.launch {
            // Отримуємо запис за bikeId. Завдяки унікальному індексу гарантовано буде лише один запис.
            var bpSetups = bpSetupDao.getBikeParkSetupById(bikeId)
            if (bpSetups == null) {
                bpSetups = BikeParkSetupData(bikeId = bikeId)
                bpSetupDao.insertBikeParkSetup(bpSetups)
            }
            val fields = mapOf(
                forkHSR to "forkHSR",
                forkLSR to "forkLSR",
                forkHSC to "forkHSC",
                forkLSC to "forkLSC",
                forkNotes to "forkNotes",
                shockHSR to "shockHSR",
                shockLSR to "shockLSR",
                shockHSC to "shockHSC",
                shockLSC to "shockLSC",
                fTyrePressure to "frontTyrePressure",
                rTyrePressure to "rearTyrePressure",
                tyreNotes to "tyreNotes",
                forkSag to "forkSag",
                shockSag to "shockSag",
                forkPressure to "forkPressure",
                shockPressure to "shockPressure"
            )

            fields.forEach { (editText, fieldName) ->
                val savedValue = bpSetups.getFieldValue(fieldName)
                editText.setText(savedValue)
                // Записуємо базове значення для delta
                initialValues[editText.id] = savedValue.toIntOrNull() ?: 0
                // Додаємо слухач для оновлення
                setupEditTextListener(editText, bikeId, fieldName, bpSetupDao)
            }


        }
    }






    // Get the value as a String for display purposes.
    private fun BikeParkSetupData.getFieldValue(field: String): String {
        return when (field) {
            "forkHSR" -> forkHSR.toString()
            "forkLSR" -> forkLSR.toString()
            "forkHSC" -> forkHSC.toString()
            "forkLSC" -> forkLSC.toString()
            "forkNotes" -> forkNotes
            "shockHSR" -> shockHSR.toString()
            "shockLSR" -> shockLSR.toString()
            "shockHSC" -> shockHSC.toString()
            "shockLSC" -> shockLSC.toString()
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

    // Save the value, converting strings to ints where needed.
    private fun BikeParkSetupData.setFieldValue(field: String, value: String) {
        when (field) {
            "forkHSR" -> forkHSR = value.toIntOrNull() ?: 0
            "forkLSR" -> forkLSR = value.toIntOrNull() ?: 0
            "forkHSC" -> forkHSC = value.toIntOrNull() ?: 0
            "forkLSC" -> forkLSC = value.toIntOrNull() ?: 0
            "forkNotes" -> forkNotes = value
            "shockHSR" -> shockHSR = value.toIntOrNull() ?: 0
            "shockLSR" -> shockLSR = value.toIntOrNull() ?: 0
            "shockHSC" -> shockHSC = value.toIntOrNull() ?: 0
            "shockLSC" -> shockLSC = value.toIntOrNull() ?: 0
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
    private fun BikeParkSetupData.setDeltaFieldValue(field: String, delta: String) {
        when (field) {
            "forkHSR" -> this.forkHSRDelta = delta
            "forkLSR" -> this.forkLSRDelta = delta
            "forkHSC" -> this.forkHSCDelta = delta
            "forkLSC" -> this.forkLSCDelta = delta
            "shockHSR" -> this.shockHSRDelta = delta
            "shockLSR" -> this.shockLSRDelta = delta
            "shockHSC" -> this.shockHSCDelta = delta
            "shockLSC" -> this.shockLSCDelta = delta
            // Add other fields as needed.
        }
    }


    private fun updateDeltaFieldInDb(bikeId: Int, bpSetupDao: BPSetupDao, field: String, delta: String) {
        lifecycleScope.launch {
            val bpSetup = bpSetupDao.getBikeParkSetupById(bikeId)
            bpSetup?.let {
                it.setDeltaFieldValue(field, delta)
                bpSetupDao.updateBikeParkSetup(it)
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

    private fun loadSetupById(bikeId: Int) {
        lifecycleScope.launch {
            val setupDao = BikeDatabase.getDatabase(this@MaketSetup).setupDao()
            val setup = setupDao.getSetupById(bikeId)
            setup?.let {
                findViewById<TextView>(R.id.setup_name).append("\nДані: ${it.setupName}")
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun loadMarksData(bikeId: Int) {
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
                tyrePlussiness.setText(it.tyresPlussiness)
                findViewById<TextView>(average_mark).text = it.averageMark.toString()
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
            private var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEditing || s == null) return

                isEditing = true

                // Додаємо перенос рядка після кожних 50 символів, якщо його ще немає
                val formattedText = StringBuilder()
                for (i in s.indices) {
                    formattedText.append(s[i])
                    if ((i + 1) % 45 == 0 && i != s.lastIndex && s[i + 1] != '\n') {
                        formattedText.append("\n")
                    }
                }

                val newText = formattedText.toString()
                if (newText != s.toString()) {
                    editText.setText(newText)
                    editText.setSelection(newText.length)
                }

                isEditing = false

                // Оновлюємо дані в базі даних
                lifecycleScope.launch {
                    val bpSetups = bpSetupDao.getBikeParkSetupById(bikeId)
                    bpSetups?.let {
                        it.setFieldValue(field, newText)
                        bpSetupDao.updateBikeParkSetup(it)
                    }
                }
            }
        })
    }
    private fun loadDeltaValues(bikeId: Int, bpSetupDao: BPSetupDao) {
        lifecycleScope.launch {
            val bpSetup = bpSetupDao.getBikeParkSetupById(bikeId)
            bpSetup?.let {
                // forkHSR delta
                if (it.forkHSRDelta.isNotEmpty()) {
                    forkHSRDelta.text = it.forkHSRDelta
                    val colorRes = when {
                        it.forkHSRDelta.startsWith("+") -> R.color.green
                        it.forkHSRDelta.startsWith("-") -> R.color.red_dark
                        else -> R.color.green
                    }
                    forkHSRDelta.setTextColor(ContextCompat.getColor(forkHSRDelta.context, colorRes))
                    forkHSRDelta.visibility = View.VISIBLE
                    scheduleHideDelta(forkHSRDelta)
                }
                // forkLSR delta
                if (it.forkLSRDelta.isNotEmpty()) {
                    forkLSRDelta.text = it.forkLSRDelta
                    val colorRes = when {
                        it.forkLSRDelta.startsWith("+") -> R.color.green
                        it.forkLSRDelta.startsWith("-") -> R.color.red_dark
                        else -> R.color.green
                    }
                    forkLSRDelta.setTextColor(ContextCompat.getColor(forkLSRDelta.context, colorRes))
                    forkLSRDelta.visibility = View.VISIBLE
                    scheduleHideDelta(forkLSRDelta)
                }
                // forkHSC delta
                if (it.forkHSCDelta.isNotEmpty()) {
                    forkHSCDelta.text = it.forkHSCDelta
                    val colorRes = when {
                        it.forkHSCDelta.startsWith("+") -> R.color.green
                        it.forkHSCDelta.startsWith("-") -> R.color.red_dark
                        else -> R.color.green
                    }
                    forkHSCDelta.setTextColor(ContextCompat.getColor(forkHSCDelta.context, colorRes))
                    forkHSCDelta.visibility = View.VISIBLE
                    scheduleHideDelta(forkHSCDelta)
                }
                // forkLSC delta
                if (it.forkLSCDelta.isNotEmpty()) {
                    forkLSCDelta.text = it.forkLSCDelta
                    val colorRes = when {
                        it.forkLSCDelta.startsWith("+") -> R.color.green
                        it.forkLSCDelta.startsWith("-") -> R.color.red_dark
                        else -> R.color.green
                    }
                    forkLSCDelta.setTextColor(ContextCompat.getColor(forkLSCDelta.context, colorRes))
                    forkLSCDelta.visibility = View.VISIBLE
                    scheduleHideDelta(forkLSCDelta)
                }
                if (it.shockHSRDelta.isNotEmpty()) {
                    shockHSRDelta.text = it.shockHSRDelta
                    val colorRes = when {
                        it.shockHSRDelta.startsWith("+") -> R.color.green
                        it.shockHSRDelta.startsWith("-") -> R.color.red_dark
                        else -> R.color.green
                    }
                    shockHSRDelta.setTextColor(ContextCompat.getColor(shockHSRDelta.context, colorRes))
                    shockHSRDelta.visibility = View.VISIBLE
                    scheduleHideDelta(shockHSRDelta)
                }
                // forkLSR delta
                if (it.shockLSRDelta.isNotEmpty()) {
                    shockLSRDelta.text = it.shockLSRDelta
                    val colorRes = when {
                        it.shockLSRDelta.startsWith("+") -> R.color.green
                        it.shockLSRDelta.startsWith("-") -> R.color.red_dark
                        else -> R.color.green
                    }
                    shockLSRDelta.setTextColor(ContextCompat.getColor(shockLSRDelta.context, colorRes))
                    shockLSRDelta.visibility = View.VISIBLE
                    scheduleHideDelta(shockLSRDelta)
                }
                // forkHSC delta
                if (it.shockHSCDelta.isNotEmpty()) {
                    shockHSCDelta.text = it.shockHSCDelta
                    val colorRes = when {
                        it.shockHSCDelta.startsWith("+") -> R.color.green
                        it.shockHSCDelta.startsWith("-") -> R.color.red_dark
                        else -> R.color.green
                    }
                    shockHSCDelta.setTextColor(ContextCompat.getColor(shockHSCDelta.context, colorRes))
                    shockHSCDelta.visibility = View.VISIBLE
                    scheduleHideDelta(shockHSCDelta)
                }
                // forkLSC delta
                if (it.shockLSCDelta.isNotEmpty()) {
                    shockLSCDelta.text = it.shockLSCDelta
                    val colorRes = when {
                        it.shockLSCDelta.startsWith("+") -> R.color.green
                        it.shockLSCDelta.startsWith("-") -> R.color.red_dark
                        else -> R.color.green
                    }
                    shockLSCDelta.setTextColor(ContextCompat.getColor(shockLSCDelta.context, colorRes))
                    shockLSCDelta.visibility = View.VISIBLE
                    scheduleHideDelta(shockLSCDelta)
                }

            }
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
        dialogBuilder.window?.setBackgroundDrawable(transparentColor.toDrawable())

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
        dialogBuilder.window?.setBackgroundDrawable(transparentColor.toDrawable())

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
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferences.edit { putString("forkSegUnit", unit) }

        // Оновлення TextView
        forkSegUnits.text = unit
    }

    // Метод для збереження вибраної одиниці
    private fun saveSelectedUnit(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferences.edit { putString("shockSegUnit", unit) }

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
        dialogBuilder.window?.setBackgroundDrawable(transparentColor.toDrawable())

        // Налаштування кнопок
        val btnPsi = dialogView.findViewById<Button>(R.id.btn_psi)
        val btnBar = dialogView.findViewById<Button>(R.id.btn_bar)

        btnPsi.setOnClickListener {
            savePressureUnitForFork("PSI")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        btnBar.setOnClickListener {
            savePressureUnitForFork("BAR")
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
        dialogBuilder.window?.setBackgroundDrawable(transparentColor.toDrawable())

        // Налаштування кнопок
        val btnPsi = dialogView.findViewById<Button>(R.id.btn_psi)
        val btnBar = dialogView.findViewById<Button>(R.id.btn_bar)

        btnPsi.setOnClickListener {
            savePressureUnitForShock("PSI")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        btnBar.setOnClickListener {
            savePressureUnitForShock("BAR")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        // Показ діалогу
        dialogBuilder.show()
    }

    // Метод для збереження вибраної одиниці для Fork
    private fun savePressureUnitForFork(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferences.edit { putString("forkPressureShared", unit) }

        // Оновлення TextView
        forkPressureUnits.text = unit
    }

    // Метод для збереження вибраної одиниці
    private fun savePressureUnitForShock(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferences.edit { putString("shockPressureShared", unit) }

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
        dialogBuilder.window?.setBackgroundDrawable(transparentColor.toDrawable())

        // Налаштування кнопок
        val btnPsi = dialogView.findViewById<Button>(R.id.btn_psi)
        val btnBar = dialogView.findViewById<Button>(R.id.btn_bar)

        btnPsi.setOnClickListener {
            savePressureUnitForTyres("PSI")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        btnBar.setOnClickListener {
            savePressureUnitForTyres("BAR")
            dialogBuilder.dismiss() // Закриття діалогу
        }

        // Показ діалогу
        dialogBuilder.show()
    }

    // Метод для збереження вибраної одиниці для Fork
    private fun savePressureUnitForTyres(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferences.edit { putString("tyresPressureUnit", unit) }

        // Оновлення TextView
        tyresPressureUnits.text = unit
    }

    private fun dialogForMarks(bikeId: Int, onExpandChange: (Boolean) -> Unit) {
        // Знаходимо overlay із розміткою діалогу, який вже включено в DrawerLayout
        val marksOverlay = findViewById<FrameLayout>(R.id.marks_overlay)


        @SuppressLint("SetTextI18n")
        fun updateMarks() {
            lifecycleScope.launch {
                // Видаляємо суфікс "/24" перед конвертацією в число
                val marksList = listOf(
                    gOut.text.toString().removeSuffix("/24"),
                    numbHands.text.toString().removeSuffix("/24"),
                    squareEdgedHits.text.toString().removeSuffix("/24"),
                    riderShifts.text.toString().removeSuffix("/24"),
                    bottomOutSus.text.toString().removeSuffix("/24"),
                    susSwinging.text.toString().removeSuffix("/24"),
                    stability.text.toString().removeSuffix("/24"),
                    tyrePlussiness.text.toString().removeSuffix("/24"),
                    pulling.text.toString().removeSuffix("/24"),
                    cornersEdit.text.toString().removeSuffix("/24"),
                    feetTired.text.toString().removeSuffix("/24")
                )

                val validMarks = marksList.mapNotNull { it.toIntOrNull() }
                val averageMark =
                    if (validMarks.isNotEmpty()) validMarks.sum() / validMarks.size else 0

                // Оновлюємо відображення середнього значення в UI одразу
                averageMark.toString().also { findViewById<TextView>(average_mark).text = it }

                val newMarks = BpMarksSuspenshion(
                    bikeId = bikeId,
                    gOut = gOut.text.toString(),
                    numbHands = numbHands.text.toString(),
                    squareEdgedHits = squareEdgedHits.text.toString(),
                    riderShifts = riderShifts.text.toString(),
                    bottomOutSus = bottomOutSus.text.toString(),
                    susSwinging = susSwinging.text.toString(),
                    stability = stability.text.toString(),
                    tyresPlussiness = tyrePlussiness.text.toString(),
                    pulling = pulling.text.toString(),
                    corners = cornersEdit.text.toString(),
                    tiredFeet = feetTired.text.toString(),
                    averageMark = averageMark
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
        setupEditTextWithLimit(tyrePlussiness, onValidTextChanged = { updateMarks() })
        setupEditTextWithLimit(pulling, onValidTextChanged = { updateMarks() })
        setupEditTextWithLimit(cornersEdit, onValidTextChanged = { updateMarks()})
        setupEditTextWithLimit(feetTired, onValidTextChanged = { updateMarks()})
        // Спільна функція для закриття overlay з анімацією і анімацією кнопки marks_handle
        fun closeOverlayWithAnimation() {
            marksHandle.isEnabled = false // Блокуємо кнопку перед анімацією
             val handleNormal = ContextCompat.getDrawable(this, R.drawable.btn_right_handle)
            val handleActive = ContextCompat.getDrawable(this, R.drawable.btn_right_handle_activated)
            val fadeDuration = 800 // duration in milliseconds
            val animationSet = AnimatorSet().apply {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Додаємо затримку перед розблокуванням кнопки
                        marksHandle.postDelayed({
                            marksHandle.isEnabled = true
                        }, 650) // Додаємо 500 мс до загального часу блокування
                    }
                })
            }

            // Запуск анімацій
            animationSet.start()
            // Create a TransitionDrawable to crossfade from active to normal.
            val transition = TransitionDrawable(arrayOf(handleActive, handleNormal))
            transition.isCrossFadeEnabled = true
            marksHandle.background = transition
            transition.startTransition(fadeDuration)

            // Анімація закриття overlay (slide_out_right)
            marksOverlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right))
            marksOverlay.postDelayed({ marksOverlay.visibility = View.GONE }, 400)
            marksHandle.postDelayed({
                marksHandle.background = handleNormal
            }, fadeDuration.toLong())
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
                        "Maximum - 24!",
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
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Відкладене виконання, щоб система встановила власну позицію курсора
                editText.post {
                    val allowedPosition = editText.text.toString().indexOf(suffix)
                    if (editText.selectionStart > allowedPosition) {
                        editText.setSelection(allowedPosition)
                    }
                }
            }
            false
        }
    }
}