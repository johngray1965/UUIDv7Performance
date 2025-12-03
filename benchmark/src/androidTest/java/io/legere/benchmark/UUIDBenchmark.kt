/*
 * Copyright (c) 2025.  Legere. All rights reserved.
 */

package io.legere.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.legere.utils.UUIDv7r4
import io.legere.uuidv7.UUIDv7
import io.legere.uuidv7.UUIDv7r0
import io.legere.uuidv7.UUIDv7r01
import io.legere.uuidv7.UUIDv7r2
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
    fun benchmarkUUIDv7r4() {
        benchmarkRule.measureRepeated {
            UUIDv7r4.randomUUID()
        }
    }

    @Test
    fun benchmarkUUIDv7() {
        benchmarkRule.measureRepeated {
            UUIDv7.randomUUID()
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
            UUIDv7.fastUuidString(msb, lsb)
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
