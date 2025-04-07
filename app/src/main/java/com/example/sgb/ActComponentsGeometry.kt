package com.example.sgb

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.Bike
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.Component
import com.example.sub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class ActComponentsGeometry : AppCompatActivity() {

    private lateinit var bikeAndModelView: TextView
    private lateinit var bikeImageView: ImageView
    // Використовуємо OpenDocument, тому параметр – Array<String>
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Array<String>>
    private val REQUEST_CODE_READ_MEDIA_IMAGES = 1001
    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1002

    // Глобальна змінна для збереження вибраного URI
    private var selectedImageUri: Uri? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_compgeomrty)

        val bikeId = intent.getIntExtra("bike_id", -1)
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)

        initViews()
        backButtonListener(selectedBikeId)

        // Ініціалізація imagePickerLauncher з використанням OpenDocument
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                // Встановлюємо фіксовані флаги для збереження дозволів
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Не вдалося зберегти дозвіл на доступ до зображення", Toast.LENGTH_SHORT).show()
                }
                selectedImageUri = uri
            }
        }

        if (bikeId != -1) {
            loadBikeData(bikeId)
        }
        val btnAddComponent: Button = findViewById(R.id.Add_component_info)

        fun Int.toPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

        fun addComponentToUI(component: Component) {
            val gridContainer = findViewById<GridLayout>(R.id.components_container_grid)
            var nextRowIndex = gridContainer.childCount

            // 1-й рядок: Тип компонента
            val tvType = TextView(this).apply {
                text = component.compType
                textSize = 25f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
                background = ContextCompat.getDrawable(this@ActComponentsGeometry, R.drawable.tb_end_start)
                setPadding(24, 8, 8, 8)
            }
            val tvTypeParams = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(nextRowIndex, 1)
                columnSpec = GridLayout.spec(0, 2)
                width = 300.toPx(this@ActComponentsGeometry)
                height = 44.toPx(this@ActComponentsGeometry)
                setGravity(Gravity.CENTER)
            }
            gridContainer.addView(tvType, tvTypeParams)

            // 2-й рядок: Заголовок
            val brandModel = "${component.compBrand} ${component.compModel}"
            val spannable = SpannableStringBuilder(brandModel)
            if (component.compYear.isNotEmpty()) {
                spannable.append(" ${component.compYear}")
                val start = spannable.length - component.compYear.length
                spannable.setSpan(SuperscriptSpan(), start, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(RelativeSizeSpan(0.7f), start, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            val tvHeader = TextView(this).apply {
                text = spannable
                textSize = 24f
                background = ContextCompat.getDrawable(this@ActComponentsGeometry, R.drawable.tb_start_bott_end_tr25)
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
                gravity = Gravity.CENTER
                setPadding(0, 12.toPx(this@ActComponentsGeometry), 0, 0)
            }

            val tvHeaderParams = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(nextRowIndex + 1, 1)
                columnSpec = GridLayout.spec(0, 2)
                height = 42.toPx(this@ActComponentsGeometry)
                width = 200.toPx(this@ActComponentsGeometry)
                setGravity(Gravity.CENTER)
            }
            gridContainer.addView(tvHeader, tvHeaderParams)




            // 3-й рядок: Розмір
            val tvSize = TextView(this).apply {
                text = if (component.compSize.isNotEmpty()) "Size: ${component.compSize}" else ""
                textSize = 18f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            }
            val tvSizeParams = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(nextRowIndex + 2, 1)
                columnSpec = GridLayout.spec(0)
                height = 48.toPx(this@ActComponentsGeometry)
            }
            gridContainer.addView(tvSize, tvSizeParams)

            // 4-й рядок: Вага
            val tvWeight = TextView(this).apply {
                text = if (component.compWeight.isNotEmpty()) "Weight: ${component.compWeight}" else ""
                textSize = 18f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            }
            val tvWeightParams = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(nextRowIndex + 3, 1)
                columnSpec = GridLayout.spec(0)
                height = 48.toPx(this@ActComponentsGeometry)
            }
            gridContainer.addView(tvWeight, tvWeightParams)
            // Фото: у другій колонці, займає третій та четвертий рядок
            val ivPhoto = ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    android.util.Log.d("ActComponentsGeometry", "ImageView clicked for component: ${component.compType}")
                    pickImageForComponent()
                }
                if (!component.photoUri.isNullOrEmpty()) {
                    setImageURI(component.photoUri.toUri())
                } else {
                    setImageResource(R.drawable.img_fork) // замініть на свій ресурс за потребою
                }
            }
            val ivPhotoParams = GridLayout.LayoutParams().apply {
                // Розміщуємо фото, починаючи з третього рядка (індекс 2) і займаючи 2 рядки (тобто третій та четвертий)
                rowSpec = GridLayout.spec(nextRowIndex + 2, 2)
                // Розміщення у другій колонці (індекс 1)
                columnSpec = GridLayout.spec(1)
                width = 100.toPx(this@ActComponentsGeometry)
                height = 100.toPx(this@ActComponentsGeometry)
                setMargins(
                    8.toPx(this@ActComponentsGeometry),
                    8.toPx(this@ActComponentsGeometry),
                    8.toPx(this@ActComponentsGeometry),
                    8.toPx(this@ActComponentsGeometry)
                )
            }
            gridContainer.addView(ivPhoto, ivPhotoParams)

            // 5-й рядок: Нотатки (якщо є)
            if (component.compNotes.isNotEmpty()) {
                val tvNotes = TextView(this).apply {
                    text = component.compNotes
                    textSize = 16f
                    gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
                    setPadding(8, 4, 8, 4)
                }
                val tvNotesParams = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(nextRowIndex + 4, 1)
                    columnSpec = GridLayout.spec(0, 2)
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setGravity(Gravity.CENTER)
                }
                gridContainer.addView(tvNotes, tvNotesParams)
                nextRowIndex += 5
            } else {
                nextRowIndex += 4
            }

        }

        btnAddComponent.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.di_component_info, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            val spinner = dialogView.findViewById<Spinner>(R.id.compType)
            val types = resources.getStringArray(R.array.component_types)
            // Ініціалізація ImageButton в діалоговому вікні
            val dialogPhotoPlaceholder = dialogView.findViewById<ImageButton>(R.id.photoPlaceholder)
            dialogPhotoPlaceholder?.setOnClickListener {
                pickImageForComponent()
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
                val compBrand = dialogView.findViewById<EditText>(R.id.compbrand).text.toString()
                val compYearInput = dialogView.findViewById<EditText>(R.id.compyear).text.toString()
                val compModel = dialogView.findViewById<EditText>(R.id.compmodel).text.toString()
                val compSizeInput = dialogView.findViewById<EditText>(R.id.compsize).text.toString()
                val compWeightInput = dialogView.findViewById<EditText>(R.id.compweight).text.toString()
                val compNotesInput = dialogView.findViewById<EditText>(R.id.compnotes)?.text?.toString() ?: ""
                val selectedType = spinner.selectedItem.toString()

                val compYearCb = dialogView.findViewById<android.widget.CheckBox>(R.id.compyear_cb)
                val compSizeCb = dialogView.findViewById<android.widget.CheckBox>(R.id.compsize_cb)
                val compWeightCb = dialogView.findViewById<android.widget.CheckBox>(R.id.compweight_cb)
                val compNotesCb = dialogView.findViewById<android.widget.CheckBox>(R.id.compnotes_cb)

                val compYear = if (compYearCb.isChecked) compYearInput else ""
                val compSize = if (compSizeCb.isChecked) compSizeInput else ""
                val compWeight = if (compWeightCb.isChecked) compWeightInput else ""
                val compNotes = if (compNotesCb.isChecked) compNotesInput else ""

                val currentBikeId = intent.getIntExtra("bike_id", -1)
                val photoUriString = selectedImageUri?.toString()

                val newComponent = Component(
                    bikeId = currentBikeId,
                    compType = selectedType,
                    compBrand = compBrand,
                    compYear = compYear,
                    compModel = compModel,
                    compSize = compSize,
                    compWeight = compWeight,
                    compNotes = compNotes,
                    photoUri = photoUriString
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    BikeDatabase.getDatabase(applicationContext).componentsDao().insertComponent(newComponent)
                    withContext(Dispatchers.Main) {
                        addComponentToUI(newComponent)
                        dialog.dismiss()
                    }
                }
            }
            dialog.show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val componentsList = BikeDatabase.getDatabase(applicationContext)
                .componentsDao()
                .getComponentsByBikeId(bikeId)
            withContext(Dispatchers.Main) {
                componentsList.forEach { component ->
                    addComponentToUI(component)
                }
            }
        }
    }

    private fun initViews() {
        bikeAndModelView = findViewById(R.id.brand_model)
        bikeImageView = findViewById(R.id.bike_photo)
    }

    private fun backButtonListener(selectedBikeId: Int) {
        val backButton: Button = findViewById(R.id.back)
        backButton.setOnClickListener {
            if (selectedBikeId != -1) {
                val intent = Intent(this@ActComponentsGeometry, ActBikeGarage::class.java).apply {
                    putExtra("bike_id", selectedBikeId)
                }
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.fade_in_faster, R.anim.fade_out_faster
                )
                startActivity(intent, options.toBundle())
                finish()
            }
        }
    }

    private fun loadBikeData(bikeId: Int) {
        lifecycleScope.launch {
            val bikeDao = BikeDatabase.getDatabase(this@ActComponentsGeometry).bikeDao()
            val bike = bikeDao.getBikeById(bikeId)
            bike?.let {
                bikeAndModelView.text = getString(R.string.two_strings, it.brand, it.modelsJson.keys.first())
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

    private fun pickImageForComponent() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    REQUEST_CODE_READ_MEDIA_IMAGES else REQUEST_CODE_READ_EXTERNAL_STORAGE
            )
        } else {
            // Використовуємо OpenDocument для отримання постійного доступу
            imagePickerLauncher.launch(arrayOf("image/*"))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch(arrayOf("image/*"))
        } else {
            Toast.makeText(this, "Permission required to access images", Toast.LENGTH_SHORT).show()
        }
    }
}
