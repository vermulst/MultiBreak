package me.vermulst.multibreak;

import me.vermulst.multibreak.commands.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

import java.util.HashMap;
import java.util.Map;

public class Commands {

    Map<String, CommandExecutor> commands = new HashMap<>();
    Map<String, TabCompleter> tabCompleters = new HashMap<>();

    public void init(Main plugin) {
        commands.put("multibreak", new MultiBreakCommand(plugin));
        tabCompleters.put("multibreak", new MultiBreakTabCompleter());

        commands.put("multipreset", new MultiPresetCommand(plugin));
        tabCompleters.put("multipreset", new MultiPresetTabCompleter(plugin.getConfigManager()));

        commands.put("multireload", new MultiReloadCommand(plugin));
        tabCompleters.put("multireload", new NullTabCompleter());
    }

    public void register(Main plugin) {
        for (String commandName : commands.keySet()) {
            PluginCommand command = plugin.getServer().getPluginCommand(commandName);
            command.setExecutor(commands.get(commandName));
            command.setTabCompleter(tabCompleters.get(commandName));
        }
    }

}
