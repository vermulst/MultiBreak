package me.vermulst.multibreak.item;

import io.papermc.paper.persistence.PersistentDataContainerView;
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


    private static final NamespacedKey KEY_WIDTH    = new NamespacedKey(Main.getInstance(), "width");
    private static final NamespacedKey KEY_HEIGHT   = new NamespacedKey(Main.getInstance(), "height");
    private static final NamespacedKey KEY_DEPTH    = new NamespacedKey(Main.getInstance(), "depth");

    private static final NamespacedKey KEY_WIDTH_ROTATION  = new NamespacedKey(Main.getInstance(), "widthR");
    private static final NamespacedKey KEY_HEIGHT_ROTATION = new NamespacedKey(Main.getInstance(), "heightR");
    private static final NamespacedKey KEY_DEPTH_ROTATION  = new NamespacedKey(Main.getInstance(), "depthR");

    private static final NamespacedKey KEY_WIDTH_OFFSET  = new NamespacedKey(Main.getInstance(), "widthO");
    private static final NamespacedKey KEY_HEIGHT_OFFSET = new NamespacedKey(Main.getInstance(), "heightO");
    private static final NamespacedKey KEY_DEPTH_OFFSET  = new NamespacedKey(Main.getInstance(), "depthO");

    private static final NamespacedKey KEY_TYPE_ID  = new NamespacedKey(Main.getInstance(), "type_id");
    private static final NamespacedKey KEY_FIGURE_INFO = new NamespacedKey(Main.getInstance(), "figure_info");


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
            return FigureItemDataType.this.toPrimitive(figureInfo.figure(), persistentDataAdapterContext);
        }

        @Override
        public @NotNull FigureItemInfo fromPrimitive(PersistentDataContainer persistentDataContainer, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
            Figure figure = FigureItemDataType.this.fromPrimitive(persistentDataContainer, persistentDataAdapterContext);
            return new FigureItemInfo(figure);
        }
    }

    public Figure figure;

    public ItemStack set(ItemStack itemStack, Figure figure) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer customItemTagContainer = itemMeta.getPersistentDataContainer();
        customItemTagContainer.set(KEY_FIGURE_INFO, this, figure);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public Figure get(ItemStack itemStack) {
        PersistentDataContainerView customItemTagContainer = itemStack.getPersistentDataContainer();
        Figure figure = customItemTagContainer.get(KEY_FIGURE_INFO, this);
        if (figure != null) return figure;

        /** Fall back on record type and migrate to new type */
        LegacyFigureItemDataType legacyType = new LegacyFigureItemDataType();
        FigureItemInfo oldInfo = customItemTagContainer.get(KEY_FIGURE_INFO, legacyType);
        if (oldInfo != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta == null) return null;
            figure = oldInfo.figure();

            PersistentDataContainer customItemTagContainerMeta = meta.getPersistentDataContainer();
            customItemTagContainerMeta.remove(KEY_FIGURE_INFO);
            itemStack.setItemMeta(meta);

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

        // The Primitive Type (P) for Figure is PersistentDataContainer, which maps to TAG_CONTAINER.
        return customItemTagContainer.has(KEY_FIGURE_INFO, PersistentDataType.TAG_CONTAINER);
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
        persistentDataContainer.set(KEY_WIDTH, PersistentDataType.INTEGER, figureInfo.getWidth());
        persistentDataContainer.set(KEY_HEIGHT, PersistentDataType.INTEGER, figureInfo.getHeight());
        persistentDataContainer.set(KEY_DEPTH, PersistentDataType.INTEGER, figureInfo.getDepth());
        if (figureInfo.getRotationWidth() != 0) {
            persistentDataContainer.set(KEY_WIDTH_ROTATION, PersistentDataType.SHORT, figureInfo.getRotationWidth());
        }
        if (figureInfo.getRotationHeight() != 0) {
            persistentDataContainer.set(KEY_HEIGHT_ROTATION, PersistentDataType.SHORT, figureInfo.getRotationHeight());
        }
        if (figureInfo.getRotationDepth() != 0) {
            persistentDataContainer.set(KEY_DEPTH_ROTATION, PersistentDataType.SHORT, figureInfo.getRotationDepth());
        }
        if (figureInfo.getOffSetWidth() != 0) {
            persistentDataContainer.set(KEY_WIDTH_OFFSET, PersistentDataType.INTEGER, figureInfo.getOffSetWidth());
        }
        if (figureInfo.getOffSetHeight() != 0) {
            persistentDataContainer.set(KEY_HEIGHT_OFFSET, PersistentDataType.INTEGER, figureInfo.getOffSetHeight());
        }
        if (figureInfo.getOffSetDepth() != 0) {
            persistentDataContainer.set(KEY_DEPTH_OFFSET, PersistentDataType.INTEGER, figureInfo.getOffSetDepth());
        }
        persistentDataContainer.set(KEY_TYPE_ID, PersistentDataType.INTEGER, figureInfo.getFigureType().ordinal());
        return persistentDataContainer;
    }

    @Override
    public @NotNull Figure fromPrimitive(PersistentDataContainer persistentDataContainer, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
        int width = persistentDataContainer.getOrDefault(KEY_WIDTH, PersistentDataType.INTEGER, 3);
        int height = persistentDataContainer.getOrDefault(KEY_HEIGHT, PersistentDataType.INTEGER, 3);
        int depth = persistentDataContainer.getOrDefault(KEY_DEPTH, PersistentDataType.INTEGER, 1);

        int figureTypeOrdinal = persistentDataContainer.getOrDefault(KEY_TYPE_ID, PersistentDataType.INTEGER, 0);
        FigureType figureType = FigureType.values()[figureTypeOrdinal];
        Figure figure = figureType.build(width, height, depth);

        short widthR = persistentDataContainer.getOrDefault(KEY_WIDTH_ROTATION, PersistentDataType.SHORT, (short) 0);
        short heightR = persistentDataContainer.getOrDefault(KEY_HEIGHT_ROTATION, PersistentDataType.SHORT, (short) 0);
        short depthR = persistentDataContainer.getOrDefault(KEY_DEPTH_ROTATION, PersistentDataType.SHORT, (short) 0);
        int widthO = persistentDataContainer.getOrDefault(KEY_WIDTH_OFFSET, PersistentDataType.INTEGER, 0);
        int heightO = persistentDataContainer.getOrDefault(KEY_HEIGHT_OFFSET, PersistentDataType.INTEGER, 0);
        int depthO = persistentDataContainer.getOrDefault(KEY_DEPTH_OFFSET, PersistentDataType.INTEGER, 0);
        figure.setRotations(widthR, heightR, depthR);
        figure.setOffsets(widthO, heightO, depthO);
        figure.updateCachedKey();
        return figure;
    }

    private int getOrDefault(Integer integer, int defaultValue) {
        return integer != null ? integer : defaultValue;
    }

    private boolean fromByte(Byte byteValue, boolean defaultValue) {
        return byteValue != null ? byteValue == 1 : defaultValue;
    }
}
