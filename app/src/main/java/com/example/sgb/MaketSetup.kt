package com.example.sgb

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sub.R
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class MaketSetup : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.maket_setup)

        val backBttn: Button = findViewById(R.id.back)
        backBttn.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in_faster, R.anim.fade_out_faster)
        }

        val setupName = intent.getStringExtra("setup_name") ?: "Невідомий сетап"
        val setupId = intent.getIntExtra("setup_id", -1)

        val textView: TextView = findViewById(R.id.setup_data)
        textView.text = setupName

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
}
