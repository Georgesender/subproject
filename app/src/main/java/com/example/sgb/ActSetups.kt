package com.example.sgb

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.MarksForSetup
import com.example.sub.R
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class ActSetups : AppCompatActivity() {
    private lateinit var bikeNameTextView: TextView
    private var isDeleteMode = false
    private var buttonIdCounter = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_setups)

        bikeNameTextView = findViewById(R.id.bike_name)
        val bikeId = intent.getIntExtra("bike_id", -1)

        findViewById<Button>(R.id.back).setOnClickListener { navigateToBikeGarage(bikeId) }
        setupAlertDialog(findViewById(R.id.burger_menu), bikeId)
        findViewById<Button>(R.id.bikepark_setup).setOnClickListener {
            navigateToMaketSetup(bikeId, "BikePark")
        }

        if (bikeId != -1) loadBikeData(bikeId)
    }

    private fun navigateToBikeGarage(bikeId: Int) {
        val intent = Intent(this, ActBikeGarage::class.java).apply {
            putExtra("bike_id", bikeId)
        }
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this, R.anim.fade_in_faster, R.anim.fade_out_faster
        )
        startActivity(intent, options.toBundle())
        finish()
    }

    private fun navigateToMaketSetup(bikeId: Int, checkingText: String) {
        val intent = Intent(this, MaketSetup::class.java).apply {
            putExtra("bike_id", bikeId)
            putExtra("BikePark", checkingText)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_faster, R.anim.fade_out_faster)
    }

    private fun loadBikeData(bikeId: Int) {
        lifecycleScope.launch {
            val bikeDao = BikeDatabase.getDatabase(this@ActSetups).bikeDao()
            bikeDao.getBikeById(bikeId)?.let { bike ->
                saveSelectedBikeId(bikeId)
                bikeNameTextView.text = getString(R.string.two_strings, bike.brand, bike.modelsJson.keys.first())
            }
        }
    }

    private fun saveSelectedBikeId(bikeId: Int) {
        getSharedPreferences("bike_prefs", MODE_PRIVATE).edit().apply {
            putInt("selected_bike_id", bikeId)
            apply()
        }
    }

    private fun setupAlertDialog(burgerMenuButton: Button, bikeId: Int) {
        val items = arrayOf("Додати сетап", "Видалити сетап")

        burgerMenuButton.setOnClickListener {
            AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
                .setTitle("Меню")
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> showNameInputDialog(bikeId)
                        1 -> activateDeleteMode(burgerMenuButton, bikeId)
                    }
                }
                .show()
        }
    }

    private fun showNameInputDialog(bikeId: Int) {
        val dialogView = layoutInflater.inflate(R.layout.di_textwriter, null)
        val inputText = dialogView.findViewById<EditText>(R.id.inputText)

        AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            .setView(dialogView)
            .create()
            .apply {
                dialogView.findViewById<Button>(R.id.okButton).setOnClickListener {
                    val setupName = inputText.text.toString().trim()
                    if (setupName.isNotEmpty()) {
                        addSetupButton(setupName, bikeId)
                        dismiss()
                    } else {
                        Toast.makeText(this@ActSetups, "Назва не може бути порожньою!", Toast.LENGTH_SHORT).show()
                    }
                }
                show()
            }
    }

    private fun addSetupButton(name: String, bikeId: Int, setupId: Int? = null) {
        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.setups).apply {
            val newButton = Button(this@ActSetups).apply {
                text = name
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.white, theme))
                setBackgroundResource(R.drawable.kt_textviews_basic)
                id = buttonIdCounter++
                layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (childCount > 0) {
                        topToBottom = getChildAt(childCount - 1).id
                    } else {
                        topToTop = id
                    }
                    startToStart = id
                    marginStart = 16
                    topMargin = 16
                }

                setOnClickListener {
                    if (isDeleteMode) deleteSetup(this, bikeId, name, setupId) else openSetup(name, bikeId, setupId)
                }
            }
            addView(newButton)
            if (setupId == null) saveSetupToDatabase(name, bikeId)
        }
    }

    private fun deleteSetup(button: Button, bikeId: Int, name: String, setupId: Int?) {
        lifecycleScope.launch {
            BikeDatabase.getDatabase(this@ActSetups).setupDao().deleteSetup(MarksForSetup(setupId ?: 0, bikeId, name))
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.setups).removeView(button)
        }
    }

    private fun openSetup(name: String, bikeId: Int, setupId: Int?) {
        val intent = Intent(this, MaketSetup::class.java).apply {
            putExtra("setup_name", name)
            putExtra("bike_id", bikeId)
            putExtra("setup_id", setupId)
        }
        startActivity(intent)
    }

    private fun saveSetupToDatabase(name: String, bikeId: Int) {
        lifecycleScope.launch {
            BikeDatabase.getDatabase(this@ActSetups).setupDao().insertSetup(MarksForSetup(0, bikeId, name))
        }
    }

    private fun activateDeleteMode(burgerMenuButton: Button, bikeId: Int) {
        isDeleteMode = true
        burgerMenuButton.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, theme))
        burgerMenuButton.setOnClickListener {
            isDeleteMode = false
            burgerMenuButton.setBackgroundResource(R.drawable.btn_burger)
            setupAlertDialog(burgerMenuButton, bikeId)
        }
    }
}
