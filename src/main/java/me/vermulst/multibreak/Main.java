package me.vermulst.multibreak;

import me.vermulst.multibreak.api.MultiBreakAPI;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.multibreak.BreakEvents;
import me.vermulst.multibreak.multibreak.BreakManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;


public final class Main extends JavaPlugin {

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

        MultiBreakAPI.init();

        // config
        if (Config.getInstance().load(this.getConfig())) {
            this.saveConfig();
        }

        // events
        Listener[] events = new Listener[] {
                new BreakEvents(new BreakManager())
        };
        for (Listener event : events) {
            this.getServer().getPluginManager().registerEvents(event, this);
        }

        // commands
        Commands commands = new Commands();
        commands.init();
        commands.register(this);
    }

    @Override
    public void onDisable() {
        Config.getInstance().save(this.getConfig());
        this.saveConfig();
    }
}
