package me.vermulst.multibreak.commands;

import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.config.ConfigOption;
import me.vermulst.multibreak.figure.types.FigureType;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MultiConfigTabCompleter implements TabCompleter {

    private final ConfigManager configManager;
    public MultiConfigTabCompleter(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        ArrayList<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1 -> {
                completions.add("apply");
                completions.add("create");
                completions.add("reload");
                completions.add("delete");
            }
            case 2 -> {
                switch (args[0]) {
                    case "apply", "delete" -> {
                        completions.addAll(this.getConfigManager().getConfigOptions().keySet());
                    }
                    case "create" -> {
                        completions.add("name");
                    }
                }
            }
            case 3 -> {
                switch (args[0]) {
                    case "apply" -> {
                        completions.add("holding");
                        completions.add("itemtype");
                    }
                    case "create" -> {
                        for (FigureType figureType : FigureType.values()) {
                            completions.add(figureType.name());
                        }
                    }
                }
            }
            case 4 -> {
                switch (args[0]) {
                    case "apply" -> {
                        if ("itemtype".equals(args[2])) {
                            for (Material material : Material.values()) {
                                if (material.name().toLowerCase().contains(args[3].toLowerCase())) {
                                    completions.add(material.name());
                                }
                            }
                        }
                    }
                    case "create" -> {
                        completions.add("width");
                    }
                }
            }
            case 5 -> {
                if ("create".equals(args[0])) {
                    completions.add("Height");
                }
            }
            case 6 -> {
                if ("create".equals(args[0])) {
                    completions.add("Depth");
                }
            }
            case 7 -> {
                if ("create".equals(args[0])) {
                    completions.add("Width offset");
                }
            }
            case 8 -> {
                if ("create".equals(args[0])) {
                    completions.add("Height offset");
                }
            }
            case 9 -> {
                if ("create".equals(args[0])) {
                    completions.add("Depth offset");
                }
            }
        }

        return completions;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
