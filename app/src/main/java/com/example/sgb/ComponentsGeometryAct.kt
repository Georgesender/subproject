package com.example.sgb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
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
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.Component
import com.example.sgb.room.ComponentsDao
import com.example.sub.R
import kotlinx.coroutines.launch

class ComponentsGeometryAct : AppCompatActivity() {
    private lateinit var bikeAndModelView: TextView
    private lateinit var bikeImageView: ImageView
    private lateinit var component1View: EditText
    private lateinit var component2View: EditText
    private lateinit var series1View: EditText
    private lateinit var series2View: EditText
    private lateinit var size1View: TextView

    @SuppressLint("DiscouragedApi", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compgeomrty)

        // Отримуємо bikeId із Intent
        val bikeId = intent.getIntExtra("bike_id", -1)
        val componentsDao = BikeDatabase.getDatabase(this).componentsDao()

        val backButton: Button = findViewById(R.id.back)
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)

        bikeAndModelView = findViewById(R.id.brand_model)
        bikeImageView = findViewById(R.id.bike_photo)
        component1View = findViewById(R.id.component1)
        component2View = findViewById(R.id.component2)
        series1View = findViewById(R.id.series1)
        series2View = findViewById(R.id.series2)
        size1View = findViewById(R.id.size1)

        backButton.setOnClickListener {
            if (selectedBikeId != -1) {
                val intent = Intent(this@ComponentsGeometryAct, BikeGarageAct::class.java)
                intent.putExtra("bike_id", selectedBikeId)
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.fade_in, R.anim.fade_out
                )
                startActivity(intent, options.toBundle())
                finish()
            }
        }

        if (bikeId != -1) {
            lifecycleScope.launch {
                val bikeDao = BikeDatabase.getDatabase(this@ComponentsGeometryAct).bikeDao()
                val bike = bikeDao.getBikeById(bikeId)
                val components = componentsDao.getComponentsByBikeId(bikeId)
                    ?: Component(bikeId = bikeId).also { componentsDao.insertComponent(it) }

                component1View.setText(components.component1)
                component2View.setText(components.component2)
                series1View.setText(components.series1)
                series2View.setText(components.series2)
                size1View.setOnClickListener{
                    setupSize1ViewDialog(bikeId, componentsDao)
                }
                size1View.text = getString(R.string.size_format, components.sSizeWidth, components.sSizeGoes)


                setEditTextListener(component1View, bikeId, "component1", componentsDao)
                setEditTextListener(component2View, bikeId, "component2", componentsDao)
                setEditTextListener(series1View, bikeId, "series1", componentsDao)
                setEditTextListener(series2View, bikeId, "series2", componentsDao)


                if (bike != null) {
                    bikeAndModelView.text = getString(R.string.bike_name, bike.brand, bike.modelsJson.keys.first())

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
    }

    private fun setEditTextListener(
        editText: EditText,
        bikeId: Int,
        field: String,
        componentsDao: ComponentsDao
    ) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    val components = componentsDao.getComponentsByBikeId(bikeId)
                    if (components != null) {
                        if (field == "component1") components.component1 = s.toString().take(15)
                        if (field == "component2") components.component2 = s.toString().take(15)
                        if (field == "series1") components.series1 = s.toString().take(15)
                        if (field == "series2") components.series2 = s.toString().take(15)
                        if (field == "widthEditText") components.sSizeWidth = s.toString().take(3)
                        if (field == "goesEditText") components.sSizeGoes = s.toString().take(2)
                        componentsDao.updateComponent(components)
                    }
                }
            }
        })
    }
    private fun setupSize1ViewDialog(bikeId: Int, componentsDao: ComponentsDao) {
        val dialogView = layoutInflater.inflate(R.layout.dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val widthEditText = dialogView.findViewById<EditText>(R.id.widthEditText)
        val goesEditText = dialogView.findViewById<EditText>(R.id.goesEditText)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        // Додати анімацію для введення
        widthEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Можна додати ефект перед зміною
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    val components = componentsDao.getComponentsByBikeId(bikeId)
                    if (components != null) {
                        components.sSizeWidth = s.toString().take(3)
                        componentsDao.updateComponent(components)
                        size1View.text = getString(R.string.size_format, components.sSizeWidth, components.sSizeGoes)
                    }
                }
            }
        })

        goesEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Можна додати ефект перед зміною
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Анімація при введенні
            }

            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    val components = componentsDao.getComponentsByBikeId(bikeId)
                    if (components != null) {
                        components.sSizeGoes = s.toString().take(2)
                        componentsDao.updateComponent(components)
                        size1View.text = getString(R.string.size_format, components.sSizeWidth, components.sSizeGoes)
                    }
                }
            }
        })

        okButton.setOnClickListener {
            dialog.dismiss() // Закриваємо діалог
        }

        dialog.show()
    }




}
