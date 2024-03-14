package com.leadwoods.p2p_environment.networking

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.EXTRA_NETWORK_INFO
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_STATE
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_DISABLED
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED
import com.leadwoods.p2p_environment.activities.MainActivity
import com.leadwoods.p2p_environment.support.Logger


class P2PConnectionsListener(
    private val manager: WifiP2pManager,
    private val channel: Channel,
    private val callingActivity: MainActivity,
) : BroadcastReceiver() {


    private lateinit var action: String


    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent) {
        Logger.d("called | reading intent")
        action = intent.action.toString()

        when (action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                Logger.d("State Changed")
                when (val state = intent.getIntExtra(EXTRA_WIFI_STATE, -1)){
                    WIFI_P2P_STATE_ENABLED -> {
                        callingActivity.p2pEnabled(true)
                    }
                    WIFI_P2P_STATE_DISABLED -> {
                        callingActivity.p2pEnabled(false)
                    }

                    else -> {
                        Logger.w("Unexpected wifi state: $state")
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Logger.d("Peers Changed")
                manager.requestPeers(channel) { peers: WifiP2pDeviceList? ->
                    if(peers == null)
                        return@requestPeers
                    Logger.d("peers found: ${peers.deviceList.size}")
                    callingActivity.onPeersAvailable(peers)
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                Logger.d("Connection Changed")

                val networkInfo: NetworkInfo = intent.getParcelableExtra(EXTRA_NETWORK_INFO) ?: return

                if(networkInfo.isConnected){
                    Logger.d("Connection Successful | Getting Connection Info")
                    manager.requestConnectionInfo(channel, callingActivity)
                } else {
                    callingActivity.clearPeerResults()
                    Logger.w("Disconnected")
                }


            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                Logger.d("Device Changed")
                // Respond to this device's wifi state changing
            }
        }

    }

}