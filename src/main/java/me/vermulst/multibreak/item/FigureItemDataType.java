package me.vermulst.multibreak.item;

import me.vermulst.multibreak.figure.Figure;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

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

    @Override
    public Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    public Class<FigureItemInfo> getComplexType() {
        return FigureItemInfo.class;
    }

    @Override
    public PersistentDataContainer toPrimitive(FigureItemInfo figureInfo, PersistentDataAdapterContext persistentDataAdapterContext) {
        PersistentDataContainer persistentDataContainer = persistentDataAdapterContext.newPersistentDataContainer();
        persistentDataContainer.set(key("width"), PersistentDataType.INTEGER, figureInfo.figure().getWidth());
        persistentDataContainer.set(key("height"), PersistentDataType.INTEGER, figureInfo.figure().getHeight());
        persistentDataContainer.set(key("depth"), PersistentDataType.INTEGER, figureInfo.figure().getDepth());
        persistentDataContainer.set(key("widthO"), PersistentDataType.INTEGER, figureInfo.figure().getOffSetWidth());
        persistentDataContainer.set(key("heightO"), PersistentDataType.INTEGER, figureInfo.figure().getOffSetHeight());
        persistentDataContainer.set(key("depthO"), PersistentDataType.INTEGER, figureInfo.figure().getOffSetDepth());
        //todo: add more options
        return persistentDataContainer;
    }

    @Override
    public FigureItemInfo fromPrimitive(PersistentDataContainer persistentDataContainer, PersistentDataAdapterContext persistentDataAdapterContext) {
        int width = persistentDataContainer.get(key("width"), PersistentDataType.INTEGER);
        int height = persistentDataContainer.get(key("height"), PersistentDataType.INTEGER);
        int depth = persistentDataContainer.get(key("depth"), PersistentDataType.INTEGER);
        Figure figure = new Figure(width,height,depth);
        int widthO = persistentDataContainer.get(key("widthO"), PersistentDataType.INTEGER);
        int heightO = persistentDataContainer.get(key("heightO"), PersistentDataType.INTEGER);
        int depthO = persistentDataContainer.get(key("depthO"), PersistentDataType.INTEGER);
        figure.setOffsets(widthO, heightO, depthO);
        FigureItemInfo figureItemInfo = new FigureItemInfo(figure);
        return figureItemInfo;
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
