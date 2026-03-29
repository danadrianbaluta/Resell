package com.resell.app.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val purchasePrice: String = "",
    val imageUri: String = "",
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
    LISTED("Listed products"),
    UNLISTED("Unlisted products"),
    SOLD("Sold products"),
    DELETED("Inactive products"),
    ALL("All products")
}

enum class AppScreen(val label: String) {
    MAIN("Products"),
    DETAILS("Details"),
    SUMMARY("Summary")
}

fun ProductFilter.summaryLabel(count: Int): String = when (this) {
    ProductFilter.ALL -> "$count products"
    ProductFilter.LISTED -> "$count listed"
    ProductFilter.UNLISTED -> "$count unlisted"
    ProductFilter.SOLD -> "$count sold"
    ProductFilter.DELETED -> "$count inactive"
}

fun Product.normalized(): Product {
    val alignedPlatforms = PlatformType.entries.map { type ->
        platforms.firstOrNull { it.platform == type } ?: PlatformListing(platform = type)
    }
    return copy(platforms = alignedPlatforms)
}

fun Product.matchesFilter(filter: ProductFilter): Boolean {
    val normalized = normalized()
    val hasListings = normalized.platforms.any { it.dateListed.isNotBlank() || it.price.isNotBlank() }
    val hasSales = normalized.platforms.any { it.sold || it.dateSold.isNotBlank() || it.finalPrice.isNotBlank() }
    return when (filter) {
        ProductFilter.LISTED -> !deleted && hasListings && !hasSales
        ProductFilter.UNLISTED -> !deleted && !hasListings && !hasSales
        ProductFilter.SOLD -> !deleted && hasSales
        ProductFilter.DELETED -> deleted
        ProductFilter.ALL -> true
    }
}

fun Product.hasActivityBetween(start: LocalDate, end: LocalDate): Boolean {
    return platforms.any { listing ->
        parseDateOrNull(listing.dateListed)?.let { !it.isBefore(start) && !it.isAfter(end) } == true ||
            parseDateOrNull(listing.dateSold)?.let { !it.isBefore(start) && !it.isAfter(end) } == true
    }
}

fun parseDateOrNull(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

fun parseAmount(value: String): Double = value.replace(",", ".").toDoubleOrNull() ?: 0.0
