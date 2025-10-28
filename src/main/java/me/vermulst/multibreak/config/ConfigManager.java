package me.vermulst.multibreak.config;

import me.vermulst.multibreak.Main;
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
import java.util.logging.Level;
import java.util.stream.Stream;

public class ConfigManager {

    private Map<String, Figure> configOptions;
    private Map<Material, String> materialOptions;
    private final EnumSet<Material> includedMaterials = EnumSet.noneOf(Material.class);
    private final EnumSet<Material> ignoredMaterials = EnumSet.noneOf(Material.class);
    private int maxRange = 10;
    private int fairModeTicksLeeway = 1;

    private final boolean[] options = new boolean[] {
            false,  // legacy mode
            true // fair mode
    };
    private static final String[] optionNames = new String[]{"legacy_mode", "fair_mode"};

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

        /** Max range **/
        fileConfiguration.set("fair_mode_leeway", this.fairModeTicksLeeway);
        fileConfiguration.setComments("fair_mode_leeway",
                List.of("", "The amount of ticks which will count as being in range within fair mode.",
                        "For example, when set to 1, blocks that would take 5 ticks to break while the source block takes 6 ticks, will still be broken.",
                        "1 is the recommended value."));

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

        /** Included materials **/
        List<String> includedMaterialNames = new ArrayList<>(includedMaterials.size());
        includedMaterials.forEach(material -> includedMaterialNames.add(material.toString()));
        fileConfiguration.set("included_materials", includedMaterialNames);
        fileConfiguration.setComments("included_materials", List.of("", "List of block types strictly included by multibreaks."
        , "if empty, all block types count"));

        /** Ignored materials **/
        List<String> ignoredMaterialNames = new ArrayList<>(ignoredMaterials.size());
        ignoredMaterials.forEach(material -> ignoredMaterialNames.add(material.toString()));
        fileConfiguration.set("ignored_materials", ignoredMaterialNames);
        fileConfiguration.setComments("ignored_materials", List.of("", "List of block types ignored by multibreaks."));

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
        boolean save = this.loadOptions(fileConfiguration);
        save = save || this.loadPresets(fileConfiguration);
        save = save || this.loadMaterialPresets(fileConfiguration);
        save = save || this.loadIncludedMaterials(fileConfiguration);
        save = save || this.loadIgnoredMaterials(fileConfiguration);
        save = save || this.loadMaxRange(fileConfiguration);
        save = save || this.loadFairModeLeeway(fileConfiguration);
        if (save) this.save(fileConfiguration);
        return save;
    }

    public boolean loadOptions(FileConfiguration fileConfiguration) {
        boolean save = false;
        int index = 0;
        for (String option : optionNames) {
            if (fileConfiguration.getKeys(false).contains(option)) {
                this.options[index] = fileConfiguration.getBoolean(option);
            } else {
                save = true;
            }
            index++;
        }
        return save;
    }

    private boolean loadPresets(FileConfiguration fileConfiguration) {
        String path = null;
        boolean save = false;
        if (fileConfiguration.getKeys(false).contains(NEW_PRESETS_PATH)) {
            path = NEW_PRESETS_PATH;
        } else if (fileConfiguration.getKeys(false).contains(OLD_PRESETS_PATH)) {
            path = OLD_PRESETS_PATH;
            save = true;
        }
        if (path == null) return save;

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
        return save;
    }

    private boolean loadMaterialPresets(FileConfiguration fileConfiguration) {
        String path = null;
        boolean save = false;
        if (fileConfiguration.getKeys(false).contains(NEW_MATERIAL_PRESETS_PATH)) {
            path = NEW_MATERIAL_PRESETS_PATH;
        } else if (fileConfiguration.getKeys(false).contains(OLD_MATERIAL_PRESETS_PATH)) {
            path = OLD_MATERIAL_PRESETS_PATH;
            save = true;
        }
        if (path == null) return save;
        ConfigurationSection section = fileConfiguration.getConfigurationSection(path);
        if (section == null) return save;
        for (String itemtype : section.getKeys(false)) {
            String configOption = section.getString(itemtype);
            Material material = Material.valueOf(itemtype);
            this.getMaterialOptions().put(material, configOption);
        }
        return save;
    }

    private boolean loadIncludedMaterials(FileConfiguration fileConfiguration) {
        List<String> defaultIgnoredNames = new ArrayList<>();

        boolean save = setIfMissing(fileConfiguration, "included_materials", defaultIgnoredNames);
        this.includedMaterials.clear();
        List<String> materialNames = fileConfiguration.getStringList("included_materials");
        for (String matName : materialNames) {
            Material mat = Material.getMaterial(matName);
            if (mat == null) continue;
            this.includedMaterials.add(mat);
        }
        return save;
    }

    private boolean loadIgnoredMaterials(FileConfiguration fileConfiguration) {
        List<String> defaultIgnoredNames = Stream.of(Material.BEDROCK)
                .map(Enum::toString)
                .toList();

        boolean save = setIfMissing(fileConfiguration, "ignored_materials", defaultIgnoredNames);
        this.ignoredMaterials.clear();
        List<String> materialNames = fileConfiguration.getStringList("ignored_materials");
        for (String matName : materialNames) {
            Material mat = Material.getMaterial(matName);
            if (mat == null) continue;
            this.ignoredMaterials.add(mat);
        }
        return save;
    }


    private boolean loadMaxRange(FileConfiguration fileConfiguration) {
        boolean save = setIfMissing(fileConfiguration, "max_break_range", this.maxRange);
        if (!save) this.maxRange = fileConfiguration.getInt("max_break_range");
        return save;
    }

    private boolean loadFairModeLeeway(FileConfiguration fileConfiguration) {
        boolean save = setIfMissing(fileConfiguration, "fair_mode_leeway", this.maxRange);
        if (!save) this.fairModeTicksLeeway = fileConfiguration.getInt("fair_mode_leeway");
        return save;
    }

    private boolean setIfMissing(ConfigurationSection config, String key, Object defaultValue) {
        if (!config.getKeys(false).contains(key)) {
            config.set(key, defaultValue);
            return true;
        }
        return false;
    }

    public void updateDeletePreset(FileConfiguration fileConfiguration, String name) {
        fileConfiguration.set(OLD_PRESETS_PATH + "." + name, null);
        fileConfiguration.set(NEW_PRESETS_PATH + "." + name, null);
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

    public Map<String, Figure> getConfigOptions() {
        return configOptions;
    }

    public boolean[] getOptions() {
        return options;
    }

    public Map<Material, String> getMaterialOptions() {
        return materialOptions;
    }

    public EnumSet<Material> getIncludedMaterials() {
        return includedMaterials;
    }

    public EnumSet<Material> getIgnoredMaterials() {
        return ignoredMaterials;
    }

    public int getMaxRange() {
        return maxRange;
    }

    public int getFairModeTicksLeeway() {
        return fairModeTicksLeeway;
    }
}
