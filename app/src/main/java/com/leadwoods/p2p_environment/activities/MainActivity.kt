package com.leadwoods.p2p_environment.activities

import android.Manifest
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.leadwoods.p2p_environment.PeersAdapter
import com.leadwoods.p2p_environment.R
import com.leadwoods.p2p_environment.WifiP2pPeer
import com.leadwoods.p2p_environment.deviceListToListOfPeers
import com.leadwoods.p2p_environment.networking.DataListener
import com.leadwoods.p2p_environment.networking.P2pBaseHandler
import com.leadwoods.p2p_environment.support.AppBase
import com.leadwoods.p2p_environment.support.Logger
import com.leadwoods.p2p_environment.support.checkAllPermissions
import com.leadwoods.p2p_environment.support.permissionsOverview
import com.leadwoods.p2p_environment.support.requestAllPermissions
import com.leadwoods.p2p_environment.tagPeersOwnerStatus
import kotlin.math.min


/**
 * Main Activity
 * @property peersRV RecyclerView for displaying instances of [WifiP2pDevice]
 * @property sendDataB Button for broadcasting as a hot
 * @property scanForGroupsB Button for searching scan for hosts
 * @property isP2PEnabled Boolean flag
 * @property p2pIntentFilter
 * @property p2pManager
 * @property p2pChannel
 * @property p2pReceiver
 */
class MainActivity: AppBase(), PeersAdapter.PeerTouchInterface, ConnectionInfoListener {


    companion object {
        const val PERMISSION_REQUEST_CODE = 902
    }

    // Game Control Flags
    private var isGameMaster: Boolean = false


    // UI elements
    private lateinit var peersRV: RecyclerView
    private lateinit var connectionDetailsCL: ConstraintLayout
    private lateinit var hostGroupB: Button
    private lateinit var scanForGroupsB: Button
    private lateinit var sendDataB: Button

    private lateinit var userTypeTV: TextView
    private lateinit var transmitET: EditText
    private lateinit var receivedTV: TextView

    // Networking
    private var isP2PEnabled: Boolean = false
    private lateinit var p2pHandler: P2pBaseHandler
    private var mostRecentPeers = listOf<WifiP2pPeer>()


    /**
     * callback from P2PConnectionsListener, sets p2p enabled status
     * @param enabled passed value for enabled status
     */
    fun p2pEnabled(enabled: Boolean){
        Logger.d("called | P2P: ${if(enabled) "Enabled" else "Not Available"}")
        isP2PEnabled = enabled
    }


    /**
     * callback from permission request dialogs
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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
     * Check & request any missing permissions
     * @param toast Boolean flag indicating whether the user should be notified of the permission results
     */
    private fun checkAndRequestPermissions(toast: Boolean) {
        Logger.d("called")
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


    /**
     * Creates the activity, calls for permission checks, sets up broadcasting & configures interface
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.d("called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        p2pHandler = P2pBaseHandler.getInstance()

        // Attempt to Initialise P2P
        if (!p2pHandler.initP2p(this)) {
            // Toast Error if P2P not available
            Logger.e("ERROR in P2P initialisation")
            finish()
        }

        // P2P is available, so check permissions
        checkAndRequestPermissions(false)

        // Configure Interface
        configInterface()
    }


    /**
     * Configures the User Interface
     */
    private fun configInterface() {
        // Get UI Elements
        peersRV = findViewById(R.id.RV_Groups)
        connectionDetailsCL = findViewById(R.id.CL_ConnectionDetails)
        hostGroupB = findViewById(R.id.B_HostGroup)
        sendDataB = findViewById(R.id.B_BroadcastToGroup)
        scanForGroupsB = findViewById(R.id.B_ScanForGroups)

        userTypeTV = findViewById(R.id.TV_UserType)
        receivedTV = findViewById(R.id.TV_ReceivedData)
        transmitET = findViewById(R.id.ET_TransmitData)

        peersRV.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        // Send Data across network Button
        sendDataB.setOnClickListener {
            Logger.d("${resources.getResourceEntryName(it.id)} clicked")

            val data = transmitET.text.toString()
            Logger.d("Transmitting: ${data.subSequence(0, min(data.length, 20))}")

            // if address not found, don't start a service
            val address = p2pHandler.getHostAddress()
            if(address == null)
            {
                Logger.e("Error: Null Address in Handler")
                return@setOnClickListener
            }

            if(isGameMaster)
                p2pHandler.transmitDataToAll(this, data)
            else
                p2pHandler.transmitData(this, data, address)
        }

        // Scan for nearby peers
        scanForGroupsB.setOnClickListener {
            Logger.d("${resources.getResourceEntryName(it.id)} clicked")

            if(p2pHandler.getGroupStatus()){
                Toast.makeText(this, "Please leave your current group first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Clear existing Peers
            clearPeerResults()

            // Scan for nearby peers
            p2pHandler.scanForNearbyPeers()
        }

        // Start hosting a lobby
        hostGroupB.setOnClickListener {
            Logger.d("${resources.getResourceEntryName(it.id)} clicked")

            if(p2pHandler.getGroupStatus()){
                Toast.makeText(this, "Please leave your current group first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Clear existing Peers
            clearPeerResults()

            // Create a new group and make it visible
            createGroup()
        }
    }

    /**
     * Clear the peers RV
     */
    fun clearPeerResults() {
        Logger.d("called")
        peersRV.adapter = PeersAdapter()
    }


    /**
     * When resuming the the activity, create a new listener if necessary then register it
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        Logger.d("called")
        super.onResume()

//        if (p2pReceiver == null){
//            Logger.d("p2pReceiver was null")
//            if(p2pManager != null && p2pChannel != null)
//                p2pReceiver = P2PConnectionsListener(p2pManager!!, p2pChannel!!, this)
//        }
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//            registerReceiver(p2pReceiver, p2pIntentFilter, RECEIVER_NOT_EXPORTED)
//        else
//            registerReceiver(p2pReceiver, p2pIntentFilter)


        p2pHandler.register(this)

    }

    /**
     * When pausing the the activity, unregister the receiver if it still exists
     */
    override fun onPause() {
        Logger.d("called")
        super.onPause()

//        p2pReceiver?.also { p2pReceiver ->
//            unregisterReceiver(p2pReceiver)
//        }

        p2pHandler.unregister(this)

    }


    /**
     *  Inflate the options menu, disable the NearbyDevice's prompt if not on at least Tiramisu
     *  @param menu the [Menu] object to inflate a view inside
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Logger.d("called")
        menuInflater.inflate(R.menu.menu_primary, menu)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            menu?.findItem(R.id.MI_NearbyDevices)?.setVisible(false)
            menu?.findItem(R.id.SI_AllPeersToggle)?.isChecked = p2pHandler.getPeerVisibility()
        }

        return super.onCreateOptionsMenu(menu)
    }


    @SuppressLint("UseSwitchCompatOrMaterialCode")
    /**
     * @param item The [MenuItem] that was selected by the user
     * @return always true as all outcomes finish handling the user's input
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Logger.d("called")
        when(item.itemId){
            R.id.MI_RequestPermissions -> {
                checkAndRequestPermissions(true)
            }

            R.id.MI_NearbyDevices -> {
                if(ContextCompat.checkSelfPermission(this, NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
            }

            R.id.MI_Disconnect -> {
                if(p2pHandler.getGroupStatus()) {
                    disconnect()
                }
            }

            R.id.SI_AllPeersToggle -> {
                p2pHandler.getPeerVisibility().let {
                    p2pHandler.setPeersVisibility(!it)
                    item.isChecked = !it
                }

                // Refresh the peers recycler view
                peersRV.adapter = PeersAdapter(mostRecentPeers, p2pHandler.getPeerVisibility(), this)
            }

            else -> {
                throw Exception("Unexpected Menu Selection")
            }
        }

        return true
    }


    /**
     * callback from p2pListener with list of found peers
     * @param devices instance of [WifiP2pDeviceList] containing nearby peers
     */
    fun onPeersAvailable(devices: WifiP2pDeviceList) {
        Logger.d("called")

        mostRecentPeers = deviceListToListOfPeers(devices)

        // Display the required peers in the list
        peersRV.adapter = PeersAdapter(mostRecentPeers, p2pHandler.getPeerVisibility(), this)

    }

    /**
     * callback from [PeersAdapter] indicating the user has selected a peer to connect to
     * @param peer Instance of [WifiP2pDevice] denoting a nearby device
     */
    override fun peerConnectRequest(peer: WifiP2pPeer) {
        Logger.d("called | Connecting To: ${peer.device.deviceName}")

        if(peer.device.isGroupOwner)
            p2pHandler.connect(peer)
        else{
            Toast.makeText(this, "This User is not a Group Owner", Toast.LENGTH_SHORT).show()
        }
    }



    /**
     * Connection info about the current group available
     * @param info Information to be displayed
     */
    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        Logger.d("called | $info")
        if (info != null) {
            Logger.d("Connected to ${info.groupOwnerAddress}")
            hostInfo = info

            // Update the UI to reflect the device's user type
            if(info.groupFormed) {
                DataListener(this).execute()

                isGroupOwner = info.isGroupOwner

                updateInterface(info, true)
            } else {
                isGroupOwner = false
                updateInterface(null, false)
            }
        }
    }

    private fun updateInterface(info: WifiP2pInfo?, connectedToGroup: Boolean) {
        Logger.d("called")

        if(connectedToGroup) {
            peersRV.visibility = View.GONE
            connectionDetailsCL.visibility = View.VISIBLE

            if (isGroupOwner) {
                userTypeTV.text = "Hosting Group"
            } else {
                userTypeTV.text = "Connected to: ${info!!.groupOwnerAddress}"
            }
        } else {
            peersRV.visibility = View.VISIBLE
            connectionDetailsCL.visibility = View.GONE
        }
    }

    /**
     *
     */
    fun downloadComplete(received: String) {

        receivedTV.text = received

        if(keepDownloading)
            DataListener(this).execute()
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

}