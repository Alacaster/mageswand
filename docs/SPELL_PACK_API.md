# MagesWand spell executor contract

This simplified version keeps the public executor-facing contract inside the main plugin module.
There is no separate API module.

At startup and on `/wand reload`, the core plugin:

1. scans `plugins/mageswand/spells/` for `.jar` files
2. discovers concrete classes that implement `com.firstmage.mageswand.wand.WandExecutor`
3. reads `plugins/mageswand/wand-fuels.json`
4. instantiates the configured `executorClass` values by fully-qualified class name

A spell jar therefore needs only:

- a class that implements `WandExecutor`
- a public no-argument constructor
- any helper classes it uses packaged in the same jar

## Public executor contract

External spell jars should treat only these classes as stable:

- `WandExecutor`
- `WandContext`
- `WandExecutionResult`
- `WandInteractionType`
- `WandTargetType`
- `WandConstants`
- `WandClientBiomes`

`WandContext` exposes the owning Bukkit `Plugin` as `context.plugin()`. That gives external executors the plugin handle they need for scheduler-owned or plugin-owned Paper calls without exposing the concrete `MagesWand` class.

## What external spell projects should compile against

External spell projects should compile against:

- the built `mageswand-core` plugin jar from this repository
- the Paper API

The spell jar should only use the public executor contract listed above. It should not depend on internal engine classes such as `FuelRegistry`, `WandListener`, `WandItems`, `WandTargeting`, or `MagesWand`.

The spell jar should not shade Paper or MagesWand classes into itself.

## Default fuel config in this repo

The default `wand-fuels.json` in this repository starts empty on purpose.
That keeps the core plugin independent from any bundled spell content.
