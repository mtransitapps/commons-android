package org.mtransit.android.commons.task

import org.mtransit.android.commons.MTLog

@Deprecated(message = "Deprecated in Android SDK")
abstract class MTCancellableAsyncTask<Params, Progress, Result> :
    MTAsyncTask<Params, Progress, Result>(), MTLog.Loggable {

    override fun doInBackgroundMT(vararg params: Params?): Result? {
        if (isCancelled) {
            return null
        }
        return doInBackgroundNotCancelledMT(*params)
    }

    protected abstract fun doInBackgroundNotCancelledMT(vararg params: Params?): Result?

    override fun onProgressUpdate(vararg values: Progress?) {
        super.onProgressUpdate(*values)
        if (isCancelled) {
            return
        }
        onProgressUpdateNotCancelledMT(*values)
    }

    @Suppress("UNUSED_PARAMETER")
    protected open fun onProgressUpdateNotCancelledMT(
        vararg values: Progress?
    ) {
        // not mandatory
    }

    override fun onPostExecute(result: Result?) {
        super.onPostExecute(result)
        if (isCancelled) {
            return
        }
        onPostExecuteNotCancelledMT(result)
    }

    @Suppress("UNUSED_PARAMETER")
    protected open fun onPostExecuteNotCancelledMT(
        result: Result?
    ) {
        // not mandatory
    }
}