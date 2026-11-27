package pl.sinclair.terrelremote.protocol

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import pl.sinclair.terrelremote.model.AcMode
import pl.sinclair.terrelremote.model.AcState
import pl.sinclair.terrelremote.model.DiscoveredAcDevice
import pl.sinclair.terrelremote.model.FanSpeed
import pl.sinclair.terrelremote.model.HorizontalSwing
import pl.sinclair.terrelremote.model.SavedAcDevice
import pl.sinclair.terrelremote.model.VerticalSwing
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException

class GreeWifiClient(private val context: Context) {

    companion object {
        private const val GREE_PORT = 7000
        private val STATUS_COLS = listOf(
            "Pow", "Mod", "SetTem", "WdSpd", "Blo", "Health",
            "SwhSlp", "Lig", "SwingLfRig", "SwUpDn", "Quiet",
            "Tur", "StHt", "SvSt", "TemSen"
        )
    }

    suspend fun discoverDevices(manualIp: String? = null): List<DiscoveredAcDevice> =
        withContext(Dispatchers.IO) {
            val found = LinkedHashMap<String, DiscoveredAcDevice>()
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val multicastLock = wifiManager?.createMulticastLock("sinclair_gree_scan")
            try {
                multicastLock?.setReferenceCounted(false)
                multicastLock?.acquire()

                val scanBytes = "{\"t\":\"scan\"}".toByteArray(Charsets.UTF_8)
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.soTimeout = 700

                    val targets = LinkedHashSet<InetAddress>()
                    if (!manualIp.isNullOrBlank()) {
                        runCatching { targets.add(InetAddress.getByName(manualIp.trim())) }
                    } else {
                        runCatching { targets.add(InetAddress.getByName("255.255.255.255")) }
                        getLocalBroadcastAddresses().forEach { targets.add(it) }
                    }

                    // Send twice to reduce UDP packet loss on Wi-Fi
                    repeat(2) {
                        for (addr in targets) {
                            runCatching {
                                socket.send(DatagramPacket(scanBytes, scanBytes.size, addr, GREE_PORT))
                            }
                        }
                    }

                    val deadline = System.currentTimeMillis() + 2600L
                    val buf = ByteArray(4096)
                    while (System.currentTimeMillis() < deadline) {
                        val packet = DatagramPacket(buf, buf.size)
                        try {
                            socket.receive(packet)
                            val raw = String(packet.data, 0, packet.length, Charsets.UTF_8)
                            parseScanResponse(raw, packet.address.hostAddress ?: "")?.let { dev ->
                                found[dev.mac] = dev
                            }
                        } catch (_: SocketTimeoutException) {
                            // Continue waiting until deadline
                        }
                    }
                }
            } finally {
                if (multicastLock?.isHeld == true) {
                    runCatching { multicastLock.release() }
                }
            }
            found.values.toList()
        }

    suspend fun bindDevice(device: DiscoveredAcDevice): SavedAcDevice = withContext(Dispatchers.IO) {
        val attempts = listOf(device.useGcm, !device.useGcm)
        var lastError: Exception? = null

        for (tryGcm in attempts) {
            try {
                val genericKey = if (tryGcm) GreeCrypto.GENERIC_KEY_GCM else GreeCrypto.GENERIC_KEY_ECB
                val inner = JSONObject().apply {
                    put("mac", device.mac)
                    put("t", "bind")
                    put("uid", 0)
                }
                val enc = GreeCrypto.encrypt(inner.toString(), genericKey, tryGcm)
                val outer = JSONObject().apply {
                    put("cid", "app")
                    put("i", 1)
                    put("pack", enc.pack)
                    if (tryGcm && enc.tag != null) {
                        put("tag", enc.tag)
                    }
                    put("t", "pack")
                    put("tcid", device.mac)
                    put("uid", 0)
                }

                val responseRaw = sendUdpRequest(device.ip, outer.toString())
                val respOuter = JSONObject(responseRaw)
                val respPack = respOuter.getString("pack")
                val respTag = respOuter.optString("tag", "")
                val decrypted = GreeCrypto.decrypt(
                    packBase64 = respPack,
                    key = genericKey,
                    useGcm = tryGcm && respTag.isNotEmpty(),
                    tagBase64 = respTag
                )
                val bindJson = JSONObject(decrypted)
                val key = bindJson.optString("key", "")
                if (key.isNotEmpty()) {
                    return@withContext SavedAcDevice(
                        ip = device.ip,
                        mac = device.mac,
                        name = device.name,
                        key = key,
                        useGcm = tryGcm
                    )
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Nie udało się sparować klimatyzatora (${device.ip})")
    }

    suspend fun fetchStatus(device: SavedAcDevice, current: AcState): AcState =
        withContext(Dispatchers.IO) {
            val inner = JSONObject().apply {
                put("cols", JSONArray(STATUS_COLS))
                put("mac", device.mac)
                put("t", "status")
            }
            val decrypted = sendEncryptedCommand(device, inner)
            val json = JSONObject(decrypted)
            val cols = json.optJSONArray("cols") ?: return@withContext current
            val dat = json.optJSONArray("dat") ?: return@withContext current

            val map = HashMap<String, Int>()
            for (i in 0 until minOf(cols.length(), dat.length())) {
                map[cols.getString(i)] = dat.optInt(i, 0)
            }

            val rawTempSensor = map["TemSen"]
            val roomTemp = when {
                rawTempSensor == null || rawTempSensor == 0 -> current.roomTemperature
                rawTempSensor > 40 -> rawTempSensor - 40
                else -> rawTempSensor
            }

            current.copy(
                power = (map["Pow"] ?: if (current.power) 1 else 0) == 1,
                mode = AcMode.fromGreeCode(map["Mod"] ?: current.mode.greeCode),
                temperature = (map["SetTem"] ?: current.temperature).coerceIn(16, 30),
                roomTemperature = roomTemp,
                fanSpeed = FanSpeed.fromGreeCode(map["WdSpd"] ?: current.fanSpeed.greeCode),
                xFan = (map["Blo"] ?: if (current.xFan) 1 else 0) == 1,
                health = (map["Health"] ?: if (current.health) 1 else 0) == 1,
                sleep = (map["SwhSlp"] ?: if (current.sleep) 1 else 0) == 1,
                light = (map["Lig"] ?: if (current.light) 1 else 0) == 1,
                horizontalSwing = HorizontalSwing.fromGreeCode(
                    map["SwingLfRig"] ?: current.horizontalSwing.greeCode
                ),
                verticalSwing = VerticalSwing.fromGreeCode(
                    map["SwUpDn"] ?: current.verticalSwing.greeCode
                ),
                quiet = (map["Quiet"] ?: if (current.quiet) 2 else 0) > 0,
                turbo = (map["Tur"] ?: if (current.turbo) 1 else 0) == 1,
                heating8C = (map["StHt"] ?: if (current.heating8C) 1 else 0) == 1,
                eco = (map["SvSt"] ?: if (current.eco) 1 else 0) == 1
            )
        }

    suspend fun sendCommand(device: SavedAcDevice, params: Map<String, Int>) =
        withContext(Dispatchers.IO) {
            val optArray = JSONArray()
            val pArray = JSONArray()
            params.forEach { (k, v) ->
                optArray.put(k)
                pArray.put(v)
            }
            val inner = JSONObject().apply {
                put("opt", optArray)
                put("p", pArray)
                put("t", "cmd")
            }
            sendEncryptedCommand(device, inner)
        }

    private fun sendEncryptedCommand(device: SavedAcDevice, innerJson: JSONObject): String {
        val enc = GreeCrypto.encrypt(innerJson.toString(), device.key, device.useGcm)
        val outer = JSONObject().apply {
            put("cid", "app")
            put("i", 0)
            put("pack", enc.pack)
            if (device.useGcm && enc.tag != null) {
                put("tag", enc.tag)
            }
            put("t", "pack")
            put("tcid", device.mac)
            put("uid", 0)
        }

        val responseRaw = sendUdpRequest(device.ip, outer.toString())
        val respOuter = JSONObject(responseRaw)
        val respPack = respOuter.getString("pack")
        val respTag = respOuter.optString("tag", "")
        return GreeCrypto.decrypt(
            packBase64 = respPack,
            key = device.key,
            useGcm = device.useGcm && respTag.isNotEmpty(),
            tagBase64 = respTag
        )
    }

    private fun sendUdpRequest(ip: String, payload: String): String {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        val address = InetAddress.getByName(ip)
        var lastException: Exception? = null

        repeat(2) {
            try {
                DatagramSocket().use { socket ->
                    socket.soTimeout = 2200
                    socket.send(DatagramPacket(bytes, bytes.size, address, GREE_PORT))
                    val buf = ByteArray(4096)
                    val resp = DatagramPacket(buf, buf.size)
                    socket.receive(resp)
                    return String(resp.data, 0, resp.length, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw lastException ?: SocketTimeoutException("Brak odpowiedzi z klimatyzatora ($ip)")
    }

    private fun parseScanResponse(rawJson: String, ip: String): DiscoveredAcDevice? {
        return runCatching {
            val outer = JSONObject(rawJson)
            if (!outer.has("pack")) return null
            val pack = outer.getString("pack")
            val tag = outer.optString("tag", "")
            val useGcm = tag.isNotEmpty()
            val key = if (useGcm) GreeCrypto.GENERIC_KEY_GCM else GreeCrypto.GENERIC_KEY_ECB
            val decrypted = GreeCrypto.decrypt(pack, key, useGcm, tag)
            val info = JSONObject(decrypted)
            val mac = info.optString("mac", outer.optString("cid", ""))
            if (mac.isEmpty()) return null
            val rawName = info.optString("name", "").ifBlank { "Sinclair Terrel" }
            val ver = info.optString("ver", "")
            DiscoveredAcDevice(
                ip = ip,
                mac = mac,
                name = rawName,
                version = ver,
                useGcm = useGcm
            )
        }.getOrNull()
    }

    private fun getLocalBroadcastAddresses(): List<InetAddress> {
        val result = mutableListOf<InetAddress>()
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return result
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                for (ifaceAddr in iface.interfaceAddresses) {
                    val addr = ifaceAddr.address
                    val bcast = ifaceAddr.broadcast
                    if (addr is Inet4Address && bcast != null) {
                        result.add(bcast)
                    }
                }
            }
        }
        return result
    }
}
