package me.vermulst.multibreak;

import me.vermulst.multibreak.commands.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class Commands {

    Map<String, CommandExecutor> commands = new HashMap<>();
    Map<String, TabCompleter> tabCompleters = new HashMap<>();

    public void init() {
        commands.put("multibreak", new MultiBreakCommand());
        tabCompleters.put("multibreak", new MultiBreakTabCompleter());

        commands.put("multipreset", new MultiPresetCommand());
        tabCompleters.put("multipreset", new MultiPresetTabCompleter());

        commands.put("multireload", new MultiReloadCommand());
        tabCompleters.put("multireload", new NullTabCompleter());
    }

    public void register(Plugin plugin) {
        for (String commandName : commands.keySet()) {
            PluginCommand command = plugin.getServer().getPluginCommand(commandName);
            command.setExecutor(commands.get(commandName));
            command.setTabCompleter(tabCompleters.get(commandName));
        }
    }

}
