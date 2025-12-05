# High-Performance, Zero-Allocation UUIDv7 for Kotlin/Android

This repository documents the deep optimization of a **UUIDv7** generator for Kotlin, achieving a **~12x performance increase** over the standard `java.util.UUID` on Android. It serves as a practical case study in low-level performance tuning on the ART runtime.

Where we started:

| Implementation | Time (Total) | Speedup | 
| :--- |:-------------|:------------------| 
| UUIDv7 (Initial) | ~330 ns | Slower | 
| java.util.UUID (v4) | 981 ns | 1x (Baseline) |
## 🚀 The Results

Benchmarks were run on a Pixel 10 Pro using Jetpack Microbenchmark, which accounts for JIT/AOT warm-up and provides stable, reliable metrics.

### vs. Native Java UUID (Summary)

| Implementation | Time (Total) | Allocations | Speedup |
| :--- |:-------------|:------------|:------------------|
| **UUIDv7** | **82.0 ns** | **1** | **~11.9x Faster** |
| `java.util.UUID` (v4) | 981 ns | ~20 | 1x (Baseline) |

*Note: "Total" includes both generating the ID and converting it to a formatted String. The single allocation in our implementation is the final `String` object itself.*

### Detailed Breakdown

| Benchmark Case | Time (ns) | Allocations | Notes |
| :--- |:------------|:------------| :--- |
| `UUIDv7.generate()` | **34.2 ns** | **0** | Raw generation (primitive `Long`s only) |
| `UUIDv7.toString()` | **47.8 ns** | **1** | Custom char buffer formatting |
| `UUID.randomUUID()` | ~300 ns | 1+ | Native generation cost |
| `UUID.toString()` | ~675 ns | ~7-20 | Native string formatting cost |

---

## 🧬 The Optimization Journey

The performance gains were achieved through a series of methodical, profile-driven optimizations.

### 1. The Entropy Source: `SecureRandom` vs. `ThreadLocalRandom`
*   **Problem:** The baseline implementation used `SecureRandom`, which is cryptographically secure but slow due to synchronization and OS-level entropy gathering. Profiling showed this was the single largest bottleneck.
*   **Solution:** Switched to `ThreadLocalRandom`. For generating unique IDs (not security tokens), its statistical randomness is more than sufficient and orders of magnitude faster.
*   **Result:** Performance jumped from **~330ns** to **~95ns**.

### 2. The Allocation Trap: Eliminating Intermediate Objects
*   **Problem:** Early versions created `ByteArray`, `Pair`, and per-call `CharArray` buffers. Each allocation adds GC pressure and slows down execution in a tight loop.
*   **Solution:**
    1.  Refactored the logic to work directly on primitive `Long`s, eliminating the `ByteArray` and `Pair`.
    2.  Used a `ThreadLocal<CharArray>` for the string formatting buffer, making the `toString()` call effectively zero-allocation (amortized).
*   **Result:** This brought the generation time down to its final state, limited only by the raw cost of the operations themselves.

### 3. The Concurrency Problem: Two Atomics vs. One
*   **Problem:** Managing `lastTimestamp` and `sequenceNumber` with two separate `Atomic` variables required two volatile reads and complex CAS (Compare-and-Swap) logic, which showed up as significant overhead in the profiler.
*   **Solution:** Packed the 48-bit timestamp and 12-bit sequence into a **single 64-bit `AtomicLong`**. This guarantees atomicity and reduces the synchronization logic to a single, efficient CAS loop.

### 4. The Final Squeeze: `Long` vs. `Int` Arithmetic
*   **Problem:** The string formatting involved 32 bit-shift and `toInt()` operations on 64-bit `Long`s.
*   **Solution:** Split the two 64-bit `Long`s (`msb`, `lsb`) into four 32-bit `Int`s at the start of the function. The subsequent operations on `Int`s are slightly faster on the 32/64-bit ART runtime.
*   **Result:** Shaved off the final few nanoseconds, bringing the formatting time down to a remarkable **~48ns**.

---

## 🛠 Technical Highlights

### 1. Why `fastUuidString` is Critical on Android
The standard `java.util.UUID.toString()` is surprisingly slow on Android because it is often implemented in pure Java, not as a native intrinsic. A single call can create **7 or more temporary objects** (5 strings for the parts, a `StringBuilder`, and the final `String`), leading to high allocation counts and GC pressure. Our custom implementation uses a pre-allocated buffer and direct bit manipulation, creating **zero temporary objects**.

### 2. Correct Monotonicity
The final implementation correctly handles the edge case of **Sequence Overflow** (generating >4096 IDs in a single millisecond). If the sequence rolls over (`0xFFF`), we artificially increment the timestamp to preserve the strict sort order required by the UUIDv7 spec.

## 📦 How to Run the Benchmarks

The benchmark suite is configured to run on a connected Android device. Due to the complexities of file permissions on modern Android versions (Scoped Storage), running the benchmarks from within Android Studio is the most reliable method.

### Running from Android Studio (Recommended)

1.  **Open the Project:** Open the project in a recent version of Android Studio.
2.  **Locate the Benchmark File:** Navigate to the test file:
    `benchmark/src/androidTest/java/io/legere/benchmark/UUIDBenchmark.kt`.
3.  **Run the Benchmarks:** You will see green "play" icons in the gutter next to the class definition and each individual test method (`@Test`).

    *   To run **all** benchmarks, click the play icon next to the `class UUIDBenchmark` line and select "Run 'UUIDBenchmark'".
    *   To run a **specific** benchmark, click the play icon next to that `@Test` function (e.g., `benchmarkUUIDv7String`).

    ![Run from Gutter](https://user-images.githubusercontent.com/1392632/194429210-2d6a54a9-17d4-4927-8671-502a1e360249.png)

4.  **Select a Device:** Choose your connected device from the dropdown menu and run the configuration.
5.  **View Results:** The benchmark results will appear in the **Run** tool window within Android Studio. It provides a clear, digestible summary of the timings and allocation counts for each test.

This approach bypasses the command-line file-pulling issues and gives you immediate, actionable results directly in the IDE.
