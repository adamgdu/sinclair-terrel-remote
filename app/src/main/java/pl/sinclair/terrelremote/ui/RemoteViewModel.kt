package pl.sinclair.terrelremote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.sinclair.terrelremote.data.RemotePreferences
import pl.sinclair.terrelremote.model.AcMode
import pl.sinclair.terrelremote.model.AcState
import pl.sinclair.terrelremote.model.DiscoveredAcDevice
import pl.sinclair.terrelremote.model.SavedAcDevice
import pl.sinclair.terrelremote.model.TransportMode
import pl.sinclair.terrelremote.protocol.GreeIrTransmitter
import pl.sinclair.terrelremote.protocol.GreeWifiClient

data class RemoteUiState(
    val acState: AcState = AcState(),
    val transportMode: TransportMode = TransportMode.WIFI,
    val hasIrBlaster: Boolean = false,
    val savedDevice: SavedAcDevice? = null,
    val discoveredDevices: List<DiscoveredAcDevice> = emptyList(),
    val isScanning: Boolean = false,
    val isBusy: Boolean = false,
    val statusMessage: String = "Gotowy do pracy",
    val isError: Boolean = false,
    val showSetupDialog: Boolean = false
)

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = RemotePreferences(application)
    private val wifiClient = GreeWifiClient(application)
    private val irTransmitter = GreeIrTransmitter(application)

    private val _uiState = MutableStateFlow(
        RemoteUiState(
            transportMode = prefs.loadTransportMode(),
            hasIrBlaster = irTransmitter.hasIrBlaster(),
            savedDevice = prefs.loadDevice(),
            statusMessage = when {
                prefs.loadTransportMode() == TransportMode.IR && irTransmitter.hasIrBlaster() ->
                    "Tryb podczerwieni (IR) gotowy"
                prefs.loadDevice() != null ->
                    "Połączono: ${prefs.loadDevice()?.name} (${prefs.loadDevice()?.ip})"
                else ->
                    "Kliknij ikonę zębatki, aby wyszukać klimatyzator w Wi-Fi"
            }
        )
    )
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    init {
        if (_uiState.value.transportMode == TransportMode.WIFI && _uiState.value.savedDevice != null) {
            syncWifiStatus(silent = false)
        }
    }

    fun setTransportMode(mode: TransportMode) {
        prefs.saveTransportMode(mode)
        _uiState.update {
            it.copy(
                transportMode = mode,
                isError = false,
                statusMessage = when (mode) {
                    TransportMode.IR -> if (it.hasIrBlaster) {
                        "Tryb IR aktywny – wyceluj telefonem w klimatyzator"
                    } else {
                        "Ten telefon nie ma wbudowanej diody IR"
                    }
                    TransportMode.WIFI -> if (it.savedDevice != null) {
                        "Tryb Wi-Fi aktywny (${it.savedDevice.ip})"
                    } else {
                        "Tryb Wi-Fi – wyszukaj klimatyzator w ustawieniach"
                    }
                }
            )
        }
        if (mode == TransportMode.WIFI && _uiState.value.savedDevice != null) {
            syncWifiStatus(silent = true)
        }
    }

    fun openSetupDialog() {
        _uiState.update { it.copy(showSetupDialog = true) }
        scanWifiDevices()
    }

    fun closeSetupDialog() {
        _uiState.update { it.copy(showSetupDialog = false) }
    }

    fun scanWifiDevices(manualIp: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    isError = false,
                    statusMessage = if (manualIp.isNullOrBlank()) {
                        "Szukam Sinclair Terrel w sieci lokalnej..."
                    } else {
                        "Łączenie z $manualIp..."
                    }
                )
            }
            try {
                val devices = wifiClient.discoverDevices(manualIp)
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        discoveredDevices = devices,
                        statusMessage = if (devices.isEmpty()) {
                            "Nie znaleziono urządzeń (sprawdź Wi-Fi lub wpisz IP ręcznie)"
                        } else {
                            "Znaleziono urządzeń: ${devices.size}"
                        },
                        isError = devices.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        isError = true,
                        statusMessage = "Błąd skanowania: ${e.message ?: "brak sieci"}"
                    )
                }
            }
        }
    }

    fun pairDevice(device: DiscoveredAcDevice) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = "Parowanie z ${device.ip}...") }
            try {
                val saved = wifiClient.bindDevice(device)
                prefs.saveDevice(saved)
                _uiState.update {
                    it.copy(
                        savedDevice = saved,
                        transportMode = TransportMode.WIFI,
                        showSetupDialog = false,
                        isBusy = false,
                        isError = false,
                        statusMessage = "Sparowano z ${saved.name} (${saved.ip})"
                    )
                }
                prefs.saveTransportMode(TransportMode.WIFI)
                syncWifiStatus(silent = false)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isError = true,
                        statusMessage = "Błąd parowania: ${e.message ?: "spróbuj ponownie"}"
                    )
                }
            }
        }
    }

    fun forgetDevice() {
        prefs.clearDevice()
        _uiState.update {
            it.copy(
                savedDevice = null,
                statusMessage = "Usunięto zapisany klimatyzator",
                isError = false
            )
        }
    }

    fun syncOrResend() {
        if (_uiState.value.transportMode == TransportMode.WIFI) {
            syncWifiStatus(silent = false)
        } else {
            sendIrCurrent("Ponownie wysłano stan przez IR")
        }
    }

    fun syncWifiStatus(silent: Boolean = false) {
        val device = _uiState.value.savedDevice
        if (device == null) {
            if (!silent) {
                _uiState.update {
                    it.copy(
                        showSetupDialog = true,
                        statusMessage = "Najpierw wyszukaj i wybierz klimatyzator"
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isBusy = true, statusMessage = "Odczyt stanu z klimatyzatora...") }
            }
            try {
                val updated = wifiClient.fetchStatus(device, _uiState.value.acState)
                _uiState.update {
                    it.copy(
                        acState = updated,
                        isBusy = false,
                        isError = false,
                        statusMessage = if (updated.roomTemperature != null) {
                            "Zsynchronizowano • Pokój: ${updated.roomTemperature}°C"
                        } else {
                            "Zsynchronizowano po Wi-Fi"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isError = true,
                        statusMessage = "Brak odpowiedzi Wi-Fi (${device.ip}) – sprawdź sieć lub użyj IR"
                    )
                }
            }
        }
    }

    // --- Remote control actions ---

    fun togglePower() {
        val next = !_uiState.value.acState.power
        applyStateChange(
            transform = { it.copy(power = next) },
            wifiParams = mapOf("Pow" to if (next) 1 else 0),
            actionLabel = if (next) "Włączono klimatyzację" else "Wyłączono klimatyzację"
        )
    }

    fun cycleMode() {
        val nextMode = _uiState.value.acState.mode.next()
        applyStateChange(
            transform = {
                it.copy(
                    power = true,
                    mode = nextMode,
                    heating8C = if (nextMode != AcMode.HEAT) false else it.heating8C
                )
            },
            wifiParams = buildMap {
                put("Pow", 1)
                put("Mod", nextMode.greeCode)
                if (nextMode != AcMode.HEAT) put("StHt", 0)
            },
            actionLabel = "Tryb: ${nextMode.labelPl}"
        )
    }

    fun selectMode(mode: AcMode) {
        applyStateChange(
            transform = {
                it.copy(
                    power = true,
                    mode = mode,
                    heating8C = if (mode != AcMode.HEAT) false else it.heating8C
                )
            },
            wifiParams = buildMap {
                put("Pow", 1)
                put("Mod", mode.greeCode)
                if (mode != AcMode.HEAT) put("StHt", 0)
            },
            actionLabel = "Tryb: ${mode.labelPl}"
        )
    }

    fun changeTemperature(delta: Int) {
        val current = _uiState.value.acState
        val newTemp = (current.temperature + delta).coerceIn(16, 30)
        applyStateChange(
            transform = { it.copy(power = true, temperature = newTemp, heating8C = false) },
            wifiParams = mapOf("Pow" to 1, "SetTem" to newTemp, "TemUn" to 0, "StHt" to 0),
            actionLabel = "Temperatura: $newTemp°C"
        )
    }

    fun cycleFanSpeed() {
        val nextFan = _uiState.value.acState.fanSpeed.next()
        applyStateChange(
            transform = { it.copy(power = true, fanSpeed = nextFan, turbo = false, quiet = false) },
            wifiParams = mapOf("Pow" to 1, "WdSpd" to nextFan.greeCode, "Tur" to 0, "Quiet" to 0),
            actionLabel = "Wentylator: ${nextFan.labelPl}"
        )
    }

    fun toggleTurbo() {
        val next = !_uiState.value.acState.turbo
        applyStateChange(
            transform = { it.copy(power = true, turbo = next, quiet = if (next) false else it.quiet) },
            wifiParams = buildMap {
                put("Pow", 1)
                put("Tur", if (next) 1 else 0)
                if (next) put("Quiet", 0)
            },
            actionLabel = if (next) "Turbo WŁ" else "Turbo WYŁ"
        )
    }

    fun toggleQuiet() {
        val next = !_uiState.value.acState.quiet
        applyStateChange(
            transform = { it.copy(power = true, quiet = next, turbo = if (next) false else it.turbo) },
            wifiParams = buildMap {
                put("Pow", 1)
                put("Quiet", if (next) 2 else 0)
                if (next) put("Tur", 0)
            },
            actionLabel = if (next) "Tryb cichy (Quiet) WŁ" else "Tryb cichy WYŁ"
        )
    }

    fun cycleVerticalSwing() {
        val next = _uiState.value.acState.verticalSwing.next()
        applyStateChange(
            transform = { it.copy(verticalSwing = next) },
            wifiParams = mapOf("SwUpDn" to next.greeCode),
            actionLabel = "Żaluzja pionowa: ${next.labelPl}"
        )
    }

    fun cycleHorizontalSwing() {
        val next = _uiState.value.acState.horizontalSwing.next()
        applyStateChange(
            transform = { it.copy(horizontalSwing = next) },
            wifiParams = mapOf("SwingLfRig" to next.greeCode),
            actionLabel = "Żaluzja pozioma: ${next.labelPl}"
        )
    }

    fun toggleLight() {
        val next = !_uiState.value.acState.light
        applyStateChange(
            transform = { it.copy(light = next) },
            wifiParams = mapOf("Lig" to if (next) 1 else 0),
            actionLabel = if (next) "Podświetlenie LED WŁ" else "Podświetlenie LED WYŁ"
        )
    }

    fun toggleXFan() {
        val next = !_uiState.value.acState.xFan
        applyStateChange(
            transform = { it.copy(xFan = next) },
            wifiParams = mapOf("Blo" to if (next) 1 else 0),
            actionLabel = if (next) "X-FAN (osuszanie) WŁ" else "X-FAN WYŁ"
        )
    }

    fun toggleHealth() {
        val next = !_uiState.value.acState.health
        applyStateChange(
            transform = { it.copy(health = next) },
            wifiParams = mapOf("Health" to if (next) 1 else 0),
            actionLabel = if (next) "Jonizator Plasma WŁ" else "Jonizator Plasma WYŁ"
        )
    }

    fun toggleSleep() {
        val next = !_uiState.value.acState.sleep
        applyStateChange(
            transform = { it.copy(power = true, sleep = next) },
            wifiParams = mapOf("Pow" to 1, "SwhSlp" to if (next) 1 else 0),
            actionLabel = if (next) "Tryb nocny (Sleep) WŁ" else "Tryb nocny WYŁ"
        )
    }

    fun toggleHeating8C() {
        val next = !_uiState.value.acState.heating8C
        applyStateChange(
            transform = {
                if (next) {
                    it.copy(power = true, mode = AcMode.HEAT, heating8C = true, turbo = false)
                } else {
                    it.copy(heating8C = false)
                }
            },
            wifiParams = if (next) {
                mapOf("Pow" to 1, "Mod" to AcMode.HEAT.greeCode, "StHt" to 1, "Tur" to 0)
            } else {
                mapOf("StHt" to 0)
            },
            actionLabel = if (next) "Grzanie dyżurne 8°C WŁ" else "Grzanie 8°C WYŁ"
        )
    }

    private fun applyStateChange(
        transform: (AcState) -> AcState,
        wifiParams: Map<String, Int>,
        actionLabel: String
    ) {
        val newState = transform(_uiState.value.acState)
        _uiState.update { it.copy(acState = newState) }

        when (_uiState.value.transportMode) {
            TransportMode.IR -> sendIrCurrent(actionLabel)
            TransportMode.WIFI -> sendWifiParams(wifiParams, actionLabel)
        }
    }

    private fun sendIrCurrent(actionLabel: String) {
        val ok = irTransmitter.sendState(_uiState.value.acState)
        _uiState.update {
            it.copy(
                isError = !ok,
                statusMessage = if (ok) {
                    "IR wysłano • $actionLabel"
                } else {
                    "Brak diody IR w telefonie – przełącz na Wi-Fi"
                }
            )
        }
    }

    private fun sendWifiParams(params: Map<String, Int>, actionLabel: String) {
        val device = _uiState.value.savedDevice
        if (device == null) {
            _uiState.update {
                it.copy(
                    isError = true,
                    showSetupDialog = true,
                    statusMessage = "Najpierw sparuj klimatyzator w Wi-Fi"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                wifiClient.sendCommand(device, params)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isError = false,
                        statusMessage = "Wi-Fi OK • $actionLabel"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isError = true,
                        statusMessage = "Błąd Wi-Fi (${device.ip}) – przełącz na IR lub sprawdź sieć"
                    )
                }
            }
        }
    }
}
