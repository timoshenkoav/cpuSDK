package com.tunebrains.cpu.library

import android.content.Context
import android.preference.PreferenceManager
import java.util.*


class TokenRepository(val ctx: Context) {
    companion object {
        const val TOKEN_PREF = "cpu:sdk:prefs:token"
    }

    val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
    fun token(): String {
        var token = prefs.getString(TOKEN_PREF, "") ?: ""
        if (token.isEmpty()) {
            token = UUID.randomUUID().toString()
            prefs.edit().putString(TOKEN_PREF, token).apply()
        }
        return token
    }
}