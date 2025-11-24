package me.vermulst.multibreak.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class LocationKeyUtil {
    /**
     * Packs the X, Y, and Z coordinates of a Block into a single 64-bit long.
     * This key is NOT world-unique; it only represents the coordinates.
     * * @param block The Bukkit Block object.
     * @return A single long representing the coordinates.
     */
    public static long packBlock(Block block) {
        return packCoordinates(block.getX(), block.getY(), block.getZ());
    }

    public static long packLocation(Location location) {
        return packCoordinates(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Packs raw coordinates into a single 64-bit long using bitwise shifts.
     * Scheme: X (highest bits) | Z (middle bits) | Y (lowest bits)
     */
    public static long packCoordinates(int x, int y, int z) {
        long key = 0;

        // 1. Pack X: Shift X by 38 bits.
        key |= ((long) x & 0xFFFFFFFFL) << 38;

        // 2. Pack Z: Shift Z by 12 bits.
        key |= ((long) z & 0xFFFFFFFFL) << 12;

        // 3. Pack Y: Use the lowest 12 bits for Y.
        key |= (long) y & 0x0FFF;

        return key;
    }


    // --- UNPACKING (long to Coordinates) ---

    /**
     * Gets the X coordinate from a packed long key.
     */
    public static int getX(long key) {
        return (int) (key >> 38);
    }

    /**
     * Gets the Y coordinate from a packed long key.
     */
    public static int getY(long key) {
        // Mask the lowest 12 bits where Y is stored.
        return (int) (key & 0x0FFF);
    }

    /**
     * Gets the Z coordinate from a packed long key.
     */
    public static int getZ(long key) {
        // Shift right to position Z, mask out X.
        return (int) ((key >> 12) & 0x3FFFFFF);
    }


    /**
     * Checks if a Block's coordinates match the coordinates encoded in a Packed long key.
     * * This is an inverse operation to packBlock, often used to check if a live Block
     * matches a stored coordinate key without having to unpack the long first.
     * * @param block The live Block object to check.
     * @param packedKey The stored long key representing a specific coordinate.
     * @return True if the Block's X, Y, and Z match the key's encoded X, Y, and Z.
     */
    public static boolean matchBlock(Block block, long packedKey) {
        // The most performant way to match is to simply pack the block and compare the two longs.
        // This avoids the overhead of three separate unpacking/extraction operations.
        long blockKey = packBlock(block);

        // Primitive long equality is the fastest comparison available.
        return blockKey == packedKey;
    }

    public static boolean matchLocation(Location location, long packedKey) {
        // The most performant way to match is to simply pack the block and compare the two longs.
        // This avoids the overhead of three separate unpacking/extraction operations.
        long locationKey = packLocation(location);

        // Primitive long equality is the fastest comparison available.
        return locationKey == packedKey;
    }
}
