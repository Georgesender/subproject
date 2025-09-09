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
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.MarksForSetup
import com.example.sub.R
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import androidx.core.view.isNotEmpty
import java.io.OutputStreamWriter
import java.io.PrintWriter

@Suppress("DEPRECATION")
class ActSetups : AppCompatActivity() {
    private lateinit var bikeNameTextView: TextView
    private var isDeleteMode = false

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

        findViewById<Button>(R.id.back).setOnClickListener { navigateToBikeGarage(bikeId) }
        setupAlertDialog(findViewById(R.id.burger_menu), bikeId)
        findViewById<MaterialCardView>(R.id.bikepark_card).setOnClickListener {
            navigateToMaketSetup(bikeId, "BikePark")
        }

        if (bikeId != -1) loadBikeData(bikeId)
    }

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

    private fun addSetupButton(name: String, bikeId: Int, setupId: Int? = null) {
        val parent = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.setups)

        try {
            val newButton = Button(this@ActSetups).apply {
                text = name
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.white, theme))
                setBackgroundResource(R.drawable.kt_textviews_basic)
                // безпечний унікальний id
                id = View.generateViewId()

                // подія кліку
                setOnClickListener {
                    if (isDeleteMode) deleteSetup(this, bikeId, name, setupId) else openSetup(name, bikeId, setupId)
                }
            }

            // правильно формуємо LayoutParams для ConstraintLayout
            val lp = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // відступи в dp -> px
                val margin16 = (16 * resources.displayMetrics.density).toInt()

                /* якщо в parent вже є діти — прив'язуємо topToBottom до останнього діда */
                if (parent.isNotEmpty()) {
                    val lastChild = parent.getChildAt(parent.childCount - 1)
                    // переконаємося, що у останнього є id
                    val anchorId = if (lastChild.id != View.NO_ID) lastChild.id else androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    topToBottom = anchorId
                    topMargin = margin16
                } else {
                    // перший елемент — прив'язуємо до top of parent
                    topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    topMargin = margin16
                }

                // горизонтальна прив'язка до початку parent
                startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = margin16
            }

            newButton.layoutParams = lp

            // додаємо в parent
            parent.addView(newButton)

            // зберегти в БД, якщо треба
            if (setupId == null) saveSetupToDatabase(name, bikeId)
        } catch (e: Exception) {
            // логнемо і показуємо, щоб мати інформацію про помилку
            android.util.Log.e("ActSetups", "addSetupButton failed", e)
            val fileName = saveErrorToFile("addSetupButton", e)
            Toast.makeText(this@ActSetups, "Помилка при додаванні кнопки. Деталі збережено у файл: $fileName в папці Downloads.", Toast.LENGTH_LONG).show()
        }
    }


    private fun deleteSetup(button: Button, bikeId: Int, name: String, setupId: Int?) {
        lifecycleScope.launch {
            BikeDatabase.getDatabase(this@ActSetups).setupDao().deleteSetup(MarksForSetup(setupId ?: 0, bikeId, name))
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.setups).removeView(button)
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