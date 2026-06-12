package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

data class GameFpsSpecs(
    val name: String,
    val description: String,
    val emoji: String,
    val baseFps: Int,
    val isCpuHeavy: Boolean,
    val isGpuHeavy: Boolean
)

object GamesDatabase {
    val games = listOf(
        GameFpsSpecs("Counter-Strike 2 (CS2)", "Highly competitive tactical shooter. High refresh rates are crucial.", "🔫", 120, isCpuHeavy = true, isGpuHeavy = false),
        GameFpsSpecs("Cyberpunk 2077", "Advanced ray tracing and massive futuristic neon metropolis. Extreme GPU load.", "🦾", 40, isCpuHeavy = false, isGpuHeavy = true),
        GameFpsSpecs("Grand Theft Auto V", "Immense open world action. Balanced CPU/GPU optimization.", "🚗", 60, isCpuHeavy = true, isGpuHeavy = true),
        GameFpsSpecs("Valorant", "Lightweight competitive character-based FPS. Dependent on CPU single-core performance.", "🎯", 150, isCpuHeavy = true, isGpuHeavy = false),
        GameFpsSpecs("Red Dead Redemption 2", "Vast highly detailed realistic frontier atmosphere. Heavily GPU bound.", "🤠", 35, isCpuHeavy = false, isGpuHeavy = true)
    )
}

@Composable
fun GamingPerformanceTab(
    uiState: BuilderUIState,
    viewModel: PCBuilderViewModel
) {
    val cpu = remember(uiState.selectedParts) { uiState.selectedParts[PartCategory.CPU] }
    val gpu = remember(uiState.selectedParts) { uiState.selectedParts[PartCategory.GPU] }

    val cpuIndex = remember(cpu) { cpu?.let { getCpuPowerIndex(it) } ?: 0 }
    val gpuIndex = remember(gpu) { gpu?.let { getGpuPowerIndex(it) } ?: 0 }
    val resMultiplier = remember(uiState.selectedResolution) {
        when (uiState.selectedResolution) {
            0 -> 1.0   // 1080p
            1 -> 0.72  // 1440p (2K)
            else -> 0.45 // 4K UHD
        }
    }

    val resolutionLabels = remember { listOf("1080p FHD", "1440p (2K)", "2160p (4K UHD)") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Header Explanation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎮 GAMING PERFORMANCE SIMULATOR",
                        color = CosmicPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Estimated frame rates (FPS) are calculated in real-time based on your picked processor (CPU) and graphics card (GPU).",
                        color = CosmicGrayText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp)
                    )
                }
            }
        }

        // Active Specs Block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ACTIVE PROCESSOR (CPU)", color = CosmicGrayText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = cpu?.getFullName() ?: "NOT SELECTED",
                            color = if (cpu != null) CosmicWhite else AccentOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("ACTIVE GRAPHICS CARD (GPU)", color = CosmicGrayText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = gpu?.getFullName() ?: "NOT SELECTED",
                            color = if (gpu != null) CosmicWhite else AccentOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Resolution Switcher
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Target Display Resolution",
                    color = CosmicWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicSurface, RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    resolutionLabels.forEachIndexed { index, label ->
                        val isSelected = uiState.selectedResolution == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CosmicPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.setResolution(index) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) CosmicPrimary else CosmicGrayText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Validate if processor and graphic cards exist
        if (cpu == null || gpu == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎮", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Add Hardware to Estimate Frames",
                            color = CosmicWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please assign a CPU and GPU in the builder tab to unlock gaming simulator diagnostics.",
                            color = CosmicGrayText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(GamesDatabase.games, key = { game -> game.name }) { game ->
                val simulatedFps = remember(game, cpuIndex, gpuIndex, resMultiplier) {
                    calculateSimulatedFps(game, cpuIndex, gpuIndex, resMultiplier)
                }
                GameFpsCard(game = game, fps = simulatedFps)
            }
        }
    }
}

@Composable
fun GameFpsCard(game: GameFpsSpecs, fps: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicCardBorder.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CosmicSurfaceHeader.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(game.emoji, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.name,
                        color = CosmicWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = game.description,
                        color = CosmicGrayText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Big Frame rating indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$fps FPS",
                        color = when {
                            fps >= 144 -> CosmicSecondary
                            fps >= 60 -> AccentGreen
                            fps >= 30 -> AccentOrange
                            else -> AccentRed
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Text(
                        text = when {
                            fps >= 144 -> "Exceptional (Esports)"
                            fps >= 60 -> "Fluid (60 FPS+)"
                            fps >= 30 -> "Playable"
                            else -> "May Struggle"
                        },
                        color = CosmicGrayText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom colored frame rate progress bar
            LinearProgressIndicator(
                progress = { (fps / 240f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = when {
                    fps >= 144 -> CosmicSecondary
                    fps >= 60 -> AccentGreen
                    fps >= 30 -> AccentOrange
                    else -> AccentRed
                },
                trackColor = CosmicCardBorder.copy(alpha = 0.4f)
            )
        }
    }
}

// Power Indices
private fun getCpuPowerIndex(cpu: HardwarePart): Int {
    val name = cpu.modelOrSpecs.lowercase() + " " + cpu.brand.lowercase()
    return when {
        name.contains("7800x3d") || name.contains("9800x3d") -> 10
        name.contains("14900") || name.contains("13900") || name.contains("9950x") -> 9
        name.contains("14600") || name.contains("7600") || name.contains("13600") -> 8
        name.contains("5600") || name.contains("12400") -> 5
        name.contains("12100") || name.contains("5500") -> 4
        cpu.priceUsd > 400 -> 9
        cpu.priceUsd > 250 -> 8
        cpu.priceUsd > 150 -> 6
        else -> 4
    }
}

private fun getGpuPowerIndex(gpu: HardwarePart): Int {
    val name = gpu.modelOrSpecs.lowercase()
    return when {
        name.contains("4090") -> 10
        name.contains("4080") -> 9
        name.contains("4070") || name.contains("7800 xt") -> 8
        name.contains("4060 ti") || name.contains("7700") -> 6
        name.contains("4060") || name.contains("7600") -> 5
        name.contains("6600") -> 4
        gpu.priceUsd > 800 -> 9
        gpu.priceUsd > 500 -> 8
        gpu.priceUsd > 300 -> 6
        else -> 4
    }
}

private fun calculateSimulatedFps(
    game: GameFpsSpecs,
    cpuIndex: Int,
    gpuIndex: Int,
    resMultiplier: Double
): Int {
    val base = when {
        game.isCpuHeavy && !game.isGpuHeavy -> {
            // CPU intensive (Valorant, CS2)
            (cpuIndex * 20 + gpuIndex * 4) * 1.5
        }
        game.isGpuHeavy && !game.isCpuHeavy -> {
            // GPU intensive (Cyberpunk 2077, RDR2)
            (gpuIndex * 18 + cpuIndex * 2) * 0.95
        }
        else -> {
            // Balanced (GTA V)
            (gpuIndex * 11 + cpuIndex * 11) * 1.05
        }
    }

    // Apply resolution multiplier (higher resolutions lower the framerate)
    val scaleFactor = if (game.isCpuHeavy && !game.isGpuHeavy) {
        // CPU heavy games lose less FPS at higher resolutions
        java.lang.Math.max(resMultiplier, 0.70)
    } else {
        resMultiplier
    }

    val result = (base * scaleFactor).toInt()
    return java.lang.Math.max(result, 15) // absolute low cap
}
