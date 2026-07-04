package com.rideautoacceptor.util

import android.view.accessibility.AccessibilityNodeInfo

/** Recursively collect all non-empty text from an accessibility node tree. */
fun AccessibilityNodeInfo.collectText(sb: StringBuilder) {
    text?.let { if (it.isNotBlank()) sb.append(it).append('\n') }
    contentDescription?.let { if (it.isNotBlank()) sb.append(it).append('\n') }
    for (i in 0 until childCount) {
        getChild(i)?.also { child ->
            child.collectText(sb)
            child.recycle()
        }
    }
}

/**
 * Depth-first search for the first clickable node whose trimmed text matches
 * any entry in [targetTexts].
 * SAFETY: never matches nodes whose text is in [Constants.DECLINE_TEXTS].
 */
fun AccessibilityNodeInfo.findClickableNode(
    targetTexts: Set<String>,
    excludeTexts: Set<String> = Constants.DECLINE_TEXTS
): AccessibilityNodeInfo? {
    val nodeText = text?.toString()?.trim() ?: contentDescription?.toString()?.trim()
    if (nodeText != null && nodeText in excludeTexts) return null
    if (nodeText != null && nodeText in targetTexts && isClickable) return this

    for (i in 0 until childCount) {
        val child = getChild(i) ?: continue
        val result = child.findClickableNode(targetTexts, excludeTexts)
        if (result != null) return result
        child.recycle()
    }
    return null
}

/** Format a Float as a human-readable distance string: "1.6 km" or "500 m" */
fun Float.toDistanceString(): String =
    if (this < 1f) "${(this * 1000).toInt()} m" else "${"%.1f".format(this)} km"

/** Format a Float as a Rupee amount: "₹89" */
fun Float.toRupeeString(): String = "₹${this.toInt()}"

/** Convert epoch millis to a human-readable time: "10:30 AM" */
fun Long.toTimeString(): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = this@toTimeString }
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val min  = cal.get(java.util.Calendar.MINUTE)
    val amPm = if (hour < 12) "AM" else "PM"
    val h12  = when {
        hour == 0  -> 12
        hour > 12  -> hour - 12
        else       -> hour
    }
    return "%d:%02d %s".format(h12, min, amPm)
}

/** Convert epoch millis to a date string: "4 Jul 2026" */
fun Long.toDateString(): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = this@toDateString }
    val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    return "${cal.get(java.util.Calendar.DAY_OF_MONTH)} " +
           "${months[cal.get(java.util.Calendar.MONTH)]} " +
           "${cal.get(java.util.Calendar.YEAR)}"
}

/** True if this epoch-millis timestamp is on today's date */
fun Long.isToday(): Boolean {
    val today = java.util.Calendar.getInstance()
    val that  = java.util.Calendar.getInstance().apply { timeInMillis = this@isToday }
    return today.get(java.util.Calendar.DATE) == that.get(java.util.Calendar.DATE) &&
           today.get(java.util.Calendar.MONTH) == that.get(java.util.Calendar.MONTH) &&
           today.get(java.util.Calendar.YEAR) == that.get(java.util.Calendar.YEAR)
}
