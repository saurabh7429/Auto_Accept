package com.rideautoacceptor.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.rideautoacceptor.R

/** Data model for each permission row */
data class PermissionItem(
    val id: String,
    val icon: String,                  // emoji
    val title: String,
    val subtitle: String,              // bilingual
    val whyDescription: String,        // bilingual
    val actionLabel: String,
    var isGranted: Boolean = false
)

class PermissionAdapter(
    private val items: List<PermissionItem>,
    private val onAction: (PermissionItem) -> Unit
) : RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder>() {

    inner class PermissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIcon:        TextView = itemView.findViewById(R.id.tv_perm_icon)
        val tvTitle:       TextView = itemView.findViewById(R.id.tv_perm_title)
        val tvSubtitle:    TextView = itemView.findViewById(R.id.tv_perm_subtitle)
        val tvStatusIcon:  TextView = itemView.findViewById(R.id.tv_perm_status_icon)
        val tvWhy:         TextView = itemView.findViewById(R.id.tv_perm_why)
        val btnAction:     Button   = itemView.findViewById(R.id.btn_perm_action)
        val tvGranted:     TextView = itemView.findViewById(R.id.tv_perm_granted)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_permission_card, parent, false)
        return PermissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        val item = items[position]
        holder.tvIcon.text       = item.icon
        holder.tvTitle.text      = item.title
        holder.tvSubtitle.text   = item.subtitle
        holder.tvWhy.text        = item.whyDescription
        holder.btnAction.text    = item.actionLabel

        if (item.isGranted) {
            holder.tvStatusIcon.text = "✅"
            holder.btnAction.isVisible = false
            holder.tvGranted.isVisible = true
        } else {
            holder.tvStatusIcon.text = "❌"
            holder.btnAction.isVisible = true
            holder.tvGranted.isVisible = false
        }

        holder.btnAction.setOnClickListener { onAction(item) }
    }

    override fun getItemCount() = items.size

    fun updateGrantStatus(id: String, granted: Boolean) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            items[index].isGranted = granted
            notifyItemChanged(index)
        }
    }
}
