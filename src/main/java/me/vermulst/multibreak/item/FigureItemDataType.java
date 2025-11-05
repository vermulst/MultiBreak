package me.vermulst.multibreak.item;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureType;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class FigureItemDataType implements PersistentDataType<PersistentDataContainer, Figure> {

    public record FigureItemInfo(Figure figure) {}
    // A separate, temporary type to read the old data.
    private class LegacyFigureItemDataType implements PersistentDataType<PersistentDataContainer, FigureItemInfo> {
        @Override
        public @NotNull Class<PersistentDataContainer> getPrimitiveType() {
            return PersistentDataContainer.class;
        }

        @Override
        public @NotNull Class<FigureItemInfo> getComplexType() {
            return FigureItemInfo.class;
        }

        // NOTE: The toPrimitive and fromPrimitive logic must exactly match
        // the original FigureItemDataType's logic, just using FigureItemInfo.
        @Override
        public @NotNull PersistentDataContainer toPrimitive(FigureItemInfo figureInfo, PersistentDataAdapterContext persistentDataAdapterContext) {
            // Re-using the outer class's toPrimitive logic directly on the Figure
            return FigureItemDataType.this.toPrimitive(figureInfo.figure(), persistentDataAdapterContext);
        }

        @Override
        public @NotNull FigureItemInfo fromPrimitive(PersistentDataContainer persistentDataContainer, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
            // Re-using the outer class's fromPrimitive logic to build the Figure, then wrap it
            Figure figure = FigureItemDataType.this.fromPrimitive(persistentDataContainer, persistentDataAdapterContext);
            return new FigureItemInfo(figure);
        }
    }

    public Figure figure;

    public ItemStack set(ItemStack itemStack, Figure figure) {
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "figure_info");
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer customItemTagContainer = itemMeta.getPersistentDataContainer();
        customItemTagContainer.set(key, this, figure);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public Figure get(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer customItemTagContainer = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "figure_info");

        Figure figure = customItemTagContainer.get(key, this);
        if (figure != null) return figure;

        /** Fall back on record type and migrate to new type */
        LegacyFigureItemDataType legacyType = new LegacyFigureItemDataType();
        FigureItemInfo oldInfo = customItemTagContainer.get(key, legacyType);
        if (oldInfo != null) {
            figure = oldInfo.figure();
            customItemTagContainer.remove(key);
            this.set(itemStack, figure);
            return figure;
        }

        return null;
    }

    public ItemStack remove(ItemStack itemStack) {
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "figure_info");
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer customItemTagContainer = itemMeta.getPersistentDataContainer();
        customItemTagContainer.remove(key);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean has(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer customItemTagContainer = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "figure_info");

        // The Primitive Type (P) for Figure is PersistentDataContainer, which maps to TAG_CONTAINER.
        return customItemTagContainer.has(key, PersistentDataType.TAG_CONTAINER);
    }

    @Override
    public @NotNull Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    public @NotNull Class<Figure> getComplexType() {
        return Figure.class;
    }

    @Override
    public @NotNull PersistentDataContainer toPrimitive(Figure figureInfo, PersistentDataAdapterContext persistentDataAdapterContext) {
        PersistentDataContainer persistentDataContainer = persistentDataAdapterContext.newPersistentDataContainer();
        persistentDataContainer.set(key("width"), PersistentDataType.INTEGER, figureInfo.getWidth());
        persistentDataContainer.set(key("height"), PersistentDataType.INTEGER, figureInfo.getHeight());
        persistentDataContainer.set(key("depth"), PersistentDataType.INTEGER, figureInfo.getDepth());
        if (figureInfo.getRotationWidth() != 0) {
            persistentDataContainer.set(key("widthR"), PersistentDataType.SHORT, figureInfo.getRotationWidth());
        }
        if (figureInfo.getRotationHeight() != 0) {
            persistentDataContainer.set(key("heightR"), PersistentDataType.SHORT, figureInfo.getRotationHeight());
        }
        if (figureInfo.getRotationDepth() != 0) {
            persistentDataContainer.set(key("depthR"), PersistentDataType.SHORT, figureInfo.getRotationDepth());
        }
        if (figureInfo.getOffSetWidth() != 0) {
            persistentDataContainer.set(key("widthO"), PersistentDataType.INTEGER, figureInfo.getOffSetWidth());
        }
        if (figureInfo.getOffSetHeight() != 0) {
            persistentDataContainer.set(key("heightO"), PersistentDataType.INTEGER, figureInfo.getOffSetHeight());
        }
        if (figureInfo.getOffSetDepth() != 0) {
            persistentDataContainer.set(key("depthO"), PersistentDataType.INTEGER, figureInfo.getOffSetDepth());
        }
        persistentDataContainer.set(key("type_id"), PersistentDataType.INTEGER, figureInfo.getFigureType().ordinal());
        //todo: add more options
        return persistentDataContainer;
    }

    @Override
    public @NotNull Figure fromPrimitive(PersistentDataContainer persistentDataContainer, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
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
        return figure;
    }



    private NamespacedKey key(String key) {
        return new NamespacedKey(Main.getInstance(), key);
    }

    private int getOrDefault(Integer integer, int defaultValue) {
        return integer != null ? integer : defaultValue;
    }

    private boolean fromByte(Byte byteValue, boolean defaultValue) {
        return byteValue != null ? byteValue == 1 : defaultValue;
    }
}
