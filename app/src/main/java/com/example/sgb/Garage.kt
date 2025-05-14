package com.example.sgb

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import com.example.sub.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import kotlin.math.abs

class GarageActivity : AppCompatActivity() {
    private lateinit var bikeDao: BikeDao
    private lateinit var miniRecycler: RecyclerView
    private lateinit var viewPager: ViewPager2
    private lateinit var fabAdd: FloatingActionButton

    private val bikeList = mutableListOf<Bike>()
    private lateinit var miniAdapter: MiniBikeAdapter
    private lateinit var pagerAdapter: BikePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testgarage)

        bikeDao = BikeDatabase.getDatabase(this).bikeDao()
        miniRecycler = findViewById(R.id.miniRecycler)
        viewPager = findViewById(R.id.viewPager)
        fabAdd = findViewById(R.id.fab_add_bike)

        setupMiniRecycler()
        setupViewPager()
        loadBikes()

        fabAdd.setOnClickListener {
            startActivity(Intent(this, PreAddBikeActivity::class.java))
        }
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
                if (bikeList != bikes) { // Перевіряємо, чи змінився список
                    bikeList.clear()
                    bikeList.addAll(bikes)
                    miniAdapter.submitList(bikes)
                    pagerAdapter.updateBikes(bikes)
                }
            }
        }
    }
    }

// Adapter для mini-RecyclerView
class MiniBikeAdapter(
    private val onClick: (Int, Int) -> Unit
) : RecyclerView.Adapter<MiniBikeAdapter.MiniViewHolder>() {
    private val items = mutableListOf<Bike>()
    private var selectedPos = 0

    fun submitList(newBikes: List<Bike>) {
        val diffCallback = BikeDiffCallback(items, newBikes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newBikes)
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
            .inflate(R.layout.fragment_bike_card, parent, false)
        return MiniViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiniViewHolder, position: Int) {
        val bike = items[position]
        holder.bind(bike, position == selectedPos)
        holder.itemView.setOnClickListener { onClick(bike.id, position) }
    }

    override fun getItemCount() = items.size

    class MiniViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.bike_image)
        private val title: TextView = view.findViewById(R.id.bike_name)

        fun bind(bike: Bike, selected: Boolean) {
            itemView.alpha = if (selected) 1f else 0.6f
            itemView.scaleX = if (selected) 1.1f else 1f
            itemView.scaleY = if (selected) 1.1f else 1f
            val uri = bike.addedImgBikeUri?.toUri()
            Glide.with(itemView)
                .load(uri ?: bike.modelsJson.values.first().submodels.values.first().imageRes)
                .centerCrop()
                .into(image)
            title.text = bike.brand
        }
    }

    // DiffUtil Callback для порівняння списків
    private class BikeDiffCallback(
        private val oldList: List<Bike>,
        private val newList: List<Bike>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

// Adapter для ViewPager2
class BikePagerAdapter(
    fa: FragmentActivity,
    private val bikes: MutableList<Bike> // Змінено на MutableList
) : FragmentStateAdapter(fa) {
    override fun getItemCount() = bikes.size

    override fun createFragment(position: Int) =
        BikeCardFragment.newInstance(bikes[position].id)

    fun updateBikes(newBikes: List<Bike>) {
        bikes.clear()
        bikes.addAll(newBikes)
        notifyDataSetChanged() // Тимчасово залишаємо, але з перевіркою
    }
}

// Фрагмент-картка велосипеда (без змін)
class BikeCardFragment : Fragment(R.layout.fragment_bike_card) {
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
        val size = view.findViewById<TextView>( R.id.bike_size)
        val year = view.findViewById<TextView>(R.id.bike_year)
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
            val years = bike?.modelsJson?.values?.first()?.submodels?.values?.first()?.years
            year.text = years?.keys?.first() ?: "error: data not loaded" // Отримуємо перший рік з карти

        }

        card.setOnClickListener {
            startActivity(Intent(requireContext(), ActBikeGarage::class.java).apply {
                putExtra("bike_id", bikeId)
            })
        }
    }
}