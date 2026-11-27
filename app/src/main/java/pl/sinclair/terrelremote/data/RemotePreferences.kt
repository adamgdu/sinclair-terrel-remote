package pl.sinclair.terrelremote.data

import android.content.Context
import pl.sinclair.terrelremote.model.SavedAcDevice
import pl.sinclair.terrelremote.model.TransportMode

class RemotePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("sinclair_terrel_prefs", Context.MODE_PRIVATE)

    fun saveDevice(device: SavedAcDevice) {
        prefs.edit()
            .putString("ip", device.ip)
            .putString("mac", device.mac)
            .putString("name", device.name)
            .putString("key", device.key)
            .putBoolean("use_gcm", device.useGcm)
            .apply()
    }

    fun loadDevice(): SavedAcDevice? {
        val ip = prefs.getString("ip", null) ?: return null
        val mac = prefs.getString("mac", null) ?: return null
        val key = prefs.getString("key", null) ?: return null
        val name = prefs.getString("name", "Sinclair Terrel") ?: "Sinclair Terrel"
        val useGcm = prefs.getBoolean("use_gcm", false)
        return SavedAcDevice(ip = ip, mac = mac, name = name, key = key, useGcm = useGcm)
    }

    fun clearDevice() {
        prefs.edit()
            .remove("ip")
            .remove("mac")
            .remove("name")
            .remove("key")
            .remove("use_gcm")
            .apply()
    }

    fun saveTransportMode(mode: TransportMode) {
        prefs.edit().putString("transport", mode.name).apply()
    }

    fun loadTransportMode(): TransportMode {
        val raw = prefs.getString("transport", TransportMode.WIFI.name)
        return runCatching { TransportMode.valueOf(raw ?: TransportMode.WIFI.name) }
            .getOrDefault(TransportMode.WIFI)
    }
}
