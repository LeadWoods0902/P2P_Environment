package com.leadwoods.p2p_environment.networking

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pConfig.GROUP_OWNER_INTENT_MIN
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Build
import com.leadwoods.p2p_environment.WifiP2pPeer
import com.leadwoods.p2p_environment.activities.MainActivity
import com.leadwoods.p2p_environment.support.Logger
import java.net.InetAddress

/**
 *
 */
open class P2pBaseHandler protected constructor(){

    interface p2pHandlerInterface{
        fun connected(success: Boolean)
        fun disconnected(success: Boolean)
    }

    // network info
    protected lateinit var hostInfo: WifiP2pInfo
    protected lateinit var groupPeers: ArrayList<WifiP2pPeer>
    protected var peerCount: Int = 0
    protected var isHost: Boolean = false

    protected var inGroup: Boolean = false


    // p2p flags
    protected var keepDownloading: Boolean = true
    protected var showAllPeers: Boolean = true

    // p2p networking elements
    protected lateinit var p2pManager: WifiP2pManager
    protected lateinit var p2pChannel: WifiP2pManager.Channel
    protected lateinit var p2pReceiver: BroadcastReceiver
    protected val p2pIntentFilter = IntentFilter()

    fun initP2p(context: Context): Boolean {
        Logger.d("called | initialising P2P")
        // Compatibility check
        if(!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Logger.e("Wi-Fi Direct is not supported by this device")
            return false
        }

        // Hardware check
        if(context.getSystemService(Context.WIFI_SERVICE) == null){
            Logger.e("Cannot get Wi-Fi system service.")
            return false
        }

        // Availability check
        if (!(context.getSystemService(Context.WIFI_SERVICE) as WifiManager).isP2pSupported) {
            Logger.e("Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.")
            return false
        }

        // Create
        p2pManager = (context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager)
        if(p2pManager == null){
            Logger.e("Cannot get Wi-Fi Direct system service.")
            return false
        }

        p2pChannel = p2pManager!!.initialize(context, context.mainLooper, null)
        if(p2pChannel == null){
            Logger.e("Cannot initialize Wi-Fi Direct.")
            return false
        }

        p2pIntentFilter.apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        Logger.d("called | initialised P2P Successfully")
        return true
    }
    @SuppressLint("MissingPermission")
    /**
     * Attempt to connect to the given peer
     * @param peer The device to attempt a connection with
     */
    fun connect(peer: WifiP2pPeer){
        Logger.w("called | Attempting Connection to: ${peer.device.deviceName}")

        val connectionConfig = WifiP2pConfig().apply {
            deviceAddress = peer.device.deviceAddress
//            groupOwnerIntent = if(isGameMaster) GROUP_OWNER_INTENT_MAX else GROUP_OWNER_INTENT_MIN
            groupOwnerIntent = GROUP_OWNER_INTENT_MIN
        }

        p2pManager.connect(p2pChannel, connectionConfig, object : ActionListener {
            override fun onSuccess() {
                Logger.i("Connected Request Sent Successfully")
            }

            override fun onFailure(reason: Int) {
                Logger.w(
                    "Connect Request Failed: " +
                            when(reason){
                                WifiP2pManager.P2P_UNSUPPORTED -> "P2P is Unsupported"
                                WifiP2pManager.BUSY -> "BUSY"
                                WifiP2pManager.NO_SERVICE_REQUESTS -> "NO_SERVICE_REQUESTS"
                                else -> "Unexpected Error"
                            }
                )
            }

        })
    }


    /**
     * Disconnect from the current group
     */
    fun disconnect() {
        Logger.d("called")

        p2pManager.removeGroup(p2pChannel, object : ActionListener {
            override fun onSuccess() {
                Logger.d("disconnected from group")
            }

            override fun onFailure(reason: Int) {
                Logger.w(
                    "Connect Request Failed: " +
                            when(reason){
                                WifiP2pManager.P2P_UNSUPPORTED -> "P2P is Unsupported"
                                WifiP2pManager.BUSY -> "BUSY"
                                WifiP2pManager.NO_SERVICE_REQUESTS -> "NO_SERVICE_REQUESTS"
                                else -> "Unexpected Error"
                            }
                )
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun createGroup() {
        Logger.d("called | Creating a new empty group")
        p2pChannel?.also { channel ->
            p2pManager?.createGroup(channel, object: ActionListener{
                override fun onSuccess() {
                    Logger.d("Group Creation successful")
                }

                override fun onFailure(reason: Int) {
                    Logger.e("Failed to create group: $reason")
                }
            })
        }
    }

    /**
     *
     */
    @SuppressLint("MissingPermission")
    fun scanForNearbyPeers() {
        Logger.d("called")

        p2pManager?.discoverPeers(p2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Logger.i("Discovery Launched Successfully")
            }

            override fun onFailure(reason: Int) {
                Logger.w(
                    "Error Launching Discovery: " +
                            when(reason){
                                WifiP2pManager.P2P_UNSUPPORTED -> "P2P is Unsupported"
                                WifiP2pManager.BUSY -> "BUSY"
                                WifiP2pManager.NO_SERVICE_REQUESTS -> "NO_SERVICE_REQUESTS"
                                else -> "Unexpected Error"
                            }
                )
            }
        })
    }



    fun transmitData(context: Context, data: String? = null, address: String? = null) {
        if(data.isNullOrEmpty()) {
            Logger.w("Cannot transfer null data")
            return
        }

        if(address.isNullOrEmpty()) {
            Logger.w("Cannot transfer data to unspecified address")
            return
        }

        try {
            context.startService(Intent(context, DataTransferService::class.java).apply {
                action = ACTION_SEND_JSON
                putExtra(RAW_DATA, data)
                putExtra(GO_ADDRESS, address)
                putExtra(GO_PORT, 8988)
            })
        } catch (e: Exception){
            Logger.e("Error starting Data Transfer Service: ${e.message}")
        }
    }

    fun transmitDataToAll(context: Context, data: String? = null) {
        if(peerCount == 0){
            Logger.w("Cannot transfer data to empty group")
            return
        }

        if(data.isNullOrEmpty()) {
            Logger.w("Cannot transfer null data")
            return
        }

        for(peer in groupPeers) {
            try {
                context.startService(Intent(context, DataTransferService::class.java).apply {
                    action = ACTION_SEND_JSON
                    putExtra(RAW_DATA, data)
                    putExtra(GO_ADDRESS, peer.device.deviceAddress)
                    putExtra(GO_PORT, 8988)
                })
            } catch (e: Exception) {
                Logger.e("Error starting Data Transfer Service: ${e.message}")
            }
        }
    }

    // Getters & Setters

    /**
     * @return string of the current host address
     */
    fun getHostAddress(): String? {
        return hostInfo.groupOwnerAddress.hostAddress
    }

    fun setHostAddress(groupOwnerAddress: InetAddress?, isOwner: Boolean) {
        if(groupOwnerAddress == null)
            Logger.w("Cannot Save a Null Host Address")

        hostInfo.groupOwnerAddress = groupOwnerAddress
        isHost = isOwner
    }

    /**
     * @return group status: In a group (true), Not in a group (false)
     */
    fun getGroupStatus(): Boolean {
        return inGroup
    }

    /**
     * Register the broadcast receiver to the given context
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun register(context: Context):Boolean {
        Logger.d("called")
        return if(::p2pReceiver.isInitialized) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(p2pReceiver, p2pIntentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else
                context.registerReceiver(p2pReceiver, p2pIntentFilter)
            Logger.i("p2pReceiver registered")
            true
        } else {
            Logger.w("Cannot register p2pReceiver at this time")
            false
        }
    }

    /**
     * Unregister the broadcast receiver from the given context
     */
    fun unregister(context: Context):Boolean {
        Logger.d("called")
        return if(::p2pReceiver.isInitialized) {
            context.unregisterReceiver(p2pReceiver)
            Logger.i("p2pReceiver unregistered")
            true
        } else{
            Logger.w("Cannot unregister p2pReceiver at this time")
            false
        }
    }

    fun getPeerVisibility(): Boolean {
        return showAllPeers
    }

    fun setPeersVisibility(newFlag: Boolean) {
        showAllPeers = newFlag
    }

    fun startSocketListening(activity: MainActivity) {
        DataListener(activity).execute()
    }

    fun resumeSocketListening(): Boolean {
        return keepDownloading
    }

    companion object {
        private var instance: P2pBaseHandler? = null

        /**
         * @return instance of [P2pBaseHandler], creating a new one if one doesn't exist.
         */
        fun getInstance(): P2pBaseHandler {
            return instance ?: P2pBaseHandler()
        }
    }


}