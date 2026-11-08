package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_leaves")
data class CustomerLeaveEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val customerId: String,
    val customerName: String,
    val customerMobile: String = "",
    val startDate: Long, // Epoch day normalized to midnight
    val endDate: Long,   // Epoch day normalized to midnight (same as startDate for single day)
    val shift: String = "BOTH", // "MORNING", "EVENING", "BOTH"
    val reason: String = "Customer informed not to supply milk", // e.g. "Out of station", "No milk needed", "Informed on call"
    val dailyQuantitySaved: Double = 0.0, // Expected liters deducted per day
    val milkType: String = "COW", // "COW", "BUFFALO", "BOTH"
    val status: String = "ACTIVE", // "ACTIVE", "CANCELLED"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val syncStatus: String = "PENDING",
    val deletedAt: Long? = null
)
