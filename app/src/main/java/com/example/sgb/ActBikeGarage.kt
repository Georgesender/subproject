package com.example.sgb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sub.R
import kotlinx.coroutines.launch

class ActBikeGarage : AppCompatActivity() {

    private lateinit var bikeNameTextView: TextView
    private lateinit var bikeSubmodelTextView: TextView
    private lateinit var bikeYearTextView: TextView
    private lateinit var bikeImageView: ImageView

    @SuppressLint("DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_bikegarage_active)
        setupBottomNavigation()

        val clearButton = findViewById<Button>(R.id.right_button_2)

        clearButton.setOnClickListener {
            // Отримуємо ID вибраного байка
            val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
            val bikeId = sharedPreferences.getInt("selected_bike_id", -1)

            if (bikeId != -1) {
                // Видаляємо байк і переходимо до AddBikeActivity
                deleteBikeAndClearPreferences(bikeId)
            }
        }


        val bikeId = intent.getIntExtra("bike_id", -1) // Отримуємо bikeId
        // Перевіряємо, чи bikeId дійсне
        if (bikeId != -1) {
            lifecycleScope.launch {
                val bikeDao = BikeDatabase.getDatabase(this@ActBikeGarage).bikeDao()
                val bike = bikeDao.getBikeById(bikeId)

                // Якщо байк знайдений
                if (bike != null) {
                    saveSelectedBikeId(bikeId)
                    bikeNameTextView.text = getString(R.string.two_strings, bike.brand, bike.modelsJson.keys.first())
                    bikeSubmodelTextView.text = bike.modelsJson.values.first().submodels.keys.first()
                    // Отримуємо рік зі структури BikeSubmodel
                    val years = bike.modelsJson.values.first().submodels.values.first().years
                    val year = years.keys.first() // Отримуємо перший рік з карти
                    bikeYearTextView.text = year

                    // Завантажуємо зображення байка
                    val imageName = bike.modelsJson.values.first().submodels.values.first().imageName
                    if (imageName != null) {
                        val resourceId = resources.getIdentifier(imageName, "drawable", packageName)
                        if (resourceId != 0) {
                            val drawable = ResourcesCompat.getDrawable(resources, resourceId, null)
                            bikeImageView.setImageDrawable(drawable)
                        }
                    }


                }
            }
        }

        setupBottomNavigation()

        val compGeometry = findViewById<View>(R.id.componentsGeometry)
        compGeometry.setOnClickListener {
            val intent = Intent(this, ActComponentsGeometry::class.java)
            intent.putExtra("bike_id", bikeId)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this, R.anim.fade_in_faster, R.anim.fade_out_faster
            )
            startActivity(intent, options.toBundle())
        }

        val frameGeometry = findViewById<View>(R.id.frameGeometry)
        frameGeometry.setOnClickListener {
            val intent = Intent(this, ActBikeGeometry::class.java)
            intent.putExtra("bike_id", bikeId) // Передаємо bikeId
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this, R.anim.fade_in_faster, R.anim.fade_out_faster
            )
            startActivity(intent, options.toBundle())
        }

        val setupBike = findViewById<View>(R.id.setups)
        setupBike.setOnClickListener {
            val intent = Intent(this, ActSetups::class.java)
            intent.putExtra("bike_id", bikeId)
            val option = ActivityOptionsCompat.makeCustomAnimation(
                this, R.anim.fade_in_faster, R.anim.fade_out_faster
            )
            startActivity(intent, option.toBundle())
        }

        val testing = findViewById<Button>(R.id.left_button)
        testing.setOnClickListener {
            startActivity(Intent(this, PreAddBikeActivity::class.java))
        }

        bikeNameTextView = findViewById(R.id.bike_name)
        bikeSubmodelTextView = findViewById(R.id.bike_submodel)
        bikeYearTextView = findViewById(R.id.bike_year)
        bikeImageView = findViewById(R.id.bike_image)
    }
    private fun saveSelectedBikeId(bikeId: Int) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("selected_bike_id", bikeId)
        editor.apply()
    }


    private fun setupBottomNavigation() {
        val navHome = findViewById<TextView>(R.id.nav_home)
        val navCompCheck = findViewById<TextView>(R.id.nav_setups)
        val navDiscover = findViewById<TextView>(R.id.nav_etc)

        navCompCheck.setOnClickListener {
            val intent = Intent(this, ComponentsCheker::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this, R.anim.fade_in_faster, R.anim.fade_out_faster
            )
            startActivity(intent, options.toBundle())
            finish()
        }

        navDiscover.setOnClickListener {
            val intent = Intent(this, Discover::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this, R.anim.fade_in_faster, R.anim.fade_out_faster
            )
            startActivity(intent, options.toBundle())
            finish()
        }

        navHome.setTypeface(null, Typeface.BOLD)
        navHome.textSize = navHome.textSize / resources.displayMetrics.density + 10
    }

    private fun deleteBikeAndClearPreferences(bikeId: Int) {
        lifecycleScope.launch {
            // Ініціалізуємо базу даних
            val database = BikeDatabase.getDatabase(this@ActBikeGarage)
            val bikeDao = database.bikeDao()
            val geometryDao = database.geometryDao()
            val componentsDao = database.componentsDao()  // Додаємо DAO для компонентів

            // Видаляємо компоненти байка
            componentsDao.deleteComponentsByBikeId(bikeId)

            // Видаляємо геометрію байка
            geometryDao.deleteGeometryByBikeId(bikeId)

            // Видаляємо сам байк
            bikeDao.deleteBikeById(bikeId)

            // Очищаємо вибір у SharedPreferences
            val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.remove("selected_bike_id")
            editor.apply()

            // Переходимо до AddBikeActivity
            val intent = Intent(this@ActBikeGarage, PreAddBikeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}