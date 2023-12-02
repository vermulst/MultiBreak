package me.vermulst.multibreak;

import me.vermulst.multibreak.commands.MultiBreakCommand;
import me.vermulst.multibreak.commands.MultiBreakTabCompleter;
import me.vermulst.multibreak.multibreak.BreakManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;


public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new BreakManager(this), this);
        PluginCommand multibreakCommand = this.getServer().getPluginCommand("multibreak");
        multibreakCommand.setExecutor(new MultiBreakCommand(this));
        multibreakCommand.setTabCompleter(new MultiBreakTabCompleter());
    }

    @Override
    public void onDisable() {
    }
}
