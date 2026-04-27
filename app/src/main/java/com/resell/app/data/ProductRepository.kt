package com.resell.app.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "resell_products")

class ProductRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private val productsKey = stringPreferencesKey("products")
    private val autoBackupEnabledKey = booleanPreferencesKey("auto_backup_enabled")
    private val driveFolderUriKey = stringPreferencesKey("drive_folder_uri")

    val products: Flow<List<Product>> = context.dataStore.data.map { preferences ->
        val raw = preferences[productsKey].orEmpty()
        if (raw.isBlank()) {
            emptyList()
        } else {
            decodeProducts(raw).map(::normalizeStoredProduct)
        }
    }

    val backupPreferences: Flow<BackupPreferences> = context.dataStore.data.map { preferences ->
        BackupPreferences(
            autoBackupEnabled = preferences[autoBackupEnabledKey] ?: false,
            driveFolderUri = preferences[driveFolderUriKey].orEmpty()
        )
    }

    suspend fun saveProduct(product: Product) {
        context.dataStore.edit { preferences ->
            val current = preferences[productsKey]
                ?.let(::decodeProductsOrNull)
                .orEmpty()
                .map(::normalizeStoredProduct)
                .toMutableList()

            val index = current.indexOfFirst { it.id == product.id }
            if (index >= 0) {
                val existing = current[index]
                current[index] = product
                    .copy(createdAt = product.createdAt.ifBlank { existing.createdAt })
                    .normalized()
            } else {
                current.add(
                    product.copy(createdAt = product.createdAt.ifBlank { formatDateForDisplay(LocalDate.now()) })
                        .normalized()
                )
            }

            preferences[productsKey] = json.encodeToString(current)
        }
    }

    suspend fun deleteProduct(productId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[productsKey]
                ?.let(::decodeProductsOrNull)
                .orEmpty()
                .map(::normalizeStoredProduct)
                .filterNot { it.id == productId }

            preferences[productsKey] = json.encodeToString(current)
        }
    }

    suspend fun migrateLegacyProducts() {
        context.dataStore.edit { preferences ->
            val raw = preferences[productsKey].orEmpty()
            if (raw.isBlank()) return@edit

            val decoded = decodeProductsOrNull(raw) ?: return@edit
            val normalized = decoded.map { product ->
                val migratedCreatedAt = product.createdAt.ifBlank {
                    inferImageDateFromImageUri(product.imageUri) ?: formatDateForDisplay(LocalDate.now())
                }
                product.copy(createdAt = migratedCreatedAt).normalized()
            }
            if (normalized != decoded) {
                preferences[productsKey] = json.encodeToString(normalized)
            }
        }
    }

    suspend fun exportBackup(targetUri: Uri): Boolean {
        val payload = BackupPayload(
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            products = products.first().map { product ->
                BackupProduct(
                    id = product.id,
                    createdAt = product.createdAt,
                    description = product.description,
                    purchasePrice = product.purchasePrice,
                    storageLocation = product.storageLocation,
                    expenses = product.expenses,
                    deleted = product.deleted,
                    platforms = product.platforms.map { it.normalized() },
                    image = product.imageUri.takeIf { it.isNotBlank() }?.let { imageUri ->
                        imageUriToBackupImage(imageUri)
                    }
                )
            }
        )

        return runCatching {
            context.contentResolver.openOutputStream(targetUri)?.bufferedWriter()?.use { writer ->
                writer.write(json.encodeToString(payload))
            } ?: error("Unable to open backup destination")
        }.isSuccess
    }

    suspend fun restoreBackup(sourceUri: Uri): Boolean {
        return runCatching {
            val payload = context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { reader ->
                json.decodeFromString<BackupPayload>(reader.readText())
            } ?: error("Unable to open backup source")

            val restoredProducts = payload.products.map { backup ->
                Product(
                    id = backup.id,
                    createdAt = backup.createdAt,
                    description = backup.description,
                    purchasePrice = backup.purchasePrice,
                    storageLocation = backup.storageLocation,
                    expenses = backup.expenses,
                    deleted = backup.deleted,
                    imageUri = backup.image?.let { restoreBackupImage(backup.id, it) }.orEmpty(),
                    platforms = backup.platforms.map { it.normalized() }
                ).normalized()
            }

            context.dataStore.edit { preferences ->
                preferences[productsKey] = json.encodeToString(restoredProducts)
            }
        }.isSuccess
    }

    suspend fun updateAutoBackup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[autoBackupEnabledKey] = enabled
        }
        syncScheduledDriveBackup()
    }

    suspend fun setDriveFolderUri(uri: Uri) {
        context.dataStore.edit { preferences ->
            preferences[driveFolderUriKey] = uri.toString()
        }
        syncScheduledDriveBackup()
    }

    suspend fun clearDriveFolderUri() {
        context.dataStore.edit { preferences ->
            preferences[driveFolderUriKey] = ""
        }
        syncScheduledDriveBackup()
    }

    suspend fun exportBackupToDriveFolder(): Boolean {
        val prefs = backupPreferences.first()
        val folderUri = prefs.driveFolderUri.takeIf { it.isNotBlank() } ?: return false
        val treeUri = Uri.parse(folderUri)
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        if (!root.canWrite()) return false

        val fileName = "resell-backup-${java.time.LocalDate.now()}.json"
        val document = root.findFile(fileName) ?: root.createFile("application/json", fileName.removeSuffix(".json"))
        val targetUri = document?.uri ?: return false
        return exportBackup(targetUri)
    }

    private suspend fun syncScheduledDriveBackup() {
        val prefs = backupPreferences.first()
        val workManager = WorkManager.getInstance(context)
        if (prefs.autoBackupEnabled && prefs.driveFolderUri.isNotBlank()) {
            val request = PeriodicWorkRequestBuilder<GoogleDriveBackupWorker>(24, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(
                GOOGLE_DRIVE_BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(GOOGLE_DRIVE_BACKUP_WORK_NAME)
        }
    }

    private fun imageUriToBackupImage(imageUri: String): BackupImage? {
        val file = runCatching { File(Uri.parse(imageUri).path.orEmpty()) }.getOrNull()
            ?.takeIf { it.exists() && it.isFile }
            ?: return null
        val bytes = file.readBytes()
        return BackupImage(
            fileName = file.name,
            timestampMillis = inferImageTimestampFromImageUri(imageUri),
            base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
        )
    }

    private fun restoreBackupImage(productId: String, backupImage: BackupImage): String {
        val bytes = Base64.decode(backupImage.base64Data, Base64.DEFAULT)
        val extension = backupImage.fileName.substringAfterLast('.', "jpg")
        val imagesDir = File(context.filesDir, "product-images").apply { mkdirs() }
        val timestamp = backupImage.timestampMillis
            ?: inferTimestampFromFileName(backupImage.fileName)
            ?: System.currentTimeMillis()
        val targetFile = File(imagesDir, "${productId}_${timestamp}.$extension")
        targetFile.writeBytes(bytes)
        return Uri.fromFile(targetFile).toString()
    }

    private fun decodeProducts(raw: String): List<Product> =
        decodeProductsOrNull(raw).orEmpty()

    private fun decodeProductsOrNull(raw: String): List<Product>? =
        runCatching { json.decodeFromString<List<Product>>(raw) }.getOrNull()

    private fun normalizeStoredProduct(product: Product): Product =
        product.normalized()

    private fun inferTimestampFromFileName(fileName: String): Long? =
        fileName.substringAfterLast('_', "").substringBefore('.').toLongOrNull()

    companion object {
        const val GOOGLE_DRIVE_BACKUP_WORK_NAME = "google_drive_backup"
    }
}
