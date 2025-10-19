package me.vermulst.multibreak.api;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.item.FigureItemDataType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MultiBreakAPI {


    private static FigureItemDataType DATA_TYPE_INSTANCE;

    private MultiBreakAPI() {
    }

    public static void init(@NotNull Plugin plugin) {
        if (DATA_TYPE_INSTANCE != null) {
            throw new IllegalStateException("MultiBreakAPI already initialized!");
        }
        // Instantiate FigureItemDataType ONLY ONCE here
        DATA_TYPE_INSTANCE = new FigureItemDataType(plugin);
    }

    /**
     * Sets a Figure onto an ItemStack, returning the modified ItemStack.
     *
     * @param itemStack The ItemStack to modify.
     * @param figure The Figure to apply.
     * @return The modified ItemStack with the Figure data.
     */
    @NotNull
    public static ItemStack setFigure(@NotNull ItemStack itemStack, @NotNull Figure figure) {
        if (DATA_TYPE_INSTANCE == null) {
            throw new IllegalStateException("MultiBreakAPI not initialized. Load MultiBreak before.");
        }
        return DATA_TYPE_INSTANCE.set(itemStack, figure);
    }

    /**
     * Retrieves the Figure associated with an ItemStack.
     *
     * @param itemStack The ItemStack to check.
     * @return The Figure if present and valid, otherwise null.
     */
    @Nullable
    public static Figure getFigure(@NotNull ItemStack itemStack) {
        if (DATA_TYPE_INSTANCE == null) {
            throw new IllegalStateException("MultiBreakAPI not initialized. Load MultiBreak before.");
        }
        return DATA_TYPE_INSTANCE.get(itemStack);
    }

    /**
     * Removes the Figure data from an ItemStack, returning the modified ItemStack.
     *
     * @param itemStack The ItemStack to modify.
     * @return The modified ItemStack without the Figure data.
     */
    @NotNull
    public static ItemStack removeFigure(@NotNull ItemStack itemStack) {
        if (DATA_TYPE_INSTANCE == null) {
            throw new IllegalStateException("MultiBreakAPI not initialized. Load MultiBreak before.");
        }
        return DATA_TYPE_INSTANCE.remove(itemStack);
    }

    /**
     * Checks if an ItemStack has a Figure associated with it.
     *
     * @param itemStack The ItemStack to check.
     * @return True if a Figure is present, false otherwise.
     */
    public static boolean hasFigure(@NotNull ItemStack itemStack) {
        if (DATA_TYPE_INSTANCE == null) {
            throw new IllegalStateException("MultiBreakAPI not initialized. Load MultiBreak before.");
        }
        return DATA_TYPE_INSTANCE.has(itemStack);
    }
}