package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String, // Staff ID (e.g. STF-101 or UUID)
    val businessId: String,
    val name: String,
    val mobile: String,
    val pin: String,
    val role: String = "DELIVERY_BOY", // MANAGER, SUPERVISOR, DELIVERY_BOY, MILKING_STAFF, FEEDING_WORKER, VET_DOCTOR, CLEANER, MILK_TESTER, CUSTOM
    val customRoleTitle: String = "", // e.g. "Milking Specialist", "Feeding Caretaker", "Milk Tester"
    val permissions: String = "DELIVERY,CUSTOMERS", // Comma-delimited list of permitted modules
    val assignedRoute: String = "",
    val monthlySalary: Double = 12000.0,
    val dailyWage: Double = 400.0,
    val salaryType: String = "MONTHLY", // "MONTHLY" or "DAILY"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "owner",
    val updatedBy: String = "owner",
    val version: Long = 1L,
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
) {
    fun hasPermission(permissionKey: String): Boolean {
        if (role == "MANAGER" || role == "SUPERVISOR") return true
        val list = permissions.split(",").map { it.trim().uppercase() }
        return list.contains(permissionKey.uppercase()) || list.contains("ALL")
    }

    fun displayRole(): String {
        return if (customRoleTitle.isNotBlank()) customRoleTitle else role.replace("_", " ")
    }
}
