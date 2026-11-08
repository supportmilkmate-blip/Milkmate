package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.local.entity.BusinessEntity
import com.example.ui.theme.*

data class DairyBusinessType(
    val code: String,
    val title: String,
    val shortName: String,
    val badge: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val themeColor: Color,
    val showHerdBreeding: Boolean,
    val showMilkingProduction: Boolean,
    val showCattleFeed: Boolean,
    val showProductInventory: Boolean,
    val showFarmerProcurement: Boolean,
    val showRetailRoutes: Boolean,
    val showBilling: Boolean,
    val showBulkDispatches: Boolean,
    val showRetailPos: Boolean,
    val keyFeatures: List<String>,
    val hiddenFeatures: List<String>
) {
    val showInventoryManagement: Boolean
        get() = showCattleFeed || showProductInventory
    val showCollectionDesk: Boolean
        get() = showFarmerProcurement
    val showDeliveryRoutes: Boolean
        get() = showRetailRoutes
}

object BusinessModeFeatures {

    val ALL_TYPES = listOf(
        // 1. Farmer selling to Household Customers + Surplus to Center
        DairyBusinessType(
            code = "FARMER",
            title = "Dairy Farmer (Direct Retail + Center)",
            shortName = "Farmer & Retail",
            badge = "🚜 Producer & Seller",
            subtitle = "Own herd + deliver to households & sell surplus to Center/Trader",
            description = "For farmers who produce milk from their own cattle, deliver milk to household customer subscription routes, and deliver excess milk to a collection center or trader.",
            icon = Icons.Default.Agriculture,
            themeColor = GrassGreen,
            showHerdBreeding = true,
            showMilkingProduction = true,
            showCattleFeed = true,
            showProductInventory = true,
            showFarmerProcurement = false,
            showRetailRoutes = true,
            showBilling = true,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "Cattle Milking Yield & Lactation Stages",
                "Cattle Feed, Fodder & Feed Inventory",
                "Dairy Byproducts (Paneer, Ghee, Curd, Khoya)",
                "Morning & Evening Doorstep Delivery Routes",
                "Monthly Customer Billing & WhatsApp Invoices",
                "Surplus Milk Dispatch to Collection Center"
            ),
            hiddenFeatures = listOf(
                "Village Farmer Milk Collection Desk",
                "Bulk Tanker Logistics"
            )
        ),

        // 2. Farmer selling to Collection Center / Trader / BMC (No household delivery)
        DairyBusinessType(
            code = "FARMER_WHOLESALE",
            title = "Dairy Farmer (Wholesale / Center)",
            shortName = "Producer (Center)",
            badge = "🚜 Farm to Center",
            subtitle = "Own cattle herd & supply milk to Collection Center or Trader",
            description = "For dairy farmers who produce milk from their own cows/buffalos and supply it daily to a Dairy Collection Centre, BMC Chilling Point, Trader, or Dairy Plant.",
            icon = Icons.Default.Agriculture,
            themeColor = GrassGreen,
            showHerdBreeding = true,
            showMilkingProduction = true,
            showCattleFeed = true,
            showProductInventory = true,
            showFarmerProcurement = false,
            showRetailRoutes = false,
            showBilling = false,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "Cattle Milking Yield & Lactation Tracking",
                "Cattle Feed, Silage & Fodder Inventory",
                "Dairy Byproducts (Paneer, Ghee, Curd)",
                "Daily Milk Supply Slips to Collection Center / Trader",
                "FAT, SNF & Rate Received Ledger",
                "Cattle Feed, Fodder & Veterinary Expenses"
            ),
            hiddenFeatures = listOf(
                "Household Door-to-Door Delivery Routes",
                "Individual Retail Customer Monthly Khatas",
                "Village Farmer Milk Collection Desk"
            )
        ),

        // 3. Integrated Producer & Aggregator (Own Herd + Sells to Individuals + Collects from Nearby Farmers)
        DairyBusinessType(
            code = "INTEGRATED",
            title = "Integrated Producer & Collector",
            shortName = "Integrated Farm",
            badge = "🌐 Farm & Aggregator",
            subtitle = "Own Farm + Collect from Farmers + Retail Routes + Bulk Supply",
            description = "For progressive dairy businesses with their own cattle farm who also collect milk from nearby farmers, distribute to household customers, and supply bulk buyers.",
            icon = Icons.Default.Hub,
            themeColor = DeepOceanNavy,
            showHerdBreeding = true,
            showMilkingProduction = true,
            showCattleFeed = true,
            showProductInventory = true,
            showFarmerProcurement = true,
            showRetailRoutes = true,
            showBilling = true,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "Own Cattle Milking Yields & Herd Breeding",
                "Village Farmer Milk Collection Desk (FAT/SNF)",
                "Dairy Byproducts (Paneer, Ghee, Curd, Butter, Khoya)",
                "Doorstep Customer Delivery Routes & Monthly Invoices",
                "Bulk Dispatches & Wholesale Supply"
            ),
            hiddenFeatures = emptyList()
        ),

        // 4. Milk Trader / Vendor / Wholesaler (Collects from Farmers -> Sells to Bulk & Households)
        DairyBusinessType(
            code = "TRADER",
            title = "Milk Trader / Vendor / Wholesaler",
            shortName = "Milk Trader",
            badge = "🚚 Trader & Vendor",
            subtitle = "Collect milk from farmers & distribute to bulk buyers, hotels & households",
            description = "For milk traders, vendors, and aggregators who do NOT keep cattle, but purchase milk from local village farmers and distribute to bulk buyers, tea stalls, restaurants, and retail customers.",
            icon = Icons.Default.LocalShipping,
            themeColor = GoldenOrange,
            showHerdBreeding = false,
            showMilkingProduction = false,
            showCattleFeed = false,
            showProductInventory = true,
            showFarmerProcurement = true,
            showRetailRoutes = true,
            showBilling = true,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "Farmer Milk Collection Desk (FAT, SNF, Rate Chart)",
                "Farmer Passbooks & 1-Tap Cash/UPI Payouts",
                "Dairy Byproducts (Paneer, Ghee, Curd, Pouch Milk)",
                "Commercial B2B Accounts (Hotels, Cafes, Bulk Buyers)",
                "Household Delivery Routes & Trading Margin Spread",
                "Milk Collection vs Sales Volume Reconciliation"
            ),
            hiddenFeatures = listOf(
                "Cattle Herd, Cows & Buffalo Breeding",
                "Farm Milking Yield Logs",
                "Cattle Feed, Silage & Fodder Stock"
            )
        ),

        // 5. Milk Collection Centre / BMC / Chilling Center (Collects from Farmers -> Supplies Factory)
        DairyBusinessType(
            code = "COLLECTION_CENTER",
            title = "Milk Collection Centre / BMC",
            shortName = "Collection Centre",
            badge = "🏢 BMC Center",
            subtitle = "Collect milk from village farmers & supply factory tankers",
            description = "For village milk collection centres, BMC chilling points, and societies that collect milk from hundreds of farmers, perform FAT/SNF testing, and dispatch bulk tankers to dairy factories.",
            icon = Icons.Default.LocalDrink,
            themeColor = RoyalBluePrimary,
            showHerdBreeding = false,
            showMilkingProduction = false,
            showCattleFeed = true,
            showProductInventory = true,
            showFarmerProcurement = true,
            showRetailRoutes = false,
            showBilling = false,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "High-Speed Village Farmer Collection Desk (FAT, SNF, CLR)",
                "Farmer Passbooks, Deductions & Payout Sheets",
                "Dairy Byproducts & Cattle Feed Distribution",
                "Bulk Tanker & Chilling Plant Outward Challans",
                "Center Spread: (Factory Sales − Farmer Payouts)"
            ),
            hiddenFeatures = listOf(
                "Door-to-Door Household Delivery Routes",
                "Consumer Household Monthly Khatas",
                "Cattle Herd & Milking Logs"
            )
        ),

        // 6. Custom / Build Your Own Dairy Setup
        DairyBusinessType(
            code = "CUSTOM",
            title = "Custom Dairy Setup (Build Your Own)",
            shortName = "Custom Setup",
            badge = "🛠️ Custom Setup",
            subtitle = "Custom modular setup tailored to your exact business requirements",
            description = "For dairy businesses with unique workflows. Pick and choose exactly which sourcing, distribution, and billing features you need.",
            icon = Icons.Default.Tune,
            themeColor = Color(0xFF00897B),
            showHerdBreeding = true,
            showMilkingProduction = true,
            showCattleFeed = true,
            showProductInventory = true,
            showFarmerProcurement = true,
            showRetailRoutes = true,
            showBilling = true,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "Custom Sourcing (Own Cattle + Village Farmers)",
                "Custom Sales Channels (Doorstep, Wholesale, Tankers)",
                "Dairy Byproducts (Paneer, Ghee, Curd, Khoya)",
                "Cattle Feed Inventory & Milk Intake",
                "Full Workspace Tailoring"
            ),
            hiddenFeatures = emptyList()
        )
    )

    fun getByType(code: String?): DairyBusinessType {
        return ALL_TYPES.find { it.code == code }
            ?: if (code == "FARMER_RETAIL_WHOLESALE") ALL_TYPES.find { it.code == "FARMER" } ?: ALL_TYPES.first()
            else ALL_TYPES.first()
    }

    fun computeModeFromCapabilities(
        sourceOwnCattle: Boolean,
        sourceVillageFarmers: Boolean,
        destHouseholds: Boolean,
        destCollectionCenter: Boolean,
        destBulkCommercial: Boolean,
        destFactoryTankers: Boolean,
        explicitMode: String? = null
    ): String {
        if (explicitMode == "CUSTOM") return "CUSTOM"
        return when {
            sourceOwnCattle && sourceVillageFarmers -> "INTEGRATED"
            !sourceOwnCattle && sourceVillageFarmers && destFactoryTankers -> "COLLECTION_CENTER"
            !sourceOwnCattle && sourceVillageFarmers -> "TRADER"
            sourceOwnCattle && !destHouseholds && (destCollectionCenter || destBulkCommercial || destFactoryTankers) -> "FARMER_WHOLESALE"
            sourceOwnCattle && destHouseholds -> "FARMER"
            else -> explicitMode ?: "FARMER"
        }
    }

    fun showHerdBreeding(business: BusinessEntity?): Boolean {
        if (business == null) return false
        return business.sourceOwnCattle
    }

    fun showHerdBreeding(mode: String?): Boolean {
        if (mode == null) return false
        if (mode in listOf("TRADER", "COLLECTION_CENTER")) {
            return false
        }
        return getByType(mode).showHerdBreeding
    }

    fun showMilkingProduction(business: BusinessEntity?): Boolean {
        if (business == null) return false
        return business.sourceOwnCattle
    }

    fun showMilkingProduction(mode: String?): Boolean {
        if (mode == null) return false
        if (mode in listOf("TRADER", "COLLECTION_CENTER")) {
            return false
        }
        return getByType(mode).showMilkingProduction
    }

    fun showCattleFeed(business: BusinessEntity?): Boolean {
        if (business == null) return false
        return business.hasCattleFeed || business.sourceOwnCattle
    }

    fun showCattleFeed(mode: String?): Boolean {
        if (mode == null) return false
        if (mode in listOf("TRADER", "COLLECTION_CENTER")) {
            return false
        }
        return getByType(mode).showCattleFeed
    }

    fun showProductInventory(business: BusinessEntity?): Boolean {
        if (business == null) return true
        return business.hasPaneer || business.hasCurdChaas || business.hasGheeButter || business.hasKhoyaSweets
    }

    fun showProductInventory(mode: String?): Boolean = true

    fun showInventoryManagement(business: BusinessEntity?): Boolean = true

    fun showInventoryManagement(mode: String?): Boolean = true

    fun showCollectionDesk(business: BusinessEntity?): Boolean {
        if (business == null) return false
        return business.sourceVillageFarmers
    }

    fun showCollectionDesk(mode: String?): Boolean = getByType(mode).showFarmerProcurement

    fun showDeliveryRoutes(business: BusinessEntity?): Boolean {
        if (business == null) return true
        return business.destHouseholds
    }

    fun showDeliveryRoutes(mode: String?): Boolean = getByType(mode).showRetailRoutes

    fun showBilling(business: BusinessEntity?): Boolean {
        if (business == null) return true
        return business.destHouseholds || business.sourceVillageFarmers
    }

    fun showBilling(mode: String?): Boolean = getByType(mode).showBilling

    fun showBulkDispatches(business: BusinessEntity?): Boolean {
        if (business == null) return false
        return business.destCollectionCenter || business.destBulkCommercial || business.destFactoryTankers
    }

    fun showBulkDispatches(mode: String?): Boolean = getByType(mode).showBulkDispatches

    fun showRetailPos(business: BusinessEntity?): Boolean = false

    fun showRetailPos(mode: String?): Boolean = false

    // Feed vs Dairy Product Item Classification
    fun isFeedItem(itemName: String): Boolean {
        val lower = itemName.lowercase()
        return lower.contains("chokar") || lower.contains("chokkar") || lower.contains("feed") ||
               lower.contains("makka") || lower.contains("mustard") || lower.contains("cake") ||
               lower.contains("doc") || lower.contains("soya") || lower.contains("cotton") ||
               lower.contains("binola") || lower.contains("silage") || lower.contains("fodder") ||
               lower.contains("khal") || lower.contains("khali") || lower.contains("bran") ||
               lower.contains("straw") || lower.contains("bhusa") || lower.contains("bhoosa") ||
               lower.contains("mineral") || lower.contains("calcium") || lower.contains("supplement") ||
               lower.contains("grass") || lower.contains("grain") || lower.contains("pellet") ||
               lower.contains("kutty") || lower.contains("kutti") || lower.contains("churi") ||
               lower.contains("chuni") || lower.contains("pashu") || lower.contains("ahar") ||
               lower.contains("khurak") || lower.contains("dana") || lower.contains("sarson") ||
               itemName.contains("चोकर") || itemName.contains("चोक्खर") || itemName.contains("खली") ||
               itemName.contains("पशु आहार") || itemName.contains("दाना") || itemName.contains("चारा") ||
               itemName.contains("भूसा") || itemName.contains("साइलेज") || itemName.contains("घास") ||
               itemName.contains("ଦାନା") || itemName.contains("ଗୋ-ଖାଦ୍ୟ") || itemName.contains("କୁଣ୍ଡା") ||
               itemName.contains("ଖାଦ୍ୟ")
    }

    fun isDairyProductItem(itemName: String): Boolean = !isFeedItem(itemName)

    fun filterInventoryForMode(items: List<com.example.data.local.entity.InventoryItemEntity>, mode: String?): List<com.example.data.local.entity.InventoryItemEntity> {
        return items
    }

    fun getDefaultInventoryItemsForMode(mode: String?): List<Triple<String, String, Double>> = when (mode) {
        "TRADER", "COLLECTION_CENTER" -> listOf(
            Triple("Fresh Paneer (Cottage Cheese)", "kg", 360.0),
            Triple("Pure Desi Ghee", "litre", 650.0),
            Triple("Fresh Curd (Dahi)", "kg", 70.0),
            Triple("Chilled Buttermilk (Chaas)", "litre", 30.0),
            Triple("Raw Cow Milk (Bulk)", "litre", 40.0),
            Triple("Raw Buffalo Milk (Bulk)", "litre", 60.0)
        )
        "FARMER", "FARMER_WHOLESALE", "INTEGRATED", "CUSTOM" -> listOf(
            Triple("Fresh Paneer (Cottage Cheese)", "kg", 360.0),
            Triple("Pure Desi Ghee", "litre", 650.0),
            Triple("Fresh Curd (Dahi)", "kg", 70.0),
            Triple("Chilled Buttermilk (Chaas)", "litre", 30.0),
            Triple("Khoya / Mawa", "kg", 320.0),
            Triple("Makka", "kg", 24.0),
            Triple("Mustard Cake (Sarson Khal)", "kg", 32.0),
            Triple("Mustard DOC", "kg", 28.0),
            Triple("Soya Meal", "kg", 45.0),
            Triple("Cotton Cake / Binola Khal", "kg", 38.0),
            Triple("Chokar (Wheat Bran)", "kg", 22.0),
            Triple("Mineral Mixture", "kg", 120.0),
            Triple("Silage / Green Fodder", "kg", 8.0)
        )
        else -> listOf(
            Triple("Fresh Paneer (Cottage Cheese)", "kg", 360.0),
            Triple("Pure Desi Ghee", "litre", 650.0),
            Triple("Fresh Curd (Dahi)", "kg", 70.0),
            Triple("Makka", "kg", 24.0),
            Triple("Mustard Cake (Sarson Khal)", "kg", 32.0),
            Triple("Chokar (Wheat Bran)", "kg", 22.0),
            Triple("Mineral Mixture", "kg", 120.0)
        )
    }

    fun getExpenseCategoriesForMode(mode: String?): List<String> = when (mode) {
        "TRADER" -> listOf(
            "Farmer Milk Collection Outlay",
            "Route Vehicle Diesel & Fuel",
            "Vehicle Maintenance & Tyres",
            "Driver & Delivery Helper Wages",
            "Can Washing, Ice & Sanitation",
            "Chilling Center Fees",
            "Route Tolls & Taxes",
            "Other Trading Expense"
        )
        "COLLECTION_CENTER" -> listOf(
            "Farmer Milk Intake Outlay",
            "BMC Chilling Vat Electricity",
            "Testing Chemicals (Acid/Alcohol/Fat)",
            "Center Operator & Helper Salary",
            "Tanker Dispatch Loading / Freight",
            "BMC Servicing & Generator Diesel",
            "Other BMC Expense"
        )
        else -> listOf(
            "Cattle Feed (Khali/Pellets)",
            "Supplements & Minerals",
            "Veterinary Doctor & Meds",
            "Milker & Labor Salary",
            "Diesel & Delivery Fuel",
            "Electricity & Power",
            "Shed & Equipment Repair",
            "Packaging & Milk Pouches",
            "Other Farm Expense"
        )
    }

    // Milk Type Helpers
    fun isCowEnabled(supportedMilkTypes: String?): Boolean {
        return supportedMilkTypes != "BUFFALO_ONLY"
    }

    fun isBuffaloEnabled(supportedMilkTypes: String?): Boolean {
        return supportedMilkTypes != "COW_ONLY"
    }

    fun isMixedOrBoth(supportedMilkTypes: String?): Boolean {
        return supportedMilkTypes == "BOTH" || supportedMilkTypes == "MIXED"
    }
}
