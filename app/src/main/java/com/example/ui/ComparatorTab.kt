package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight as FontWeightCompose
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.api.BenchmarkCompareResult

@Composable
fun ComparatorTab(viewModel: PCBuilderViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf(PartCategory.CPU) }
    
    // Retrieve parts for the selected category
    val allParts = remember(selectedCategory) {
        HardwareDatabase.getAllPartsForCategory(selectedCategory)
    }

    // Default left and right selected parts
    var partLeft by remember(selectedCategory, allParts) {
        mutableStateOf(allParts.getOrNull(0))
    }
    var partRight by remember(selectedCategory, allParts) {
        mutableStateOf(allParts.getOrNull(1) ?: allParts.getOrNull(0))
    }

    var dropdownCategoryExpanded by remember { mutableStateOf(false) }
    var dropdownLeftExpanded by remember { mutableStateOf(false) }
    var dropdownRightExpanded by remember { mutableStateOf(false) }

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

    // Auto-clear stale benchmark results when selection changes
    LaunchedEffect(selectedCategory, partLeft, partRight) {
        viewModel.clearBenchmarkResult()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Title and Description
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔄 HARDWARE COMPARATIVE MATRIX",
                        color = CosmicPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeightCompose.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Compare two different PC hardware components side-by-side to review exact pricing, power efficiency ratios (TDP), and detailed technical specifications.",
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp)
                    )
                }
            }
        }

        // Category Selector
        item {
            Column {
                Text(
                    text = "Hardware Category to Compare",
                    color = CosmicWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeightCompose.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicSurface)
                        .border(BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.8f)), RoundedCornerShape(8.dp))
                        .clickable { dropdownCategoryExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("comparator_category_selector")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = getCategoryEmoji(selectedCategory),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = selectedCategory.displayName,
                                color = CosmicWhite,
                                fontWeight = FontWeightCompose.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = CosmicGrayText
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownCategoryExpanded,
                        onDismissRequest = { dropdownCategoryExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(CosmicSurface)
                            .border(BorderStroke(1.dp, CosmicCardBorder), RoundedCornerShape(8.dp))
                    ) {
                        PartCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = getCategoryEmoji(category), fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category.displayName, color = CosmicWhite)
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    dropdownCategoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Components Selectors Side-By-Side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left Part Picker
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Component A (Left)",
                        color = CosmicGrayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeightCompose.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicSurface)
                            .border(BorderStroke(1.5.dp, CosmicPrimary.copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                            .clickable { dropdownLeftExpanded = true }
                            .padding(10.dp)
                            .testTag("comparator_left_part_selector"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = partLeft?.let { "${it.brand} ${it.modelOrSpecs}" } ?: "Not Selected",
                                color = CosmicWhite,
                                fontWeight = FontWeightCompose.Bold,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = CosmicPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownLeftExpanded,
                            onDismissRequest = { dropdownLeftExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.43f)
                                .background(CosmicSurface)
                                .border(BorderStroke(1.dp, CosmicCardBorder), RoundedCornerShape(8.dp))
                        ) {
                            allParts.forEach { part ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${part.brand} ${part.modelOrSpecs}",
                                                color = CosmicWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeightCompose.Bold
                                            )
                                            Text(
                                                text = formatCost(part.priceUsd),
                                                color = CosmicSecondary,
                                                fontSize = 9.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        partLeft = part
                                        dropdownLeftExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Right Part Picker
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Component B (Right)",
                        color = CosmicGrayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeightCompose.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicSurface)
                            .border(BorderStroke(1.5.dp, CosmicSecondary.copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                            .clickable { dropdownRightExpanded = true }
                            .padding(10.dp)
                            .testTag("comparator_right_part_selector"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = partRight?.let { "${it.brand} ${it.modelOrSpecs}" } ?: "Not Selected",
                                color = CosmicWhite,
                                fontWeight = FontWeightCompose.Bold,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownRightExpanded,
                            onDismissRequest = { dropdownRightExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.43f)
                                .background(CosmicSurface)
                                .border(BorderStroke(1.dp, CosmicCardBorder), RoundedCornerShape(8.dp))
                        ) {
                            allParts.forEach { part ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${part.brand} ${part.modelOrSpecs}",
                                                color = CosmicWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeightCompose.Bold
                                            )
                                            Text(
                                                text = formatCost(part.priceUsd),
                                                color = CosmicSecondary,
                                                fontSize = 9.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        partRight = part
                                        dropdownRightExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Comparison Table & Analysis Matrix
        val left = partLeft
        val right = partRight
        if (left != null && right != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📊 TECHNICAL SPECIFICATION COMPARISON",
                            color = CosmicWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeightCompose.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Brand
                        CompareRow("Brand", left.brand, right.brand)
                        
                        // Model Name
                        CompareRow("Model / Series Tag", left.modelOrSpecs, right.modelOrSpecs)

                        // Local Estimated Cost
                        val isLeftPriceCheaper = left.priceUsd < right.priceUsd
                        val isRightPriceCheaper = right.priceUsd < left.priceUsd
                        CompareRow(
                            label = "Estimated Regional Cost",
                            leftValue = formatCost(left.priceUsd),
                            rightValue = formatCost(right.priceUsd),
                            highlightLeft = isLeftPriceCheaper,
                            highlightRight = isRightPriceCheaper,
                            badgeText = "Cheaper 💸"
                        )

                        // Price USD
                        CompareRow(
                            label = "Standard Price (USD)",
                            leftValue = "$${formatUsd(left.priceUsd)} USD",
                            rightValue = "$${formatUsd(right.priceUsd)} USD",
                            highlightLeft = isLeftPriceCheaper,
                            highlightRight = isRightPriceCheaper,
                            badgeText = "Savings 💰"
                        )

                        // TDP (Power Consumption)
                        val isLeftTdpLower = left.tdpWatts < right.tdpWatts && left.tdpWatts > 0
                        val isRightTdpLower = right.tdpWatts < left.tdpWatts && right.tdpWatts > 0
                        CompareRow(
                            label = "Power Index (TDP)",
                            leftValue = if (left.tdpWatts > 0) "${left.tdpWatts} W" else "Unspecified",
                            rightValue = if (right.tdpWatts > 0) "${right.tdpWatts} W" else "Unspecified",
                            highlightLeft = isLeftTdpLower,
                            highlightRight = isRightTdpLower,
                            badgeText = "Efficient ⚡"
                        )

                        // Socket Compatibility
                        if (left.socket != null || right.socket != null) {
                            CompareRow(
                                label = "Platform Socket",
                                leftValue = left.socket ?: "Universal Style",
                                rightValue = right.socket ?: "Universal Style"
                            )
                        }

                        // RAM Type Generation
                        if (left.ramType != null || right.ramType != null) {
                            CompareRow(
                                label = "Memory Support",
                                leftValue = left.ramType ?: "Universal",
                                rightValue = right.ramType ?: "Universal"
                            )
                        }

                        // Other Details
                        CompareRow(
                            label = "Reference Specifications",
                            leftValue = left.details ?: "Standard design specs",
                            rightValue = right.details ?: "Standard design specs"
                        )
                    }
                }
            }

            // Quick Analytical Takeaway Box
            item {
                val isLeftCheaper = left.priceUsd < right.priceUsd
                val cheaperPart = if (isLeftCheaper) left else right
                val pricierPart = if (isLeftCheaper) right else left
                val absDiffUsd = java.lang.Math.abs(left.priceUsd - right.priceUsd)

                val isLeftMorePowerEfficient = left.tdpWatts < right.tdpWatts && left.tdpWatts > 0
                val isRightMorePowerEfficient = right.tdpWatts < left.tdpWatts && right.tdpWatts > 0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicPrimary.copy(alpha = 0.05f))
                        .border(BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.15f)), RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PC BUILDER ANALYTICAL INSIGHTS",
                                color = CosmicPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeightCompose.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Price analysis bullet
                        if (absDiffUsd > 0.1) {
                            val percentDiff = ((absDiffUsd / pricierPart.priceUsd) * 100).toInt()
                            val percentToShow = if (percentDiff > 0) percentDiff else 1
                            Text(
                                text = "• ${cheaperPart.brand} ${cheaperPart.modelOrSpecs} is roughly %$percentToShow more cost-effective than its rival ${pricierPart.brand} ${pricierPart.modelOrSpecs}, saving you about ${formatCost(absDiffUsd)} in your regional currency.",
                                color = CosmicGrayText,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        } else {
                            Text(
                                text = "• Both components have identical retail pricing profiles.",
                                color = CosmicGrayText,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Power efficiency bullet
                        val efficiencyText = when {
                            isLeftMorePowerEfficient -> "${left.brand} ${left.modelOrSpecs} offers a lower power rating (${left.tdpWatts}W) than the rival product (${right.tdpWatts}W) saving ${right.tdpWatts - left.tdpWatts} Watts. This guarantees easier cooling, lower thermal outputs, and diminished PSU capacity demands."
                            isRightMorePowerEfficient -> "${right.brand} ${right.modelOrSpecs} offers a lower power rating (${right.tdpWatts}W) than the rival product (${left.tdpWatts}W) saving ${left.tdpWatts - right.tdpWatts} Watts. This guarantees easier cooling, lower thermal outputs, and diminished PSU capacity demands."
                            left.tdpWatts == right.tdpWatts && left.tdpWatts > 0 -> "Both models consume an identical thermal power rate (${left.tdpWatts}W TDP), exerting similar strain on system PSUs."
                            else -> "Both products utilize a standard power consumption metric."
                        }
                        Text(
                            text = "• $efficiencyText",
                            color = CosmicGrayText,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        // Custom Socket comparison warning helper
                        if (left.socket != null && right.socket != null && !left.socket.equals(right.socket, ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentRed.copy(alpha = 0.08f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "🔴 Socket Standard Incompatibility: These components utilize distinct motherboard sockets (${left.socket} vs ${right.socket}). Ensure you pair them with matching motherboards during full system design.",
                                    color = AccentRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeightCompose.SemiBold,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- LIVE GOOGLE SEARCH / TOM'S HARDWARE BENCHMARKS ---
            item {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🌐", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REAL-TIME BENCHMARKS & PERFORMANCE MATRIX",
                            color = CosmicWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeightCompose.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Benchmark UI Controller Box
                    val isCpuOrGpu = selectedCategory == PartCategory.CPU || selectedCategory == PartCategory.GPU
                    
                    if (uiState.benchmarkResult == null && !uiState.benchmarkLoading) {
                        // Initial State: Button to trigger
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                            border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Crawl active benchmark registries like Tom's Hardware, PassMark, and TechPowerUp to retrieve live synthetic frame scores and verified tests.",
                                    color = CosmicGrayText,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 12.dp),
                                    lineHeight = 15.sp
                                )
                                
                                Button(
                                    onClick = { viewModel.fetchRealtimeBenchmark(left, right, selectedCategory) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CosmicPrimary,
                                        contentColor = CosmicBackground
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("fetch_realtime_benchmark_btn")
                                ) {
                                    Text(
                                        text = "Query Live Benchmarks 🔍",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeightCompose.Bold
                                    )
                                }
                                
                                if (!isCpuOrGpu) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Note: For Category (${selectedCategory.displayName}), a theoretical performance comparison matrix will be synthesized.",
                                        color = CosmicGrayText.copy(alpha = 0.7f),
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else if (uiState.benchmarkLoading) {
                        // Loading State
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                            border = BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = CosmicPrimary,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Retrieving web performance registries using Google grounding crawlers...",
                                    color = CosmicWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeightCompose.Medium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    text = "Parsing Tom's Hardware, TechPowerUp index cards, and professional review indices...",
                                    color = CosmicGrayText,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (uiState.benchmarkError != null) {
                        // Error State
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                            border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = uiState.benchmarkError ?: "Real-time performance registries are currently unavailable.",
                                    color = AccentRed,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Button(
                                    onClick = { viewModel.fetchRealtimeBenchmark(left, right, selectedCategory) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                                ) {
                                    Text("Retry Query", fontSize = 11.sp, color = CosmicBackground)
                                }
                            }
                        }
                    } else if (uiState.benchmarkResult != null) {
                        // Success / Result State
                        val result = uiState.benchmarkResult!!
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (result.isRealtime) CosmicPrimary.copy(alpha = 0.4f) else CosmicWhite.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header with Badge of Source
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (result.isRealtime) CosmicPrimary.copy(alpha = 0.15f) else CosmicWhite.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (result.isRealtime) "LIVE VERIFIED 🌐" else "SMART PREDICTION ⚙️",
                                                color = if (result.isRealtime) CosmicPrimary else CosmicWhite,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeightCompose.Bold
                                            )
                                        }
                                    }

                                    // Source Domain Link Info
                                    Text(
                                        text = result.sourceName,
                                        color = CosmicPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeightCompose.Bold,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth(0.6f)
                                    )
                                }

                                // Winner Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AccentGreen.copy(alpha = 0.08f))
                                        .border(BorderStroke(1.dp, AccentGreen.copy(alpha = 0.2f)), RoundedCornerShape(6.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("👑", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "PERFORMANCE LEADER",
                                                color = AccentGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeightCompose.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = result.winner,
                                                color = CosmicWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeightCompose.Bold
                                            )
                                        }
                                    }
                                }

                                // Summary Takeaway Text
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CosmicSurfaceHeader.copy(alpha = 0.2f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "💡 INSIGHT SUMMARY",
                                        color = CosmicWhite,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeightCompose.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = result.summary,
                                        color = CosmicGrayText,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }

                                // Interactive / Visual Performance Analysis Chart Sütunu
                                PerformanceChartSection(result)

                                // Part A Benchmark Data block
                                Column {
                                    Text(
                                        text = "📊 ${result.partAName} Reference Metrics",
                                        color = CosmicPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeightCompose.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = result.partABenchmark,
                                        color = CosmicWhite,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                // Part B Benchmark Data block
                                Column {
                                    Text(
                                        text = "📊 ${result.partBName} Reference Metrics",
                                        color = CosmicPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeightCompose.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = result.partBBenchmark,
                                        color = CosmicWhite,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                // Footer Info With Link and Timestamp
                                HorizontalDivider(color = CosmicCardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Last Crawled: ${result.timestamp}",
                                        color = CosmicGrayText.copy(alpha = 0.6f),
                                        fontSize = 8.sp
                                    )

                                    // Clickable URL Text
                                    Text(
                                        text = "Browse Source 🔗",
                                        color = CosmicPrimary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeightCompose.Bold,
                                        modifier = Modifier.clickable {
                                            try {
                                                uriHandler.openUri(result.sourceUrl)
                                            } catch (e: Exception) {
                                                // Handle safely
                                            }
                                        }
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

@Composable
fun CompareRow(
    label: String,
    leftValue: String,
    rightValue: String,
    highlightLeft: Boolean = false,
    highlightRight: Boolean = false,
    badgeText: String? = null
) {
    Column {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = CosmicGrayText,
            fontSize = 10.sp,
            fontWeight = FontWeightCompose.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left column cell
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (highlightLeft) AccentGreen.copy(alpha = 0.12f)
                        else CosmicSurfaceHeader.copy(alpha = 0.2f)
                    )
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = if (highlightLeft) AccentGreen.copy(alpha = 0.3f) else CosmicCardBorder.copy(alpha = 0.3f)
                        ),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = leftValue,
                            color = if (highlightLeft) AccentGreen else CosmicWhite,
                            fontSize = 11.sp,
                            fontWeight = if (highlightLeft) FontWeightCompose.Bold else FontWeightCompose.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (highlightLeft && badgeText != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = AccentGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeightCompose.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Right column cell
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (highlightRight) AccentGreen.copy(alpha = 0.12f)
                        else CosmicSurfaceHeader.copy(alpha = 0.2f)
                    )
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = if (highlightRight) AccentGreen.copy(alpha = 0.3f) else CosmicCardBorder.copy(alpha = 0.3f)
                        ),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = rightValue,
                            color = if (highlightRight) AccentGreen else CosmicWhite,
                            fontSize = 11.sp,
                            fontWeight = if (highlightRight) FontWeightCompose.Bold else FontWeightCompose.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (highlightRight && badgeText != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = AccentGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeightCompose.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = CosmicCardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
    }
}

private fun getCategoryEmoji(category: PartCategory): String {
    return when (category) {
        PartCategory.CPU -> "💻"
        PartCategory.MOTHERBOARD -> "🔌"
        PartCategory.RAM -> "💾"
        PartCategory.GPU -> "🎨"
        PartCategory.PSU -> "⚡"
        PartCategory.STORAGE -> "🗄️"
        PartCategory.COOLER -> "❄️"
        PartCategory.CASE -> "📦"
    }
}

@Composable
fun PerformanceChartSection(result: BenchmarkCompareResult) {
    val pmA = result.passmarkScoreA
    val pmB = result.passmarkScoreB
    val ubA = result.userBenchmarkScoreA
    val ubB = result.userBenchmarkScoreB

    if (pmA == 0 && pmB == 0 && ubA == 0 && ubB == 0) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicSurfaceHeader.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .testTag("performance_analysis_column"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "⚡ PERFORMANCE GRAPHICS & SCALING MATRIX",
            color = CosmicPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeightCompose.Bold,
            letterSpacing = 0.5.sp
        )

        // 1. PassMark (CPU / GPU Mark) Comparer if values exist
        if (pmA > 0 || pmB > 0) {
            val maxPm = java.lang.Math.max(pmA, pmB).toFloat()
            val ratioA = if (maxPm > 0) (pmA.toFloat() / maxPm) else 0f
            val ratioB = if (maxPm > 0) (pmB.toFloat() / maxPm) else 0f

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PassMark Performance Score (Higher is better)",
                        color = CosmicWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeightCompose.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CosmicPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PASSMARK SCORE",
                            color = CosmicPrimary,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeightCompose.Bold
                        )
                    }
                }

                // Bar for Part A
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result.partAName,
                            color = CosmicGrayText,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%,d Pts", pmA),
                            color = CosmicWhite,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeightCompose.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratioA)
                                .fillMaxHeight()
                                .background(
                                    if (pmA >= pmB) AccentGreen else CosmicPrimary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                // Bar for Part B
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result.partBName,
                            color = CosmicGrayText,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%,d Pts", pmB),
                            color = CosmicWhite,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeightCompose.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratioB)
                                .fillMaxHeight()
                                .background(
                                    if (pmB >= pmA) AccentGreen else CosmicSecondary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }

        // 2. UserBenchmark % Comparer if values exist
        if (ubA > 0 || ubB > 0) {
            val maxUb = java.lang.Math.max(ubA, ubB).toFloat()
            val ratioA = if (maxUb > 0) (ubA.toFloat() / maxUb) else 0f
            val ratioB = if (maxUb > 0) (ubB.toFloat() / maxUb) else 0f

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HorizontalDivider(color = CosmicCardBorder.copy(alpha = 0.2f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UserBenchmark / Overall Speed Index",
                        color = CosmicWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeightCompose.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CosmicSecondary.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "USERBENCHMARK %",
                            color = CosmicSecondary,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeightCompose.Bold
                        )
                    }
                }

                // Bar for Part A
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result.partAName,
                            color = CosmicGrayText,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "%$ubA",
                            color = CosmicWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeightCompose.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratioA)
                                .fillMaxHeight()
                                .background(
                                    if (ubA >= ubB) AccentGreen else CosmicPrimary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                // Bar for Part B
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result.partBName,
                            color = CosmicGrayText,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "%$ubB",
                            color = CosmicWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeightCompose.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratioB)
                                .fillMaxHeight()
                                .background(
                                    if (ubB >= ubA) AccentGreen else CosmicSecondary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
