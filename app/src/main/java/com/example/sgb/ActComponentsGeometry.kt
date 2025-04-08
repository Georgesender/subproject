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
import android.widget.LinearLayout
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
    // Глобальна змінна для зберігання доступних типів
    private lateinit var availableComponentTypes: MutableList<String>
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
// Ініціалізація списку доступних типів з ресурсів
        availableComponentTypes = resources.getStringArray(R.array.component_types).toMutableList()
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
        // Допоміжна функція для конвертації dp в px
        fun Int.toPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
        fun addComponentToUI(component: Component) {
            val gridContainer = findViewById<GridLayout>(R.id.components_container_grid)
            var nextRowIndex = gridContainer.childCount

            // Контейнер для рядка, який міститиме кнопки та назву компонента
            val rowContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 8, 8, 8)
            }

            // Кнопка для видалення (25x25dp)
            val btnDelete = ImageButton(this).apply {
                setImageResource(R.drawable.img_delete) // Ваш ресурс для іконки видалення
                layoutParams = LinearLayout.LayoutParams(25.toPx(this@ActComponentsGeometry), 25.toPx(this@ActComponentsGeometry))
                setBackgroundResource(0) // робимо фон прозорим, або задаємо кастомний стиль
            }

            // Кнопка для редагування (25x25dp)
            val btnEdit = ImageButton(this).apply {
                setImageResource(R.drawable.img_edit) // Ваш ресурс для іконки редагування
                layoutParams = LinearLayout.LayoutParams(25.toPx(this@ActComponentsGeometry), 25.toPx(this@ActComponentsGeometry))
                setBackgroundResource(0)
            }

            // TextView для відображення типу компонента
            val tvType = TextView(this).apply {
                text = component.compType
                textSize = 25f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
                // Можна задати свій background чи відступи
                setPadding(16, 8, 8, 8)
            }

            // Додаємо кнопки та текст до контейнера
            rowContainer.addView(btnDelete)
            rowContainer.addView(btnEdit)
            rowContainer.addView(tvType)

            // Розміщуємо контейнер у GridLayout
            val rowParams = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(nextRowIndex, 1)
                columnSpec = GridLayout.spec(0, 2)
                width = GridLayout.LayoutParams.WRAP_CONTENT
                height = 44.toPx(this@ActComponentsGeometry)
                setGravity(Gravity.CENTER)
            }
            gridContainer.addView(rowContainer, rowParams)

            // Наступні рядки (заголовок, розмір, вага, фото, нотатки) – як раніше
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
            // 5-й рядок: Нотатки – тепер завжди створюємо TextView для нотаток
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
            // Оновлюємо nextRowIndex (якщо даних нотаток не було, цей TextView може бути порожнім)
            nextRowIndex += 5

            // Видалення компонента
            btnDelete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Підтвердження видалення")
                    .setMessage("Ви впевнені, що хочете видалити цей компонент?")
                    .setPositiveButton("Так") { dialog, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            // Видаляємо компонент з бази даних
                            BikeDatabase.getDatabase(applicationContext).componentsDao().deleteComponent(component)
                            withContext(Dispatchers.Main) {
                                // Видаляємо рядок з GridLayout
                                gridContainer.removeView(rowContainer)
                                // (Можна також видалити і пов’язані з компонентом рядки, наприклад, заголовок тощо)
                                // Повертаємо тип компонента у список доступних
                                availableComponentTypes.add(component.compType)
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Ні") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

            // Редагування компонента
            btnEdit.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.di_component_info, null)
                val editDialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create()

                // Попереднє заповнення полів поточними даними
                dialogView.findViewById<EditText>(R.id.compbrand).setText(component.compBrand)
                dialogView.findViewById<EditText>(R.id.compyear).setText(component.compYear)
                dialogView.findViewById<EditText>(R.id.compmodel).setText(component.compModel)
                dialogView.findViewById<EditText>(R.id.compsize).setText(component.compSize)
                dialogView.findViewById<EditText>(R.id.compweight).setText(component.compWeight)
                dialogView.findViewById<EditText>(R.id.compnotes).setText(component.compNotes)
                // Зберігаємо посилання на чекбокси
                val yearCb = dialogView.findViewById<android.widget.CheckBox>(R.id.compyear_cb)
                val sizeCb = dialogView.findViewById<android.widget.CheckBox>(R.id.compsize_cb)
                val weightCb = dialogView.findViewById<android.widget.CheckBox>(R.id.compweight_cb)
                val notesCb = dialogView.findViewById<android.widget.CheckBox>(R.id.compnotes_cb)

                // Автоматичне встановлення стану чекбоксів (це виконується лише на початку)
                yearCb?.isChecked = component.compYear.isNotEmpty()
                sizeCb?.isChecked = component.compSize.isNotEmpty()
                weightCb?.isChecked = component.compWeight.isNotEmpty()
                notesCb?.isChecked = component.compNotes.isNotEmpty()

                // Кнопка для вибору фото (як у вас)
                dialogView.findViewById<ImageButton>(R.id.photoPlaceholder)?.setOnClickListener {
                    pickImageForComponent()
                }

                // Обробка підтвердження редагування
                dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
                    // Зчитування значень з діалогу для редагування
                    val updatedCompBrand = dialogView.findViewById<EditText>(R.id.compbrand).text.toString()
                    val updatedCompYear = if (yearCb?.isChecked == true)
                        dialogView.findViewById<EditText>(R.id.compyear).text.toString() else ""
                    val updatedCompModel = dialogView.findViewById<EditText>(R.id.compmodel).text.toString()
                    val updatedCompSize = if (sizeCb?.isChecked == true)
                        dialogView.findViewById<EditText>(R.id.compsize).text.toString() else ""
                    val updatedCompWeight = if (weightCb?.isChecked == true)
                        dialogView.findViewById<EditText>(R.id.compweight).text.toString() else ""
                    val updatedCompNotes = if (notesCb?.isChecked == true)
                        dialogView.findViewById<EditText>(R.id.compnotes).text.toString()  else ""
                    val photoUriString =
                        selectedImageUri?.toString() ?: component.photoUri  // якщо фото не змінювалось, залишаємо старе значення


                    // Створюємо оновлений об'єкт компонента (ідентифікатор залишається незмінним)
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

                            // Оновлюємо UI (наприклад, tvHeader, tvSize, tvWeight, ivPhoto)
                            val newHeaderText = SpannableStringBuilder("${updatedComponent.compBrand} ${updatedComponent.compModel}")
                            if (updatedComponent.compYear.isNotEmpty()) {
                                newHeaderText.append(" ${updatedComponent.compYear}")
                                val start = newHeaderText.length - updatedComponent.compYear.length
                                newHeaderText.setSpan(SuperscriptSpan(), start, newHeaderText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                newHeaderText.setSpan(RelativeSizeSpan(0.7f), start, newHeaderText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            tvHeader.text = newHeaderText
                            tvSize.text = if (updatedComponent.compSize.isNotEmpty()) "Size: ${updatedComponent.compSize}" else ""
                            tvWeight.text = if (updatedComponent.compWeight.isNotEmpty()) "Weight: ${updatedComponent.compWeight}" else ""
                            tvNotes.text = if (updatedComponent.compNotes.isNotEmpty()) updatedComponent.compNotes else ""
                            if (!updatedComponent.photoUri.isNullOrEmpty()) {dialogView.findViewById<EditText>(R.id.compnotes).text
                                ivPhoto.setImageURI(updatedComponent.photoUri.toUri())
                            }
                            editDialog.dismiss()
                        }
                    }
                }
                editDialog.show()
            }


        }




        btnAddComponent.setOnClickListener {
            // Якщо немає доступних типів для вибору, показуємо повідомлення
            if (availableComponentTypes.isEmpty()) {
                Toast.makeText(this, "Всі типи компонентів вже додані", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dialogView = layoutInflater.inflate(R.layout.di_component_info, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            // Замість завантаження з ресурсів використовуємо availableComponentTypes
            val spinner = dialogView.findViewById<Spinner>(R.id.compType)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableComponentTypes)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            // Ініціалізація ImageButton в діалоговому вікні
            val dialogPhotoPlaceholder = dialogView.findViewById<ImageButton>(R.id.photoPlaceholder)
            dialogPhotoPlaceholder?.setOnClickListener {
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
                        // Видаляємо обраний тип із списку доступних
                        availableComponentTypes.remove(selectedType)
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
                    // Якщо в базі вже існує компонент із певним типом, видаляємо цей тип із availableComponentTypes,
                    // щоб уникнути його повторного вибору
                    availableComponentTypes.remove(component.compType)
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
