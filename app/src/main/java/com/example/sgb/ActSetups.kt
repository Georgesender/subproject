package com.example.sgb

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sub.R
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class ActSetups : AppCompatActivity() {
    private lateinit var bikeNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_setups)

        bikeNameTextView = findViewById(R.id.bike_name)
        val bikeId = intent.getIntExtra("bike_id", -1) // Отримуємо bikeId

        val backButton: Button = findViewById(R.id.back)
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in_faster, R.anim.fade_out_faster) // Додаємо анімацію
        }

        // Кнопка для відображення меню
        val burgerMenuButton: Button = findViewById(R.id.burger_menu)
        setupAlertDialog(burgerMenuButton)

        // Перевіряємо, чи bikeId дійсне
        if (bikeId != -1) {
            lifecycleScope.launch {
                val bikeDao = BikeDatabase.getDatabase(this@ActSetups).bikeDao()
                val bike = bikeDao.getBikeById(bikeId)

                // Якщо байк знайдений
                if (bike != null) {
                    saveSelectedBikeId(bikeId)
                    bikeNameTextView.text = getString(R.string.bike_name, bike.brand, bike.modelsJson.keys.first())
                }
            }
        }
    }

    private fun saveSelectedBikeId(bikeId: Int) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("selected_bike_id", bikeId)
        editor.apply()
    }

    // Відображення AlertDialog для меню
    private fun setupAlertDialog(button: Button) {
        val items = arrayOf("Додати сетап", "Видалити сетап")
        val setupsContainer: androidx.constraintlayout.widget.ConstraintLayout = findViewById(R.id.setups)

        button.setOnClickListener {
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            builder.setTitle("Меню")
            builder.setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        // Відкриваємо діалог для введення назви
                        showNameInputDialog(setupsContainer)
                    }
                    1 -> Toast.makeText(this, "Вибрано: Видалити сетап", Toast.LENGTH_SHORT).show()
                }
            }
            builder.show()
        }
    }

    private fun showNameInputDialog(container: androidx.constraintlayout.widget.ConstraintLayout) {
        // Створення діалогового вікна з кастомним лейаутом
        val dialogView = layoutInflater.inflate(R.layout.di_textwriter, null)
        val inputText = dialogView.findViewById<EditText>(R.id.inputText)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            .setView(dialogView)
            .create()

        okButton.setOnClickListener {
            val setupName = inputText.text.toString().trim()
            if (setupName.isNotEmpty()) {
                // Додаємо кнопку до контейнера з введеною назвою
                addSetupButton(container, setupName)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Назва не може бути порожньою!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun addSetupButton(container: androidx.constraintlayout.widget.ConstraintLayout, name: String) {
        val newButton = Button(this).apply {
            text = name
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.white, theme))
            setBackgroundResource(R.drawable.kt_textviews_basic) // Приклад стилю
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = container.id
                startToStart = container.id
                marginStart = 16 // Відступи
                topMargin = 16
            }
        }
        container.addView(newButton)
    }
}
