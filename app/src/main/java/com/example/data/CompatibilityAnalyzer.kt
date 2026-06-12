package com.example.data

data class CompatibilityReport(
    val hasWarnings: Boolean,
    val socketWarning: String? = null,
    val ramWarning: String? = null,
    val coolerWarning: String? = null,
    val gpuPsuWarning: String? = null,
    val caseWarning: String? = null,
    val psuReport: PsuWarningReport? = null,
    val warningsList: List<String> = emptyList()
)

data class PsuWarningReport(
    val totalTdp: Int,
    val psuCapacity: Int,
    val loadPercentage: Int,
    val isOverloaded: Boolean,
    val isNearLimit: Boolean, // > 90% boundary
    val warningText: String? = null
)

object CompatibilityAnalyzer {

    fun getGpuMinRecommendedPsu(gpu: HardwarePart): Int {
        val name = "${gpu.brand} ${gpu.modelOrSpecs} ${gpu.details ?: ""}".lowercase()
        return when {
            name.contains("4090") || name.contains("5090") -> 850
            name.contains("4080") || name.contains("5080") -> 750
            name.contains("4070") || name.contains("7800") || gpu.tdpWatts >= 200 -> 650
            name.contains("4060") || name.contains("7700") || gpu.tdpWatts >= 150 -> 550
            gpu.tdpWatts >= 100 -> 500
            else -> 450
        }
    }

    fun analyzeParts(parts: Map<PartCategory, HardwarePart>): CompatibilityReport {
        val cpu = parts[PartCategory.CPU]
        val motherboard = parts[PartCategory.MOTHERBOARD]
        val ram = parts[PartCategory.RAM]
        val psu = parts[PartCategory.PSU]
        val cooler = parts[PartCategory.COOLER]
        val gpu = parts[PartCategory.GPU]
        val case = parts[PartCategory.CASE]

        val listWarnings = mutableListOf<String>()

        // 1. Socket compatibility between CPU and Motherboard
        var socketWarning: String? = null
        if (cpu != null && motherboard != null) {
            val cpuSocket = cpu.socket?.trim()
            val mbSocket = motherboard.socket?.trim()
            
            if (!cpuSocket.isNullOrEmpty() && !mbSocket.isNullOrEmpty()) {
                if (!cpuSocket.equals(mbSocket, ignoreCase = true)) {
                    socketWarning = "Socket Incompatibility: Processor socket ($cpuSocket) and Motherboard socket ($mbSocket) do not match! Physical installation is impossible."
                    listWarnings.add("❌ $socketWarning")
                }
            }
        }

        // 2. Memory generation compatibility
        var ramWarning: String? = null
        if (motherboard != null && ram != null) {
            val mbRam = motherboard.ramType?.trim()
            val ramTypeDesc = ram.ramType?.trim()

            if (!mbRam.isNullOrEmpty() && !ramTypeDesc.isNullOrEmpty()) {
                if (!mbRam.equals(ramTypeDesc, ignoreCase = true)) {
                    ramWarning = "Memory Type Incompatibility: Motherboard supports $mbRam RAM generation, but selected RAM is $ramTypeDesc! System will not boot."
                    listWarnings.add("❌ $ramWarning")
                }
            }
        }

        // 3. Cooler and CPU Overheating compatibility
        var coolerWarning: String? = null
        if (cpu != null) {
            val isHighEndCpu = cpu.tdpWatts >= 100 || cpu.modelOrSpecs.contains("K", ignoreCase = true) || cpu.modelOrSpecs.contains("X3D", ignoreCase = true)
            if (cooler == null) {
                if (isHighEndCpu) {
                    coolerWarning = "High Thermal Load Warning: Selected high-performance CPU (${cpu.modelOrSpecs}) runs hot and requires a dedicated cooler (none included in the box). Please add a dedicated Liquid or Air Cooler."
                    listWarnings.add("⚠️ $coolerWarning")
                }
            } else {
                val isStockCooler = cooler.brand.lowercase().contains("amd") && cooler.modelOrSpecs.lowercase().contains("stok")
                if (isHighEndCpu && isStockCooler) {
                    coolerWarning = "Thermal Throttling Risk: You paired a heavy CPU (${cpu.modelOrSpecs}) with a baseline stock cooler! This will cause overheating and heavy thermal throttling. Upgrading to a tower air or liquid cooler is highly recommended."
                    listWarnings.add("⚠️ $coolerWarning")
                } else {
                    val cpuSocket = cpu.socket?.trim()
                    val coolerSockets = cooler.socket?.trim()
                    if (!cpuSocket.isNullOrEmpty() && !coolerSockets.isNullOrEmpty()) {
                        val supportedSockets = coolerSockets.lowercase().split(Regex("[/,\\-\\s]+")).map { it.trim() }
                        val cleanCpuSocket = cpuSocket.lowercase().trim()
                        val isCompatible = supportedSockets.any { supported -> 
                            cleanCpuSocket.contains(supported) || supported.contains(cleanCpuSocket)
                        }
                        if (!isCompatible) {
                            coolerWarning = "Cooler Socket Incompatibility: Processor socket ($cpuSocket) is not supported by the selected cooler! (Supported: $coolerSockets)"
                            listWarnings.add("❌ $coolerWarning")
                        }
                    }
                }
            }
        }

        // 4. Motherboard vs Case form factor compatibility
        var caseWarning: String? = null
        if (motherboard != null && case != null) {
            val mbDetails = "${motherboard.modelOrSpecs} ${motherboard.details ?: ""}".lowercase()
            val caseDetails = "${case.modelOrSpecs} ${case.details ?: ""}".lowercase()
            
            val isMbAtx = mbDetails.contains("atx") && !mbDetails.contains("matx") && !mbDetails.contains("micro-atx")
            val isCaseMatxOnly = (caseDetails.contains("matx") || caseDetails.contains("micro") || caseDetails.contains("itx") || caseDetails.contains("mini")) 
                    && !caseDetails.contains("mid") && !caseDetails.contains("full") && !caseDetails.contains("atx")

            if (isMbAtx && isCaseMatxOnly) {
                caseWarning = "Chassis Incompatibility: Selected ATX motherboard will not fit inside a micro-ATX chassis! Please pick a Mid-Tower/ATX case or an mATX motherboard."
                listWarnings.add("❌ $caseWarning")
            }
        }

        // 5. Power calculation and PSU safety test
        val totalTdp = parts.filter { it.key != PartCategory.PSU }.values.sumOf { it.tdpWatts }
        val psuCapacity = getPsuCapacity(psu)
        var psuReport: PsuWarningReport? = null

        var gpuPsuWarning: String? = null
        if (gpu != null && psu != null && psuCapacity > 0) {
            val minRecommendedPsu = getGpuMinRecommendedPsu(gpu)
            if (psuCapacity < minRecommendedPsu) {
                gpuPsuWarning = "GPU Power Requirement: Picked GPU (${gpu.brand} ${gpu.modelOrSpecs}) demands at least a ${minRecommendedPsu}W PSU. Your current PSU (${psuCapacity}W) falls short."
                listWarnings.add("⚡ $gpuPsuWarning")
            }
        }

        if (psu != null && psuCapacity > 0) {
            val percentage = ((totalTdp.toDouble() / psuCapacity) * 100).toInt()
            val isOverloaded = totalTdp > psuCapacity
            val isNearLimit = percentage >= 90

            val warningText = when {
                isOverloaded -> "Power Limit Exceeded: Total system TDP limit (${totalTdp}W) exceeds your power supply capacity (${psuCapacity}W)! System will shutdown under load."
                isNearLimit -> "Power Load Limit Alert (%$percentage): System draws ${totalTdp}W, dangerously close to safe PSU boundaries! A PSU of at least ${(totalTdp * 1.25).toInt()}W is suggested."
                else -> null
            }

            if (warningText != null) {
                listWarnings.add("⚡ $warningText")
            }

            psuReport = PsuWarningReport(
                totalTdp = totalTdp,
                psuCapacity = psuCapacity,
                loadPercentage = percentage,
                isOverloaded = isOverloaded,
                isNearLimit = isNearLimit,
                warningText = warningText
            )
        } else if (totalTdp > 0) {
            val warningText = "System Draw: ${totalTdp}W. Please assign a Power Supply (PSU) with sufficient capacity to operate the build."
            listWarnings.add("⚡ $warningText")
            psuReport = PsuWarningReport(
                totalTdp = totalTdp,
                psuCapacity = 0,
                loadPercentage = 0,
                isOverloaded = false,
                isNearLimit = false,
                warningText = warningText
            )
        }

        return CompatibilityReport(
            hasWarnings = listWarnings.isNotEmpty(),
            socketWarning = socketWarning,
            ramWarning = ramWarning,
            coolerWarning = coolerWarning,
            gpuPsuWarning = gpuPsuWarning,
            caseWarning = caseWarning,
            psuReport = psuReport,
            warningsList = listWarnings
        )
    }

    fun getPsuCapacity(psu: HardwarePart?): Int {
        if (psu == null) return 0
        if (psu.isCustom && psu.tdpWatts > 0) {
            return psu.tdpWatts
        }
        val textToSearch = "${psu.modelOrSpecs} ${psu.details ?: ""}".lowercase()
        val regexPatterns = listOf(
            Regex("(\\d+)\\s*w"),
            Regex("(\\d+)\\s*watt")
        )
        for (pattern in regexPatterns) {
            val match = pattern.find(textToSearch)
            if (match != null) {
                return match.groupValues[1].toIntOrNull() ?: 500
            }
        }
        return 500
    }
}
