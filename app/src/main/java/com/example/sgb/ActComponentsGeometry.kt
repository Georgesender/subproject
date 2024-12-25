package com.example.sgb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.Bike
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.Component
import com.example.sgb.room.ComponentsDao
import com.example.sub.R
import kotlinx.coroutines.launch

class ActComponentsGeometry : AppCompatActivity() {

    private lateinit var bikeAndModelView: TextView
    private lateinit var bikeImageView: ImageView
    private lateinit var component1View: TextView
    private lateinit var component2View: TextView
    private lateinit var series1View: TextView
    private lateinit var series2View: TextView
    private lateinit var size1View: TextView
    private lateinit var size2View: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_compgeomrty)

        val bikeId = intent.getIntExtra("bike_id", -1)
        val componentsDao = BikeDatabase.getDatabase(this).componentsDao()
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)

        // ініціалізація елементів інтерфейсу
        initViews()

        backButtonListener(selectedBikeId)

        if (bikeId != -1) {
            loadBikeData(bikeId, componentsDao)
        }
    }

    private fun initViews() {
        bikeAndModelView = findViewById(R.id.brand_model)
        bikeImageView = findViewById(R.id.bike_photo)
        component1View = findViewById(R.id.component1)
        component2View = findViewById(R.id.component2)
        series1View = findViewById(R.id.series1)
        series2View = findViewById(R.id.series2)
        size1View = findViewById(R.id.size1)
        size2View = findViewById(R.id.size2)
    }

    private fun backButtonListener(selectedBikeId: Int) {
        val backButton: Button = findViewById(R.id.back)
        backButton.setOnClickListener {
            if (selectedBikeId != -1) {
                val intent = Intent(this@ActComponentsGeometry, ActBikeGarage::class.java).apply {
                    putExtra("bike_id", selectedBikeId)
                }
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.fade_in, R.anim.fade_out
                )
                startActivity(intent, options.toBundle())
                finish()
            }
        }
    }

    private fun loadBikeData(bikeId: Int, componentsDao: ComponentsDao) {
        lifecycleScope.launch {
            val bikeDao = BikeDatabase.getDatabase(this@ActComponentsGeometry).bikeDao()
            val bike = bikeDao.getBikeById(bikeId)
            val components = componentsDao.getComponentsByBikeId(bikeId)
                ?: Component(bikeId = bikeId).also { componentsDao.insertComponent(it) }

            // Встановлення початкових значень
            component1View.text = components.component1
            component2View.text = components.component2
            series1View.text = components.series1
            series2View.text = components.series2
            size2View.text = getString(R.string.size_with_mm, components.fSize2)


            // Налаштування слухачів кліків
            setViewDialogListener(component1View, bikeId, "component1", componentsDao)
            setViewDialogListener(component2View, bikeId, "component2", componentsDao)
            setViewDialogListener(series1View, bikeId, "series1", componentsDao)
            setViewDialogListener(series2View, bikeId, "series2", componentsDao)
            setViewDialogListener(size2View, bikeId, "size2", componentsDao)



            size1View.setOnClickListener { setupSize1ViewDialog(bikeId, componentsDao) }
            size1View.text =
                getString(R.string.size_format, components.sSizeWidth, components.sSizeGoes)



            bike?.let {
                bikeAndModelView.text =
                    getString(R.string.bike_name, it.brand, it.modelsJson.keys.first())
                loadBikeImage(it)
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun loadBikeImage(bike: Bike) {
        val imageName = bike.modelsJson.values.first().submodels.values.first().imageName
        imageName?.let {
            val resourceId = resources.getIdentifier(it, "drawable", packageName)
            if (resourceId != 0) {
                val drawable = ResourcesCompat.getDrawable(resources, resourceId, null)
                bikeImageView.setImageDrawable(drawable)
            }
        }
    }

    private fun setViewDialogListener(
        editText: TextView,
        bikeId: Int,
        field: String,
        componentsDao: ComponentsDao
    ) {
        editText.setOnClickListener {
            viewDialog { newValue ->
                // Оновлюємо значення одразу в TextView
                editText.text = newValue

                // Оновлюємо дані в базі після введення
                lifecycleScope.launch {
                    val components = componentsDao.getComponentsByBikeId(bikeId)
                    components?.let {
                        when (field) {
                            "component1" -> it.component1 = newValue
                            "component2" -> it.component2 = newValue
                            "series1" -> it.series1 = newValue
                            "series2" -> it.series2 = newValue
                            "size2" -> {
                                it.fSize2 = newValue
                                // Оновлюємо текст для size2 з додаванням " mm"
                                size2View.text = getString(R.string.size_with_mm, newValue)
                            }
                        }
                        componentsDao.updateComponent(it)
                    }
                }
            }
        }
    }



    private fun viewDialog(
        onValueSubmitted: (String) -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.di_textwriter, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialog.window?.setBackgroundDrawable(ColorDrawable(transparentColor))

        val inputText = dialogView.findViewById<EditText>(R.id.inputText)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Це дозволить одразу оновлювати TextView в реальному часі
                onValueSubmitted(s.toString())
            }
        })

        okButton.setOnClickListener {
            val newValue = inputText.text.toString().take(15) // Максимум 15 символів
            onValueSubmitted(newValue) // Передаємо значення назад
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupSize1ViewDialog(bikeId: Int, componentsDao: ComponentsDao) {
        val dialogView = layoutInflater.inflate(R.layout.di_shock_sizeformat, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialog.window?.setBackgroundDrawable(ColorDrawable(transparentColor))


        val widthEditText = dialogView.findViewById<EditText>(R.id.widthEditText)
        val goesEditText = dialogView.findViewById<EditText>(R.id.goesEditText)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        setupSizeEditTextListener(widthEditText, bikeId, componentsDao, "widthEditText")
        setupSizeEditTextListener(goesEditText, bikeId, componentsDao, "goesEditText")

        okButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupSizeEditTextListener(
        editText: EditText,
        bikeId: Int,
        componentsDao: ComponentsDao,
        field: String
    ) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    val components = componentsDao.getComponentsByBikeId(bikeId)
                    components?.let {
                        if (field == "widthEditText") it.sSizeWidth = s.toString().take(3)
                        if (field == "goesEditText") it.sSizeGoes = s.toString().take(2)
                        componentsDao.updateComponent(it)
                        size1View.text =
                            getString(R.string.size_format, it.sSizeWidth, it.sSizeGoes)
                    }
                }
            }
        })
    }
}
