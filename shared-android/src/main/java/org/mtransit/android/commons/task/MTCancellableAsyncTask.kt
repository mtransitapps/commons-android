package org.mtransit.android.commons.task

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import org.mtransit.android.commons.MTLog

@Suppress("DEPRECATION")
@Deprecated(message = "Deprecated in Android SDK")
abstract class MTCancellableAsyncTask<Params, Progress, Result> :
    MTAsyncTask<Params, Progress, Result>(), MTLog.Loggable {

    @WorkerThread
    override fun doInBackgroundMT(vararg params: Params?): Result? {
        if (isCancelledMT) return null
        return doInBackgroundNotCancelledMT(*params)
    }

    open val isCancelledMT: Boolean get() = isCancelled

    @WorkerThread
    protected abstract fun doInBackgroundNotCancelledMT(vararg params: Params?): Result?

    @Deprecated("Deprecated in Java")
    @MainThread
    override fun onProgressUpdate(vararg values: Progress?) {
        super.onProgressUpdate(*values)
        if (isCancelledMT) return
        onProgressUpdateNotCancelledMT(*values)
    }

    @Suppress("UNUSED_PARAMETER")
    @MainThread
    protected open fun onProgressUpdateNotCancelledMT(
        vararg values: Progress?
    ) {
        // not mandatory
    }

    @Deprecated("Deprecated in Java")
    @MainThread
    override fun onPostExecute(result: Result?) {
        super.onPostExecute(result)
        if (isCancelledMT) return
        onPostExecuteNotCancelledMT(result)
    }

    @Suppress("UNUSED_PARAMETER")
    @MainThread
    protected open fun onPostExecuteNotCancelledMT(
        result: Result?
    ) {
        // not mandatory
    }
}
