package com.leadwoods.p2p_environment.networking

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.leadwoods.p2p_environment.support.Logger
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.min
import kotlin.text.Charsets.UTF_8

// GM send data to all players
// GM receive data from a player
// Player send data to GM
// Player receive data from GM

// Group Owner Receive Data
// Group Owner Send Data


const val ACTION_SEND_JSON = "ACTION_SEND_JSON"

const val FILE_URL = "FILE_URL"

const val RAW_DATA = "RAW_DATA"

const val GO_ADDRESS = "GO_ADDRESS"

const val GO_PORT = "GO_PORT"

class DataTransferService: IntentService("DataTransferService") {

    override fun onHandleIntent(intent: Intent?) {
        Log.d("test", "Service Started")

        if (intent != null) {
            if (intent.action.equals(ACTION_SEND_JSON)) {
                val port = intent.getIntExtra(GO_PORT, -1)
                if (port == -1) {
                    Logger.e("get GO_PORT failed")
                    return
                }

                val host = intent.getStringExtra(GO_ADDRESS) ?: ""
                val stringData = intent.getStringExtra(RAW_DATA) ?: "Default Message"

                Logger.d("Sending starting: ${stringData.subSequence(0, min(20, stringData.length))}, total length: ${stringData.length} to $host @ $port")

                val socket = Socket()

                try {
                    Logger.d("Opening Client Socket")
                    socket.bind(null)
                    socket.connect(InetSocketAddress(host, port), 50000)

                    Logger.d("Client Socket - ${socket.isConnected}")

                    val oStream: OutputStream = socket.getOutputStream()

                    try {
                        val stringBytes = stringData.toByteArray(UTF_8)
                        oStream.write(stringBytes)
                        oStream.flush()
                        Logger.d("Client: Data Written")

                    } catch (e: Exception) {
                        Logger.e("Error writing data to oStream: ${e.message}")
                    } finally {
                        try {
                            oStream.close()
                        } catch (e: Exception) {
                            Logger.e("Error Closing oStream: ${e.message}")
                        }
                    }
                } catch (e: IOException) {
                    Logger.e("${e.message}")
                } finally {
                    if (socket.isConnected) {
                        try {
                            socket.close()
                        } catch (e: IOException) {
                            Logger.e("${e.printStackTrace()}")
                        }
                    }
                }
            }
        }
    }
}