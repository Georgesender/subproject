package com.example.sgb

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
        val addBikeButton = findViewById<Button>(R.id.add_bike_button)
        val rootLayout = findViewById<ConstraintLayout>(R.id.root)
        val density = resources.displayMetrics.density
        // Статичне розмиття з тією ж константою
        BlurUtils.applyBlur(imageView, BlurUtils.BLUR_RADIUS)

        addBikeButton.apply {
            // Початковий стан: невидима, нульових розмірів
            alpha = 0f
            scaleX = 0f
            scaleY = 0f
            isEnabled = true

            // Запускаємо анімацію зміни прозорості та масштабу
            animate()
                .alpha(1f)           // від 0 → 1
                .scaleX(1f)          // від 0 → норм.
                .scaleY(1f)          // від 0 → норм.
                .setDuration(1000)    // тривалість в мс
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        addBikeButton.apply {
            isEnabled = true
            setOnClickListener {
                // Анімація зникнення кнопки
                animate()
                    .alpha(0f)           // Прозорість до 0
                    .setDuration(300)    // Тривалість 300 мс
                    .withEndAction {
                        isEnabled = false // Вимикаємо кнопку після зникнення
                        // Запускаємо основну анімацію
                        startMainAnimation(imageView, rootLayout, density)
                    }
                    .start()
            }
        }

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
                        }
                    }
                }
            }
        }
    }

    private fun startMainAnimation(imageView: ImageView, rootLayout: ConstraintLayout, density: Float) {
        val targetWidth = (30 * density).toInt()
        val startWidth = imageView.width

        val widthAnim = ValueAnimator.ofInt(startWidth, targetWidth).apply {
            addUpdateListener { anim ->
                val lp = imageView.layoutParams
                lp.width = anim.animatedValue as Int
                imageView.layoutParams = lp
            }
        }

        // Отримуємо поточний padding (наприклад, 24dp)
        val startPadding = (24 * density).toInt()
        val targetPadding = (8 * density).toInt()

        val paddingAnim = ValueAnimator.ofInt(startPadding, targetPadding).apply {
            duration = 750
            addUpdateListener { a ->
                val paddingValue = a.animatedValue as Int
                rootLayout.setPadding(paddingValue, paddingValue, paddingValue, paddingValue)
            }
        }

        AnimatorSet().apply {
            playSequentially(widthAnim, paddingAnim)
            duration = 1500
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val intent = Intent(this@MainBikeGarage, PreAddBikeActivity::class.java)
                    val options = ActivityOptions.makeCustomAnimation(
                        this@MainBikeGarage,
                        0, // Вхідна анімація
                        0  // Вихідна анімація
                    )
                    startActivity(intent, options.toBundle())
                    finish()
                }
            })
            start()
        }
    }
}