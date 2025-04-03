package com.example.sgb

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.java

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Запускаємо фонова завантаження даних
        lifecycleScope.launch(Dispatchers.IO) {
            val dataLoaded = loadInitialData()

            withContext(Dispatchers.Main) {
                if (dataLoaded) {
                    startActivity(Intent(this@SplashActivity, MainBikeGarage::class.java))
                    finish()
                } else {
                    // Тут можна відобразити повідомлення про помилку або щось інше
                    Toast.makeText(this@SplashActivity, "Помилка завантаження даних", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun loadInitialData(): Boolean {
        return try {
            val database = BikeDatabase.getDatabase(applicationContext)
            val bikeDao = database.bikeDao()
            val geometryDao = database.geometryDao()
            val componentsDao = database.componentsDao()
            val bpSetupDao = database.bpSetupDao()
            val bpMarksSuspensionDao = database.bpMarksSusDao()

            // Завантаження всіх даних
            // Викликаємо методи без збереження в змінні
            bikeDao.getBikeById(1)
            geometryDao.getGeometryByBikeId(1)
            componentsDao.getComponentsByBikeId(1)
            bpSetupDao.getBikeParkSetupById(1)
            bpMarksSuspensionDao.getBpMarksSusByBikeId(1)
            // Симулюємо коротку затримку для реалістичності (наприклад, якщо є API-запити)
            delay(500)

            true // Якщо все завантажено без помилок
        } catch (e: Exception) {
            e.printStackTrace()
            false // Помилка при завантаженні
        }
    }
}
