# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.5] - 2026-08-21

### Added
- **Multi-Dimensional Collections & Map Traversals**: Recursive generic type traversal in `ValixProcessor` supporting nested collections (e.g. `grid[0][1].value`) and maps (e.g. `meta['key'].value`).
- **Unified KSP Compiler Node Diagnostics**: Compilation constraint validation errors now map directly to the offending `KSAnnotation` AST node rather than the property declaration, enabling IDEs to highlight the exact invalid annotation.
- **Dedicated Documentation & GEO Suite**:
  - Added `docs/FAQ.md` addressing runtime vs build-time boundaries and reflection-free mechanics.
  - Added `docs/COMPARISON.md` with side-by-side architectural analysis against Hibernate Validator (JSR-380).
  - Restructured framework guides (`SPRING.md`, `KTOR.md`, `MICRONAUT.md`) using search-optimized question-first headings.
  - Added high-density context documents `docs/LLMS.txt` and `docs/LLMS_FULL.txt`.
- **OpenAPI & JSON Schema Generation**: Endpoints and schema generator support for OpenAPI 3.1 YAML and Draft-07 JSON Schema via `valix-schema`.
- **Open Source Governance & Policies**:
  - Dedicated `LICENSE` (Apache 2.0 with Developer Syndicate copyright notice).
  - `SECURITY.md` vulnerability disclosure policy.
  - `CODE_OF_CONDUCT.md` (Contributor Covenant v2.1).
  - `.github/PULL_REQUEST_TEMPLATE.md`.
- **Dokka SEO/GEO Enhancements**: Dynamic `sitemap.xml` generation and `<meta description>` / JSON-LD schema injection in GitHub Pages documentation pipeline.

### Changed
- Expanded `@NotEmpty` and `@Size` constraint support to `Map` and `MutableMap` properties.
- Refactored `ConstraintGenerator`, `ValixProcessor`, and rule modules to eliminate hardcoded magic strings via centralized `ValixAnnotationNames`.

---

## [1.0.4] - 2026-08-18

### Added
- **KSP Incremental Processing**: Configured individual validator outputs as `Isolating` dependencies and `ValixRegistry` as an `Aggregating` dependency for fast incremental builds.
- **GraalVM Native Image Reachability Metadata**: Packaged `reflect-config.json` inside `valix-core` resources to ensure native binary compatibility.
- **Nested Entity Tree Benchmarks**: Added comparative benchmarks demonstrating up to 15.96x throughput improvement over Hibernate Validator on complex object graphs.

---

## [1.0.3] - 2026-08-12

### Added
- Framework adapters for Spring Boot (`@ValidValix`), Ktor 3.x plugin (`ValixKtorPlugin`), and Micronaut (`@ValixValidated`).
- Jetpack Compose form validation helpers (`rememberValixForm`) and Coroutines Flow integration (`validateWith`).

---

## [1.0.2] - 2026-08-12

### Added
- Kotlin Multiplatform (KMP) support across JVM, Android, iOS (`iosArm64`, `iosX64`, `iosSimulatorArm64`), Web (JS), and WebAssembly (Wasm).
- Core `ValidationResult` and `ValidationError` data structures.

---

## [1.0.1] - 2026-06-16

### Changed
- Modularized architecture: consolidated annotations, metadata, and core contracts into clean multiplatform submodules.

---

## [1.0.0] - 2026-06-15

### Added
- Initial release of Valix: compile-time generated validation engine powered by Kotlin Symbol Processing (KSP) with zero runtime reflection.
- Built-in string, numeric, date/time, and collection constraint annotations.
- Custom validator extensions via `ConstraintValidator<T>` and suspending `AsyncConstraintValidator<T>`.
