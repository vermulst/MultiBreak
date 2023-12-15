package me.vermulst.multibreak;

import me.vermulst.multibreak.commands.MultiBreakCommand;
import me.vermulst.multibreak.commands.MultiBreakTabCompleter;
import me.vermulst.multibreak.commands.MultiConfigCommand;
import me.vermulst.multibreak.commands.MultiConfigTabCompleter;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.multibreak.BreakManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;


public final class Main extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager();
        this.getConfigManager().load(this.getConfig());

        this.getServer().getPluginManager().registerEvents(new BreakManager(this), this);

        //multibreak command
        PluginCommand multibreakCommand = this.getServer().getPluginCommand("multibreak");
        multibreakCommand.setExecutor(new MultiBreakCommand(this));
        multibreakCommand.setTabCompleter(new MultiBreakTabCompleter());

        //multiconfig command
        PluginCommand multiconfigCommand = this.getServer().getPluginCommand("multiconfig");
        multiconfigCommand.setExecutor(new MultiConfigCommand(this));
        multiconfigCommand.setTabCompleter(new MultiConfigTabCompleter(this.getConfigManager()));
    }

    @Override
    public void onDisable() {
        this.getConfigManager().save(this.getConfig());
        this.saveConfig();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
