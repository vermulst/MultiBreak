package me.vermulst.multibreak.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MultiBreakTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 1 -> {
                completions.add("Width");
            }
            case 2 -> {
                completions.add("Height");
            }
            case 3 -> {
                completions.add("Depth");
            }
            case 4 -> {
                completions.add("Width offset");
            }
            case 5 -> {
                completions.add("Height offset");
            }
            case 6 -> {
                completions.add("Depth offset");
            }
        }
        return completions;
    }

}
