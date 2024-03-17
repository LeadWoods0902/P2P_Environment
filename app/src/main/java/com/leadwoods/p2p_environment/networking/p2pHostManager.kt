//package com.leadwoods.p2p_environment.networking
//
//import android.net.wifi.p2p.WifiP2pDevice
//import com.leadwoods.p2p_environment.WifiP2pPeer
//
//class P2pHostHandler: P2pBaseHandler() {
//
//    val myInfo = hostInfo
//
//    private lateinit var peers: ArrayList<WifiP2pPeer>
//
//    fun addPeerAddress(device: WifiP2pDevice){
//        peers.add(
//            WifiP2pPeer(
//            device = device,
//            isGroupOwner = false,
//            isconnected = true)
//        )
//    }
//
//    fun removePeerAddress(addressesToKeep: List<String>){
//        for(peer in peers){
//            if(!(addressesToKeep.contains(peer.device.deviceAddress))){
//                peers.remove(peer)
//            }
//        }
//    }
//}