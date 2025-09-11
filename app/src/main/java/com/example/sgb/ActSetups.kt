package com.example.sgb

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.example.sgb.room.BikeDatabase
import com.example.sgb.room.MaketSetupDao
import com.example.sgb.room.MarksForSetup
import com.example.sgb.room.SetupData
import com.example.sub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sgb.adapters.SetupAdapter
import com.example.sgb.adapters.SetupItem
import androidx.core.content.edit

@Suppress("DEPRECATION")
class ActSetups : AppCompatActivity() {
    private lateinit var bikeNameTextView: TextView
    private var isDeleteMode = false
    private lateinit var bpSetupDao: MaketSetupDao
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: SetupAdapter
    private lateinit var prefs: SharedPreferences
    private var originalOrderIds: List<Int> = emptyList()
    private var currentBikeId: Int = -1

    private lateinit var burgerMenuButton: Button
    private var initialBikeId: Int = -1

    // поле для ItemTouchHelper
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

            setContentView(R.layout.kt_setups)
        prefs = getSharedPreferences("setup_orders", MODE_PRIVATE)

        recycler = findViewById(R.id.setups_recycler)
        recycler.layoutManager = LinearLayoutManager(this)

        // cancel button
        val cancelBtn = findViewById<Button>(R.id.cancel_changes)
        cancelBtn.setOnClickListener {
            // скасувати будь-які незбережені зміни: якщо reorder — відновити початковий порядок; якщо delete — просто вийти з режиму без видалення
            if (isReorderMode) {
                exitReorderMode(save = false)
            } else if (isDeleteMode) {
                exitDeleteMode()
            }
        }

        bikeNameTextView = findViewById(R.id.bike_name)
        val bikeId = intent.getIntExtra("bike_id", -1)

        // Ініціалізація бази даних
        val db = BikeDatabase.getDatabase(this)
        bpSetupDao = db.maketSetupDao()

        findViewById<Button>(R.id.back).setOnClickListener { navigateToBikeGarage(bikeId) }
        burgerMenuButton = findViewById(R.id.burger_menu)
        initialBikeId = bikeId
        setupAlertDialog(burgerMenuButton, bikeId)


        if (bikeId != -1) {
            loadBikeData(bikeId)
            loadExistingSetups(bikeId)
        }
    }
    // додаємо два стани
    private var isReorderMode = false

    // ItemTouchHelper.Callback
    private inner class DragCallback : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            adapter.moveItem(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // swipe disabled
        }

        override fun isLongPressDragEnabled(): Boolean {
            // дозволяємо drag тільки коли в режимі reorder
            return isReorderMode
        }
    }
    // helper для order persistence
    private fun saveOrderToPrefs(bikeId: Int, order: List<Int>) {
        val str = order.joinToString(",")
        prefs.edit { putString("order_bike_$bikeId" , str) }
    }

    private fun loadOrderFromPrefs(bikeId: Int): List<Int>? {
        val str = prefs.getString("order_bike_$bikeId", null) ?: return null
        if (str.isEmpty()) return emptyList()
        return str.split(",").mapNotNull { it.toIntOrNull() }
    }

    // loadExistingSetups — переписаний для RecyclerView
    private fun loadExistingSetups(bikeId: Int) {
        lifecycleScope.launch {
            val setupDao = BikeDatabase.getDatabase(this@ActSetups).setupDao()
            val maketDao = BikeDatabase.getDatabase(this@ActSetups).maketSetupDao()

            val setupsWithData: List<Pair<MarksForSetup, SetupData?>> = withContext(Dispatchers.IO) {
                val setups = setupDao.getSetupsByBikeIdOrdered(bikeId)
                setups.map { marks ->
                    val data = maketDao.getSetupBySetupId(marks.id)
                    marks to data
                }
            }

            withContext(Dispatchers.Main) {
                // збираємо SetupItem
                val items = setupsWithData.map { (marks, data) -> SetupItem(marks , data) }.toMutableList()

                // порядок: застосуємо збережений порядок якщо є
                val saved = loadOrderFromPrefs(bikeId)
                if (!saved.isNullOrEmpty()) {
                    // reorder items according to saved list
                    val map = items.associateBy { it.marks.id }
                    val ordered = saved.mapNotNull { map[it] }.toMutableList()
                    // додамо ті, що лишилися
                    val remaining = items.filter { it.marks.id !in saved }
                    ordered.addAll(remaining)
                    adapter = SetupAdapter(ordered, { openOnAdapter(it) }, { onItemToggled() })
                } else {
                    adapter = SetupAdapter(items, { openOnAdapter(it) }, { onItemToggled() })
                    // зберегти поточний (default) порядок для подальшого редагування
                    originalOrderIds = adapter.getCurrentOrderIds()
                    saveOrderToPrefs(bikeId, originalOrderIds)
                }

                recycler.adapter = adapter

                // attach ItemTouchHelper
                val callback = DragCallback()
                itemTouchHelper = ItemTouchHelper(callback)
                itemTouchHelper?.attachToRecyclerView(recycler)

                // збережемо початковий порядок
                originalOrderIds = adapter.getCurrentOrderIds()
                currentBikeId = bikeId
            }
        }
    }
    // виклик відкриття з адаптера
    private fun openOnAdapter(item: SetupItem) {
        openSetup(item.marks.setupName, item.marks.bikeId, item.marks.id)
    }

    private fun onItemToggled() {}

    // --- Режими ---
    private fun enterDeleteMode(burgerBtn: Button) {
        isDeleteMode = true
        adapter.isDeleteMode = true
        // змінюємо іконку burger_menu на img_delete (припускаю що в тебе drawable img_delete)
        burgerBtn.setBackgroundResource(R.drawable.img_delete)
        findViewById<Button>(R.id.cancel_changes).visibility = View.VISIBLE
    }

    private fun exitDeleteMode() {
        isDeleteMode = false
        adapter.isDeleteMode = false
        // повертаємо іконку назад
        burgerMenuButton.setBackgroundResource(R.drawable.btn_burger)
        findViewById<Button>(R.id.cancel_changes).visibility = View.GONE
        // зняти виділення з усіх елементів
        adapter.clearSelection()

        // Відновлюємо первинну поведінку burgerMenu (щоб знову відкривалось меню)
        setupAlertDialog(burgerMenuButton, initialBikeId)
    }


    private fun confirmDeleteSelected() {
        // збираємо selected
        val toDelete = adapter.removeSelected()
        if (toDelete.isEmpty()) {
            // нічого не видаляти — просто вийдемо з режиму
            exitDeleteMode()
            return
        }
        // видалити з БД
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val setupDao = BikeDatabase.getDatabase(this@ActSetups).setupDao()
                val maketDao = BikeDatabase.getDatabase(this@ActSetups).maketSetupDao()
                toDelete.forEach { si ->
                    setupDao.deleteSetup(si.marks)
                    try {
                        maketDao.deleteBySetupId(si.marks.id)
                    } catch (_: Exception) {
                        val row = maketDao.getSetupBySetupId(si.marks.id)
                        row?.let { maketDao.deleteSetupById(it.id) }
                    }
                }
            }
            // після видалення з БД — оновлюємо prefs порядок
            saveOrderToPrefs(currentBikeId, adapter.getCurrentOrderIds())
            // виходимо з режиму
            exitDeleteMode()
        }
    }

    private fun enterReorderMode(burgerBtn: Button) {
        isReorderMode = true
        adapter.isReorderMode = true
        burgerBtn.setBackgroundResource(R.drawable.img_edit)
        findViewById<Button>(R.id.cancel_changes).visibility = View.VISIBLE
        // itemTouchHelper дозволить drag (isLongPressDragEnabled контролює LongPress)
        // Можна починати drag і через хендлер, але достатньо тримання елементу
    }

    private fun exitReorderMode(save: Boolean) {
        isReorderMode = false
        adapter.isReorderMode = false
        findViewById<Button>(R.id.burger_menu).setBackgroundResource(R.drawable.btn_burger)
        findViewById<Button>(R.id.cancel_changes).visibility = View.GONE
        setupAlertDialog(burgerMenuButton, initialBikeId)
        if (save) {
            // зберегти поточний порядок
            val newOrder = adapter.getCurrentOrderIds()
            saveOrderToPrefs(currentBikeId, newOrder)
            originalOrderIds = newOrder
        } else {
            // відкотити зміни до originalOrderIds
            // переставимо adapter items згідно originalOrderIds
            val map = adapter.items.associateBy { it.marks.id }
            val restored = originalOrderIds.mapNotNull { map[it] }.toMutableList()
            // додамо ті, що з'явилися нові (якщо такі є)
            val remaining = adapter.items.filter { it.marks.id !in originalOrderIds }
            restored.addAll(remaining)
            adapter.replaceItems(restored)
        }
    }


    private fun navigateToBikeGarage(bikeId: Int) {
        val intent = Intent(this, ActBikeGarage::class.java).apply {
            putExtra("bike_id", bikeId)
        }
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this, R.anim.fade_in_faster, R.anim.fade_out_faster
        )
        startActivity(intent, options.toBundle())
        finish()
    }

    private fun loadBikeData(bikeId: Int) {
        lifecycleScope.launch {
            val bikeDao = BikeDatabase.getDatabase(this@ActSetups).bikeDao()
            bikeDao.getBikeById(bikeId)?.let { bike ->
                saveSelectedBikeId(bikeId)
                bikeNameTextView.text = getString(R.string.two_strings, bike.brand, bike.modelsJson.keys.first())
            }
        }
    }

    private fun saveSelectedBikeId(bikeId: Int) {
        getSharedPreferences("bike_prefs", MODE_PRIVATE).edit().apply {
            putInt("selected_bike_id", bikeId)
            apply()
        }
    }


    private fun setupAlertDialog(burgerMenuButton: Button, bikeId: Int) {
        val items = arrayOf("Add Setup", "Delete setuo", "Reorder")

        burgerMenuButton.setOnClickListener {
            AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
                .setTitle("Menu")
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> showNameInputDialog(bikeId)
                        1 -> {
                            if (!isDeleteMode) {
                                enterDeleteMode(burgerMenuButton)
                                // тепер burgerMenuButton використовується як "підтвердити видалення"
                                burgerMenuButton.setOnClickListener {
                                    confirmDeleteSelected()
                                }
                            } else {
                                // якщо ми вже в режимі — confirm
                                confirmDeleteSelected()
                            }
                        }
                        2 -> {
                            if (!isReorderMode) {
                                enterReorderMode(burgerMenuButton)
                                // на другий клік підтверджуємо збереження
                                burgerMenuButton.setOnClickListener {
                                    exitReorderMode(save = true)
                                }
                            } else {
                                exitReorderMode(save = true)
                            }
                        }
                    }
                }
                .show()
        }
    }

    private fun showNameInputDialog(bikeId: Int) {
        val dialogView = layoutInflater.inflate(R.layout.di_textwriter, null)
        val inputText = dialogView.findViewById<EditText>(R.id.inputText)

        AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            .setView(dialogView)
            .create()
            .apply {
                dialogView.findViewById<Button>(R.id.okButton).setOnClickListener {
                    val setupName = inputText.text.toString().trim()
                    if (setupName.isNotEmpty()) {
                        addSetupButton(setupName, bikeId)
                        dismiss()
                    } else {
                        Toast.makeText(this@ActSetups, "Назва не може бути порожньою!", Toast.LENGTH_SHORT).show()
                    }
                }
                show()
            }
    }

    // Змініть метод addSetupButton
    // не забудь змінити addSetupButton щоб після додавання знову перезавантажити адаптер і зберегти порядок
    private fun addSetupButton(name: String, bikeId: Int) {
        lifecycleScope.launch {
            val setupDao = BikeDatabase.getDatabase(this@ActSetups).setupDao()
            val maketSetupDao = BikeDatabase.getDatabase(this@ActSetups).maketSetupDao()

            val newSetup = MarksForSetup(bikeId = bikeId, setupName = name)
            val newId = withContext(Dispatchers.IO) { setupDao.insertSetup(newSetup).toInt() }

            val newSetupData = SetupData(bikeId = bikeId, setupId = newId)
            withContext(Dispatchers.IO) { maketSetupDao.insertSetup(newSetupData) }

            withContext(Dispatchers.Main) {
                // перезавантажимо список повністю (простіше)
                loadExistingSetups(bikeId)
            }
        }
    }


    private fun openSetup(name: String, bikeId: Int, setupId: Int?) {
        val intent = Intent(this, MaketSetup::class.java).apply {
            putExtra("setup_name", name)
            putExtra("bike_id", bikeId)
            putExtra("setup_id", setupId)
        }
        startActivity(intent)
    }

}