/*
 * Copyright (c) 2025.  Legere. All rights reserved.
 */

package io.legere.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.legere.uuidv7.UUIDv7r0
import io.legere.uuidv7.UUIDv7r01
import io.legere.uuidv7.UUIDv7r2
import io.legere.uuidv7.UUIDv7r3
import io.legere.uuidv7.UUIDv7r4
import io.legere.uuidv7.UUIDv7r5
import io.legere.uuidv7.UUIDv7r6
import java.util.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UUIDBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmarkUUIDv7r0() {
        benchmarkRule.measureRepeated {
            UUIDv7r0.randomUUID()
        }
    }

    @Test
    fun benchmarkUUIDv7r01() {
        benchmarkRule.measureRepeated {
            UUIDv7r01.randomUUID()
        }
    }

    @Test
    fun benchmarkUUIDv7r1() {
        benchmarkRule.measureRepeated {
            UUIDv7r1.randomUUID()
        }
    }

    @Test
    fun benchmarkUUIDv7r2() {
        benchmarkRule.measureRepeated {
            UUIDv7r2.randomUUID()
        }
    }

    @Test
    fun benchmarkUUIDv7r3() {
        benchmarkRule.measureRepeated {
            UUIDv7r3.randomUUID()
        }
    }

    @Test
    fun benchmarkUUIDv7r4() {
        benchmarkRule.measureRepeated {
            UUIDv7r4.randomUUID()
        }
    }

    @Test
    fun benchmarkUUIDv7r5() {
        benchmarkRule.measureRepeated {
            UUIDv7r5.randomUUID()
        }
    }

    @Test
    fun benchmarkUUIDv7r6() {
        benchmarkRule.measureRepeated {
            UUIDv7r6.randomUUID()
        }
    }

    @Test
    fun benchmarkUUIDv4() {
        benchmarkRule.measureRepeated {
            UUID.randomUUID()
        }
    }

    @Test
    fun benchmarkFormattingOnly_Custom() {
        val uuid = UUID.randomUUID()
        val msb = uuid.mostSignificantBits
        val lsb = uuid.leastSignificantBits

        benchmarkRule.measureRepeated {
            UUIDv7r4.fastUuidString(msb, lsb)
        }
    }

    @Test
    fun benchmarkFormattingOnly_ThreadLocalBuffer() {
        val uuid = UUID.randomUUID()
        val msb = uuid.mostSignificantBits
        val lsb = uuid.leastSignificantBits

        benchmarkRule.measureRepeated {
            UUIDv7r5.fastUuidString(msb, lsb)
        }
    }

    @Test
    fun benchmarkFormattingOnlyr6() {
        val uuid = UUID.randomUUID()
        val msb = uuid.mostSignificantBits
        val lsb = uuid.leastSignificantBits

        benchmarkRule.measureRepeated {
            UUIDv7r6.fastUuidString(msb, lsb)
        }
    }

    @Test
    fun benchmarkR6String() {

        benchmarkRule.measureRepeated {
            UUIDv7r6.randomUUIDString()
        }
    }

    @Test
    fun benchmarkFormattingOnly_Native() {
        val uuid = UUID.randomUUID()

        benchmarkRule.measureRepeated {
            uuid.toString()
        }
    }
}
