package org.mtransit.android.commons.data

import android.annotation.SuppressLint
import java.util.function.IntFunction

data class ServiceUpdates @JvmOverloads constructor(
    val list: MutableList<ServiceUpdate> = mutableListOf(),
) : MutableList<ServiceUpdate> by list {

    companion object {
        @JvmField
        val EMPTY = ServiceUpdates()

        @JvmStatic
        fun from(serviceUpdates: Iterable<ServiceUpdate>) = serviceUpdates.toMutableList().toServiceUpdates()
    }

    fun areUseful() = any { it.isUseful }

    fun isSeverityWarning(): Boolean = any { it.isSeverityWarning }

    @Suppress("unused") // main app only
    fun isSeverityWarningXorInfo(): Pair<Boolean, Boolean> {
        if (any { it.isSeverityWarning }) return true to false
        if (any { it.isSeverityInfo }) return false to true
        return false to false
    }

    fun distinct() = this.list.toSet().toServiceUpdates()

    fun filter(filter: (ServiceUpdate) -> Boolean): ServiceUpdates = filterTo(mutableListOf(), filter).toServiceUpdates()

    @SuppressLint("DeprecatedCall")
    @Suppress("DEPRECATION")
    @Deprecated("deprecated in MutableList")
    override fun <T> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?> {
        return super.toArray(generator)
    }
}

fun ServiceUpdates?.orEmpty(): ServiceUpdates = this?.list.orEmpty().toServiceUpdates()
@Suppress("unused") // main app only
fun ServiceUpdates.distinctByOriginalId(): ServiceUpdates = this.distinctBy { it.originalId ?: it.id }.toServiceUpdates() // keep 1st occurrence from sorted list (in *Manager)

fun MutableList<ServiceUpdate>.toServiceUpdates(): ServiceUpdates = ServiceUpdates(this)
fun Collection<ServiceUpdate>.toServiceUpdates(): ServiceUpdates = ServiceUpdates.from(this)
