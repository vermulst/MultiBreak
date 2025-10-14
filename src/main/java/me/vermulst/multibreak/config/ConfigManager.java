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

import java.util.*;

public class ConfigManager {

    private Map<String, Figure> configOptions;
    private Map<Material, String> materialOptions;
    private final EnumSet<Material> ignoredMaterials = EnumSet.noneOf(Material.class);
    private int maxRange = 10;

    private final boolean[] options = new boolean[optionNames.length];
    private static final String[] optionNames = new String[]{"fair_mode", "legacy_mode"};

    private static final String OLD_MATERIAL_PRESETS_PATH = "material_configs";
    private static final String NEW_MATERIAL_PRESETS_PATH = "material_presets";

    private static final String OLD_PRESETS_PATH = "config_options";
    private static final String NEW_PRESETS_PATH = "presets";

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
        List<String>[] optionComments = new List[]{
                List.of("If enabled, blocks that take longer to break than the source, will not be multibroken."),
                List.of("", "If enabled, old, more performance heavy logic will be used to detect block breaks", "however might be more reliable if you see bugs.")
        };

        /** Boolean options **/
        for (int i = 0; i < options.length; i++) {
            String optionName = optionNames[i];
            boolean option = options[i];
            fileConfiguration.set(optionName, option);
            fileConfiguration.setComments(optionName, optionComments[i]);
        }

        /** Presets **/
        fileConfiguration.set(OLD_PRESETS_PATH, null);
        for (Map.Entry<String, Figure> entry : this.getConfigOptions().entrySet()) {
            String name = entry.getKey();
            String path = "presets." + name;
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

        /** Material presets **/
        fileConfiguration.set(OLD_MATERIAL_PRESETS_PATH, null);
        for (Map.Entry<Material, String> entry : materialOptions.entrySet()) {
            fileConfiguration.set("material_presets." + entry.getKey().name(), entry.getValue());
        }

        /** Ignored materials **/
        List<String> materialNames = new ArrayList<>(ignoredMaterials.size());
        ignoredMaterials.forEach(material -> materialNames.add(material.toString()));
        fileConfiguration.set("ignored_materials", materialNames);
        fileConfiguration.setComments("ignored_materials", List.of("", "List of blocks ignored by multibreaks."));

        /** Max range **/
        fileConfiguration.set("max_break_range", this.maxRange);
        fileConfiguration.setComments("max_break_range",
                List.of("", "The maximum range in blocks from which a player can break blocks with multibreak.",
                        "Needed when for example increasing the block range attribute of a player."));
    }

    /** Load the config from file
     *
     * @param fileConfiguration - config
     * @return true if the config file needs to be saved (defaults were inserted)
     */
    public boolean load(FileConfiguration fileConfiguration) {
        this.configOptions = new HashMap<>();
        this.materialOptions = new HashMap<>();
        boolean save1 = this.loadOptions(fileConfiguration);
        this.loadPresets(fileConfiguration);
        this.loadMaterialPresets(fileConfiguration);
        boolean save2 = this.loadIgnoredMaterials(fileConfiguration);
        boolean save3 = this.loadMaxRange(fileConfiguration);
        return save1 || save2 || save3;
    }

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

    private void loadPresets(FileConfiguration fileConfiguration) {
        String path = null;
        if (fileConfiguration.getKeys(false).contains(NEW_PRESETS_PATH)) {
            path = NEW_PRESETS_PATH;
        } else if (fileConfiguration.getKeys(false).contains(OLD_PRESETS_PATH)) {
            path = OLD_PRESETS_PATH;
        }
        if (path == null) return;

        ConfigurationSection section = fileConfiguration.getConfigurationSection(path);
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

    private void loadMaterialPresets(FileConfiguration fileConfiguration) {
        String path = null;

        if (fileConfiguration.getKeys(false).contains(NEW_MATERIAL_PRESETS_PATH)) {
            path = NEW_MATERIAL_PRESETS_PATH;
        } else if (fileConfiguration.getKeys(false).contains(OLD_MATERIAL_PRESETS_PATH)) {
            path = OLD_MATERIAL_PRESETS_PATH;
        }
        if (path == null) return;
        ConfigurationSection section = fileConfiguration.getConfigurationSection(path);
        if (section == null) return;
        for (String itemtype : section.getKeys(false)) {
            String configOption = section.getString(itemtype);
            Material material = Material.valueOf(itemtype);
            this.getMaterialOptions().put(material, configOption);
        }
    }

    private boolean loadIgnoredMaterials(FileConfiguration fileConfiguration) {
        if (fileConfiguration.getKeys(false).contains("ignored_materials")) {
            List<String> materialNames = fileConfiguration.getStringList("ignored_materials");
            for (String matName : materialNames) {
                this.ignoredMaterials.add(Material.getMaterial(matName));
            }
        } else {
            List<String> ignoredMaterials = new ArrayList<>();
            ignoredMaterials.add(Material.BEDROCK.toString());
            fileConfiguration.set("ignored_materials", ignoredMaterials);
            this.ignoredMaterials.add(Material.BEDROCK);
            return true;
        }
        return false;
    }

    private boolean loadMaxRange(FileConfiguration fileConfiguration) {
        if (fileConfiguration.getKeys(false).contains("max_break_range")) {
            this.maxRange = fileConfiguration.getInt("max_break_range");
        } else {
            fileConfiguration.set("max_break_range", this.maxRange);
            return true;
        }
        return false;
    }

    public void updateDeleteConfig(FileConfiguration fileConfiguration, String name) {
        fileConfiguration.set("config_options." + name, null);
        String path = null;
        if (fileConfiguration.getKeys(false).contains(NEW_MATERIAL_PRESETS_PATH)) {
            path = NEW_MATERIAL_PRESETS_PATH;
        } else if (fileConfiguration.getKeys(false).contains(OLD_MATERIAL_PRESETS_PATH)) {
            path = OLD_MATERIAL_PRESETS_PATH;
        }
        if (path == null) return;
        ConfigurationSection section = fileConfiguration.getConfigurationSection(path);
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

    public Map<String, Figure> getConfigOptions() {
        return configOptions;
    }

    public boolean[] getOptions() {
        return options;
    }

    public Map<Material, String> getMaterialOptions() {
        return materialOptions;
    }

    public EnumSet<Material> getIgnoredMaterials() {
        return ignoredMaterials;
    }

    public int getMaxRange() {
        return maxRange;
    }
}
