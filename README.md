# High-Performance UUIDv7 for Kotlin/Android

This repository demonstrates the optimization of **UUIDv7** generation on the JVM (specifically tuned for Android). It documents the evolution from a standard implementation to a "zero-allocation" generator that is **~17x faster** than `java.util.UUID`.

## 🚀 The Results

Benchmarks were run on Android (Pixel/Emulator) using Jetpack Benchmark.

### vs Native Java UUID (Summary)

| Implementation | Time (Total) | Allocations | Speedup |
| :--- | :--- | :--- | :--- |
| **Legere UUIDv7** | **129 ns** | **2** | **~17.4x Faster** |
| `java.util.UUID` (v4) | 2,249 ns | 22 | 1x (Baseline) |

*Note: "Total" includes both generating the ID and converting it to a formatted String.*

### Detailed Breakdown

| Benchmark Case | Time (ns) | Allocations | Notes |
| :--- | :--- | :--- | :--- |
| `UUIDv7.generate()` | **50.0 ns** | **0** | Raw generation (Longs only) |
| `UUIDv7.toString()` | **77.0 ns** | **2** | Custom char buffer formatting |
| `UUID.randomUUID()` | ~300-500 ns | 1+ | Native generation cost |
| `UUID.toString()` | 674 ns | 19-20 | Native string formatting cost |

---

## 🧬 The Evolution of Optimization

This repository includes 6 variations (`r0` through `r4`/final) showing the step-by-step optimization process.

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

### 5. `r4` - The "Zero Cost" Implementation (Final)
*   **Change:**
    *   **Merged Atomics:** Combined `timestamp` and `sequence` into a single `AtomicLong` to perform one CAS (Compare-and-Swap) operation instead of two.
    *   **MSB Storage:** Stores the data in the AtomicLong *already formatted* as the UUID's Most Significant Bits, removing bit-shift overhead during read.
    *   **Hoisted RNG:** Calls `ThreadLocalRandom.current()` exactly once per generation.
    *   **Branch Prediction:** Optimized the monotonic check to favor the "hot path" (same millisecond).
*   **Performance:** **50.0 ns** (0 Allocations)
*   **Result:** Hit the theoretical limit of JVM performance.

---

## 🛠 Technical Highlights

### 1. Fast String Formatting
Standard `UUID.toString()` is surprisingly slow on Android. We implemented a custom `fastUuidString` that fills a pre-allocated `CharArray`, avoiding the overhead of `String.format`.


### 2. The "Single CAS" Lock
To ensure monotonicity (increasing sort order) without locking, we pack the timestamp and sequence into a single 64-bit Atomic.


### 3. Correctness vs Speed
The final implementation handles the edge case of **Sequence Overflow** (generating >4096 IDs in a single millisecond). If the sequence rolls over (`0xFFF`), we artificially increment the timestamp to preserve sort order, ensuring strict monotonicity even under heavy load.

## 📦 How to Run

1. Clone the repo.
2. Open in Android Studio.
3. Run the benchmarks in `UUIDBenchmark.kt` using the Android Gradle Plugin benchmark runner:
```bash
./gradlew :benchmark:connectedCheck
