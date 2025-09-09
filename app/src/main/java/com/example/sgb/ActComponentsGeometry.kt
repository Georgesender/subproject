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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
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
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.Component
import com.example.sub.R
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.example.sgb.room.Bike

class ActComponentsGeometry : AppCompatActivity() {

    private var currentDialogView: View? = null // Для доступу до діалогу
    private lateinit var bikeAndModelView: TextView
    private lateinit var bikeImageView: ImageView
    private var bikeId: Int = 0
    private var selectedColor: Int = Color.WHITE // Початковий колір для видалення
    private var backgroundRemovalDialog: AlertDialog? = null
    // Global variable to store available component types
    private lateinit var availableComponentTypes: MutableList<String>

    // Modify the imagePickerLauncher initialization
    private val REQUEST_CODE_READ_MEDIA_IMAGES = 1001
    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1002
    // Global variable to hold the selected image URI
    private var selectedImageUri: Uri? = null
    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                // Визначаємо MIME-тип
                val mimeType = contentResolver.getType(resultUri) ?: run {
                    when (resultUri.path?.substringAfterLast(".")?.lowercase()) {
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        else -> null
                    }
                }
                Log.d("CropImage", "MIME type: $mimeType, URI: $resultUri")

                // Завжди оновлюємо прев'ю і беремо дозволи
                selectedImageUri = resultUri
                updatePhotoPreview(resultUri)
                if (isExternalUri(resultUri)) {
                    takePersistablePermissions(resultUri)
                }

                // Показуємо діалог з вибором дії
                showConfirmationDialog(resultUri)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            cropError?.printStackTrace()
            Toast.makeText(this, "Помилка обрізання: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

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
            val transparentColor = resources.getColor(R.color.transparent, theme)
            dialog.window?.setBackgroundDrawable(transparentColor.toDrawable())
            val adaptiveEditText = dialogView.findViewById<EditText>(R.id.compAdaptive)
            val compWeightCb = dialogView.findViewById<CheckBox>(R.id.compweight_cb)
            val etWeight = dialogView.findViewById<EditText>(R.id.compweight)
            val extraBrandLabel = dialogView.findViewById<TextView>(R.id.labelBrandExtra)
            val extraBrand = dialogView.findViewById<EditText>(R.id.compBrandExtra)
            // 1. Оптимізація створення списку доступних типів
            val editAvailableTypes = (availableComponentTypes)
                .toMutableSet() // Використовуємо Set для автоматичного видалення дублікатів
                .toMutableList()
// 2. Спрощена ініціалізація спінера з використанням apply
            val spinner = dialogView.findViewById<Spinner>(R.id.compType).apply {
                adapter = ArrayAdapter(
                    this@ActComponentsGeometry,
                    android.R.layout.simple_spinner_item,
                    editAvailableTypes
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            }
            // 3. Отримання посилань на UI-елементи
            val views = mapOf(
                "adaptive" to dialogView.findViewById(R.id.labelAdaptive),
                "model" to dialogView.findViewById(R.id.labelModel),
                "adaptiveEditText" to adaptiveEditText,
                "forkSize" to dialogView.findViewById<TextView>(R.id.labelForkSize)
            )

// Обробник зміни вибраного типу
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedType = parent?.getItemAtPosition(position) as? String ?: ""

                    // Мапа з конфігурацією для кожного типу
                    val typeConfig = mapOf(
                        "Cranks" to mapOf(
                            "adaptive" to "Підмодель",
                            "forkSize" to "Довжина"
                        ),
                        "Handlebar" to mapOf(
                            "adaptive" to "Матеріал",
                            "forkSize" to "Довжина/ширина"
                        ),
                        "Rim" to mapOf(
                            "adaptive" to "Підмодель",
                            "forkSize" to "Розмір"
                        ),
                        "Fork" to mapOf(
                            "adaptive" to "Картридж",
                            "forkSize" to "Хід"
                        ),
                        "Shock" to mapOf(
                            "adaptive" to "Картридж",
                            "forkSize" to "Хід"
                        ),
                        "Tyre" to mapOf(
                            "model" to "Переднє",
                            "adaptive" to "Заднє",
                            "forkSize" to "Розмір"
                        )
                    )

                    // Оновлення значень за замовчуванням
                    views["adaptive"]?.text = ""
                    views["model"]?.text = "Модель"
                    views["forkSize"]?.text = "Розмір"

                    // Застосування конфігурації для обраного типу
                    typeConfig[selectedType]?.forEach { (key , value) ->
                        views[key]?.text = value
                    }
                    if (selectedType == "Saddle") {
                        adaptiveEditText.visibility = View.GONE
                        dialogView.findViewById<TextView>(R.id.labelWeight).visibility = View.GONE
                        etWeight.visibility = View.GONE
                        compWeightCb.visibility = View.GONE
                    }
                    if (selectedType == "Tyre"){
                        extraBrand.visibility = View.VISIBLE
                        extraBrandLabel.visibility = View.VISIBLE
                    } else {
                        extraBrand.visibility = View.GONE
                        extraBrandLabel.visibility = View.GONE
                    }

            }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

// Ініціалізуємо стан при першому відкритті
            spinner.setSelection(0, false)
            spinner.onItemSelectedListener?.onItemSelected(null, null, 0, 0)

            // Initialize ImageButton for photo selection in the dialog
            dialogView.findViewById<ImageButton>(R.id.photoPlaceholder)?.setOnClickListener {
                selectedImageUri = null // Скидаємо попереднє фото
                pickImageForComponent()

            }
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


            // Отримуємо посилання на EditText'и та CheckBox'и
            val etYear = dialogView.findViewById<EditText>(R.id.compyear)
            val etSize = dialogView.findViewById<EditText>(R.id.compsize)
            val etNotes = dialogView.findViewById<EditText>(R.id.compnotes)
            val compYearCb = dialogView.findViewById<CheckBox>(R.id.compyear_cb)
            val compSizeCb = dialogView.findViewById<CheckBox>(R.id.compsize_cb)

            val compNotesCb = dialogView.findViewById<CheckBox>(R.id.compnotes_cb)


            setupAutoCheck(etYear, compYearCb)
            setupAutoCheck(etSize, compSizeCb)
            setupAutoCheck(etWeight, compWeightCb)
            setupAutoCheck(etNotes, compNotesCb)

            dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
                val compBrand = dialogView.findViewById<EditText>(R.id.compbrand).text.toString()
                etYear.text.toString()
                val compModel = dialogView.findViewById<EditText>(R.id.compmodel).text.toString()
                etSize.text.toString()
                val compAdaptive = adaptiveEditText.text.toString()
               etWeight.text.toString()
                etNotes?.text?.toString() ?: ""
                val selectedType = spinner.selectedItem.toString()
                val compExtraBrand = extraBrand.text.toString()



                // Правильний варіант
                val compYear = if (compYearCb.isChecked) etYear.text.toString() else ""
                val compSize = if (compSizeCb.isChecked) etSize.text.toString() else ""
                val compWeight = if (compWeightCb.isChecked) etWeight.text.toString() else ""
                val compNotes = if (compNotesCb.isChecked) etNotes.text.toString() else ""

                val currentBikeId = intent.getIntExtra("bike_id", -1)
                val photoUriString = selectedImageUri?.toString()

                if (!validateRequiredFields(compBrand, compModel)) return@setOnClickListener

                val newComponent = Component(
                    bikeId = currentBikeId,
                    compType = selectedType,
                    compBrand = compBrand,
                    compYear = compYear ,
                    compModel = compModel,
                    compAdaptive = compAdaptive,
                    compSize = compSize,
                    compWeight = compWeight,
                    compNotes = compNotes ,
                    compBrandExtra = compExtraBrand,
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
            startCrop(originalUri)
        }
    }
    private fun startCrop(uri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.png"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))

        val options = UCrop.Options().apply {
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)
            setStatusBarColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.black))
            setToolbarColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            setToolbarTitle("Обріжте фото")
            setCompressionFormat(Bitmap.CompressFormat.PNG)
            setCompressionQuality(100)
        }

        // Створюємо об'єкт uCrop із заданими параметрами та максимальним розміром результату
        val uCrop = UCrop.of(uri, destinationUri)
            .withOptions(options)
            .withMaxResultSize(2000, 2000)

        // Отримуємо Intent для uCrop і запускаємо його через cropImageLauncher
        val uCropIntent = uCrop.getIntent(this)
        cropImageLauncher.launch(uCropIntent)
    }


    private fun updatePhotoPreview(uri: Uri?) {
        currentDialogView?.findViewById<ImageButton>(R.id.photoPlaceholder)?.let { imageButton ->
            Glide.with(this)
                .load(uri)
                .override(500, 500)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Вимкнути кеш
                .skipMemoryCache(true)
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


    private fun loadBikeImage(bike: Bike) {
        // 1) Якщо є URI із бази — показуємо його
        val uriString = bike.addedImgBikeUri
        if (!uriString.isNullOrEmpty()) {
            Glide.with(bikeImageView.context)
                .load(uriString.toUri())
                .override(200, 200)
                .centerCrop()
                .placeholder(R.drawable.img_fork)
                .error(R.drawable.img_fork)
                .into(bikeImageView)

            // 2) Інакше, якщо є imageName — шукаємо drawable і встановлюємо
        } else {
            if(bike.addedImgBikeUri == null) {
                val imageRes = bike.modelsJson
                    .values.first()
                    .submodels
                    .values.first()
                    .imageRes
                if (imageRes != null) {
                    bikeImageView.setImageResource(imageRes)
                }
            } else {
                Toast.makeText(this@ActComponentsGeometry, "Troble with bike photo(dev)",  Toast.LENGTH_SHORT).show()
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

    // Оновлений метод для збереження фото
    private fun saveBitmapToInternalStorage(bitmap: Bitmap): Uri {
        val filename = "img_${UUID.randomUUID()}.png"
        val imagesDir = File(filesDir, "images").apply { mkdirs() }
        val file = File(imagesDir, filename).apply {
            FileOutputStream(this).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) // Формат PNG
            }
        }
        Log.d("ImageDebug", "Saved image path: ${file.absolutePath}")
        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
    }
    private fun isExternalUri(uri: Uri): Boolean {
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
        // Припустимо, що components_container_grid тепер має тип LinearLayout
        val parentContainer = findViewById<LinearLayout>(R.id.components_container_grid)
        parentContainer.orientation = LinearLayout.VERTICAL

// Створюємо новий GridLayout, що буде обгорткою для шести елементів
        val gridWrapper = androidx.gridlayout.widget.GridLayout(this).apply {
            // Наприклад, задаємо необхідну кількість стовпців (якщо потрібно):
            columnCount = 2
            // Якщо потрібна динамічна кількість рядків – вона буде визначатися автоматично
            layoutParams = LinearLayout.LayoutParams(
                MATCH_PARENT ,
                WRAP_CONTENT

            )

            // За бажанням можна задати інші властивості (відступи, фон тощо)
            setPadding(8.toPx(this@ActComponentsGeometry), 8.toPx(this@ActComponentsGeometry),
                8.toPx(this@ActComponentsGeometry), 8.toPx(this@ActComponentsGeometry))
        }

// Приклад існуючого коду, що створює компоненти (рядок, заголовок, розміри тощо)
// (Ми залишаємо ваш код, але замість додавання до gridContainer, будемо додавати до gridWrapper)

// Отримуємо індекс для наступного рядка (як було у вашому коді)
        var nextRowIndex = 0 // Можна починати з 0, або відновити лічильник, якщо потрібно
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
            val padding = 8.toPx(this@ActComponentsGeometry)
            setPadding(padding, padding, padding, padding)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.tb_white_border)
        }

        val btnDelete = ImageButton(this).apply {
            setImageResource(R.drawable.img_delete)
            layoutParams = LinearLayout.LayoutParams(25.toPx(this@ActComponentsGeometry), 25.toPx(this@ActComponentsGeometry))
            setBackgroundResource(0)
        }

        val tvType = TextView(this).apply {
            text = component.compType
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT , 48.toPx(this@ActComponentsGeometry), 1f)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setBackgroundResource(R.drawable.tb_hrlines)
        }

        val btnEdit = ImageButton(this).apply {
            setImageResource(R.drawable.img_edit)
            layoutParams = LinearLayout.LayoutParams(25.toPx(this@ActComponentsGeometry), 25.toPx(this@ActComponentsGeometry))
            setBackgroundResource(0)
        }

        rowContainer.apply {
            addView(btnDelete)
            addView(tvType)
            addView(btnEdit)
        }

// Для GridLayout використаємо GridLayout.LayoutParams із зазначенням рядку та колонки
        val rowParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
            // Рядок із кнопками займає перший рядок (row = nextRowIndex)
            rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex, 1)
            // Якщо потрібно, щоб елемент простягався на 2 стовпці:
            columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
            width = MATCH_PARENT
            height = 48.toPx(this@ActComponentsGeometry)
            setGravity(Gravity.FILL_HORIZONTAL or Gravity.CENTER_VERTICAL)
        }

// Додаємо рядок до gridWrapper
        gridWrapper.addView(rowContainer, rowParams)
        componentViews.add(rowContainer)

// ======= 2. Заголовок з двома TextView (ліва частина з брендом, моделлю, роком та права – значення) =======
        val leftText = if (component.compBrandExtra.isNotEmpty()){
            SpannableStringBuilder(component.compModel)
        } else {
            SpannableStringBuilder("${component.compBrand} ${component.compModel}")
        }

        if (component.compYear.isNotEmpty() || component.compBrandExtra.isEmpty()) {
            leftText.append(" ${component.compYear}")
            val start = leftText.length - component.compYear.length
            leftText.setSpan(SuperscriptSpan(), start, leftText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            leftText.setSpan(RelativeSizeSpan(0.7f), start, leftText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

// Створюємо tvLeft з правильними LinearLayout.LayoutParams
        val tvLeft = TextView(this).apply {
            text = leftText
            textSize = 25f
            setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            // Присвоюємо параметри: ширина 0 та вага 1, щоб займати вільний простір
            layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT , 1f).apply {
                setPadding(10.toPx(this@ActComponentsGeometry) , 0, 0 , 0)
            }
        }

// Створюємо tvRight з правильними параметрами (якщо потрібно вирівнювати текст справа)
        val tvRight = TextView(this).apply {
            text = component.compAdaptive
            textSize = 25f
            setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT , 1f).apply {
                setPadding(0 , 0, 10.toPx(this@ActComponentsGeometry) , 0)
            }
        }

// Контейнер headerContainer як LinearLayout
        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            // Тут встановимо layout params для GridLayout
            layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec( 1, 1)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
                width = MATCH_PARENT
                height = 48.toPx(this@ActComponentsGeometry)
                setGravity(Gravity.CENTER)
            }
            setBackgroundResource(R.drawable.tb_start_bott_end_withmargin_lightwt)
        }

        headerContainer.addView(tvLeft, LinearLayout.LayoutParams(0, MATCH_PARENT , 1f))
        headerContainer.addView(tvRight, LinearLayout.LayoutParams(WRAP_CONTENT , MATCH_PARENT))
        gridWrapper.addView(headerContainer)
        componentViews.add(headerContainer)

// ======= 3. Рядок з розмірами, якщо hasSize =======
        if (hasSize) {
            val tvSize = TextView(this).apply {
                text = "> ${component.compSize}."
                textSize = 24f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
                setBackgroundResource(R.drawable.tb_start_bott_end_withmargin_lightwt)
            }
            val tvSizeParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec =  androidx.gridlayout.widget.GridLayout.spec(
                     2,
                    if (shouldExpandSize) 2 else 1
                )
                columnSpec =  androidx.gridlayout.widget.GridLayout.spec(0)
                height = if (shouldExpandSize) 192.toPx(this@ActComponentsGeometry) else if (!hasWeight) 192.toPx(this@ActComponentsGeometry) else 96.toPx(this@ActComponentsGeometry)
                width = if (shouldExpandSize) 340.toPx(this@ActComponentsGeometry) else 120.toPx(this@ActComponentsGeometry)

            }
            gridWrapper.addView(tvSize, tvSizeParams)
            componentViews.add(tvSize)
        }

// ======= 4. Рядок з вагою, якщо hasWeight =======
        if (component.compWeight.isNotEmpty()) {
            val tvWeight = TextView(this).apply {
                text = "> ${component.compWeight}."
                textSize = 24f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
                setBackgroundResource(R.drawable.tb_start_bott_end_withmargin_lightwt)
            }
            val tvWeightParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(
                    if (shouldExpandWeight) 2 else 3,
                    if (shouldExpandWeight) 2 else 1
                )
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0)
                height = if (shouldExpandSize) 192.toPx(this@ActComponentsGeometry) else if (!hasSize) 192.toPx(this@ActComponentsGeometry) else 96.toPx(this@ActComponentsGeometry)
                width = if (shouldExpandWeight) 340.toPx(this@ActComponentsGeometry) else 120.toPx(this@ActComponentsGeometry)
            }
            gridWrapper.addView(tvWeight, tvWeightParams)
            componentViews.add(tvWeight)
        }

// ======= 5. Фото, якщо є (розташовується у 2-му стовпці) =======
        if (!component.photoUri.isNullOrEmpty()) {
            val ivPhoto = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER
                try {
                    val uri = component.photoUri.toUri()
                    if (isUriValid(uri)) {
                        Glide.with(context)
                            .load(uri)
                            .into(this)
                    }
                } catch (_: Exception) {
                    setImageResource(R.drawable.svg_error)
                }
            }
            val ivPhotoParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec( 2, 2)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(1)
                when (component.compType) {
                    "Handlebar" , "Cranks" -> {
                        width = 220.toPx(this@ActComponentsGeometry)
                        height = 108.toPx(this@ActComponentsGeometry)
                    }
                    "Rim" -> {
                        width = 190.toPx(this@ActComponentsGeometry)
                        height = 192.toPx(this@ActComponentsGeometry)
                    }
                    "Tyre" -> {
                        width = 220.toPx(this@ActComponentsGeometry)
                        height = 192.toPx(this@ActComponentsGeometry)
                    }
                    else -> {
                        width = 108.toPx(this@ActComponentsGeometry)
                        height = 192.toPx(this@ActComponentsGeometry)
                    }
                }
                setGravity(Gravity.CENTER)
            }
            gridWrapper.addView(ivPhoto, ivPhotoParams)
            componentViews.add(ivPhoto)
        }

// ======= 6. Нотатки, якщо вони є =======
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
                rowSpec = androidx.gridlayout.widget.GridLayout.spec( 4, 1)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
                height = 48.toPx(this@ActComponentsGeometry)
                width = MATCH_PARENT
            }
            gridWrapper.addView(tvNotes, tvNotesParams)
            componentViews.add(tvNotes)
            // Коригуємо індекс, якщо нотатки додано
            nextRowIndex += 1
        }

// ======= 7. Додаємо відступ між компонентами =======
        nextRowIndex += 4
        val marginView = View(this).apply {
            layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
                height = 25.toPx(this@ActComponentsGeometry)
                width = MATCH_PARENT
            }
        }
        gridWrapper.addView(marginView)
        componentViews.add(marginView)

// Нарешті, додаємо сформований gridWrapper до зовнішнього LinearLayout
        parentContainer.addView(gridWrapper)


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
                            reloadComponents(bikeId)

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
            val dialogView = layoutInflater.inflate(R.layout.di_component_info, null).apply { currentDialogView = this }
            val editDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()
            val transparentColor = resources.getColor(R.color.transparent, theme)
            editDialog.window?.setBackgroundDrawable(transparentColor.toDrawable())
            // Отримуємо посилання на всі поля та чекбокси
            val etBrand = dialogView.findViewById<EditText>(R.id.compbrand)
            val etModel = dialogView.findViewById<EditText>(R.id.compmodel)
            val adaptiveEditText = dialogView.findViewById<EditText>(R.id.compAdaptive)
            val etYear = dialogView.findViewById<EditText>(R.id.compyear)
            val etSize = dialogView.findViewById<EditText>(R.id.compsize)
            val etWeight = dialogView.findViewById<EditText>(R.id.compweight)
            val etNotes = dialogView.findViewById<EditText>(R.id.compnotes)
            val extraBrandLabel = dialogView.findViewById<TextView>(R.id.labelBrandExtra)
            val extraBrand = dialogView.findViewById<EditText>(R.id.compBrandExtra)


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
            extraBrand.setText(component.compBrandExtra)
            adaptiveEditText.setText(component.compAdaptive)
            etSize.setText(component.compSize)
            etWeight.setText(component.compWeight)
            etNotes.setText(component.compNotes)
            val uri = component.photoUri?.toUri()
            currentDialogView?.findViewById<ImageButton>(R.id.photoPlaceholder)?.let { imageButton ->
                Glide.with(this)
                    .load(uri)
                    .override(500, 500)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.svg_add_photo)
                    .error(R.drawable.svg_error)
                    .into(imageButton)
            }

            // Automatically set checkbox states based on data
            yearCb.isChecked = component.compYear.isNotEmpty()
            sizeCb.isChecked = component.compSize.isNotEmpty()
            weightCb.isChecked = component.compWeight.isNotEmpty()
            notesCb.isChecked = component.compNotes.isNotEmpty()

            val currentType = component.compType


// 2. Спрощена ініціалізація спінера з використанням apply
            // Варіант 1: обмежуємо адаптер лише одним елементом
            val spinner = dialogView.findViewById<Spinner>(R.id.compType).apply {
                val arrayAdapter = ArrayAdapter(
                    this@ActComponentsGeometry,
                    android.R.layout.simple_spinner_item,
                    listOf(currentType) // Використовуємо лише currentType
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                adapter = arrayAdapter
                setSelection(0)
            }

// 3. Отримання посилань на UI-елементи
            val views = mapOf(
                "adaptive" to dialogView.findViewById(R.id.labelAdaptive),
                "model" to dialogView.findViewById(R.id.labelModel),
                "forkSize" to dialogView.findViewById<TextView>(R.id.labelForkSize)
            )


            // 4. Логіка оновлення тексту у вигляді окремої функції
            fun updateLabels(selectedType: String) {
                // Мапа з конфігурацією для кожного типу
                val typeConfig = mapOf(
                    "Cranks" to mapOf(
                        "adaptive" to "Підмодель",
                        "forkSize" to "Довжина"
                    ),
                    "Handlebar" to mapOf(
                        "adaptive" to "Матеріал",
                        "forkSize" to "Довжина/ширина"
                    ),
                    "Rim" to mapOf(
                        "adaptive" to "Підмодель",
                        "forkSize" to "Розмір"
                    ),
                    "Fork" to mapOf(
                        "adaptive" to "Картридж",
                        "forkSize" to "Хід"
                    ),
                    "Shock" to mapOf(
                        "adaptive" to "Картридж",
                        "forkSize" to "Хід"
                    ),
                    "Tyre" to mapOf(
                        "model" to "Переднє",
                        "adaptive" to "Заднє",
                        "forkSize" to "Розмір"
                    )
                )
                views["adaptive"]?.text = ""
                views["model"]?.text = "Модель"
                views["forkSize"]?.text = "Розмір"

                // Застосування конфігурації для обраного типу
                typeConfig[selectedType]?.forEach { (key, value) ->
                    views[key]?.text = value
                }
                if (selectedType == "Saddle") {
                    adaptiveEditText.visibility = View.GONE
                    dialogView.findViewById<TextView>(R.id.labelWeight).visibility = View.GONE
                    etWeight.visibility = View.GONE
                    weightCb.visibility = View.GONE
                }
                if (selectedType == "Tyre"){
                    extraBrand.visibility = View.VISIBLE
                    extraBrandLabel.visibility = View.VISIBLE
                } else {
                    extraBrand.visibility = View.GONE
                    extraBrandLabel.visibility = View.GONE
                }
            }
// 5. Спрощений обробник вибору з використанням when
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateLabels(parent?.getItemAtPosition(position).toString())
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
// 6. Ініціалізація стану при відкритті
            updateLabels(currentType)
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
                val updatedCompAdaptive = adaptiveEditText.text.toString()
                val updatedCompSize = if (sizeCb.isChecked)
                    etSize.text.toString() else ""
                val updatedCompWeight = if (weightCb.isChecked)
                    etWeight.text.toString() else ""
                val updatedCompNotes = if (notesCb.isChecked)
                    etNotes.text.toString() else ""
                val extraBrand1 = extraBrand.text.toString()
                // If the photo was not changed, keep the old value
                val photoUriString = selectedImageUri?.toString() ?: component.photoUri
                val updatedComponent = component.copy(
                    compBrand = updatedCompBrand,
                    compYear = updatedCompYear,
                    compModel = updatedCompModel,
                    compAdaptive = updatedCompAdaptive,
                    compSize = updatedCompSize,
                    compWeight = updatedCompWeight,
                    compBrandExtra = extraBrand1,
                    compNotes = updatedCompNotes,
                    photoUri = photoUriString
                )
                lifecycleScope.launch(Dispatchers.IO) {

                    BikeDatabase.getDatabase(applicationContext).componentsDao().updateComponent(updatedComponent)
                    withContext(Dispatchers.Main) {
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
        val gridContainer = findViewById<LinearLayout>(R.id.components_container_grid)
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
    private fun Int.toPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

    private fun showBackgroundRemovalDialog(originalUri: Uri) {
        // Закриваємо попередній діалог якщо він відкритий
        backgroundRemovalDialog?.dismiss()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_background_removal, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        backgroundRemovalDialog = dialog // Зберігаємо посилання на новий діалог

        val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreview)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekBarThreshold)
        val colorPreview = dialogView.findViewById<ImageView>(R.id.colorPreview)

        // Кнопка для вибору кольору
        colorPreview.setOnClickListener {
            showColorPickerDialog()
        }

        try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val originalBitmap = contentResolver.openInputStream(originalUri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            val scaledBitmap = originalBitmap?.scale(originalBitmap.width / 4, originalBitmap.height / 4)
            var currentThreshold = 50

            // Оновлення прев'ю при зміні параметрів
            fun updatePreview() {
                scaledBitmap?.let {
                    imagePreview.setImageBitmap(
                        removeBackgroundByColor(it, currentThreshold, selectedColor)
                    )
                }
            }

            seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    currentThreshold = progress
                    updatePreview()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            dialogView.findViewById<Button>(R.id.btnCancel)?.setOnClickListener { dialog.dismiss() }
            dialogView.findViewById<Button>(R.id.btnConfirm)?.setOnClickListener {
                lifecycleScope.launch(Dispatchers.Default) {
                    originalBitmap?.let { bitmap ->
                        val processedBitmap = removeBackgroundByColor(bitmap, currentThreshold, selectedColor)
                        val processedUri = saveBitmapToInternalStorage(processedBitmap)
                        withContext(Dispatchers.Main) {
                            selectedImageUri = processedUri
                            updatePhotoPreview(processedUri)
                            if (isExternalUri(processedUri)) takePersistablePermissions(processedUri)
                            dialog.dismiss()
                        }
                    }
                }
            }

            updatePreview()
            colorPreview.setBackgroundColor(selectedColor)

        } catch (_: Exception) {
            Toast.makeText(this, "Помилка завантаження зображення", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showColorPickerDialog() {
        val colorPicker = AmbilWarnaDialog(this, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                selectedColor = color
                // Оновлюємо прев'ю кольору
                currentDialogView?.findViewById<ImageView>(R.id.colorPreview)?.setBackgroundColor(color)
// Закриваємо поточний діалог видалення фону
                backgroundRemovalDialog?.dismiss()
                // Отримуємо URI через selectedImageUri, який вже містить результат обрізання
                selectedImageUri?.let { uri ->
                    showBackgroundRemovalDialog(uri)
                } ?: run {
                    Toast.makeText(
                        this@ActComponentsGeometry,
                        "Зображення не знайдено",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancel(dialog: AmbilWarnaDialog) {}
        })
        colorPicker.show()
    }

    private fun removeBackgroundByColor(bitmap: Bitmap, threshold: Int, targetColor: Int): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return bitmap
        result.setHasAlpha(true)

        val targetRed = Color.red(targetColor)
        val targetGreen = Color.green(targetColor)
        val targetBlue = Color.blue(targetColor)

        val thresholdValue = (threshold * 2.55).toInt() // Конвертація 0-100 у 0-255

        for (x in 0 until result.width) {
            for (y in 0 until result.height) {
                val pixel = result[x , y]
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                if (abs(red - targetRed) <= thresholdValue &&
                    abs(green - targetGreen) <= thresholdValue &&
                    abs(blue - targetBlue) <= thresholdValue
                ) {
                    result[x , y] = Color.TRANSPARENT
                }
            }
        }
        return result
    }
    private fun showConfirmationDialog(resultUri: Uri) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_adder_img, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            // Додаткові дії не потрібні, оскільки selectedImageUri вже оновлено
        }

        dialogView.findViewById<Button>(R.id.btnOkRemoveBg).setOnClickListener {
            dialog.dismiss()
            showBackgroundRemovalDialog(resultUri)
        }

        dialog.show()
    }

}