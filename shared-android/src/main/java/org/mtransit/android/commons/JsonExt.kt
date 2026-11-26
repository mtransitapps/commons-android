package org.mtransit.android.commons

inline fun <T> Any.jsonRunCatching(
    loggable: Any = this,
    block: () -> T,
): Result<T> = runCatching(
    block = block
).onFailure { t ->
    MTLogK.w(loggable, "Error while [de]serializing JSON '$this'!", t)
}
