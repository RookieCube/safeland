package com.safeland.chat.network

import com.safeland.chat.model.Packet
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.nio.charset.Charset

/**
 * UDP网络管理器
 * 负责UDP端口1338的数据收发
 */
class UdpManager(
    private val port: Int = 1338,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {
        private const val BUFFER_SIZE = 65535
        private const val BROADCAST_ADDRESS = "255.255.255.255"
    }

    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null

    // 接收到的原始数据包流
    private val _incomingPackets = MutableSharedFlow<Pair<Packet, String>>(extraBufferCapacity = 100)
    val incomingPackets: SharedFlow<Pair<Packet, String>> = _incomingPackets.asSharedFlow()

    // 发送队列
    private val sendChannel = Channel<Triple<Packet, String, Int>>(capacity = 1000)

    // 运行状态
    private var isRunning = false

    /**
     * 启动UDP管理器
     */
    fun start() {
        if (isRunning) return
        isRunning = true

        scope.launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket(port).apply {
                    broadcast = true
                    reuseAddress = true
                }

                // 启动接收协程
                startReceiving()

                // 启动发送协程
                startSending()

            } catch (e: SocketException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 停止UDP管理器
     */
    fun stop() {
        isRunning = false
        receiveJob?.cancel()
        receiveJob = null
        socket?.close()
        socket = null
    }

    /**
     * 发送数据包到指定地址
     */
    suspend fun sendPacket(packet: Packet, targetIp: String, targetPort: Int = port) {
        sendChannel.send(Triple(packet, targetIp, targetPort))
    }

    /**
     * 广播数据包
     */
    suspend fun broadcastPacket(packet: Packet) {
        sendChannel.send(Triple(packet, BROADCAST_ADDRESS, port))
    }

    /**
     * 批量发送数据包（用于噪声扩散）
     */
    suspend fun sendBatch(packets: List<Packet>, targetIp: String, targetPort: Int = port) {
        packets.forEach { packet ->
            sendChannel.send(Triple(packet, targetIp, targetPort))
        }
    }

    /**
     * 启动接收循环
     */
    private fun startReceiving() {
        receiveJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(BUFFER_SIZE)

            while (isRunning && socket != null && !socket!!.isClosed) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    socket?.receive(receivePacket)

                    val data = String(receivePacket.data, 0, receivePacket.length, Charset.forName("UTF-8"))
                    val senderIp = receivePacket.address.hostAddress

                    // 解析数据包
                    val packet = Packet.parse(data)
                    if (packet != null) {
                        _incomingPackets.tryEmit(packet to senderIp)
                    }

                } catch (e: Exception) {
                    if (isRunning) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * 启动发送循环
     */
    private fun startSending() {
        scope.launch(Dispatchers.IO) {
            while (isRunning) {
                try {
                    val (packet, targetIp, targetPort) = sendChannel.receive()

                    val data = packet.serialize().toByteArray(Charset.forName("UTF-8"))
                    val address = InetAddress.getByName(targetIp)
                    val sendPacket = DatagramPacket(data, data.size, address, targetPort)

                    socket?.send(sendPacket)

                } catch (e: Exception) {
                    if (isRunning) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * 获取本地IP地址
     */
    fun getLocalIpAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }

    /**
     * 检查是否在运行
     */
    fun isActive(): Boolean = isRunning && socket?.isClosed == false
}
