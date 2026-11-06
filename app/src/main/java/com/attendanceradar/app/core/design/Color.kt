package com.attendanceradar.app.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand & surface tokens — Light
val LightPrimary = Color(0xFF1B4FD8)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDCE4FF)
val LightOnPrimaryContainer = Color(0xFF001551)

val LightSecondary = Color(0xFF0A8765)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFC4F5E2)
val LightOnSecondaryContainer = Color(0xFF002116)

val LightBackground = Color(0xFFF7F8FA)
val LightOnBackground = Color(0xFF111827)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF111827)
val LightSurfaceVariant = Color(0xFFEEF1F6)
val LightOnSurfaceVariant = Color(0xFF4B5563)
val LightOutline = Color(0xFFD1D5DB)
val LightOutlineVariant = Color(0xFFE5E7EB)

// Brand & surface tokens — Dark (Deep navy/charcoal surfaces)
val DarkPrimary = Color(0xFF6B9BFF)
val DarkOnPrimary = Color(0xFF002682)
val DarkPrimaryContainer = Color(0xFF173B9E)
val DarkOnPrimaryContainer = Color(0xFFDCE4FF)

val DarkSecondary = Color(0xFF1EE6A8)
val DarkOnSecondary = Color(0xFF003826)
val DarkSecondaryContainer = Color(0xFF005139)
val DarkOnSecondaryContainer = Color(0xFFA6F7D8)

val DarkBackground = Color(0xFF0B1221)
val DarkOnBackground = Color(0xFFF3F4F6)
val DarkSurface = Color(0xFF131C2E)
val DarkOnSurface = Color(0xFFF3F4F6)
val DarkSurfaceVariant = Color(0xFF1C273D)
val DarkOnSurfaceVariant = Color(0xFF9CA3AF)
val DarkOutline = Color(0xFF374151)
val DarkOutlineVariant = Color(0xFF243047)

// Semantic Attendance Status Colors (always paired with text/icons in UI)
@Immutable
data class SemanticAttendanceColors(
    val onTrackForeground: Color,
    val onTrackContainer: Color,
    val atRiskForeground: Color,
    val atRiskContainer: Color,
    val belowTargetForeground: Color,
    val belowTargetContainer: Color,
    val neutralForeground: Color,
    val neutralContainer: Color,
    val brandSymbolAccent: Color,
    val brandSymbolTrack: Color
)

val LightSemanticColors = SemanticAttendanceColors(
    onTrackForeground = Color(0xFF066A4E),
    onTrackContainer = Color(0xFFD1FAE5),
    atRiskForeground = Color(0xFF92400E),
    atRiskContainer = Color(0xFFFEF3C7),
    belowTargetForeground = Color(0xFFB91C1C),
    belowTargetContainer = Color(0xFFFEE2E2),
    neutralForeground = Color(0xFF4B5563),
    neutralContainer = Color(0xFFE5E7EB),
    brandSymbolAccent = Color(0xFF0A8765),
    brandSymbolTrack = Color(0xFFD1D5DB)
)

val DarkSemanticColors = SemanticAttendanceColors(
    onTrackForeground = Color(0xFF34D399),
    onTrackContainer = Color(0xFF064E3B),
    atRiskForeground = Color(0xFFFBBF24),
    atRiskContainer = Color(0xFF5A3408),
    belowTargetForeground = Color(0xFFF87171),
    belowTargetContainer = Color(0xFF5F1419),
    neutralForeground = Color(0xFF9CA3AF),
    neutralContainer = Color(0xFF1F2937),
    brandSymbolAccent = Color(0xFF1EE6A8),
    brandSymbolTrack = Color(0xFF26334D)
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

// Restrained built-in subject accent palette
object SubjectPalette {
    val Colors: List<String> = listOf(
        "#1B4FD8", // Deep Blue
        "#0A8765", // Teal Mint
        "#6D28D9", // Indigo Violet
        "#B45309", // Warm Amber
        "#BE123C", // Crimson Rose
        "#0369A1", // Ocean Cyan
        "#4338CA", // Slate Indigo
        "#15803D"  // Forest Emerald
    )

    const val DEFAULT_HEX = "#1B4FD8"

    fun isValidHex(hex: String): Boolean {
        val normalized = hex.trim().uppercase()
        return Regex("^#[0-9A-F]{6}$").matches(normalized)
    }

    fun parseColorOrDefault(hex: String): Color {
        val clean = hex.trim().removePrefix("#")
        if (clean.length != 6) return Color(0xFF1B4FD8)
        return try {
            val rgb = clean.toLong(16)
            Color(0xFF000000 or rgb)
        } catch (_: NumberFormatException) {
            Color(0xFF1B4FD8)
        }
    }
}
