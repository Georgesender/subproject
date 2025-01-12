package com.example.sgb

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BPSetupDao
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.BikeParkSetupData
import com.example.sgb.room.Component
import com.example.sgb.room.ComponentsDao
import com.example.sub.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.maket_setup)

        // Отримання даних з Intent
        val bikeId = intent.getIntExtra("bike_id", -1)
        val setupName = intent.getStringExtra("setup_name")
        val checkedText = intent.getStringExtra("BikePark")
        val setupId = intent.getIntExtra("setup_id", -1)

        // Обробка натискання на fork_marks
        findViewById<TextView>(R.id.fork_marks).setOnClickListener {
            showSetupDialog("вилки")
        }

        // Обробка натискання на shock_marks
        findViewById<TextView>(R.id.shock_marks).setOnClickListener {
            showSetupDialog("аморта")
        }

        // Ініціалізація View
        initView()

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
                tyreNotes to "tyreNotes"
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
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showSetupDialog(componentName: String) {
        // Надування макета для діалогового вікна
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_marks_for_setups, null)

        // Оновлення тексту заголовка
        val titleTextView = dialogView.findViewById<TextView>(R.id.tv_setup_rating_title)
        titleTextView.text = "Оцінка сетапу для $componentName"

        // Побудова та відображення діалогового вікна
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialogBuilder.window?.setBackgroundDrawable(ColorDrawable(transparentColor))


        dialogBuilder.show()
    }

}
