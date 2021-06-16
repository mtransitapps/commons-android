package org.mtransit.android.commons.data

import androidx.collection.LruCache

operator fun <K : Any, V : Any> LruCache<K, V>.set(key: K, value: V): V? {
    return this.put(key, value)
}