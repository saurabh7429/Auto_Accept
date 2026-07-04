package com.rideautoacceptor.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.rideautoacceptor.data.model.RideEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface RideEventDao {

    // ── Insert ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: RideEvent): Long

    // ── Queries ───────────────────────────────────────────────────────────────

    /** All events, newest first — for the Activity Log RecyclerView via Paging */
    @Query("SELECT * FROM ride_events ORDER BY timestamp DESC")
    fun getAllPaged(): PagingSource<Int, RideEvent>

    /** Only accepted events */
    @Query("SELECT * FROM ride_events WHERE action = 'ACCEPTED' ORDER BY timestamp DESC")
    fun getAcceptedPaged(): PagingSource<Int, RideEvent>

    /** Only skipped events */
    @Query("SELECT * FROM ride_events WHERE action = 'SKIPPED' ORDER BY timestamp DESC")
    fun getSkippedPaged(): PagingSource<Int, RideEvent>

    /** Live count of events accepted TODAY (for dashboard stat) */
    @Query("""
        SELECT COUNT(*) FROM ride_events
        WHERE action = 'ACCEPTED'
          AND date(timestamp / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
    """)
    fun getTodayAcceptedCount(): Flow<Int>

    /** Live count of events skipped TODAY */
    @Query("""
        SELECT COUNT(*) FROM ride_events
        WHERE action = 'SKIPPED'
          AND date(timestamp / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
    """)
    fun getTodaySkippedCount(): Flow<Int>

    /** Most recent event (for live status on dashboard) */
    @Query("SELECT * FROM ride_events ORDER BY timestamp DESC LIMIT 1")
    fun getLatestEvent(): Flow<RideEvent?>

    // ── Delete ────────────────────────────────────────────────────────────────

    @Delete
    suspend fun delete(event: RideEvent)

    @Query("DELETE FROM ride_events")
    suspend fun deleteAll()

    /** Delete events older than [thresholdMillis] to avoid unbounded growth */
    @Query("DELETE FROM ride_events WHERE timestamp < :thresholdMillis")
    suspend fun deleteOlderThan(thresholdMillis: Long)
}
