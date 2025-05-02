package com.example.sgb

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sgb.utils.BlurUtils
import com.example.sub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainBikeGarage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_bikegarage)

        // 1) Знаходимо в’юшки
        val imageView = findViewById<ImageView>(R.id.main_image)
        val firstText = findViewById<TextView>(R.id.first_text)
        val addBikeButton = findViewById<Button>(R.id.add_bike_button)

        // Статичне розмиття з тією ж константою
        BlurUtils.applyBlur(imageView, BlurUtils.BLUR_RADIUS)

        // Спочатку ховаємо обидва
        firstText.alpha = 0f
        addBikeButton.alpha = 0f

        // Плавне показування тексту (fade-in за 600 мс)
        firstText.animate()
            .alpha(1f)
            .setDuration(600)
            .start()

        // 4) Логіка показу кнопки
        val bikeDao = BikeDatabase.getDatabase(this).bikeDao()
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)

        if (selectedBikeId != -1) {
            // якщо байк вже вибрано
            startActivity(Intent(this, ActBikeGarage::class.java).apply {
                putExtra("bike_id", selectedBikeId)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            })
            finish()
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                val allBikes = bikeDao.getAllBikes()
                withContext(Dispatchers.Main) {
                    if (allBikes.isNotEmpty()) {
                        startActivity(Intent(this@MainBikeGarage, GarageActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                        finish()
                    } else {
                        // показуємо і анімуємо кнопку
                        addBikeButton.apply {
                            isEnabled = true
                            animate()
                                .alpha(1f)
                                .setDuration(600)
                                .start()

                            setOnClickListener {
                                startActivity(Intent(this@MainBikeGarage, PreAddBikeActivity::class.java))
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            }
                        }
                    }
                }
            }
        }
    }
}
