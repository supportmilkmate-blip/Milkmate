package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        BusinessEntity::class,
        StaffEntity::class,
        StaffAttendanceEntity::class,
        StaffPaymentEntity::class,
        CustomerEntity::class,
        DeliveryEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        InventoryItemEntity::class,
        InventoryTransactionEntity::class,
        InventoryMonthClosingEntity::class,
        OrderEntity::class,
        SyncQueueEntity::class,
        FarmerEntity::class,
        MilkCollectionEntity::class,
        BulkDispatchEntity::class,
        CattleEntity::class,
        CattleWeightEntity::class,
        CattleCmtEntity::class,
        CattleBcsEntity::class,
        BreedingRecordEntity::class,
        DewormingRecordEntity::class,
        VaccinationRecordEntity::class,
        TreatmentRecordEntity::class,
        FarmObservationEntity::class,
        CattleMilkingRecordEntity::class,
        MilkWastageEntity::class,
        CustomerLeaveEntity::class,
        StaffActivityLogEntity::class
    ],
    version = 19,
    exportSchema = false
)
abstract class MilkMateDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun staffDao(): StaffDao
    abstract fun staffActivityLogDao(): StaffActivityLogDao
    abstract fun customerDao(): CustomerDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun orderDao(): OrderDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun farmerDao(): FarmerDao
    abstract fun milkCollectionDao(): MilkCollectionDao
    abstract fun bulkDispatchDao(): BulkDispatchDao
    abstract fun cattleDao(): CattleDao
    abstract fun milkWastageDao(): MilkWastageDao
    abstract fun customerLeaveDao(): CustomerLeaveDao

    companion object {
        @Volatile
        private var INSTANCE: MilkMateDatabase? = null

        private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN rateMethod TEXT NOT NULL DEFAULT 'FLAT'")
            }
        }

        private val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN sequenceOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `customer_leaves` (
                        `id` TEXT NOT NULL,
                        `businessId` TEXT NOT NULL,
                        `customerId` TEXT NOT NULL,
                        `customerName` TEXT NOT NULL,
                        `customerMobile` TEXT NOT NULL DEFAULT '',
                        `startDate` INTEGER NOT NULL,
                        `endDate` INTEGER NOT NULL,
                        `shift` TEXT NOT NULL DEFAULT 'BOTH',
                        `reason` TEXT NOT NULL DEFAULT 'Customer informed not to supply milk',
                        `dailyQuantitySaved` REAL NOT NULL DEFAULT 0.0,
                        `milkType` TEXT NOT NULL DEFAULT 'COW',
                        `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `createdBy` TEXT NOT NULL DEFAULT 'system',
                        `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): MilkMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MilkMateDatabase::class.java,
                    "milkmate_production.db"
                )
                    .addMigrations(MIGRATION_14_15, MIGRATION_16_17, MIGRATION_17_18)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
