package com.firstmage.mageswand.wand;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtocolLibWandClientBiomes implements WandClientBiomes.Adapter {
    private static final long REFRESH_INTERVAL_TICKS = 10L;

    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, ActiveOverride> activeOverrides = new ConcurrentHashMap<>();
    private boolean listenerInstalled;
    private volatile boolean spoofFailureLogged;
    private volatile boolean resetFailureLogged;

    public ProtocolLibWandClientBiomes(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        installPacketListener();
        startRefreshTask();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void spoofForDuration(Player player, NamespacedKey biomeKey, long durationTicks) {
        if (player == null || biomeKey == null || durationTicks <= 0L) {
            return;
        }

        long untilTick = Bukkit.getCurrentTick() + durationTicks;
        this.activeOverrides.put(player.getUniqueId(), new ActiveOverride(biomeKey, untilTick));
        sendSpoofedBiomePacket(player, biomeKey);
    }

    @Override
    public void clear(Player player) {
        if (player == null) {
            return;
        }

        ActiveOverride removed = this.activeOverrides.remove(player.getUniqueId());
        if (removed != null && player.isOnline()) {
            sendActualBiomePacket(player);
        }
    }

    private void installPacketListener() {
        if (this.listenerInstalled) {
            return;
        }

        this.protocolManager.addPacketListener(new PacketAdapter(
                this.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.CHUNKS_BIOMES
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                ActiveOverride override = activeOverrides.get(event.getPlayer().getUniqueId());
                if (override == null) {
                    return;
                }

                event.setCancelled(true);
            }
        });

        this.listenerInstalled = true;
    }

    private void startRefreshTask() {
        Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            long currentTick = Bukkit.getCurrentTick();

            this.activeOverrides.entrySet().removeIf(entry -> {
                Player player = Bukkit.getPlayer(entry.getKey());
                ActiveOverride override = entry.getValue();

                if (player == null || !player.isOnline()) {
                    return true;
                }

                if (currentTick >= override.untilTick()) {
                    sendActualBiomePacket(player);
                    return true;
                }

                sendSpoofedBiomePacket(player, override.biomeKey());
                return false;
            });
        }, 0L, REFRESH_INTERVAL_TICKS);
    }

    private void sendSpoofedBiomePacket(Player player, NamespacedKey biomeKey) {
        try {
            LevelChunk levelChunk = currentLevelChunk(player.getWorld(), player);
            Holder<Biome> biomeHolder = biomeHolder(player.getWorld(), biomeKey);
            byte[] buffer = buildFakeBiomeBuffer(levelChunk, biomeHolder);

            ClientboundChunksBiomesPacket packet = new ClientboundChunksBiomesPacket(List.of(
                    new ClientboundChunksBiomesPacket.ChunkBiomeData(levelChunk.getPos(), buffer)
            ));

            this.protocolManager.sendServerPacket(player, new PacketContainer(PacketType.Play.Server.CHUNKS_BIOMES, packet), false);
        } catch (Throwable throwable) {
            logBiomeFailure("spoof", player, throwable, true);
        }
    }

    private void sendActualBiomePacket(Player player) {
        try {
            LevelChunk levelChunk = currentLevelChunk(player.getWorld(), player);
            ClientboundChunksBiomesPacket packet = ClientboundChunksBiomesPacket.forChunks(List.of(levelChunk));
            this.protocolManager.sendServerPacket(player, new PacketContainer(PacketType.Play.Server.CHUNKS_BIOMES, packet), false);
        } catch (Throwable throwable) {
            logBiomeFailure("reset", player, throwable, false);
        }
    }

    private static LevelChunk currentLevelChunk(World world, Player player) {
        ServerLevel handle = ((CraftWorld) world).getHandle();
        return handle.getChunk(player.getChunk().getX(), player.getChunk().getZ());
    }

    private static Holder<Biome> biomeHolder(World world, NamespacedKey biomeKey) {
        ServerLevel handle = ((CraftWorld) world).getHandle();
        Registry<Biome> biomeRegistry = handle.registryAccess().lookupOrThrow(Registries.BIOME);
        Identifier location = Identifier.fromNamespaceAndPath(biomeKey.getNamespace(), biomeKey.getKey());
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, location);
        return biomeRegistry.getOrThrow(key);
    }

private void logBiomeFailure(String operation, Player player, Throwable throwable, boolean spoofOperation) {
    boolean alreadyLogged = spoofOperation ? this.spoofFailureLogged : this.resetFailureLogged;
    if (alreadyLogged) {
        return;
    }

    if (spoofOperation) {
        this.spoofFailureLogged = true;
    } else {
        this.resetFailureLogged = true;
    }

    this.plugin.getLogger().warning(
            "ProtocolLib biome " + operation + " failed for player " + player.getName()
                    + ": " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage()
    );
}

    private static byte[] buildFakeBiomeBuffer(LevelChunk chunk, Holder<Biome> spoofedBiome) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), chunk.getLevel().registryAccess());

        for (LevelChunkSection section : chunk.getSections()) {
            @SuppressWarnings("unchecked")
            PalettedContainer<Holder<Biome>> spoofedBiomes = (PalettedContainer<Holder<Biome>>) section.getBiomes().copy();

            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    for (int z = 0; z < 4; z++) {
                        spoofedBiomes.set(x, y, z, spoofedBiome);
                    }
                }
            }

            spoofedBiomes.write(buffer);
        }

        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(0, bytes);
        return bytes;
    }

    private record ActiveOverride(NamespacedKey biomeKey, long untilTick) {
    }
}
