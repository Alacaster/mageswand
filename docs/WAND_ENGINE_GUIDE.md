# Magic wand engine guide

This guide matches the current engine shape after the event-association, retry-as-untargeted, dynamic-fuel-cost, and click-companion swallow changes.

The point of the engine is simple:

- the listener turns Paper events into a single `WandContext`
- the listener decides which Paper event owns a click when Paper gives you more than one event for the same physical input
- the fuel registry decides which external executor to run from the offhand item
- the executor decides spell behavior
- the `WandExecutionResult` tells the listener what to do afterward

That split is the whole architecture. Most of the design choices came from trying to keep those jobs separate after running into real bugs.

## Why the final design looks like this

The early versions tried to reconstruct clicks from broader signals and then let spell code do too much of the event plumbing. That caused two big classes of bugs.

The first class was duplicated casts. One physical click could show up as more than one Paper event. The worst offender was right-click entity use plus a generic right-click companion event, which caused one click to both pull and teleport. The fix was not to make the executor smarter. The fix was to make the listener own event association.

The second class was spell logic re-implementing too much targeting and fallback logic in inconsistent ways. That pushed too much engine behavior into every executor. The fix was to keep the listener responsible for building a consistent `WandContext`, then let the executor either handle that target or explicitly decline it and ask for a retry as untargeted.

So the final rules are:

- `WandTargeting` builds truth once
- executors interpret that truth
- `WandExecutionResult` communicates post-execution decisions
- the listener handles duplicates, fuel mutation, no-fuel fallback, and retry-as-untargeted redirects

## The core classes and what they mean

### `WandListener`

This is the engine entry point.

It listens to the authoritative Paper interaction families:

- `PlayerInteractEvent` for air and block interactions
- `PrePlayerAttackEntityEvent` for left-click entity attack attempts
- `PlayerInteractEntityEvent` and `PlayerInteractAtEntityEvent` for right-click entity use

It does four important things:

1. builds a `WandContext`
2. runs the matched executor
3. applies the `WandExecutionResult`
4. swallows known companion events so one physical click only casts once

The one-shot swallow identity is there because Paper can describe the same click through more than one event. The listener stores a very small identity saying, in effect, “if a generic right-click companion event appears for this player and hand on this tick, swallow exactly one.” That is much narrower than muting a whole tick.

### `WandTargeting`

This turns Paper events into a stable `WandContext`.

It computes things like:

- player and eye locations
- look direction
- target entity or block when one exists
- interaction point when Paper exposes one
- nearest target point otherwise
- base entity and block interaction range from the player’s actual base attributes
- whether the target is within those base ranges

This is why executors do not need to redo vanilla range math or target snapshots unless the spell is intentionally doing extra custom targeting.

### `WandContext`

This is the executor input. It is meant to be the fully baked snapshot of what happened.

Important fields and helpers:

- `interactionType()` tells you exactly which family you are in
- `targetEntity()` and `targetBlock()` are the authoritative event target when there is one
- `distanceToTarget()` is already computed
- `withinBaseEntityRange()` and `withinBaseBlockRange()` tell you whether vanilla would normally allow the interaction based on base attributes
- `isFirstMageUser()` and `isTargetingFirstMage()` are convenience helpers for your current wand design
- `isLeftClick()`, `isRightClick()`, `isBlockInteraction()`, and `isEntityInteraction()` are convenience helpers for branching cleanly

If a spell needs more targeting than the event already gave you, that is allowed. The engine gives you the event truth. Spells can still do extra custom searches or traces if the spell concept calls for it.

### `FuelRegistry`, `FuelSignature`, `RegisteredFuel`

This is the offhand-to-executor mapping system.

- `FuelRegistry` reads `wand-fuels.json`
- `FuelSignature` is material plus optional exact meta string
- `RegisteredFuel` just pairs a signature with an executor instance

The registry first tries an exact signature match, then falls back to material-only match.

That is why a key like `LAPIS_LAZULI` works for plain lapis, while a key like `SOME_ITEM||{meta...}` can be exact.

### `WandExecutor`

This is the spell interface.

The engine dispatches through six interaction methods:

- `onLeftClickAir`
- `onLeftClickBlock`
- `onLeftClickEntityAttack`
- `onRightClickAir`
- `onRightClickBlock`
- `onRightClickEntityUse`

You usually override only the methods your spell cares about. The default implementation returns `WandExecutionResult.passThrough()`.

### `WandExecutionResult`

This is the executor’s contract with the engine.

It currently carries:

- `fuelCost`
- `triggerNoFuelFallback`
- `retryAsUntargetedInteraction`
- `allowVanillaBlockUse`
- `allowVanillaItemUse`
- `allowVanillaEntityAttack`
- `allowVanillaEntityUse`

This record exists because one boolean was never enough.

You need to be able to say things like:

- consume 2 fuel and deny all vanilla follow-through
- consume 0 fuel and let vanilla continue
- trigger the no-fuel fallback even though the offhand item matched the fuel type
- decline this entity target and ask the listener to retry as untargeted air

Important static factories:

- `passThrough()`
- `denyAll()`
- `noFuelFallback()`
- `retryAsUntargeted()`
- `consumeAndDenyAll(int fuelCost)`
- `consumeAndPassThrough(int fuelCost)`

Important note: the boolean accessor on the record is named `retryAsUntargetedInteraction()`, but the factory method is named `retryAsUntargeted()`. That naming split exists on purpose to avoid the Java record accessor collision that bit the codebase earlier.

## The normal engine flow

1. The listener receives a Paper event.
2. It decides whether this event is authoritative or whether it is a known companion event that should be swallowed.
3. It builds a `WandContext`.
4. It finds the executor from the offhand fuel.
5. It calls `executor.execute(context)`.
6. If the result says `triggerNoFuelFallback`, the listener applies no-fuel damage and message.
7. If the result says `fuelCost > 0`, the listener consumes fuel.
8. If the result says `retryAsUntargetedInteraction`, the listener rebuilds context as the untargeted version of that interaction family and runs the executor again.
9. The listener applies the vanilla permission flags to the original Paper event.

The important part is that retry-as-untargeted is listener-owned, not executor-owned. The executor is allowed to say “this entity target is not meaningful for this spell,” but the listener is the thing that reinterprets the click.

## Why `retryAsUntargeted` exists

This showed up because “entity target exists” and “this spell should treat that entity as the real spell target” are not the same thing.

Example: a pull spell has a spell pull range of 20, but the client and Paper still hand you an entity-use event because the wand item itself has much larger interaction range. The spell can look at the event target and decide “too far, not my real target.” In older versions, the executor then had to manually call its own teleport fallback logic. That worked, but it duplicated a common engine pattern.

Now the executor can just return `WandExecutionResult.retryAsUntargeted()`, and the listener will rerun the spell as `RIGHT_CLICK_AIR` or `LEFT_CLICK_AIR` as appropriate.

This keeps the executor simple while still letting the listener own interaction reinterpretation.

## How to write new executors

The pattern is:

1. Decide which interaction methods you care about.
2. Read only from `WandContext` unless your spell intentionally does extra custom targeting.
3. Return a `WandExecutionResult` that describes what the listener should do.
4. Let the listener mutate inventory and decide event plumbing.

A simple decision checklist for each method:

- do I care about this interaction family at all?
- do I have enough fuel for the branch I want?
- do I want normal no-fuel fallback if I cannot do this branch?
- is the current event target meaningful to the spell?
- if the target is not meaningful, do I want `retryAsUntargeted()`?
- if the spell succeeds, how much fuel should it cost?
- should vanilla continue after the spell or should it be denied?

## A practical rule for targeted spells

Use this pattern when the spell can fall back to an untargeted version:

- entity method checks spell-specific target validity
- if valid, do the targeted spell and return a normal result
- if invalid but you still want the click to mean something, return `WandExecutionResult.retryAsUntargeted()`
- let `onRightClickAir` or `onLeftClickAir` implement the untargeted fallback behavior

That is now the preferred way to express “too far to pull, so blink instead.”

## Vanilla permissions and why they are split

There are separate flags for block use, item use, entity attack, and entity use because Paper itself splits those behaviors. One yes-or-no result was not enough.

Examples:

- you may want to deny vanilla chest opening but still allow the held item behavior
- you may want to deny entity attack but still allow your spell to cast
- you may want to consume fuel and do your own custom effect while completely denying vanilla follow-through

So do not try to compress these back into one success boolean.

## Fuel cost and no-fuel fallback rules

The listener owns inventory mutation. Executors should not directly consume fuel from the offhand stack.

The executor decides the cost by returning it in `WandExecutionResult`.

If the executor wants the normal self-damage and message path even though a fuel type matched, it returns `noFuelFallback()`.

This is how dynamic costs work cleanly without every executor inventing its own inventory mutation logic.

## Compile-time debug with no release overhead

Runtime debug gates are not enough if you want effectively zero release overhead. If you want debug branches stripped from the compiled code path, use a compile-time constant.

The pattern is:

1. generate a build flag class during Gradle build
2. make `DEBUG` a `public static final boolean` compile-time constant
3. wrap every debug statement in `if (WandBuildFlags.DEBUG) { ... }`

Because the condition is a compile-time constant, `javac` can inline it and the dead branch disappears in non-debug builds. That is the important part. A helper method that checks a runtime boolean does not give you the same property.

Recommended generated class:

```java
package com.firstmage.mageswand.wand;

public final class WandBuildFlags {
    public static final boolean DEBUG = false;

    private WandBuildFlags() {
    }
}
```

In a debug build, generate the same class with `DEBUG = true;`.

Then use it like this everywhere:

```java
if (WandBuildFlags.DEBUG) {
    WandDebug.chat(context.player(), "entered onRightClickEntityUse target=" + context.targetEntity());
}
```

Do not hide the condition inside `WandDebug.chat(...)` if your goal is stripping the call site in release builds. The whole `if (WandBuildFlags.DEBUG)` should be at the call site.

A small helper class is still useful for formatting messages, but it should not be the thing deciding whether debug is enabled.

Recommended `WandDebug` shape:

```java
package com.firstmage.mageswand.wand;

import org.bukkit.entity.Player;

public final class WandDebug {
    private static final String PREFIX = "[WAND DEBUG] ";

    private WandDebug() {
    }

    public static void chat(Player player, String message) {
        player.sendMessage(PREFIX + message);
    }

    public static void chat(WandContext context, String message) {
        chat(context.player(), message);
    }
}
```

That way the whole call is gone in release builds because the enclosing `if` was compiled away.

## Gradle shape for compile-time debug flags

The clean way is to generate `WandBuildFlags.java` during the build.

Use a Gradle property to decide whether the build is debug or release.

Conceptually:

- normal run/build: `wandDebug=false`
- debug run/build: `wandDebug=true`

Then generate the class into a generated source directory and add that directory to the main source set.

A good build shape is included in the dev kit as a separate snippet file.

## Anti-patterns to avoid

- do not mutate the offhand stack directly from executors
- do not make executors manage Paper duplicate-event plumbing
- do not reimplement vanilla base-range checks in every executor unless the spell is intentionally doing different custom range logic
- do not put broad tick-wide suppression flags in the listener when a one-shot swallow identity will do
- do not use runtime debug gating if you want debug stripped from release code
- do not name a static factory on a record the same as a record component accessor

## When extra executor-side targeting is good

Extra targeting work in executors is fine when the spell concept itself needs it.

Examples:

- find the nearest chest through walls and open it magically
- scan for nearby players in a cone and push them
- choose the nearest safe blink destination from a custom search

That is different from redoing event truth. The engine already tells you what the original click targeted. Custom spell targeting is an extra layer on top of that.

## Mental model to keep in your head

The engine handles “what click was this really?”

The executor handles “what does this spell do with that click?”

If you keep that boundary clean, the code stays understandable and weird Paper edge cases do not leak into every spell class.
