# MagesWand simplified project

This repository is a cleaned and consolidated version of the recovered project, simplified to keep only the two pieces that matter most in practice:

- `mageswand-core` — the Paper plugin and the executor-facing public types
- `mageswand-spell-pack` — the recovered external spell pack with the four executor classes

There is no separate API module in this version. The public executor contract now lives directly in the main plugin source tree, and external spell jars compile against the built `mageswand-core` jar plus the Paper API.

## What this project is

The core plugin is a wand engine:

1. it listens for wand interactions
2. turns those interactions into a normalized `WandContext`
3. looks up the offhand fuel item in `wand-fuels.json`
4. instantiates and runs the configured `WandExecutor`
5. applies the executor result to vanilla interaction handling and fuel consumption

The spell pack is just a jar full of `WandExecutor` implementations. At runtime, the plugin scans `plugins/mageswand/spells/` for jars and loads concrete executor classes from them.

## Main simplifications in this version

- Removed the separate `mageswand-api` module
- Switched `mageswand-spell-pack` to compile against `:mageswand-core`
- Removed the external example project that depended on the deleted API module
- Kept the runtime spell-jar loading model
- Kept the optional ProtocolLib biome bridge
- Added a few code-quality fixes called out in `REVIEW_AND_CHANGES.md`

## Build

From the repository root:

```bash
./gradlew :mageswand-core:build
./gradlew :mageswand-spell-pack:build
```

Artifacts will be placed in each module's `build/libs/` directory.

## Runtime notes

- Put the built core plugin jar on the server as a normal Paper plugin.
- Put the built spell-pack jar into `plugins/mageswand/spells/`.
- Copy `examples/wand-fuels.with-spell-pack.json` into `plugins/mageswand/wand-fuels.json` and adjust as needed.
- ProtocolLib is optional. If it is present, the client-biome bridge is installed. If it is absent, biome spoofing becomes a no-op.

## External spell jars

External spell jars should compile against:

- the built `mageswand-core` jar from this project
- the Paper API

They should only use the public executor-facing classes:

- `WandExecutor`
- `WandContext`
- `WandExecutionResult`
- `WandInteractionType`
- `WandTargetType`
- `WandConstants`
- `WandClientBiomes`

They should not depend on internal engine classes such as `FuelRegistry`, `WandListener`, `WandItems`, `WandTargeting`, or `MagesWand`.

## Useful Gradle tasks

Build and compile tasks from the repository root:

```bash
./gradlew compileCore
./gradlew compileAll
./gradlew buildCoreJar
./gradlew buildSpellPackJar
./gradlew buildRuntimeArtifacts
```

Selected spell-pack builds:

```bash
./gradlew :mageswand-spell-pack:listSpellExecutors
./gradlew buildSelectedSpellPackJar -PspellExecutors=AmethystShardWandExecutor,SnowBlockStormWandExecutor
```

Local Paper runs:

```bash
./gradlew runServer
./gradlew runServerCoreOnly
./gradlew runServerSelected -PspellExecutors=AmethystShardWandExecutor,SnowBlockStormWandExecutor
```

`runServer` uses the full spell pack and writes the example `wand-fuels.json` into `run/plugins/mageswand/`.

`runServerCoreOnly` writes an empty `wand-fuels.json` into `run-core-only/plugins/mageswand/`.

`runServerSelected` builds a spell-pack jar containing only the selected executors and generates a matching `wand-fuels.json` inside `run-selected/plugins/mageswand/`.

Executor names can be passed either as simple class names such as `SnowBlockStormWandExecutor` or as fully-qualified class names.
