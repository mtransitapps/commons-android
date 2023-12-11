@file:Suppress("unused")

package org.mtransit.android.commons

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observeNonNull(owner: LifecycleOwner, observer: (t: T) -> Unit) {
    this.observe(owner) {
        it?.let(observer)
    }
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, block: (T) -> Unit) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(value: T) {
            block(value)
            removeObserver(this)
        }
    })
}
