package com.example.sgb

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.sub.R

class ComponentsCheker : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_components_checker)
        setupBottomNavigation()


    }

    private fun setupBottomNavigation() {
        // Знайти кнопки
        val navHome = findViewById<TextView>(R.id.nav_home)
        val navCompCheck = findViewById<TextView>(R.id.nav_setups)
        val navDiscover = findViewById<TextView>(R.id.nav_etc)

        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)

        navHome.setOnClickListener {
            if (selectedBikeId != -1) {
                val intent = Intent(this@ComponentsCheker, ActBikeGarage::class.java)
                intent.putExtra("bike_id", selectedBikeId)
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.fade_in, R.anim.fade_out
                )
                startActivity(intent, options.toBundle())
                finish()
            }
        }
        navDiscover.setOnClickListener {
            val intent = Intent(this, Discover::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this,
                R.anim.fade_in,
                R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
            finish()
        }
        navCompCheck.setTypeface(null, Typeface.BOLD) // Жирний текст
        navCompCheck.textSize = navCompCheck.textSize / resources.displayMetrics.density + 10
    }
}
