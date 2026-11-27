package pl.sinclair.terrelremote.protocol

import android.content.Context
import android.hardware.ConsumerIrManager
import pl.sinclair.terrelremote.model.AcState
import pl.sinclair.terrelremote.model.HorizontalSwing
import pl.sinclair.terrelremote.model.VerticalSwing

class GreeIrTransmitter(context: Context) {

    private val irManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    companion object {
        private const val CARRIER_FREQ_HZ = 38000
        private const val HEADER_MARK = 9000
        private const val HEADER_SPACE = 4500
        private const val BIT_MARK = 620
        private const val ONE_SPACE = 1660
        private const val ZERO_SPACE = 540
        private const val MSG_SPACE = 19980
        private const val END_SPACE = 40000
    }

    fun hasIrBlaster(): Boolean = runCatching {
        irManager?.hasIrEmitter() == true
    }.getOrDefault(false)

    fun sendState(state: AcState): Boolean {
        val manager = irManager ?: return false
        if (!hasIrBlaster()) return false

        val bytes = encodeGreeFrame(state)
        val pattern = buildTimingPattern(bytes)
        return runCatching {
            manager.transmit(CARRIER_FREQ_HZ, pattern)
            true
        }.getOrDefault(false)
    }

    /**
     * Encodes the 8-byte Gree / Sinclair Terrel (YAC1FB9) IR protocol frame.
     */
    private fun encodeGreeFrame(state: AcState): IntArray {
        val b = IntArray(8)

        val swingAutoBit =
            if (state.verticalSwing == VerticalSwing.FULL || state.horizontalSwing == HorizontalSwing.FULL) 1 else 0

        // Byte 0: [Mode:0..2][Power:3][Fan:4..5][SwingAuto:6][Sleep:7]
        b[0] = (state.mode.greeCode and 0x07) or
            ((if (state.power) 1 else 0) shl 3) or
            ((state.fanSpeed.irCode and 0x03) shl 4) or
            (swingAutoBit shl 6) or
            ((if (state.sleep) 1 else 0) shl 7)

        // Byte 1: [Temp-16:0..3][Timer:4..7]
        val tempOffset = (state.temperature.coerceIn(16, 30) - 16) and 0x0F
        b[1] = tempOffset

        // Byte 2: [Timer:0..3][Turbo:4][Light:5][Health/Model:6][XFan:7]
        b[2] = ((if (state.turbo) 1 else 0) shl 4) or
            ((if (state.light) 1 else 0) shl 5) or
            ((if (state.health) 1 else 0) shl 6) or
            ((if (state.xFan) 1 else 0) shl 7)

        // Byte 3: Fixed Gree protocol header bits (0x50)
        b[3] = 0x50

        // Byte 4: [VerticalSwing:0..3][HorizontalSwing:4..7]
        b[4] = (state.verticalSwing.greeCode and 0x0F) or
            ((state.horizontalSwing.greeCode and 0x0F) shl 4)

        // Byte 5: Standard YAW1F / YAC1FB9 flag (0x20)
        b[5] = 0x20

        // Byte 6: Reserved / Extended
        b[6] = 0x00

        // Byte 7: Lower nibble flags (0x00), Upper nibble = 4-bit Gree checksum
        val sum = (
            (b[0] and 0x0F) +
                (b[1] and 0x0F) +
                (b[2] and 0x0F) +
                (b[3] and 0x0F) +
                ((b[4] ushr 4) and 0x0F) +
                ((b[5] ushr 4) and 0x0F) +
                ((b[6] ushr 4) and 0x0F) +
                10
            ) and 0x0F
        b[7] = (sum shl 4)

        return b
    }

    private fun buildTimingPattern(bytes: IntArray): IntArray {
        val pulses = ArrayList<Int>(150)

        // Header
        pulses.add(HEADER_MARK)
        pulses.add(HEADER_SPACE)

        // Block 1: Bytes 0..3 (32 bits, LSB first)
        for (i in 0..3) {
            appendByteLsb(pulses, bytes[i])
        }

        // 3-bit Gree block footer: 0, 1, 0 (0b010 LSB first)
        appendBit(pulses, 0)
        appendBit(pulses, 1)
        appendBit(pulses, 0)

        // Inter-block gap (~20 ms)
        pulses.add(BIT_MARK)
        pulses.add(MSG_SPACE)

        // Block 2: Bytes 4..7 (32 bits, LSB first)
        for (i in 4..7) {
            appendByteLsb(pulses, bytes[i])
        }

        // Stop mark + trailing space
        pulses.add(BIT_MARK)
        pulses.add(END_SPACE)

        return pulses.toIntArray()
    }

    private fun appendByteLsb(pulses: MutableList<Int>, value: Int) {
        for (bit in 0..7) {
            appendBit(pulses, (value ushr bit) and 1)
        }
    }

    private fun appendBit(pulses: MutableList<Int>, bit: Int) {
        pulses.add(BIT_MARK)
        pulses.add(if (bit == 1) ONE_SPACE else ZERO_SPACE)
    }
}
