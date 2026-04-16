# Gradle task guide

This version adds a small root orchestrator build so you can stay in the repository root and ask Gradle for the thing you actually mean.

## Main tasks

```bash
./gradlew compileCore
./gradlew compileSpellPack
./gradlew compileAll
./gradlew buildCoreJar
./gradlew buildSpellPackJar
./gradlew buildSelectedSpellPackJar -PspellExecutors=AmethystShardWandExecutor,SnowBlockStormWandExecutor
./gradlew buildRuntimeArtifacts
./gradlew runServer
./gradlew runServerCoreOnly
./gradlew runServerSelected -PspellExecutors=AmethystShardWandExecutor,SnowBlockStormWandExecutor
```

## How selected spell-pack builds work

The selected jar task does not try to compile a separate source set. That would make the build significantly more complicated for very little gain.

Instead, it compiles the normal spell-pack source set and then packages only the requested executor class files into a separate jar.

That means this is mainly a packaging and local-testing convenience task, which is probably what you want here.

The matching selected `wand-fuels.json` file is generated from `examples/wand-fuels.with-spell-pack.json`, so that run tasks and local tests do not try to load executor classes that are missing from the selected jar.

## Listing valid executors

```bash
./gradlew :mageswand-spell-pack:listSpellExecutors
```

## Notes

`runServer` and the other run tasks use explicit attached plugin jars instead of relying on automatic detection. That is deliberate in a multi-module project, because it makes it obvious which jars are being installed into which dev server run directory.

The core run task uses the Paperweight runtime jar, while the spell-pack run tasks attach either the full spell pack or the selected-only jar depending on the task you run.


## Important note

The root run tasks use the runtime jars produced in each module's `build/libs` directory. The root build no longer queries the `reobfJar` task directly from `:mageswand-core` during configuration, because that can fail in a multi-project Kotlin DSL build before the subproject has registered the task.
