package com.rideautoacceptor.ui.log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rideautoacceptor.R
import com.rideautoacceptor.data.model.RideEvent
import com.rideautoacceptor.util.toDistanceString
import com.rideautoacceptor.util.toRupeeString
import com.rideautoacceptor.util.toTimeString

class RideEventAdapter : PagingDataAdapter<RideEvent, RideEventAdapter.EventViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RideEvent>() {
            override fun areItemsTheSame(a: RideEvent, b: RideEvent) = a.id == b.id
            override fun areContentsTheSame(a: RideEvent, b: RideEvent) = a == b
        }
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val accentBar:    View     = itemView.findViewById(R.id.view_accent_bar)
        val tvAppName:    TextView = itemView.findViewById(R.id.tv_app_name)
        val tvBadge:      TextView = itemView.findViewById(R.id.tv_action_badge)
        val tvFare:       TextView = itemView.findViewById(R.id.tv_fare)
        val tvDistances:  TextView = itemView.findViewById(R.id.tv_distances)
        val tvSkipReason: TextView = itemView.findViewById(R.id.tv_skip_reason)
        val tvTimestamp:  TextView = itemView.findViewById(R.id.tv_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ride_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position) ?: return
        val ctx   = holder.itemView.context

        holder.tvAppName.text   = event.appName
        holder.tvTimestamp.text = event.timestamp.toTimeString()

        // Fare
        holder.tvFare.text = event.fareAmount?.toRupeeString() ?: "₹?"

        // Distances
        val parts = mutableListOf<String>()
        event.pickupDistanceKm?.let { parts.add("↑ ${it.toDistanceString()}") }
        event.dropDistanceKm?.let   { parts.add("↓ ${it.toDistanceString()}") }
        holder.tvDistances.text = parts.joinToString("  ")

        // Action badge + accent color
        if (event.action == RideEvent.ACTION_ACCEPTED) {
            holder.tvBadge.text = "✅ ACCEPTED"
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_accepted)
            holder.tvBadge.setTextColor(ctx.getColor(R.color.color_accepted))
            holder.accentBar.setBackgroundColor(ctx.getColor(R.color.color_accepted))
            holder.tvSkipReason.visibility = View.GONE
        } else {
            holder.tvBadge.text = "⏭ SKIPPED"
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_skipped)
            holder.tvBadge.setTextColor(ctx.getColor(R.color.color_skipped_text))
            holder.accentBar.setBackgroundColor(ctx.getColor(R.color.color_skipped))
            event.skipReason?.let { reason ->
                holder.tvSkipReason.text = "Reason: $reason"
                holder.tvSkipReason.visibility = View.VISIBLE
            } ?: run {
                holder.tvSkipReason.visibility = View.GONE
            }
        }
    }
}
