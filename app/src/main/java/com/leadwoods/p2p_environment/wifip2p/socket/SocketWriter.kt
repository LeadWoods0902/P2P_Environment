package com.leadwoods.p2p_environment.wifip2p.socket

import android.app.IntentService
import android.content.Intent
import com.leadwoods.p2p_environment.networking.ACTION_SEND_JSON
import com.leadwoods.p2p_environment.support.Logger
import com.leadwoods.p2p_environment.wifip2p.p2p_handler.WifiP2PHandler.Companion.GROUP_PORT
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.min

const val RECIPIENT_ADDRESS = "RECIPIENT_ADDRESS"
const val JSON_DATA = "JSON_DATA"

class SocketWriter: IntentService("SockerWriter") {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        Logger.f()

        if(intent == null) {
            return
        }

        when(intent.action) {
            ACTION_SEND_JSON -> {
                sendJson(intent)
            }
            else -> {
                Logger.w("Unexpected action passed to socket writer")
            }
        }
    }

    fun sendJson(intent: Intent) {
        val recipientAddress = intent.getStringExtra(RECIPIENT_ADDRESS)
        val jsonData = intent.getStringExtra(JSON_DATA)

        if(recipientAddress.isNullOrBlank()){
            Logger.w("No address specified")
            return
        }

        if(jsonData.isNullOrBlank()){
            Logger.w("No data passed")
            return
        }

        Logger.i("Sending starting: ${jsonData.subSequence(0, min(20, jsonData.length))}, total length: ${jsonData.length} to $recipientAddress")

        val socket = Socket()

        try{
            socket.bind(null)
            socket.connect(InetSocketAddress(recipientAddress, GROUP_PORT), 5000)

            Logger.i("Client Socket: ${socket.isConnected}")

            if(!socket.isConnected)
                return

            val oStream = socket.getOutputStream()

            try{
                val jsonBytes = jsonData.toByteArray()
                oStream.write(jsonBytes)
                oStream.flush()
                Logger.i("Client Socket: Data Written")
            } catch (e: Exception) {
                Logger.e("Client Socket: Error writing data to oStream: ${e.message}")
            } finally {
                try {
                    oStream.close()
                } catch (e: Exception) {
                    Logger.e("Client Socket: Error Closing oStream: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Logger.e("Client Socket: Error writing to socket - ${e.message}")
        } finally {
            if (socket.isConnected) {
                try {
                    socket.close()
                } catch (e: Exception) {
                    Logger.e("${e.printStackTrace()}")
                }
            }
        }
    }
}