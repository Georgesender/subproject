package com.example.sgb.adapters

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.sgb.room.MarksForSetup
import com.example.sgb.room.SetupData
import com.example.sub.R

data class SetupItem(
    val marks: MarksForSetup,
    val data: SetupData?,
    var isSelected: Boolean = false
)

class SetupAdapter(
    val items: MutableList<SetupItem>, // зробив public для зручності в Activity (якщо потрібно)
    private val onOpen: (SetupItem) -> Unit,
    private val onItemToggled: (SetupItem) -> Unit
) : RecyclerView.Adapter<SetupAdapter.VH>() {

    companion object {
        private const val PAYLOAD_MODE = "payload_mode"
        private const val PAYLOAD_SELECTION = "payload_selection"
    }

    var isDeleteMode: Boolean = false
        set(value) {
            field = value
            // Оновлюємо лише візуальні частини (shake / інші індикатори) через payload
            notifyItemRangeChanged(0, itemCount, PAYLOAD_MODE)
        }

    var isReorderMode: Boolean = false
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_MODE)
        }

    inner class VH(val view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.right_label)
        private val shockHSC = view.findViewById<TextView>(R.id.shock_hsc_value)
        private val shockLSC = view.findViewById<TextView>(R.id.shock_lsc_value)
        private val shockHSR = view.findViewById<TextView>(R.id.shock_hsr_value)
        private val shockLSR = view.findViewById<TextView>(R.id.shock_lsr_value)
        private val shockP = view.findViewById<TextView>(R.id.shock_pressure_value)
        private val forkHSC = view.findViewById<TextView>(R.id.fork_hsc_value)
        private val forkLSC = view.findViewById<TextView>(R.id.fork_lsc_value)
        private val forkHSR = view.findViewById<TextView>(R.id.fork_hsr_value)
        private val forkLSR = view.findViewById<TextView>(R.id.fork_lsr_value)
        private val forkP = view.findViewById<TextView>(R.id.fork_pressure_value)
        private val frontTyre = view.findViewById<TextView>(R.id.front_tyre_pressure_value)
        private val rearTyre = view.findViewById<TextView>(R.id.rear_tyre_pressure_value)

        fun bind(item: SetupItem) {
            // Повне звичайне биндування
            title.text = item.marks.setupName
            fun intOrDash(v: Int) = if (v != 0) v.toString() else "—"
            fun strOrDash(v: String?) = if (!v.isNullOrEmpty()) v else "—"

            shockHSC.text = item.data?.shockHSC?.let { intOrDash(it) } ?: "—"
            shockLSC.text = item.data?.shockLSC?.let { intOrDash(it) } ?: "—"
            shockHSR.text = item.data?.shockHSR?.let { intOrDash(it) } ?: "—"
            shockLSR.text = item.data?.shockLSR?.let { intOrDash(it) } ?: "—"
            shockP.text = item.data?.shockPressure?.let { strOrDash(it) } ?: "—"

            forkHSC.text = item.data?.forkHSC?.let { intOrDash(it) } ?: "—"
            forkLSC.text = item.data?.forkLSC?.let { intOrDash(it) } ?: "—"
            forkHSR.text = item.data?.forkHSR?.let { intOrDash(it) } ?: "—"
            forkLSR.text = item.data?.forkLSR?.let { intOrDash(it) } ?: "—"
            forkP.text = item.data?.forkPressure?.let { strOrDash(it) } ?: "—"

            frontTyre.text = item.data?.frontTyrePressure?.let { strOrDash(it) } ?: "—"
            rearTyre.text = item.data?.rearTyrePressure?.let { strOrDash(it) } ?: "—"

            // Візуальний стан виділення
            updateSelection(item)

            // Візуальний стан режимів (shake / hint)
            updateMode(isDeleteMode, isReorderMode)

            // Клік — залежно від режиму
            view.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                if (isDeleteMode) {
                    item.isSelected = !item.isSelected
                    // конкретне оновлення тільки selection
                    notifyItemChanged(pos, PAYLOAD_SELECTION)
                    onItemToggled(item)
                } else {
                    onOpen(item)
                }
            }
        }

        // часткове оновлення: режими (shake)
        fun updateMode(deleteMode: Boolean, reorderMode: Boolean) {
            if (deleteMode) startShake(view) else stopShake(view)
            // тут можна додати візуальний hint для reorderMode (наприклад, іконка grip), але мінімально:
            view.isClickable = !reorderMode // приклад: в reorder вимикаємо звичайні кліки (можна змінити)
        }

        // часткове оновлення: selection
        fun updateSelection(item: SetupItem) {
            view.alpha = if (item.isSelected) 0.5f else 1.0f
            view.isEnabled = !item.isSelected
        }

        private fun startShake(v: View) {
            // зупиняємо попередній, якщо був
            stopShake(v)
            val anim = ObjectAnimator.ofFloat(v, "rotation", -2f, 2f).apply {
                duration = 80
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                start()
            }
            v.setTag(R.id.tag_shake_anim, anim)
        }

        private fun stopShake(v: View) {
            val tag = v.getTag(R.id.tag_shake_anim)
            if (tag is ObjectAnimator) {
                tag.cancel()
                v.rotation = 0f
                v.setTag(R.id.tag_shake_anim, null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.btn_maket_setup, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    // Підтримуємо payloads — щоб виконувати часткові оновлення
    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            holder.bind(items[position])
        } else {
            // для кожного payload виконуємо мінімальні оновлення
            for (p in payloads) {
                when (p) {
                    PAYLOAD_MODE -> holder.updateMode(isDeleteMode, isReorderMode)
                    PAYLOAD_SELECTION -> holder.updateSelection(items[position])
                    else -> holder.bind(items[position])
                }
            }
        }
    }

    // залишаємо дефолтний onBind (він просто делегує)
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    fun moveItem(from: Int, to: Int) {
        if (from == to) return
        val it = items.removeAt(from)
        items.add(to, it)
        notifyItemMoved(from, to)
        // краще викликати notifyItemChanged для обох позицій, якщо потрібно оновити порядкові нумерації тощо
        notifyItemChanged(to)
        notifyItemChanged(from)
    }

    fun removeSelected(): List<SetupItem> {
        val removed = mutableListOf<SetupItem>()
        // Видаляємо з кінця до початку, щоб не ламати індекси, і використовуємо notifyItemRemoved
        for (i in items.size - 1 downTo 0) {
            if (items[i].isSelected) {
                removed.add(0, items[i]) // додаємо в початок, щоб зберегти початковий порядок у результаті
                items.removeAt(i)
                notifyItemRemoved(i)
            }
        }
        return removed
    }

    fun getCurrentOrderIds(): List<Int> = items.map { it.marks.id }
    /**
     * Знімає selection з усіх елементів і викликає часткові оновлення для змінених позицій.
     */
    fun clearSelection() {
        val changedPositions = mutableListOf<Int>()
        for (i in items.indices) {
            if (items[i].isSelected) {
                items[i].isSelected = false
                changedPositions.add(i)
            }
        }
        // Оновлюємо тільки змінені позиції
        changedPositions.forEach { pos ->
            notifyItemChanged(pos, PAYLOAD_SELECTION)
        }
    }

    /**
     * Замінює весь список елементів, але використовує DiffUtil для оптимальних оновлень.
     */
    fun replaceItems(newItems: List<SetupItem>) {
        val old = items.toList()
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = old.size
            override fun getNewListSize(): Int = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return old[oldItemPosition].marks.id == newItems[newItemPosition].marks.id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val o = old[oldItemPosition]
                val n = newItems[newItemPosition]
                // перевіряємо важливі поля: name, isSelected, data (можна адаптувати)
                return o.marks.setupName == n.marks.setupName
                        && o.isSelected == n.isSelected
                        && (o.data == n.data) // тут Kotlin data class порівняння або null-логіка
            }
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }
}
