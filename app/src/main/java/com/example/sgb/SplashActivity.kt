package com.example.sgb

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sgb.utils.BlurUtils
import com.example.sub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.java

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var rootLayout: ViewGroup
    private lateinit var blurOverlay: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        rootLayout = findViewById(R.id.splash_root)
        blurOverlay = findViewById(R.id.blur_overlay)
        // Запускаємо фонова завантаження даних
        // Отримання SharedPreferences для перевірки selectedBikeId
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)
        lifecycleScope.launch(Dispatchers.IO) {

            val bikeDao = BikeDatabase.getDatabase(applicationContext).bikeDao()
            val allBikes = bikeDao.getAllBikes()
            withContext(Dispatchers.Main) {
                if (selectedBikeId != -1) {
                    loadInitialData()
                    // якщо байк вже вибрано
                    startActivity(Intent(this@SplashActivity, ActBikeGarage::class.java).apply {
                        putExtra("bike_id", selectedBikeId)
                        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    })
                    finish()
                } else if (allBikes.isNotEmpty()) {
                    // Якщо є байки в базі, переходимо до GarageActivity без анімації
                    startActivity(Intent(this@SplashActivity, GarageActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    })
                    overridePendingTransition(0, 0)
                    finish()
                } else  {
                    startIntroAnimation {
                        val intent = Intent(this@SplashActivity, MainBikeGarage::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        }
                        startActivity(intent)
                        // Якщо потрібно, все одно можна додати overridePendingTransition(0, 0)
                        overridePendingTransition(0, 0)
                        finish()
                    }
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

    private fun startIntroAnimation(onEnd: () -> Unit) {
        // 1) Показуємо blur-overlay
        blurOverlay.visibility = View.VISIBLE

        // 2) Готуємо аніматори
        val density = resources.displayMetrics.density
        val targetPadding = (24 * density).toInt()
        val paddingAnimator = ValueAnimator.ofInt(0, targetPadding).apply {
            addUpdateListener { anim ->
                val pad = anim.animatedValue as Int
                // додаємо паддінг з кожного боку
                rootLayout.setPadding(pad, pad, pad, pad)
            }
        }

        // blur-аніматор з використанням константи BLUR_RADIUS
        val blurAnimator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ValueAnimator.ofFloat(0f, BlurUtils.BLUR_RADIUS).apply {
                addUpdateListener { anim ->
                    BlurUtils.applyBlur(blurOverlay, anim.animatedValue as Float)
                }
            }
        } else null

        // 3) Запускаємо їх разом
        val animators = mutableListOf<Animator>(paddingAnimator)
        blurAnimator?.let { animators += it }

        AnimatorSet().apply {
            playTogether(animators)
            duration = 2000L   // тривалість анімації в мс
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            start()
        }
    }
}

