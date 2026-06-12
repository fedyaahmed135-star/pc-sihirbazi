package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.data.formatTl
import com.example.data.formatUsd
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: PCBuilderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HardwareDatabase.initialize(this)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(themeOption = uiState.selectedTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = CosmicBackground
                ) { innerPadding ->
                    PCBuilderApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PCBuilderApp(
    viewModel: PCBuilderViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val compatibilityReport = remember(uiState.selectedParts) {
        CompatibilityAnalyzer.analyzeParts(uiState.selectedParts)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        // App Header
        AppHeader(
            selectedTheme = uiState.selectedTheme,
            selectedCountry = uiState.selectedCountry,
            onThemeSelected = { viewModel.selectTheme(it) },
            onCountrySelected = { viewModel.selectCountry(it) }
        )

        val onTabSelected = remember(viewModel) { { tab: Int -> viewModel.setActiveTab(tab) } }

        // Tab Navigation Bar
        CustomTabBar(
            activeTab = uiState.activeTab,
            onTabSelected = onTabSelected
        )

        // Main Scrollable Area containing Tabs
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState.activeTab) {
                0 -> BuildTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    compatibilityReport = compatibilityReport
                )
                1 -> GamingPerformanceTab(
                    uiState = uiState,
                    viewModel = viewModel
                )
                2 -> BudgetPresetsTab(
                    viewModel = viewModel
                )
                3 -> ComparatorTab(viewModel = viewModel)
            }
        }

        // Active Diagnostics & Summary Persistent Footer
        ActiveSummaryFooter(
            uiState = uiState,
            report = compatibilityReport
        )
    }
}

@Composable
fun AppHeader(
    selectedTheme: ThemeOption = ThemeOption.COSMIC_AMBER,
    selectedCountry: String = "USA",
    onThemeSelected: (ThemeOption) -> Unit = {},
    onCountrySelected: (String) -> Unit = {}
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = CosmicSurface,
            title = {
                Text(
                    text = "🎨 Interface Theme",
                    color = CosmicWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Choose a neon color palette to style your setup workspace:",
                        color = CosmicGrayText,
                        fontSize = 12.sp
                    )

                    ThemeOption.values().forEach { theme ->
                        val isSelected = theme == selectedTheme
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) theme.primaryColor.copy(alpha = 0.15f)
                                    else CosmicSurfaceHeader.copy(alpha = 0.3f)
                                )
                                .border(
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = if (isSelected) theme.primaryColor else CosmicCardBorder.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onThemeSelected(theme)
                                    showThemeDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(theme.emoji, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = theme.displayName,
                                    color = CosmicWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = theme.description,
                                    color = CosmicGrayText,
                                    fontSize = 10.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = theme.primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showThemeDialog = false }
                ) {
                    Text("Close", color = CosmicSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showCountryDialog) {
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            containerColor = CosmicSurface,
            title = {
                Text(
                    text = "🌐 Target Country & Storefronts",
                    color = CosmicWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Toggle your localization settings to automatically redirect search queries, localize fallback listings, and use local currency formulas:",
                        color = CosmicGrayText,
                        fontSize = 12.sp
                    )

                    listOf(
                        Triple("USA", "🇺🇸 USA", "Market: Amazon US, Newegg, Best Buy (USD)"),
                        Triple("Azerbaijan", "🇦🇿 Azerbaijan", "Market: comp.az, Baku.az, Kontakt (AZN)"),
                        Triple("Turkey", "🇹🇷 Turkey", "Market: Amazon TR, Itopya, Sinerji (TL)"),
                        Triple("Germany", "🇩🇪 Germany", "Market: Caseking, Mindfactory (EUR)")
                    ).forEach { (code, label, desc) ->
                        val isSelected = code == selectedCountry
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) CosmicPrimary.copy(alpha = 0.15f)
                                    else CosmicSurfaceHeader.copy(alpha = 0.3f)
                                )
                                .border(
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = if (isSelected) CosmicPrimary else CosmicCardBorder.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onCountrySelected(code)
                                    showCountryDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = label,
                                color = CosmicWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = desc,
                                    color = CosmicGrayText,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = CosmicPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showCountryDialog = false }
                ) {
                    Text("Close", color = CosmicSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val cardBorder = CosmicCardBorder
    val surfaceColor = CosmicSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = cardBorder.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicPrimary.copy(alpha = 0.15f))
                    .clickable { showThemeDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎨",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.clickable { showThemeDialog = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Rig Studio",
                        color = CosmicWhite,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedTheme.emoji,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "Aesthetics & Colors (Change Theme)",
                    color = CosmicSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val countryFlagLabel = when (selectedCountry.lowercase()) {
                "azerbaijan" -> "🇦🇿 AZN"
                "turkey" -> "🇹🇷 TRY"
                "germany" -> "🇩🇪 EUR"
                else -> "🇺🇸 USD"
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = CosmicPrimary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.7f)),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable { showCountryDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AccentGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = countryFlagLabel,
                        color = CosmicWhite.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun CustomTabBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabTitles = remember { listOf("🛠️ Sistem", "🎮 Oyun", "💡 Bütçe", "🔄 Kıyasla") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(CosmicSurface, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
            .padding(4.dp)
    ) {
        tabTitles.forEachIndexed { index, title ->
            val isSelected = activeTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) CosmicPrimary.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) CosmicPrimary else CosmicGrayText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BuildTab(
    uiState: BuilderUIState,
    viewModel: PCBuilderViewModel,
    compatibilityReport: CompatibilityReport
) {
    var showPartDialogCategory by remember { mutableStateOf<PartCategory?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Dashboard Header within build list
            item(key = "dashboard_header") {
                BuildDashboardHeader(uiState, compatibilityReport)
            }

            // Budget Tracking Panel within build list
            item(key = "budget_tracker") {
                val onBudgetChange = remember(viewModel) { { budget: Double -> viewModel.setTargetBudgetUsd(budget) } }
                BudgetTrackingPanel(
                    uiState = uiState,
                    onTargetBudgetChange = onBudgetChange
                )
            }

            // Engine Selector Card
            item(key = "engine_selector") {
                val onToggleEngine = remember(viewModel) { { mode: Boolean -> viewModel.setOfflineOnlyMode(mode) } }
                EngineSelectorSection(
                    useOfflineOnly = uiState.useOfflineOnly,
                    onToggleEngine = onToggleEngine
                )
            }

            // Warnings Card
            if (compatibilityReport.hasWarnings) {
                item(key = "warning_banner") {
                    WarningBannerSection(compatibilityReport)
                }
            }

            // Grid items for categories
            items(
                items = PartCategory.values(),
                key = { "category_" + it.name }
            ) { category ->
                val onSelect = remember(category) { { showPartDialogCategory = category } }
                val onToggle = remember(category, viewModel) { { viewModel.toggleCustomInput(category) } }
                val onUpdate = remember(category, viewModel) { { updateBlock: (CustomInputState) -> CustomInputState -> viewModel.updateCustomInputField(category, updateBlock) } }
                val onVerify = remember(category, viewModel) { { viewModel.runInternetVerifyField(category) } }
                val onRemove = remember(category, viewModel) { { viewModel.removePart(category) } }

                PartCategoryCard(
                    category = category,
                    selectedPart = uiState.selectedParts[category],
                    customInput = uiState.customInputs[category] ?: CustomInputState(),
                    isLoading = uiState.verificationLoading[category] == true,
                    aiComment = uiState.verificationMessages[category],
                    aiSource = uiState.verificationSources[category],
                    useOfflineOnly = uiState.useOfflineOnly,
                    onSelectDatabasePart = onSelect,
                    onToggleCustomInput = onToggle,
                    onUpdateCustomField = onUpdate,
                    onVerifyClick = onVerify,
                    onRemovePart = onRemove
                )
            }

            // Real-time Console Logs
            item(key = "console_logs") {
                ConsoleLogsSection(logs = uiState.consoleLog)
            }
        }

        // Predefined selection dialog overlay
        showPartDialogCategory?.let { category ->
            PartSelectionDialog(
                category = category,
                predefinedList = HardwareDatabase.getAllPartsForCategory(category),
                onDismiss = { showPartDialogCategory = null },
                onPartSelect = { part ->
                    viewModel.selectPartFromDatabase(category, part)
                    showPartDialogCategory = null
                }
            )
        }
    }
}

@Composable
fun BuildDashboardHeader(
    uiState: BuilderUIState,
    report: CompatibilityReport
) {
    val totalUsd = remember(uiState.selectedParts) { uiState.selectedParts.values.sumOf { it.priceUsd } }
    val totalTdp = remember(report) { report.psuReport?.totalTdp ?: 0 }
    val psuCapacity = remember(report) { report.psuReport?.psuCapacity ?: 0 }
    val psuPercentage = remember(report) { report.psuReport?.loadPercentage ?: 0 }

    val rate = when (uiState.selectedCountry.lowercase()) {
        "azerbaijan" -> 1.70
        "turkey" -> 33.0
        "germany" -> 0.92
        else -> 1.0
    }
    val symbol = when (uiState.selectedCountry.lowercase()) {
        "azerbaijan" -> " AZN"
        "turkey" -> " TL"
        "germany" -> " €"
        else -> "$"
    }

    val formatCost = { amount: Double ->
        val converted = amount * rate
        val formattedNum = "%,.0f".format(java.util.Locale.US, converted)
        if (symbol == "$") "$$formattedNum" else "$formattedNum$symbol"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚡ SYSTEM DIAGNOSTICS",
                color = CosmicPrimary,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Investment",
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCost(totalUsd),
                        color = CosmicWhite,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    )
                    if (uiState.selectedCountry.lowercase() != "usa") {
                        Text(
                            text = "($${formatUsd(totalUsd)} USD)",
                            color = CosmicGrayText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Hardware Power Draw",
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalTdp W" + if (psuCapacity > 0) " / ${psuCapacity}W" else "",
                        color = when {
                            psuPercentage > 100 -> AccentRed
                            psuPercentage >= 90 -> AccentOrange
                            else -> CosmicSecondary
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    )
                    if (psuCapacity > 0) {
                        Text(
                            text = "PSU Capacity Load: $psuPercentage%",
                            color = if (psuPercentage >= 90) AccentOrange else CosmicGrayText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
                        )
                    } else {
                        Text(
                            text = "PSU Capacity Not Specified",
                            color = AccentOrange,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            if (psuCapacity > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (totalTdp.toFloat() / psuCapacity).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when {
                        psuPercentage > 100 -> AccentRed
                        psuPercentage >= 90 -> AccentOrange
                        else -> CosmicSecondary
                    },
                    trackColor = CosmicCardBorder.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun BudgetTrackingPanel(
    uiState: BuilderUIState,
    onTargetBudgetChange: (Double) -> Unit
) {
    val totalUsd = remember(uiState.selectedParts) { uiState.selectedParts.values.sumOf { it.priceUsd } }
    val targetBudgetUsd = uiState.targetBudgetUsd

    val ratio = if (targetBudgetUsd > 0) (totalUsd / targetBudgetUsd).toFloat().coerceIn(0f, 2f) else 0f
    val isOverBudget = totalUsd > targetBudgetUsd
    val isNearBudget = !isOverBudget && ratio >= 0.85f

    val progressColor = when {
        isOverBudget -> AccentRed
        isNearBudget -> AccentOrange
        else -> AccentGreen
    }

    val rate = when (uiState.selectedCountry.lowercase()) {
        "azerbaijan" -> 1.70
        "turkey" -> 33.0
        "germany" -> 0.92
        else -> 1.0
    }
    val symbol = when (uiState.selectedCountry.lowercase()) {
        "azerbaijan" -> " AZN"
        "turkey" -> " TL"
        "germany" -> " €"
        else -> "$"
    }

    val formatCost = { amount: Double ->
        val converted = amount * rate
        val formattedNum = "%,.0f".format(java.util.Locale.US, converted)
        if (symbol == "$") "$$formattedNum" else "$formattedNum$symbol"
    }

    var showBreakdown by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, progressColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "💰 BUDGET TRACKING PANEL",
                        color = CosmicPrimary,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(progressColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            isOverBudget -> "Over Budget! ❌"
                            isNearBudget -> "Near Limit! ⚠️"
                            else -> "Within Budget ✅"
                        },
                        color = progressColor,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Spent",
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCost(totalUsd),
                        color = CosmicWhite,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                    if (uiState.selectedCountry.lowercase() != "usa") {
                        Text(
                            text = "($${formatUsd(totalUsd)} USD)",
                            color = CosmicGrayText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(CosmicCardBorder.copy(alpha = 0.5f))
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Your Target Budget",
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCost(targetBudgetUsd),
                        color = CosmicWhite,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                    if (uiState.selectedCountry.lowercase() != "usa") {
                        Text(
                            text = "($${formatUsd(targetBudgetUsd)} USD)",
                            color = CosmicSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget Usage: ${(ratio * 100).toInt()}%",
                        color = CosmicGrayText,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (isOverBudget) {
                            "Difference: +${formatCost(totalUsd - targetBudgetUsd)}"
                        } else {
                            "Remaining: ${formatCost(targetBudgetUsd - totalUsd)}"
                        },
                        color = if (isOverBudget) AccentRed else AccentGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = CosmicCardBorder.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🎯 Hedef Bütçe Ayarla:",
                color = CosmicWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = targetBudgetUsd.toFloat(),
                onValueChange = { onTargetBudgetChange(it.toDouble()) },
                valueRange = 400f..5000f,
                steps = 46,
                colors = SliderDefaults.colors(
                    thumbColor = CosmicPrimary,
                    activeTrackColor = CosmicPrimary,
                    inactiveTrackColor = CosmicCardBorder.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val presets = listOf(600.0, 1200.0, 2000.0, 3500.0, 5000.0)
                presets.forEach { preset ->
                    val isSelected = (preset == targetBudgetUsd)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) CosmicPrimary.copy(alpha = 0.2f)
                                else CosmicSurfaceHeader.copy(alpha = 0.3f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CosmicPrimary else CosmicCardBorder.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onTargetBudgetChange(preset) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\$${preset.toInt()}",
                            color = if (isSelected) CosmicWhite else CosmicGrayText,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(progressColor.copy(alpha = 0.06f))
                    .border(BorderStroke(1.dp, progressColor.copy(alpha = 0.15f)), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when {
                            isOverBudget -> "⚠️"
                            isNearBudget -> "👀"
                            else -> "⚡"
                        },
                        fontSize = 14.sp
                    )
                    Text(
                        text = when {
                            isOverBudget -> "Hedef bütçe aşıldı! Bazı parçaları daha ekonomik olanlarla değiştirmeyi veya bütçenizi yukarı çekmeyi düşünebilirsiniz."
                            isNearBudget -> "Bütçe sınırına çok yaklaştınız. Son parçaları seçerken fiyata dikkat etmek faydalı olabilir!"
                            else -> "Harika! Bütçeniz güvende. Bu şablon için bütçenizde hala pay var, dilerseniz bileşenleri yükseltebilirsiniz."
                        },
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.sp)
                    )
                }
            }

            if (uiState.selectedParts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showBreakdown = !showBreakdown }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showBreakdown) "Detaylı Masraf Dağılımını Gizle 🔼" else "Parça fiyat analizini göster 🔽",
                        color = CosmicSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                AnimatedVisibility(
                    visible = showBreakdown,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📊 Parça Masraf Payı Analizi:",
                            color = CosmicWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        uiState.selectedParts.forEach { (cat, part) ->
                            val partPercentage = if (totalUsd > 0) ((part.priceUsd / totalUsd) * 100).toInt() else 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CosmicSurfaceHeader.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = getCategoryEmoji(cat), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = cat.displayName,
                                            color = CosmicWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "${part.brand} ${part.modelOrSpecs}",
                                            color = CosmicGrayText,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "TL ${formatTl(part.getPriceTl())}",
                                        color = CosmicWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CosmicPrimary.copy(alpha = 0.12f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "%$partPercentage",
                                            color = CosmicPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WarningBannerSection(report: CompatibilityReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AccentRed, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "UYUMLULUK UYARILARI / HATALARI",
                    color = AccentRed,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold, 
                        letterSpacing = 0.5.sp, 
                        fontSize = 10.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            report.warningsList.forEachIndexed { index, warning ->
                val warningColor = when {
                    warning.startsWith("❌") -> AccentRed
                    warning.startsWith("⚠️") -> AccentOrange
                    warning.startsWith("⚡") -> if (report.psuReport?.isOverloaded == true) AccentRed else AccentOrange
                    else -> CosmicWhite
                }
                Text(
                    text = warning,
                    color = warningColor,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                    modifier = Modifier.padding(bottom = if (index < report.warningsList.size - 1) 6.dp else 0.dp)
                )
            }
        }
    }
}

@Composable
fun PartCategoryCard(
    category: PartCategory,
    selectedPart: HardwarePart?,
    customInput: CustomInputState,
    isLoading: Boolean,
    aiComment: String?,
    aiSource: String?,
    useOfflineOnly: Boolean = false,
    onSelectDatabasePart: () -> Unit,
    onToggleCustomInput: () -> Unit,
    onUpdateCustomField: ((CustomInputState) -> CustomInputState) -> Unit,
    onVerifyClick: () -> Unit,
    onRemovePart: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val textFieldColors = rememberTextFieldColors()

    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(
            1.dp, 
            if (customInput.isEnabled) CosmicPrimary.copy(alpha = 0.3f) else CosmicCardBorder.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Core component info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Emoji Icon representing category
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CosmicSurfaceHeader.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryEmoji(category),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.displayName.uppercase(),
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold, 
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                    
                    if (selectedPart != null) {
                        Text(
                            text = selectedPart.getFullName(),
                            color = CosmicWhite,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "💵 ${formatTl(selectedPart.getPriceTl())} (\$${formatUsd(selectedPart.priceUsd)})",
                                color = CosmicSecondary,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            )
                            if (selectedPart.tdpWatts > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⚡ ${selectedPart.tdpWatts}W TDP",
                                    color = CosmicPrimary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            if (!selectedPart.socket.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🔌 ${selectedPart.socket}",
                                    color = CosmicWhite.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Bileşen Seçilmedi",
                            color = CosmicGrayText.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        )
                    }
                }

                // Remove selection action
                if (selectedPart != null) {
                    IconButton(
                        onClick = onRemovePart,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Kaldır",
                            tint = AccentRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action triggers for selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelectDatabasePart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicSurfaceHeader.copy(alpha = 0.7f),
                        contentColor = CosmicWhite
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("+ Şablondan Seç", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onToggleCustomInput,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (customInput.isEnabled) CosmicPrimary else CosmicGrayText
                    ),
                    border = BorderStroke(
                        1.dp, 
                        if (customInput.isEnabled) CosmicPrimary.copy(alpha = 0.5f) else CosmicCardBorder.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (customInput.isEnabled) "✍️ Kapat" else "✍️ Özel Yaz", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Real-time custom form details
            AnimatedVisibility(
                visible = customInput.isEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(CosmicSurfaceHeader.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .border(BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.3f)), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "✍️ ÖZEL BİLEŞEN TANIMLAMA",
                        color = CosmicPrimary,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Single full description input for automated AI extraction
                    OutlinedTextField(
                        value = customInput.model,
                        onValueChange = { newVal -> onUpdateCustomField { s -> s.copy(model = newVal, brand = "") } },
                        label = { Text("Tam Cihaz / Model Adı", fontSize = 10.sp) },
                        placeholder = {
                            val placeholder = when (category) {
                                PartCategory.CPU -> "Örn: AMD Ryzen 5 7600X veya Intel Core i5 12400F"
                                PartCategory.GPU -> "Örn: MSI GeForce RTX 4070 Super veya RX 7800 XT"
                                PartCategory.MOTHERBOARD -> "Örn: ASUS Prime B650M-A veya MSI TOMAHAWK Z790"
                                PartCategory.RAM -> "Örn: G.Skill Trident Z5 32GB DDR5 6000MHz"
                                PartCategory.STORAGE -> "Örn: Samsung 990 Pro 1TB NVMe M.2"
                                PartCategory.PSU -> "Örn: Corsair RM750e 750W 80+ Gold"
                                PartCategory.COOLER -> "Örn: Thermalright Peerless Assassin 120 SE"
                                PartCategory.CASE -> "Örn: Corsair 4000D Airflow"
                            }
                            Text(placeholder, fontSize = 11.sp, color = CosmicGrayText.copy(alpha = 0.4f))
                        },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (useOfflineOnly) {
                            "⚡ Çevrimdışı akıllı çözümleyici devrede. Parça soket, bellek, tahmini fiyat ve TDP değerleri yerel kütüphaneden saptanır."
                        } else {
                            "🌐 İnternet motoru devrede. Parça soket, bellek nesli, güncel piyasa fiyatı ve TDP özellikleri otomatik taranıp çekilir."
                        },
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 13.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search engine verification launcher button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onVerifyClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmicPrimary,
                            contentColor = CosmicBackground
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(size = 16.dp, color = CosmicBackground, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (useOfflineOnly) "İşleniyor..." else "Aranıyor...", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = if (useOfflineOnly) "⚡ Çevrimdışı Tanımla" else "🌐 İnternette Ara ve Doğrula", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // AI results commentary block
            if (!aiComment.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CosmicSurfaceHeader.copy(alpha = 0.2f))
                        .border(BorderStroke(1.dp, CosmicSecondary.copy(alpha = 0.25f)), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(CosmicSecondary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kayıt Kaynağı: $aiSource",
                                color = CosmicSecondary,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aiComment,
                            color = CosmicWhite,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConsoleLogsSection(logs: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📊 SİSTEM SÜREÇ GÜNLÜĞÜ",
                color = CosmicSecondary,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold, 
                    fontSize = 10.sp, 
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Text("Sistem kayıt günlüğü boş.", color = CosmicGrayText.copy(alpha = 0.5f), fontSize = 11.sp)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(logs) { log ->
                            Text(
                                text = log,
                                color = CosmicGrayText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = CosmicWhite,
        unfocusedTextColor = CosmicWhite,
        focusedBorderColor = CosmicPrimary,
        unfocusedBorderColor = CosmicCardBorder,
        focusedLabelColor = CosmicPrimary,
        unfocusedLabelColor = CosmicGrayText,
        focusedContainerColor = CosmicSurface,
        unfocusedContainerColor = CosmicSurface
    )
}

@Composable
fun CircularProgressIndicator(
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    strokeWidth: androidx.compose.ui.unit.Dp
) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = strokeWidth
    )
}

@Composable
fun PartSelectionDialog(
    category: PartCategory,
    predefinedList: List<HardwarePart>,
    onDismiss: () -> Unit,
    onPartSelect: (HardwarePart) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val textFieldColors = rememberTextFieldColors()

    val filteredList = remember(searchQuery, predefinedList) {
        if (searchQuery.trim().isEmpty()) {
            predefinedList
        } else {
            val q = searchQuery.lowercase().trim()
            predefinedList.filter { part ->
                "${part.brand} ${part.modelOrSpecs} ${part.details ?: ""}".lowercase().contains(q)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            border = BorderStroke(1.dp, CosmicCardBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Text(
                    text = "${category.displayName} Kataloğu",
                    color = CosmicPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Yerel veritabanında doğrulanmış tüm modeller (2005 - 2026)",
                    color = CosmicGrayText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Model ara (ör: 4070, Ryzen, E6600...)", fontSize = 12.sp, color = CosmicGrayText.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = textFieldColors,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Arama Simgesi",
                            tint = CosmicPrimary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = CosmicCardBorder)

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aranan kriterlere uygun model bulunamadı.",
                            color = CosmicGrayText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    // Scrollable items
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { part ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicSurfaceHeader.copy(alpha = 0.3f))
                                .clickable { onPartSelect(part) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${part.brand} ${part.modelOrSpecs}",
                                    color = CosmicWhite,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                part.details?.let {
                                    Text(
                                        text = it,
                                        color = CosmicGrayText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    if (part.socket != null) {
                                        Badge(containerColor = CosmicCardBorder) {
                                            Text("Soket: ${part.socket}", color = CosmicWhite, fontSize = 9.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    if (part.ramType != null) {
                                        Badge(containerColor = CosmicCardBorder) {
                                            Text(part.ramType, color = CosmicWhite, fontSize = 9.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    if (part.tdpWatts > 0) {
                                        Badge(containerColor = CosmicSurfaceHeader) {
                                            Text("${part.tdpWatts}W TDP", color = CosmicPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "💵 ${formatTl(part.getPriceTl())}",
                                    color = CosmicSecondary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "\$${part.priceUsd.toInt()}",
                                    color = CosmicGrayText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCardBorder),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Kapat")
                }
            }
        }
    }
}

@Composable
fun Badge(
    containerColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        modifier = Modifier.padding(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun ActiveSummaryFooter(
    uiState: BuilderUIState,
    report: CompatibilityReport
) {
    val totalUsd = remember(uiState.selectedParts) { uiState.selectedParts.values.sumOf { it.priceUsd } }
    val totalTl = remember(totalUsd) { totalUsd * 33.0 }
    val itemsSize = remember(uiState.selectedParts) { uiState.selectedParts.size }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        color = CosmicSurface,
        border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Aktif Bileşen Sayısı: $itemsSize / 8",
                    color = CosmicGrayText,
                    fontSize = 11.sp
                )
                Text(
                    text = "💵 ${formatTl(totalTl)}",
                    color = CosmicPrimary,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                )
                Text(
                    text = "TDP Tüketimi: ${report.psuReport?.totalTdp ?: 0}W",
                    color = CosmicSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (report.hasWarnings) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentRed)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚠️ Hatalı Kurulum",
                            color = CosmicBackground,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    }
                } else if (itemsSize >= 6) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentGreen)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✅ %100 Uyumlu",
                            color = CosmicBackground,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicCardBorder.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Seçim Bekleniyor",
                            color = CosmicWhite,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EngineSelectorSection(
    useOfflineOnly: Boolean,
    onToggleEngine: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicSurfaceHeader.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (useOfflineOnly) "🔌" else "🌐",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Doğrulama Motoru Seçimi",
                            color = CosmicWhite,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (useOfflineOnly) "Anında Çevrimdışı Tahmin" else "İnternet Arama Doğrulaması (Aktif)",
                            color = if (useOfflineOnly) CosmicGrayText else CosmicPrimary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Switch(
                    checked = useOfflineOnly,
                    onCheckedChange = { onToggleEngine(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CosmicBackground,
                        checkedTrackColor = CosmicSecondary,
                        uncheckedThumbColor = CosmicBackground,
                        uncheckedTrackColor = CosmicPrimary
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CosmicSurfaceHeader.copy(alpha = 0.2f))
                    .border(BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.3f)), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = if (useOfflineOnly) {
                        "💡 Çevrimdışı Hızlı Motor devrede. Parça doğrulaması anında yerel algoritma ve yerel veri tabanıyla gerçekleştirilir, internet araması simüle edilmez."
                    } else {
                        "🌐 İnternet Arama Motoru devrede. Belirttiğiniz cihaz adını internet ve parça veritabanlarında arayarak TDP, soket ve RAM uyumu parametrelerini doğrular."
                    },
                    color = CosmicGrayText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp)
                )
            }
        }
    }
}

// Helpers for strings
fun getCategoryEmoji(category: PartCategory): String {
    return when (category) {
        PartCategory.CPU -> "💻"
        PartCategory.MOTHERBOARD -> "🧠"
        PartCategory.RAM -> "⚡"
        PartCategory.GPU -> "🎮"
        PartCategory.PSU -> "🔌"
        PartCategory.STORAGE -> "📦"
        PartCategory.COOLER -> "❄️"
        PartCategory.CASE -> "🛡️"
    }
}

