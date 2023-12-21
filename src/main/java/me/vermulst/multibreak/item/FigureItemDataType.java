package me.vermulst.multibreak.item;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureType;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class FigureItemDataType implements PersistentDataType<PersistentDataContainer, FigureItemInfo> {

    private final Plugin javaPlugin;

    public FigureItemDataType(Plugin javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    public ItemStack set(ItemStack itemStack, FigureItemInfo figureItemInfo) {
        NamespacedKey key = new NamespacedKey(this.javaPlugin, "figure_info");
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer customItemTagContainer = itemMeta.getPersistentDataContainer();
        customItemTagContainer.set(key, this, figureItemInfo);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public FigureItemInfo get(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        NamespacedKey key = new NamespacedKey(this.javaPlugin, "figure_info");
        PersistentDataContainer customItemTagContainer = meta.getPersistentDataContainer();
        return customItemTagContainer.get(key, this);
    }

    public ItemStack remove(ItemStack itemStack) {
        NamespacedKey key = new NamespacedKey(this.javaPlugin, "figure_info");
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer customItemTagContainer = itemMeta.getPersistentDataContainer();
        customItemTagContainer.remove(key);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @Override
    public @NotNull Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    public @NotNull Class<FigureItemInfo> getComplexType() {
        return FigureItemInfo.class;
    }

    @Override
    public @NotNull PersistentDataContainer toPrimitive(FigureItemInfo figureInfo, PersistentDataAdapterContext persistentDataAdapterContext) {
        PersistentDataContainer persistentDataContainer = persistentDataAdapterContext.newPersistentDataContainer();
        persistentDataContainer.set(key("width"), PersistentDataType.INTEGER, figureInfo.figure().getWidth());
        persistentDataContainer.set(key("height"), PersistentDataType.INTEGER, figureInfo.figure().getHeight());
        persistentDataContainer.set(key("depth"), PersistentDataType.INTEGER, figureInfo.figure().getDepth());
        if (figureInfo.figure().getRotationWidth() != 0) {
            persistentDataContainer.set(key("widthR"), PersistentDataType.SHORT, figureInfo.figure().getRotationWidth());
        }
        if (figureInfo.figure().getRotationHeight() != 0) {
            persistentDataContainer.set(key("heightR"), PersistentDataType.SHORT, figureInfo.figure().getRotationHeight());
        }
        if (figureInfo.figure().getRotationDepth() != 0) {
            persistentDataContainer.set(key("depthR"), PersistentDataType.SHORT, figureInfo.figure().getRotationDepth());
        }
        if (figureInfo.figure().getOffSetWidth() != 0) {
            persistentDataContainer.set(key("widthO"), PersistentDataType.INTEGER, figureInfo.figure().getOffSetWidth());
        }
        if (figureInfo.figure().getOffSetHeight() != 0) {
            persistentDataContainer.set(key("heightO"), PersistentDataType.INTEGER, figureInfo.figure().getOffSetHeight());
        }
        if (figureInfo.figure().getOffSetDepth() != 0) {
            persistentDataContainer.set(key("depthO"), PersistentDataType.INTEGER, figureInfo.figure().getOffSetDepth());
        }
        persistentDataContainer.set(key("type_id"), PersistentDataType.INTEGER, figureInfo.figure().getFigureType().ordinal());
        //todo: add more options
        return persistentDataContainer;
    }

    @Override
    public @NotNull FigureItemInfo fromPrimitive(PersistentDataContainer persistentDataContainer, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
        int width = persistentDataContainer.getOrDefault(key("width"), PersistentDataType.INTEGER, 3);
        int height = persistentDataContainer.getOrDefault(key("height"), PersistentDataType.INTEGER, 3);
        int depth = persistentDataContainer.getOrDefault(key("depth"), PersistentDataType.INTEGER, 1);

        int figureTypeOrdinal = persistentDataContainer.getOrDefault(key("type_id"), PersistentDataType.INTEGER, 0);
        FigureType figureType = FigureType.values()[figureTypeOrdinal];
        Figure figure = figureType.build(width, height, depth);

        short widthR = persistentDataContainer.getOrDefault(key("widthR"), PersistentDataType.SHORT, (short) 0);
        short heightR = persistentDataContainer.getOrDefault(key("heightR"), PersistentDataType.SHORT, (short) 0);
        short depthR = persistentDataContainer.getOrDefault(key("depthR"), PersistentDataType.SHORT, (short) 0);
        int widthO = persistentDataContainer.getOrDefault(key("widthO"), PersistentDataType.INTEGER, 0);
        int heightO = persistentDataContainer.getOrDefault(key("heightO"), PersistentDataType.INTEGER, 0);
        int depthO = persistentDataContainer.getOrDefault(key("depthO"), PersistentDataType.INTEGER, 0);
        figure.setRotations(widthR, heightR, depthR);
        figure.setOffsets(widthO, heightO, depthO);
        return new FigureItemInfo(figure);
    }



    private NamespacedKey key(String key) {
        return new NamespacedKey(javaPlugin, key);
    }

    private int getOrDefault(Integer integer, int defaultValue) {
        return integer != null ? integer : defaultValue;
    }

    private boolean fromByte(Byte byteValue, boolean defaultValue) {
        return byteValue != null ? byteValue == 1 : defaultValue;
    }
}
