package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.api.BudgetSourceOption
import com.example.api.BudgetPartItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetPresetsTab(
    viewModel: PCBuilderViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    var budgetInput by remember { mutableStateOf("1200") }
    var expandedOptionIndex by remember { mutableStateOf<Int?>(0) } // first is open by default
    
    val fastBudgets = remember {
        listOf(
            "$600" to "600",
            "$1,000" to "1000",
            "$1,300" to "1300",
            "$1,800" to "1800",
            "$2,500" to "2500"
        )
    }

    val onSearchBudget = remember(budgetInput, viewModel) {
        {
            val rawBudget = budgetInput.filter { it.isDigit() }.toDoubleOrNull() ?: 1200.0
            viewModel.fetchRealtimeBudgetOptions(rawBudget)
            // auto-expand the first option when loaded
            expandedOptionIndex = 0
        }
    }

    // Auto-fetch budget initially if results are empty to make it look full and ready
    LaunchedEffect(Unit) {
        if (uiState.budgetSourceOptions.isEmpty() && !uiState.budgetSearchLoading) {
            onSearchBudget()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Banner Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔍 SMART ONLINE BUDGET DETECTIVE (AI)",
                        color = CosmicPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter your target budget. Our neural crawler will scan live web storefronts (Google Search) in real-time to find, verify, and itemize complete PC system set-ups suited for your active Location Profile.",
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp)
                    )
                }
            }
        }

        // Budget Entry Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎯 ENTER OR SELECT YOUR TARGET BUDGET",
                        color = CosmicWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            if (clean.length <= 7) {
                                budgetInput = clean
                            }
                        },
                        label = { Text("Maximum Budget Limit ($ USD)", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 1200", color = CosmicGrayText.copy(alpha = 0.4f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Budget Icon",
                                tint = CosmicPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicPrimary,
                            unfocusedBorderColor = CosmicCardBorder,
                            focusedTextColor = CosmicWhite,
                            unfocusedTextColor = CosmicWhite,
                            focusedLabelColor = CosmicPrimary,
                            unfocusedLabelColor = CosmicGrayText,
                            focusedContainerColor = CosmicSurfaceHeader.copy(alpha = 0.2f),
                            unfocusedContainerColor = CosmicSurfaceHeader.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fast budgets flow layout
                    Text(
                        text = "Quick Choose Recommendations:",
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        fastBudgets.forEach { (label, value) ->
                            val isSelected = budgetInput == value
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CosmicPrimary.copy(alpha = 0.2f) else CosmicSurfaceHeader.copy(alpha = 0.4f))
                                    .border(BorderStroke(1.dp, if (isSelected) CosmicPrimary else CosmicCardBorder.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                                    .clickable {
                                        budgetInput = value
                                        onSearchBudget()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                textLabel(label, isSelected)
                             }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary trigger button
                    Button(
                        onClick = { onSearchBudget() },
                        enabled = budgetInput.isNotEmpty() && !uiState.budgetSearchLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmicPrimary,
                            contentColor = CosmicBackground,
                            disabledContainerColor = CosmicCardBorder.copy(alpha = 0.5f),
                            disabledContentColor = CosmicGrayText
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("search_budget_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.budgetSearchLoading) "Searching Online Inventory..." else "Scan & Match Storefronts 🔍",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Processing / Scanning Logs Terminal during loading state
        if (uiState.budgetSearchLoading) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CosmicPrimary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Scanning Live Setup Inventory with Real-time Grounded AI Engine...",
                            color = CosmicWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Crawl queries target local merchants depending on your active Location Profile (${uiState.selectedCountry}) to pull accurate, region-specific PC hardware builds.",
                            color = CosmicGrayText,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .border(BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.3f)), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("📡 [CONNECT] Web grounding engine active...", color = CosmicPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text("🔍 [QUERY] PC builds inside ${uiState.selectedCountry} region around equivalent budget...", color = CosmicWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text("📥 [FETCH] Parsing storefront matching matrices...", color = CosmicGrayText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Error card
        if (uiState.budgetSearchError != null && !uiState.budgetSearchLoading) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.budgetSearchError ?: "An error occurred while scanning live market storefronts.",
                            color = AccentRed,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = { onSearchBudget() },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                        ) {
                            Text("Retry Search", fontSize = 11.sp, color = CosmicBackground)
                        }
                    }
                }
            }
        }

        // Success: Render searched sources
        if (!uiState.budgetSearchLoading && uiState.budgetSourceOptions.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🤖 AI ACTIVE", fontSize = 10.sp, color = CosmicPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FOUND STOREFRONTS & OFFERS",
                        color = CosmicWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Render each vendor/magaza card
            items(uiState.budgetSourceOptions.size) { index ->
                val option = uiState.budgetSourceOptions[index]
                val isExpanded = expandedOptionIndex == index
                
                StoreOptionCard(
                    option = option,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedOptionIndex = if (isExpanded) null else index
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun textLabel(text: String, isSelected: Boolean) {
    Text(
        text = text,
        color = if (isSelected) CosmicPrimary else CosmicWhite,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun StoreOptionCard(
    option: BudgetSourceOption,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    viewModel: PCBuilderViewModel
) {
    val uriHandler = LocalUriHandler.current
    
    // Select store color scheme & badge info
    val isAmazon = option.sourceName.contains("Amazon", ignoreCase = true)
    val isItopya = option.sourceName.contains("Itopya", ignoreCase = true)
    val isEbay = option.sourceName.contains("Ebay", ignoreCase = true)
    
    val storeColor = if (isAmazon) CosmicPrimary else if (isItopya) CosmicSecondary else if (isEbay) CosmicWhite else AccentGreen
    val storeBadge = if (isAmazon) "📦 AMAZON" else if (isItopya) "🎮 ITOPYA" else if (isEbay) "🏪 EBAY GLOBAL" else "💻 ${option.sourceName.uppercase()}"

    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, if (isExpanded) storeColor.copy(alpha = 0.4f) else CosmicCardBorder.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Main Card Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(storeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = storeBadge,
                                color = storeColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (option.isRealtime) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE CRAWLED 🌐",
                                    color = AccentGreen,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.systemTitle,
                        color = CosmicWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = option.formattedPrice,
                        color = if (option.sourceName.contains("Amazon", ignoreCase = true)) CosmicPrimary else CosmicSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isExpanded) "Collapse ▲" else "Details ▼",
                        color = CosmicGrayText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expanded list of components
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = CosmicCardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                    
                    Text(
                        text = "📦 SYSTEM HARDWARE MATRIX & PRICE BREAKDOWN",
                        color = CosmicGrayText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    
                    // Display each part item under this vendor
                    option.parts.forEach { part ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CosmicSurfaceHeader.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category Badge
                            Box(
                                modifier = Modifier
                                    .width(75.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CosmicCardBorder.copy(alpha = 0.3f))
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = part.categoryName.uppercase(),
                                    color = CosmicWhite,
                                    fontSize = 7.9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = part.partName,
                                    color = CosmicWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (part.specs.isNotEmpty()) {
                                    Text(
                                        text = part.specs,
                                        color = CosmicGrayText,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    // Bottom links and actions inside expansion
                    HorizontalDivider(color = CosmicCardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Activate Preset inside Wizard Build
                        Button(
                            onClick = {
                                loadBudgetOptionIntoBuilder(option, viewModel)
                                // open the first wizard tab to show parts loaded!
                                viewModel.setActiveTab(0)
                             },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = storeColor,
                                contentColor = CosmicBackground
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load into Builder 🔧", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Web Redirect link
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri(option.shopUrl)
                                } catch (e: Exception) {
                                    // prevent crashing
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicSurfaceHeader,
                                contentColor = CosmicWhite
                            ),
                            border = BorderStroke(1.dp, storeColor.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Go to Store 🔗", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (option.searchTimestamp.isNotEmpty()) {
                        Text(
                            text = "Last Updated: ${option.searchTimestamp}",
                            color = CosmicGrayText.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun loadBudgetOptionIntoBuilder(option: BudgetSourceOption, viewModel: PCBuilderViewModel) {
    val partMap = mutableMapOf<com.example.data.PartCategory, com.example.data.HardwarePart>()
    option.parts.forEach { partItem ->
        val cat = when (partItem.categoryName.uppercase().trim()) {
            "CPU", "PROCESSOR" -> com.example.data.PartCategory.CPU
            "GPU", "VIDEO CARD", "GRAPHICS" -> com.example.data.PartCategory.GPU
            "MOTHERBOARD", "MAINBOARD" -> com.example.data.PartCategory.MOTHERBOARD
            "RAM", "MEMORY" -> com.example.data.PartCategory.RAM
            "STORAGE", "SSD", "HDD" -> com.example.data.PartCategory.STORAGE
            "POWERSUPPLY", "PSU" -> com.example.data.PartCategory.PSU
            "CASE" -> com.example.data.PartCategory.CASE
            "COOLER", "FAN" -> com.example.data.PartCategory.COOLER
            else -> null
        }
        if (cat != null) {
            val nameParts = partItem.partName.split(" ", limit = 2)
            val brand = nameParts.getOrNull(0) ?: "Custom"
            val model = nameParts.getOrNull(1) ?: partItem.partName
            
            // Resolve from existing database if can find, else make dynamic custom
            val resolved = com.example.data.HardwareDatabase.resolveOfflinePartData(partItem.partName, cat) ?: com.example.data.HardwarePart(
                id = "budget_scouted_${cat.name}",
                category = cat,
                brand = brand,
                modelOrSpecs = model,
                priceUsd = when (cat) {
                    com.example.data.PartCategory.CPU -> 200.0
                    com.example.data.PartCategory.GPU -> 400.0
                    com.example.data.PartCategory.MOTHERBOARD -> 130.0
                    com.example.data.PartCategory.RAM -> 95.0
                    com.example.data.PartCategory.STORAGE -> 70.0
                    com.example.data.PartCategory.PSU -> 80.0
                    com.example.data.PartCategory.COOLER -> 40.0
                    com.example.data.PartCategory.CASE -> 70.0
                },
                tdpWatts = when (cat) {
                    com.example.data.PartCategory.CPU -> 65
                    com.example.data.PartCategory.GPU -> 200
                    com.example.data.PartCategory.RAM -> 5
                    com.example.data.PartCategory.STORAGE -> 5
                    com.example.data.PartCategory.COOLER -> 5
                    else -> 0
                },
                details = partItem.specs,
                isCustom = true
            )
            partMap[cat] = resolved
        }
    }
    
    val preset = com.example.data.PresetBuild(
        id = "budget_scouted_build",
        name = "Scouted (${option.sourceName})",
        description = "Custom build imported directly from region-scouted market results (${option.sourceName}).",
        budgetRange = option.formattedPrice,
        parts = partMap
    )
    viewModel.loadPresetBuild(preset)
}
