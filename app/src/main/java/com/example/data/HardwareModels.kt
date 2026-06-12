package com.example.data

private val decimalFormatter = ThreadLocal.withInitial {
    java.text.DecimalFormat("#,###", java.text.DecimalFormatSymbols(java.util.Locale.US))
}

fun formatTl(amount: Double): String {
    val formatter = decimalFormatter.get() ?: java.text.DecimalFormat("#,###", java.text.DecimalFormatSymbols(java.util.Locale.US))
    return "$" + formatter.format(amount)
}

fun formatUsd(amount: Double): String {
    val formatter = decimalFormatter.get() ?: java.text.DecimalFormat("#,###", java.text.DecimalFormatSymbols(java.util.Locale.US))
    return formatter.format(amount)
}

enum class PartCategory(val displayName: String, val systemIconName: String) {
    CPU("Processor (CPU)", "cpu"),
    MOTHERBOARD("Motherboard", "dns"),
    RAM("Memory (RAM)", "memory"),
    GPU("Graphics Card (GPU)", "gradient"),
    PSU("Power Supply (PSU)", "bolt"),
    STORAGE("Storage (SSD/HDD)", "storage"),
    COOLER("Processor Cooler", "ac_unit"),
    CASE("Chassis (Case)", "widgets")
}

data class HardwarePart(
    val id: String,
    val category: PartCategory,
    val brand: String,
    val modelOrSpecs: String,
    val priceUsd: Double,
    val tdpWatts: Int,
    val socket: String? = null,    // "AM5", "LGA1700", "AM4", etc.
    val ramType: String? = null,   // "DDR4", "DDR5", etc.
    val details: String? = null,   // Extra parameters like "750W Gold", "1TB M.2 NVMe"
    val isCustom: Boolean = false  // Set to true if manually edited/typed by user
) {
    fun getPriceTl(): Double {
        return priceUsd
    }

    fun getFullName(): String {
        return if (isCustom) "$brand $modelOrSpecs (Custom Part)" else "$brand $modelOrSpecs"
    }
}

data class PresetBuild(
    val id: String,
    val name: String,
    val description: String,
    val budgetRange: String,
    val parts: Map<PartCategory, HardwarePart>
)

object HardwareDatabase {
    private var isInitialized = false
    private var jsonParts: Map<PartCategory, List<HardwarePart>> = emptyMap()

    fun initialize(context: android.content.Context) {
        if (isInitialized) return
        try {
            val partsMutable = mutableMapOf<PartCategory, MutableList<HardwarePart>>()
            
            val loadFile = { fileName: String ->
                try {
                    val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
                    val jsonArray = org.json.JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getString("id")
                        val categoryStr = obj.getString("category")
                        val category = PartCategory.valueOf(categoryStr)
                        val brand = obj.getString("brand")
                        val modelOrSpecs = obj.getString("modelOrSpecs")
                        val priceUsd = obj.getDouble("priceUsd")
                        val tdpWatts = obj.getInt("tdpWatts")
                        val socket = if (obj.isNull("socket")) null else obj.getString("socket")
                        val ramType = if (obj.isNull("ramType")) null else obj.getString("ramType")
                        val details = if (obj.isNull("details")) null else obj.getString("details")
                        
                        val part = HardwarePart(id, category, brand, modelOrSpecs, priceUsd, tdpWatts, socket, ramType, details)
                        partsMutable.getOrPut(category) { mutableListOf() }.add(part)
                    }
                } catch (inner: Exception) {
                    android.util.Log.e("HardwareDatabase", "Error loading file: $fileName", inner)
                }
            }

            loadFile("cpu_database.json")
            loadFile("gpus_database.json")
            loadFile("motherboards_database.json")
            loadFile("rams_database.json")
            loadFile("storages_database.json")
            loadFile("psus_database.json")
            loadFile("coolers_database.json")
            loadFile("cases_database.json")

            jsonParts = partsMutable
            isInitialized = true
        } catch (e: Exception) {
            android.util.Log.e("HardwareDatabase", "Error loading multi-part databases JSON", e)
        }
    }

    val predefinedParts: Map<PartCategory, List<HardwarePart>> = mapOf(
        PartCategory.CPU to listOf(
            HardwarePart("cpu_r5_7600", PartCategory.CPU, "AMD", "Ryzen 5 7600", 195.0, 65, socket = "AM5", details = "6 Çekirdek, 5.1GHz"),
            HardwarePart("cpu_r7_7800x3d", PartCategory.CPU, "AMD", "Ryzen 7 7800X3D", 389.0, 120, socket = "AM5", details = "8 Çekirdek, 3D V-Cache En İyi Oyun İşlemcisi"),
            HardwarePart("cpu_i5_14600k", PartCategory.CPU, "Intel", "Core i5-14600K", 300.0, 125, socket = "LGA1700", details = "14 Çekirdek, 5.3GHz"),
            HardwarePart("cpu_r5_5600", PartCategory.CPU, "AMD", "Ryzen 5 5600", 125.0, 65, socket = "AM4", details = "6 Çekirdek, Fiyat/Performas Şampiyonu"),
            HardwarePart("cpu_i3_12100f", PartCategory.CPU, "Intel", "Core i3-12100F", 85.0, 58, socket = "LGA1700", details = "4 Çekirdek, Bütçe Dostu")
        ),
        PartCategory.MOTHERBOARD to listOf(
            HardwarePart("mb_b650_pro", PartCategory.MOTHERBOARD, "MSI", "PRO B650M-A WiFi", 160.0, 0, socket = "AM5", ramType = "DDR5", details = "mATX, Wi-Fi 6E, PCIe 4.0"),
            HardwarePart("mb_b450_prime", PartCategory.MOTHERBOARD, "ASUS", "Prime B450M-K II", 75.0, 0, socket = "AM4", ramType = "DDR4", details = "mATX, Klasik Bütçe Anakartı"),
            HardwarePart("mb_b760_prime", PartCategory.MOTHERBOARD, "ASUS", "Prime B760M-K D4", 110.0, 0, socket = "LGA1700", ramType = "DDR4", details = "mATX, LGA1700 DDR4 Anakartı"),
            HardwarePart("mb_x670_tuf", PartCategory.MOTHERBOARD, "ASUS", "TUF Gaming X670E-Plus", 310.0, 0, socket = "AM5", ramType = "DDR5", details = "ATX, Üst Segment Overclock Anakartı"),
            HardwarePart("mb_h610_gigabyte", PartCategory.MOTHERBOARD, "Gigabyte", "H610M H V2", 80.0, 0, socket = "LGA1700", ramType = "DDR4", details = "LGA1700 DDR4 Taban Giriş Seviyesi")
        ),
        PartCategory.RAM to listOf(
            HardwarePart("ram_veng_ddr5_32", PartCategory.RAM, "Corsair", "Vengeance 32GB (2x16GB)", 115.0, 5, ramType = "DDR5", details = "6000MHz CL30 Intel XMP/AMD EXPO"),
            HardwarePart("ram_ripj_ddr4_16", PartCategory.RAM, "G.Skill", "Ripjaws V 16GB (2x8GB)", 40.0, 4, ramType = "DDR4", details = "3200MHz CL16 DDR4 Siyah Dual Kit"),
            HardwarePart("ram_fury_ddr5_16", PartCategory.RAM, "Kingston", "Fury Beast 16GB", 65.0, 5, ramType = "DDR5", details = "5200MHz CL40 Tek Modül"),
            HardwarePart("ram_veng_ddr4_16", PartCategory.RAM, "Corsair", "Vengeance LPX 16GB (1x16GB)", 45.0, 4, ramType = "DDR4", details = "3600MHz CL18 DDR4")
        ),
        PartCategory.GPU to listOf(
            HardwarePart("gpu_rtx_4070s", PartCategory.GPU, "NVIDIA", "GeForce RTX 4070 Super", 599.0, 220, details = "12GB GDDR6X, DLSS 3.0 Ray Tracing"),
            HardwarePart("gpu_rtx_4060ti", PartCategory.GPU, "NVIDIA", "GeForce RTX 4060 Ti", 389.0, 160, details = "8GB GDDR6, Fiyat / Performans RTX"),
            HardwarePart("gpu_rx_7800xt", PartCategory.GPU, "AMD", "Radeon RX 7800 XT", 499.0, 263, details = "16GB GDDR6, Güçlü Kas Belleği"),
            HardwarePart("gpu_rtx_4080s", PartCategory.GPU, "NVIDIA", "GeForce RTX 4080 Super", 999.0, 320, details = "16GB GDDR6X, 4K Canavarı"),
            HardwarePart("gpu_rx_6600", PartCategory.GPU, "AMD", "Radeon RX 6600", 200.0, 132, details = "8GB GDDR6, Bütçe Dostu 1080p Kartı")
        ),
        PartCategory.PSU to listOf(
            HardwarePart("psu_corsair_750", PartCategory.PSU, "Corsair", "RM750e 750W", 100.0, 0, details = "750W 80+ Gold Tam Modüler sessiz fan"),
            HardwarePart("psu_msi_650", PartCategory.PSU, "MSI", "MAG A650BN 650W", 65.0, 0, details = "650W 80+ Bronze Güvenilir Akım Koruması"),
            HardwarePart("psu_cm_850", PartCategory.PSU, "Cooler Master", "MWE 850W Gold", 120.0, 0, details = "850W 80+ Gold Güç Canavarı"),
            HardwarePart("psu_hp_500", PartCategory.PSU, "High Power", "500W Eco", 40.0, 0, details = "500W 80+ Aktif PFC Sade Tasarım")
        ),
        PartCategory.STORAGE to listOf(
            HardwarePart("st_sam_990_1tb", PartCategory.STORAGE, "Samsung", "990 Pro M.2 1TB", 110.0, 6, details = "7450MB/s Okuma, 6900MB/s Yazma Gen4 NVMe"),
            HardwarePart("st_king_nv2_1tb", PartCategory.STORAGE, "Kingston", "NV2 PCIe 4.0 NVMe 1TB", 60.0, 5, details = "3500MB/s Okuma bütçe dostu SSD"),
            HardwarePart("st_crucial_480", PartCategory.STORAGE, "Crucial", "BX500 SATA 480GB", 35.0, 4, details = "2.5 inç SATA standard SSD"),
            HardwarePart("st_wd_2tb", PartCategory.STORAGE, "WD", "Blue 2TB HDD", 65.0, 6, details = "7200 RPM 3.5 inç Mekanik Depolama")
        ),
        PartCategory.COOLER to listOf(
            HardwarePart("col_peerless_120", PartCategory.COOLER, "Thermalright", "Peerless Assassin 120 SE", 40.0, 5, socket = "AM5/LGA1700/AM4", details = "Çift Kule Tipi Dev Performanslı Hava Soğutma"),
            HardwarePart("col_deep_lt720", PartCategory.COOLER, "DeepCool", "LT720 360mm", 125.0, 8, socket = "AM5/LGA1700/AM4", details = "360mm Radyatör Sıvı Soğutma Infinite Ayna"),
            HardwarePart("col_default_amd", PartCategory.COOLER, "AMD", "Wraith Prism Stok Cooler", 15.0, 3, socket = "AM4/AM5", details = "İşlemciyle Gelen Temel RGB Fan"),
            HardwarePart("col_msi_240", PartCategory.COOLER, "MSI", "MAG Coreliquid 240R V2", 95.0, 7, socket = "AM5/LGA1700/AM4", details = "240mm Radyatör RGB Pompa Entegre")
        ),
        PartCategory.CASE to listOf(
            HardwarePart("cs_msi_forge", PartCategory.CASE, "MSI", "MAG FORGE 100M", 70.0, 0, details = "Mid-Tower Temperli Cam Mesh Izgara"),
            HardwarePart("cs_corsair_4000d", PartCategory.CASE, "Corsair", "4000D Airflow", 95.0, 0, details = "Mid-Tower Yüksek Kalite Havalandırma"),
            HardwarePart("cs_lianli_o11", PartCategory.CASE, "Lian Li", "O11 Dynamic EVO", 160.0, 0, details = "Akvaryum Tasarım Çift Odalı Lüks Kasa"),
            HardwarePart("cs_frisby_fc93", PartCategory.CASE, "Frisby", "FC-9320G ARGB", 55.0, 0, details = "4 ARGB Fanlı Hazır Fiyat/Performans Kasa")
        )
    )

    // Recommended Preset Builds
    val presetBuilds: List<PresetBuild> = listOf(
        PresetBuild(
            id = "preset_budget",
            name = "Entry-Level (Price / Performance)",
            description = "The absolute budget champion to smoothly play all modern games at 1080p resolution.",
            budgetRange = "$500 - $800",
            parts = mapOf(
                PartCategory.CPU to predefinedParts[PartCategory.CPU]!![3], // Ryzen 5 5600
                PartCategory.MOTHERBOARD to predefinedParts[PartCategory.MOTHERBOARD]!![1], // Prime B450
                PartCategory.RAM to predefinedParts[PartCategory.RAM]!![1], // Ripjaws DDR4 16GB
                PartCategory.GPU to predefinedParts[PartCategory.GPU]!![4], // RX 6600
                PartCategory.PSU to predefinedParts[PartCategory.PSU]!![3], // High Power 500W
                PartCategory.STORAGE to predefinedParts[PartCategory.STORAGE]!![1], // Kingston 1TB
                PartCategory.COOLER to predefinedParts[PartCategory.COOLER]!![2], // Stock Cooler
                PartCategory.CASE to predefinedParts[PartCategory.CASE]!![3] // Frisby case
            )
        ),
        PresetBuild(
            id = "preset_mid",
            name = "Mid-Range (Gaming, Streaming & Coding)",
            description = "AM5-powered beast with superior 1440p (2K) and Ray-Tracing/DLSS capabilities.",
            budgetRange = "$800 - $1,500",
            parts = mapOf(
                PartCategory.CPU to predefinedParts[PartCategory.CPU]!![0], // Ryzen 5 7600
                PartCategory.MOTHERBOARD to predefinedParts[PartCategory.MOTHERBOARD]!![0], // PRO B650
                PartCategory.RAM to predefinedParts[PartCategory.RAM]!![0], // Vengeance DDR5 32GB
                PartCategory.GPU to predefinedParts[PartCategory.GPU]!![1], // RTX 4060 Ti
                PartCategory.PSU to predefinedParts[PartCategory.PSU]!![1], // MSI 650W
                PartCategory.STORAGE to predefinedParts[PartCategory.STORAGE]!![1], // Kingston 1TB
                PartCategory.COOLER to predefinedParts[PartCategory.COOLER]!![0], // Peerless 120
                PartCategory.CASE to predefinedParts[PartCategory.CASE]!![0] // MSI Forge
            )
        ),
        PresetBuild(
            id = "preset_high",
            name = "High-End (Ultimate 4K Monster)",
            description = "Equipped with the fastest gaming processor and RTX 4080 Super for ultra premium enthusiast performance.",
            budgetRange = "$2,000+",
            parts = mapOf(
                PartCategory.CPU to predefinedParts[PartCategory.CPU]!![1], // Ryzen 7 7800X3D
                PartCategory.MOTHERBOARD to predefinedParts[PartCategory.MOTHERBOARD]!![3], // TUF X670E DDR5
                PartCategory.RAM to predefinedParts[PartCategory.RAM]!![0], // Vengeance DDR5 32GB
                PartCategory.GPU to predefinedParts[PartCategory.GPU]!![3], // RTX 4080 Super
                PartCategory.PSU to predefinedParts[PartCategory.PSU]!![2], // Cooler Master 850W
                PartCategory.STORAGE to predefinedParts[PartCategory.STORAGE]!![0], // Samsung 990 Pro 1TB
                PartCategory.COOLER to predefinedParts[PartCategory.COOLER]!![1], // DeepCool 360mm Liquid
                PartCategory.CASE to predefinedParts[PartCategory.CASE]!![2] // Lian Li O11 Dynamic
            )
        )
    )

    fun getAllPartsForCategory(category: PartCategory): List<HardwarePart> {
        val predefinedList = predefinedParts[category] ?: emptyList()
        if (!isInitialized) return predefinedList
        val jsonList = jsonParts[category] ?: emptyList()
        return (predefinedList + jsonList).distinctBy { "${it.brand} ${it.modelOrSpecs}".lowercase().replace(" ", "") }
    }

    // Offline Fallback resolution for Custom inputs described in requirement 4
    fun resolveOfflinePartData(query: String, category: PartCategory): HardwarePart? {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return null

        // 1. Instantly check JSON asset database if loaded
        if (isInitialized) {
            val list = jsonParts[category] ?: emptyList()
            // First check for exact full name match (case-insensitive)
            val exactMatch = list.firstOrNull { 
                val pFullName = "${it.brand} ${it.modelOrSpecs}".lowercase().replace(" ", "")
                val cleanQ = q.replace(" ", "")
                pFullName == cleanQ || it.modelOrSpecs.lowercase().replace(" ", "") == cleanQ
            }
            if (exactMatch != null) return exactMatch

            // Second check: check if either the query contains the database model, or the database model/full name contains the query.
            var bestMatch: HardwarePart? = null
            var bestScore = 0 // higher matches are preferred

            for (part in list) {
                val modelLower = part.modelOrSpecs.lowercase()
                val brandLower = part.brand.lowercase()
                val cleanModel = modelLower.replace(" ", "")
                val cleanQuery = q.replace(" ", "")

                val match = cleanQuery.contains(cleanModel) || 
                            cleanModel.contains(cleanQuery) || 
                            q.contains(modelLower) || 
                            modelLower.contains(q) ||
                            "${brandLower}${cleanModel}".contains(cleanQuery)

                if (match) {
                    val score = part.modelOrSpecs.length
                    if (score > bestScore) {
                        bestScore = score
                        bestMatch = part
                    }
                }
            }

            if (bestMatch != null) {
                return bestMatch
            }
        }

        // Try matching key phrases to supply ASUS, Corsair, MSI, AMD, etc. dynamic details
        return when (category) {
            PartCategory.CPU -> {
                if (q.contains("7800x3d")) {
                    HardwarePart("offline_7800x3d", PartCategory.CPU, "AMD", "Ryzen 7 7800X3D", 389.0, 120, socket = "AM5")
                } else if (q.contains("7600")) {
                    HardwarePart("offline_7600", PartCategory.CPU, "AMD", "Ryzen 5 7600", 195.0, 65, socket = "AM5")
                } else if (q.contains("14600")) {
                    HardwarePart("offline_14600k", PartCategory.CPU, "Intel", "Core i5-14600K", 300.0, 125, socket = "LGA1700")
                } else if (q.contains("5600")) {
                    HardwarePart("offline_5600", PartCategory.CPU, "AMD", "Ryzen 5 5600", 125.0, 65, socket = "AM4")
                } else if (q.contains("12100")) {
                    HardwarePart("offline_12100", PartCategory.CPU, "Intel", "Core i3-12100F", 85.0, 58, socket = "LGA1700")
                } else if (q == "intel" || q == "intel cpu" || q == "intel işlemci") {
                    HardwarePart("offline_intel_generic", PartCategory.CPU, "Intel", "Core i5-13400F", 190.0, 65, socket = "LGA1700")
                } else if (q == "amd" || q == "ryzen" || q == "amd cpu" || q == "amd işlemci") {
                    HardwarePart("offline_amd_generic", PartCategory.CPU, "AMD", "Ryzen 5 5500", 95.0, 65, socket = "AM4")
                } else {
                    null
                }
            }
            PartCategory.MOTHERBOARD -> {
                if (q.contains("b650")) {
                    HardwarePart("offline_b650", PartCategory.MOTHERBOARD, "MSI", "PRO B650M-A WiFi", 160.0, 0, socket = "AM5", ramType = "DDR5")
                } else if (q.contains("b450")) {
                    HardwarePart("offline_b450", PartCategory.MOTHERBOARD, "ASUS", "Prime B450M-K II", 75.0, 0, socket = "AM4", ramType = "DDR4")
                } else if (q.contains("b760")) {
                    HardwarePart("offline_b760", PartCategory.MOTHERBOARD, "ASUS", "Prime B760-Plus DDR5", 140.0, 0, socket = "LGA1700", ramType = "DDR5")
                } else if (q.contains("x670")) {
                    HardwarePart("offline_x670", PartCategory.MOTHERBOARD, "ASUS", "TUF Gaming X670E-Plus", 310.0, 0, socket = "AM5", ramType = "DDR5")
                } else if (q.contains("h610")) {
                    HardwarePart("offline_h610", PartCategory.MOTHERBOARD, "Gigabyte", "H610M H V2", 80.0, 0, socket = "LGA1700", ramType = "DDR4")
                } else {
                    null
                }
            }
            PartCategory.RAM -> {
                if (q.contains("ddr5")) {
                    HardwarePart("offline_ram_ddr5", PartCategory.RAM, "Corsair", "Vengeance DDR5 16GB", 60.0, 5, ramType = "DDR5")
                } else if (q.contains("ddr4")) {
                    HardwarePart("offline_ram_ddr4", PartCategory.RAM, "Kingston", "Fury Beast DDR4 16GB", 40.0, 4, ramType = "DDR4")
                } else {
                    null
                }
            }
            PartCategory.GPU -> {
                if (q.contains("5090")) {
                    HardwarePart("offline_gpu_5090", PartCategory.GPU, "NVIDIA", "GeForce RTX 5090 32GB", 1999.0, 600, details = "32GB GDDR7, Ultra High-End GPU")
                } else if (q.contains("5080")) {
                    HardwarePart("offline_gpu_5080", PartCategory.GPU, "NVIDIA", "GeForce RTX 5080 16GB", 1199.0, 400, details = "16GB GDDR7, Next-Gen Performance")
                } else if (q.contains("5070")) {
                    HardwarePart("offline_gpu_5070", PartCategory.GPU, "NVIDIA", "GeForce RTX 5070 12GB", 649.0, 250, details = "12GB GDDR7, Next-Gen High Quality")
                } else if (q.contains("4090")) {
                    HardwarePart("offline_gpu_4090", PartCategory.GPU, "MSI", "GeForce RTX 4090 24GB Trio", 1799.0, 450)
                } else if (q.contains("4080")) {
                    HardwarePart("offline_gpu_4080", PartCategory.GPU, "NVIDIA", "GeForce RTX 4080 Super", 999.0, 320)
                } else if (q.contains("4070")) {
                    HardwarePart("offline_gpu_4070", PartCategory.GPU, "ASUS", "Dual GeForce RTX 4070 Super", 599.0, 220)
                } else if (q.contains("4060")) {
                    HardwarePart("offline_gpu_4060", PartCategory.GPU, "GIGABYTE", "GeForce RTX 4060 WindForce", 299.0, 115)
                } else if (q.contains("4050")) {
                    HardwarePart("offline_gpu_4050", PartCategory.GPU, "NVIDIA", "GeForce RTX 4050", 199.0, 95, details = "6GB GDDR6, Fiyat/Performans Bütçe Kartı")
                } else if (q.contains("7800")) {
                    HardwarePart("offline_gpu_7800", PartCategory.GPU, "AMD", "Radeon RX 7800 XT", 499.0, 263)
                } else if (q.contains("7700")) {
                    HardwarePart("offline_gpu_7700", PartCategory.GPU, "ASUS", "TUF Radeon RX 7700 XT", 419.0, 245)
                } else if (q.contains("6600")) {
                    HardwarePart("offline_gpu_6600", PartCategory.GPU, "AMD", "Radeon RX 6600", 200.0, 132)
                } else if (q == "nvidia" || q == "rtx" || q == "nvidia gpu" || q == "nvidia ekran kartı") {
                    HardwarePart("offline_gpu_rtx_gen", PartCategory.GPU, "NVIDIA", "GeForce RTX 3060 12GB", 280.0, 170)
                } else if (q == "amd" || q == "rx" || q == "amd gpu" || q == "amd ekran kartı") {
                    HardwarePart("offline_gpu_rx_gen", PartCategory.GPU, "AMD", "Radeon RX 7600", 260.0, 165)
                } else {
                    null
                }
            }
            PartCategory.PSU -> {
                if (q.contains("850")) {
                    HardwarePart("offline_psu_850", PartCategory.PSU, "Cooler Master", "MWE Gold 850W v2", 120.0, 0, details = "850W Gold")
                } else if (q.contains("750")) {
                    HardwarePart("offline_psu_750", PartCategory.PSU, "Corsair", "RM750e 750W", 100.0, 0, details = "750W Gold")
                } else if (q.contains("650")) {
                    HardwarePart("offline_psu_650", PartCategory.PSU, "MSI", "MAG A650BN 650W", 65.0, 0, details = "650W Bronze")
                } else if (q.contains("500")) {
                    HardwarePart("offline_psu_500", PartCategory.PSU, "High Power", "500W Eco", 40.0, 0, details = "500W Eco")
                } else {
                    null
                }
            }
            PartCategory.STORAGE -> {
                if (q.contains("990")) {
                    HardwarePart("offline_storage_990", PartCategory.STORAGE, "Samsung", "990 Pro PCIe 4.0 1TB", 110.0, 6, details = "NVMe M.2")
                } else if (q == "ssd" || q == "m.2" || q == "m2 ssd" || q == "disk") {
                    HardwarePart("offline_storage_ssd", PartCategory.STORAGE, "Kingston", "NV2 NVMe M.2 SSD 1TB", 60.0, 5, details = "NVMe M.2")
                } else {
                    null
                }
            }
            PartCategory.COOLER -> {
                if (q.contains("peerless") || q.contains("assassin")) {
                    HardwarePart("offline_cooler_air", PartCategory.COOLER, "Thermalright", "Peerless Assassin 120", 40.0, 5, socket = "AM5/LGA1700/AM4")
                } else if (q.contains("lt720") || q.contains("360")) {
                    HardwarePart("offline_cooler_liquid_360", PartCategory.COOLER, "DeepCool", "LT720 360mm Sıvı Soğutma", 125.0, 8, socket = "AM5/LGA1700/AM4")
                } else if (q.contains("240") || q.contains("coreliquid")) {
                    HardwarePart("offline_cooler_liquid_240", PartCategory.COOLER, "MSI", "MAG Coreliquid 240R V2", 95.0, 7, socket = "AM5/LGA1700/AM4")
                } else if (q == "sıvı" || q == "sıvı soğutma" || q == "360mm" || q == "water cooler") {
                    HardwarePart("offline_cooler_liquid", PartCategory.COOLER, "DeepCool", "LT720 360mm Sıvı Soğutma", 125.0, 8, socket = "AM5/LGA1700/AM4")
                } else if (q == "hava" || q == "hava soğutma" || q == "air cooler" || q == "stok" || q == "stock") {
                    HardwarePart("offline_cooler_air", PartCategory.COOLER, "Thermalright", "Peerless Assassin 120", 40.0, 5, socket = "AM5/LGA1700/AM4")
                } else {
                    null
                }
            }
            PartCategory.CASE -> {
                if (q.contains("4000d")) {
                    HardwarePart("offline_case_corsair", PartCategory.CASE, "Corsair", "4000D Airflow ATX", 95.0, 0, details = "Mesh ATX")
                } else if (q.contains("forge")) {
                    HardwarePart("offline_case_msi", PartCategory.CASE, "MSI", "MAG FORGE 100M", 70.0, 0, details = "Mid-Tower Mesh")
                } else if (q.contains("o11") || q.contains("dynamic")) {
                    HardwarePart("offline_case_lianli", PartCategory.CASE, "Lian Li", "O11 Dynamic EVO", 160.0, 0, details = "Akvaryum Tasarım Çift Odalı Lüks Kasa")
                } else if (q.contains("frisby")) {
                    HardwarePart("offline_case_frisby", PartCategory.CASE, "Frisby", "FC-9320G ARGB", 55.0, 0, details = "4 ARGB Fanlı Hazır Kasa")
                } else if (q == "kasa" || q == "case" || q == "pc case" || q == "computer case") {
                    HardwarePart("offline_case_generic", PartCategory.CASE, "Corsair", "4000D Airflow ATX", 95.0, 0, details = "Mesh ATX")
                } else {
                    null
                }
            }
        }
    }
}
