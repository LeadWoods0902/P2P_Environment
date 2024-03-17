//package com.leadwoods.p2p_environment.networking
//
//import android.os.AsyncTask
//import com.leadwoods.p2p_environment.activities.MainActivity
//import com.leadwoods.p2p_environment.activities.SecondaryActivity
//import com.leadwoods.p2p_environment.support.Logger
//import java.io.DataInputStream
//import java.net.ServerSocket
//import java.net.Socket
//import kotlin.math.min
//
//
//class DataListener(
//    private val callback: SecondaryActivity
//): AsyncTask<Void, String, Unit>() {
//    private val serverSocket: ServerSocket? = null
//    private val clientSocket: Socket? = null
//    private val stream: DataInputStream? = null
//
//    private var received = ""
//
//
//    override fun doInBackground(vararg params: Void?) {
//        try {
//            val socket = ServerSocket(8988)
//            val client = socket.accept()
//            Logger.d("Socket opened on port: ${socket.localPort}\nClient: ${if(client.isConnected) "Connected" else "Not Connected"}")
//
//            val stream = DataInputStream(client.getInputStream())
//            Logger.d("data: ${if(received.isNotEmpty()) "received" else "not received"}")
//
//            received = stream.bufferedReader().use { it.readText() }
//
//            Logger.e("${received.subSequence(0, min(20, received.length))}, total length: ${received.length}")
//
//            socket.close()
//
//        } catch (e: Exception){
//            Logger.e("Error downloading: ${e.message}")
//        } finally {
//
//            // close the stream
//            if(stream != null){
//                try {
//                    stream.close()
//                } catch (e: Exception){
//                    Logger.e("${e.message}")
//                }
//            }
//
//            // close the client socket
//            if(clientSocket != null){
//                try {
//                    clientSocket.close()
//                } catch (e: Exception){
//                    Logger.e("${e.message}")
//                }
//            }
//
//            //close the server socket
//            if(serverSocket != null){
//                try {
//                    serverSocket.close()
//                } catch (e: Exception){
//                    Logger.e("${e.message}")
//                }
//            }
//        }
//    }
//
//    override fun onPostExecute(result: Unit?) {
//        super.onPostExecute(result)
//        callback.downloadComplete(received)
//    }
//}
//
//
////class DataDownloadTask : AsyncTask<Void, Void, ByteArray>() {
////
////    override fun doInBackground(vararg params: Void?): ByteArray? {
////        // Connect to the socket and download data
////        val data = connectAndDownloadData()
////        return data
////    }
////
////    override fun onPostExecute(result: ByteArray?) {
////        // Handle downloaded data
////        if (result != null) {
////            processData(result)
////        }
////
////        // Restart the AsyncTask if needed
////        if (/* Check if more data needs to be downloaded */) {
////            execute()
////        }
////    }
////
////    private fun connectAndDownloadData(): ByteArray? {
////        // Implement socket connection and data download logic here
////        return null
////    }
////
////    private fun processData(data: ByteArray) {
////        // Implement logic to process downloaded data here
////    }
////}
