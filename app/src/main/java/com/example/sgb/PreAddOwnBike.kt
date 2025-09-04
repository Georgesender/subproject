package com.example.sgb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.graphics.set
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.sgb.room.Bike
import com.example.sgb.room.BikeDao
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.BikeGeometry
import com.example.sgb.room.BikeModel
import com.example.sgb.room.BikeSubmodel
import com.example.sgb.room.GeometryDao
import com.example.sub.R
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PreAddOwnBike : AppCompatActivity() {
    private val REQUEST_CODE_READ_MEDIA_IMAGES = 1001
    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1002
    private var backgroundRemovalDialog: AlertDialog? = null
    private var selectedColor: Int = Color.WHITE // Початковий колір для видалення

    private lateinit var etBrand: EditText
    private lateinit var etModel: EditText
    private lateinit var etSubmodel: EditText
    private lateinit var etYear: EditText
    private lateinit var etSelectedSize: EditText

    private lateinit var bikeDao: BikeDao
    private lateinit var geometryDao: GeometryDao

    // Поля для геометрії
    private lateinit var etWheelBase: EditText
    private lateinit var etReach: EditText
    private lateinit var etStack: EditText
    private lateinit var etBottomBracketOffset: EditText
    private lateinit var etStandOverHeight: EditText
    private lateinit var etHeadTubeLength: EditText
    private lateinit var etSeatTubeAngle: EditText
    private lateinit var etSeatTubeLength: EditText
    private lateinit var etTopTubeLength: EditText
    private lateinit var etSeatHeight: EditText
    private lateinit var etHeadTubeAngle: EditText
    private lateinit var etChainstayLength: EditText
    private lateinit var etBodyHeight: EditText
    private lateinit var etWheelSize: EditText

    private lateinit var addBikeImage: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_manual_bike)

        bikeDao = BikeDatabase.getDatabase(this).bikeDao()
        geometryDao = BikeDatabase.getDatabase(this).geometryDao()

        etBrand = findViewById(R.id.etBrand)
        etModel = findViewById(R.id.etModel)
        etSubmodel = findViewById(R.id.etSubmodel)
        etYear = findViewById(R.id.etYear)
        etSelectedSize = findViewById(R.id.etSelectedSize)

        // Ініціалізація нових полів геометрії
        etWheelBase = findViewById(R.id.etWheelBase)
        etReach = findViewById(R.id.etReach)
        etStack = findViewById(R.id.etStack)
        etBottomBracketOffset = findViewById(R.id.etBottomBracketOffset)
        etStandOverHeight = findViewById(R.id.etStandOverHeight)
        etHeadTubeLength = findViewById(R.id.etHeadTubeLength)
        etSeatTubeAngle = findViewById(R.id.etSeatTubeAngle)
        etSeatTubeLength = findViewById(R.id.etSeatTubeLength)
        etTopTubeLength = findViewById(R.id.etTopTubeLength)
        etSeatHeight = findViewById(R.id.etSeatHeight)
        etHeadTubeAngle = findViewById(R.id.etHeadTubeAngle)
        etChainstayLength = findViewById(R.id.etChainstayLength)
        etBodyHeight = findViewById(R.id.etBodyHeight)
        etWheelSize = findViewById(R.id.etWheelSize)

        addBikeImage = findViewById(R.id.bike_image)
        addBikeImage.setOnClickListener{
            pickImageForComponent()
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveBikeToDatabase()

        }


    }
        private fun saveBikeToDatabase() {


            val brand = etBrand.text.toString()
            val model = etModel.text.toString()
            val submodel = etSubmodel.text.toString()
            val year = etYear.text.toString()
            val size = etSelectedSize.text.toString()
            val photoUriString = selectedImageUri.toString()

            val bikeSubmodel = BikeSubmodel(
                name = submodel ,
                years = mapOf(size to listOf(year)) ,
                geometry = null
            )

            val bikeModel = BikeModel(
                name = model ,
                submodels = mapOf(submodel to bikeSubmodel)
            )

            val bike = Bike(
                brand = brand ,
                modelsJson = mapOf(model to bikeModel) ,
                addedImgBikeUri = photoUriString ,
                selectedSize = size
            )
            lifecycleScope.launch {
// Вставка байка та отримання ID
                val bikeId = bikeDao.insertBike(bike).toInt()



                // Парсимо значення геометрії
                val geometry = BikeGeometry(
                    bikeId = bikeId , // Тимочасове значення, буде оновлено після вставки байка
                    wheelBase = etWheelBase.text.toString().toIntOrNull() ?: 0 ,
                    reach = etReach.text.toString().toIntOrNull() ?: 0 ,
                    stack = etStack.text.toString().toIntOrNull() ?: 0 ,
                    bottomBracketOffset = etBottomBracketOffset.text.toString().toIntOrNull() ?: 0 ,
                    standOverHeight = etStandOverHeight.text.toString().toIntOrNull() ?: 0 ,
                    headTubeLength = etHeadTubeLength.text.toString().toIntOrNull() ?: 0 ,
                    seatTubeAngle = etSeatTubeAngle.text.toString().toIntOrNull() ?: 0 ,
                    seatTubeLength = etSeatTubeLength.text.toString().toIntOrNull() ?: 0 ,
                    topTubeLength = etTopTubeLength.text.toString().toIntOrNull() ?: 0 ,
                    seatHeight = etSeatHeight.text.toString().toIntOrNull() ?: 0 ,
                    headTubeAngle = etHeadTubeAngle.text.toString().toIntOrNull() ?: 0 ,
                    chainstayLength = etChainstayLength.text.toString().toIntOrNull() ?: 0 ,
                    bodyHeight = etBodyHeight.text.toString().toIntOrNull() ?: 0 ,
                    wheelSize = etWheelSize.text.toString().toIntOrNull() ?: 0
                )
                geometryDao.insertGeometry(geometry)
                // Перехід після успішного збереження
                val intent = Intent(this@PreAddOwnBike , GarageActivity::class.java)
                startActivity(intent)
                finish()
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
            imagePickerLauncher.launch(arrayOf("image/*", "video/webm"))
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { originalUri ->
            lifecycleScope.launch {
                val processedUri = handleFileType(originalUri)
                withContext(Dispatchers.Main) {
                    startCrop(processedUri)
                }
            }
        }
    }

    private suspend fun handleFileType(uri: Uri): Uri {
        val mimeType = contentResolver.getType(uri)
        return if (mimeType == "video/webm") {
            convertWebMToPng(uri)
        } else {
            uri
        }
    }

    private suspend fun convertWebMToPng(uri: Uri): Uri = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this@PreAddOwnBike, uri)
            val bitmap = retriever.getFrameAtTime(0) // Отримуємо перший кадр
            bitmap?.let {
                saveBitmapToInternalStorage(it)
            } ?: throw Exception("Не вдалося отримати кадр із WebM")
        } catch (e: Exception) {
            Log.e("WebMConversion", "Помилка конвертації WebM: ${e.message}")
            uri // Повертаємо оригінальний URI у разі помилки
        } finally {
            retriever.release()
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationFileName = "cropped_${System.currentTimeMillis()}.png"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))

        val options = UCrop.Options().apply {
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)
            setStatusBarColor(ContextCompat.getColor(this@PreAddOwnBike, R.color.black))
            setToolbarColor(ContextCompat.getColor(this@PreAddOwnBike, R.color.white))
            setToolbarTitle("Обріжте фото")
            setCompressionFormat(Bitmap.CompressFormat.PNG)
            setCompressionQuality(100)
        }

        val uCrop = UCrop.of(uri, destinationUri)
            .withOptions(options)
            .withMaxResultSize(2000, 2000)

        val uCropIntent = uCrop.getIntent(this)
        cropImageLauncher.launch(uCropIntent)
    }

    private var selectedImageUri: Uri? = null

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                selectedImageUri = resultUri
                updatePhotoPreview(resultUri)
                if (isExternalUri(resultUri)) {
                    takePersistablePermissions(resultUri)
                }
                showConfirmationDialog(resultUri)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(this, "Помилка обрізання: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePhotoPreview(uri: Uri?) {
        findViewById<ImageButton>(R.id.bike_image)?.let { imageButton ->
            Glide.with(this)
                .load(uri)
                .override(500, 500)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.img_fork)
                .error(R.drawable.img_fork)
                .into(imageButton)
        }
    }

    private fun isExternalUri(uri: Uri): Boolean {
        return when (uri.scheme) {
            "content" -> uri.authority?.startsWith("com.android.externalstorage") == true
            else -> false
        }
    }

    private fun takePersistablePermissions(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: SecurityException) {
            Toast.makeText(this, "Не вдалося зберегти дозвіл для доступу до зображення", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfirmationDialog(resultUri: Uri) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.di_adder_img, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnOkRemoveBg).setOnClickListener {
            dialog.dismiss()
            showBackgroundRemovalDialog(resultUri)
        }

        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showBackgroundRemovalDialog(originalUri: Uri) {
        backgroundRemovalDialog?.dismiss()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_background_removal, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        backgroundRemovalDialog = dialog

        val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreview)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekBarThreshold)
        val colorPreview = dialogView.findViewById<ImageView>(R.id.colorPreview)

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

            fun updatePreview() {
                scaledBitmap?.let {
                    imagePreview.setImageBitmap(
                        removeBackgroundByColor(it, currentThreshold, selectedColor)
                    )
                }
            }

            // Вибір кольору через натискання на зображення
// Вибір кольору через натискання на зображення
            imagePreview.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    scaledBitmap?.let { bitmap ->
                        val x = event.x.toInt().coerceIn(0, bitmap.width - 1)
                        val y = event.y.toInt().coerceIn(0, bitmap.height - 1)
                        selectedColor = bitmap[x, y] // Використовуємо KTX-розширення
                        colorPreview.setBackgroundColor(selectedColor)
                        updatePreview()
                        view.performClick() // Додаємо для доступності
                    }
                    true
                } else {
                    false
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
                backgroundRemovalDialog?.dismiss()
                selectedImageUri?.let { uri ->
                    showBackgroundRemovalDialog(uri)
                } ?: Toast.makeText(this@PreAddOwnBike, "Зображення не знайдено", Toast.LENGTH_SHORT).show()
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

        val thresholdValue = (threshold * 2.55).toInt()

        for (x in 0 until result.width) {
            for (y in 0 until result.height) {
                val pixel = result[x, y]
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                val distance = kotlin.math.sqrt(
                    ((red - targetRed) * (red - targetRed) +
                            (green - targetGreen) * (green - targetGreen) +
                            (blue - targetBlue) * (blue - targetBlue)).toDouble()
                ).toInt()

                if (distance <= thresholdValue) {
                    result[x, y] = Color.TRANSPARENT
                }
            }
        }
        return result
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap): Uri {
        val filename = "img_${UUID.randomUUID()}.png"
        val imagesDir = File(filesDir, "images").apply { mkdirs() }
        val file = File(imagesDir, filename).apply {
            FileOutputStream(this).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
        return FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
    }
    }
