package com.leadwoods.p2p_environment.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.leadwoods.p2p_environment.PeersAdapter
import com.leadwoods.p2p_environment.R
import com.leadwoods.p2p_environment.WifiP2pPeer
import com.leadwoods.p2p_environment.databinding.ActivityMainBinding
import com.leadwoods.p2p_environment.deviceListToListOfPeers
import com.leadwoods.p2p_environment.support.AppBase
import com.leadwoods.p2p_environment.support.INTERNAL_CONNECTED
import com.leadwoods.p2p_environment.support.INTERNAL_DEVICE_CHANGED
import com.leadwoods.p2p_environment.support.INTERNAL_ERROR_MSG
import com.leadwoods.p2p_environment.support.INTERNAL_MESSAGE_RECEIVED
import com.leadwoods.p2p_environment.support.INTERNAL_PEERS_CHANGED
import com.leadwoods.p2p_environment.support.INTERNAL_WIFI_STATE_CHANGED
import com.leadwoods.p2p_environment.support.Logger
import com.leadwoods.p2p_environment.support.checkAllPermissions
import com.leadwoods.p2p_environment.support.permissionsOverview
import com.leadwoods.p2p_environment.support.requestAllPermissions
import com.leadwoods.p2p_environment.wifip2p.p2p_handler.P2PHandlerBroadcastReceiver
import com.leadwoods.p2p_environment.wifip2p.p2p_handler.WifiP2PHandler
import kotlin.math.min

class SecondaryActivity: AppBase(), PeersAdapter.PeerTouchInterface, P2PHandlerBroadcastReceiver.ReceiverInterface {

    companion object {
        const val PERMISSION_REQUEST_CODE = 902
    }

    private var p2pService: WifiP2PHandler? = null
    private var p2pServiceConnection: P2PServiceConnection? = null
    private var p2pBound: Boolean = false

    /**
     * Service connection implementation used for binding to [WifiP2PHandler].
     * Handles the connection and disconnection events between the activity and the service.
     */
    inner class P2PServiceConnection: ServiceConnection {

        /**
         * Called when a connection to the service has been established
         * @param name The component name of the service that has been connected
         * @param service The IBinder of the service's communication channel, which you can now make calls on.
         */
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Logger.f()

            if(service is WifiP2PHandler.P2PServiceBinder){
                p2pService = service.getService()
                p2pBound = true
                Logger.i("WifiP2PHandlerService connected")
                 return
            }
        }

        /**
         * Called when a connection to the service has been lost
         * This does not remove the [ServiceConnection]
         * @param name The component name of the service whose connection has been lost.
         */
        override fun onServiceDisconnected(name: ComponentName) {
            Logger.f()
            p2pService = null
            p2pBound = true
            Logger.i("WifiP2PHandler disconnected")
        }
    }

    // UI Binding
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    // UI Elements
    private val peersRV: RecyclerView get() = binding.RVGroups
    private val connectionDetailsCL: ConstraintLayout get() = binding.CLConnectionDetails
    private val hostGroupB: Button get() = binding.BHostGroup
    private val scanForGroupsB: Button get() = binding.BScanForGroups
    private val sendDataB: Button get() = binding.BBroadcastToGroup
    private val userTypeTV: TextView get() = binding.TVUserType
    private val transmitET: EditText get() = binding.ETTransmitData
    private val receivedTV: TextView get() = binding.TVReceivedData

    // Filtering Flag
    private var showOnlyGroupOwners: Boolean = false

    // ListOf last retrieved peers
    private var mostRecentPeers = listOf<WifiP2pPeer>()


    /**
     * Callback for the result of requesting permissions.
     * @param requestCode The request code passed to requestPermissions()
     * @param permissions The requested permissions. This array will never be empty.
     * @param grantResults The results of the corresponding permissions requests. This array will be the same length as the permissions array.
     * Contains either [PackageManager.PERMISSION_GRANTED] or [PackageManager.PERMISSION_DENIED] for each permission requested.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Logger.f()
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_REQUEST_CODE -> {
                for (permission in grantResults) {
                    if (permission != PackageManager.PERMISSION_GRANTED) {
                        Logger.w("Permission Not Granted (${permission.toString().split(".").last()})")
                        finish()
                    }
                }
            }
            else -> {
                Logger.w("Unexpected Permission Request Code")
            }
        }
    }

    /**
     * Checks for necessary permissions and requests them if not granted.
     * If [toast] is true, displays a toast message containing the permissions overview.
     *
     * @param toast Flag indicating whether to display a toast message with permissions overview.
     */
    private fun checkAndRequestPermissions(toast: Boolean) {
        Logger.f()
        // Request Permissions if not granted
        if(!checkAllPermissions())
        {
            requestAllPermissions(this, supportFragmentManager)
        }

        // Display results if requested
        if(toast) {
            val overview = permissionsOverview()
            Toast.makeText(this, "${overview.second}/${overview.third}", Toast.LENGTH_SHORT).show()
            Logger.d(overview.first)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.f()
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        checkSystemRequirements()

        checkAndRequestPermissions(false)

        registerCommunicationReceiver()

        p2pServiceConnection = P2PServiceConnection()
        val serviceIntent = Intent(this, WifiP2PHandler::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, p2pServiceConnection!!, BIND_AUTO_CREATE)

        configInteractables()

    }

    /**
     * Checks system requirements related to Wi-Fi Direct.
     * Checks if Wi-Fi Direct is supported by the device, if Wi-Fi system service can be obtained,
     * and if Wi-Fi Direct is supported by the hardware and Wi-Fi is enabled.
     * Displays error messages and returns Boolean value indicating success
     * @return Boolean value indicating check success
     */
    private fun checkSystemRequirements(): Boolean {
        Logger.f()
        // Compatibility check
        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Logger.e("Wi-Fi Direct is not supported by this device")
            Toast.makeText(this, "Wi-Fi Direct is not supported by this device", Toast.LENGTH_LONG).show()
            return false
        }

        // Hardware check
        if(getSystemService(Context.WIFI_SERVICE) == null){
            Logger.e("Cannot get Wi-Fi system service")
            Toast.makeText(this, "Cannot get Wi-Fi system service", Toast.LENGTH_LONG).show()
            return false
        }

        // Availability check
        if (!(getSystemService(Context.WIFI_SERVICE) as WifiManager).isP2pSupported) {
            Logger.e("Wi-Fi Direct is not supported by the hardware or Wi-Fi is off")
            Toast.makeText(this, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    /**
     * Called when the activity is about to be destroyed.
     * This is the final call that the activity will receive before it is destroyed.
     * Unbinds the service connection if it is currently bound to the P2PHandler service.
     */
    override fun onDestroy() {
        Logger.f()
        super.onDestroy()
        if (p2pBound) {
            unbindService(p2pServiceConnection!!)
            p2pBound = false
        }

        Logger.i("Unbound P2P Handler")
    }

    /**
     * Initialize the contents of the Activity's standard options menu.
     * This is only called once, the first time the options menu is displayed.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Logger.f()
        menuInflater.inflate(R.menu.menu_primary, menu)

        menu?.findItem(R.id.SI_AllPeersToggle)?.isChecked = showOnlyGroupOwners

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * @param item The [MenuItem] that was selected by the user
     * @return always true as all outcomes finish handling the user's input
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Logger.f()
        when(item.itemId){
            R.id.MI_RequestPermissions -> {
                checkAndRequestPermissions(true)
            }


            R.id.MI_Disconnect -> {
                p2pService?.also{ serviceHandler ->
                    if(!serviceHandler.verifyP2PState()){
                        Logger.w("P2P Not initialised")
                        return true
                    }

                    if(serviceHandler.isGroupFormedStatus()) {
                        serviceHandler.removeGroup()

                    }
                }
            }

            R.id.SI_AllPeersToggle -> {
                showOnlyGroupOwners = !showOnlyGroupOwners
                item.isChecked = showOnlyGroupOwners

                // Refresh the peers recycler view
                peersRV.adapter = PeersAdapter(mostRecentPeers, showOnlyGroupOwners, this)
            }

            else -> {
                throw Exception("Unexpected Menu Selection")
            }
        }

        return true
    }

    private fun configInteractables() {

        // Configure peers RV
        peersRV.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        // Send Data across network Button
        sendDataB.setOnClickListener {v ->
            Logger.d("${resources.getResourceEntryName(v.id)} clicked")

            p2pService?.also { serviceHandler ->
                if (!serviceHandler.verifyP2PState()) {
                    Logger.w("P2P Not initialised")
                    return@setOnClickListener
                }


//                val socketWriter = serviceHandler.getSocketWriter()
//                if (socketWriter == null) {
//                    Logger.w("Cannot send data using null socket writer")
//                    return@setOnClickListener
//                }

                val data = transmitET.text.toString()
                Logger.d("Transmitting: ${data.subSequence(0, min(data.length, 20))}")


                if(serviceHandler.isGroupOwnerStatus()){
                    val group = serviceHandler.getP2PGroup()
                    if(group== null){
                        Logger.w("There are no peers in this group")
                    }
                    for(peer in group!!){
                        val address = peer.device.deviceAddress
                        Logger.i("Sending data to peer @ $address")
                        serviceHandler.sendData(this@SecondaryActivity, data, address)
                    }
                }
                else{
                    val address = serviceHandler.getP2PHost()?.deviceAddress
                    if (address == null) {
                        Logger.w("Cannot send data to host with null address")
                        return@setOnClickListener
                    }

                    Logger.i("Sending data to host")
                    serviceHandler.sendData(this@SecondaryActivity, data, address)
                }
            }
        }

        // Scan for nearby peers
        scanForGroupsB.setOnClickListener { v ->
            Logger.d("${resources.getResourceEntryName(v.id)} clicked")

            p2pService?.also { serviceHandler ->
                if(!serviceHandler.verifyP2PState()){
                    Logger.w("P2P Not initialised")
                    return@setOnClickListener
                }

                if (serviceHandler.isGroupFormedStatus()) {
                    Toast.makeText(this, "Please leave your current group first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Clear existing Peers
                clearPeersRV()

                // Begin Discovery for nearby peers
                serviceHandler.openDiscovery()
                Logger.i("Peer Discovery Opened")
            }
        }

        // Start hosting a lobby
        hostGroupB.setOnClickListener { v ->
            Logger.d("${resources.getResourceEntryName(v.id)} clicked")

            p2pService?.also { serviceHandler ->
                if(!serviceHandler.verifyP2PState()){
                    Logger.w("P2P Not initialised")
                    return@setOnClickListener
                }

                if (serviceHandler.isGroupFormedStatus()) {
                    Toast.makeText(this, "Cannot host more than one group at a time", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Set the Host Button to non-clickable
                v.isClickable = false
                v.alpha = .5f

                // Create New P2P Group
                serviceHandler.createGroup()
                Logger.i("Handling Create Group Request")
            }
        }
    }

    /**
     * Registers Broadcaster Receiver for P2PHandler intents
     */
    private fun registerCommunicationReceiver() {
        Logger.f()
        val communicationReceiver = P2PHandlerBroadcastReceiver(this)
        val intentFilter = IntentFilter().apply {
            addAction(INTERNAL_CONNECTED)
            addAction(INTERNAL_DEVICE_CHANGED)
            addAction(INTERNAL_MESSAGE_RECEIVED)
            addAction(INTERNAL_WIFI_STATE_CHANGED)
            addAction(INTERNAL_PEERS_CHANGED)
            addAction(INTERNAL_ERROR_MSG)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(communicationReceiver, intentFilter)

        Logger.i("CommunicationReceiver registered")
    }

    override fun peerConnectRequest(peer: WifiP2pPeer) {
        Logger.f()

        p2pService?.also{ serviceHandler ->
            if(!serviceHandler.verifyP2PState()){
                Logger.w("P2P Not initialised")
                return@peerConnectRequest
            }

            Toast.makeText(this, "Connecting to: ${peer.device.deviceName}", Toast.LENGTH_SHORT).show()
            serviceHandler.initiateConnectToPeer(peer.device.deviceAddress)
            return
        }

        Logger.w("p2pHandler is null")
    }

    override fun dataReceived() {
        Logger.f()
    }

    override fun peersReceived(peers: WifiP2pDeviceList) {
        Logger.f()
        mostRecentPeers = deviceListToListOfPeers(peers = peers)
        peersRV.adapter = PeersAdapter(mostRecentPeers, showOnlyGroupOwners, this)
    }

    override fun peerConnected() {
        Logger.f()

        updateUI()
    }

    private fun updateUI(){
        peersRV.visibility = if (p2pService?.isGroupFormedStatus() == true) View.GONE else View.VISIBLE
        connectionDetailsCL.visibility = if (p2pService?.isGroupFormedStatus() == true) View.VISIBLE else View.GONE

        userTypeTV.text = if(p2pService?.isGroupOwnerStatus() == true) "You are the Group Owner" else "Connected to: ${p2pService?.getP2PHost()?.deviceAddress}"
    }


    private fun clearPeersRV(){
        peersRV.adapter = PeersAdapter()
    }
}