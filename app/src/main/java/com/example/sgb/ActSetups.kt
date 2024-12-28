package com.example.sgb

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
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

        button.setOnClickListener {
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            builder.setTitle("Меню")
            builder.setItems(items) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Вибрано: Додати сетап", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Вибрано: Видалити сетап", Toast.LENGTH_SHORT).show()
                }
            }
            builder.show()
        }
    }

}
