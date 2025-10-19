package me.vermulst.multibreak;

import me.vermulst.multibreak.api.MultiBreakAPI;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.multibreak.BreakManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


public final class Main extends JavaPlugin {

    private ConfigManager configManager;
    private final Set<Material> nonOccluding = Arrays.stream(Material.values()).filter(material -> !material.isOccluding()).collect(Collectors.toSet());
    private static Main INSTANCE;

    public static Main getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        // bStats
        int pluginId = 20516;
        new Metrics(this, pluginId);

        MultiBreakAPI.init(this);

        // config
        this.configManager = new ConfigManager();
        if (this.getConfigManager().load(this.getConfig())) {
            this.saveConfig();
        }

        // events
        Listener[] events = new Listener[] {
                new BreakManager(this)
        };
        for (Listener event : events) {
            this.getServer().getPluginManager().registerEvents(event, this);
        }

        // commands
        Commands commands = new Commands();
        commands.init(this);
        commands.register(this);
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
