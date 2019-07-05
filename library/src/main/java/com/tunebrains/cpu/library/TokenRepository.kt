package com.tunebrains.cpu.library

import android.content.Context
import android.preference.PreferenceManager
import io.reactivex.subjects.BehaviorSubject
import java.util.*


data class TokenInfo(val token: String, val fcmToken: String)
class TokenRepository(val ctx: Context) {
    companion object {
        const val PREF_IP = "cpu:sdk:prefs:ip"
        const val TOKEN_PREF = "cpu:sdk:prefs:token"
        const val FCM_TOKEN_PREF = "cpu:sdk:prefs:fcm_token"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
    val events = BehaviorSubject.create<TokenInfo>()
    val ipEvents = BehaviorSubject.create<String>()

    init {
        val token = generateToken()
        events.onNext(TokenInfo(token, prefs.getString(FCM_TOKEN_PREF, "") ?: ""))
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

    fun id(): String {
        return "random id"
    }

    fun ip(): String {
        return prefs.getString(PREF_IP, "") ?: ""
    }

    fun saveIp(ip: String) {
        prefs.edit().putString(PREF_IP, ip).apply()
        ipEvents.onNext(ip)
    }

    fun app(): String {
        return ctx.packageName
    }
}