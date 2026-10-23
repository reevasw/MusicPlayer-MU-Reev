package com.example.mureev

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mureev.databinding.DialogQueueBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Collections

class QueueBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: DialogQueueBinding
    private lateinit var adapter: QueueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Menerapkan tema yang sudah kita buat
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheet)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = QueueAdapter(requireContext(), PlayerActivity.musicListPA) { clickedPosition ->
            // Kode ini akan berjalan saat lagu di antrean diklik
            val playerActivity = activity as? PlayerActivity
            if (playerActivity != null) {
                playerActivity.playSongFromQueue(clickedPosition)
                dismiss()
            }
        }

        binding.queueRV.adapter = adapter
        binding.queueRV.layoutManager = LinearLayoutManager(requireContext())

        // Implementasi Drag & Drop dan Swipe
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, // Arah drag
            ItemTouchHelper.START or ItemTouchHelper.END // Arah swipe
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Logika saat item di-drag
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                Collections.swap(PlayerActivity.musicListPA, fromPosition, toPosition)
                adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Logika saat item di-swipe
                val position = viewHolder.adapterPosition
                PlayerActivity.musicListPA.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.queueRV)
    }
}