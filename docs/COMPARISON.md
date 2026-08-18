# Comparison: Valix vs. Hibernate Validator (JSR-380)

This document provides a technical comparison between **Valix** and **Hibernate Validator** (the reference implementation of Jakarta Bean Validation / JSR-380) to help developers understand the architectural differences and select the appropriate tool for their environment.

---

## 1. Architectural Comparison Matrix

| Feature | Valix | Hibernate Validator (JSR-380) |
| --- | --- | --- |
| **Primary Language** | Kotlin-First (multiplatform) | Java-Centric (JVM only) |
| **Execution Model** | KSP Codegen (Compile-Time) | Runtime Reflection |
| **Reflection Required** | No (for generated validators) | Yes (heavy use of reflection APIs) |
| **Kotlin Multiplatform (KMP)** | Yes (`commonMain` compatible) | No (restricted to JVM architectures) |
| **GraalVM Native Image** | Out-of-the-box, zero configuration | Requires reflection configuration registers |
| **Android Optimized** | Yes (no reflection startup penalty) | Heavy resource footprints on client devices |
| **Custom Constraints** | Yes (synchronous and suspending/async) | Yes (synchronous only) |
| **Schema Generation** | Built-in JSON Schema and OpenAPI 3.1 | Typically requires third-party plugins |

---

## 2. Key Differences Deep-Dive

### A. Execution Model (Reflection vs. Generated Code)
* **Hibernate Validator** inspects target data classes at runtime. It scans annotations, parses constraints, caches metadata, and invokes property getters using Java reflection. This model introduces startup overhead (metadata analysis) and execution latency on hot loops.
* **Valix** moves metadata resolution to the build phase. Using KSP, it generates plain Kotlin code containing direct condition evaluations (e.g. `if (value.age < 18)`). At runtime, validation executes as standard procedural calls, completely bypassing reflection caches.

### B. Platform Compatibility
* **Hibernate Validator** relies on Java reflection APIs and class structures, limiting its usage to JVM environments (desktop JVMs, Spring Boot, etc.).
* **Valix** is written in pure Kotlin. The core validation interfaces, runtime rules, and generated validator classes are fully compatible with Kotlin Multiplatform (KMP). It can run validations identically on the JVM, Android, iOS, JavaScript (Node.js/Browser), and WebAssembly (WasmJS).

### C. GraalVM Native Image Compilation
* Compiling Hibernate Validator into a standalone GraalVM native binary requires configuring a reflection registry (e.g. `reflect-config.json`) listing every data class, constraint annotation, and validator class. This configuration is necessary because the native image compiler strips reflection metadata by default.
* Because Valix's generated validator classes call fields directly (`value.email`), the GraalVM compiler traces and compiles the code paths automatically. No reflection registration is required for validation execution.

---

## 3. When to Use Which?

### Choose Valix if:
1. **You are building Kotlin Multiplatform (KMP) applications** and need to share request-validation schemas between backend JVM servers and iOS/Android/Web clients.
2. **You compile to GraalVM Native Images** (e.g., in serverless environments) and want fast cold starts and zero reflection configuration maintenance.
3. **You are building Android client applications** where startup times, garbage collection pauses, and binary sizes are highly constrained.
4. **You need asynchronous validation constraints** (e.g. database lookups) out-of-the-box.

### Choose Hibernate Validator if:
1. **You are working in a Java-only codebase** that doesn't use Kotlin.
2. **Your application relies on standard Jakarta EE specifications** (such as JPA cascading validations or integration with standard Jakarta Restful Web Services) where Hibernate Validator is pre-configured by the runtime container.
