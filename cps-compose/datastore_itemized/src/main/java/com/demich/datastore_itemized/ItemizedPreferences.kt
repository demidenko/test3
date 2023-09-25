package com.demich.datastore_itemized

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ItemizedPreferences(private val preferences: Preferences) {
    operator fun<T> get(item: DataStoreItem<T>): T =
        item.converter.getFrom(preferences)
}

class ItemizedMutablePreferences(private val preferences: MutablePreferences) {
    operator fun<T> get(item: DataStoreItem<T>): T =
        item.converter.getFrom(preferences)

    operator fun<T> set(item: DataStoreItem<T>, value: T) =
        item.converter.setTo(preferences, value)

    inline fun<K, V> edit(item: DataStoreItem<Map<K, V>>, block: MutableMap<K, V>.() -> Unit) {
        set(item, value = get(item).toMutableMap().apply(block))
    }

    fun clear() {
        preferences.clear()
    }

    fun<T> remove(item: DataStoreItem<T>) {
        item.converter.removeFrom(preferences)
    }
}

fun<D: ItemizedDataStore, R> D.flowBy(transform: D.(ItemizedPreferences) -> R): Flow<R> =
    dataStore.data.map { transform(ItemizedPreferences(it)) }

suspend fun<D: ItemizedDataStore, R> D.withSnapShot(block: D.(ItemizedPreferences) -> R): R =
    dataStore.data.first().let { block(ItemizedPreferences(it)) }

suspend fun<D: ItemizedDataStore> D.edit(block: D.(ItemizedMutablePreferences) -> Unit) {
    dataStore.edit { block(ItemizedMutablePreferences(it)) }
}