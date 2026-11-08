package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff_activity_log")
data class StaffActivityLogEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val staffId: String,
    val staffName: String,
    val staffRole: String,
    val module: String, // "MILKING", "HEALTH", "COLLECTION", "DELIVERY", "INVENTORY", "PAYMENTS", "CUSTOMERS"
    val actionSummary: String, // e.g. "Logged 14.5L Morning Milk for Cow Tag #C-101"
    val timestamp: Long = System.currentTimeMillis()
)
