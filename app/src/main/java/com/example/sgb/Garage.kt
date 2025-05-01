package com.example.sgb

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sgb.room.Bike
import com.example.sgb.room.BikeDao
import com.example.sgb.room.BikeDatabase
import com.example.sub.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class GarageActivity : AppCompatActivity() {
    private lateinit var bikeDao: BikeDao
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BikeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.garage)

        bikeDao = BikeDatabase.getDatabase(this).bikeDao()
        recyclerView = findViewById(R.id.recycler_view)

        setupRecyclerView()
        loadBikes()

        findViewById<FloatingActionButton>(R.id.fab_add_bike).setOnClickListener {
            startActivity(Intent(this, PreAddBikeActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = BikeAdapter { bikeId ->
            val intent = Intent(this, ActBikeGarage::class.java).apply {
                putExtra("bike_id", bikeId)
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadBikes() {
        lifecycleScope.launch {
            bikeDao.getAllBikesFlow().collect { bikes ->
                adapter.submitList(bikes)
            }
        }
    }
}
class BikeAdapter(private val onClick: (Int) -> Unit) :
    ListAdapter<Bike, BikeAdapter.ViewHolder>(BikeDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.bike_name)
        val model: TextView = view.findViewById(R.id.bike_model)
        val icon: ImageView = view.findViewById(R.id.bike_image)   // ← нове поле
    }

    override fun onCreateViewHolder(parent: ViewGroup , viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bike , parent , false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bike = getItem(position)

        holder.name.text  = bike.brand
        holder.model.text = bike.modelsJson.keys.firstOrNull() ?: ""

        // 1) Якщо є URI із бази — показуємо його
        val uriString = bike.addedImgBikeUri
        if (!uriString.isNullOrEmpty()) {
            Glide.with(holder.icon.context)
                .load(uriString.toUri())
                .override(200, 200)
                .centerCrop()
                .placeholder(R.drawable.img_fork)
                .error(R.drawable.img_fork)
                .into(holder.icon)

            // 2) Інакше, якщо є imageName — шукаємо drawable і встановлюємо
        } else if(bike.addedImgBikeUri == null) {
            val imageRes = bike.modelsJson
                .values.first()
                .submodels
                .values.first()
                .imageRes
            if (imageRes != null) {
                holder.icon.setImageResource(imageRes)
            }
        } else {
            //
        }

        holder.itemView.setOnClickListener { onClick(bike.id) }
    }


    class BikeDiffCallback : DiffUtil.ItemCallback<Bike>() {
            override fun areItemsTheSame(oldItem: Bike , newItem: Bike) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Bike , newItem: Bike) =
                oldItem == newItem
        }
    }


