package com.example.sgb

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.Component
import com.example.sgb.room.ComponentsDao
import com.example.sub.R
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class MaketSetup : AppCompatActivity() {
    private lateinit var fork: TextView
    private lateinit var shock: TextView
    private lateinit var fTyre: TextView
    private lateinit var rTyre: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.maket_setup)

        fork = findViewById(R.id.fork)
        shock = findViewById(R.id.shock)
        fTyre = findViewById(R.id.front_tyre)
        rTyre = findViewById(R.id.rear_tyre)

        val backBttn: Button = findViewById(R.id.back)
        backBttn.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in_faster, R.anim.fade_out_faster)
        }

        val setupName = intent.getStringExtra("setup_name") ?: "Невідомий сетап"
        val setupId = intent.getIntExtra("setup_id", -1)

        val textView: TextView = findViewById(R.id.setup_name)
        textView.text = setupName

        val bikeId = intent.getIntExtra("bike_id", -1) // Отримуємо bikeId
        val componentsDao = BikeDatabase.getDatabase(this).componentsDao()
        if (bikeId != -1) {
            loadBikeData(bikeId, componentsDao)
        }

        if (setupId != -1) {
            lifecycleScope.launch {
                val setupDao = BikeDatabase.getDatabase(this@MaketSetup).setupDao()
                val setup = setupDao.getSetupById(setupId)


                setup?.let {
                    // Вивести дані для сетапу
                    textView.append("\nДані: ${it.setupName}")
                }
            }
        }
    }

    private fun loadBikeData(bikeId: Int, componentsDao: ComponentsDao) {
        lifecycleScope.launch {
            val components = componentsDao.getComponentsByBikeId(bikeId)
                ?: Component(bikeId = bikeId).also { componentsDao.insertComponent(it) }
            fork.text =
                getString(R.string.two_strings, components.forkBrand, components.forkSeries)
            shock.text =
                getString(R.string.two_strings, components.shockBrand, components.shockSeries)
            fTyre.text =
                getString(R.string.two_strings, components.forkBrand, components.frontTyreSeries)
            rTyre.text =
                getString(R.string.two_strings, components.frontTyreBrand, components.rearTyreSeries)



        }
    }
}
