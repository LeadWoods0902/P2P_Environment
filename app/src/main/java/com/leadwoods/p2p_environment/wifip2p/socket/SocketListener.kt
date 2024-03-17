package com.leadwoods.p2p_environment.wifip2p.socket

import android.os.AsyncTask
import android.os.Handler
import com.leadwoods.p2p_environment.support.Logger
import com.leadwoods.p2p_environment.wifip2p.p2p_handler.WifiP2PHandler.Companion.GROUP_PORT
import java.io.DataInputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.min

class SocketListener(
    private val handler: Handler
): AsyncTask<Void, String, Unit>() {

    private lateinit var serverSocket: ServerSocket
    private lateinit var clientSocket: Socket
    private lateinit var stream: DataInputStream

    private lateinit var received: String


    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Void?) {
        try{
            serverSocket = ServerSocket(GROUP_PORT)
            clientSocket = serverSocket.accept()

            Logger.d("Socket opened on port: ${serverSocket.localPort}\nClient: ${if(clientSocket.isConnected) "Connected" else "Not Connected"}")

            received = stream.bufferedReader().use { it.readText() }


            Logger.i("received: ${received.subSequence(0, min(20, received.length))}, total length: ${received.length}")

            clientSocket.close()
            serverSocket.close()

        } catch (e: Exception) {
            Logger.e("Error handling socket listening: ${e.message}")
        } finally {

            // close the stream
            try {
                stream.close()
            } catch (e: Exception){
                Logger.e("${e.message}")
            }

            // close the client socket
            try {
                clientSocket.close()
            } catch (e: Exception){
                Logger.e("${e.message}")
            }

            //close the server socket
            try {
                serverSocket.close()
            } catch (e: Exception){
                Logger.e("${e.message}")
            }

        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)
        handler.obtainMessage(0x400 + 1, -1, -1, received).sendToTarget()
    }

    companion object {

        private var instance: SocketListener? = null

        fun getInstance(handler: Handler): SocketListener {
            return instance ?: synchronized(this) {
                instance ?: SocketListener(handler).also { instance = it }
            }
        }
    }
}