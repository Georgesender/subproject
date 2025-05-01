package com.example.sgb

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainBikeGarage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_bikegarage)

        val addBikeButton = findViewById<Button>(R.id.add_bike_button)
        val bikeDao = BikeDatabase.getDatabase(this).bikeDao()

        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)

        if (selectedBikeId != -1) {
            // Якщо байк вибрано, відкриваємо BikeGarageAct з ID байка
            val intent = Intent(this@MainBikeGarage, ActBikeGarage::class.java)
            intent.putExtra("bike_id", selectedBikeId)
            startActivity(intent)
            finish()
        } else {
            // Без вибраного байка – перевіряємо, чи є в БД хоч один
            lifecycleScope.launch {
                val allBikes = bikeDao.getAllBikes()
                if (allBikes.isNotEmpty()) {
                    // Якщо є хоча б один – йдемо в загальний гараж
                    startActivity(Intent(this@MainBikeGarage, GarageActivity::class.java))
                    finish()
                } else {
                    // Якщо ні – показуємо кнопку "додати" і реагуємо на натискання
                    withContext(Dispatchers.Main) {
                        addBikeButton.isEnabled = true
                        addBikeButton.setOnClickListener {
                            startActivity(Intent(this@MainBikeGarage, PreAddBikeActivity::class.java))
                        }
                    }
                }
            }
        }
    }
}
