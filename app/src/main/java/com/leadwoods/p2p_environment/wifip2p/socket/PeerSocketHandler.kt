//package com.leadwoods.p2p_environment.wifip2p.socket
//
//import android.os.Handler
//import com.leadwoods.p2p_environment.support.Logger
//import com.leadwoods.p2p_environment.wifip2p.p2p_handler.WifiP2PHandler.Companion.GROUP_PORT
//import java.net.InetAddress
//import java.net.InetSocketAddress
//import java.net.Socket
//
//class PeerSocketHandler(private val handler: Handler, private val hostAddress: InetAddress): Thread() {
//
//    private val socketTimeout: Int = 5000
//
//    private lateinit var socket: Socket
//
//    override fun run() {
//        Logger.f()
//
////        socket = Socket()
//
//        try{
////            socket.bind(null)
////            Logger.i("Client Socket: Opened")
////
////            socket.connect(InetSocketAddress(hostAddress, WifiP2PHandler.GROUP_PORT), socketTimeout)
////            Logger.i("Client Socket: ${if(socket.isConnected) "Connected" else "Not Connected"}")
//
////            val socketWriter = SocketReaderWriter(socket, handler)
////            Thread(socketWriter).start()
//
//            val socketListener = SocketListener(handler)
//            socketListener.execute()
//        } catch (e: Exception){
//            Logger.e("Error while launching socket handler: ${e.message}")
//            try{
//                socket.close()
//                Logger.i("Client Socket: Closed")
//            } catch (e: Exception){
//                Logger.e("Error while closing socket: ${e.message}")
//            }
//        }
//    }
//
//    override fun interrupt() {
//        Logger.f()
//        super.interrupt()
//
//        try{
//            if(::socket.isInitialized && !socket.isClosed) {
//                socket.close()
//                Logger.i("Client Socket: Closed")
//            }
//        } catch (e: Exception){
//            Logger.e("Error while closing socket: ${e.message}")
//        }
//    }
//}
//
//class ClientSocketThread(private val handler: Handler, private val hostAddress: InetAddress): Thread(){
//
//    private var socket = Socket()
//
//
//    override fun run() {
//        try {
//            socket.connect(InetSocketAddress(hostAddress, GROUP_PORT), 5000)
//
//
//
//
//
//
//        } catch (e: Exception){
//            Logger.e("Error processing IO: ${e.message}")
//        }
//    }
//}