/*
 * Copyright (c) 2025.  Legere. All rights reserved.
 */

package io.legere.uuidv7

import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong

object UUIDv7r4 {
    private val HEX_DIGITS =
        charArrayOf(
            '0',
            '1',
            '2',
            '3',
            '4',
            '5',
            '6',
            '7',
            '8',
            '9',
            'a',
            'b',
            'c',
            'd',
            'e',
            'f'
        )

    // Shared monotonic state
    // Stores the 48-bit timestamp and 12-bit sequence in a single 64-bit Long.
    // Layout: [Timestamp (52 bits allowed, 48 used)] [Sequence (12 bits)]
    private val state = AtomicLong(0L)

    /**
     * @return A UUID object representing a UUIDv7 value.
     */
    fun randomUUID(): UUID = generate { msb, lsb -> UUID(msb, lsb) }

    fun randomUUIDString(): String = generate { msb, lsb -> fastUuidString(msb, lsb) }

    private inline fun <T> generate(block: (Long, Long) -> T): T {
        // 1. Get the RandomProvider (volatile read) once
        val provider = ThreadLocalRandom.current()
        val now = System.currentTimeMillis()

        var ts: Long
        var newState: Long


        // 2. Monotonic CAS Loop (Inlined to avoid Pair allocation)
        while (true) {
            val current = state.get()
            val lastTs = current ushr 16
            val lastSeq = current and 0xFFF

            // 1. Clamp timestamp (Monotonicity vs Clock Rollback)
            ts = if (now >= lastTs) now else lastTs

            // 2. Determine if we need a new random sequence
            //    True if: Time moved forward OR Sequence ran out (overflow)
            val sequenceOverflow = lastSeq == 0xFFFL
            val needsRandom = (ts != lastTs) || sequenceOverflow

            val nextSeq = if (needsRandom) {
                // If we are here purely due to overflow, we MUST push time forward
                // to preserve sort order.
                if (sequenceOverflow && ts == lastTs) {
                    ts++
                }

                // Optimization: nextInt(4096) is effectively (nextLong() & 0xFFF)
                // if the RNG is good, but nextInt handles the bound logic.
                provider.nextInt(4096).toLong()
            } else {
                lastSeq + 1
            }

            // 3. Pack directly into MSB format
            //    (48-bit Timestamp | 4-bit Version (7) | 12-bit Sequence)
            newState = (ts shl 16) or 0x7000L or nextSeq

            if (state.compareAndSet(current, newState)) {
                break
            }
            // If CAS fails, loop again
        }

        // 3. Construct High 64 bits
        // newState = (ts shl 16) or 0x7000L or nextSeqU
        val msb = newState

        // 4. Construct Low 64 bits
        // 2 bits variant (2) | 62 bits random
        // Fast path optimization to avoid interface dispatch
        val randomLow = provider.nextLong()
        val lsb = (randomLow and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE

        return block(msb, lsb)
    }

    fun fastUuidString(
        msb: Long,
        lsb: Long
    ): String {
        val buf = CharArray(36)

        buf[0] = HEX_DIGITS[((msb ushr 60) and 0xF).toInt()]
        buf[1] = HEX_DIGITS[((msb ushr 56) and 0xF).toInt()]
        buf[2] = HEX_DIGITS[((msb ushr 52) and 0xF).toInt()]
        buf[3] = HEX_DIGITS[((msb ushr 48) and 0xF).toInt()]
        buf[4] = HEX_DIGITS[((msb ushr 44) and 0xF).toInt()]
        buf[5] = HEX_DIGITS[((msb ushr 40) and 0xF).toInt()]
        buf[6] = HEX_DIGITS[((msb ushr 36) and 0xF).toInt()]
        buf[7] = HEX_DIGITS[((msb ushr 32) and 0xF).toInt()]
        buf[8] = '-'
        buf[9] = HEX_DIGITS[((msb ushr 28) and 0xF).toInt()]
        buf[10] = HEX_DIGITS[((msb ushr 24) and 0xF).toInt()]
        buf[11] = HEX_DIGITS[((msb ushr 20) and 0xF).toInt()]
        buf[12] = HEX_DIGITS[((msb ushr 16) and 0xF).toInt()]
        buf[13] = '-'
        buf[14] = HEX_DIGITS[((msb ushr 12) and 0xF).toInt()]
        buf[15] = HEX_DIGITS[((msb ushr 8) and 0xF).toInt()]
        buf[16] = HEX_DIGITS[((msb ushr 4) and 0xF).toInt()]
        buf[17] = HEX_DIGITS[(msb and 0xF).toInt()]
        buf[18] = '-'
        buf[19] = HEX_DIGITS[((lsb ushr 60) and 0xF).toInt()]
        buf[20] = HEX_DIGITS[((lsb ushr 56) and 0xF).toInt()]
        buf[21] = HEX_DIGITS[((lsb ushr 52) and 0xF).toInt()]
        buf[22] = HEX_DIGITS[((lsb ushr 48) and 0xF).toInt()]
        buf[23] = '-'
        buf[24] = HEX_DIGITS[((lsb ushr 44) and 0xF).toInt()]
        buf[25] = HEX_DIGITS[((lsb ushr 40) and 0xF).toInt()]
        buf[26] = HEX_DIGITS[((lsb ushr 36) and 0xF).toInt()]
        buf[27] = HEX_DIGITS[((lsb ushr 32) and 0xF).toInt()]
        buf[28] = HEX_DIGITS[((lsb ushr 28) and 0xF).toInt()]
        buf[29] = HEX_DIGITS[((lsb ushr 24) and 0xF).toInt()]
        buf[30] = HEX_DIGITS[((lsb ushr 20) and 0xF).toInt()]
        buf[31] = HEX_DIGITS[((lsb ushr 16) and 0xF).toInt()]
        buf[32] = HEX_DIGITS[((lsb ushr 12) and 0xF).toInt()]
        buf[33] = HEX_DIGITS[((lsb ushr 8) and 0xF).toInt()]
        buf[34] = HEX_DIGITS[((lsb ushr 4) and 0xF).toInt()]
        buf[35] = HEX_DIGITS[(lsb and 0xF).toInt()]
        return String(buf)
    }
}
