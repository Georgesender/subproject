package com.example.sgb

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.MarksForSetup
import com.example.sub.R
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import androidx.core.view.isNotEmpty
import com.example.sgb.room.MaketSetupDao
import com.example.sgb.room.SetupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.io.PrintWriter

@Suppress("DEPRECATION")
class ActSetups : AppCompatActivity() {
    private lateinit var bikeNameTextView: TextView
    private var isDeleteMode = false
    private lateinit var bpSetupDao: MaketSetupDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.kt_setups)
        } catch (e: Exception) {
            android.util.Log.e("ActSetups", "setContentView failed", e)
            val fileName = saveErrorToFile("setContentView", e)
            Toast.makeText(this, "Помилка в setContentView. Деталі збережено у файл: $fileName в папці Downloads.", Toast.LENGTH_LONG).show()
        }

        bikeNameTextView = findViewById(R.id.bike_name)
        val bikeId = intent.getIntExtra("bike_id", -1)

        // Ініціалізація бази даних
        val db = BikeDatabase.getDatabase(this)
        bpSetupDao = db.maketSetupDao()

        findViewById<Button>(R.id.back).setOnClickListener { navigateToBikeGarage(bikeId) }
        setupAlertDialog(findViewById(R.id.burger_menu), bikeId)


        if (bikeId != -1) {
            loadBikeData(bikeId)
//            loadBikeParkSetupData(bikeId)
            loadExistingSetups(bikeId)  // Додайте цей виклик
        }
    }

    // Додано функцію для завантаження даних BikePark
//    private fun loadBikeParkSetupData(bikeId: Int) {
//        lifecycleScope.launch {
//            val setupData = bpSetupDao.getSetupById(bikeId)
//            setupData?.let { populateBikeParkData(it) }
//        }
//    }

    private fun loadExistingSetups(bikeId: Int) {
        lifecycleScope.launch {
            val setups = withContext(Dispatchers.IO) {
                BikeDatabase.getDatabase(this@ActSetups)
                    .setupDao()
                    .getSetupsByBikeId(bikeId)
            }
            runOnUiThread {
                setups.forEach { setup ->
                    // Використовуємо новий метод для існуючих сетапів
                    addExistingSetupButton(setup.setupName, bikeId, setup.id)
                }
            }
        }
    }
    // Новий метод для додавання кнопки існуючого сетапу
    private fun addExistingSetupButton(name: String, bikeId: Int, setupId: Int) {
        val parent = findViewById<ConstraintLayout>(R.id.setups)
        val newButton = createSetupButton(name, bikeId, setupId)

        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )

        val lastChild = parent.getChildAt(parent.childCount - 1)
        if (lastChild != null) {
            params.topToBottom = lastChild.id
        } else {
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        }

        newButton.layoutParams = params
        parent.addView(newButton)
    }

    // Додано функцію для заповнення даних BikePark
//    private fun populateBikeParkData(data: SetupData) {
//        // Заповнення значень шока
//        findViewById<TextView>(R.id.shock_hsc_value).text = data.shockHSC.toString().takeIf { it != "0" } ?: "—"
//        findViewById<TextView>(R.id.shock_lsc_value).text = data.shockLSC.toString().takeIf { it != "0" } ?: "—"
//        findViewById<TextView>(R.id.shock_hsr_value).text = data.shockHSR.toString().takeIf { it != "0" } ?: "—"
//        findViewById<TextView>(R.id.shock_lsr_value).text = data.shockLSR.toString().takeIf { it != "0" } ?: "—"
//        findViewById<TextView>(R.id.shock_pressure_value).text = data.shockPressure.takeIf { it.isNotEmpty() } ?: "—"
//
//        // Заповнення значень вилки
//        findViewById<TextView>(R.id.fork_hsc_value).text = data.forkHSC.toString().takeIf { it != "0" } ?: "—"
//        findViewById<TextView>(R.id.fork_lsc_value).text = data.forkLSC.toString().takeIf { it != "0" } ?: "—"
//        findViewById<TextView>(R.id.fork_hsr_value).text = data.forkHSR.toString().takeIf { it != "0" } ?: "—"
//        findViewById<TextView>(R.id.fork_lsr_value).text = data.forkLSR.toString().takeIf { it != "0" } ?: "—"
//        findViewById<TextView>(R.id.fork_pressure_value).text = data.forkPressure.takeIf { it.isNotEmpty() } ?: "—"
//
//        // Заповнення тиску у шинах
//        findViewById<TextView>(R.id.front_tyre_pressure_value).text = data.frontTyrePressure.takeIf { it.isNotEmpty() } ?: "—"
//        findViewById<TextView>(R.id.rear_tyre_pressure_value).text = data.rearTyrePressure.takeIf { it.isNotEmpty() } ?: "—"
//    }

    // Решта коду залишається без змін...
    private fun saveErrorToFile(context: String, e: Exception): String {
        val timestamp = System.currentTimeMillis()
        val fileName = "error_log_$timestamp.txt"
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = contentResolver.insert(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                },
                contentValues
            )

            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    val writer = PrintWriter(OutputStreamWriter(outputStream))
                    writer.println("=== Error at $timestamp ===")
                    writer.println("Context: $context")
                    writer.println("Message: ${e.message}")
                    writer.println("Stack Trace:")
                    e.printStackTrace(writer)
                    writer.println("=== End of Error ===")
                    writer.println()
                    writer.close()
                }
                android.util.Log.e("ActSetups", "Error saved to Downloads: $fileName")
                return fileName
            }
        } catch (fileEx: Exception) {
            android.util.Log.e("ActSetups", "Failed to save error to file", fileEx)
        }
        return "unknown"
    }

    private fun navigateToBikeGarage(bikeId: Int) {
        val intent = Intent(this, ActBikeGarage::class.java).apply {
            putExtra("bike_id", bikeId)
        }
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this, R.anim.fade_in_faster, R.anim.fade_out_faster
        )
        startActivity(intent, options.toBundle())
        finish()
    }

    private fun navigateToMaketSetup(bikeId: Int, checkingText: String) {
        val intent = Intent(this, MaketSetup::class.java).apply {
            putExtra("bike_id", bikeId)
            putExtra("BikePark", checkingText)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_faster, R.anim.fade_out_faster)
    }

    private fun loadBikeData(bikeId: Int) {
        lifecycleScope.launch {
            val bikeDao = BikeDatabase.getDatabase(this@ActSetups).bikeDao()
            bikeDao.getBikeById(bikeId)?.let { bike ->
                saveSelectedBikeId(bikeId)
                bikeNameTextView.text = getString(R.string.two_strings, bike.brand, bike.modelsJson.keys.first())
            }
        }
    }

    private fun saveSelectedBikeId(bikeId: Int) {
        getSharedPreferences("bike_prefs", MODE_PRIVATE).edit().apply {
            putInt("selected_bike_id", bikeId)
            apply()
        }
    }

    private fun setupAlertDialog(burgerMenuButton: Button, bikeId: Int) {
        val items = arrayOf("Додати сетап", "Видалити сетап")

        burgerMenuButton.setOnClickListener {
            AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
                .setTitle("Меню")
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> showNameInputDialog(bikeId)
                        1 -> activateDeleteMode(burgerMenuButton, bikeId)
                    }
                }
                .show()
        }
    }

    private fun showNameInputDialog(bikeId: Int) {
        val dialogView = layoutInflater.inflate(R.layout.di_textwriter, null)
        val inputText = dialogView.findViewById<EditText>(R.id.inputText)

        AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            .setView(dialogView)
            .create()
            .apply {
                dialogView.findViewById<Button>(R.id.okButton).setOnClickListener {
                    val setupName = inputText.text.toString().trim()
                    if (setupName.isNotEmpty()) {
                        addSetupButton(setupName, bikeId)
                        dismiss()
                    } else {
                        Toast.makeText(this@ActSetups, "Назва не може бути порожньою!", Toast.LENGTH_SHORT).show()
                    }
                }
                show()
            }
    }

    // Змініть метод addSetupButton
    private fun addSetupButton(name: String, bikeId: Int) {
        lifecycleScope.launch {
            val setupDao = BikeDatabase.getDatabase(this@ActSetups).setupDao()
            val maketSetupDao = BikeDatabase.getDatabase(this@ActSetups).maketSetupDao()

            // Створюємо новий запис у setups_table
            val newSetup = MarksForSetup(bikeId = bikeId, setupName = name)
            val setupId = setupDao.insertSetup(newSetup).toInt()  // Тепер працює

            // Створюємо відповідний запис у maket_setup_table
            val newSetupData = SetupData(bikeId = bikeId, setupId = setupId)
            maketSetupDao.insertSetup(newSetupData)

            // Додаємо кнопку на UI
            runOnUiThread {
                val parent = findViewById<ConstraintLayout>(R.id.setups)
                val newButton = createSetupButton(name, bikeId, setupId)
                parent.addView(newButton)
            }
        }
    }

    // Оновіть метод createSetupButton
    private fun createSetupButton(name: String, bikeId: Int, setupId: Int): Button {
        return Button(this).apply {
            text = name
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.white, theme))
            setBackgroundResource(R.drawable.kt_textviews_basic)
            id = View.generateViewId()

            setOnClickListener {
                if (isDeleteMode) {
                    deleteSetup(this, bikeId, name, setupId)
                } else {
                    openSetup(name, bikeId, setupId)
                }
            }
        }
    }

    // Оновіть метод deleteSetup
    private fun deleteSetup(button: Button, bikeId: Int, name: String, setupId: Int) {
        lifecycleScope.launch {
            BikeDatabase.getDatabase(this@ActSetups).setupDao().deleteSetup(MarksForSetup(setupId, bikeId, name))
            findViewById<ConstraintLayout>(R.id.setups).removeView(button)
        }
    }

    private fun openSetup(name: String, bikeId: Int, setupId: Int?) {
        val intent = Intent(this, MaketSetup::class.java).apply {
            putExtra("setup_name", name)
            putExtra("bike_id", bikeId)
            putExtra("setup_id", setupId)
        }
        startActivity(intent)
    }

    private fun saveSetupToDatabase(name: String, bikeId: Int) {
        lifecycleScope.launch {
            BikeDatabase.getDatabase(this@ActSetups).setupDao().insertSetup(MarksForSetup(0, bikeId, name))
        }
    }

    private fun activateDeleteMode(burgerMenuButton: Button, bikeId: Int) {
        isDeleteMode = true
        burgerMenuButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, theme))
        burgerMenuButton.setOnClickListener {
            isDeleteMode = false
            burgerMenuButton.setBackgroundResource(R.drawable.btn_burger)
            setupAlertDialog(burgerMenuButton, bikeId)
        }
    }
}