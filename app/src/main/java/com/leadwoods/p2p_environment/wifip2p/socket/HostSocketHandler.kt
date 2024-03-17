//package com.leadwoods.p2p_environment.wifip2p.socket
//
//import android.os.Handler
//import com.leadwoods.p2p_environment.support.Logger
//
//class HostSocketHandler(private val handler: Handler) : Thread() {
//
////    private lateinit var serverSocket: ServerSocket
//
////    private val threadCount = 10
//
////    private val pool: ThreadPoolExecutor = ThreadPoolExecutor(
////        threadCount, threadCount, 10, TimeUnit.SECONDS,
////        LinkedBlockingQueue()
////    )
//
////    init {
////        Logger.f()
////        try {
////            serverSocket = ServerSocket(GROUP_PORT)
////        } catch (e: Exception){
////            Logger.e("Error while creating socket: ${e.message}")
////            pool.shutdown()
////            throw e
////        }
////    }
//
//    override fun run() {
//        Logger.f()
//        super.run()
//
//        while(true) {
//            try{
////                pool.execute(SocketReaderWriter(serverSocket.accept(), handler))
////                Logger.i("Server Socket: Opened")
//
//                val socketListener = SocketListener(handler)
//                socketListener.execute()
//
//            } catch (e: Exception) {
//                Logger.e("Error launcher the socket handler: ${e.message}")
//
////                try {
////                    if(!serverSocket.isClosed) {
////                        serverSocket.close()
////                        Logger.i("Server Socket: Closed")
////                    }
////                } catch (e: Exception){
////                    Logger.e("Error while closing socket: ${e.message}")
////                }
////                pool.shutdown()
//                break
//            }
//        }
//    }
//
//    override fun interrupt() {
//        Logger.f()
//        super.interrupt()
//
////        try {
////            if(!serverSocket.isClosed) {
////                serverSocket.close()
////                Logger.i("Server Socket: Closed")
////            }
////        } catch (e: Exception) {
////            Logger.e("Error while closing socket: ${e.message}")
////        }
//    }
//}
