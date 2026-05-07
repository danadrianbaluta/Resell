package com.resell.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: String = "",
    val description: String = "",
    val purchasePrice: String = "",
    val imageUri: String = "",
    val storageLocation: String = "",
    val expenses: String = "",
    val deleted: Boolean = false,
    val platforms: List<PlatformListing> = PlatformType.entries.map { PlatformListing(platform = it) }
)

@Serializable
data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val amount: String = "",
    val date: String = "",
    val comment: String = ""
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
    ETSY("Etsy"),
    FACEBOOK("Facebook")
}

enum class ProductFilter(val label: String) {
    LISTED("Listed"),
    UNLISTED("Unlisted"),
    SOLD("Sold"),
    INACTIVE("Inactive")
}

enum class AppScreen(val label: String) {
    MAIN("Products"),
    DETAILS("Details"),
    EXPENSES("Expenses"),
    EXPENSE_DETAILS("Expense details"),
    SUMMARY("Summary"),
    SETTINGS("Settings")
}

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: String,
    val products: List<BackupProduct>,
    val expenses: List<BackupExpense> = emptyList()
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
    @SerialName("timestamp_ms") val timestampMillis: Long? = null,
    @SerialName("mime_type") val mimeType: String = "image/jpeg",
    @SerialName("base64_data") val base64Data: String
)

@Serializable
data class BackupExpense(
    val id: String,
    val amount: String = "",
    val date: String = "",
    val comment: String = ""
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

fun Product.normalized(): Product {
    val alignedPlatforms = PlatformType.entries.map { type ->
        (platforms.firstOrNull { it.platform == type } ?: PlatformListing(platform = type)).normalized()
    }
    return copy(
        createdAt = createdAt.trim().takeIf { it.isNotBlank() }?.let(::formatCreatedAtForDisplay).orEmpty(),
        platforms = alignedPlatforms
    )
}

fun Expense.normalized(): Expense = copy(
    amount = amount.trim(),
    date = formatDateForDisplay(date),
    comment = comment.trim()
)

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
private val displayDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
private val legacyIsoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val legacyIsoDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun formatDateForDisplay(value: String): String {
    val parsed = parseDateOrNull(value) ?: return value
    return parsed.format(displayDateFormatter)
}

fun formatDateForDisplay(date: LocalDate): String = date.format(displayDateFormatter)

fun formatCreatedAtForDisplay(dateTime: LocalDateTime): String = dateTime.format(displayDateTimeFormatter)

fun parseDateOrNull(value: String): LocalDate? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    return runCatching { LocalDate.parse(trimmed, displayDateFormatter) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(trimmed, displayDateTimeFormatter).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(trimmed, legacyIsoDateFormatter) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(trimmed, legacyIsoDateTimeFormatter).toLocalDate() }.getOrNull()
}

fun parseCreatedAtOrNull(value: String): LocalDateTime? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    return runCatching { LocalDateTime.parse(trimmed, displayDateTimeFormatter) }.getOrNull()
        ?: runCatching { LocalDate.parse(trimmed, displayDateFormatter).atStartOfDay() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(trimmed, legacyIsoDateTimeFormatter) }.getOrNull()
        ?: runCatching { LocalDate.parse(trimmed, legacyIsoDateFormatter).atStartOfDay() }.getOrNull()
}

private fun hasCreatedAtTime(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return false
    return runCatching { LocalDateTime.parse(trimmed, displayDateTimeFormatter) }.isSuccess ||
        runCatching { LocalDateTime.parse(trimmed, legacyIsoDateTimeFormatter) }.isSuccess
}

fun Product.createdAtSortDateTime(): LocalDateTime? {
    val parsedCreatedAt = parseCreatedAtOrNull(createdAt)
    if (parsedCreatedAt != null && hasCreatedAtTime(createdAt)) return parsedCreatedAt

    val imageCreatedAt = inferImageTimestampFromImageUri(imageUri)
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }

    return if (
        parsedCreatedAt != null &&
        imageCreatedAt != null &&
        imageCreatedAt.toLocalDate() == parsedCreatedAt.toLocalDate()
    ) {
        imageCreatedAt
    } else {
        parsedCreatedAt ?: imageCreatedAt
    }
}

fun formatCreatedAtForDisplay(value: String): String {
    val parsed = parseCreatedAtOrNull(value) ?: return value
    return parsed.format(displayDateTimeFormatter)
}

fun parseAmount(value: String): Double = value.replace(",", ".").toDoubleOrNull() ?: 0.0

fun inferImageTimestampFromImageUri(imageUri: String): Long? {
    val fileName = runCatching { android.net.Uri.parse(imageUri).lastPathSegment }.getOrNull().orEmpty()
    return fileName
        .substringAfterLast('_', "")
        .substringBefore('.')
        .toLongOrNull()
}

fun inferImageDateFromImageUri(imageUri: String): String? {
    val timestamp = inferImageTimestampFromImageUri(imageUri) ?: return null
    val localDate = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return formatDateForDisplay(localDate)
}
