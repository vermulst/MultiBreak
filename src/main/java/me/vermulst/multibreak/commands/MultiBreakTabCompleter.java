package me.vermulst.multibreak.commands;

import me.vermulst.multibreak.figure.types.FigureType;
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
                for (FigureType figureType : FigureType.values()) {
                    completions.add(figureType.name());
                }
            }
            case 2 -> {
                completions.add("Width");
            }
            case 3 -> {
                completions.add("Height");
            }
            case 4 -> {
                completions.add("Depth");
            }
            case 5 -> {
                completions.add("Width offset");
            }
            case 6 -> {
                completions.add("Height offset");
            }
            case 7 -> {
                completions.add("Depth offset");
            }
        }
        return completions;
    }

}
