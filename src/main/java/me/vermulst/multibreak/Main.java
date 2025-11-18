package me.vermulst.multibreak;

import me.vermulst.multibreak.api.MultiBreakAPI;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.multibreak.event.BlockDestroyEvents;
import me.vermulst.multibreak.multibreak.event.BreakEvents;
import me.vermulst.multibreak.multibreak.BreakManager;
import me.vermulst.multibreak.multibreak.event.RefreshEvents;
import me.vermulst.multibreak.multibreak.event.SpeedChangeEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Main extends JavaPlugin {

    private static Main INSTANCE;
    private static ExecutorService highPriorityExecutor;

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

        BreakManager breakManager = BreakManager.getInstance();

        // events
        Listener[] events = new Listener[]{
                new BreakEvents(breakManager),
                new RefreshEvents(breakManager),
                new BlockDestroyEvents(breakManager),
                new SpeedChangeEvents(breakManager),
        };
        for (Listener event : events) {
            this.getServer().getPluginManager().registerEvents(event, this);
        }

        // commands
        Commands commands = new Commands();
        commands.init();
        commands.register(this);

        int threads = Math.min(2, Runtime.getRuntime().availableProcessors()/2);
        highPriorityExecutor = Executors.newFixedThreadPool(threads, (Runnable r) -> {
            Thread t = new Thread(r, "MultiBreak-High-Priority-Pool");
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });
    }

    @Override
    public void onDisable() {
        highPriorityExecutor.shutdownNow();
        Config.getInstance().save(this.getConfig());
        this.saveConfig();
    }


    public static ExecutorService getHighPriorityExecutor() {
        return highPriorityExecutor;
    }
}
