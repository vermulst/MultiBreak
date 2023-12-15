package me.vermulst.multibreak.config;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private HashMap<String, Figure> configOptions;
    private HashMap<Material, String> materialOptions;

    public void save(FileConfiguration fileConfiguration) {
        for (Map.Entry<String, Figure> entry : this.getConfigOptions().entrySet()) {
            String name = entry.getKey();
            String path = "config_options." + name;
            String figurePath = path + ".figure.";
            Figure figure = entry.getValue();
            fileConfiguration.set(figurePath + "type", figure.getFigureType().name());
            fileConfiguration.set(figurePath + "width", figure.getWidth());
            fileConfiguration.set(figurePath + "height", figure.getHeight());
            fileConfiguration.set(figurePath + "depth", figure.getDepth());
            fileConfiguration.set(figurePath + "width_offset", figure.getOffSetWidth());
            fileConfiguration.set(figurePath + "height_offset", figure.getOffSetHeight());
            fileConfiguration.set(figurePath + "depth_offset", figure.getOffSetDepth());
        }
        for (Map.Entry<Material, String> entry : materialOptions.entrySet()) {
            fileConfiguration.set("material_configs." + entry.getKey().name(), entry.getValue());
        }
    }

    public void load(FileConfiguration fileConfiguration) {
        this.configOptions = new HashMap<>();
        this.materialOptions = new HashMap<>();
        if (fileConfiguration.getKeys(false).contains("config_options")) {
            ConfigurationSection section = fileConfiguration.getConfigurationSection("config_options");
            for (String name : section.getKeys(false)) {
                ConfigurationSection section1 = section.getConfigurationSection(name).getConfigurationSection("figure");
                if (section1 == null) continue;
                FigureType figureType = FigureType.valueOf(section1.getString("type"));
                int width = section1.getInt("width");
                int height = section1.getInt("height");
                int depth = section1.getInt("depth");
                int width_offset = section1.getInt("width_offset");
                int height_offset = section1.getInt("height_offset");
                int depth_offset = section1.getInt("depth_offset");
                Figure figure = figureType.build(width, height, depth);
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

    public HashMap<Material, String> getMaterialOptions() {
        return materialOptions;
    }
}
