package com.example.sgb

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.sub.R

class MainBikeGarage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bikegarage)

        val addBikeButton = findViewById<Button>(R.id.add_bike_button)

        // Перевіряємо, чи є вибраний байк у SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)

        if (selectedBikeId != -1) {
            // Якщо байк вибрано, відкриваємо BikeGarageAct з ID байка
            val intent = Intent(this@MainBikeGarage, BikeGarageAct::class.java)
            intent.putExtra("bike_id", selectedBikeId)
            startActivity(intent)
            finish()
        } else {
            // Якщо байк не вибрано, показуємо кнопку додавання
            addBikeButton.setOnClickListener {
                val intent = Intent(this@MainBikeGarage, PreAddBikeActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
