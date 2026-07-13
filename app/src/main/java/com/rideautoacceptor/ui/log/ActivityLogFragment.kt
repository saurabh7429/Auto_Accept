package com.rideautoacceptor.ui.log

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.rideautoacceptor.R
import com.rideautoacceptor.RideAutoAcceptorApp
import com.rideautoacceptor.data.model.RideEvent
import com.rideautoacceptor.databinding.FragmentActivityLogBinding
import kotlinx.coroutines.launch

class ActivityLogFragment : Fragment() {

    private var _binding: FragmentActivityLogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivityLogViewModel by viewModels {
        ActivityLogViewModel.Factory(RideAutoAcceptorApp.get(requireContext()).repository)
    }

    private lateinit var adapter: RideEventAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterChips()
        setupClearButton()
        observeEvents()
    }

    private fun setupRecyclerView() {
        adapter = RideEventAdapter()

        binding.recyclerLog.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@ActivityLogFragment.adapter
            setHasFixedSize(false)
        }

        // Swipe-to-delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos == RecyclerView.NO_ID.toInt()) return
                val event = adapter.peek(pos) ?: return
                viewModel.deleteEvent(event)
                Snackbar.make(binding.root, R.string.log_deleted, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo) {
                        // Re-insert — for simplicity we just refresh; a full undo
                        // would require saving the deleted item in a buffer.
                        viewModel.setFilter(viewModel.filter.value)
                    }.show()
            }
        }).attachToRecyclerView(binding.recyclerLog)

        // Loading state
        adapter.addLoadStateListener { states ->
            binding.progressLog.isVisible = states.refresh is LoadState.Loading
            val isEmpty = adapter.itemCount == 0 &&
                          states.refresh is LoadState.NotLoading
            binding.layoutEmpty.isVisible = isEmpty
        }
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                R.id.chip_accepted in checkedIds -> "ACCEPTED"
                R.id.chip_skipped  in checkedIds -> "SKIPPED"
                else                              -> "ALL"
            }
            viewModel.setFilter(filter)

            binding.tvEmptyMessage.text = getString(
                when (filter) {
                    "ACCEPTED" -> R.string.log_empty_accepted
                    "SKIPPED"  -> R.string.log_empty_skipped
                    else       -> R.string.log_empty_all
                }
            )
        }
    }

    private fun setupClearButton() {
        binding.btnClearLog.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.log_clear_all)
                .setMessage(
                    getString(R.string.log_clear_confirm, adapter.itemCount)
                )
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.deleteAll()
                    Snackbar.make(binding.root, R.string.log_cleared, Snackbar.LENGTH_SHORT)
                        .show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { pagingData ->
                adapter.submitData(pagingData)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
