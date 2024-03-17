package com.leadwoods.p2p_environment.wifip2p.p2p_handler

//import com.leadwoods.p2p_environment.wifip2p.P2PIntentService

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
import android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION
import android.net.wifi.WifiManager.WIFI_STATE_DISABLED
import android.net.wifi.WifiManager.WIFI_STATE_ENABLED
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pConfig.GROUP_OWNER_INTENT_MIN
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.EXTRA_NETWORK_INFO
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_DISABLED
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.leadwoods.p2p_environment.WifiP2pPeer
import com.leadwoods.p2p_environment.deviceListToListOfPeers
import com.leadwoods.p2p_environment.networking.ACTION_SEND_JSON
import com.leadwoods.p2p_environment.support.INTERNAL_CONNECTED
import com.leadwoods.p2p_environment.support.INTERNAL_DEVICE_CHANGED
import com.leadwoods.p2p_environment.support.INTERNAL_DISCONNECTED
import com.leadwoods.p2p_environment.support.INTERNAL_ERROR
import com.leadwoods.p2p_environment.support.INTERNAL_ERROR_MSG
import com.leadwoods.p2p_environment.support.INTERNAL_PEERS
import com.leadwoods.p2p_environment.support.INTERNAL_PEERS_CHANGED
import com.leadwoods.p2p_environment.support.Logger
import com.leadwoods.p2p_environment.wifip2p.socket.JSON_DATA
import com.leadwoods.p2p_environment.wifip2p.socket.RECIPIENT_ADDRESS
import com.leadwoods.p2p_environment.wifip2p.socket.SocketListener
import com.leadwoods.p2p_environment.wifip2p.socket.SocketWriter
import kotlin.math.min


class WifiP2PHandler: Service(), ConnectionInfoListener, Handler.Callback {


    // For Binding
    private val binder = P2PServiceBinder()

    inner class P2PServiceBinder: Binder() {
        fun getService(): WifiP2PHandler {
            return this@WifiP2PHandler
        }

    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private var scanResults: List<ScanResult>? = null
//    private var socketWriter: SocketReaderWriter? = null
    private var socketHandler: Thread? = null

    private var p2pManager: WifiP2pManager? = null
    private var p2pReceiver: P2PBroadcastReceiver? = null
    private var p2pChannel: Channel? = null
    private lateinit var p2pIntentFiler: IntentFilter

    private var wifiManager: WifiManager? = null
    private var wifiReceiver: WifiBroadcastReceiver? = null
    private lateinit var wifiIntentFilter: IntentFilter

    private lateinit var broadcaster: LocalBroadcastManager
    private var thisDevice: WifiP2pDevice? = null
    private var p2pGroup: List<WifiP2pPeer>? = null
    private var p2pHost: WifiP2pDevice? = null


    private var isGroupFormed: Boolean = false
    private var isGroupOwner: Boolean = false

    private var isP2pEnabled: Boolean = false


    private val handler = Handler(this as Handler.Callback)

    private var socketListening: Boolean = false


    override fun onCreate() {
        Logger.f()
        super.onCreate()

        wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        registerWiFiReceiver()

//        wifiManager!!.startScan()

        if (wifiManager!!.isWifiEnabled) {
            Logger.i("Wi-Fi enabled")
        } else {
            Logger.w("Wi-Fi disabled")
        }


        broadcaster = LocalBroadcastManager.getInstance(this)
    }


    private fun registerP2P(){
        Logger.f()
        p2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        if(p2pManager == null){
            Logger.e("Could not create p2pManager")
        }

        p2pChannel = p2pManager!!.initialize(this, Looper.getMainLooper(), null)
        if(p2pChannel == null){
            Logger.e("Could not create p2pChannel")
        }

        Logger.i("Registered P2P")
    }

    private fun unregisterP2P(){
        Logger.f()
        if(p2pManager != null) {
            p2pManager = null
            p2pChannel = null
            thisDevice = null
            broadcaster.sendBroadcast(Intent().apply {
                action = INTERNAL_DEVICE_CHANGED
            })
            Logger.i("Unregistered P2P")
        }
    }

    private fun registerP2PReceiver(){
        Logger.f()
        p2pReceiver = P2PBroadcastReceiver()

        p2pIntentFiler = IntentFilter().apply {
            addAction(WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        registerReceiver(p2pReceiver, p2pIntentFiler)
        Logger.i("Registered p2pReceiver")
    }

    private fun unregisterP2PReceiver(){
        Logger.f()
        if(p2pReceiver != null){
            unregisterReceiver(p2pReceiver)
            p2pReceiver = null
            Logger.i("Unregistered p2pReceiver")
        }
    }

    private fun registerWiFiReceiver() {
        Logger.f()
        wifiReceiver = WifiBroadcastReceiver()
        wifiIntentFilter = IntentFilter().apply {
            addAction(WIFI_STATE_CHANGED_ACTION)
            addAction(SCAN_RESULTS_AVAILABLE_ACTION)
        }

        registerReceiver(wifiReceiver, wifiIntentFilter)
        Logger.i("Registered wifiReceiver")
    }

    private fun unregisterWifiReceiver() {
        Logger.f()
        if (wifiReceiver != null) {
            unregisterReceiver(wifiReceiver)
            wifiReceiver = null
            Logger.i("Unregistered wifiReceiver")
        }
    }

    inner class WifiBroadcastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            onHandleIntent(intent)
        }

    }


    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        Logger.f()
        Logger.i("Connection information available: $info")

        if(info != null) {
            isGroupFormed = info.groupFormed
            isGroupOwner  = info.isGroupOwner

            if(isGroupFormed) {
//                stopServiceDiscovery()

                startSocketListening()

                if(isGroupOwner){
                    Logger.i("Connected as Group Owner")

                    if( socketHandler == null) {
                        try {
//                            socketHandler = HostSocketHandler(handler)
//                            (socketHandler as HostSocketHandler).start()
                        } catch (e: Exception) {
                            Logger.e("Failed to create a host handler: ${e.message}")
                        }
                    }

                } else {
                    Logger.i("Connected as Peer")
//                    socketHandler = PeerSocketHandler(handler, info.groupOwnerAddress)
//                    (socketHandler as PeerSocketHandler).start()
                }

                broadcaster.sendBroadcast(Intent().apply {
                    action = INTERNAL_CONNECTED
                })


            } else {
                Logger.w("No Group Formed")


            }


        }

        broadcaster.sendBroadcast(Intent().apply {
            action = INTERNAL_DEVICE_CHANGED
        })
    }

    override fun onDestroy() {
        Logger.f()
        super.onDestroy()
//        stopServiceDiscovery()
        removeGroup()
        unregisterP2PReceiver()
        unregisterWifiReceiver()
        unregisterP2P()
        Logger.i("WifiP2PHandler destroyed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }



    fun removeGroup() {
        Logger.f()

        stopSocketListening()

        if(p2pGroup == null){
            Logger.w("Cannot remove user from null group")
            return
        }

        p2pManager?.also { manager ->
            manager.removeGroup(p2pChannel, object: ActionListener{
                override fun onSuccess() {
                    socketHandler?.also{ socket ->
                        socket.interrupt()
                    }
                    p2pGroup = null
                    isGroupFormed = false
                    isGroupOwner = false
                    Logger.i("Removed From Group")
                }

                override fun onFailure(reason: Int) {
                    Logger.e(
                        "Connect Request Failed: " +
                                when(reason){
                                    WifiP2pManager.P2P_UNSUPPORTED -> "P2P is Unsupported"
                                    WifiP2pManager.BUSY -> "BUSY"
                                    WifiP2pManager.NO_SERVICE_REQUESTS -> "NO_SERVICE_REQUESTS"
                                    else -> "Unexpected Error"
                                }
                    )

                    broadcaster.sendBroadcast(Intent().apply {
                        action = INTERNAL_ERROR
                        putExtra(INTERNAL_ERROR_MSG, "Unable to leave group, please try again")
                    })
                }

            })
        }
    }


    /**
     * Handle intent actions for [p2pReceiver] and [wifiReceiver]
     */
    fun onHandleIntent(intent: Intent) {
        Logger.f("called | ${intent.action?.split('.')?.last()}")

        when(intent.action){
            WIFI_P2P_PEERS_CHANGED_ACTION -> peersChanged(intent)
            WIFI_P2P_CONNECTION_CHANGED_ACTION -> connectionChanged(intent)
            WIFI_P2P_STATE_CHANGED_ACTION -> p2pStateChanged(intent)
            WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> deviceChanged(intent)
            WIFI_STATE_CHANGED_ACTION -> wifiStateChanged(intent)
//            SCAN_RESULTS_AVAILABLE_ACTION -> scanResultsAvailable(intent)
            else -> {
                Logger.w("Received unaccounted for action: ${intent.action}")
            }
        }
    }


    private fun p2pStateChanged(intent: Intent) {
        Logger.f()

        isP2pEnabled = when(val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)){
            WIFI_P2P_STATE_ENABLED -> {
                Logger.i("Wifi P2P Enabled")
                true
            }
            WIFI_P2P_STATE_DISABLED -> {
                Logger.i("Wifi P2P Disabled")
                false
            }
            else -> {
                Logger.w("Unexpected WifiP2P State: $state")
                false
            }

        }

    }

    private fun wifiStateChanged(intent: Intent) {
        Logger.f()

        when (val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)){
            WIFI_STATE_ENABLED -> {
                Logger.i("WiFi Enabled - Registering P2P")
                registerP2P()
                registerP2PReceiver()
            }
            WIFI_STATE_DISABLED -> {
                Logger.i("WiFi Disabled - Unregistering P2P")
                unregisterP2PReceiver()
                unregisterP2P()
            }
            else -> {
                Logger.w("Unexpected wifi state: $state")
            }
        }
    }

    /**
     * Peers are available, broadcast the list of peers
     */
    @SuppressLint("MissingPermission")
    private fun peersChanged(intent: Intent) {
        Logger.d("Called")

        p2pManager?.also {
            it.requestPeers(p2pChannel) { peers ->
                if (peers == null) {
                    Logger.e("Error retrieving peers")
                    return@requestPeers
                }

                Logger.i("Peers Available: ${deviceListToListOfPeers(peers).size}")

                broadcaster.sendBroadcast(Intent().apply {
                    action = INTERNAL_PEERS_CHANGED
                    putExtra(INTERNAL_PEERS, peers)
                })
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectionChanged(intent: Intent) {
        Logger.f()

        if(p2pManager == null)
            return

        val netInfo: NetworkInfo? = intent.getParcelableExtra(EXTRA_NETWORK_INFO)
        netInfo.also {
            if(it!!.isConnected){
                p2pManager!!.requestConnectionInfo(p2pChannel, this)
            } else {
                broadcaster.sendBroadcast(Intent().apply {
                    action = INTERNAL_DISCONNECTED
                })
            }
        }

        p2pManager!!.requestGroupInfo(p2pChannel, GroupInfoListener { group ->
            if(group == null)
                return@GroupInfoListener

            p2pGroup = deviceListToListOfPeers(group = group)
            p2pHost = group.owner
        })
    }

    private fun deviceChanged(intent: Intent) {
        Logger.f()

        thisDevice = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE)
        Logger.i("This Device: \n$thisDevice")

        broadcaster.sendBroadcast(Intent().apply {
            action = INTERNAL_DEVICE_CHANGED
        })
    }

    @SuppressLint("MissingPermission")
    fun openDiscovery() {
        Logger.f()

        p2pManager?.also {
            it.discoverPeers(p2pChannel, object : ActionListener {
                override fun onSuccess() {
                    Logger.i("Discovery Launched Successfully")
                }

                override fun onFailure(reason: Int) {
                    Logger.w(
                        "Error Launching Discovery: " +
                                when (reason) {
                                    WifiP2pManager.P2P_UNSUPPORTED -> "P2P is Unsupported"
                                    WifiP2pManager.BUSY -> "BUSY"
                                    WifiP2pManager.NO_SERVICE_REQUESTS -> "NO_SERVICE_REQUESTS"
                                    else -> "Unexpected Error"
                                }
                    )
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    fun initiateConnectToPeer(peerAddress: String) {

        if(isGroupFormed){
            Logger.w("You are already in a group")
            return
        }

        val p2pConfig = WifiP2pConfig().apply {
            deviceAddress = peerAddress
            groupOwnerIntent = GROUP_OWNER_INTENT_MIN
        }

        p2pManager?.also {
            it.connect(p2pChannel, p2pConfig, object : ActionListener{
                override fun onSuccess() {
                    Logger.i("Initiating Connection")
                }

                override fun onFailure(reason: Int) {
                    Logger.i("Failed to Initiate Connection: $reason")
                }
            })
        }
    }


    /**
     * Handle messages sent from [SocketReaderWriter]
     */
    override fun handleMessage(msg: Message): Boolean {
        Logger.f()

        when(msg.what) {
            0x400 + 1 -> {
                Logger.w("SocketWriter Sent: 0x400 + 1")
                val readBuf = msg.obj as ByteArray

                // construct a string from the valid bytes in the buffer
                val receivedMessage = String(readBuf, 0, msg.arg1)
                Logger.i("Received: ${receivedMessage.subSequence(0, min(0, receivedMessage.length))} | Length: ${receivedMessage.length}")

//                broadcaster.sendBroadcast(Intent().apply {
//                    action = INTERNAL_MESSAGE_RECEIVED
//                    putExtra(INTERNAL_MESSAGE_KEY, readBuf)
//                })

            }

//            0x400 + 2 -> {
//                Logger.w("SocketWriter Sent: 0x400 + 2")
//                socketWriter = msg.obj as SocketReaderWriter
//                broadcaster.sendBroadcast(Intent().apply {
//                    action = INTERNAL_CONNECTED
//                })
//            }
//
//            0x400 + 3 -> {
//                Logger.w("SocketWriter Sent: 0x400 + 3")
//                broadcaster.sendBroadcast(Intent().apply {
//                    action = INTERNAL_DISCONNECTED
//                })
//            }
        }

        return true
    }

    fun isGroupFormedStatus(): Boolean {
        Logger.d("called | isGroupFormed $isGroupFormed")
        return isGroupFormed
    }

    fun isGroupOwnerStatus(): Boolean {
        Logger.d("called | isGroupOwner $isGroupOwner")
        return isGroupOwner
    }

    fun getP2PGroup(): List<WifiP2pPeer>?{
        return p2pGroup
    }

    fun getP2PHost(): WifiP2pDevice? {
        return p2pHost
    }

//    fun getSocketWriter(): SocketReaderWriter? {
//        return socketWriter
//    }

    @SuppressLint("MissingPermission")
    fun createGroup() {
        Logger.d("called | Creating a new empty group")

//        val p2pConfig = WifiP2pConfig().apply {
//            groupOwnerIntent = GROUP_OWNER_INTENT_MAX
//        }


        p2pChannel?.also {channel ->
            p2pManager?.also {
                it.createGroup(channel, object : ActionListener {
                    override fun onSuccess() {
                        Logger.d("Group created successfully")
                    }

                    override fun onFailure(reason: Int) {
                        Logger.e("Failed to create group: $reason")
                    }

                })
            }
        }
    }

    fun sendData(context: Context, data: String, address: String): Boolean{
        if(data.isBlank()){
            Logger.w("Inefficient to transmit empty data")
//            return
        }

        try{
            startService(Intent(context, SocketWriter::class.java).apply {
                action = ACTION_SEND_JSON
                putExtra(JSON_DATA, data)
                putExtra(RECIPIENT_ADDRESS, address)
            })
        } catch (e: Exception) {
            Logger.e("Failed to start SocketWriter service")
            return false
        }
        return true
    }

    private fun startSocketListening(){
        if(!socketListening) {
            SocketListener.getInstance(handler).execute()
            socketListening = true
        }
    }

    private fun stopSocketListening(){
        if(socketListening) {
            SocketListener.getInstance(handler).cancel(true)
            socketListening = false
        }
    }

    /**
     * @return Boolean value indicating state of [WifiP2PHandler]
     */
    fun verifyP2PState(): Boolean{
        return ((p2pChannel != null) && (p2pManager != null))
    }

    inner class P2PBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            onHandleIntent(intent)
        }

    }



    companion object {
        const val GROUP_PORT: Int = 8898

        private var instance: WifiP2PHandler? = null

        fun getInstance(): WifiP2PHandler {
            return instance ?: synchronized(this) {
                instance ?: WifiP2PHandler().also { instance = it }
            }
        }
    }
}


