package com.firstmage.mageswand.command;

import com.firstmage.mageswand.MagesWand;
import com.firstmage.mageswand.wand.RegisteredFuel;
import com.firstmage.mageswand.wand.WandConstants;
import com.firstmage.mageswand.wand.WandExecutorRegistry;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class WandAdminCommand implements BasicCommand {
    private final MagesWand plugin;

    public WandAdminCommand(MagesWand plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            sender.sendMessage("/wand reload, /wand spells, /wand classes, /wand getwand [player]");
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "spells" -> handleSpells(sender);
            case "classes" -> handleClasses(sender);
            case "getwand" -> handleGetWand(sender, args);
            default -> sender.sendMessage("Unknown subcommand. Use /wand reload, /wand spells, /wand classes, or /wand getwand [player].");
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            return List.of("reload", "spells", "classes", "getwand");
        }

        if (args.length == 1) {
            return filterStartingWith(List.of("reload", "spells", "classes", "getwand"), args[0]);
        }

        if (args.length == 2 && "getwand".equalsIgnoreCase(args[0])) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return filterStartingWith(names, args[1]);
        }

        return List.of();
    }

    @Override
    public @Nullable String permission() {
        return WandConstants.COMMAND_PERMISSION;
    }

    private void handleReload(CommandSender sender) {
        MagesWand.ReloadReport report = this.plugin.reloadSpellSystem();
        sender.sendMessage("Reloaded spell jars and fuel mappings: "
                + report.executorReport().loadedJars() + " jars, "
                + report.executorReport().discoveredExternalExecutorClasses() + " discovered external executor classes, "
                + report.fuelReport().loadedMappings() + " fuel mappings.");
        if (!report.executorReport().issues().isEmpty() || report.fuelReport().skippedMappings() > 0) {
            sender.sendMessage("Some classes or mappings were skipped. Check console or run /wand classes and /wand spells.");
        }
    }

    private void handleSpells(CommandSender sender) {
        List<RegisteredFuel> fuels = this.plugin.getFuelRegistry().getRegisteredFuels();
        if (fuels.isEmpty()) {
            sender.sendMessage("No fuel mappings are loaded.");
            return;
        }

        sender.sendMessage("Configured wand spells:");
        for (RegisteredFuel fuel : fuels) {
            sender.sendMessage("- " + fuel.signature().material()
                    + " -> " + fuel.spellName()
                    + " class=" + fuel.executorClass()
                    + " source=" + fuel.executorSource()
                    + (fuel.operatorOnly() ? " op-only" : ""));
        }
    }

    private void handleClasses(CommandSender sender) {
        List<WandExecutorRegistry.LoadedExecutorClass> classes = this.plugin.getExecutorRegistry().getKnownExecutorClasses();
        if (classes.isEmpty()) {
            sender.sendMessage("No executor classes are currently known. Add spell jars to plugins/mageswand/spells and run /wand reload.");
            return;
        }

        sender.sendMessage("Known executor classes:");
        for (WandExecutorRegistry.LoadedExecutorClass loaded : classes) {
            sender.sendMessage("- " + loaded.className() + " source=" + loaded.source());
        }
    }

    private void handleGetWand(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage("Console must specify a player: /wand getwand <player>");
            return;
        }

        var leftovers = target.getInventory().addItem(this.plugin.createWandItem());
        leftovers.values().forEach(itemStack -> target.getWorld().dropItemNaturally(target.getLocation(), itemStack));
        sender.sendMessage("Gave FirstMage's Wand to " + target.getName() + '.');
    }

    private Collection<String> filterStartingWith(Collection<String> values, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                results.add(value);
            }
        }
        return results;
    }
}
