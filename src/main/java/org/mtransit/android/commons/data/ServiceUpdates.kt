package org.mtransit.android.commons.data

import android.annotation.SuppressLint
import java.util.function.IntFunction

class ServiceUpdates @JvmOverloads constructor(
    val list: MutableList<ServiceUpdate> = mutableListOf(),
) : MutableList<ServiceUpdate> by list {

    companion object {
        @JvmStatic
        fun newEmpty() = ServiceUpdates()
    }

    fun areUseful() = any { it.isUseful }

    fun isSeverityWarning(): Boolean = any { it.isSeverityWarning }

    @Suppress("unused") // main app only
    fun isSeverityWarningXorInfo(): Pair<Boolean, Boolean> {
        if (any { it.isSeverityWarning }) return true to false
        if (any { it.isSeverityInfo }) return false to true
        return false to false
    }

    @Suppress("unused") // main app only
    fun distinctByOriginalId(): ServiceUpdates = ServiceUpdates(this.distinctBy { it.originalId ?: it.id }.toMutableList())

    fun distinct(): ServiceUpdates = ServiceUpdates(this.toSet().toMutableList())

    fun filter(filter: (ServiceUpdate) -> Boolean) = ServiceUpdates(filterTo(mutableListOf(), filter))
    fun filterNot(filter: (ServiceUpdate) -> Boolean) = ServiceUpdates(filterNotTo(mutableListOf(), filter))

    fun map(transform: (ServiceUpdate) -> ServiceUpdate) = ServiceUpdates(mapTo(mutableListOf(), transform))

    fun sortWith(comparator: Comparator<ServiceUpdate>) = apply { this.list.sortWith(comparator) }

    @SuppressLint("DeprecatedCall")
    @Suppress("DEPRECATION", "PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
    @Deprecated("deprecated in Collection")
    override fun <T> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?> {
        return (this.list as java.util.Collection<T?>).toArray(generator)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServiceUpdates) return false

        if (list != other.list) return false

        return true
    }

    override fun hashCode() = list.hashCode()

}

inline fun buildServiceUpdates(builderAction: MutableCollection<ServiceUpdate>.() -> Unit): ServiceUpdates {
    return ServiceUpdates().apply(builderAction)
}

fun ServiceUpdates?.orNewEmpty(): ServiceUpdates = this ?: ServiceUpdates.newEmpty()
