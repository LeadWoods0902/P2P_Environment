//package com.leadwoods.p2p_environment.activities
//
//import android.Manifest.permission.NEARBY_WIFI_DEVICES
//import android.annotation.SuppressLint
//import android.content.pm.PackageManager
//import android.net.wifi.p2p.WifiP2pDevice
//import android.net.wifi.p2p.WifiP2pDeviceList
//import android.net.wifi.p2p.WifiP2pInfo
//import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
//import android.os.Build
//import android.os.Bundle
//import android.view.Menu
//import android.view.MenuItem
//import android.view.View
//import android.widget.Button
//import android.widget.EditText
//import android.widget.TextView
//import android.widget.Toast
//import androidx.constraintlayout.widget.ConstraintLayout
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.leadwoods.p2p_environment.PeersAdapter
//import com.leadwoods.p2p_environment.R
//import com.leadwoods.p2p_environment.WifiP2pPeer
//import com.leadwoods.p2p_environment.databinding.ActivityMainBinding
//import com.leadwoods.p2p_environment.deviceListToListOfPeers
//import com.leadwoods.p2p_environment.networking.P2pBaseHandler
//import com.leadwoods.p2p_environment.support.AppBase
//import com.leadwoods.p2p_environment.support.Logger
//import com.leadwoods.p2p_environment.support.checkAllPermissions
//import com.leadwoods.p2p_environment.support.permissionsOverview
//import com.leadwoods.p2p_environment.support.requestAllPermissions
//import kotlin.math.min
//
//
///**
// * Main Activity
// * @property peersRV RecyclerView for displaying instances of [WifiP2pDevice]
// * @property sendDataB Button for broadcasting as a hot
// * @property scanForGroupsB Button for searching scan for hosts
// * @property isP2PEnabled Boolean flag
// * @property p2pIntentFilter
// * @property p2pManager
// * @property p2pChannel
// * @property p2pReceiver
// */
//class MainActivity: AppBase(), PeersAdapter.PeerTouchInterface, ConnectionInfoListener  {
//
//
//    companion object {
//        const val PERMISSION_REQUEST_CODE = 902
//    }
//
//    // Game Control Flags
//    private var isGameMaster: Boolean = false
//
//
//    // UI elements
//
//    private var _binding: ActivityMainBinding? = null
//
//    private val binding get() = _binding!!
//
//    private val peersRV: RecyclerView get() = binding.RVGroups
//    private val connectionDetailsCL: ConstraintLayout  get() = binding.CLConnectionDetails
//    private val hostGroupB: Button get() = binding.BHostGroup
//    private val scanForGroupsB: Button get() = binding.BScanForGroups
//    private val sendDataB: Button get() = binding.BBroadcastToGroup
//
//    private val userTypeTV: TextView get() = binding.TVUserType
//    private val transmitET: EditText get() = binding.ETTransmitData
//    private val receivedTV: TextView get() = binding.TVReceivedData
//
//    // Networking
//    private var isP2PEnabled: Boolean = false
//    private lateinit var p2pHandler: P2pBaseHandler
//    private var mostRecentPeers = listOf<WifiP2pPeer>()
//
//
//    /**
//     * callback from P2PConnectionsListener, sets p2p enabled status
//     * @param enabled passed value for enabled status
//     */
//    fun p2pEnabled(enabled: Boolean){
//        Logger.d("called | P2P: ${if(enabled) "Enabled" else "Not Available"}")
//        isP2PEnabled = enabled
//    }
//
//
//    /**
//     * callback from permission request dialogs
//     * @param requestCode
//     * @param permissions
//     * @param grantResults
//     */
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when(requestCode){
//            PERMISSION_REQUEST_CODE -> {
//                for (permission in grantResults) {
//                    if (permission != PackageManager.PERMISSION_GRANTED) {
//                        Logger.w("Permission Not Granted (${permission.toString().split(".").last()})")
//                        finish()
//                    }
//                }
//            }
//            else -> {
//                Logger.w("Unexpected Permission Request Code")
//            }
//        }
//    }
//
//
//    /**
//     * Check & request any missing permissions
//     * @param toast Boolean flag indicating whether the user should be notified of the permission results
//     */
//    private fun checkAndRequestPermissions(toast: Boolean) {
//        Logger.f()
//        // Request Permissions if not granted
//        if(!checkAllPermissions())
//        {
//            requestAllPermissions(this, supportFragmentManager)
//        }
//
//        // Display results if requested
//        if(toast) {
//            val overview = permissionsOverview()
//            Toast.makeText(this, "${overview.second}/${overview.third}", Toast.LENGTH_SHORT).show()
//            Logger.d(overview.first)
//        }
//    }
//
//
//    /**
//     * Creates the activity, calls for permission checks, sets up broadcasting & configures interface
//     * @param savedInstanceState
//     */
//    override fun onCreate(savedInstanceState: Bundle?) {
//        Logger.f()
//        super.onCreate(savedInstanceState)
//        _binding = ActivityMainBinding.inflate(layoutInflater)
//
//        setContentView(binding.root)
//
//        p2pHandler = P2pBaseHandler.getInstance()
//
//        // Attempt to Initialise P2P
//        if (!p2pHandler.initP2p(this)) {
//            // Toast Error if P2P not available
//            Logger.e("ERROR in P2P initialisation")
//            finish()
//        }
//
//        // P2P is available, so check permissions
//        checkAndRequestPermissions(false)
//
//        // Configure Interface
//        configInterface()
//    }
//
//    /**
//     * When resuming the the activity, create a new listener if necessary then register it
//     */
//    override fun onResume() {
//        Logger.f()
//        super.onResume()
//        if(::p2pHandler.isInitialized)
//            p2pHandler.register(this)
//
//    }
//
//    /**
//     * When pausing the the activity, unregister the receiver if it still exists
//     */
//    override fun onPause() {
//        Logger.f()
//        super.onPause()
//        if(::p2pHandler.isInitialized)
//            p2pHandler.register(this)
//    }
//
//
//    /**
//     * Configures the User Interface
//     */
//    private fun configInterface() {
//
//        // Configure peers RV
//        peersRV.layoutManager = LinearLayoutManager(this).apply {
//            orientation = LinearLayoutManager.VERTICAL
//        }
//
//        // Send Data across network Button
//        sendDataB.setOnClickListener {
//            Logger.d("${resources.getResourceEntryName(it.id)} clicked")
//
//            val data = transmitET.text.toString()
//            Logger.d("Transmitting: ${data.subSequence(0, min(data.length, 20))}")
//
//            // if address not found, don't start a service
//            val address = p2pHandler.getHostAddress()
//            if(address == null)
//            {
//                Logger.e("Error: Null Address in Handler")
//                return@setOnClickListener
//            }
//
//            if(isGameMaster)
//                p2pHandler.transmitDataToAll(this, data)
//            else
//                p2pHandler.transmitData(this, data, address)
//        }
//
//        // Scan for nearby peers
//        scanForGroupsB.setOnClickListener {
//            Logger.d("${resources.getResourceEntryName(it.id)} clicked")
//
//            if(p2pHandler.getGroupStatus()){
//                Toast.makeText(this, "Please leave your current group first", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            // Clear existing Peers
//            clearPeerResults()
//
//            // Scan for nearby peers
//            p2pHandler.scanForNearbyPeers()
//        }
//
//        // Start hosting a lobby
//        hostGroupB.setOnClickListener {
//            Logger.d("${resources.getResourceEntryName(it.id)} clicked")
//
//            if(p2pHandler.getGroupStatus()){
//                Toast.makeText(this, "Please leave your current group first", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            // Clear existing Peers
//            clearPeerResults()
//
//            // Create a new group and make it visible
//            p2pHandler.createGroup()
//        }
//    }
//
//    /**
//     * Clear the peers RV
//     */
//    fun clearPeerResults() {
//        Logger.f()
//        peersRV.adapter = PeersAdapter()
//    }
//
//
//
//
//
//    /**
//     *  Inflate the options menu, disable the NearbyDevice's prompt if not on at least Tiramisu
//     *  @param menu the [Menu] object to inflate a view inside
//     */
//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        Logger.f()
//        menuInflater.inflate(R.menu.menu_primary, menu)
//
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//            menu?.findItem(R.id.MI_NearbyDevices)?.setVisible(false)
//            menu?.findItem(R.id.SI_AllPeersToggle)?.isChecked = p2pHandler.getPeerVisibility()
//        }
//
//        return super.onCreateOptionsMenu(menu)
//    }
//
//
//    @SuppressLint("UseSwitchCompatOrMaterialCode")
//    /**
//     * @param item The [MenuItem] that was selected by the user
//     * @return always true as all outcomes finish handling the user's input
//     */
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        Logger.f()
//        when(item.itemId){
//            R.id.MI_RequestPermissions -> {
//                checkAndRequestPermissions(true)
//            }
//
//            R.id.MI_NearbyDevices -> {
//                if(ContextCompat.checkSelfPermission(this, NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED)
//                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
//                else
//                    Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
//            }
//
//            R.id.MI_Disconnect -> {
//                if(p2pHandler.getGroupStatus()) {
//                    p2pHandler.disconnect()
//                }
//            }
//
//            R.id.SI_AllPeersToggle -> {
//                p2pHandler.getPeerVisibility().let {
//                    p2pHandler.setPeersVisibility(!it)
//                    item.isChecked = !it
//                }
//
//                // Refresh the peers recycler view
//                peersRV.adapter = PeersAdapter(mostRecentPeers, p2pHandler.getPeerVisibility(), this)
//            }
//
//            else -> {
//                throw Exception("Unexpected Menu Selection")
//            }
//        }
//
//        return true
//    }
//
//
//    /**
//     * callback from p2pListener with list of found peers
//     * @param devices instance of [WifiP2pDeviceList] containing nearby peers
//     */
//    fun onPeersAvailable(devices: WifiP2pDeviceList) {
//        Logger.f()
//
//        mostRecentPeers = deviceListToListOfPeers(devices)
//
//        // Display the required peers in the list
//        peersRV.adapter = PeersAdapter(mostRecentPeers, p2pHandler.getPeerVisibility(), this)
//
//    }
//
//    /**
//     * callback from [PeersAdapter] indicating the user has selected a peer to connect to
//     * @param peer Instance of [WifiP2pDevice] denoting a nearby device
//     */
//    override fun peerConnectRequest(peer: WifiP2pPeer) {
//        Logger.d("called | Connecting To: ${peer.device.deviceName}")
//
//        if(peer.device.isGroupOwner)
//            p2pHandler.connect(peer)
//        else{
//            Toast.makeText(this, "This User is not a Group Owner", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//
//
//    /**
//     * Connection info about the current group available
//     * @param info Information to be displayed
//     */
//    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
//        Logger.d("called | $info")
//        if (info != null) {
//            Logger.d("Connected to ${info.groupOwnerAddress}")
//            // Update the UI to reflect the device's user type
//            if(info.groupFormed) {
//
//                p2pHandler.setHostAddress(info.groupOwnerAddress, info.isGroupOwner)
//
//                p2pHandler.startSocketListening(this)
//
//                updateInterface(info, true)
//            } else {
//                updateInterface(null, false)
//            }
//        }
//    }
//
//    private fun updateInterface(info: WifiP2pInfo?, connectedToGroup: Boolean) {
//        Logger.f()
//
//        if(connectedToGroup) {
//            peersRV.visibility = View.GONE
//            connectionDetailsCL.visibility = View.VISIBLE
//
//            if (info!!.isGroupOwner) {
//                userTypeTV.text = "Hosting Group"
//            } else {
//                userTypeTV.text = "Connected to: ${info.groupOwnerAddress}"
//            }
//        } else {
//            peersRV.visibility = View.VISIBLE
//            connectionDetailsCL.visibility = View.GONE
//        }
//    }
//
//    /**
//     * Callback from DataListener
//     */
//    fun downloadComplete(received: String) {
//        Logger.f()
//        receivedTV.text = received
//
//        if(p2pHandler.resumeSocketListening())
//            p2pHandler.startSocketListening(this)
//    }
//
//}