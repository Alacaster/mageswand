package com.firstmage.mageswand;

import com.firstmage.mageswand.command.WandAdminCommand;
import com.firstmage.mageswand.wand.FuelRegistry;
import com.firstmage.mageswand.wand.RegisteredFuel;
import com.firstmage.mageswand.wand.WandExecutorRegistry;
import com.firstmage.mageswand.wand.WandItems;
import com.firstmage.mageswand.wand.WandClientBiomes;
import com.firstmage.mageswand.wand.ProtocolLibWandClientBiomes;
import com.firstmage.mageswand.wand.WandListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

public final class MagesWand extends JavaPlugin {
    private WandExecutorRegistry executorRegistry;
    private FuelRegistry fuelRegistry;
    private WandListener wandListener;

    @Override
    public void onEnable() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register("wand", new WandAdminCommand(this));
        });

        ensureDefaultFiles();

        WandClientBiomes.install(WandClientBiomes.Adapter.unavailable());
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            WandClientBiomes.install(new ProtocolLibWandClientBiomes(this));
        } else {
            getLogger().info("ProtocolLib not found; client-biome spoofing is disabled.");
        }

        this.executorRegistry = new WandExecutorRegistry(this);
        this.fuelRegistry = new FuelRegistry(this, this.executorRegistry);
        this.wandListener = new WandListener(this, this.fuelRegistry);

        getServer().getPluginManager().registerEvents(this.wandListener, this);

        ReloadReport report = reloadSpellSystem();
        getLogger().info("Loaded " + report.executorReport().loadedJars() + " spell jars, "
                + report.executorReport().discoveredExternalExecutorClasses() + " discovered external executor classes, and "
                + report.fuelReport().loadedMappings() + " configured fuel mappings.");

        for (String issue : report.executorReport().issues()) {
            getLogger().warning(issue);
        }
        for (String issue : report.fuelReport().issues()) {
            getLogger().warning(issue);
        }
    }

    @Override
    public void onDisable() {
        if (this.wandListener != null) {
            this.wandListener.clear();
        }
        WandClientBiomes.install(WandClientBiomes.Adapter.unavailable());
        if (this.executorRegistry != null) {
            this.executorRegistry.close();
        }
    }

    public ReloadReport reloadSpellSystem() {
        try {
            WandExecutorRegistry.LoadReport executorReport = this.executorRegistry.reload();
            FuelRegistry.LoadReport fuelReport = this.fuelRegistry.load();
            this.wandListener.clear();

            logLoadedSpells();

            return new ReloadReport(executorReport, fuelReport);
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Failed to reload spell jars and fuel mappings.", exception);
            throw exception;
        }
    }

    private void logLoadedSpells() {
        List<RegisteredFuel> fuels = this.fuelRegistry.getRegisteredFuels();

        getLogger().info("Registered wand spells: " + fuels.size());

        if (fuels.isEmpty()) {
            getLogger().info("  (none)");
            return;
        }

        for (RegisteredFuel fuel : fuels) {
            getLogger().info("  - "
                    + fuel.spellName()
                    + " | fuel=" + fuel.signature().material()
                    + " | class=" + fuel.executorClass()
                    + " | source=" + fuel.executorSource()
                    + (fuel.operatorOnly() ? " | op-only" : ""));
        }
    }

    public WandExecutorRegistry getExecutorRegistry() {
        return this.executorRegistry;
    }

    public FuelRegistry getFuelRegistry() {
        return this.fuelRegistry;
    }

    public Path getSpellPackDirectory() {
        return getDataFolder().toPath().resolve("spells");
    }

    public ItemStack createWandItem() {
        return WandItems.createFirstMageWand();
    }

    private void ensureDefaultFiles() {
        try {
            Files.createDirectories(getDataFolder().toPath());
            Files.createDirectories(getSpellPackDirectory());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create plugin data directories.", exception);
        }

        if (!new java.io.File(getDataFolder(), "wand-fuels.json").exists()) {
            saveResource("wand-fuels.json", false);
        }
    }

    public record ReloadReport(
            WandExecutorRegistry.LoadReport executorReport,
            FuelRegistry.LoadReport fuelReport
    ) {
    }
}