package me.vermulst.multibreak.commands;

import me.vermulst.multibreak.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class MultiReloadCommand implements CommandExecutor {

    private final Main plugin;
    public MultiReloadCommand(Main plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        this.getPlugin().reloadConfig();
        this.getPlugin().getConfigManager().load(this.getPlugin().getConfig());
        sender.sendMessage(Component.text("Config reloaded").color(TextColor.color(85, 255, 85)));
        return true;
    }

    public Main getPlugin() {
        return plugin;
    }
}
