package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.StaffActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffActivityLogDao {
    @Query("SELECT * FROM staff_activity_log WHERE businessId = :businessId ORDER BY timestamp DESC LIMIT 200")
    fun getActivityLogsFlow(businessId: String): Flow<List<StaffActivityLogEntity>>

    @Query("SELECT * FROM staff_activity_log WHERE businessId = :businessId AND staffId = :staffId ORDER BY timestamp DESC LIMIT 100")
    fun getActivityLogsByStaffFlow(businessId: String, staffId: String): Flow<List<StaffActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StaffActivityLogEntity)

    @Query("DELETE FROM staff_activity_log WHERE businessId = :businessId AND timestamp < :olderThanEpochMs")
    suspend fun purgeOldLogs(businessId: String, olderThanEpochMs: Long)
}
