# High-Performance UUIDv7 for Kotlin/Android

This repository demonstrates the optimization of **UUIDv7** generation on the JVM (specifically tuned for Android). It documents the evolution from a standard implementation to a "zero-allocation" generator that is **~17x faster** than `java.util.UUID`.

## 🚀 The Results

Benchmarks were run on Android (Pixel 10 Pro) using Jetpack Benchmark.

### vs Native Java UUID (Summary)

| Implementation | Time (Total) | Allocations | Speedup           |
| :--- |:-------------|:------------|:------------------|
| **Legere UUIDv7** | **92 ns**    | **1**       | **~10.6x Faster** |
| `java.util.UUID` (v4) | 981 ns     | 22          | 1x (Baseline)     |

*Note: "Total" includes both generating the ID and converting it to a formatted String.*

### Detailed Breakdown

| Benchmark Case | Time (ns)   | Allocations | Notes |
| :--- |:------------|:------------| :--- |
| `UUIDv7.generate()` | **50.0 ns** | **0**       | Raw generation (Longs only) |
| `UUIDv7.toString()` | **50.0 ns** | **1**       | Custom char buffer formatting |
| `UUID.randomUUID()` | ~300-500 ns | 1+          | Native generation cost |
| `UUID.toString()` | 674 ns      | 19-20       | Native string formatting cost |

---

## 🧬 The Evolution of Optimization

This repository includes 6 variations (`r0` through `r5`/final) showing the step-by-step optimization process.

### 1. `r0` - The Baseline
*   **Approach:** Port of a standard Kotlin UUIDv7 implementation.
*   **Performance:** ~327ns
*   **Bottleneck:** Uses `SecureRandom` (expensive synchronization/OS calls) and standard byte-array manipulation.

### 2. `r01` - The RNG Swap
*   **Change:** Switched from `SecureRandom` to `ThreadLocalRandom`.
*   **Performance:** ~95ns
*   **Result:** Massive speedup. Proves that the entropy source is the single largest cost in UUID generation.

### 3. `r1` - The "Bad" Refactor
*   **Change:** Attempted to introduce a lambda-based generator while still using byte arrays for entropy.
*   **Performance:** ~594ns
*   **Result:** Slower. Demonstrated the cost of mixed abstractions and allocation overheads (3 allocations per call).

### 4. `r2` - Direct Write
*   **Change:** Removed all intermediate `ByteArray`s. Writes random bits directly to `Long` variables.
*   **Performance:** ~54ns
*   **Result:** Near-zero allocation for the generation phase.

### 5. `r4` - The "Zero Cost" Implementation
*   **Change:**
    *   **Merged Atomics:** Combined `timestamp` and `sequence` into a single `AtomicLong` to perform one CAS (Compare-and-Swap) operation instead of two.
    *   **MSB Storage:** Stores the data in the AtomicLong *already formatted* as the UUID's Most Significant Bits, removing bit-shift overhead during read.
    *   **Hoisted RNG:** Calls `ThreadLocalRandom.current()` exactly once per generation.
    *   **Branch Prediction:** Optimized the monotonic check to favor the "hot path" (same millisecond).
*   **Performance:** **50.0 ns** (0 Allocations)

### 6. `r5` - The randomUUIDString improvement (Final)
*   **Change:**
    *   **LocalStorage:** Moved the buffer in randomUUIDString to LocalStorage
*   **Performance:** **50.0 ns** (1 Allocations) for the String conversion
*   **Result:** Hit the theoretical limit of JVM performance.

---

## 🛠 Technical Highlights

### 1. Fast String Formatting
Standard `UUID.toString()` is surprisingly slow on Android. We implemented a custom `fastUuidString` that fills a pre-allocated `CharArray`, avoiding the overhead of `String.format`.


### 2. The "Single CAS" Lock
To ensure monotonicity (increasing sort order) without locking, we pack the timestamp and sequence into a single 64-bit Atomic.


### 3. Correctness vs Speed
The final implementation handles the edge case of **Sequence Overflow** (generating >4096 IDs in a single millisecond). If the sequence rolls over (`0xFFF`), we artificially increment the timestamp to preserve sort order, ensuring strict monotonicity even under heavy load.

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
