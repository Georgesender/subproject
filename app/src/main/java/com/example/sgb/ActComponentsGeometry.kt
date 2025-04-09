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
import android.view.View
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

    // Global variable to store available component types
    private lateinit var availableComponentTypes: MutableList<String>

    // Using OpenDocument, so parameter is Array<String>
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Array<String>>
    private val REQUEST_CODE_READ_MEDIA_IMAGES = 1001
    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1002

    // Global variable to hold the selected image URI
    private var selectedImageUri: Uri? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_compgeomrty)

        val bikeId = intent.getIntExtra("bike_id", -1)
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)

        // Initialize list of available component types from resources
        availableComponentTypes = resources.getStringArray(R.array.component_types).toMutableList()

        initViews()
        backButtonListener(selectedBikeId)

        // Initialize imagePickerLauncher using OpenDocument
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                // Set fixed flags for persisting URI permission
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to save permission for image access", Toast.LENGTH_SHORT).show()
                }
                selectedImageUri = it
            }
        }

        // Load bike data if bikeId is valid
        if (bikeId != -1) {
            loadBikeData(bikeId)
        }

        val btnAddComponent: Button = findViewById(R.id.Add_component_info)



        fun addComponentToUI(component: Component) {
            val gridContainer = findViewById<androidx.gridlayout.widget.GridLayout>(R.id.components_container_grid)
            var nextRowIndex = gridContainer.childCount

            val componentViews = mutableListOf<View>()

            // Container for row that includes delete/edit buttons and component type text
            val rowContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 8, 8, 8)
            }

            // Delete button (25x25dp)
            val btnDelete = ImageButton(this).apply {
                setImageResource(R.drawable.img_delete) // Resource for delete icon
                layoutParams = LinearLayout.LayoutParams(25.toPx(this@ActComponentsGeometry), 25.toPx(this@ActComponentsGeometry))
                setBackgroundResource(0) // Remove background or apply custom style
            }

            // Edit button (25x25dp)
            val btnEdit = ImageButton(this).apply {
                setImageResource(R.drawable.img_edit) // Resource for edit icon
                layoutParams = LinearLayout.LayoutParams(25.toPx(this@ActComponentsGeometry), 25.toPx(this@ActComponentsGeometry))
                setBackgroundResource(0)
            }

            // TextView to display component type
            val tvType = TextView(this).apply {
                text = component.compType
                textSize = 25f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
                setPadding(16, 8, 8, 8)
            }

            // Add buttons and text to row container
            rowContainer.apply {
                addView(btnDelete)
                addView(btnEdit)
                addView(tvType)
            }

            // Add row container to androidx.gridlayout.widget.GridLayout
            val rowParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex, 1)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
                width = androidx.gridlayout.widget.GridLayout.LayoutParams.WRAP_CONTENT
                height = 44.toPx(this@ActComponentsGeometry)
                setGravity(Gravity.CENTER)
            }
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
                setPadding(0, 12.toPx(this@ActComponentsGeometry), 0, 0)
            }
            val tvHeaderParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 1, 1)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
                height = 42.toPx(this@ActComponentsGeometry)
                width = 200.toPx(this@ActComponentsGeometry)
                setGravity(Gravity.CENTER)
            }
            gridContainer.addView(tvHeader, tvHeaderParams)
            componentViews.add(tvHeader)
            // 3rd row: Size
            val tvSize = TextView(this).apply {
                text = if (component.compSize.isNotEmpty()) "Size: ${component.compSize}" else ""
                textSize = 18f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
                setBackgroundResource(R.drawable.tb_start_bott_end_withmargin)
            }
            val tvSizeParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 2, 1)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0)
                height = 50.toPx(this@ActComponentsGeometry)
            }
            gridContainer.addView(tvSize, tvSizeParams)
            componentViews.add(tvSize)

            // 4th row: Weight
            val tvWeight = TextView(this).apply {
                text = if (component.compWeight.isNotEmpty()) "Weight: ${component.compWeight}" else ""
                textSize = 18f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
                setBackgroundResource(R.drawable.tb_start_bott_end_withmargin)
            }
            val tvWeightParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 3, 1)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(0)
                height = 50.toPx(this@ActComponentsGeometry)
            }
            gridContainer.addView(tvWeight, tvWeightParams)
            componentViews.add(tvWeight)
            // Photo: occupies 3rd and 4th row in 2nd column
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
                    setImageResource(R.drawable.img_fork) // Default image resource if no photo exists
                }
            }
            val ivPhotoParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 2, 2)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(1)
                width = androidx.gridlayout.widget.GridLayout.LayoutParams.MATCH_PARENT
                height = 100.toPx(this@ActComponentsGeometry)
                setMargins(8.toPx(this@ActComponentsGeometry), 8.toPx(this@ActComponentsGeometry), 8.toPx(this@ActComponentsGeometry), 8.toPx(this@ActComponentsGeometry))
            }
            gridContainer.addView(ivPhoto, ivPhotoParams)
            componentViews.add(ivPhoto)
            // 5th row: Notes
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
                width = androidx.gridlayout.widget.GridLayout.LayoutParams.MATCH_PARENT

            }
            gridContainer.addView(tvNotes, tvNotesParams)
            componentViews.add(tvNotes)
            nextRowIndex += 5

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

                // Pre-fill fields with current component data
                dialogView.findViewById<EditText>(R.id.compbrand).setText(component.compBrand)
                dialogView.findViewById<EditText>(R.id.compyear).setText(component.compYear)
                dialogView.findViewById<EditText>(R.id.compmodel).setText(component.compModel)
                dialogView.findViewById<EditText>(R.id.compsize).setText(component.compSize)
                dialogView.findViewById<EditText>(R.id.compweight).setText(component.compWeight)
                dialogView.findViewById<EditText>(R.id.compnotes).setText(component.compNotes)

                val yearCb = dialogView.findViewById<CheckBox>(R.id.compyear_cb)
                val sizeCb = dialogView.findViewById<CheckBox>(R.id.compsize_cb)
                val weightCb = dialogView.findViewById<CheckBox>(R.id.compweight_cb)
                val notesCb = dialogView.findViewById<CheckBox>(R.id.compnotes_cb)

                // Automatically set checkbox states based on data
                yearCb.isChecked = component.compYear.isNotEmpty()
                sizeCb.isChecked = component.compSize.isNotEmpty()
                weightCb.isChecked = component.compWeight.isNotEmpty()
                notesCb.isChecked = component.compNotes.isNotEmpty()

                // Photo selection button in the dialog
                dialogView.findViewById<ImageButton>(R.id.photoPlaceholder)?.setOnClickListener {
                    pickImageForComponent()
                }

                // Confirm edit button event
                dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
                    // Read values from dialog for updating component
                    val updatedCompBrand = dialogView.findViewById<EditText>(R.id.compbrand).text.toString()
                    val updatedCompYear = if (yearCb.isChecked)
                        dialogView.findViewById<EditText>(R.id.compyear).text.toString() else ""
                    val updatedCompModel = dialogView.findViewById<EditText>(R.id.compmodel).text.toString()
                    val updatedCompSize = if (sizeCb.isChecked)
                        dialogView.findViewById<EditText>(R.id.compsize).text.toString() else ""
                    val updatedCompWeight = if (weightCb.isChecked)
                        dialogView.findViewById<EditText>(R.id.compweight).text.toString() else ""
                    val updatedCompNotes = if (notesCb.isChecked)
                        dialogView.findViewById<EditText>(R.id.compnotes).text.toString() else ""
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
                            tvSize.text = if (updatedComponent.compSize.isNotEmpty()) "Size: ${updatedComponent.compSize}" else ""
                            tvWeight.text = if (updatedComponent.compWeight.isNotEmpty()) "Weight: ${updatedComponent.compWeight}" else ""
                            tvNotes.text = updatedComponent.compNotes
                            if (!updatedComponent.photoUri.isNullOrEmpty()) {
                                ivPhoto.setImageURI(updatedComponent.photoUri.toUri())
                            } else {
                                ivPhoto.setImageResource(R.drawable.img_fork)
                            }
                            editDialog.dismiss()
                        }
                    }
                }
                editDialog.show()
            }
        }

        btnAddComponent.setOnClickListener {
            // If there are no available component types, show a toast message
            if (availableComponentTypes.isEmpty()) {
                Toast.makeText(this, "All component types have been added", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dialogView = layoutInflater.inflate(R.layout.di_component_info, null)
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
                        // Remove the chosen type from available component types
                        availableComponentTypes.remove(selectedType)
                        addComponentToUI(newComponent)
                        reloadComponents(bikeId)
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
    fun addComponentToUI(component: Component) {
        val gridContainer = findViewById<androidx.gridlayout.widget.GridLayout>(R.id.components_container_grid)
        var nextRowIndex = gridContainer.childCount

        val componentViews = mutableListOf<View>()

        // Container for row that includes delete/edit buttons and component type text
        val rowContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 8)
        }

        // Delete button (25x25dp)
        val btnDelete = ImageButton(this).apply {
            setImageResource(R.drawable.img_delete) // Resource for delete icon
            layoutParams = LinearLayout.LayoutParams(25.toPx(this@ActComponentsGeometry), 25.toPx(this@ActComponentsGeometry))
            setBackgroundResource(0) // Remove background or apply custom style
        }

        // Edit button (25x25dp)
        val btnEdit = ImageButton(this).apply {
            setImageResource(R.drawable.img_edit) // Resource for edit icon
            layoutParams = LinearLayout.LayoutParams(25.toPx(this@ActComponentsGeometry), 25.toPx(this@ActComponentsGeometry))
            setBackgroundResource(0)
        }

        // TextView to display component type
        val tvType = TextView(this).apply {
            text = component.compType
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            setPadding(16, 8, 8, 8)
        }

        // Add buttons and text to row container
        rowContainer.apply {
            addView(btnDelete)
            addView(btnEdit)
            addView(tvType)
        }

        // Add row container to androidx.gridlayout.widget.GridLayout
        val rowParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
            rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex, 1)
            columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
            width = androidx.gridlayout.widget.GridLayout.LayoutParams.WRAP_CONTENT
            height = 44.toPx(this@ActComponentsGeometry)
            setGravity(Gravity.CENTER)
        }
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
            setPadding(0, 12.toPx(this@ActComponentsGeometry), 0, 0)
        }
        val tvHeaderParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
            rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 1, 1)
            columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 2)
            height = 42.toPx(this@ActComponentsGeometry)
            width = 200.toPx(this@ActComponentsGeometry)
            setGravity(Gravity.CENTER)
        }
        gridContainer.addView(tvHeader, tvHeaderParams)
        componentViews.add(tvHeader)
        // 3rd row: Size
        val tvSize = TextView(this).apply {
            text = if (component.compSize.isNotEmpty()) "Size: ${component.compSize}" else ""
            textSize = 18f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            setBackgroundResource(R.drawable.tb_start_bott_end_withmargin)
        }
        val tvSizeParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
            rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 2, 1)
            columnSpec = androidx.gridlayout.widget.GridLayout.spec(0)
            height = 50.toPx(this@ActComponentsGeometry)
        }
        gridContainer.addView(tvSize, tvSizeParams)
        componentViews.add(tvSize)

        // 4th row: Weight
        val tvWeight = TextView(this).apply {
            text = if (component.compWeight.isNotEmpty()) "Weight: ${component.compWeight}" else ""
            textSize = 18f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(ContextCompat.getColor(this@ActComponentsGeometry, R.color.white))
            setBackgroundResource(R.drawable.tb_start_bott_end_withmargin)
        }
        val tvWeightParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
            rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 3, 1)
            columnSpec = androidx.gridlayout.widget.GridLayout.spec(0)
            height = 50.toPx(this@ActComponentsGeometry)
        }
        gridContainer.addView(tvWeight, tvWeightParams)
        componentViews.add(tvWeight)
        // Photo: occupies 3rd and 4th row in 2nd column
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
                setImageResource(R.drawable.img_fork) // Default image resource if no photo exists
            }
        }
        val ivPhotoParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
            rowSpec = androidx.gridlayout.widget.GridLayout.spec(nextRowIndex + 2, 2)
            columnSpec = androidx.gridlayout.widget.GridLayout.spec(1)
            width = androidx.gridlayout.widget.GridLayout.LayoutParams.MATCH_PARENT
            height = 100.toPx(this@ActComponentsGeometry)
            setMargins(8.toPx(this@ActComponentsGeometry), 8.toPx(this@ActComponentsGeometry), 8.toPx(this@ActComponentsGeometry), 8.toPx(this@ActComponentsGeometry))
        }
        gridContainer.addView(ivPhoto, ivPhotoParams)
        componentViews.add(ivPhoto)
        // 5th row: Notes
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
            width = androidx.gridlayout.widget.GridLayout.LayoutParams.MATCH_PARENT

        }
        gridContainer.addView(tvNotes, tvNotesParams)
        componentViews.add(tvNotes)
        nextRowIndex += 5

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

            // Pre-fill fields with current component data
            dialogView.findViewById<EditText>(R.id.compbrand).setText(component.compBrand)
            dialogView.findViewById<EditText>(R.id.compyear).setText(component.compYear)
            dialogView.findViewById<EditText>(R.id.compmodel).setText(component.compModel)
            dialogView.findViewById<EditText>(R.id.compsize).setText(component.compSize)
            dialogView.findViewById<EditText>(R.id.compweight).setText(component.compWeight)
            dialogView.findViewById<EditText>(R.id.compnotes).setText(component.compNotes)

            val yearCb = dialogView.findViewById<CheckBox>(R.id.compyear_cb)
            val sizeCb = dialogView.findViewById<CheckBox>(R.id.compsize_cb)
            val weightCb = dialogView.findViewById<CheckBox>(R.id.compweight_cb)
            val notesCb = dialogView.findViewById<CheckBox>(R.id.compnotes_cb)

            // Automatically set checkbox states based on data
            yearCb.isChecked = component.compYear.isNotEmpty()
            sizeCb.isChecked = component.compSize.isNotEmpty()
            weightCb.isChecked = component.compWeight.isNotEmpty()
            notesCb.isChecked = component.compNotes.isNotEmpty()

            // Photo selection button in the dialog
            dialogView.findViewById<ImageButton>(R.id.photoPlaceholder)?.setOnClickListener {
                pickImageForComponent()
            }

            // Confirm edit button event
            dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
                // Read values from dialog for updating component
                val updatedCompBrand = dialogView.findViewById<EditText>(R.id.compbrand).text.toString()
                val updatedCompYear = if (yearCb.isChecked)
                    dialogView.findViewById<EditText>(R.id.compyear).text.toString() else ""
                val updatedCompModel = dialogView.findViewById<EditText>(R.id.compmodel).text.toString()
                val updatedCompSize = if (sizeCb.isChecked)
                    dialogView.findViewById<EditText>(R.id.compsize).text.toString() else ""
                val updatedCompWeight = if (weightCb.isChecked)
                    dialogView.findViewById<EditText>(R.id.compweight).text.toString() else ""
                val updatedCompNotes = if (notesCb.isChecked)
                    dialogView.findViewById<EditText>(R.id.compnotes).text.toString() else ""
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
                        tvSize.text = if (updatedComponent.compSize.isNotEmpty()) "Size: ${updatedComponent.compSize}" else ""
                        tvWeight.text = if (updatedComponent.compWeight.isNotEmpty()) "Weight: ${updatedComponent.compWeight}" else ""
                        tvNotes.text = updatedComponent.compNotes
                        if (!updatedComponent.photoUri.isNullOrEmpty()) {
                            ivPhoto.setImageURI(updatedComponent.photoUri.toUri())
                        } else {
                            ivPhoto.setImageResource(R.drawable.img_fork)
                        }
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