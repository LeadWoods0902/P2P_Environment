package com.leadwoods.p2p_environment.wifip2p.p2p_handler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Build
import android.widget.Toast
import com.leadwoods.p2p_environment.support.INTERNAL_CONNECTED
import com.leadwoods.p2p_environment.support.INTERNAL_DEVICE_CHANGED
import com.leadwoods.p2p_environment.support.INTERNAL_DISCONNECTED
import com.leadwoods.p2p_environment.support.INTERNAL_ERROR
import com.leadwoods.p2p_environment.support.INTERNAL_ERROR_MSG
import com.leadwoods.p2p_environment.support.INTERNAL_MESSAGE_RECEIVED
import com.leadwoods.p2p_environment.support.INTERNAL_PEERS
import com.leadwoods.p2p_environment.support.INTERNAL_PEERS_CHANGED
import com.leadwoods.p2p_environment.support.INTERNAL_WIFI_STATE_CHANGED
import com.leadwoods.p2p_environment.support.Logger

class P2PHandlerBroadcastReceiver(private val callback: ReceiverInterface): BroadcastReceiver() {
    interface ReceiverInterface {
        fun dataReceived()
        fun peersReceived(peers: WifiP2pDeviceList)
        fun peerConnected()
    }

    override fun onReceive(context: Context?, intent: Intent) {
        Logger.f()

        when(intent.action){
            INTERNAL_DISCONNECTED -> {
                Logger.i("Disconnected from Group")
            }

            INTERNAL_CONNECTED -> {
                Logger.i("Connected to Group")
                callback.peerConnected()
            }

            INTERNAL_DEVICE_CHANGED -> {
                Logger.i("This Device Changed")
            }

            INTERNAL_MESSAGE_RECEIVED -> {
                Logger.i("Message Received")
                Toast.makeText(context, "Message Received", Toast.LENGTH_SHORT).show()
            }

            INTERNAL_WIFI_STATE_CHANGED -> {
                Logger.i("Wi-Fi state changed")
            }

            INTERNAL_PEERS_CHANGED -> {
                Logger.i("Peers changed")
                val peers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(INTERNAL_PEERS, WifiP2pDeviceList::class.java)
                } else {
                    intent.getParcelableExtra(INTERNAL_PEERS)
                }

                if (peers == null) {
                    Logger.e("Error retrieving peers from broadcast")
                    return
                }

                callback.peersReceived(peers)
            }

            INTERNAL_ERROR -> {
                Logger.e(intent.getStringExtra(INTERNAL_ERROR_MSG)?: "Unable to retrieve error message")
                Toast.makeText(context, intent.getStringExtra(INTERNAL_ERROR_MSG)?: "Unable to retrieve error message", Toast.LENGTH_LONG).show()
            }

            else -> {
                Logger.w("Unexpected state")
            }
        }
    }

}
