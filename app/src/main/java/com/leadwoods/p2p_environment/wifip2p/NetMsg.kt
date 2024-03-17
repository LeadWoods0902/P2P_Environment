package com.leadwoods.p2p_environment.wifip2p

import java.io.Serializable

enum class MessageType {
    RAW_TEXT,
    GSON,
    MAGE,
    AR,
}

class NetMsg(
    val messageType: MessageType,
    val message: ByteArray
): Serializable