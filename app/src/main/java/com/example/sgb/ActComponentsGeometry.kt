package com.example.sgb

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.Bike
import com.example.sgb.room.BikeDatabase
import com.example.sub.R
import kotlinx.coroutines.launch

class ActComponentsGeometry : AppCompatActivity() {
    private var nextRowIndex = 2 // Припустимо, що перші 2 рядки зайняті заголовком та кнопкою

    private lateinit var bikeAndModelView: TextView
    private lateinit var bikeImageView: ImageView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.kt_compgeomrty)

        val bikeId = intent.getIntExtra("bike_id" , -1)
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs" , MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id" , -1)

        initViews()

        backButtonListener(selectedBikeId)

        if (bikeId != -1) {
            loadBikeData(bikeId)
        }
        val btnAddComponent: Button = findViewById(R.id.Add_component_info)

        fun Int.toPx(context: Context): Int =
            (this * context.resources.displayMetrics.density).toInt()

        btnAddComponent.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.di_component_info , null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            val spinner = dialogView.findViewById<Spinner>(R.id.compType)
            val types = resources.getStringArray(R.array.component_types)
            val adapter = ArrayAdapter(this , android.R.layout.simple_spinner_item , types)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
                val compBrand = dialogView.findViewById<EditText>(R.id.compbrand).text.toString()
                val compYear = dialogView.findViewById<EditText>(R.id.compyear).text.toString()
                val compModel = dialogView.findViewById<EditText>(R.id.compmodel).text.toString()
                val compSize = dialogView.findViewById<EditText>(R.id.compsize).text.toString()
                val compWeight = dialogView.findViewById<EditText>(R.id.compweight).text.toString()
                val selectedType = spinner.selectedItem.toString()

                val photoResId = R.drawable.img_fork

                val gridContainer = findViewById<GridLayout>(R.id.components_container_grid)
                gridContainer.rowCount = 5

                val tvType = TextView(this).apply {
                    text = selectedType
                    textSize = 25f
                    setTypeface(null , Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(dialog.context , R.color.white))
                    background = ContextCompat.getDrawable(dialog.context , R.drawable.tb_end_start)
                    setPadding(24 , 8 , 8 , 0)
                }
                val tvTypeParams = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(0)
                    columnSpec = GridLayout.spec(0 , 2)
                    width = 390.toPx(this@ActComponentsGeometry)
                    height = 36.toPx(this@ActComponentsGeometry)


                }
                tvType.layoutParams = tvTypeParams
                gridContainer.addView(tvType)

                val tvHeader = TextView(this).apply {
                    text = "$compBrand $compModel $compYear"
                    textSize = 24f
                    gravity = Gravity.CENTER
                    background = ContextCompat.getDrawable(
                        dialog.context ,
                        R.drawable.tb_start_bott_end_tr25
                    )
                    setTextColor(ContextCompat.getColor(dialog.context , R.color.white))
                }
                val tvHeaderParams = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(1)
                    columnSpec = GridLayout.spec(0 , 2)
                    height = 42.toPx(this@ActComponentsGeometry)
                    setGravity(Gravity.CENTER)

                }
                tvHeader.layoutParams = tvHeaderParams
                gridContainer.addView(tvHeader)

                val tvSize = TextView(this).apply {
                    text = "Size: $compSize"
                    textSize = 18f
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(ContextCompat.getColor(dialog.context , R.color.white))
                    setPadding(8 , 4 , 8 , 4)
                }
                val tvSizeParams = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(2)
                    columnSpec = GridLayout.spec(0)
                    height = 48.toPx(this@ActComponentsGeometry)

                }
                tvSize.layoutParams = tvSizeParams
                gridContainer.addView(tvSize)

                val tvWeight = TextView(this).apply {
                    text = "Weight: $compWeight"
                    textSize = 18f
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(ContextCompat.getColor(dialog.context , R.color.white))
                    setPadding(8 , 4 , 8 , 4)
                }
                val tvWeightParams = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(3)
                    columnSpec = GridLayout.spec(0)
                    height = 48.toPx(this@ActComponentsGeometry)

                }
                tvWeight.layoutParams = tvWeightParams
                gridContainer.addView(tvWeight)

                val ivPhoto = ImageView(this).apply {
                    setImageResource(photoResId)
                    scaleType =
                        ImageView.ScaleType.FIT_CENTER
                }

                val ivPhotoParams = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(2 , 2)
                    columnSpec = GridLayout.spec(1)
                    width = 150.toPx(this@ActComponentsGeometry)
                    setGravity(Gravity.CENTER)
                    height = 150.toPx(this@ActComponentsGeometry)
                    setMargins(
                        8.toPx(this@ActComponentsGeometry) ,
                        8.toPx(this@ActComponentsGeometry) ,
                        8.toPx(this@ActComponentsGeometry) ,
                        8.toPx(this@ActComponentsGeometry)
                    )
                }
                ivPhoto.layoutParams = ivPhotoParams
                gridContainer.addView(ivPhoto)


                dialog.dismiss()
            }

            dialog.show()
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
                val intent = Intent(this@ActComponentsGeometry , ActBikeGarage::class.java).apply {
                    putExtra("bike_id" , selectedBikeId)
                }
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this , R.anim.fade_in_faster , R.anim.fade_out_faster
                )
                startActivity(intent , options.toBundle())
                finish()
            }
        }
    }

    private fun loadBikeData(bikeId: Int) {
        lifecycleScope.launch {
            val bikeDao = BikeDatabase.getDatabase(this@ActComponentsGeometry).bikeDao()
            val bike = bikeDao.getBikeById(bikeId)


            bike?.let {
                bikeAndModelView.text =
                    getString(R.string.two_strings , it.brand , it.modelsJson.keys.first())
                loadBikeImage(it)
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun loadBikeImage(bike: Bike) {
        val imageName = bike.modelsJson.values.first().submodels.values.first().imageName
        imageName?.let {
            val resourceId = resources.getIdentifier(it , "drawable" , packageName)
            if (resourceId != 0) {
                val drawable = ResourcesCompat.getDrawable(resources , resourceId , null)
                bikeImageView.setImageDrawable(drawable)
            }
        }
    }

}
