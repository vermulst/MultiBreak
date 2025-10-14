package me.vermulst.multibreak.commands;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureType;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;


public class MultiPresetCommand implements CommandExecutor {

    private final Main plugin;
    public MultiPresetCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            if (args.length < 1) {
                return this.enterValidOption(p);
            }
            String option = args[0];
            switch (option) {
                case "apply" -> {
                    return this.applyConfig(p, args);
                }
                case "create" -> {
                    return this.createConfig(p, args);
                }
                case "delete" -> {
                    return this.deleteConfig(p, args);
                }
                case "menu" -> {
                    return this.openMenu(p);
                }
                default -> {
                    return this.enterValidOption(p);
                }
            }
        }
        return false;
    }

    public boolean openMenu(@NotNull Player p) {
        Inventory inventory = this.getConfigManager().getMenu();
        p.openInventory(inventory);
        return true;
    }

    public boolean deleteConfig(@NotNull Player p, @NotNull String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Enter a config name").color(TextColor.color(255, 85, 85)));
            return true;
        }
        String configName = args[1];
        Figure figure = this.getConfigManager().getConfigOptions().get(configName);
        this.getConfigManager().getConfigOptions().remove(configName);
        FigureMessages.sendDeleteMessage(p, figure, configName);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        this.getPlugin().getConfigManager().updateDeleteConfig(this.getPlugin().getConfig(), configName);
        this.getPlugin().saveConfig();
        return true;
    }

    public boolean createConfig(@NotNull Player p, @NotNull String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Enter a config name").color(TextColor.color(255, 85, 85)));
            return true;
        }
        String configName = args[1];
        if (args.length < 3) {
            return this.enterValidOption(p);
        }
        FigureType figureType;
        try {
            figureType = FigureType.valueOf(args[2]);
        } catch (Exception e) {
            return this.enterValidOption(p);
        }
        if (args.length < 6) {
            p.sendMessage(Component.text("Please enter sizes").color(TextColor.color(255, 85, 85)));
            return true;
        }
        int[] sizes = this.getSizes(p, args, 3);
        int width = sizes[0];
        int height = sizes[1];
        int depth = sizes[2];
        if (width < 1 || height < 1 || depth < 1) {
            p.sendMessage(Component.text("Please enter valid width, height and depth").color(TextColor.color(255, 85, 85)));
            return true;
        }
        int[] rotations = this.getArgs(p, args, 6);
        int[] offsets = this.getArgs(p, args, 9);
        Figure figure = figureType.build(width, height, depth);
        figure.setRotations((short) rotations[0], (short) rotations[1], (short) rotations[2]);
        figure.setOffsets(offsets[0], offsets[1], offsets[2]);

        this.getConfigManager().getConfigOptions().put(configName, figure);
        FigureMessages.sendCreateMessage(p, figure, configName);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        this.getPlugin().getConfigManager().save(this.getPlugin().getConfig());
        this.getPlugin().saveConfig();
        return true;
    }


    public boolean applyConfig(@NotNull Player p, @NotNull String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("That preset does not exist").color(TextColor.color(255, 85, 85)));
            return true;
        }
        String configOptionName = args[1];
        if (args.length < 3 || !this.getConfigManager().getConfigOptions().containsKey(configOptionName)) {
            return this.enterValidOption(p);
        }
        String applyTo = args[2];
        Figure figure = this.getConfigManager().getConfigOptions().get(configOptionName);
        FigureItemDataType.FigureItemInfo figureItemInfo = new FigureItemDataType.FigureItemInfo(figure);
        FigureItemDataType figureItemDataType = new FigureItemDataType(this.getPlugin());
        if ("holding".equals(applyTo)) {
            ItemStack item = p.getInventory().getItemInMainHand();
            item = figureItemDataType.set(item, figureItemInfo);
            p.getInventory().setItemInMainHand(item);
            FigureMessages.sendApplyMessage(p, figure, false, item.getType());
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            return true;
        } else if ("itemtype".equals(applyTo)) {
            if (args.length < 4) {
                p.sendMessage(Component.text("Please enter a valid item type").color(TextColor.color(255, 85, 85)));
                return true;
            }
            Material material = Material.valueOf(args[3]);
            this.getConfigManager().getMaterialOptions().put(material, configOptionName);
            FigureMessages.sendApplyMessage(p, figure, true, material);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            this.getPlugin().getConfigManager().save(this.getPlugin().getConfig());
            this.getPlugin().saveConfig();
            return true;
        } else {
            return this.enterValidOption(p);
        }
    }


    public int[] getSizes(Player p, @NotNull String[] args, int firstIndex) {
        int[] sizes = new int[]{0, 0, 0};
        try {
            sizes[0] = Integer.parseInt(args[firstIndex]);
            sizes[1] = Integer.parseInt(args[firstIndex + 1]);
            sizes[2] = Integer.parseInt(args[firstIndex + 2]);
            return sizes;
        } catch(Exception e) {
            p.sendMessage(Component.text("Please enter valid width, height and depth").color(TextColor.color(255, 85, 85)));
            return sizes;
        }
    }


    public int[] getArgs(Player p, @NotNull String[] args, int firstIndex) {
        int[] result = new int[]{0, 0, 0};
        if (args.length > firstIndex) {
            try {
                result[0] = Integer.parseInt(args[firstIndex]);
            } catch (Exception e) {
                p.sendMessage(Component.text("Not a valid offset for Width").color(TextColor.color(255, 85, 85)));
                return result;
            }
        }
        if (args.length > firstIndex + 1) {
            try {
                result[1] = Integer.parseInt(args[firstIndex + 1]);
            } catch (Exception e) {
                p.sendMessage(Component.text("Not a valid offset for Height").color(TextColor.color(255, 85, 85)));
                return result;
            }
        }
        if (args.length > firstIndex + 2) {
            try {
                result[2] = Integer.parseInt(args[firstIndex + 2]);
            } catch (Exception e) {
                p.sendMessage(Component.text("Not a valid offset for Depth").color(TextColor.color(255, 85, 85)));
                return result;
            }
        }
        return result;
    }

    public boolean enterValidOption(Player p) {
        p.sendMessage(Component.text("Enter a valid option").color(TextColor.color(255, 85, 85)));
        return true;
    }

    public ConfigManager getConfigManager() {
        return this.getPlugin().getConfigManager();
    }

    public Main getPlugin() {
        return plugin;
    }
}
