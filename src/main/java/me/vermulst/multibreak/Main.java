package me.vermulst.multibreak;

import me.vermulst.multibreak.commands.MultiBreakCommand;
import me.vermulst.multibreak.commands.MultiBreakTabCompleter;
import me.vermulst.multibreak.commands.MultiConfigCommand;
import me.vermulst.multibreak.commands.MultiConfigTabCompleter;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.multibreak.BreakManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


public final class Main extends JavaPlugin {

    private ConfigManager configManager;
    private final Set<Material> nonOccluding = Arrays.stream(Material.values()).filter(material -> !material.isOccluding()).collect(Collectors.toSet());

    @Override
    public void onEnable() {

        int pluginId = 20516; // <-- Replace with the id of your plugin!
        new Metrics(this, pluginId);

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

    public Set<Material> getNonOccluding() {
        return nonOccluding;
    }
}
