package com.resell.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.resell.app.data.AppScreen
import com.resell.app.data.Product
import com.resell.app.data.ProductFilter
import com.resell.app.data.matchesFilter
import com.resell.app.data.summaryLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    products: List<Product>,
    onSelectScreen: (AppScreen) -> Unit,
    onAdd: () -> Unit,
    onOpenProduct: (String) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(ProductFilter.LISTED) }
    var filterExpanded by remember { mutableStateOf(false) }
    var quoteText by remember { mutableStateOf("Loading quote of the day...") }
    var quoteAuthor by remember { mutableStateOf("") }
    val filteredProducts = products.filter { it.matchesFilter(filter) }

    LaunchedEffect(Unit) {
        val quote = fetchQuoteOfTheDay()
        if (quote != null) {
            quoteText = quote.first
            quoteAuthor = quote.second
        } else {
            quoteText = "Keep listings, purchases, and sales in one place with fast status visibility."
            quoteAuthor = ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = BrandPurple,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add product")
                }
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Products", style = MaterialTheme.typography.titleLarge)
                Text(filter.summaryLabel(filteredProducts.size), style = MaterialTheme.typography.labelMedium, color = MutedInk)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ScreenSelector(current = AppScreen.MAIN, onSelected = onSelectScreen, modifier = Modifier.fillMaxWidth())
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        FilledTonalButton(
                            onClick = { filterExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BrandPurple,
                                contentColor = Color.White
                            )
                        ) {
                            Text(filter.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                            ProductFilter.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        filter = option
                                        filterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFAAC2D0), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Quote of the day", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text(
                        quoteText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink
                    )
                    if (quoteAuthor.isNotBlank()) {
                        Text(
                            quoteAuthor,
                            style = MaterialTheme.typography.labelMedium,
                            color = Ink
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { onOpenProduct(product.id) })
                }
            }
        }
    }
}

private suspend fun fetchQuoteOfTheDay(): Pair<String, String>? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL("https://zenquotes.io/api/today").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }
        connection.inputStream.bufferedReader().use { reader ->
            val response = reader.readText()
            val item = JSONArray(response).getJSONObject(0)
            val quote = item.optString("q").trim()
            val author = item.optString("a").trim()
            quote to if (author.isBlank()) "" else " - $author"
        }
    }.getOrNull()
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val listedPlatforms = product.platforms
        .filter { it.dateListed.isNotBlank() || it.price.isNotBlank() }
        .joinToString(", ") { it.platform.label }
    val soldOn = product.platforms.firstOrNull { it.sold }?.platform?.label

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.55f)
                    .background(Brush.linearGradient(listOf(Color(0xFFF5F7FB), Color(0xFFE8EEF9)))),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUri.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = product.description,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        tint = MutedInk,
                        modifier = Modifier.size(42.dp)
                    )
                }

                if (soldOn != null) {
                    Text(
                        text = "Sold on $soldOn",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(BrandGreen, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.15f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = product.description.ifBlank { "Untitled product" },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (product.purchasePrice.isNotBlank()) {
                        Text(
                            text = "Bought for ${product.purchasePrice}",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandOrange
                        )
                    }
                }
                Text(
                    text = if (listedPlatforms.isBlank()) "Listed on: none" else "Listed on: $listedPlatforms",
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedInk
                )
            }
        }
    }
}
