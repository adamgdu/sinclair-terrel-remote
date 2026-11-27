package pl.sinclair.terrelremote.model

enum class TransportMode {
    WIFI,
    IR
}

enum class AcMode(val greeCode: Int, val labelPl: String, val shortLabel: String) {
    AUTO(0, "Auto", "AUTO"),
    COOL(1, "Chłodzenie", "COOL"),
    DRY(2, "Osuszanie", "DRY"),
    FAN(3, "Wentylacja", "FAN"),
    HEAT(4, "Grzanie", "HEAT");

    fun next(): AcMode {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun fromGreeCode(code: Int): AcMode = entries.firstOrNull { it.greeCode == code } ?: AUTO
    }
}

enum class FanSpeed(val greeCode: Int, val irCode: Int, val labelPl: String, val bars: Int) {
    AUTO(0, 0, "Auto", 0),
    LOW(1, 1, "Bieg 1", 1),
    MED_LOW(2, 1, "Bieg 2", 2),
    MEDIUM(3, 2, "Bieg 3", 3),
    MED_HIGH(4, 2, "Bieg 4", 4),
    HIGH(5, 3, "Bieg 5", 5);

    fun next(): FanSpeed {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun fromGreeCode(code: Int): FanSpeed = entries.firstOrNull { it.greeCode == code } ?: AUTO
    }
}

enum class VerticalSwing(val greeCode: Int, val labelPl: String, val shortLabel: String) {
    OFF(0, "Wyłączona", "WYŁ"),
    FULL(1, "Pełny ruch", "AUTO"),
    POS_1(2, "Pozycja 1 (Góra)", "POZ 1"),
    POS_2(3, "Pozycja 2", "POZ 2"),
    POS_3(4, "Pozycja 3 (Środek)", "POZ 3"),
    POS_4(5, "Pozycja 4", "POZ 4"),
    POS_5(6, "Pozycja 5 (Dół)", "POZ 5");

    fun next(): VerticalSwing {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun fromGreeCode(code: Int): VerticalSwing =
            entries.firstOrNull { it.greeCode == code } ?: if (code > 6) FULL else OFF
    }
}

enum class HorizontalSwing(val greeCode: Int, val labelPl: String, val shortLabel: String) {
    OFF(0, "Wyłączona", "WYŁ"),
    FULL(1, "Pełny ruch", "AUTO"),
    LEFT(2, "Skrajnie lewo", "LEWO"),
    MID_LEFT(3, "Lekko w lewo", "ŚR-L"),
    CENTER(4, "Środek", "ŚRODEK"),
    MID_RIGHT(5, "Lekko w prawo", "ŚR-P"),
    RIGHT(6, "Skrajnie prawo", "PRAWO");

    fun next(): HorizontalSwing {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun fromGreeCode(code: Int): HorizontalSwing =
            entries.firstOrNull { it.greeCode == code } ?: if (code > 6) FULL else OFF
    }
}

data class AcState(
    val power: Boolean = false,
    val mode: AcMode = AcMode.COOL,
    val temperature: Int = 22,
    val roomTemperature: Int? = null,
    val fanSpeed: FanSpeed = FanSpeed.AUTO,
    val turbo: Boolean = false,
    val quiet: Boolean = false,
    val verticalSwing: VerticalSwing = VerticalSwing.OFF,
    val horizontalSwing: HorizontalSwing = HorizontalSwing.OFF,
    val light: Boolean = true,
    val xFan: Boolean = false,
    val health: Boolean = false,
    val sleep: Boolean = false,
    val heating8C: Boolean = false,
    val eco: Boolean = false
)

data class DiscoveredAcDevice(
    val ip: String,
    val mac: String,
    val name: String,
    val version: String,
    val useGcm: Boolean
)

data class SavedAcDevice(
    val ip: String,
    val mac: String,
    val name: String,
    val key: String,
    val useGcm: Boolean
)
