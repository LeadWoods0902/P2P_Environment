package com.leadwoods.p2p_environment.support

import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity

const val CLIENT_TYPE = "CLIENT_TYPE"

enum class ClientType{
    HOST,
    PLAYER,
    UNASSIGNED
}

open class AppBase(): AppCompatActivity(){

    private lateinit var sharedPreferences: SharedPreferences

    fun setClientType(type: ClientType){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        sharedPreferences.edit().apply{
            putInt(
                CLIENT_TYPE, when(type){
                ClientType.HOST -> 0
                ClientType.PLAYER -> 1
                ClientType.UNASSIGNED -> 2
            })
        }.apply()
    }

    fun getClientType(): ClientType {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        return when(sharedPreferences.getInt(CLIENT_TYPE, 2)){
            0 -> ClientType.HOST
            1 -> ClientType.PLAYER
            else -> ClientType.UNASSIGNED
        }
    }
}