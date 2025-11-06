package me.vermulst.multibreak.commands;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.multibreak.BreakManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class MultiReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Plugin main = Main.getInstance();
        main.reloadConfig();
        if (Config.getInstance().load(main.getConfig())) {
            main.saveConfig();
        }
        sender.sendMessage(Component.text("Config reloaded").color(TextColor.color(85, 255, 85)));
        BreakManager breakManager = BreakManager.getInstance();
        for (Player p : Bukkit.getOnlinePlayers()) {
            breakManager.refreshTool(p);
        }
        return true;
    }
}
