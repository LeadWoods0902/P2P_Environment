package com.leadwoods.p2p_environment.support

import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity

const val CLIENT_TYPE = "CLIENT_TYPE"

const val INTERNAL_SERVICE_REMOVED = "SERVICE_REMOVED"
const val INTERNAL_PEERS_CHANGED = "PEERS_CHANGED"
const val INTERNAL_CONNECTED = "SERVICE_CONNECTED"
const val INTERNAL_DEVICE_CHANGED = "DEVICE_CHANGED"
const val INTERNAL_MESSAGE_RECEIVED = "MESSAGE_RECEIVED"
const val INTERNAL_WIFI_STATE_CHANGED = "WIFI_STATE_CHANGED"
const val INTERNAL_DISCONNECTED = "DISCONNECTED"
const val INTERNAL_PEERS = "PEERS"
const val INTERNAL_MESSAGE_KEY = "MESSAGE_KEY"
const val INTERNAL_ERROR = "ERROR"
const val INTERNAL_ERROR_MSG = "ERROR_MSG"
const val INTERNAL_HIDE_PEERS = "INTERNAL_HIDE_PEERS"

const val MESSAGE_READ = 0x400+1
const val ABC = 0x400+1
const val COMMUNICATION_DISCONNECTED = 0x400+1


enum class ClientType{
    HOST,
    PLAYER,
    UNASSIGNED
}

open class AppBase(): AppCompatActivity(){

    private lateinit var sharedPreferences: SharedPreferences

    fun setClientType(type: ClientType){
        Logger.f()
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
        Logger.f()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        return when(sharedPreferences.getInt(CLIENT_TYPE, 2)){
            0 -> ClientType.HOST
            1 -> ClientType.PLAYER
            else -> ClientType.UNASSIGNED
        }
    }
}