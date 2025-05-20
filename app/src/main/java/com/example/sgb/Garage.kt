package com.example.sgb

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.sgb.room.Bike
import com.example.sgb.room.BikeDao
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.Component
import com.example.sgb.room.ComponentsDao
import com.example.sgb.utils.BlurUtils
import com.example.sub.R
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.launch
import kotlin.math.abs

class GarageActivity : AppCompatActivity() {
    private lateinit var bikeDao: BikeDao
    private lateinit var miniRecycler: RecyclerView
    private lateinit var viewPager: ViewPager2
    private lateinit var fabAdd: AppCompatImageButton
    private lateinit var compomentDao: ComponentsDao
    private lateinit var appbar: AppBarLayout
    private val bikeList = mutableListOf<Bike>()
    private lateinit var miniAdapter: MiniBikeAdapter
    private lateinit var pagerAdapter: BikePagerAdapter
    private var isAppBarExpanded = false
    private lateinit var gestureDetector: GestureDetector
    private val offsetDp = 40 // Зміщення в dp

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.garage_bg)

        bikeDao = BikeDatabase.getDatabase(this).bikeDao()
        miniRecycler = findViewById(R.id.miniRecycler)
        viewPager = findViewById(R.id.viewPager)
        fabAdd = findViewById(R.id.fab_add_bike)
        compomentDao = BikeDatabase.getDatabase(this).componentsDao()
        appbar = findViewById(R.id.appbar)

        val imageView = findViewById<ImageView>(R.id.blur_overlay)
        BlurUtils.applyBlur(imageView, BlurUtils.BLUR_RADIUS50)

        // Початково згортаємо AppBar
        appbar.setExpanded(false, false)
        Log.d("GarageActivity", "AppBar collapsed on start")
        setupMiniRecycler()
        setupViewPager()
        loadBikes()

        fabAdd.setOnClickListener {
            startActivity(Intent(this, PreAddBikeActivity::class.java))
        }

        // Тестування AppBar через scrolldown
        val scrollDownButton = findViewById<ImageView>(R.id.scrolldown)
        scrollDownButton.setOnClickListener {
            appbar.setExpanded(!isAppBarExpanded, true)
            Log.d("GarageActivity", "Scroll down clicked, isExpanded: $isAppBarExpanded")
        }

        // Ініціалізація GestureDetector
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val deltaY = e2.y - e1.y
                Log.d("GestureDetector", "DeltaY: $deltaY, VelocityY: $velocityY")
                if (abs(deltaY) > 350 && abs(velocityY) > 350) {
                    if (deltaY > 0) { // Свайп вниз
                        if (!isAppBarExpanded) {
                            appbar.setExpanded(true, true)
                        } else {
                            appbar.setExpanded(false, true)
                        }
                    } else { // Свайп вгору
                        Log.d("GestureDetector", "Swipe up detected, collapsing AppBar")
                    }
                    return true
                }
                return false
            }
        })

        // Додаємо GestureDetector до gesture_overlay і gesture_overlay1
        val gestureOverlay = findViewById<View>(R.id.gesture_overlay)
        val gestureOverlay1 = findViewById<View>(R.id.gesture_overlay1)
        gestureOverlay.setOnTouchListener { _, event ->
            Log.d("GestureDetector", "Overlay touch event: ${event.action}")
            gestureDetector.onTouchEvent(event)
            true // Споживаємо події, щоб уникнути передачі до ViewPager2
        }
        gestureOverlay1.setOnTouchListener { _, event ->
            Log.d("GestureDetector", "Overlay touch event: ${event.action}")
            gestureDetector.onTouchEvent(event)
            true // Споживаємо події, щоб уникнути передачі до ViewPager2
        }

        // Відстежуємо стан AppBar і анімуємо ViewPager2
        appbar.addOnOffsetChangedListener { _, verticalOffset ->
            isAppBarExpanded = verticalOffset == 0
            Log.d("GarageActivity", "AppBar offset: $verticalOffset, isExpanded: $isAppBarExpanded")
            val offsetPx = if (isAppBarExpanded) dpToPx(offsetDp) else 0f
            ObjectAnimator.ofFloat(viewPager, "translationY", offsetPx).apply {
                duration = 1100 // Тривалість анімації в мілісекундах
                start()
            }
        }
    }

    // Функція для перетворення dp у px
    private fun dpToPx(dp: Int): Float {
        return dp * resources.displayMetrics.density
    }

    private fun setupMiniRecycler() {
        miniAdapter = MiniBikeAdapter { _, position ->
            viewPager.setCurrentItem(position, true)
        }
        miniRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        miniRecycler.adapter = miniAdapter
    }

    private fun setupViewPager() {
        pagerAdapter = BikePagerAdapter(this, bikeList)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 2

        val transformer = ViewPager2.PageTransformer { page, position ->
            val absPos = abs(position)
            page.apply {
                val scale = 1f - 0.40f * absPos
                scaleX = scale
                scaleY = scale
                translationX = -position * width * 0.35f
                alpha = 1f - 0.3f * absPos
            }
        }
        viewPager.setPageTransformer(transformer)

        val recyclerView = viewPager.getChildAt(0) as RecyclerView
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                miniRecycler.smoothScrollToPosition(position)
                miniAdapter.selectPosition(position)
            }
        })
    }

    private fun loadBikes() {
        lifecycleScope.launch {
            bikeDao.getAllBikesFlow().collect { bikes ->
                val bikeComponentPairs = bikes.map { bike ->
                    val components = compomentDao.getComponentsByBikeId(bike.id)
                    val shockComp = components.find { it.compType == "Shock" }
                    val forkComp = components.find { it.compType == "Fork" }
                    BikeWithComponents(bike, shockComp, forkComp)
                }
                miniAdapter.submitList(bikeComponentPairs)
                pagerAdapter.updateBikes(bikes)
            }
        }
    }
}

// Решта коду (MiniBikeAdapter, BikePagerAdapter, BikeCardFragment) залишається без змін
class MiniBikeAdapter(
    private val onClick: (Int, Int) -> Unit
) : RecyclerView.Adapter<MiniBikeAdapter.MiniViewHolder>() {
    private val items = mutableListOf<BikeWithComponents>()
    private var selectedPos = 0

    fun submitList(newItems: List<BikeWithComponents>) {
        val diffCallback = BikeDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    fun selectPosition(pos: Int) {
        val old = selectedPos
        selectedPos = pos
        notifyItemChanged(old)
        notifyItemChanged(pos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.garage_apperadapter, parent, false)
        return MiniViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiniViewHolder, position: Int) {
        val (bike, shockComp, forkComp) = items[position]
        holder.bind(bike, shockComp, forkComp, position == selectedPos)
        holder.itemView.setOnClickListener { onClick(bike.id, position) }
    }

    override fun getItemCount() = items.size

    class MiniViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.bike_image)
        private val name: TextView = view.findViewById(R.id.bike_name)
        private val model: TextView = view.findViewById(R.id.bike_model)
        private val number: TextView = view.findViewById(R.id.bike_number)
        private val hours: TextView = view.findViewById(R.id.bike_hours)
        private val year: TextView = view.findViewById(R.id.bike_year)
        private val size: TextView = view.findViewById(R.id.bike_size)
        private val fork: TextView = view.findViewById(R.id.bike_fork)
        private val shock: TextView = view.findViewById(R.id.bike_shock)
        private val lavelHours: TextView = view.findViewById(R.id.labelHours)

        @SuppressLint("SetTextI18n")
        fun bind(bike: Bike, shockComp: Component?, forkComp: Component?, selected: Boolean) {
            itemView.alpha = if (selected) 1f else 0.6f
            itemView.scaleX = if (selected) 1.1f else 1f
            itemView.scaleY = if (selected) 1.1f else 1f
            val forkBrand = forkComp?.compBrand
            val uri = bike.addedImgBikeUri?.toUri()
            Glide.with(itemView)
                .load(uri ?: bike.modelsJson.values.first().submodels.values.first().imageRes)
                .fitCenter()
                .into(image)

            name.text = bike.brand
            model.text = bike.modelsJson.keys.firstOrNull() ?: "error"
            number.text = "ID: ${bike.id}"
            if (bike.elapsedHoursValue != 0) {
                hours.text = "${bike.elapsedHoursValue}"
            } else {
                lavelHours.visibility = View.GONE
                hours.visibility = View.GONE
            }

            val years = bike.modelsJson.values.first().submodels.values.first().years
            year.text = years.keys.first()
            size.text = "Розмір: ${bike.selectedSize ?: "error"}"
            if (shockComp?.compBrand != null) {
                shock.text = "Аморт: ${shockComp.compBrand} ${shockComp.compModel}"
            } else {
                shock.visibility = View.GONE
            }
            if (forkBrand != null) {
                fork.text = "Вилка: ${forkComp.compBrand} ${forkComp.compModel}"
            } else {
                fork.visibility = View.GONE
            }
        }
    }

    private class BikeDiffCallback(
        private val oldList: List<BikeWithComponents>,
        private val newList: List<BikeWithComponents>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].bike.id == newList[newItemPosition].bike.id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

data class BikeWithComponents(
    val bike: Bike,
    val shockComp: Component?,
    val forkComp: Component?
)

class BikePagerAdapter(
    fa: FragmentActivity,
    private val bikes: MutableList<Bike>
) : FragmentStateAdapter(fa) {
    override fun getItemCount() = bikes.size

    override fun createFragment(position: Int) =
        BikeCardFragment.newInstance(bikes[position].id)

    @SuppressLint("NotifyDataSetChanged")
    fun updateBikes(newBikes: List<Bike>) {
        bikes.clear()
        bikes.addAll(newBikes)
        notifyDataSetChanged()
    }
}

class BikeCardFragment : Fragment(R.layout.garage_bike_card) {
    companion object {
        private const val ARG_BIKE_ID = "bike_id"
        fun newInstance(bikeId: Int) = BikeCardFragment().apply {
            arguments = Bundle().apply { putInt(ARG_BIKE_ID, bikeId) }
        }
    }

    private var bikeId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bikeId = requireArguments().getInt(ARG_BIKE_ID)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val card = view.findViewById<View>(R.id.card)
        val image = view.findViewById<ImageView>(R.id.bike_image)
        val title = view.findViewById<TextView>(R.id.bike_name)
        val subtitle = view.findViewById<TextView>(R.id.bike_model)
        val size = view.findViewById<TextView>(R.id.bike_size)
        lifecycleScope.launch {
            val bike = BikeDatabase.getDatabase(requireContext()).bikeDao().getBikeById(bikeId)
            val uri = bike?.addedImgBikeUri?.toUri()
            Glide.with(this@BikeCardFragment)
                .load(uri ?: bike?.modelsJson?.values?.first()?.submodels?.values?.first()?.imageRes)
                .fitCenter()
                .into(image)

            title.text = bike?.brand ?: "error: data not loaded"
            subtitle.text = bike?.modelsJson?.keys?.firstOrNull() ?: "error: data not loaded"
            size.text = bike?.selectedSize ?: "error: data not loaded"
        }

        card.setOnClickListener {
            startActivity(Intent(requireContext(), ActBikeGarage::class.java).apply {
                putExtra("bike_id", bikeId)
            })
        }
    }
}