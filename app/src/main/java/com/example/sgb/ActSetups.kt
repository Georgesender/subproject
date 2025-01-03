package com.example.sgb

import android.content.Intent
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
import com.example.sgb.room.MarksForSetup
import com.example.sub.R
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class ActSetups : AppCompatActivity() {
    private lateinit var bikeNameTextView: TextView
    private var isDeleteMode = false // Флаг для режиму видалення
    private var buttonIdCounter = 1 // Лічильник для ID кнопок

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_setups)

        bikeNameTextView = findViewById(R.id.bike_name)
        val bikeId = intent.getIntExtra("bike_id", -1) // Отримуємо bikeId

        val backButton: Button = findViewById(R.id.back)
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in_faster, R.anim.fade_out_faster)
        }

        val burgerMenuButton: Button = findViewById(R.id.burger_menu)
        val setupsContainer: androidx.constraintlayout.widget.ConstraintLayout = findViewById(R.id.setups)

        setupAlertDialog(burgerMenuButton, backButton, setupsContainer, bikeId)

        if (bikeId != -1) {
            lifecycleScope.launch {
                val bikeDao = BikeDatabase.getDatabase(this@ActSetups).bikeDao()
                val bike = bikeDao.getBikeById(bikeId)

                if (bike != null) {
                    saveSelectedBikeId(bikeId)
                    bikeNameTextView.text = getString(R.string.bike_name, bike.brand, bike.modelsJson.keys.first())
                    // Не завантажуємо кнопки одразу
                }
            }
        }

        // Тут кнопки не створюються автоматично
    }

    private fun saveSelectedBikeId(bikeId: Int) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("selected_bike_id", bikeId)
        editor.apply()
    }

    private fun addSetupButton(container: androidx.constraintlayout.widget.ConstraintLayout, name: String, bikeId: Int, setupId: Int? = null) {
        // Створення нової кнопки
        val newButton = Button(this).apply {
            text = name
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.white, theme))
            setBackgroundResource(R.drawable.kt_textviews_basic)

            // Присвоєння унікального ID для кнопки
            id = buttonIdCounter++

            val layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            )

            layoutParams.apply {
                // Якщо є інші кнопки, то кнопка повинна бути розміщена під попередньою
                if (container.childCount > 0) {
                    val lastButton = container.getChildAt(container.childCount - 1)
                    topToBottom = lastButton.id
                } else {
                    topToTop = container.id
                }
                startToStart = container.id
                marginStart = 16
                topMargin = 16
            }

            this.layoutParams = layoutParams

            // Дія по натисканню на кнопку
            setOnClickListener {
                if (isDeleteMode) {
                    lifecycleScope.launch {
                        val setupDao = BikeDatabase.getDatabase(this@ActSetups).setupDao()
                        setupDao.deleteSetup(MarksForSetup(setupId ?: 0, bikeId, name))
                        container.removeView(this@apply) // Видалення кнопки
                    }
                } else {
                    val intent = Intent(this@ActSetups, MaketSetup::class.java)
                    intent.putExtra("setup_name", name)
                    intent.putExtra("bike_id", bikeId)
                    intent.putExtra("setup_id", setupId) // Передаємо ID сетапу
                    startActivity(intent)
                }
            }
        }

        // Додаємо нову кнопку до контейнера
        container.addView(newButton)

        // Якщо це новий сетап, додаємо його до бази даних
        if (setupId == null) {
            lifecycleScope.launch {
                val setupDao = BikeDatabase.getDatabase(this@ActSetups).setupDao()
                setupDao.insertSetup(MarksForSetup(0, bikeId, name)) // Додаємо новий сетап
            }
        }
    }

    private fun setupAlertDialog(
        burgerMenuButton: Button,
        backButton: Button,
        setupsContainer: androidx.constraintlayout.widget.ConstraintLayout,
        bikeId: Int
    ) {
        val items = arrayOf("Додати сетап", "Видалити сетап")

        burgerMenuButton.setOnClickListener {
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            builder.setTitle("Меню")
            builder.setItems(items) { _, which ->
                when (which) {
                    0 -> showNameInputDialog(setupsContainer, bikeId) // Додати новий сетап
                    1 -> {
                        Toast.makeText(this, "Режим видалення активовано", Toast.LENGTH_SHORT).show()
                        enableDeleteMode(burgerMenuButton, backButton, setupsContainer, bikeId)
                    }
                }
            }
            builder.show()
        }
    }

    private fun showNameInputDialog(container: androidx.constraintlayout.widget.ConstraintLayout, bikeId: Int) {
        val dialogView = layoutInflater.inflate(R.layout.di_textwriter, null)
        val inputText = dialogView.findViewById<EditText>(R.id.inputText)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            .setView(dialogView)
            .create()

        okButton.setOnClickListener {
            val setupName = inputText.text.toString().trim()
            if (setupName.isNotEmpty()) {
                addSetupButton(container, setupName, bikeId)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Назва не може бути порожньою!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun enableDeleteMode(
        burgerMenuButton: Button,
        backButton: Button,
        setupsContainer: androidx.constraintlayout.widget.ConstraintLayout,
        bikeId: Int
    ) {
        isDeleteMode = true
        burgerMenuButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, theme))
        backButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, theme))

        burgerMenuButton.setOnClickListener {
            isDeleteMode = false
            burgerMenuButton.setBackgroundResource(R.drawable.img_burger)
            backButton.setBackgroundResource(R.drawable.img_back_button)
            setupAlertDialog(burgerMenuButton, backButton, setupsContainer, bikeId)
        }
    }
}
