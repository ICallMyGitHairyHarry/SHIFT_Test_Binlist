package com.example.shifttestbinlist.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shifttestbinlist.databinding.VerticalListItemBinding
import com.example.shifttestbinlist.network.BinInfo
import java.util.*

class BinListAdapter(private val clickListener: BinListener) : ListAdapter<BinInfo,
        BinListAdapter.BinItemViewHolder>(DiffCallback) {

    class BinItemViewHolder(private var binding:VerticalListItemBinding):
        RecyclerView.ViewHolder(binding.root) {
        fun bind(binItem: BinInfo, binInfoString: String, clickListener: BinListener) {
            // set the variables of the layout
            binding.binItem = binItem
            binding.binInfoString = binInfoString
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BinItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return BinItemViewHolder(
            VerticalListItemBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: BinItemViewHolder, position: Int) {
        val binItem = getItem(position)
        // list of all non-null bin info
        val binInfoList = with(binItem) {
            listOfNotNull(scheme, brand, number?.length,
                number?.luhn?.let { if (it) "Luhn: Yes" else "Luhn: No" },
                type, prepaid?.let { if (it) "Prepaid: Yes" else "Prepaid: No" },
                country?.name, bank?.values?.joinToString(", ")
            )
        }
        // binItem, capitalized string representation of binInfoList
        // and click listener are passed to bind()
        holder.bind(binItem, binInfoList.joinToString(", ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }, clickListener)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<BinInfo>() {
        override fun areItemsTheSame(oldItem: BinInfo, newItem: BinInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BinInfo, newItem: BinInfo): Boolean {
            return oldItem.scheme == newItem.scheme
                    && oldItem.type == newItem.type
                    && oldItem.bank == newItem.bank
                    && oldItem.brand == newItem.brand
                    && oldItem.prepaid == newItem.prepaid
        }
    }
}

class BinListener(
    val clickFunction: (binItem: BinInfo) -> Unit,
    val longClickFunction: (binItem: BinInfo) -> Boolean
) {
    fun onClick(binItem: BinInfo) = clickFunction(binItem)
    fun onLongClick(binItem: BinInfo) = longClickFunction(binItem)
}