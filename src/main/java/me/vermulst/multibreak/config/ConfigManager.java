package me.vermulst.multibreak.config;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private HashMap<String, Figure> configOptions;
    private HashMap<Material, String> materialOptions;

    private final boolean[] options = new boolean[optionNames.length];
    private static final String[] optionNames = new String[]{"fair_mode", "legacy_mode"};

    public Inventory getMenu() {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("Configurations"));
        int index = 0;
        for (Map.Entry<String, Figure> entry : configOptions.entrySet()) {
            String name = entry.getKey();
            Figure figure = entry.getValue();
            ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(name));
            meta.lore(figure.getLore());
            item.setItemMeta(meta);
            inventory.setItem(index, item);
            index++;
        }
        return inventory;
    }

    public void save(FileConfiguration fileConfiguration) {
        for (int i = 0; i < options.length; i++) {
            String optionName = optionNames[i];
            boolean option = options[i];
            fileConfiguration.set(optionName, option);
        }
        for (Map.Entry<String, Figure> entry : this.getConfigOptions().entrySet()) {
            String name = entry.getKey();
            String path = "config_options." + name;
            String figurePath = path + ".figure.";
            Figure figure = entry.getValue();
            fileConfiguration.set(figurePath + "type", figure.getFigureType().name());
            fileConfiguration.set(figurePath + "width", figure.getWidth());
            fileConfiguration.set(figurePath + "height", figure.getHeight());
            fileConfiguration.set(figurePath + "depth", figure.getDepth());

            fileConfiguration.set(figurePath + "width_rotation", figure.getRotationWidth());
            fileConfiguration.set(figurePath + "height_rotation", figure.getRotationHeight());
            fileConfiguration.set(figurePath + "depth_rotation", figure.getRotationDepth());

            fileConfiguration.set(figurePath + "width_offset", figure.getOffSetWidth());
            fileConfiguration.set(figurePath + "height_offset", figure.getOffSetHeight());
            fileConfiguration.set(figurePath + "depth_offset", figure.getOffSetDepth());
        }
        for (Map.Entry<Material, String> entry : materialOptions.entrySet()) {
            fileConfiguration.set("material_configs." + entry.getKey().name(), entry.getValue());
        }
    }

    // first element is whether to save or not
    public boolean loadOptions(FileConfiguration fileConfiguration) {
        boolean save = false;
        int index = 0;
        for (String option : optionNames) {
            if (fileConfiguration.getKeys(false).contains(option)) {
                this.options[index] = fileConfiguration.getBoolean(option);
            } else {
                save = true;
                this.options[index] = false;
                fileConfiguration.set(option, false);
            }
            index++;
        }
        return save;
    }

    public boolean load(FileConfiguration fileConfiguration) {
        this.configOptions = new HashMap<>();
        this.materialOptions = new HashMap<>();

        boolean save = this.loadOptions(fileConfiguration);

        if (fileConfiguration.getKeys(false).contains("config_options")) {
            ConfigurationSection section = fileConfiguration.getConfigurationSection("config_options");
            for (String name : section.getKeys(false)) {
                ConfigurationSection section1 = section.getConfigurationSection(name).getConfigurationSection("figure");
                if (section1 == null) continue;
                FigureType figureType = FigureType.valueOf(section1.getString("type"));
                int width = section1.getInt("width");
                int height = section1.getInt("height");
                int depth = section1.getInt("depth");

                short width_rotation = (short) section1.getInt("width_rotation");
                short height_rotation = (short) section1.getInt("height_rotation");
                short depth_rotation = (short) section1.getInt("depth_rotation");

                int width_offset = section1.getInt("width_offset");
                int height_offset = section1.getInt("height_offset");
                int depth_offset = section1.getInt("depth_offset");
                Figure figure = figureType.build(width, height, depth);
                figure.setRotations(width_rotation, height_rotation, depth_rotation);
                figure.setOffsets(width_offset, height_offset, depth_offset);
                this.getConfigOptions().put(name, figure);
            }
        }
        if (fileConfiguration.getKeys(false).contains("material_configs")) {
            ConfigurationSection section = fileConfiguration.getConfigurationSection("material_configs");
            for (String itemtype : section.getKeys(false)) {
                String configOption = section.getString(itemtype);
                Material material = Material.valueOf(itemtype);
                this.getMaterialOptions().put(material, configOption);
            }
        }
        return save;
    }

    public void updateDeleteConfig(FileConfiguration fileConfiguration, String name) {
        fileConfiguration.set("config_options." + name, null);
        ConfigurationSection section = fileConfiguration.getConfigurationSection("material_configs");
        if (section == null) return;
        for (String materialName : section.getKeys(false)) {
            if (name.equals(section.get(materialName))) {
                section.set(materialName, null);
                this.getMaterialOptions().remove(Material.valueOf(materialName));
            }
        }
    }



    public void updateDeleteMaterial(FileConfiguration fileConfiguration, Material material) {
        fileConfiguration.set("material_configs." + material.name(), null);
    }

    public HashMap<String, Figure> getConfigOptions() {
        return configOptions;
    }

    public boolean[] getOptions() {
        return options;
    }

    public HashMap<Material, String> getMaterialOptions() {
        return materialOptions;
    }
}
