package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerLeaveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerLeaveDao {
    @Query("SELECT * FROM customer_leaves WHERE businessId = :businessId AND deletedAt IS NULL AND status = 'ACTIVE' ORDER BY startDate ASC")
    fun getAllActiveLeavesFlow(businessId: String): Flow<List<CustomerLeaveEntity>>

    @Query("SELECT * FROM customer_leaves WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY startDate DESC")
    fun getAllLeavesFlow(businessId: String): Flow<List<CustomerLeaveEntity>>

    @Query("SELECT * FROM customer_leaves WHERE businessId = :businessId AND customerId = :customerId AND deletedAt IS NULL AND status = 'ACTIVE' ORDER BY startDate DESC")
    fun getActiveLeavesForCustomerFlow(businessId: String, customerId: String): Flow<List<CustomerLeaveEntity>>

    @Query("SELECT * FROM customer_leaves WHERE businessId = :businessId AND customerId = :customerId AND deletedAt IS NULL ORDER BY startDate DESC")
    suspend fun getLeavesForCustomer(businessId: String, customerId: String): List<CustomerLeaveEntity>

    @Query("SELECT * FROM customer_leaves WHERE businessId = :businessId AND id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getLeaveById(businessId: String, id: String): CustomerLeaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(leave: CustomerLeaveEntity)

    @Update
    suspend fun updateLeave(leave: CustomerLeaveEntity)

    @Query("UPDATE customer_leaves SET status = 'CANCELLED', updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id AND businessId = :businessId")
    suspend fun cancelLeave(businessId: String, id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE customer_leaves SET deletedAt = :deletedAt, syncStatus = 'PENDING' WHERE id = :id AND businessId = :businessId")
    suspend fun softDeleteLeave(businessId: String, id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM customer_leaves WHERE businessId = :businessId AND id LIKE 'leave_demo_%'")
    suspend fun deleteDemoLeaves(businessId: String)
}
