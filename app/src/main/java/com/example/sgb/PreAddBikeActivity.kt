package com.example.sgb

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.Bike
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.BikeGeometry
import com.example.sgb.room.BikeModel
import com.example.sgb.room.BikeSubmodel
import com.example.sgb.utils.BlurUtils
import com.example.sub.R
import kotlinx.coroutines.launch

class PreAddBikeActivity : AppCompatActivity() {

    private lateinit var brandSpinner: Spinner
    private lateinit var modelSpinner: Spinner
    private lateinit var submodelSpinner: Spinner
    private lateinit var yearSpinner: Spinner
    private lateinit var sizeSpinner: Spinner
    private val bikeData = listOf(
        Bike(
            brand = "Canyon",
            modelsJson = mapOf(
                "Sender" to BikeModel(
                    name = "Sender",
                    submodels = mapOf(
                        "CF 9.0" to BikeSubmodel(
                            name = "CF 9.0",
                            years = mapOf(
                                "2019" to listOf("S", "M", "L", "XL")
                            ),
                            imageRes = R.drawable.img_sendercf9,
                            geometry = mapOf(
                                "L" to BikeGeometry(
                                    bikeId = 0, // Заповниться після збереження в Room
                                    wheelBase = 1200,
                                    reach = 450,
                                    stack = 610,
                                    bottomBracketOffset = 25,
                                    standOverHeight = 770,
                                    headTubeLength = 110,
                                    seatTubeLength = 450,
                                    topTubeLength = 590,
                                    seatHeight = 750,
                                    seatTubeAngle = 72,
                                    headTubeAngle = 63,
                                    chainstayLength = 435,
                                    bodyHeight = 1700,
                                    wheelSize = 29
                                )
                            )
                        ),
                        "CFR" to BikeSubmodel(
                            name = "CFR",
                            years = mapOf(
                                "2024" to listOf("M", "L", "XL"),
                                "2023" to listOf("S", "M", "L")
                            ),
                            imageRes = R.drawable.img_canyonsendercfr,
                            geometry = mapOf(
                                "M" to BikeGeometry(
                                    bikeId = 1,
                                    wheelBase = 1185,
                                    reach = 440,
                                    stack = 600,
                                    bottomBracketOffset = 20,
                                    standOverHeight = 750,
                                    headTubeLength = 105,
                                    seatTubeLength = 440,
                                    topTubeLength = 570,
                                    seatHeight = 730,
                                    seatTubeAngle = 73,
                                    headTubeAngle = 64,
                                    chainstayLength = 430,
                                    bodyHeight = 1650,
                                    wheelSize = 29
                                )
                            )
                        ),
                        "CF 8.0" to BikeSubmodel(
                            name = "CF 8.0",
                            years = mapOf(
                                "2019" to listOf("S", "M", "L", "XL")
                            ),
                            imageRes =  R.drawable.img_sendercf8,
                            geometry = mapOf(
                                "S" to BikeGeometry(
                                    bikeId = 2,
                                    wheelBase = 1160,
                                    reach = 420,
                                    stack = 590,
                                    bottomBracketOffset = 15,
                                    standOverHeight = 740,
                                    headTubeLength = 100,
                                    seatTubeLength = 430,
                                    topTubeLength = 550,
                                    seatHeight = 710,
                                    seatTubeAngle = 73,
                                    headTubeAngle = 65,
                                    chainstayLength = 425,
                                    bodyHeight = 1600,
                                    wheelSize = 27
                                )
                            )
                        )
                    )
                )
            )
        ),
        Bike(
            brand = "Propain",
            modelsJson = mapOf(
                "Rage 3" to BikeModel(
                    name = "Rage 3",
                    submodels = mapOf(
                        "CF" to BikeSubmodel(
                            name = "CF",
                            years = mapOf(
                                "2024" to listOf("S", "M", "L", "XL")
                            ),
                            imageRes = R.drawable.img_propainrage3cflim,
                            geometry = mapOf(
                                "XL" to BikeGeometry(
                                    bikeId = 3,
                                    wheelBase = 1230,
                                    reach = 470,
                                    stack = 630,
                                    bottomBracketOffset = 30,
                                    standOverHeight = 780,
                                    headTubeLength = 115,
                                    seatTubeLength = 480,
                                    topTubeLength = 620,
                                    seatHeight = 790,
                                    seatTubeAngle = 71,
                                    headTubeAngle = 62,
                                    chainstayLength = 445,
                                    bodyHeight = 1750,
                                    wheelSize = 29
                                )
                            )
                        )
                    )
                )
            )
        )
    )
    private var selectedBrand: String? = null
    private var selectedModel: String? = null
    private var selectedSubmodel: String? = null
    private var selectedYear: String? = null
    private var selectedSize: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawableResource(R.drawable.bg_bikegarageact)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kt_add_bike)

        animationRoot()
        brandSpinner = findViewById(R.id.brand_spinner)
        modelSpinner = findViewById(R.id.model_spinner)
        submodelSpinner = findViewById(R.id.submodel_spinner)
        yearSpinner = findViewById(R.id.year_spinner)
        sizeSpinner = findViewById(R.id.size_spinner)
        val imageView = findViewById<ImageView>(R.id.blur_overlay)
// Статичне розмиття з тією ж константою
        BlurUtils.applyBlur(imageView , BlurUtils.BLUR_RADIUS)
        val brands = bikeData.map { it.brand }

// Оновлення спінера
        updateSpinner(brandSpinner, brands)
        brandSpinner.onItemSelectedListener = createItemSelectedListener { brand ->
            selectedBrand = brand
            val models = bikeData.firstOrNull { it.brand == selectedBrand }?.modelsJson?.keys?.toList() ?: emptyList()
            updateSpinner(modelSpinner, models)
        }
        modelSpinner.onItemSelectedListener = createItemSelectedListener { model ->
            selectedModel = model
            val submodels = bikeData.firstOrNull { it.brand == selectedBrand }
                ?.modelsJson?.get(selectedModel)?.submodels?.keys?.toList() ?: emptyList()
            updateSpinner(submodelSpinner, submodels)
        }
        submodelSpinner.onItemSelectedListener = createItemSelectedListener { submodel ->
            selectedSubmodel = submodel
            val years = bikeData.firstOrNull { it.brand == selectedBrand }
                ?.modelsJson?.get(selectedModel)?.submodels?.get(selectedSubmodel)?.years?.keys?.toList()
                ?: emptyList()
            updateSpinner(yearSpinner, years)
        }
        yearSpinner.onItemSelectedListener = createItemSelectedListener { year ->
            selectedYear = year
            val sizes = bikeData.firstOrNull { it.brand == selectedBrand }
                ?.modelsJson?.get(selectedModel)?.submodels?.get(selectedSubmodel)?.years?.get(selectedYear)
                ?: emptyList()
            updateSpinner(sizeSpinner, sizes)
        }
        sizeSpinner.onItemSelectedListener = createItemSelectedListener { size ->
            selectedSize = size
        }
        findViewById<View>(R.id.confirm_selection).setOnClickListener {
            if (selectedBrand != null && selectedModel != null && selectedSubmodel != null &&
                selectedYear != null && selectedSize != null
            ) {
                saveBike()
            }
        }
        val manualAddingBike = findViewById<Button>(R.id.make_own_bike)
        manualAddingBike.setOnClickListener{
            val intent = Intent(this , PreAddOwnBike::class.java)
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this , R.anim.fade_in_faster , R.anim.fade_out_faster
        )
        startActivity(intent , options.toBundle())
            finish()
        }



    }
    private fun updateSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(this, R.layout.s_spinner, R.id.spinner_item_text, items)
        adapter.setDropDownViewResource(R.layout.s_dropped_spinner)
        spinner.adapter = adapter
    }

    private fun createItemSelectedListener(onItemSelected: (String) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val item = parent.getItemAtPosition(position).toString()
                onItemSelected(item)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


    private fun saveBike() {
        if (selectedBrand != null && selectedModel != null && selectedSubmodel != null &&
            selectedYear != null && selectedSize != null
        ) {
            val imageName = bikeData.firstOrNull { it.brand == selectedBrand }
                ?.modelsJson?.get(selectedModel)?.submodels?.get(selectedSubmodel)?.imageRes

            val bike = Bike(
                brand = selectedBrand!!,
                modelsJson = mapOf(
                    selectedModel!! to BikeModel(
                        name = selectedModel!!,
                        submodels = mapOf(
                            selectedSubmodel!! to BikeSubmodel(
                                imageRes = imageName,
                                name = selectedSubmodel!!,
                                years = mapOf(
                                    selectedYear!! to listOf(selectedSize!!)
                                )
                            )
                        )
                    )
                ),
                selectedSize = selectedSize
            )

            lifecycleScope.launch {
                val bikeDao = BikeDatabase.getDatabase(this@PreAddBikeActivity).bikeDao()
                val bikeId = bikeDao.insertBike(bike).toInt()
                saveBikeGeometry(bikeId)

                runOnUiThread {
                    startExitAnimations {
                        val intent = Intent(this@PreAddBikeActivity, ActBikeGarage::class.java).apply {
                            putExtra("bike_id", bikeId)
                        }
                        startActivity(intent, ActivityOptionsCompat.makeCustomAnimation(
                            this@PreAddBikeActivity,
                            0,
                            1
                        ).toBundle())
                        finish()
                    }
                }
            }
        }
    }

    private fun saveBikeGeometry(bikeId: Int) {
        // Отримуємо геометрію для вибраного розміру
        val geometry = bikeData.firstOrNull { it.brand == selectedBrand }
            ?.modelsJson?.get(selectedModel)
            ?.submodels?.get(selectedSubmodel)
            ?.geometry?.get(selectedSize)

        if (geometry != null) {
            // Зберігаємо геометрію в базу даних
            val geometryToSave = geometry.copy(bikeId = bikeId) // Прив'язуємо bikeId
            lifecycleScope.launch {
                val geometryDao = BikeDatabase.getDatabase(this@PreAddBikeActivity).geometryDao()
                val geometryId = geometryDao.insertGeometry(geometryToSave).toInt()
                Log.d("GeometryID", "Inserted geometry with ID: $geometryId")
            }
        } else {
            Log.w("SaveGeometry", "Geometry not found for selected size: $selectedSize")
        }
    }
    private fun animationRoot() {
        val root = findViewById<ConstraintLayout>(R.id.root)

        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)

            // кожного разу новий екземпляр анімації
            val fadeOut = AnimationUtils.loadAnimation(this , R.anim.fade_innormal)

            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationRepeat(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    child.visibility = View.VISIBLE
                }
            })

            child.startAnimation(fadeOut)
        }
    }
    private fun startExitAnimations(onAnimationsEnd: () -> Unit) {
        val root = findViewById<ConstraintLayout>(R.id.root)
        val rootparent = findViewById<ConstraintLayout>(R.id.rootparent)
        val blurOverlay = findViewById<ImageView>(R.id.blur_overlay)
        val expandingBlock = findViewById<View>(R.id.block)

        // Перша анімація — fade out кореневого контейнера
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_outnormal).apply {
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationRepeat(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    root.visibility = View.GONE

                    // Переключаємо blurOverlay на hardware layer, задаємо pivot
                    blurOverlay.apply {
                        visibility = View.VISIBLE
                        // Pivot у лівому верхньому куті
                        pivotX = 0f
                        pivotY = 0f
                        // hardware layer для гладкого масштабування
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    }
                    // ValueAnimator для scaleX
                    ValueAnimator.ofFloat(1f, 0f).apply {
                        duration = 2000
                        addUpdateListener { animator ->
                            blurOverlay.scaleX = animator.animatedValue as Float
                        }
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                // повертаємо все назад
                                blurOverlay.visibility = View.INVISIBLE
                                blurOverlay.scaleX = 1f
                                blurOverlay.setLayerType(View.LAYER_TYPE_NONE, null)

                                // прибираємо padding із rootparent
                                rootparent.setPadding(0, 0, 0, 0)

                                // та стартуємо розширення блоку…
                                expandingBlock.visibility = View.VISIBLE
                                val targetHeight = (428 * resources.displayMetrics.density).toInt()
                                ValueAnimator.ofInt(1, targetHeight).apply {
                                    duration = 3000
                                    addUpdateListener { anim ->
                                        expandingBlock.layoutParams.height = anim.animatedValue as Int
                                        expandingBlock.requestLayout()
                                    }
                                    addListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {
                                            onAnimationsEnd()
                                        }
                                    })
                                    start()
                                }
                            }
                        })
                        start()
                    }
                }
            })
        }
        root.startAnimation(fadeOut)
    }

}
