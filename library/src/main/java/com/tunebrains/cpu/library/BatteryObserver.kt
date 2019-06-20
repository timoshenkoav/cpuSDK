package com.tunebrains.cpu.library

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast


class BatteryObserver(ctx: Context) {

    val rxBroadcast = RxBroadcast.fromBroadcast(ctx, IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
    })
}