package com.tunebrains.cpu.sample

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tunebrains.cpu.library.CPUSdk
import timber.log.Timber


class FCMService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)
        Timber.d("Message received")
        p0?.let {
            val sdkData = it.data["cpu_sdk"]
            if (!sdkData.isNullOrBlank()) {
                CPUSdk.fcmNewData(this, sdkData)
            }
            Timber.d("sdk data: $sdkData")
        }
    }

    override fun onNewToken(p0: String?) {
        super.onNewToken(p0)
        Timber.d("On new token $p0")
    }
}