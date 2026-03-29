package com.resell.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.resell.app.data.AppScreen
import com.resell.app.data.PlatformListing
import com.resell.app.data.Product
import com.resell.app.data.ProductRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    existingProduct: Product?,
    repository: ProductRepository,
    onAfterSave: (Product) -> Unit,
    onAfterDelete: () -> Unit,
    onSelectScreen: (AppScreen) -> Unit
) {
    val context = LocalContext.current
    val original = remember(existingProduct) { existingProduct ?: Product() }
    var draft by remember(existingProduct?.id) { mutableStateOf(original) }
    var pendingScreen by remember { mutableStateOf<AppScreen?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val hasChanges = draft != original
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val importedUri = importImageToAppStorage(context, uri, draft.id)
                if (importedUri != null) draft = draft.copy(imageUri = importedUri)
            }
        }
    }

    fun saveAndThen(after: () -> Unit) {
        scope.launch {
            repository.saveProduct(draft)
            after()
        }
    }

    BackHandler(enabled = hasChanges) {
        pendingScreen = AppScreen.MAIN
        showUnsavedDialog = true
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Product details", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (draft.description.isBlank()) "Create or refine a listing-ready item" else draft.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedInk
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScreenSelector(current = AppScreen.DETAILS, onSelected = { screen ->
                        if (screen == AppScreen.DETAILS) return@ScreenSelector
                        if (hasChanges) {
                            pendingScreen = screen
                            showUnsavedDialog = true
                        } else {
                            onSelectScreen(screen)
                        }
                    }, modifier = Modifier.weight(1f))
                    Button(
                        onClick = { saveAndThen { onAfterSave(draft) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) {
                        Text("Save")
                    }
                }
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Core details", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = draft.description,
                        onValueChange = { draft = draft.copy(description = it) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        label = { Text("Description") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .clickable { imagePicker.launch("image/*") }
                                .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (draft.imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = draft.imageUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(34.dp))
                                    Text("Add image", color = BrandPurple, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = draft.purchasePrice,
                                onValueChange = { draft = draft.copy(purchasePrice = it) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Purchase price") },
                                prefix = {
                                    if (draft.purchasePrice.isNotBlank()) Text("\u00A3")
                                },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = draft.expenses,
                                onValueChange = { draft = draft.copy(expenses = it) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Expenses") },
                                prefix = {
                                    if (draft.expenses.isNotBlank()) Text("\u00A3")
                                },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Active", color = MutedInk)
                                Switch(
                                    checked = !draft.deleted,
                                    onCheckedChange = { checked -> draft = draft.copy(deleted = !checked) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BrandPurple
                                    )
                                )
                            }
                        }
                    }
                }
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sales channels", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Once one platform is sold, the others lock to avoid conflicting sale records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk
                    )
                    val soldPlatform = draft.platforms.firstOrNull { it.sold }?.platform
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        draft.platforms.forEach { listing ->
                            PlatformSection(
                                listing = listing,
                                enabled = soldPlatform == null || soldPlatform == listing.platform,
                                onListingChange = { updated ->
                                    draft = draft.copy(platforms = draft.platforms.map {
                                        if (it.platform == updated.platform) updated else it
                                    })
                                }
                            )
                        }
                    }
                }
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Permanent delete", style = MaterialTheme.typography.titleMedium, color = BrandOrange)
                        Text("Remove this product forever. This cannot be undone.", style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                    }
                    Button(
                        onClick = { showDeleteDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                    }
                }
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Save changes?") },
            text = { Text("You have unsaved changes. Do you want to save before leaving this page?") },
            confirmButton = {
                TextButton(onClick = {
                    saveAndThen {
                        val next = pendingScreen
                        pendingScreen = null
                        showUnsavedDialog = false
                        if (next == null || next == AppScreen.MAIN) onAfterSave(draft) else onSelectScreen(next)
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val next = pendingScreen
                    pendingScreen = null
                    showUnsavedDialog = false
                    if (next != null) onSelectScreen(next)
                }) { Text("No") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete permanently?") },
            text = { Text("Are you sure? This action is irreversible.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteProduct(draft.id)
                        showDeleteDialog = false
                        onAfterDelete()
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private suspend fun importImageToAppStorage(
    context: android.content.Context,
    sourceUri: Uri,
    productId: String
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val imagesDir = File(context.filesDir, "product-images").apply { mkdirs() }
        val targetFile = File(imagesDir, "${productId}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Unable to open selected image" }
            val sourceBytes = input.readBytes()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, bounds)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 512, 512)
            }
            val bitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, decodeOptions)
                ?: error("Unable to decode selected image")
            targetFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }
            bitmap.recycle()
        }
        Uri.fromFile(targetFile).toString()
    }.getOrNull()
}

private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

@Composable
private fun PlatformSection(
    listing: PlatformListing,
    enabled: Boolean,
    onListingChange: (PlatformListing) -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CardBorder),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(listing.platform.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            DateField("Date listed", listing.dateListed, enabled = enabled) { onListingChange(listing.copy(dateListed = it)) }
            OutlinedTextField(
                value = listing.price,
                onValueChange = { onListingChange(listing.copy(price = it)) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                label = { Text("Price") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Checkbox(
                    checked = listing.sold,
                    enabled = enabled,
                    onCheckedChange = { checked -> onListingChange(listing.copy(sold = checked)) }
                )
                Text("Sold", color = MutedInk)
            }
            DateField("Date sold", listing.dateSold, enabled = enabled) { onListingChange(listing.copy(dateSold = it)) }
            OutlinedTextField(
                value = listing.finalPrice,
                onValueChange = { onListingChange(listing.copy(finalPrice = it)) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                label = { Text("Final price") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}
