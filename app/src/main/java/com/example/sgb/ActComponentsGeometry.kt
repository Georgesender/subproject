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
    private lateinit var component3View: TextView
    private lateinit var component4View: TextView
    private lateinit var series1View: TextView
    private lateinit var series2View: TextView
    private lateinit var series3View: TextView
    private lateinit var series4View: TextView
    private lateinit var size1View: TextView
    private lateinit var size2View: TextView
    private lateinit var size3View: TextView
    private lateinit var size4View: TextView

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
        component3View = findViewById(R.id.component3)
        component4View = findViewById(R.id.component4)
        series1View = findViewById(R.id.series1)
        series2View = findViewById(R.id.series2)
        series3View = findViewById(R.id.series3)
        series4View = findViewById(R.id.series4)
        size1View = findViewById(R.id.size1)
        size2View = findViewById(R.id.size2)
        size3View = findViewById(R.id.size3)
        size4View = findViewById(R.id.size4)
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
            component1View.text = components.shockBrand
            component2View.text = components.forkBrand
            component3View.text = components.frontTyreBrand
            component4View.text = components.rearTyreBrand
            series1View.text = components.shockSeries
            series2View.text = components.forkSeries
            series3View.text = components.frontTyreSeries
            series4View.text = components.rearTyreSeries
            size2View.text = getString(R.string.size_with_mm, components.fSize2)
            size3View.text = getString(R.string.size_with_mm, components.frontTyreSize)
            size4View.text = getString(R.string.size_with_mm, components.rearTyreSize)

            // Налаштування слухачів кліків
            setViewDialogListener(component1View, bikeId, "componentBrand1", componentsDao, isComponentBField = true)
            setViewDialogListener(component2View, bikeId, "componentBrand2", componentsDao, isComponentBField = true)
            setViewDialogListener(component3View, bikeId, "componentBrand3", componentsDao, isComponentBField = true)
            setViewDialogListener(component4View, bikeId, "componentBrand4", componentsDao, isComponentBField = true)
            setViewDialogListener(series1View, bikeId, "series1", componentsDao)
            setViewDialogListener(series2View, bikeId, "series2", componentsDao)
            setViewDialogListener(series3View, bikeId, "series3", componentsDao)
            setViewDialogListener(series4View, bikeId, "series4", componentsDao)
            setViewDialogListener(size2View, bikeId, "size2", componentsDao, true)
            setViewDialogListener(size3View, bikeId, "size3", componentsDao, true)
            setViewDialogListener(size4View, bikeId, "size4", componentsDao, true)



            size1View.setOnClickListener { setupSize1ViewDialog(bikeId, componentsDao) }
            size1View.text =
                getString(R.string.size_format, components.sSizeWidth, components.sSizeGoes)



            bike?.let {
                bikeAndModelView.text =
                    getString(R.string.two_strings, it.brand, it.modelsJson.keys.first())
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
        componentsDao: ComponentsDao,
        isSizeField: Boolean = false,
        isComponentBField: Boolean = false
    ) {
        editText.setOnClickListener {
            viewDialog({ newValue ->
                // Оновлюємо значення одразу в TextView
                editText.text = if (isComponentBField) {
                    newValue
                } else {
                    newValue
                }

                // Оновлюємо дані в базі після введення
                lifecycleScope.launch {
                    val components = componentsDao.getComponentsByBikeId(bikeId)
                    components?.let {
                        when (field) {
                            "componentBrand1" -> it.shockBrand = newValue
                            "componentBrand2" -> it.forkBrand = newValue
                            "componentBrand3" -> it.frontTyreBrand = newValue
                            "componentBrand4" -> it.rearTyreBrand = newValue
                            "series1" -> it.shockSeries = newValue
                            "series2" -> it.forkSeries = newValue
                            "series3" -> it.frontTyreSeries = newValue
                            "series4" -> it.rearTyreSeries = newValue
                            "size2" -> {
                                it.fSize2 = newValue
                                size2View.text = getString(R.string.size_with_mm, newValue)
                            }
                            "size3" -> {
                                it.frontTyreSize = newValue
                                size3View.text = getString(R.string.size_with_mm, newValue)
                            }
                            "size4" -> {
                                it.rearTyreSize = newValue
                                size4View.text = getString(R.string.size_with_mm, newValue)
                            }
                        }
                        componentsDao.updateComponent(it)
                    }
                }
            }, isSizeField, isComponentBField)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun viewDialog(
        onValueSubmitted: (String) -> Unit,
        isSizeField: Boolean = false,
        isComponentBField: Boolean = false
    ) {
        val dialogView = layoutInflater.inflate(R.layout.di_textwriter, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        val transparentColor = resources.getColor(R.color.transparent, theme)
        dialog.window?.setBackgroundDrawable(ColorDrawable(transparentColor))

        val inputText = dialogView.findViewById<EditText>(R.id.inputText)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)
        val textPasted = dialogView.findViewById<TextView>(R.id.text_pasted)

        // Відображаємо відповідний текст залежно від поля
        textPasted.text = when {
            isSizeField -> "mm"
            isComponentBField -> "brand"
            else -> ""
        }

        inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onValueSubmitted(s.toString())
            }
        })

        okButton.setOnClickListener {
            val newValue = inputText.text.toString().take(15) // Максимум 15 символів
            onValueSubmitted(newValue)
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
