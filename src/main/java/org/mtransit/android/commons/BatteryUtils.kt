package org.mtransit.android.commons

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.getSystemService

object BatteryUtils : MTLog.Loggable {

     private val LOG_TAG: String = BatteryUtils::class.java.simpleName

    private const val BATTERY_LEVEL_MIN = 70f
    private const val BATTERY_LEVEL_MIN_CHARGING = 50f

    override fun getLogTag() = LOG_TAG

    @JvmStatic
    fun shouldUseBatteryForOptionalWork(context: Context): Boolean {
        try {
            val batteryStatusIntent = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { intentFilter ->
                context.registerReceiver(null, intentFilter)
            }
            val powerManager = context.getSystemService<PowerManager>()
            // 1st - check for bad states to avoid
            powerManager?.let {
                if (it.isPowerSaveMode) return false // power save mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (it.isLowPowerStandbyEnabled) return false // low power mode
                }
            }
            batteryStatusIntent?.let {
                val health = it.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                if (health != BatteryManager.BATTERY_HEALTH_GOOD) return false // battery health NOT good
            }
            // 2nd - check for good states to use battery
            batteryStatusIntent?.let {
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                if (status == BatteryManager.BATTERY_STATUS_FULL) return true // fully charged
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = (level * 100) / scale.toFloat()
                if (batteryPct > BATTERY_LEVEL_MIN) return true // 70+ %
                if (status == BatteryManager.BATTERY_STATUS_CHARGING && batteryPct > BATTERY_LEVEL_MIN_CHARGING) return true // charging w/ 50+ %
            }
            // ELSE -> bad state > save battery
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while checking battery status!")
        }
        return false
    }
}
