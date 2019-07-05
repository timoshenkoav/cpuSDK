package com.tunebrains.cpu.library

import android.content.Context
import android.preference.PreferenceManager
import io.reactivex.subjects.PublishSubject
import java.util.*


data class TokenInfo(val token: String, val fcmToken: String)
class TokenRepository(ctx: Context) {
    companion object {
        const val TOKEN_PREF = "cpu:sdk:prefs:token"
        const val FCM_TOKEN_PREF = "cpu:sdk:prefs:fcm_token"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
    val events = PublishSubject.create<TokenInfo>()

    init {
        generateToken()
    }

    fun token(): String {
        var token = prefs.getString(TOKEN_PREF, "") ?: ""
        if (token.isEmpty()) {
            token = generateToken()
        }
        return token
    }

    private fun generateToken(): String {
        var token = prefs.getString(TOKEN_PREF, "") ?: ""

        if (token.isEmpty()) {
            token = UUID.randomUUID().toString()
            prefs.edit().putString(TOKEN_PREF, token).apply()
        }
        return token
    }

    fun fcmToken(): String {
        return prefs.getString(FCM_TOKEN_PREF, "") ?: ""
    }

    fun saveFcmToken(fcmToken: String) {
        prefs.edit().putString(FCM_TOKEN_PREF, fcmToken).apply()
        events.onNext(TokenInfo(token(), fcmToken))
    }
}