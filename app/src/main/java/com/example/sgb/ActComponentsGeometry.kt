package com.example.sgb

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.Bike
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.Component
import com.example.sub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.bumptech.glide.Glide
import java.util.UUID

class ActComponentsGeometry : AppCompatActivity() {

    private var currentDialogView: View? = null // Для доступу до діалогу
    private lateinit var bikeAndModelView: TextView
    private lateinit var bikeImageView: ImageView
    private var bikeId: Int = 0

    // Global variable to store available component types
    private lateinit var availableComponentTypes: MutableList<String>

    // Modify the imagePickerLauncher initialization
    private val REQUEST_CODE_READ_MEDIA_IMAGES = 1001
    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1002

    // Global variable to hold the selected image URI
    private var selectedImageUri: Uri? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_compgeomrty)

        bikeId = intent.getIntExtra("bike_id", -1)
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)

        // Initialize list of available component types from resources
        availableComponentTypes = resources.getStringArray(R.array.component_types).toMutableList()

        initViews()
        backButtonListener(selectedBikeId)



        // Load bike data if bikeId is valid
        if (bikeId != -1) {
            loadBikeData(bikeId)
        }

        val btnAddComponent: Button = findViewById(R.id.Add_component_info)

        btnAddComponent.setOnClickListener {
            // If there are no available component types, show a toast message
            if (availableComponentTypes.isEmpty()) {
                Toast.makeText(this, "All component types have been added", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dialogView = layoutInflater.inflate(R.layout.di_component_info, null).apply {
                currentDialogView = this
            }
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            // Use availableComponentTypes instead of resource array
            val spinner = dialogView.findViewById<Spinner>(R.id.compType)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableComponentTypes)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            // Initialize ImageButton for photo selection in the dialog
            dialogView.findViewById<ImageButton>(R.id.photoPlaceholder)?.setOnClickListener {
                selectedImageUri = null // Скидаємо попереднє фото
                pickImageForComponent()
            }

            dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
                val compBrand = dialogView.findViewById<EditText>(R.id.compbrand).text.toString()
                val compYearInput = dialogView.findViewById<EditText>(R.id.compyear).text.toString()
                val compModel = dialogView.findViewById<EditText>(R.id.compmodel).text.toString()
                val compSizeInput = dialogView.findViewById<EditText>(R.id.compsize).text.toString()
                val compWeightInput = dialogView.findViewById<EditText>(R.id.compweight).text.toString()
                val compNotesInput = dialogView.findViewById<EditText>(R.id.compnotes)?.text?.toString() ?: ""
                val selectedType = spinner.selectedItem.toString()




                val compYearCb = dialogView.findViewById<CheckBox>(R.id.compyear_cb)
                val compSizeCb = dialogView.findViewById<CheckBox>(R.id.compsize_cb)
                val compWeightCb = dialogView.findViewById<CheckBox>(R.id.compweight_cb)
                val compNotesCb = dialogView.findViewById<CheckBox>(R.id.compnotes_cb)

                val compYear = if (compYearCb.isChecked) compYearInput else ""
                val compSize = if (compSizeCb.isChecked) compSizeInput else ""
                val compWeight = if (compWeightCb.isChecked) compWeightInput else ""
                val compNotes = if (compNotesCb.isChecked) compNotesInput else null

                val currentBikeId = intent.getIntExtra("bike_id", -1)
                val photoUriString = selectedImageUri?.toString()

                if (!validateRequiredFields(compBrand, compModel)) return@setOnClickListener

                val newComponent = Component(
                    bikeId = currentBikeId,
                    compType = selectedType,
                    compBrand = compBrand,
                    compYear = compYear,
                    compModel = compModel,
                    compSize = compSize,
                    compWeight = compWeight,
                    compNotes = compNotes ?: "",
                    photoUri = photoUriString
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    // Перевірка унікальності фото ПЕРЕД вставкою
                    if (photoUriString != null) {
                        val isUnique = isPhotoUnique(photoUriString, currentBikeId)
                        if (!isUnique) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@ActComponentsGeometry,
                                    "Таке фото вже є в інших компонентах",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            return@launch // Виходимо, якщо фото не унікальне
                        }
                    }

                    // Вставка компонента тільки якщо фото унікальне
                    BikeDatabase.getDatabase(applicationContext).componentsDao().insertComponent(newComponent)

                    withContext(Dispatchers.Main) {
                        availableComponentTypes.remove(selectedType)
                        addComponentToUI(newComponent)
                        reloadComponents(currentBikeId)
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
                    // If a component of a specific type already exists in the database,
                    // remove that type from availableComponentTypes to avoid duplicate selection
                    availableComponentTypes.remove(component.compType)
                    addComponentToUI(component)
                }
            }
        }
    }
    // Перевірка обов'язкових полей
    private fun validateRequiredFields(brand: String, model: String): Boolean {
        return when {
            brand.isBlank() -> {
                Toast.makeText(this, "Введіть назву бренду", Toast.LENGTH_SHORT).show()
                false
            }
            model.isBlank() -> {
                Toast.makeText(this, "Введіть модель", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    // Перевірка унікальності фото
    private suspend fun isPhotoUnique(photoUri: String, bikeId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val existingPhotos = BikeDatabase.getDatabase(applicationContext)
                .componentsDao()
                .getPhotoUrisByBikeId(bikeId)
            !existingPhotos.contains(photoUri)
        }
    }


    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { originalUri ->
            val mimeType = contentResolver.getType(originalUri)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val processedUri = processImage(originalUri, mimeType)
                    withContext(Dispatchers.Main) {
                        selectedImageUri = processedUri
                        updatePhotoPreview(processedUri)
                        if (isExternalUri(processedUri)) {
                            takePersistablePermissions(processedUri)
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        handleImageError(originalUri)
                    }
                }
            }
        }
    }
    private fun processImage(uri: Uri, mimeType: String?): Uri {
        return if (mimeType?.startsWith("image/") == true) {
            contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                val processed = if (mimeType == "image/jpeg" || mimeType == "image/jpg") removeWhiteBackground(bitmap) else bitmap
                saveBitmapToInternalStorage(processed)
            } ?: uri
        } else {
            uri
        }
    }
    private fun handleImageError(uri: Uri) {
        Toast.makeText(this, "Помилка обробки фото", Toast.LENGTH_SHORT).show()
        selectedImageUri = uri
        updatePhotoPreview(uri)
        if (isExternalUri(uri)) {
            takePersistablePermissions(uri)
        }
    }
    private fun updatePhotoPreview(uri: Uri?) {
        currentDialogView?.findViewById<ImageButton>(R.id.photoPlaceholder)?.let { imageButton ->
            Glide.with(this)
                .load(uri)
                .override(500, 500)
                .placeholder(R.drawable.img_fork)
                .error(R.drawable.img_fork)
                .into(imageButton)
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
            // Use OpenDocument to obtain persistent access to images
            imagePickerLauncher.launch(arrayOf("image/*"))
        }
    }
    private fun takePersistablePermissions(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            Log.d("UriPermissions", "Persistent permission granted for: $uri")
        } catch (e: SecurityException) {
            e.printStackTrace()
            Log.e("UriPermissions", "Failed to get permissions for: $uri", e)
            Toast.makeText(this, "Failed to save permission for image access", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeWhiteBackground(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        for (x in 0 until result.width) {
            for (y in 0 until result.height) {
                val pixel = result[x , y]
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                if (red >= 240 && green >= 240 && blue >= 240) { // Adjust threshold as needed
                    result[x , y] = Color.TRANSPARENT
                }
            }
        }
        return result
    }

    // Оновлений метод для збереження фото
    private fun saveBitmapToInternalStorage(bitmap: Bitmap): Uri {
        val filename = "img_${UUID.randomUUID()}.png"
        val imagesDir = File(filesDir, "images").apply { mkdirs() }
        val file = File(imagesDir, filename).apply {
            FileOutputStream(this).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
    }    private fun isExternalUri(uri: Uri): Boolean {
        return when (uri.scheme) {
            "content" -> uri.authority?.startsWith("com.android.externalstorage") == true
            else -> false
        }
    }
    private fun isUriValid(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.close()
            true
        } catch (_: Exception) {
            false
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
    @SuppressLint("SetTextI18n")
    fun addComponentToUI(component: Component) {
        val gridContainer = findViewById<androidx.gridlayout.widget.GridLayout>(R.id.components_container_grid)
        var nextRowIndex = gridContainer.childCount

        val componentViews = mutableListOf<View>()

        // Визначаємо, чи є дані для size та weight, чи додано фото
        val hasSize = component.compSize.isNotEmpty()
        val hasWeight = component.compWeight.isNotEmpty()
        val hasPhoto = !component.photoUri.isNullOrEmpty()

        // Визначаємо, чи потрібно об'єднати рядки для size або weight
        val shouldExpandSize = !hasWeight && !hasPhoto && hasSize
        val shouldExpandWeight = !hasSize && !hasPhoto && hasWeight

        // Конвертер для dp в px (якщо ще не створено)


// Container for row that includes delete/edit buttons and component type text
        val rowContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            // Задаємо padding (конвертуємо dp у px)
            val padding = 8.toPx(this@ActComponentsGeometry)
            setPadding(padding, padding, padding, padding)
            // В середині LinearLayout власний gravity може залишатися CENTER_VERTICAL,
            // оскільки горизонтальне вирівнювання контролюватиметься параметрами дітей.
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.tb_white_border)

        }

// Delete button (25x25dp)
        val btnDelete = ImageButton(this).apply {
            setImageResource(R.drawable.img_delete) // Ресурс для іконки видалення
            layoutParams = LinearLayout.LayoutParams(25.toPx(this@ActComponentsGeometry), 25.toPx(this@ActComponentsGeometry))
            setBackgroundResource(0) // Видаляємо фон або застосовуємо кастомний стиль
        }



// TextView to display component type.
        val tvType = TextView(this).apply {
            text = component.compType
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            // Використовуємо параметри з вагою, щоб TextView займав доступний простір між кнопками
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, 48.toPx(this@ActComponentsGeometry), 1f)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setBackgroundResource(R.drawable.tb_hrlines)
        }
        val btnEdit = ImageButton(this).apply {
            setImageResource(R.drawable.img_edit) // Ресурс для іконки редагування
            layoutParams = LinearLayout.LayoutParams(25.toPx(this@ActComponentsGeometry), 25.toPx(this@ActComponentsGeometry))
            setBackgroundResource(0)
        }

        rowContainer.apply {
            addView(btnDelete)
            addView(tvType)
            addView(btnEdit)
        }

// Параметри рядка для GridLayout
        val rowParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
            rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex, 1)
            // Займаємо 2 колонки, щоб рядок розтягнувся на всю ширину
            columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
            width = MATCH_PARENT
            height = 48.toPx(this@ActComponentsGeometry)
            setGravity(Gravity.FILL_HORIZONTAL or Gravity.CENTER_VERTICAL)
        }

// Додаємо rowContainer до gridContainer
        gridContainer.addView(rowContainer, rowParams)
        componentViews.add(rowContainer)
        // Next rows for header, size, weight, photo, notes
        // 2nd row: Header (brand and model, with optional year)
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
            setBackgroundResource(R.drawable.tb_start_bott_end_withmargin)
            setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            gravity = Gravity.CENTER
        }
        val tvHeaderParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
            rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 1, 1)
            columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
            height = 48.toPx(this@ActComponentsGeometry)
            width = MATCH_PARENT
            setGravity(Gravity.CENTER)
        }
        gridContainer.addView(tvHeader, tvHeaderParams)
        componentViews.add(tvHeader)
        // 3rd row: Size
        if(hasSize) {
            val tvSize = TextView(this).apply {
                text = "Size: ${component.compSize}"
                textSize = 18f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry , R.color.white))
                setBackgroundResource(R.drawable.tb_start_bott_end_withmargin)
            }
            val tvSizeParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec =  androidx.gridlayout.widget.GridLayout.spec(
                    nextRowIndex + 2,
                    if (shouldExpandSize) 2 else 1
                )
                columnSpec =  androidx.gridlayout.widget.GridLayout.spec(0)
                height = if (shouldExpandSize) 100.toPx(this@ActComponentsGeometry) else 50.toPx(this@ActComponentsGeometry)
                width = if (shouldExpandSize) 0 else 150.toPx(this@ActComponentsGeometry)

            }
            gridContainer.addView(tvSize , tvSizeParams)
            componentViews.add(tvSize)
        }
        // 4th row: Weight
        if(hasWeight) {
            val tvWeight = TextView(this).apply {
                text = "Weight: ${component.compWeight}"
                textSize = 18f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry , R.color.white))
                setBackgroundResource(R.drawable.tb_start_bott_end_withmargin)
            }
            val tvWeightParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(
                    if (shouldExpandWeight) nextRowIndex + 2 else nextRowIndex + 3,
                    if (shouldExpandWeight) 2 else 1
                )
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0)
                height = if (shouldExpandWeight) 100.toPx(this@ActComponentsGeometry) else 50.toPx(this@ActComponentsGeometry)
                width = if (shouldExpandWeight) 0 else 150.toPx(this@ActComponentsGeometry)
            }
            gridContainer.addView(tvWeight , tvWeightParams)
            componentViews.add(tvWeight)
        }
        // Photo: occupies 3rd and 4th row in 2nd column
        if (hasPhoto) {
        val ivPhoto = ImageView(this).apply {
            ->
            scaleType = ImageView.ScaleType.FIT_CENTER
            try {
                val uri = component.photoUri.toUri()
                if (isUriValid(uri)) {
                    Glide.with(context)
                        .load(uri)
                        .into(this)
                }
            } catch (_: Exception) {
                setImageResource(R.drawable.img_fork)
            }
        }
        val ivPhotoParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
            rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 2, 2)
            columnSpec = androidx.gridlayout.widget.GridLayout.spec(1)
            width = 100.toPx(this@ActComponentsGeometry)
            height = 100.toPx(this@ActComponentsGeometry)
        }
        gridContainer.addView(ivPhoto, ivPhotoParams)
        componentViews.add(ivPhoto)
        }
        // 5th row: Notes (додавати ТІЛЬКИ якщо є нотатки)
        if (component.compNotes.isNotEmpty()) {
            val tvNotes = TextView(this).apply {
                text = component.compNotes
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
                setBackgroundResource(R.color.tr_white)
                setPadding(8, 4, 8, 4)
            }
            val tvNotesParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 4, 1)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
                height = 48.toPx(this@ActComponentsGeometry)
                width = MATCH_PARENT
            }
            gridContainer.addView(tvNotes, tvNotesParams)
            componentViews.add(tvNotes)
            nextRowIndex += 1 // Збільшуємо індекс тільки якщо додали рядок
        }

// Коректуємо загальний індекс рядків
        nextRowIndex += 4 // Базові 4 рядки (header, size, weight, photo)
        // Додаємо відступ 15dp між компонентами
        val marginView = View(this).apply {
            layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
                height = 25.toPx(this@ActComponentsGeometry)
                width = MATCH_PARENT
            }
        }
        gridContainer.addView(marginView)
        componentViews.add(marginView)
        nextRowIndex += 1  // Оновлюємо індекс для наступного компонента

        // --- Delete component event ---
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this component?")
                .setPositiveButton("Yes") { dialog, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        // Delete component from the database
                        BikeDatabase.getDatabase(applicationContext)
                            .componentsDao()
                            .deleteComponent(component)
                        withContext(Dispatchers.Main) {
                            // Remove all views associated with this component from the androidx.gridlayout.widget.GridLayout
                            componentViews.forEach { view ->
                                gridContainer.removeView(view)
                            }
                            // Optionally, add the component type back to available types
                            availableComponentTypes.add(component.compType)
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        // Edit component event
        btnEdit.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.di_component_info, null)
            val editDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            // Отримуємо посилання на всі поля та чекбокси
            val etBrand = dialogView.findViewById<EditText>(R.id.compbrand)
            val etModel = dialogView.findViewById<EditText>(R.id.compmodel)
            val etYear = dialogView.findViewById<EditText>(R.id.compyear)
            val etSize = dialogView.findViewById<EditText>(R.id.compsize)
            val etWeight = dialogView.findViewById<EditText>(R.id.compweight)
            val etNotes = dialogView.findViewById<EditText>(R.id.compnotes)



            val yearCb = dialogView.findViewById<CheckBox>(R.id.compyear_cb)
            val sizeCb = dialogView.findViewById<CheckBox>(R.id.compsize_cb)
            val weightCb = dialogView.findViewById<CheckBox>(R.id.compweight_cb)
            val notesCb = dialogView.findViewById<CheckBox>(R.id.compnotes_cb)

            // Додаємо TextWatcher для кожного поля
            fun setupAutoCheck(editText: EditText, checkBox: CheckBox) {
                editText.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        checkBox.isChecked = !s.isNullOrBlank()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }

            setupAutoCheck(etYear, yearCb)
            setupAutoCheck(etSize, sizeCb)
            setupAutoCheck(etWeight, weightCb)
            setupAutoCheck(etNotes, notesCb)

            // Заповнення даними
            etBrand.setText(component.compBrand)
            etYear.setText(component.compYear)
            etModel.setText(component.compModel)
            etSize.setText(component.compSize)
            etWeight.setText(component.compWeight)
            etNotes.setText(component.compNotes)
            // Automatically set checkbox states based on data
            yearCb.isChecked = component.compYear.isNotEmpty()
            sizeCb.isChecked = component.compSize.isNotEmpty()
            weightCb.isChecked = component.compWeight.isNotEmpty()
            notesCb.isChecked = component.compNotes.isNotEmpty()

            // Photo selection button in the dialog
            dialogView.findViewById<ImageButton>(R.id.photoPlaceholder)?.setOnClickListener {
                selectedImageUri = null // Скидаємо попереднє фото
                pickImageForComponent()
            }

            // Confirm edit button event
            dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
                // Read values from dialog for updating component
                val updatedCompBrand = etBrand.text.toString()
                val updatedCompYear = if (yearCb.isChecked)
                    etYear.text.toString() else ""
                val updatedCompModel = etModel.text.toString()
                val updatedCompSize = if (sizeCb.isChecked)
                    etSize.text.toString() else ""
                val updatedCompWeight = if (weightCb.isChecked)
                    etWeight.text.toString() else ""
                val updatedCompNotes = if (notesCb.isChecked)
                    etNotes.text.toString() else ""
                // If the photo was not changed, keep the old value
                val photoUriString = selectedImageUri?.toString() ?: component.photoUri

                val updatedComponent = component.copy(
                    compBrand = updatedCompBrand,
                    compYear = updatedCompYear,
                    compModel = updatedCompModel,
                    compSize = updatedCompSize,
                    compWeight = updatedCompWeight,
                    compNotes = updatedCompNotes,
                    photoUri = photoUriString
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    BikeDatabase.getDatabase(applicationContext).componentsDao().updateComponent(updatedComponent)
                    withContext(Dispatchers.Main) {
                        // Update UI fields with new data
                        val newHeaderText = SpannableStringBuilder("${updatedComponent.compBrand} ${updatedComponent.compModel}").apply {
                            if (updatedComponent.compYear.isNotEmpty()) {
                                append(" ${updatedComponent.compYear}")
                                val start = length - updatedComponent.compYear.length
                                setSpan(SuperscriptSpan(), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                setSpan(RelativeSizeSpan(0.7f), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                        }
                        tvHeader.text = newHeaderText
                        reloadComponents(bikeId)
                        editDialog.dismiss()
                    }
                }
            }
            editDialog.show()
        }
    }

    private fun reloadComponents(bikeId: Int) {
        // Отримуємо посилання на GridLayout та очищаємо його
        val gridContainer = findViewById<androidx.gridlayout.widget.GridLayout>(R.id.components_container_grid)
        gridContainer.removeAllViews()

        lifecycleScope.launch(Dispatchers.IO) {
            // Отримуємо список компонентів для конкретного bikeId
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
    // Extension function to convert dp to px
    fun Int.toPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
}