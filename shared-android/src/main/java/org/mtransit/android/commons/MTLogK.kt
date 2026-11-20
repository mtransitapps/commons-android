package org.mtransit.android.commons

@Suppress("TooManyFunctions")
object MTLogK {

    interface Loggable {
        fun getLogTag(): String
    }

    fun isLoggable(debug: Int) = MTLog.isLoggable(debug)

    fun v(loggable: Loggable, msg: String, t: Throwable? = null) = v(loggable.getLogTag(), msg, t)
    fun v(obj: Any, msg: String, t: Throwable? = null) = v(obj.javaClass, msg, t)
    fun v(clazz: Class<*>, msg: String, t: Throwable? = null) = v(clazz.simpleName, msg, t)
    fun v(tag: String, msg: String, t: Throwable? = null) {
        t?.let { MTLog.v(tag, t, msg) }
            ?: run { MTLog.v(tag, msg) }
    }

    fun d(loggable: Loggable, msg: String, t: Throwable? = null) = d(loggable.getLogTag(), msg, t)
    fun d(obj: Any, msg: String, t: Throwable? = null) = d(obj.javaClass, msg, t)
    fun d(clazz: Class<*>, msg: String, t: Throwable? = null) = d(clazz.simpleName, msg, t)
    fun d(tag: String, msg: String, t: Throwable? = null) {
        t?.let { MTLog.d(tag, t, msg) }
            ?: run { MTLog.d(tag, msg) }
    }

    fun i(loggable: Loggable, msg: String, t: Throwable? = null) = i(loggable.getLogTag(), msg, t)
    fun i(obj: Any, msg: String, t: Throwable? = null) = i(obj.javaClass, msg, t)
    fun i(clazz: Class<*>, msg: String, t: Throwable? = null) = i(clazz.simpleName, msg, t)
    fun i(tag: String, msg: String, t: Throwable? = null) {
        t?.let { MTLog.i(tag, t, msg) }
            ?: run { MTLog.i(tag, msg) }
    }

    fun w(loggable: Loggable, msg: String, t: Throwable? = null) = w(loggable.getLogTag(), msg, t)
    fun w(obj: Any, msg: String, t: Throwable? = null) = w(obj.javaClass, msg, t)
    fun w(clazz: Class<*>, msg: String, t: Throwable? = null) = w(clazz.simpleName, msg, t)
    fun w(tag: String, msg: String, t: Throwable? = null) {
        t?.let { MTLog.w(tag, t, msg) }
            ?: run { MTLog.w(tag, msg) }
    }

    fun e(loggable: Loggable, msg: String, t: Throwable? = null) = e(loggable.getLogTag(), msg, t)
    fun e(obj: Any, msg: String, t: Throwable? = null) = e(obj.javaClass, msg, t)
    fun e(clazz: Class<*>, msg: String, t: Throwable? = null) = e(clazz.simpleName, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) {
        t?.let { MTLog.e(tag, t, msg) }
            ?: run { MTLog.e(tag, msg) }
    }
}
