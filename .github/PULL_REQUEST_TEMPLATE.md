## Description
<!-- Briefly describe the changes made in this pull request and the rationale behind them. -->

## Related Issue(s)
<!-- If this PR resolves an open issue, link it here (e.g., Fixes #123, Closes #456). -->

## Type of Change
- [ ] 🐛 Bug fix (non-breaking change which fixes an issue)
- [ ] ✨ New feature (non-breaking change which adds functionality)
- [ ] ⚡ Performance improvement (optimization to codegen or runtime)
- [ ] 💥 Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] 📝 Documentation update
- [ ] 🔧 Build / CI / Tooling improvement

## Affected Modules
- [ ] `valix-annotations`
- [ ] `valix-core`
- [ ] `valix-runtime`
- [ ] `valix-ksp`
- [ ] `valix-spring`
- [ ] `valix-ktor`
- [ ] `valix-micronaut`
- [ ] `valix-compose`
- [ ] `valix-flow`
- [ ] `valix-viewmodel`
- [ ] `valix-schema`
- [ ] `valix-serialization`
- [ ] `valix-localization`
- [ ] `valix-gradle-plugin`
- [ ] Documentation / Samples

## Checklist
- [ ] My code follows the [Code Style & Guidelines](CONTRIBUTING.md#4-code-style--design-guidelines) of this project.
- [ ] I have maintained the **zero runtime reflection** principle for generated validators.
- [ ] I have verified **Kotlin Multiplatform (KMP)** compatibility (`commonMain` does not use JVM-only APIs).
- [ ] I have added tests covering the new behavior / bug fix (`./gradlew test` and `./gradlew :valix-ksp:test`).
- [ ] All existing and new tests pass locally.
- [ ] I have updated KDocs and/or relevant documentation in `docs/` where applicable.
