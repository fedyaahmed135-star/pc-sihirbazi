package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemeOption(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val surfaceHeaderColor: Color,
    val cardBorderColor: Color,
    val whiteColor: Color,       // Primary text
    val grayTextColor: Color,    // Secondary text
    val description: String,
    val emoji: String,
    val isLight: Boolean = false
) {
    COSMIC_AMBER(
        "cosmic_amber",
        "Cosmic Kehribar",
        Color(0xFFFF9F1C),
        Color(0xFF38BDF8),
        Color(0xFF0B0F19),
        Color(0xFF151D30),
        Color(0xFF1E294B),
        Color(0xFF334155),
        Color(0xFFF8FAFC),
        Color(0xFF94A3B8),
        "Gece temalı uzay turuncusu",
        ""
    ),
    BEYAZ(
        "beyaz",
        "Buzul Beyazı (Aydınlık)",
        Color(0xFF2563EB),
        Color(0xFF0EA5E9),
        Color(0xFFF1F5F9), // Light Slate Background
        Color(0xFFFFFFFF), // Pure White Card Surface
        Color(0xFFE2E8F0), // Card header contrast slate
        Color(0xFFCBD5E1), // Light border gray
        Color(0xFF0F172A), // Dark slate text for high readability
        Color(0xFF64748B), // Slate gray helper/description text
        "Temiz, aydınlık ve sade arayüz tasarımı",
        "",
        true
    ),
    SIYAH(
        "siyah",
        "Saf Gece Siyahı (OLED)",
        Color(0xFFF43F5E), // Intense Neon Rose Accent
        Color(0xFF10B981), // Vivid Emerald Green Accent
        Color(0xFF000000), // Pure OLED pitch black
        Color(0xFF121212), // Deep charcoal surface
        Color(0xFF1E1E1E), // Slightly lighter surface for headers
        Color(0xFF262626), // Silent boundary border
        Color(0xFFFFFFFF), // High contrast clean white text
        Color(0xFF9E9E9E), // Neutral mid-gray text
        "Yüksek kontrastlı saf OLED siyah tasarım",
        ""
    ),
    MAVI(
        "mavi",
        "Siber Mavi",
        Color(0xFF38BDF8), // Ice blue neon
        Color(0xFF818CF8), // Cyan/indigo glow
        Color(0xFF030712), // Deep galactic navy-black
        Color(0xFF111827), // Deep space indigo-gilded card
        Color(0xFF1F2937), // Navy-gray card sub-headers
        Color(0xFF374151), // Cosmic navy boundary lines
        Color(0xFFE0E7FF), // Lavender slate white
        Color(0xFF9CA3AF), // Medium gray
        "Siberpunk tarzı derin uzay mavisi",
        ""
    ),
    KIRMIZI(
        "kirmizi",
        "Ateş Kırmızısı",
        Color(0xFFEF4444), // Performance Engine red
        Color(0xFFFF9F1C), // Combustion amber
        Color(0xFF0F0404), // Midnight crimson background
        Color(0xFF1A0A0A), // Molten iron dark red surface
        Color(0xFF2E1010), // Burned copper header surface
        Color(0xFF4C1D1D), // Obsidian red card border
        Color(0xFFFFE4E4), // Pale coral tinted white text
        Color(0xFFFCA5A5), // Muted red-gray subtext
        "Yüksek performanslı overclock esintisi",
        ""
    ),
    SARI(
        "sari",
        "Altın Sarı",
        Color(0xFFEAB308), // Cyberpunk bumblebee gold
        Color(0xFF10B981), // Cyber green complement
        Color(0xFF0C0A02), // Yellowed dark space background
        Color(0xFF181504), // Dark copper-gold surface card
        Color(0xFF272207), // Gold accent sub-containers
        Color(0xFF3F370C), // Brass gilded card border
        Color(0xFFFEF08A), // Soft pale yellow text
        Color(0xFFCA8A04), // Golden-rod helper text
        "Siberpunk temalı fütüristik altın ve gold tınısı",
        ""
    );

    companion object {
        fun fromId(id: String): ThemeOption {
            return values().find { it.id == id } ?: COSMIC_AMBER
        }
    }
}
