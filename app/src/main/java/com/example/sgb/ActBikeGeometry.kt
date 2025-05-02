package com.example.sgb

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.sgb.room.Bike
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.BikeGeometry
import com.example.sub.R
import kotlinx.coroutines.launch

class ActBikeGeometry : AppCompatActivity() {
    private lateinit var selectedSize: TextView
    private lateinit var bikeName: TextView
    private lateinit var bikePhoto: ImageView
    private lateinit var wheelBaseTextView: TextView
    private lateinit var reachTextView: TextView
    private lateinit var stackTextView: TextView
    private lateinit var bottomBracketOffsetTextView: TextView
    private lateinit var standOverHeightTextView: TextView
    private lateinit var headTubeLengthTextView: TextView
    private lateinit var seatTubeLengthTextView: TextView
    private lateinit var topTubeLengthTextView: TextView
    private lateinit var seatHeightTextView: TextView
    private lateinit var seatTubeAngle: TextView
    private lateinit var headTubeAngleTextView: TextView
    private lateinit var chainstayLengthTextView: TextView
    private lateinit var bodyHeightTextView: TextView
    private lateinit var wheelSizeTextView: TextView

    @SuppressLint("DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_bikegeometry)


        val backButton: Button = findViewById(R.id.back)
        val sharedPreferences: SharedPreferences = getSharedPreferences("bike_prefs", MODE_PRIVATE)
        val selectedBikeId = sharedPreferences.getInt("selected_bike_id", -1)

        backButton.setOnClickListener {
                val intent = Intent(this@ActBikeGeometry, ActBikeGarage::class.java)
                intent.putExtra("bike_id", selectedBikeId)
                intent.putExtra("Taked focus", "Yes")
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.fade_in_faster, R.anim.fade_out_faster
                )
                startActivity(intent, options.toBundle())
                finish()
        }


        // Прив'язуємо TextView для кожного параметра
        selectedSize = findViewById(R.id.size)
        bikeName = findViewById(R.id.brand_model)
        bikePhoto = findViewById(R.id.bike_photo)
        wheelBaseTextView = findViewById(R.id.wheel_base)
        reachTextView = findViewById(R.id.reach)
        stackTextView = findViewById(R.id.stack)
        bottomBracketOffsetTextView = findViewById(R.id.bottom_bracket_offset)
        standOverHeightTextView = findViewById(R.id.stand_over_height)
        headTubeLengthTextView = findViewById(R.id.head_tube_length)
        seatTubeLengthTextView = findViewById(R.id.seat_tube_length)
        topTubeLengthTextView = findViewById(R.id.top_tube_length)
        seatHeightTextView = findViewById(R.id.seat_height)
        seatTubeAngle = findViewById(R.id.seat_tube_angle)
        headTubeAngleTextView = findViewById(R.id.head_tube_angle)
        chainstayLengthTextView = findViewById(R.id.chainstay_length)
        bodyHeightTextView = findViewById(R.id.body_height)
        wheelSizeTextView = findViewById(R.id.wheel_size)



        // Отримуємо bikeId із Intent
        val bikeId = intent.getIntExtra("bike_id", -1)
        lifecycleScope.launch {
            val database = BikeDatabase.getDatabase(this@ActBikeGeometry)
            val geometryDao = database.geometryDao()
            val bikeDao = database.bikeDao()
            val bike = bikeDao.getBikeById(bikeId)

            val sizeSeleceted = bikeDao.getBikeById(bikeId)
            sizeSeleceted?.let {updateBikeMainInfTextView(it)}




            // 1) Якщо є URI із бази — показуємо його
            val uriString = bike?.addedImgBikeUri
            if (!uriString.isNullOrEmpty()) {
                Glide.with(bikePhoto.context)
                    .load(uriString.toUri())
                    .override(200, 200)
                    .centerCrop()
                    .placeholder(R.drawable.img_fork)
                    .error(R.drawable.img_fork)
                    .into(bikePhoto)

                // 2) Інакше, якщо є imageName — шукаємо drawable і встановлюємо
            } else if (bike != null) {
                if(bike.addedImgBikeUri == null) {
                    val imageRes = bike.modelsJson
                        .values.first()
                        .submodels
                        .values.first()
                        .imageRes
                    if (imageRes != null) {
                        bikePhoto.setImageResource(imageRes)
                    }
                } else {
                    Toast.makeText(this@ActBikeGeometry, "Troble with bike photo(dev)",  Toast.LENGTH_SHORT).show()
                }
            }

            // Отримуємо геометрію для конкретного bikeId
            val geometry = geometryDao.getGeometryByBikeId(bikeId)
            geometry?.let { updateGeometryTextViews(it) }
        }
    }

    private fun updateBikeMainInfTextView(bike: Bike){
        selectedSize.text = getString(R.string.selected_size_label, bike.selectedSize)
        bikeName.text = getString(R.string.two_strings, bike.brand, bike.modelsJson.keys.first())
    }

    private fun updateGeometryTextViews(geometry: BikeGeometry) {
        // Оновлюємо TextView відповідно до отриманих даних
        wheelBaseTextView.text = getString(R.string.wheel_base_label, geometry.wheelBase)
        reachTextView.text = getString(R.string.reach_label, geometry.reach)
        stackTextView.text = getString(R.string.stack_label, geometry.stack)
        bottomBracketOffsetTextView.text = getString(R.string.bottom_bracket_offset_label, geometry.bottomBracketOffset)
        standOverHeightTextView.text = getString(R.string.stand_over_height_label, geometry.standOverHeight)
        headTubeLengthTextView.text = getString(R.string.head_tube_length_label, geometry.headTubeLength)
        seatTubeAngle.text = getString(R.string.seat_tube_angle_label, geometry.seatTubeAngle)
        seatTubeLengthTextView.text = getString(R.string.seat_tube_length_label, geometry.seatTubeLength)
        topTubeLengthTextView.text = getString(R.string.top_tube_length_label, geometry.topTubeLength)
        seatHeightTextView.text = getString(R.string.seat_height_label, geometry.seatHeight)
        headTubeAngleTextView.text = getString(R.string.head_tube_angle_label, geometry.headTubeAngle)
        chainstayLengthTextView.text = getString(R.string.chainstay_length_label, geometry.chainstayLength)
        bodyHeightTextView.text = getString(R.string.body_height_label, geometry.bodyHeight)
        wheelSizeTextView.text = getString(R.string.wheel_size_label, geometry.wheelSize)
    }
}