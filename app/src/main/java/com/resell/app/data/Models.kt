package com.resell.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: String = formatDateForDisplay(LocalDate.now()),
    val description: String = "",
    val purchasePrice: String = "",
    val imageUri: String = "",
    val storageLocation: String = "",
    val expenses: String = "",
    val deleted: Boolean = false,
    val platforms: List<PlatformListing> = PlatformType.entries.map { PlatformListing(platform = it) }
)

@Serializable
data class PlatformListing(
    val platform: PlatformType,
    val dateListed: String = "",
    val price: String = "",
    val sold: Boolean = false,
    val dateSold: String = "",
    val finalPrice: String = ""
)

@Serializable
enum class PlatformType(val label: String) {
    VINTED("Vinted"),
    EBAY("eBay"),
    ETSY("Etsy")
}

enum class ProductFilter(val label: String) {
    LISTED("Listed items"),
    UNLISTED("Unlisted items"),
    SOLD("Sold items"),
    INACTIVE("Inactive items")
}

enum class AppScreen(val label: String) {
    MAIN("Products"),
    DETAILS("Details"),
    SUMMARY("Summary"),
    SETTINGS("Settings")
}

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: String,
    val products: List<BackupProduct>
)

@Serializable
data class BackupProduct(
    val id: String,
    val createdAt: String = "",
    val description: String,
    val purchasePrice: String,
    val storageLocation: String = "",
    val expenses: String,
    val deleted: Boolean,
    val platforms: List<PlatformListing>,
    val image: BackupImage? = null
)

@Serializable
data class BackupImage(
    @SerialName("file_name") val fileName: String,
    @SerialName("mime_type") val mimeType: String = "image/jpeg",
    @SerialName("base64_data") val base64Data: String
)

@Serializable
data class BackupPreferences(
    val autoBackupEnabled: Boolean = false,
    val driveFolderUri: String = ""
)

fun ProductFilter.summaryLabel(count: Int): String = when (this) {
    ProductFilter.LISTED -> "$count listed"
    ProductFilter.UNLISTED -> "$count unlisted"
    ProductFilter.SOLD -> "$count sold"
    ProductFilter.INACTIVE -> "$count inactive"
}

fun Product.normalized(createdAtFallback: String? = null): Product {
    val alignedPlatforms = PlatformType.entries.map { type ->
        (platforms.firstOrNull { it.platform == type } ?: PlatformListing(platform = type)).normalized()
    }
    return copy(
        createdAt = when {
            createdAt.isNotBlank() -> formatDateForDisplay(createdAt)
            !createdAtFallback.isNullOrBlank() -> formatDateForDisplay(createdAtFallback)
            else -> formatDateForDisplay(LocalDate.now())
        },
        platforms = alignedPlatforms
    )
}

fun PlatformListing.normalized(): PlatformListing = copy(
    dateListed = formatDateForDisplay(dateListed),
    dateSold = formatDateForDisplay(dateSold)
)

fun Product.matchesFilter(filter: ProductFilter): Boolean {
    val normalized = normalized()
    val hasListings = normalized.platforms.any { it.dateListed.isNotBlank() || it.price.isNotBlank() }
    val hasSales = normalized.platforms.any { it.sold || it.dateSold.isNotBlank() || it.finalPrice.isNotBlank() }
    return when (filter) {
        ProductFilter.LISTED -> !deleted && hasListings && !hasSales
        ProductFilter.UNLISTED -> !deleted && !hasListings && !hasSales
        ProductFilter.SOLD -> !deleted && hasSales
        ProductFilter.INACTIVE -> deleted
    }
}

fun Product.hasActivityBetween(start: LocalDate, end: LocalDate): Boolean {
    return platforms.any { listing ->
        parseDateOrNull(listing.dateListed)?.let { !it.isBefore(start) && !it.isAfter(end) } == true ||
            parseDateOrNull(listing.dateSold)?.let { !it.isBefore(start) && !it.isAfter(end) } == true
    }
}

private val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
private val legacyIsoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun formatDateForDisplay(value: String): String {
    val parsed = parseDateOrNull(value) ?: return value
    return parsed.format(displayDateFormatter)
}

fun formatDateForDisplay(date: LocalDate): String = date.format(displayDateFormatter)

fun parseDateOrNull(value: String): LocalDate? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    return runCatching { LocalDate.parse(trimmed, displayDateFormatter) }.getOrNull()
        ?: runCatching { LocalDate.parse(trimmed, legacyIsoDateFormatter) }.getOrNull()
}

fun parseAmount(value: String): Double = value.replace(",", ".").toDoubleOrNull() ?: 0.0

fun inferCreatedAtFromImageUri(imageUri: String): String? {
    val fileName = runCatching { android.net.Uri.parse(imageUri).lastPathSegment }.getOrNull().orEmpty()
    val timestamp = fileName
        .substringAfterLast('_', "")
        .substringBefore('.')
        .toLongOrNull()
        ?: return null

    val localDate = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return formatDateForDisplay(localDate)
}
