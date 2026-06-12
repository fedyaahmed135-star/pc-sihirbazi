package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.api.VerificationResult
import com.example.api.ForumSummaryItem
import com.example.api.BenchmarkCompareResult
import com.example.api.GeminiSearchService
import com.example.data.HardwareDatabase
import com.example.data.HardwarePart
import com.example.data.PartCategory
import com.example.data.PresetBuild
import com.example.ui.theme.ThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

data class CustomInputState(
    val isEnabled: Boolean = false,
    val brand: String = "",
    val model: String = "",
    val priceUsdText: String = "",
    val tdpWattsText: String = "",
    val socket: String = "",
    val ramType: String = ""
)

data class BuilderUIState(
    val activeTab: Int = 0, // 0 = Build, 1 = Gaming, 2 = Budget, 3 = Comparator
    val selectedParts: Map<PartCategory, HardwarePart> = emptyMap(),
    val customInputs: Map<PartCategory, CustomInputState> = emptyMap(),
    val verificationLoading: Map<PartCategory, Boolean> = emptyMap(),
    val verificationMessages: Map<PartCategory, String> = emptyMap(),
    val verificationSources: Map<PartCategory, String> = emptyMap(),
    val selectedResolution: Int = 0, // 0 = 1080p, 1 = 2K, 2 = 4K
    val consoleLog: List<String> = emptyList(),
    val useOfflineOnly: Boolean = false,
    val showForumDialog: Boolean = false,
    val forumLoading: Boolean = false,
    val forumPartName: String = "",
    val forumCategory: PartCategory? = null,
    val forumSummaries: List<ForumSummaryItem> = emptyList(),
    val forumError: String? = null,
    val selectedTheme: ThemeOption = ThemeOption.COSMIC_AMBER,
    val targetBudgetUsd: Double = 1500.0,
    val benchmarkLoading: Boolean = false,
    val benchmarkResult: BenchmarkCompareResult? = null,
    val benchmarkError: String? = null,
    val budgetSearchLoading: Boolean = false,
    val budgetSourceOptions: List<com.example.api.BudgetSourceOption> = emptyList(),
    val budgetSearchError: String? = null,
    val selectedCountry: String = "USA"
)

class PCBuilderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BuilderUIState())
    val uiState: StateFlow<BuilderUIState> = _uiState.asStateFlow()

    init {
        // Initialize with typical empty custom states
        val initialCustoms = PartCategory.values().associateWith { CustomInputState() }
        _uiState.update { it.copy(customInputs = initialCustoms) }
        
        // Auto load Giriş Seviyesi build on first boot to welcome user
        loadPresetBuild(HardwareDatabase.presetBuilds[0])
        addLog("Application successfully started. Default entry-level setup loaded.")
    }

    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectCountry(country: String) {
        _uiState.update { it.copy(selectedCountry = country) }
        addLog("Location changed to: $country")
    }

    fun selectTheme(theme: ThemeOption) {
        _uiState.update { it.copy(selectedTheme = theme) }
        addLog("System color theme updated: ${theme.displayName}")
    }

    fun setTargetBudgetUsd(budget: Double) {
        _uiState.update { it.copy(targetBudgetUsd = budget) }
        addLog("Target budget updated: $${budget.toInt()}")
    }

    fun selectPartFromDatabase(category: PartCategory, part: HardwarePart) {
        _uiState.update { state ->
            val updatedParts = state.selectedParts.toMutableMap()
            updatedParts[category] = part
            
            // Disable custom inputs since user selected from predefined
            val updatedCustoms = state.customInputs.toMutableMap()
            updatedCustoms[category] = CustomInputState(isEnabled = false)

            state.copy(
                selectedParts = updatedParts,
                customInputs = updatedCustoms
            )
        }
        addLog("Predefined part selected for ${category.displayName}: ${part.brand} ${part.modelOrSpecs}")
    }

    /**
     * Toggles "+ Kendin Yaz" Custom Input form for a specific category
     */
    fun toggleCustomInput(category: PartCategory) {
        _uiState.update { state ->
            val currentCustom = state.customInputs[category] ?: CustomInputState()
            val nextEnabled = !currentCustom.isEnabled
            
            val updatedCustoms = state.customInputs.toMutableMap()
            
            // Generate some initial values based on current selection to save user typing
            val existing = state.selectedParts[category]
            val initialCustom = if (nextEnabled) {
                CustomInputState(
                    isEnabled = true,
                    brand = existing?.brand ?: "",
                    model = existing?.modelOrSpecs ?: "",
                    priceUsdText = existing?.priceUsd?.toInt()?.toString() ?: "",
                    tdpWattsText = existing?.tdpWatts?.toString() ?: "",
                    socket = existing?.socket ?: "",
                    ramType = existing?.ramType ?: ""
                )
            } else {
                CustomInputState(isEnabled = false)
            }
            updatedCustoms[category] = initialCustom

            val updatedParts = state.selectedParts.toMutableMap()
            if (nextEnabled) {
                // Instantly sync simulated custom config to active parts model (Kritik Kural)
                val customPart = convertInputToPart(category, initialCustom)
                updatedParts[category] = customPart
            } else {
                // Remove custom and pull standard back or leave empty
                val fallbackPart = HardwareDatabase.predefinedParts[category]?.firstOrNull()
                if (fallbackPart != null) {
                    updatedParts[category] = fallbackPart
                }
            }

            state.copy(
                customInputs = updatedCustoms,
                selectedParts = updatedParts
            )
        }
        addLog("${category.displayName} custom write mode ${if (uiState.value.customInputs[category]?.isEnabled == true) "enabled" else "disabled"}.")
    }

    /**
     * Called in real-time as user types in "+ Kendin Yaz" fields (Kritik Kural)
     */
    fun updateCustomInputField(category: PartCategory, updateBlock: (CustomInputState) -> CustomInputState) {
        _uiState.update { state ->
            val currentInput = state.customInputs[category] ?: CustomInputState()
            val newInput = updateBlock(currentInput)
            
            val updatedCustoms = state.customInputs.toMutableMap()
            updatedCustoms[category] = newInput

            // Auto-sync value instantly to selectedParts list for real-time calculations (Kritik Kural)
            val updatedParts = state.selectedParts.toMutableMap()
            val customPart = convertInputToPart(category, newInput)
            updatedParts[category] = customPart

            state.copy(
                customInputs = updatedCustoms,
                selectedParts = updatedParts
            )
        }
    }

    /**
     * Calls search crawler or resolves via local database to verify details online or offline
     */
    fun runInternetVerifyField(category: PartCategory) {
        val customInput = uiState.value.customInputs[category] ?: return
        val queryText = "${customInput.brand} ${customInput.model}".trim()
        
        if (queryText.isEmpty() || queryText == " ") return

        _uiState.update { state ->
            val loadings = state.verificationLoading.toMutableMap()
            loadings[category] = true
            state.copy(verificationLoading = loadings)
        }
        addLog("Verification initialized for ${category.displayName}: \"$queryText\"")

        viewModelScope.launch {
            try {
                val result = GeminiClient.verifyPart(queryText, category, uiState.value.useOfflineOnly)
                
                _uiState.update { state ->
                    val loadings = state.verificationLoading.toMutableMap()
                    loadings[category] = false

                    val messages = state.verificationMessages.toMutableMap()
                    messages[category] = result.summary

                    val sources = state.verificationSources.toMutableMap()
                    sources[category] = result.source

                    val parts = state.selectedParts.toMutableMap()
                    val customs = state.customInputs.toMutableMap()

                    if (result.source != "Doğrulama Başarısız") {
                        parts[category] = result.verifiedPart
                        // Sync verified fields back to UI input fields with full corrected name combined in model
                        customs[category] = CustomInputState(
                            isEnabled = true,
                            brand = "",
                            model = "${result.verifiedPart.brand} ${result.verifiedPart.modelOrSpecs}".trim(),
                            priceUsdText = result.verifiedPart.priceUsd.toInt().toString(),
                            tdpWattsText = result.verifiedPart.tdpWatts.toString(),
                            socket = result.verifiedPart.socket ?: "",
                            ramType = result.verifiedPart.ramType ?: ""
                        )
                    } else {
                        // Verification failed: do not add/keep the invalid hardware in selectedParts
                        parts.remove(category)
                        val current = customs[category]
                        customs[category] = current?.copy(
                            brand = "",
                            priceUsdText = "",
                            tdpWattsText = "",
                            socket = "",
                            ramType = ""
                        ) ?: CustomInputState(isEnabled = true, model = queryText)
                    }

                    state.copy(
                        verificationLoading = loadings,
                        verificationMessages = messages,
                        verificationSources = sources,
                        selectedParts = parts,
                        customInputs = customs
                    )
                }
                addLog("Verification Complete (${result.source}): ${result.verifiedPart.brand} ${result.verifiedPart.modelOrSpecs}")
            } catch (e: Exception) {
                _uiState.update { state ->
                    val loadings = state.verificationLoading.toMutableMap()
                    loadings[category] = false
                    state.copy(verificationLoading = loadings)
                }
                addLog("An error occurred during verification: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Toggles Verification Engine (Gemini AI vs Offline engine)
     */
    fun setOfflineOnlyMode(enabled: Boolean) {
        _uiState.update { it.copy(useOfflineOnly = enabled) }
        addLog("System verification engine changed to: ${if (enabled) "Fast Offline Emulator" else "Real-time AI Crawler"}")
    }

    /**
     * Triggers Forum Summary fetching and compilation for the selected part.
     */
    fun showForumSummariesForPart(category: PartCategory, part: HardwarePart) {
        val queryText = "${part.brand} ${part.modelOrSpecs}".replace("(Özel Donanım)", "").trim()
        _uiState.update { state ->
            state.copy(
                showForumDialog = true,
                forumLoading = true,
                forumPartName = "${part.brand} ${part.modelOrSpecs}",
                forumCategory = category,
                forumSummaries = emptyList(),
                forumError = null
            )
        }
        addLog("Forum summaries compilation started for ${category.displayName} (${part.brand} ${part.modelOrSpecs})...")

        viewModelScope.launch {
            try {
                val summaries = GeminiClient.fetchForumSummaries(
                    query = queryText,
                    category = category,
                    useOfflineOnly = uiState.value.useOfflineOnly
                )
                _uiState.update { state ->
                    state.copy(
                        forumLoading = false,
                        forumSummaries = summaries,
                        forumError = if (summaries.isEmpty()) "No forum discussions found for this part." else null
                    )
                }
                addLog("Forum summaries loaded successfully. Compiled ${summaries.size} threads.")
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        forumLoading = false,
                        forumError = "Error loading forum files: ${e.localizedMessage}"
                    )
                }
                addLog("Forum retrieval error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Dismisses the forum summaries dialog
     */
    fun dismissForumDialog() {
        _uiState.update { it.copy(showForumDialog = false) }
    }

    fun setResolution(resIndex: Int) {
        _uiState.update { it.copy(selectedResolution = resIndex) }
        addLog("Gaming target resolution modified to: ${if (resIndex == 0) "1080p Full HD" else if (resIndex == 1) "1440p (2K)" else "4K UHD Ultimate"}")
    }

    fun loadPresetBuild(build: PresetBuild) {
        _uiState.update { state ->
            // Clear custom mode and load standard items
            val updatedCustoms = state.customInputs.toMutableMap()
            PartCategory.values().forEach { cat ->
                updatedCustoms[cat] = CustomInputState(isEnabled = false)
            }
            
            // Load preset parts directly
            state.copy(
                selectedParts = build.parts,
                customInputs = updatedCustoms,
                verificationMessages = emptyMap(),
                verificationSources = emptyMap()
            )
        }
        addLog("Preset system loaded: ${build.name}")
    }

    fun removePart(category: PartCategory) {
        _uiState.update { state ->
            val parts = state.selectedParts.toMutableMap()
            parts.remove(category)
            state.copy(selectedParts = parts)
        }
        addLog("${category.displayName} removed from setup.")
    }

    private fun convertInputToPart(category: PartCategory, input: CustomInputState): HardwarePart {
        val price = input.priceUsdText.toDoubleOrNull() ?: 0.0
        val tdp = input.tdpWattsText.toIntOrNull() ?: 0
        return HardwarePart(
            id = "custom_" + category.name,
            category = category,
            brand = input.brand.ifEmpty { "Custom" },
            modelOrSpecs = input.model.ifEmpty { "Specification Not Specified" },
            priceUsd = price,
            tdpWatts = tdp,
            socket = input.socket.ifEmpty { null },
            ramType = input.ramType.ifEmpty { null },
            isCustom = true
        )
    }

    fun clearBenchmarkResult() {
        _uiState.update { it.copy(benchmarkResult = null, benchmarkError = null) }
    }

    fun fetchRealtimeBenchmark(partA: HardwarePart, partB: HardwarePart, category: PartCategory) {
        _uiState.update { it.copy(benchmarkLoading = true, benchmarkResult = null, benchmarkError = null) }
        addLog("Live benchmark analysis initialized for ${category.displayName}: \"${partA.brand} ${partA.modelOrSpecs}\" vs \"${partB.brand} ${partB.modelOrSpecs}\"")

        viewModelScope.launch {
            try {
                val result = if (uiState.value.useOfflineOnly) {
                    com.example.api.GeminiSearchService.getLocalFallbackBenchmark(partA, partB, category)
                } else {
                    GeminiSearchService.compareWithRealtimeSearch(partA, partB, category)
                }
                _uiState.update { state ->
                    state.copy(
                        benchmarkLoading = false,
                        benchmarkResult = result,
                        benchmarkError = null
                    )
                }
                addLog("Comparison Completed [${if (result.isRealtime) "Live Google Research" else "Smart Engine Summary"}]: ${result.winner} is leading.")
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        benchmarkLoading = false,
                        benchmarkError = "An error occurred retrieving comparison data: ${e.localizedMessage}"
                    )
                }
                addLog("Live comparison error: ${e.localizedMessage}")
            }
        }
    }

    fun fetchRealtimeBudgetOptions(budgetUsd: Double) {
        _uiState.update { it.copy(budgetSearchLoading = true, budgetSourceOptions = emptyList(), budgetSearchError = null) }
        addLog("Live market analysis initiated for \$${budgetUsd.toInt()} budget...")

        viewModelScope.launch {
            try {
                val results = if (uiState.value.useOfflineOnly) {
                    GeminiSearchService.getLocalFallbackBudgetBuilds(budgetUsd, uiState.value.selectedCountry)
                } else {
                    GeminiSearchService.searchRealtimeBudgetBuilds(budgetUsd, uiState.value.selectedCountry)
                }
                _uiState.update { state ->
                    state.copy(
                        budgetSearchLoading = false,
                        budgetSourceOptions = results,
                        budgetSearchError = null
                    )
                }
                addLog("Market research completed. ${results.size} storefront alternatives found.")
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        budgetSearchLoading = false,
                        budgetSearchError = "An error occurred scanning local merchants: ${e.localizedMessage}"
                    )
                }
                addLog("Storefront search error: ${e.localizedMessage}")
            }
        }
    }

    private fun addLog(message: String) {
        val formatted = "[${getFormattedTime()}] $message"
        _uiState.update { state ->
            val list = state.consoleLog.toMutableList()
            list.add(0, formatted) // prepend newest
            if (list.size > 20) list.removeLast()
            state.copy(consoleLog = list)
        }
    }

    private fun getFormattedTime(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
