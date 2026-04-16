# MagesWand

This repository is a wand engine for Paper. The core plugin turns Minecraft interaction events into a stable spell-execution model, and external spell jars provide the actual spell behavior through `WandExecutor` implementations.

The project is split into two practical pieces:

`mageswand-core` is the real plugin. It listens for input, resolves fuel, builds a stable execution snapshot, runs the selected executor, and applies the result back to the original Paper event.

`mageswand-spell-pack` is example content. It contains executor classes that show the kinds of spells the core is designed to support.

## What the core makes easier for executor authors

The biggest thing the core buys you is that executor authors do not have to become accidental event-system experts just to write a spell. In raw Paper plugin code, a spell author usually ends up re-solving the same annoying problems over and over: which interaction event actually “owns” the click, whether a click on an entity should really count as an entity-targeted spell, how to consume fuel without every spell mutating inventory differently, and how to stop vanilla behavior from leaking through in ways that make the spell feel inconsistent. The core centralizes those decisions so executor authors can mostly think in terms of “what spell should happen for this interaction” rather than “how do I survive Bukkit and Paper input edge cases.”

The clever part is that the core does not just pass through raw events. It first normalizes them into a `WandContext`, which is a snapshot of the click with already-computed targeting and range information. That means an executor gets the player, the wand, the offhand fuel item, the interaction family, the looked-at entity or block when one exists, a target point, a distance, the player’s base block and entity interaction ranges, and some convenience helpers. That sounds small until you realize how much ugly duplicated code disappears once every spell stops doing its own half-correct target reconstruction.

The second clever part is that executor authors return a `WandExecutionResult` instead of directly trying to control everything themselves. That result is not just “success” or “failure.” It lets a spell say how much fuel to consume, whether the engine should trigger the no-fuel fallback path, whether this targeted interaction should be retried as an untargeted one, whether vanilla block or item or entity behavior should still be allowed afterward, and whether extended vanilla block interactions should be enabled. In other words, the executor describes intent and the listener owns the messy platform behavior. That is a much cleaner boundary than making every spell manually cancel events, mutate inventory, and rediscover the same edge cases.

A third smart design choice is the retry-as-untargeted flow. Sometimes Paper gives you a real entity interaction event, but from the spell’s point of view that entity is not the meaningful spell target. Maybe the player clicked an entity that is technically reachable by the item interaction system but too far away for the spell’s own design. Instead of forcing every spell to duplicate “if target invalid, fall back to air cast behavior,” the executor can return `WandExecutionResult.retryAsUntargeted()`, and the core will rebuild the interaction as the untargeted version and run the executor again. This is a very specific little trick, but it is exactly the kind of trick that makes a spell API feel thoughtful instead of merely existing.

The fuel system also saves a lot of glue code. Executors do not need to parse config or decide which spell class to instantiate. The core reads `wand-fuels.json`, matches the offhand item to a registered executor, instantiates that class, and then handles fuel consumption after the cast according to the returned result. Because the engine owns fuel mutation, dynamic-cost spells become straightforward instead of turning into little inventory-management engines on their own.

The existing executors show the sort of capabilities this design supports. `AmethystShardWandExecutor` uses different behavior for air, block, and entity interactions and also falls back from invalid targeted interactions into untargeted movement-style behavior. `LapisLazuliWandExecutor` launches fireballs and uses context-aware branching based on the actual target and the player identity. `SnowBlockStormWandExecutor` uses scheduler-owned follow-up behavior and optional client-side biome spoofing through `WandClientBiomes`, which shows that executors can do time-based effects and optional platform integrations without needing direct access to internal engine classes. `DirtBarrelReachWandExecutor` shows the opposite end of the scale: a tiny executor can simply declare “allow this very specific kind of extended block interaction” and let the engine do the rest.

The practical outcome is that a spell author gets to work at the level of gameplay rules. You ask questions like “what should left click on an entity do,” “how much fuel should that branch cost,” “should vanilla continue afterward,” and “if this target is not meaningful, should I retry as untargeted.” You do not spend most of your time reinventing targeting, event cancellation, or fuel bookkeeping.

## Actual API usage

An external spell jar should compile against the built `mageswand-core` jar from this repository and the Paper API. There is no separate API module in this version. The stable contract is simply a small set of executor-facing types that live inside the core module: `WandExecutor`, `WandContext`, `WandExecutionResult`, `WandInteractionType`, `WandTargetType`, `WandConstants`, and `WandClientBiomes`.

A spell class needs only three things. It must implement `WandExecutor`. It must be concrete. It must have a public no-argument constructor. At runtime, the core scans `plugins/mageswand/spells/` for jars, discovers executor implementations, and then instantiates the class named in `wand-fuels.json`.

Here is the smallest possible executor:

```java
package com.example.spells;

import com.firstmage.mageswand.wand.WandContext;
import com.firstmage.mageswand.wand.WandExecutionResult;
import com.firstmage.mageswand.wand.WandExecutor;

public final class SimpleBurstExecutor implements WandExecutor {
    @Override
    public WandExecutionResult onRightClickAir(WandContext context) {
        context.player().sendMessage("Burst!");
        return WandExecutionResult.consumeAndDenyAll(1);
    }
}
```

That example shows the basic rhythm. Read what you need from `WandContext`, perform the spell effect, then return a `WandExecutionResult` describing what the engine should do next. `consumeAndDenyAll(1)` means “spend one fuel and do not allow vanilla follow-through.” If your spell wants to do nothing and let Minecraft behave normally, return `WandExecutionResult.passThrough()`. If your spell wants the normal no-fuel penalty path, return `WandExecutionResult.noFuelFallback()`.

`WandExecutor` is shaped around interaction families instead of around raw Paper event classes. The dispatch methods are `onLeftClickAir`, `onLeftClickBlock`, `onLeftClickEntityAttack`, `onRightClickAir`, `onRightClickBlock`, and `onRightClickEntityUse`. You usually override only the ones that matter. The default implementation for every method is `passThrough()`, so you do not need to write empty branches.

`WandContext` is the main input object. The fields you will use most often are `player()`, `wand()`, `offhandFuel()`, `interactionType()`, `targetEntity()`, `targetBlock()`, `interactionPoint()`, `targetLocation()`, `distanceToTarget()`, `withinBaseEntityRange()`, and `withinBaseBlockRange()`. It also includes helpers like `isLeftClick()`, `isRightClick()`, `isBlockInteraction()`, `isEntityInteraction()`, `isFirstMageUser()`, and `isTargetingFirstMage()`. Those last two are project-specific conveniences rather than universal API ideas, but they are available because your current spell design uses them.

A more realistic executor often looks like this:

```java
package com.example.spells;

import com.firstmage.mageswand.wand.WandContext;
import com.firstmage.mageswand.wand.WandExecutionResult;
import com.firstmage.mageswand.wand.WandExecutor;
import org.bukkit.Particle;

public final class BlinkOrMarkExecutor implements WandExecutor {
    @Override
    public WandExecutionResult onRightClickEntityUse(WandContext context) {
        if (context.targetEntity() == null) {
            return WandExecutionResult.retryAsUntargeted();
        }

        if (context.distanceToTarget() > 8.0D) {
            return WandExecutionResult.retryAsUntargeted();
        }

        context.targetEntity().getWorld().spawnParticle(
                Particle.ENCHANT,
                context.targetEntity().getLocation().add(0.0D, 1.0D, 0.0D),
                25
        );

        return WandExecutionResult.consumeAndDenyAll(1);
    }

    @Override
    public WandExecutionResult onRightClickAir(WandContext context) {
        context.player().teleport(context.player().getLocation().add(context.lookDirection().multiply(5.0D)));
        return WandExecutionResult.consumeAndDenyAll(1);
    }
}
```

That pattern is worth understanding because it uses one of the core’s best ideas. The entity-use branch does not manually call the air-cast branch. It simply says “this entity target is not meaningful for my spell, retry this interaction as untargeted,” and the listener handles the reinterpretation. That keeps the executor simpler and keeps the fallback policy in one place.

If you need scheduled follow-up work, use `context.plugin()` rather than depending on the concrete `MagesWand` class. That is the intended way for an external executor to schedule Bukkit tasks safely while still remaining outside the internal engine implementation.

```java
context.player().getServer().getScheduler().runTaskLater(
        context.plugin(),
        () -> context.player().sendMessage("Delayed effect"),
        40L
);
```

If you want optional client-side biome spoofing, use `WandClientBiomes`. It is safe to call because the adapter can be unavailable when ProtocolLib is not installed.

```java
if (WandClientBiomes.isAvailable()) {
    WandClientBiomes.spoofForDuration(
            context.player(),
            org.bukkit.NamespacedKey.minecraft("snowy_taiga"),
            20L * 8L
    );
}
```

For registration, place your built spell jar into `plugins/mageswand/spells/` and add a matching entry to `plugins/mageswand/wand-fuels.json`. The loader accepts either a top-level array of mappings or an object with a `mappings` array. A simple object-form example looks like this:

```json
{
  "mappings": [
    {
      "fuel": "SNOW_BLOCK",
      "spellName": "Snow Storm",
      "spellDescription": "Right click to call a localized snowstorm.",
      "executorClass": "com.example.spells.SnowStormExecutor"
    }
  ]
}
```

The important field is `executorClass`, which must be the fully qualified class name of your executor. The `fuel` field identifies the offhand item that selects the spell. The loader supports material-only matching and also exact item-signature matching for more specialized items.

The safest rule for external executors is simple: depend only on the executor-facing types and Paper API, and do not reach into internal engine classes like `FuelRegistry`, `WandListener`, `WandItems`, `WandTargeting`, or `MagesWand`. The moment a spell jar starts leaning on those internals, you lose the whole benefit of having a stable executor contract in the first place.

For local development from the root of this repository, the useful tasks are still:

```bash
./gradlew compileCore
./gradlew compileAll
./gradlew buildCoreJar
./gradlew buildSpellPackJar
./gradlew buildSelectedSpellPackJar -PspellExecutors=AmethystShardWandExecutor,SnowBlockStormWandExecutor
./gradlew runServer
./gradlew runServerCoreOnly
./gradlew runServerSelected -PspellExecutors=AmethystShardWandExecutor,SnowBlockStormWandExecutor
```

So the working mental model is this: the core owns input normalization, targeting truth, fuel resolution, event follow-through, and retry behavior; the executor owns spell rules and returns a result describing what should happen next. That is the contract.
