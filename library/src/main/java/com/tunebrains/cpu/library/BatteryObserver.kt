package com.tunebrains.cpu.library

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.cantrowitz.rxbroadcast.RxBroadcast


data class BatteryInfo(val percent: Float, val plugged: Int)
class BatteryObserver(ctx: Context) {

    val rxBroadcast = RxBroadcast.fromBroadcast(ctx, IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
    }).map {
        val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val battPct = level / scale.toFloat()
        val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        BatteryInfo(battPct, plugged)
    }.distinctUntilChanged { a, b ->
        val aVal = (a.percent * 100).toInt()
        val bVal = (b.percent * 100).toInt()
        a.plugged == b.plugged && (aVal < 50 && bVal < 50 || aVal > 50 && bVal > 50)
    }

}