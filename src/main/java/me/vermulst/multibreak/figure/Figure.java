package me.vermulst.multibreak.figure;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.types.FigureType;
import me.vermulst.multibreak.multibreak.MultiBlock;
import me.vermulst.multibreak.utils.CompassDirection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public abstract class Figure {

    private final int width;
    private final int height;
    private final int depth;

    private int offSetWidth = 0;
    private int offSetHeight = 0;
    private int offSetDepth = 0;

    private short rotationWidth = 0;
    private short rotationHeight = 0;
    private short rotationDepth = 0;

    public Figure(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public abstract Set<Vector> getVectors(boolean rotated);
    public abstract FigureType getFigureType();


    public BlockFace getBlockFace(Player p) {
        return p.getTargetBlockFace(Main.getInstance().getConfigManager().getMaxRange());
    }

    public Set<Block> getBlocks(Player p, Block targetBlock) {
        Set<Block> blocks = new HashSet<>();
        BlockFace blockFace = this.getBlockFace(p);
        if (blockFace == null) return blocks;
        CompassDirection compassDirection = CompassDirection.getCompassDir(p.getLocation());

        boolean rotated = this.getRotationWidth() != 0.0 || this.getRotationHeight() != 0.0 || this.getRotationDepth() != 0.0;
        Set<Vector> blockVectors = this.getVectors(rotated);

        // apply figure rotation
        if (rotated) {
            blockVectors = this.applyRotation(blockVectors);
        }

        // apply player rotation
        VectorTransformer vectorTransformer = new VectorTransformer(blockFace.getDirection(), compassDirection);
        for (Vector vector : blockVectors) {
            vectorTransformer.rotateVector(vector);
        }

        // exclude center block - (Important after rotation)
        blockVectors.remove(new Vector(0, 0, 0));
        Location loc = targetBlock.getLocation();

        for (Vector vector : blockVectors) {
            Block block = loc.clone().add(vector).getBlock();
            Material type = block.getType();
            if ((block.isLiquid() && type != Material.POWDER_SNOW) || !type.isItem()) continue;
            blocks.add(block);
        }
        return blocks;
    }

    private Set<Vector> applyRotation(Set<Vector> vectors) {
        HashSet<Vector> rotatedVectors = new HashSet<>();
        Matrix4x4 rotationMatrix = new Matrix4x4();
        rotationMatrix.setRotationX(this.getRotationWidth() * (Math.PI / 180));
        rotationMatrix.setRotationY(this.getRotationHeight() * (Math.PI / 180));
        rotationMatrix.setRotationZ(this.getRotationDepth() * (Math.PI / 180));

        for (Vector vector : vectors) {
            rotationMatrix.transform(vector);
            vector.setX(Math.round(vector.getX()));
            vector.setY(Math.round(vector.getY()));
            vector.setZ(Math.round(vector.getZ()));
            rotatedVectors.add(new Vector(Math.round(vector.getX()), Math.round(vector.getY()), Math.round(vector.getZ())));
        }
        return rotatedVectors;
    }

    public void setOffsets(int offSetWidth, int offSetHeight, int offSetDepth) {
        this.offSetWidth = offSetWidth;
        this.offSetHeight = offSetHeight;
        this.offSetDepth = offSetDepth;
    }

    public void setRotations(short rotationWidth, short rotationHeight, short rotationDepth) {
        this.rotationWidth = rotationWidth;
        this.rotationHeight = rotationHeight;
        this.rotationDepth = rotationDepth;
    }

    public List<TextComponent> getLore() {
        List<TextComponent> lore = new ArrayList<>();


        TextComponent widthC = Component.text("Width: ", NamedTextColor.GRAY);
        TextComponent heightC = Component.text("Height: ", NamedTextColor.GRAY);
        TextComponent depthC = Component.text("Depth: ", NamedTextColor.GRAY);

        lore.add(Component.empty());
        lore.add(Component.text()
                .content("Size")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.UNDERLINED, true)
                .decoration(TextDecoration.BOLD, true)
                .build());

        // Add dimensions
        lore.add(widthC
                .append(Component.text(this.getWidth(), NamedTextColor.YELLOW)));
        lore.add(heightC
                .append(Component.text(this.getHeight(), NamedTextColor.YELLOW)));
        lore.add(depthC
                .append(Component.text(this.getDepth(), NamedTextColor.YELLOW)));


        lore.add(Component.empty());
        lore.add(Component.text()
                .content("Rotations")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.UNDERLINED, true)
                .build());

        lore.add(widthC
                .append(Component.text(this.getRotationWidth(), NamedTextColor.YELLOW)));
        lore.add(heightC
                .append(Component.text(this.getRotationHeight(), NamedTextColor.YELLOW)));
        lore.add(depthC
                .append(Component.text(this.getRotationDepth(), NamedTextColor.YELLOW)));


        lore.add(Component.empty());
        lore.add(Component.text()
                .content("Offsets")
                .color(TextColor.color(255, 255, 255))
                .decoration(TextDecoration.UNDERLINED, true)
                .build());

        lore.add(widthC
                .append(Component.text(this.getOffSetWidth(), NamedTextColor.YELLOW)));
        lore.add(heightC
                .append(Component.text(this.getOffSetHeight(), NamedTextColor.YELLOW)));
        lore.add(depthC
                .append(Component.text(this.getOffSetDepth(), NamedTextColor.YELLOW)));

        return lore;
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public int getOffSetWidth() {
        return offSetWidth;
    }

    public int getOffSetHeight() {
        return offSetHeight;
    }

    public int getOffSetDepth() {
        return offSetDepth;
    }

    public short getRotationWidth() {
        return rotationWidth;
    }

    public short getRotationHeight() {
        return rotationHeight;
    }

    public short getRotationDepth() {
        return rotationDepth;
    }

}
