package com.resell.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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

    val products: Flow<List<Product>> = context.dataStore.data.map { preferences ->
        val raw = preferences[productsKey].orEmpty()
        if (raw.isBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString<List<Product>>(raw) }.getOrDefault(emptyList()).map { it.normalized() }
        }
    }

    suspend fun saveProduct(product: Product) {
        context.dataStore.edit { preferences ->
            val current = preferences[productsKey]
                ?.let { runCatching { json.decodeFromString<List<Product>>(it) }.getOrNull() }
                .orEmpty()
                .map { it.normalized() }
                .toMutableList()

            val index = current.indexOfFirst { it.id == product.id }
            if (index >= 0) {
                current[index] = product.normalized()
            } else {
                current.add(product.normalized())
            }

            preferences[productsKey] = json.encodeToString(current)
        }
    }

    suspend fun deleteProduct(productId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[productsKey]
                ?.let { runCatching { json.decodeFromString<List<Product>>(it) }.getOrNull() }
                .orEmpty()
                .map { it.normalized() }
                .filterNot { it.id == productId }

            preferences[productsKey] = json.encodeToString(current)
        }
    }
}
