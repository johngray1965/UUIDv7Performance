package io.legere.uuidv7
/**
 *     DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *            Version 2, December 2004
 *
 *  Copyright (C) 2024 0xShamil
 *
 *  Everyone is permitted to copy and distribute verbatim or modified
 *  copies of this license document, and changing it is allowed as long
 *  as the name is changed.
 *
 *             DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *    TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *   0. You just DO WHAT THE FUCK YOU WANT TO.
 */

import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
/**
 * Interface to abstract the source of randomness.
 */

interface RandomProvider {
    fun nextLong(): Long

    fun nextInt(bound: Int): Int
}

/**
 * Default provider using [ThreadLocalRandom] for maximum performance.
 * Suitable for database IDs and general unique identifiers.
 */
object FastRandomProvider : RandomProvider {
    override fun nextLong(): Long = ThreadLocalRandom.current().nextLong()

    override fun nextInt(bound: Int): Int = ThreadLocalRandom.current().nextInt(bound)
}

/**
 * Provider using [SecureRandom] wrapped in [ThreadLocal] to avoid contention.
 * Suitable for security-sensitive identifiers (e.g., session tokens, password resets).
 */
class SecureRandomProvider : RandomProvider {
    private val generator = ThreadLocal.withInitial { SecureRandom() }

    override fun nextLong(): Long = generator.get().nextLong()

    override fun nextInt(bound: Int): Int = generator.get().nextInt(bound)
}

object UUIDv7 {
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

    var randomProvider: RandomProvider = FastRandomProvider

    // Shared monotonic states
    private val lastMillis = AtomicLong(Long.MIN_VALUE)
    private val lastSeq12 = AtomicInteger(-1)

    /**
     * @return A UUID object representing a UUIDv7 value.
     */
    fun randomUUID(): UUID = generate { msb, lsb -> UUID(msb, lsb) }
    fun randomUUIDString(): String = generate { msb, lsb -> fastUuidString(msb, lsb) }

    /**
     * Generates the most significant bits (MSB) and least significant bits (LSB) for a UUIDv7
     * and passes them to the provided [block].
     *
     * MSB Layout:
     * - 48 bits: Timestamp
     * -  4 bits: Version (7)
     * - 12 bits: Sequence
     *
     * LSB Layout:
     * -  2 bits: Variant (2)
     * - 62 bits: Random
     *
     * @param block A function that accepts the MSB and LSB and returns a result of type [T].
     * @return The result returned by [block].
     */
    private inline fun <T> generate(block: (Long, Long) -> T): T {
        val now = System.currentTimeMillis()
        val prev = lastMillis.get()
        val ts = if (now >= prev) now else prev // clamp to avoid regressions
        val seq = nextSeq(ts)

        // High 64 bits:
        // 48 bits timestamp
        // 4 bits version (7)
        // 12 bits sequence (high part of random)
        val msb = (ts shl 16) or 0x7000L or seq.toLong()

        // Low 64 bits:
        // 2 bits variant (2)
        // 62 bits random
        val randomLow = randomProvider.nextLong()
        val lsb = (randomLow and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE

        return block(msb, lsb)
    }

    /**
     * Returns the 12-bit monotonic sequence for the given millisecond:
     * - If [ts] differs from the last seen timestamp, seed with a random 12-bit value.
     * - If [ts] matches, increment modulo 4096 to preserve order for same-ms calls.
     *
     * Uses CAS on [lastMillis]/[lastSeq12] to remain lock-light under contention.
     */
    private fun nextSeq(ts: Long): Int {
        while (true) {
            val lastTs = lastMillis.get()
            val lastSeq = lastSeq12.get()

            if (ts != lastTs) {
                // New millisecond: randomize the starting point to retain entropy
                val seeded = randomProvider.nextInt(1 shl 12)
                if (lastMillis.compareAndSet(lastTs, ts)) {
                    lastSeq12.set(seeded)
                    return seeded
                }
            } else {
                // Same millisecond: increment and wrap in 12 bits
                val next = (lastSeq + 1) and 0x0FFF
                if (lastSeq12.compareAndSet(lastSeq, next)) {
                    return next
                }
            }
            // If either CAS fails, retry with fresh reads
        }
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
