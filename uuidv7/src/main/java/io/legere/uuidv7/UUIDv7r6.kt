/*
 * Copyright (c) 2025.  Legere. All rights reserved.
 */

package io.legere.uuidv7

import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong

object UUIDv7r6 {
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

    private val threadLocalBuffer =
        object : ThreadLocal<CharArray>() {
            override fun initialValue() = CharArray(36)
        }

    /**
     * @return A UUID object representing a UUIDv7 value.
     */
    fun randomUUID(): UUID = generate { msb, lsb -> UUID(msb, lsb) }

    fun randomUUIDString(): String = generate { msb, lsb -> fastUuidString(msb, lsb) }

    private inline fun <T> generate(block: (Long, Long) -> T): T {
        // 1. Get the RandomProvider (ThreadLocalRandom) once
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

            val nextSeq =
                if (needsRandom) {
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
        val lsb = (randomLow ushr 2) or Long.MIN_VALUE
        return block(msb, lsb)
    }

    fun fastUuidString(
        msb: Long,
        lsb: Long
    ): String {
        val buf = threadLocalBuffer.get()!!
        val digits = HEX_DIGITS

        // Split 64-bit longs into 32-bit ints to avoid 64-bit shifts and repeated toInt() calls
        val msbHi = (msb ushr 32).toInt()
        val msbLo = msb.toInt()
        val lsbHi = (lsb ushr 32).toInt()
        val lsbLo = lsb.toInt()

        buf[0] = digits[(msbHi ushr 28) and 0xF]
        buf[1] = digits[(msbHi ushr 24) and 0xF]
        buf[2] = digits[(msbHi ushr 20) and 0xF]
        buf[3] = digits[(msbHi ushr 16) and 0xF]
        buf[4] = digits[(msbHi ushr 12) and 0xF]
        buf[5] = digits[(msbHi ushr 8) and 0xF]
        buf[6] = digits[(msbHi ushr 4) and 0xF]
        buf[7] = digits[msbHi and 0xF]

        buf[8] = '-'

        buf[9] = digits[(msbLo ushr 28) and 0xF]
        buf[10] = digits[(msbLo ushr 24) and 0xF]
        buf[11] = digits[(msbLo ushr 20) and 0xF]
        buf[12] = digits[(msbLo ushr 16) and 0xF]

        buf[13] = '-'

        buf[14] = digits[(msbLo ushr 12) and 0xF]
        buf[15] = digits[(msbLo ushr 8) and 0xF]
        buf[16] = digits[(msbLo ushr 4) and 0xF]
        buf[17] = digits[msbLo and 0xF]

        buf[18] = '-'

        buf[19] = digits[(lsbHi ushr 28) and 0xF]
        buf[20] = digits[(lsbHi ushr 24) and 0xF]
        buf[21] = digits[(lsbHi ushr 20) and 0xF]
        buf[22] = digits[(lsbHi ushr 16) and 0xF]

        buf[23] = '-'

        buf[24] = digits[(lsbHi ushr 12) and 0xF]
        buf[25] = digits[(lsbHi ushr 8) and 0xF]
        buf[26] = digits[(lsbHi ushr 4) and 0xF]
        buf[27] = digits[lsbHi and 0xF]

        buf[28] = digits[(lsbLo ushr 28) and 0xF]
        buf[29] = digits[(lsbLo ushr 24) and 0xF]
        buf[30] = digits[(lsbLo ushr 20) and 0xF]
        buf[31] = digits[(lsbLo ushr 16) and 0xF]
        buf[32] = digits[(lsbLo ushr 12) and 0xF]
        buf[33] = digits[(lsbLo ushr 8) and 0xF]
        buf[34] = digits[(lsbLo ushr 4) and 0xF]
        buf[35] = digits[lsbLo and 0xF]

        return String(buf)
    }
}
