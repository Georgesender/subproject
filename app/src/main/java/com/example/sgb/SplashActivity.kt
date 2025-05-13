package com.example.sgb

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sgb.utils.BlurUtils
import com.example.sub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var rootLayout: ViewGroup
    private lateinit var blurOverlay: ImageView
    private lateinit var appName: TextView
    private lateinit var logo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        rootLayout = findViewById(R.id.splash_root)
        blurOverlay = findViewById(R.id.blur_overlay)
        appName = findViewById(R.id.name_of_app)
        logo = findViewById(R.id.logo_image)
        // Запускаємо фонова завантаження даних
        // Отримання SharedPreferences для перевірки selectedBikeId
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs" , MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id" , -1)
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
                    finish()
                } else  {
                    startIntroAnimation {
                        val intent = Intent(this@SplashActivity, MainBikeGarage::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        }
                        startActivity(intent)
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
        blurOverlay.visibility = View.VISIBLE
        logo.visibility = View.VISIBLE
        appName.visibility = View.VISIBLE

        logo.post {
            // 1) Logo clip-in from top-right
            val lw = logo.width
            val lh = logo.height
            logo.clipBounds = Rect(lw, 0, lw, lh)
            val logoAnim = ValueAnimator.ofInt(0, lw).apply {
                duration = 1500L
                addUpdateListener { anim ->
                    val p = anim.animatedValue as Int
                    logo.clipBounds = Rect(lw - p, 0, lw, lh)
                }
            }

            appName.post {
                // 2) AppName clip-in from left
                val tw = appName.width
                val th = appName.height
                appName.clipBounds = Rect(0, 0, 0, th)
                val textAnim = ValueAnimator.ofInt(0, tw).apply {
                    duration = 2500L
                    addUpdateListener { anim ->
                        val p = anim.animatedValue as Int
                        appName.clipBounds = Rect(0, 0, p, th)
                    }
                }

                // 3) Depth fade-out and background overlay together
                val density = resources.displayMetrics.density
                // Logo depth animators
                val logoDepthScaleX = ValueAnimator.ofFloat(1f, 0.5f).apply {
                    duration = 2500L
                    addUpdateListener { a -> logo.scaleX = a.animatedValue as Float }
                }
                val logoDepthScaleY = ValueAnimator.ofFloat(1f, 0.5f).apply {
                    duration = 2500L
                    addUpdateListener { a -> logo.scaleY = a.animatedValue as Float }
                }
                val logoDepthAlpha = ValueAnimator.ofFloat(1f, 0f).apply {
                    duration = 2500L
                    addUpdateListener { a -> logo.alpha = a.animatedValue as Float }
                }
                // Text depth animators
                val textDepthScaleX = ValueAnimator.ofFloat(1f, 0.5f).apply {
                    duration = 2500L
                    addUpdateListener { a -> appName.scaleX = a.animatedValue as Float }
                }
                val textDepthScaleY = ValueAnimator.ofFloat(1f, 0.5f).apply {
                    duration = 2500L
                    addUpdateListener { a -> appName.scaleY = a.animatedValue as Float }
                }
                val textDepthAlpha = ValueAnimator.ofFloat(1f, 0f).apply {
                    duration = 2500L
                    addUpdateListener { a -> appName.alpha = a.animatedValue as Float }
                }
                // Background padding + blur
                val paddingAnim = ValueAnimator.ofInt(0, (24 * density).toInt()).apply {
                    duration = 2500L
                    addUpdateListener { a -> rootLayout.setPadding(a.animatedValue as Int) }
                }
                val blurAnim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ValueAnimator.ofFloat(0f, BlurUtils.BLUR_RADIUS).apply {
                        duration = 2500L
                        addUpdateListener { a -> BlurUtils.applyBlur(blurOverlay, a.animatedValue as Float) }
                    }
                } else null

                val combinedFadeOutSet = AnimatorSet().apply {
                    playTogether(listOfNotNull(
                        logoDepthScaleX, logoDepthScaleY, logoDepthAlpha,
                        textDepthScaleX, textDepthScaleY, textDepthAlpha,
                        paddingAnim, blurAnim
                    ))
                    interpolator = AccelerateDecelerateInterpolator()
                }

                // Sequence: logo in, text in, then combined fade-out + overlay
                AnimatorSet().apply {
                    playSequentially(logoAnim, textAnim, combinedFadeOutSet)
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            logo.visibility = View.GONE
                            appName.visibility = View.GONE
                            onEnd()
                        }
                    })
                    combinedFadeOutSet.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            blurOverlay.visibility = View.VISIBLE
                        }
                    })
                    start()
                }
            }
        }
    }

    // Extension to set equal padding
    private fun ViewGroup.setPadding(p: Int) {
        setPadding(p, p, p, p)
    }
}


