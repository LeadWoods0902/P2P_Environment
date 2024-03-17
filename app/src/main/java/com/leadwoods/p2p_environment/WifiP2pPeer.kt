package com.leadwoods.p2p_environment

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import com.leadwoods.p2p_environment.support.Logger

/**
 * Wrapper for instances of [WifiP2pDevice] with additional flags
 * @param device the WifiP2pDevice detected by the [P2PConnectionsListener]
 * @param isGroupOwner Boolean Flag for status of being a group owner
 * @param isconnected Boolean Flag for connection status between user's device and [device]
 */
class WifiP2pPeer(
    val device: WifiP2pDevice,
    val isGroupOwner: Boolean = false,
    val isconnected: Boolean = false
)

//fun tagPeersOwnerStatus(peers: List<WifiP2pPeer>): List<WifiP2pPeer>{
//
//    return mutableListOf<WifiP2pPeer>().apply {
//        for(peer in peers) {
//            if(peer.isGroupOwner)
//                add(WifiP2pPeer(peer.device, true))
//            else
//                add(WifiP2pPeer(peer.device, false))
//        }
//    }
//}

fun deviceListToListOfPeers(peers: WifiP2pDeviceList? = null, group: WifiP2pGroup? = null): List<WifiP2pPeer> {
    if((peers != null && group != null)
        || (peers == null && group == null)){
        Logger.w("Unintended use scenario")
        return listOf()
    }

    return mutableListOf<WifiP2pPeer>().apply {
        if(group == null)
            for (device in peers!!.deviceList) {
                add(WifiP2pPeer(device))
            }
        else
            for (device in group.clientList) {
                add(WifiP2pPeer(device))
            }
    }
}