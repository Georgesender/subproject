package com.example.sgb

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.MaketSetupDao
import com.example.sgb.room.MarksSuspenshion
import com.example.sgb.room.SetupData
import com.example.sub.R
import com.example.sub.R.id.average_mark
import com.example.sub.R.id.shock_seg_units
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MaketSetup : AppCompatActivity() {

    // В тілі Activity — оголошення змінних (на рівні класу)
    private var pendingJsonToSave: String? = null
    private var pendingSuggestedName: String = "bikepark_export.json"
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


    private lateinit var marksHandle: Button
    private lateinit var marksHandleCon: FrameLayout


    private var isExpanded = false


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



    private lateinit var forkHSRDelta: TextView
    private lateinit var forkLSRDelta: TextView
    private lateinit var forkHSCDelta: TextView
    private lateinit var forkLSCDelta: TextView
    private lateinit var shockHSRDelta: TextView
    private lateinit var shockLSRDelta: TextView
    private lateinit var shockHSCDelta: TextView
    private lateinit var shockLSCDelta: TextView
    // Регістрація лончера — роби це в класі (не в локальному методі), або безпосередньо у onCreate перед використанням
    private val createFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, "Збереження скасовано", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val json = pendingJsonToSave
            if (json.isNullOrEmpty()) {
                Toast.makeText(this, "Немає даних для збереження", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            // Запис у вибраний Uri в IO-потоці
            lifecycleScope.launch {
                val resultMessage = withContext(Dispatchers.IO) {
                    try {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                            out.flush()
                        } ?: throw IOException("Не вдалося відкрити потік для запису")
                        "Експортовано у $uri"
                    } catch (e: Exception) {
                        "Помилка експорту: ${e.message ?: "unknown"}"
                    }
                }

                // Показати результат
                Toast.makeText(this@MaketSetup, resultMessage, Toast.LENGTH_LONG).show()

                // Опціонально: відкрити файл у зовнішньому редакторі одразу після збереження
                if (!resultMessage.startsWith("Помилка")) {
                    try {
                        val openIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/json")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(openIntent)
                    } catch (_: Exception) {
                        // Якщо нема програми, яка відкриває JSON — нічого страшного
                    }
                }
            }
        }
    private val marksSusDao by lazy { BikeDatabase.getDatabase(this).marksSusDao() }
    private var currentSetupId: Int = -1
    private suspend fun fetchRelevantSetup(maketSetupDao: MaketSetupDao, bikeId: Int): SetupData? {
        return withContext(Dispatchers.IO) {
            if (currentSetupId != -1) {
                // шукаємо конкретно по setupId (унікальний ідентифікатор сетапу)
                maketSetupDao.getSetupBySetupId(currentSetupId)
            } else {
                // fallback: якщо відкрито не конкретний setup, беремо "за замовчуванням" по bikeId
                maketSetupDao.getSetupById(bikeId)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_maket_setup)
        currentSetupId = intent.getIntExtra("setup_id", -1)
        fun closeKeyboard() {
            val inputMethodManager =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val view = currentFocus ?: View(this)
            inputMethodManager.hideSoftInputFromWindow(
                view.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
        val nestedScrollView = findViewById<NestedScrollView>(R.id.scroll_view)

        nestedScrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            nestedScrollView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = nestedScrollView.rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            if (keyboardHeight > screenHeight * 0.3) {
                val focusedView = currentFocus
                if (focusedView is EditText) {
                    val focusedRect = Rect()
                    focusedView.getGlobalVisibleRect(focusedRect)

                    if (focusedRect.bottom > rect.bottom) {
                        nestedScrollView.post {
                            val extraOffset = 20
                            val scrollDelta = (focusedRect.bottom - rect.bottom) + extraOffset
                            nestedScrollView.smoothScrollBy(0, scrollDelta)
                        }
                    }
                }
            }
        }
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
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


        val bikeId = intent.getIntExtra("bike_id", -1)
        val setupName = intent.getStringExtra("setup_name")
        val checkedText = intent.getStringExtra("BikePark")


        // test section start

        val exportButton = findViewById<Button>(R.id.testexport)
        exportButton.setOnClickListener {
            lifecycleScope.launch {
                // --- Підготувати JSON (в IO) ---
                val pair = withContext(Dispatchers.IO) {
                    try {
                        val db = BikeDatabase.getDatabase(applicationContext)
                        val dao = db.maketSetupDao()
                        val gson = GsonBuilder().setPrettyPrinting().create()

                        if (bikeId != -1) {
                            val setup = fetchRelevantSetup(dao, bikeId)
                            if (setup == null) {
                                null to "Немає запису для bikeId=$bikeId"
                            } else {
                                val json = gson.toJson(setup)
                                val filename = "bikepark_$bikeId${System.currentTimeMillis()}.json"
                                json to filename
                            }
                        } else {
                            val list = dao.getAllSetups()
                            val json = gson.toJson(list)
                            val filename = "bikepark_all_${System.currentTimeMillis()}.json"
                            json to filename
                        }
                    } catch (e: Exception) {
                        null to ("Помилка при отриманні даних: ${e.message ?: "unknown"}")
                    }
                }

                val (json, maybeFilename) = pair
                if (json == null) {
                    Toast.makeText(this@MaketSetup, maybeFilename, Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Збережемо тимчасово та запустимо діалог з підказаною назвою
                pendingJsonToSave = json
                pendingSuggestedName = maybeFilename
                createFileLauncher.launch(pendingSuggestedName)
            }
        }
        // test section end
        fun EditText.enableLongPressEdit(context: Context) {
            isFocusable = false
            isClickable = true

            setOnLongClickListener {
                isFocusableInTouchMode = true
                requestFocus()
                val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                true
            }

            onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    isFocusable = false
                    val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(this.windowToken, 0)
                }
            }
        }


        initView()
        updateComponentsInfo(bikeId)
        forkHSR.enableLongPressEdit(this)
        forkLSR.enableLongPressEdit(this)
        forkHSC.enableLongPressEdit(this)
        forkLSC.enableLongPressEdit(this)
        shockHSR.enableLongPressEdit(this)
        shockLSR.enableLongPressEdit(this)
        shockHSC.enableLongPressEdit(this)
        shockLSC.enableLongPressEdit(this)



        val handleNormal = ContextCompat.getDrawable(this, R.drawable.btn_right_handle)
        val handleActive = ContextCompat.getDrawable(this, R.drawable.btn_right_handle_activated)

        val fadeDuration = 800

        marksHandleCon.setOnClickListener {
            marksHandle.isEnabled = false

            if (isExpanded) {

                val transition = TransitionDrawable(arrayOf(handleActive, handleNormal))
                transition.isCrossFadeEnabled = true
                marksHandle.background = transition
                transition.startTransition(fadeDuration)

                marksOverlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right))
                marksOverlay.postDelayed({ marksOverlay.visibility = View.GONE }, 400)

                marksHandle.postDelayed({
                    marksHandle.background = handleNormal
                }, fadeDuration.toLong())
            } else {
                val transition = TransitionDrawable(arrayOf(handleNormal, handleActive))
                transition.isCrossFadeEnabled = true
                marksHandle.background = transition
                transition.startTransition(fadeDuration)

                dialogForMarks(bikeId) { isExpanded = false }
                marksOverlay.visibility = View.VISIBLE
                marksOverlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right))

                marksHandle.postDelayed({
                    marksHandle.background = handleActive
                }, fadeDuration.toLong())
            }

            marksHandle.postDelayed({
                marksHandle.isEnabled = true
            }, 400)

            isExpanded = !isExpanded
        }




        val forkhint = findViewById<ImageButton>(R.id.fork_hint)
        forkhint.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.hint_fork_setup, null)


            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener {
                builder.dismiss()
            }

            builder.show()
        }


        shockSegUnits.setOnClickListener {
            showUnitSelectionDialogForShock()
        }

        forkSegUnits.setOnClickListener {
            showUnitSelectionDialogForFork()
        }

        forkPressureUnits.setOnClickListener {
            showUnitPressureDialogForFork()
        }
        shockPressureUnits.setOnClickListener {
            showUnitPressureDialogForShock()
        }
        tyresPressureUnits.setOnClickListener {
            showUnitPressureDialogForTyres()
        }

        findViewById<Button>(R.id.back).setOnClickListener {
            navigateBackToActSetups(bikeId)
        }
        setHeaderText(setupName, checkedText)

        val bikeDatabase = BikeDatabase.getDatabase(this)
        val maketSetupDao = bikeDatabase.maketSetupDao()


        loadSetupData(bikeId, maketSetupDao)
        loadSetupById(bikeId)
        loadMarksData(bikeId)
        loadDeltaValues(bikeId, maketSetupDao)






fun handleIncrement(
    editText: EditText,
    fieldName: String,
    bikeId: Int,
    maketSetupDao: MaketSetupDao,
    context: Context
) {
    val currentValue = editText.text.toString().toIntOrNull() ?: 0
    val newValue = currentValue + 1

    if (newValue > 30) {
        Toast.makeText(context, "Ви не можете мати більше 30 кліків", Toast.LENGTH_SHORT).show()
        return
    }

    editText.setText(newValue.toString())
    updateFieldInDb(fieldName, newValue, bikeId, maketSetupDao)
}


        inForkHsr.setOnClickListener {
            handleIncrement(forkHSR, "forkHSR", bikeId, maketSetupDao, this)

        }
        deForkHsr.setOnClickListener {
            val currentValue = forkHSR.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            forkHSR.setText(newValue.toString())
            updateFieldInDb("forkHSR", newValue, bikeId, maketSetupDao)
        }

        inForkLsr.setOnClickListener {
            handleIncrement(forkLSR, "forkLSR", bikeId, maketSetupDao, this)
        }

        deForkLsr.setOnClickListener {
            val currentValue = forkLSR.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            forkLSR.setText(newValue.toString())
            updateFieldInDb("forkLSR", newValue, bikeId, maketSetupDao)
        }

        inForkHsc.setOnClickListener {
            handleIncrement(forkHSC, "forkHSC", bikeId, maketSetupDao, this)
        }

        deForkHsc.setOnClickListener {
            val currentValue = forkHSC.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            forkHSC.setText(newValue.toString())
            updateFieldInDb("forkHSC", newValue, bikeId, maketSetupDao)
        }

        inForkLsc.setOnClickListener {
            handleIncrement(forkLSC, "forkLSC", bikeId, maketSetupDao, this)
        }

        deForkLsc.setOnClickListener {
            val currentValue = forkLSC.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            forkLSC.setText(newValue.toString())
            updateFieldInDb("forkLSC", newValue, bikeId, maketSetupDao)
        }


        inShockHsr.setOnClickListener {
            handleIncrement(shockHSR, "shockHSR", bikeId, maketSetupDao, this)

        }

        deShockHsr.setOnClickListener {
            val currentValue = shockHSR.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            shockHSR.setText(newValue.toString())
            updateFieldInDb("shockHSR", newValue, bikeId, maketSetupDao)
        }

        inShockLsr.setOnClickListener {
            handleIncrement(shockLSR, "shockLSR", bikeId, maketSetupDao, this)
        }

        deShockLsr.setOnClickListener {
            val currentValue = shockLSR.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            shockLSR.setText(newValue.toString())
            updateFieldInDb("shockLSR", newValue, bikeId, maketSetupDao)
        }

        inShockHsc.setOnClickListener {
            handleIncrement(shockHSC, "shockHSC", bikeId, maketSetupDao, this)
        }

        deShockHsc.setOnClickListener {
            val currentValue = shockHSC.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            shockHSC.setText(newValue.toString())
            updateFieldInDb("shockHSC", newValue, bikeId, maketSetupDao)
        }

        inShockLsc.setOnClickListener {
            handleIncrement(shockLSC, "shockLSC", bikeId, maketSetupDao, this)
        }

        deShockLsc.setOnClickListener {
            val currentValue = shockLSC.text.toString().toIntOrNull() ?: 0
            val newValue = if (currentValue > 0) currentValue - 1 else 0
            shockLSC.setText(newValue.toString())
            updateFieldInDb("shockLSC", newValue, bikeId, maketSetupDao)
        }


    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }


    private fun initView() {
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
        maketSetupDao: MaketSetupDao
    ) {
        lifecycleScope.launch {
            val maketSetups = fetchRelevantSetup(maketSetupDao, bikeId)
            if (maketSetups != null) {
                when (fieldName) {
                    "forkHSR" -> maketSetups.forkHSR = newValue
                    "forkLSR" -> maketSetups.forkLSR = newValue
                    "forkHSC" -> maketSetups.forkHSC = newValue
                    "forkLSC" -> maketSetups.forkLSC = newValue
                    "shockHSR" -> maketSetups.shockHSR = newValue
                    "shockLSR" -> maketSetups.shockLSR = newValue
                    "shockHSC" -> maketSetups.shockHSC = newValue
                    "shockLSC" -> maketSetups.shockLSC = newValue
                }
                withContext(Dispatchers.IO) { maketSetupDao.updateSetup(maketSetups) }
            }
            // оновлюємо delta в UI як і раніше
            when (fieldName) {
                "forkHSR" -> updateDeltaForField(forkHSR, forkHSRDelta, "forkHSR", bikeId, maketSetupDao)
                "forkLSR" -> updateDeltaForField(forkLSR, forkLSRDelta, "forkLSR", bikeId, maketSetupDao)
                "forkHSC" -> updateDeltaForField(forkHSC, forkHSCDelta, "forkHSC", bikeId, maketSetupDao)
                "forkLSC" -> updateDeltaForField(forkLSC, forkLSCDelta, "forkLSC", bikeId, maketSetupDao)
                "shockHSR" -> updateDeltaForField(shockHSR, shockHSRDelta, "shockHSR", bikeId, maketSetupDao)
                "shockLSR" -> updateDeltaForField(shockLSR, shockLSRDelta, "shockLSR", bikeId, maketSetupDao)
                "shockHSC" -> updateDeltaForField(shockHSC, shockHSCDelta, "shockHSC", bikeId, maketSetupDao)
                "shockLSC" -> updateDeltaForField(shockLSC, shockLSCDelta, "shockLSC", bikeId, maketSetupDao)
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
        maketSetupDao: MaketSetupDao,
        delayMillis: Long = 1000
    ) {
        val baseline = initialValues[editText.id] ?: 0
        val currentValue = editText.text.toString().toIntOrNull() ?: 0
        val diff = currentValue - baseline

        lifecycleScope.launch(Dispatchers.Main) {
            if (diff == 0) {
                deltaTextView.animate().alpha(0f).setDuration(500).withEndAction {
                    deltaTextView.visibility = View.GONE
                }
            } else {
                val deltaStr = if (diff > 0) "+$diff" else diff.toString()
                deltaTextView.text = deltaStr

                val colorRes = if (diff > 0) R.color.green else R.color.red_dark
                deltaTextView.setTextColor(ContextCompat.getColor(deltaTextView.context, colorRes))

                deltaTextView.alpha = 0f
                deltaTextView.visibility = View.VISIBLE
                deltaTextView.animate().alpha(1f).setDuration(500).start()

                updateDeltaFieldInDb(bikeId, maketSetupDao, field, deltaStr)

                scheduleHideDelta(deltaTextView, delayMillis)
            }
        }
    }

    private fun scheduleHideDelta(deltaTextView: TextView, delayMillis: Long = 4000) {
        deltaTextView.postDelayed({
            deltaTextView.animate().alpha(0f).setDuration(500).withEndAction {
                deltaTextView.visibility = View.GONE
            }
        }, delayMillis)
    }



    @SuppressLint("SetTextI18n")
    private fun loadSetupData(bikeId: Int, maketSetupDao: MaketSetupDao) {
        lifecycleScope.launch {
            var maketSetups = if (currentSetupId != -1) {
                fetchRelevantSetup(maketSetupDao, bikeId)
            } else {
                maketSetupDao.getSetupById(bikeId)
            }

            if (maketSetups == null) {
                maketSetups = SetupData(bikeId = bikeId, setupId = currentSetupId)
                maketSetupDao.insertSetup(maketSetups)
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
                val savedValue = maketSetups.getFieldValue(fieldName)
                editText.setText(savedValue)
                initialValues[editText.id] = savedValue.toIntOrNull() ?: 0
                setupEditTextListener(editText, bikeId, fieldName, maketSetupDao)
            }


        }
    }


    private fun SetupData.getFieldValue(field: String): String {
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

    private fun SetupData.setFieldValue(field: String, value: String) {
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
    private fun SetupData.setDeltaFieldValue(field: String, delta: String) {
        when (field) {
            "forkHSR" -> this.forkHSRDelta = delta
            "forkLSR" -> this.forkLSRDelta = delta
            "forkHSC" -> this.forkHSCDelta = delta
            "forkLSC" -> this.forkLSCDelta = delta
            "shockHSR" -> this.shockHSRDelta = delta
            "shockLSR" -> this.shockLSRDelta = delta
            "shockHSC" -> this.shockHSCDelta = delta
            "shockLSC" -> this.shockLSCDelta = delta
        }
    }


    private fun updateDeltaFieldInDb(bikeId: Int, maketSetupDao: MaketSetupDao, field: String, delta: String) {
        lifecycleScope.launch {
            val maketSetup = fetchRelevantSetup(maketSetupDao, bikeId)
            maketSetup?.let {
                it.setDeltaFieldValue(field, delta)
                withContext(Dispatchers.IO) { maketSetupDao.updateSetup(it) }
            }
        }
    }




    private fun loadSetupById(bikeId: Int) {
        lifecycleScope.launch {
            val setupDao = BikeDatabase.getDatabase(this@MaketSetup).setupDao()
            val setup = if (currentSetupId != -1) {
                setupDao.getSetupById(currentSetupId)
            } else {
                setupDao.getSetupsByBikeId(bikeId).firstOrNull()
            }
            setup?.let {
                findViewById<TextView>(R.id.setup_name).append("\nДані: ${it.setupName}")
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun loadMarksData(bikeId: Int) {
        lifecycleScope.launch {
            val existingMarks = marksSusDao.getMarksSusByBikeId(bikeId)
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
        maketSetupDao: MaketSetupDao
    ) {


        editText.addTextChangedListener(object : TextWatcher {
            private var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEditing || s == null) return

                isEditing = true

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

                lifecycleScope.launch {
                    val maketSetups = fetchRelevantSetup(maketSetupDao, bikeId)
                    maketSetups?.let {
                        it.setFieldValue(field, newText)
                        withContext(Dispatchers.IO) { maketSetupDao.updateSetup(it) }
                    }
                }

            }
        })
    }
    private fun loadDeltaValues(bikeId: Int, maketSetupDao: MaketSetupDao) {
        lifecycleScope.launch {
            val maketSetup =     fetchRelevantSetup(maketSetupDao, bikeId)
            maketSetup?.let {
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_percent_or_mm, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(transparentColor.toDrawable())

        val btnPercent = dialogView.findViewById<Button>(R.id.btn_percent)
        val btnMm = dialogView.findViewById<Button>(R.id.btn_mm)

        btnPercent.setOnClickListener {
            saveSelectedUnit("%")
            dialogBuilder.dismiss()
        }

        btnMm.setOnClickListener {
            saveSelectedUnit("mm")
            dialogBuilder.dismiss()
        }

        dialogBuilder.show()
    }

    private fun showUnitSelectionDialogForFork() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_percent_or_mm, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(transparentColor.toDrawable())

        val btnPercent = dialogView.findViewById<Button>(R.id.btn_percent)
        val btnMm = dialogView.findViewById<Button>(R.id.btn_mm)

        btnPercent.setOnClickListener {
            saveSelectedUnitForFork("%")
            dialogBuilder.dismiss()
        }

        btnMm.setOnClickListener {
            saveSelectedUnitForFork("mm")
            dialogBuilder.dismiss()
        }

        dialogBuilder.show()
    }

    private fun saveSelectedUnitForFork(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferences.edit { putString("forkSegUnit", unit) }

        forkSegUnits.text = unit
    }

    private fun saveSelectedUnit(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferences.edit { putString("shockSegUnit", unit) }

        shockSegUnits.text = unit
    }

    private fun showUnitPressureDialogForFork() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_psi_or_bar, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(transparentColor.toDrawable())

        val btnPsi = dialogView.findViewById<Button>(R.id.btn_psi)
        val btnBar = dialogView.findViewById<Button>(R.id.btn_bar)

        btnPsi.setOnClickListener {
            savePressureUnitForFork("PSI")
            dialogBuilder.dismiss()
        }

        btnBar.setOnClickListener {
            savePressureUnitForFork("BAR")
            dialogBuilder.dismiss()
        }

        dialogBuilder.show()
    }

    private fun showUnitPressureDialogForShock() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_psi_or_bar, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(transparentColor.toDrawable())

        val btnPsi = dialogView.findViewById<Button>(R.id.btn_psi)
        val btnBar = dialogView.findViewById<Button>(R.id.btn_bar)

        btnPsi.setOnClickListener {
            savePressureUnitForShock("PSI")
            dialogBuilder.dismiss()
        }

        btnBar.setOnClickListener {
            savePressureUnitForShock("BAR")
            dialogBuilder.dismiss()
        }

        dialogBuilder.show()
    }

    private fun savePressureUnitForFork(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferences.edit { putString("forkPressureShared", unit) }

        forkPressureUnits.text = unit
    }

    private fun savePressureUnitForShock(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferences.edit { putString("shockPressureShared", unit) }

        shockPressureUnits.text = unit
    }

    private fun showUnitPressureDialogForTyres() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_psi_or_bar, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(transparentColor.toDrawable())

        val btnPsi = dialogView.findViewById<Button>(R.id.btn_psi)
        val btnBar = dialogView.findViewById<Button>(R.id.btn_bar)

        btnPsi.setOnClickListener {
            savePressureUnitForTyres("PSI")
            dialogBuilder.dismiss()
        }

        btnBar.setOnClickListener {
            savePressureUnitForTyres("BAR")
            dialogBuilder.dismiss()
        }

        dialogBuilder.show()
    }

    private fun savePressureUnitForTyres(unit: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        sharedPreferences.edit { putString("tyresPressureUnit", unit) }

        tyresPressureUnits.text = unit
    }

    private fun dialogForMarks(bikeId: Int, onExpandChange: (Boolean) -> Unit) {
        val marksOverlay = findViewById<FrameLayout>(R.id.marks_overlay)


        @SuppressLint("SetTextI18n")
        fun updateMarks() {
            lifecycleScope.launch {
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

                averageMark.toString().also { findViewById<TextView>(average_mark).text = it }

                val newMarks = MarksSuspenshion(
                    bikeId = bikeId ,
                    gOut = gOut.text.toString() ,
                    numbHands = numbHands.text.toString() ,
                    squareEdgedHits = squareEdgedHits.text.toString() ,
                    riderShifts = riderShifts.text.toString() ,
                    bottomOutSus = bottomOutSus.text.toString() ,
                    susSwinging = susSwinging.text.toString() ,
                    stability = stability.text.toString() ,
                    tyresPlussiness = tyrePlussiness.text.toString() ,
                    pulling = pulling.text.toString() ,
                    corners = cornersEdit.text.toString() ,
                    tiredFeet = feetTired.text.toString() ,
                    averageMark = averageMark
                )

                val existingMarks = marksSusDao.getMarksSusByBikeId(bikeId)
                if (existingMarks == null) {
                    marksSusDao.insertMarksSus(newMarks)
                } else {
                    marksSusDao.updateMarksSus(newMarks.copy(id = existingMarks.id))
                }
            }
        }

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
        fun closeOverlayWithAnimation() {
            marksHandle.isEnabled = false
             val handleNormal = ContextCompat.getDrawable(this, R.drawable.btn_right_handle)
            val handleActive = ContextCompat.getDrawable(this, R.drawable.btn_right_handle_activated)
            val fadeDuration = 800
            val animationSet = AnimatorSet().apply {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        marksHandle.postDelayed({
                            marksHandle.isEnabled = true
                        }, 650)
                    }
                })
            }

            animationSet.start()
            val transition = TransitionDrawable(arrayOf(handleActive, handleNormal))
            transition.isCrossFadeEnabled = true
            marksHandle.background = transition
            transition.startTransition(fadeDuration)

            marksOverlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right))
            marksOverlay.postDelayed({ marksOverlay.visibility = View.GONE }, 400)
            marksHandle.postDelayed({
                marksHandle.background = handleNormal
            }, fadeDuration.toLong())
            onExpandChange(false)
        }

        btnCancel.setOnClickListener {
            closeOverlayWithAnimation()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    fun setupEditTextWithLimit(editText: EditText, onValidTextChanged: (() -> Unit)? = null) {
        val suffix = "/24"

        if (editText.text.isEmpty()) {
            editText.setText(suffix)
            editText.setSelection(0)
        }
        var previousNumeric = ""
        editText.addTextChangedListener(object : TextWatcher {
            var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true

                var fullText = s.toString()
                if (fullText.endsWith(suffix)) {
                    fullText = fullText.substring(0, fullText.length - suffix.length)
                } else {
                    val slashIndex = fullText.indexOf('/')
                    if (slashIndex != -1) {
                        fullText = fullText.substring(0, slashIndex)
                    }
                }

                val number = fullText.toIntOrNull()
                if (number != null && number > 24) {
                    Toast.makeText(
                        editText.context,
                        "Maximum - 24!",
                        Toast.LENGTH_SHORT
                    ).show()
                    fullText = previousNumeric
                }
                previousNumeric = fullText

                editText.setText(fullText + suffix)
                editText.setSelection(fullText.length)

                isEditing = false

                onValidTextChanged?.invoke()
            }
        })
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
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
    private fun updateComponentsInfo(bikeId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Отримуємо список компонентів для даного байка
            val components = BikeDatabase.getDatabase(applicationContext)
                .componentsDao()
                .getComponentsByBikeId(bikeId)
            // Фільтруємо компонент типу "Fork" (без врахування регістру)
            val forkComponent = components.firstOrNull {
                it.compType.equals("Fork", ignoreCase = true)
            }
            val shockComponent = components.firstOrNull {
                it.compType.equals("Shock", ignoreCase = true)
            }
            val tyresComponent = components.firstOrNull {
                it.compType.equals("Tyre", ignoreCase = true)
            }
            if (forkComponent != null) {
                // Формуємо рядок із бренду, моделі та картриджу (compAdaptive)
                val forkInfo = "${forkComponent.compBrand} ${forkComponent.compModel} ${forkComponent.compAdaptive}"
                // Перетворюємо photoUri (якщо є) на Uri
                val forkPhotoUri = forkComponent.photoUri?.toUri()
                withContext(Dispatchers.Main) {
                    // Оновлюємо ImageView для фото вилки, і TextView для інформації про вилку
                    val forkIconImageView = findViewById<ImageView>(R.id.fork_icon)
                    val forkInfoTextView = findViewById<TextView>(R.id.fork)
                    forkIconImageView.setBackgroundColor(Color.TRANSPARENT)

                    // Якщо є фото, завантажуємо його за допомогою Glide
                    if (forkPhotoUri != null) {
                        Glide.with(this@MaketSetup)
                            .load(forkPhotoUri)
                            .into(forkIconImageView)
                    } else {
                        // Можна встановити стандартне зображення (як placeholder)
                        forkIconImageView.setImageResource(R.drawable.img_fork)
                    }
                    // Встановлюємо текст рядка для вилки
                    forkInfoTextView.text = forkInfo
                }
            }
            if (shockComponent != null) {
                // Формуємо рядок із бренду, моделі та картриджу (compAdaptive)
                val shockInfo = "${shockComponent.compBrand} ${shockComponent.compModel} ${shockComponent.compAdaptive}"
                // Перетворюємо photoUri (якщо є) на Uri
                val shockPhotoUri = shockComponent.photoUri?.toUri()
                withContext(Dispatchers.Main) {
                    // Оновлюємо ImageView для фото вилки, і TextView для інформації про вилку
                    val shockIconImageView = findViewById<ImageView>(R.id.shock_icon)
                    val shockInfoTextView = findViewById<TextView>(R.id.shock)
                    shockIconImageView.setBackgroundColor(Color.TRANSPARENT)

                    // Якщо є фото, завантажуємо його за допомогою Glide
                    if (shockPhotoUri != null) {
                        Glide.with(this@MaketSetup)
                            .load(shockPhotoUri)
                            .into(shockIconImageView)
                    } else {
                        // Можна встановити стандартне зображення (як placeholder)
                        shockIconImageView.setImageResource(R.drawable.img_shock)
                    }
                    // Встановлюємо текст рядка для вилки
                    shockInfoTextView.text = shockInfo
                }
            }
            if (tyresComponent != null) {
                // Формуємо рядок із бренду, моделі та картриджу (compAdaptive)
                val tyreFront = "${tyresComponent.compBrand} ${tyresComponent.compModel}"
                val tyreRear = "${tyresComponent.compBrandExtra} ${tyresComponent.compAdaptive}"

                // Перетворюємо photoUri (якщо є) на Uri
                val tyresPhotoUri = tyresComponent.photoUri?.toUri()
                withContext(Dispatchers.Main) {
                    // Оновлюємо ImageView для фото вилки, і TextView для інформації про вилку
                    val frontTyreIcon = findViewById<ImageView>(R.id.ftyre_icon)
                    val rearTyreIcon = findViewById<ImageView>(R.id.rtyre_icon)
                    val tyreInfoFront = findViewById<TextView>(R.id.front_tyre)
                    val tyreInfoRear = findViewById<TextView>(R.id.rear_tyre)
                    frontTyreIcon.setBackgroundColor(Color.TRANSPARENT)
                    rearTyreIcon.setBackgroundColor(Color.TRANSPARENT)
                    // Якщо є фото, завантажуємо його за допомогою Glide
                    if (tyresPhotoUri != null) {
                        Glide.with(this@MaketSetup)
                            .load(tyresPhotoUri)
                            .into(frontTyreIcon)
                        Glide.with(this@MaketSetup)
                            .load(tyresPhotoUri)
                            .into(rearTyreIcon)
                    } else {
                        // Можна встановити стандартне зображення (як placeholder)
                        frontTyreIcon.setImageResource(R.drawable.img_tyre)
                        rearTyreIcon.setImageResource(R.drawable.img_tyre)
                    }
                    // Встановлюємо текст рядка для вилки
                    tyreInfoFront.text = tyreFront
                    tyreInfoRear.text = tyreRear
                }
            }
        }
    }
}