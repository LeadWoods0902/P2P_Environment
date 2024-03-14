package com.leadwoods.p2p_environment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.leadwoods.p2p_environment.support.Logger

/**
 * Recycler Adapter for the P2P peers
 * Call empty function to adapt empty list
 * @param peers List of [WifiP2pPeer] objects to adapt to the RV
 * @param filtered Boolean denoting whether to filter out non- Group Owners
 * @param touchInterface callback interface for buttons on the adapted CVs
 */
class PeersAdapter(
    private val peers: List<WifiP2pPeer> = listOf(),
    private val filtered: Boolean = false,
    private val touchInterface: PeerTouchInterface? = null
) : RecyclerView.Adapter<PeersAdapter.Companion.PeerHolder>() {

    interface PeerTouchInterface{
        fun peerConnectRequest(peer: WifiP2pPeer)
    }

    companion object{
        class PeerHolder(v: View): RecyclerView.ViewHolder(v){
            val peerNameTV: TextView = v.findViewById(R.id.TV_PeerName)
            val peerAddressTV: TextView = v.findViewById(R.id.TV_PeerAddress)
            val peerConnectB: Button = v.findViewById(R.id.B_PeerConnet)
        }
    }

    private lateinit var taggedPeers: List<WifiP2pPeer>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerHolder {
        Logger.d("called")
        return PeerHolder(LayoutInflater.from(parent.context).inflate(R.layout.card_peer, parent, false))
    }

    override fun getItemCount(): Int {
        Logger.d("called")
        return if (filtered) {
            peers.filter { it.isGroupOwner }.size
        } else {
            peers.size
        }
    }

    override fun onBindViewHolder(holder: PeerHolder, position: Int) {
        Logger.d("called")

        val peer = peers[position].device

        holder.peerNameTV.text = peer.deviceName
        holder.peerAddressTV.text = peer.deviceAddress

        holder.peerConnectB.setOnClickListener {
            touchInterface?.peerConnectRequest(peers[position])
        }
    }


}
