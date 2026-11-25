package org.mtransit.android.commons

import kotlin.reflect.KClass

@Suppress("TooManyFunctions")
object MTLogK {

    interface Loggable {
        fun getLogTag(): String
    }

    fun isLoggable(debug: Int) = MTLog.isLoggable(debug)

    fun v(loggable: Loggable, msg: String, t: Throwable? = null) = v(loggable.getLogTag(), msg, t)
    fun v(obj: Any, msg: String, t: Throwable? = null) = when (obj) {
        is MTLog.Loggable -> v(obj.logTag, msg, t)
        is Loggable -> v(obj, msg, t)
        else -> v(obj.javaClass, msg, t)
    }
    fun v(clazz: Class<*>, msg: String, t: Throwable? = null) = v(clazz.simpleName, msg, t)
    fun v(kClass: KClass<*>, msg: String, t: Throwable? = null) = v(kClass.java, msg, t)
    fun v(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) MTLog.v(tag, t, msg) else MTLog.v(tag, msg)
    }

    fun d(loggable: Loggable, msg: String, t: Throwable? = null) = d(loggable.getLogTag(), msg, t)
    fun d(obj: Any, msg: String, t: Throwable? = null) = when (obj) {
        is MTLog.Loggable -> d(obj.logTag, msg, t)
        is Loggable -> d(obj, msg, t)
        else -> d(obj.javaClass, msg, t)
    }
    fun d(clazz: Class<*>, msg: String, t: Throwable? = null) = d(clazz.simpleName, msg, t)
    fun d(kClass: KClass<*>, msg: String, t: Throwable? = null) = d(kClass.java, msg, t)
    fun d(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) MTLog.d(tag, t, msg) else MTLog.d(tag, msg)
    }

    fun i(loggable: Loggable, msg: String, t: Throwable? = null) = i(loggable.getLogTag(), msg, t)
    fun i(obj: Any, msg: String, t: Throwable? = null) = when (obj) {
        is MTLog.Loggable -> i(obj.logTag, msg, t)
        is Loggable -> i(obj, msg, t)
        else -> i(obj.javaClass, msg, t)
    }
    fun i(clazz: Class<*>, msg: String, t: Throwable? = null) = i(clazz.simpleName, msg, t)
    fun i(kClass: KClass<*>, msg: String, t: Throwable? = null) = i(kClass.java, msg, t)
    fun i(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) MTLog.i(tag, t, msg) else MTLog.i(tag, msg)
    }

    fun w(loggable: Loggable, msg: String, t: Throwable? = null) = w(loggable.getLogTag(), msg, t)
    fun w(obj: Any, msg: String, t: Throwable? = null) = when (obj) {
        is MTLog.Loggable -> w(obj.logTag, msg, t)
        is Loggable -> w(obj, msg, t)
        else -> w(obj.javaClass, msg, t)
    }
    fun w(clazz: Class<*>, msg: String, t: Throwable? = null) = w(clazz.simpleName, msg, t)
    fun w(kClass: KClass<*>, msg: String, t: Throwable? = null) = w(kClass.java, msg, t)
    fun w(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) MTLog.w(tag, t, msg) else MTLog.w(tag, msg)
    }

    fun e(loggable: Loggable, msg: String, t: Throwable? = null) = e(loggable.getLogTag(), msg, t)
    fun e(obj: Any, msg: String, t: Throwable? = null) = when (obj) {
        is MTLog.Loggable -> e(obj.logTag, msg, t)
        is Loggable -> e(obj, msg, t)
        else -> e(obj.javaClass, msg, t)
    }
    fun e(clazz: Class<*>, msg: String, t: Throwable? = null) = e(clazz.simpleName, msg, t)
    fun e(kClass: KClass<*>, msg: String, t: Throwable? = null) = e(kClass.java, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) MTLog.e(tag, t, msg) else MTLog.e(tag, msg)
    }
}
