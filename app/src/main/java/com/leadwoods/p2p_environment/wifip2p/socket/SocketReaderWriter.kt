//package com.leadwoods.p2p_environment.wifip2p.socket
//
//import android.os.Handler
//import com.leadwoods.p2p_environment.support.Logger
//import java.io.IOException
//import java.io.OutputStream
//import java.net.Socket
//import java.nio.ByteBuffer
//
//
//class SocketReaderWriter(private val socket: Socket, private val handler: Handler): Runnable{
//
//    private lateinit var oStream: OutputStream
//
//    override fun run() {
//        Logger.f()
//
//        try{
//            val iStream = socket.getInputStream()
//            oStream = socket.getOutputStream()
//
//            val messageSizeBuffer = ByteArray(Int.SIZE_BYTES)
//            var messageSize: Int
//            var buffer: ByteArray
//            var bytes: Int
//            var totalBytes: Int
//
//            handler.obtainMessage( 0x400 + 2, this).sendToTarget()
//
//            while (true) {
//                try{
//                    bytes = iStream.read(messageSizeBuffer)
//                    if(bytes == -1)
//                        break
//
//                    messageSize = ByteBuffer.wrap(messageSizeBuffer).getInt()
//
//                    buffer = ByteArray(messageSize)
//                    bytes = iStream.read(buffer)
//
//                    totalBytes = bytes
//
//                    while(bytes!= -1 && totalBytes < messageSize) {
//                        bytes = iStream.read(buffer, totalBytes, messageSize - totalBytes)
//                        totalBytes += bytes
//                    }
//                    if(bytes == -1)
//                        break
//
//                    handler.obtainMessage(0x400 + 1, bytes, -1, buffer).sendToTarget()
//                } catch (e: Exception) {
//                    handler.obtainMessage(0x400 + 3, this).sendToTarget()
//                    Logger.i("Communication Disconnected")
//                }
//            }
//        } catch (e: Exception) {
//            Logger.e("Error while handling socket: ${e.message}")
//        } finally {
//            try {
//                socket.close()
//            } catch (e: Exception) {
//                Logger.e("Error closing socket; ${e.message}")
//            }
//        }
//    }
//
//    fun writeToSocket(message: ByteArray) {
//        Logger.f()
//        try {
//            val sizeBuffer = ByteBuffer.allocate(Integer.SIZE / java.lang.Byte.SIZE)
//            val sizeArray = sizeBuffer.putInt(message.size).array()
//            val completeMessage = ByteArray(sizeArray.size + message.size)
//            System.arraycopy(sizeArray, 0, completeMessage, 0, sizeArray.size)
//            System.arraycopy(message, 0, completeMessage, sizeArray.size, message.size)
//            oStream.write(completeMessage)
//        } catch (e: IOException) {
//            Logger.e("Exception during write: ${e.message}")
//        }
//    }
//
//    fun write(message: ByteArray) {
//        Logger.f()
//        try{
//            oStream.write(message)
//        } catch (e: Exception) {
//            Logger.e("Error Writing Bites: ${e.message}")
//        }
//    }
//}